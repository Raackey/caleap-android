package com.maisor.caleap.food.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * V21 real-device vision adapter.
 *
 * ML Kit's bundled/default image-labeling model is a general visual model,
 * not a food-specialized classifier. We therefore expose its labels as
 * candidates and require the Food Graph / recognition policy to decide
 * whether they are useful food identities.
 *
 * This is intentionally not described as a dedicated food model.
 */
class MlKitVisionModelAdapter(
    private val bitmapDecoder: (ByteArray) -> Bitmap? = { bytes ->
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
) : FoodVisionModelAdapter {

    override val modelId: String = "google-mlkit-image-labeling-default"
    override val modelVersion: String = "17.0.9"
    override val license: String = "ML Kit SDK terms; verify distribution terms before production release"

    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    override suspend fun infer(input: FoodVisionInput): FoodVisionResult {
        val bitmap = bitmapDecoder(input.imageBytes)
            ?: return FoodVisionResult(
                candidates = emptyList(),
                modelId = modelId,
                modelVersion = modelVersion,
                inferenceMs = 0L,
                status = VisionInferenceStatus.ERROR,
                errorMessage = "Image bytes could not be decoded."
            )

        val start = System.nanoTime()

        return suspendCancellableCoroutine { continuation ->
            val task = labeler.process(InputImage.fromBitmap(bitmap, 0))

            task.addOnSuccessListener { labels ->
                val candidates = labels
                    .sortedByDescending { it.confidence }
                    .take(10)
                    .map {
                        FoodVisionCandidate(
                            label = it.text,
                            confidence = it.confidence.coerceIn(0f, 1f),
                            modelId = modelId,
                            modelVersion = modelVersion,
                            license = license
                        )
                    }

                val status = when {
                    candidates.isEmpty() -> VisionInferenceStatus.NO_FOOD
                    candidates.first().confidence < 0.30f -> VisionInferenceStatus.LOW_CONFIDENCE
                    else -> VisionInferenceStatus.SUCCESS
                }

                if (continuation.isActive) {
                    continuation.resume(
                        FoodVisionResult(
                            candidates = candidates,
                            modelId = modelId,
                            modelVersion = modelVersion,
                            inferenceMs = (System.nanoTime() - start) / 1_000_000L,
                            status = status
                        )
                    )
                }
            }.addOnFailureListener { error ->
                if (continuation.isActive) {
                    continuation.resume(
                        FoodVisionResult(
                            candidates = emptyList(),
                            modelId = modelId,
                            modelVersion = modelVersion,
                            inferenceMs = (System.nanoTime() - start) / 1_000_000L,
                            status = VisionInferenceStatus.ERROR,
                            errorMessage = error.message
                        )
                    )
                }
            }

            // Google Tasks are not cancellable through a public `cancel()` API here.
            // The continuation guard above prevents a late callback from updating
            // a cancelled coroutine.
        }
    }
}
