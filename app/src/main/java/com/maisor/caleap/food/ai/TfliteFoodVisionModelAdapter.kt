package com.maisor.caleap.food.ai

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * V22 TFLite runtime adapter.
 *
 * It only loads a model when the exact artifact exists in app/src/main/assets.
 * It never silently substitutes a different model.
 */
class TfliteFoodVisionModelAdapter(
    private val context: Context,
    private val modelAsset: String,
    private val labels: List<String>,
    override val modelId: String,
    override val modelVersion: String,
    override val license: String
) : FoodVisionModelAdapter {

    override suspend fun infer(input: FoodVisionInput): FoodVisionResult {
        val started = System.currentTimeMillis()

        return try {
            val model = context.assets.open(modelAsset).use { it.readBytes() }
            val interpreter = Interpreter(ByteBuffer.allocateDirect(model.size)
                .order(ByteOrder.nativeOrder()).apply { put(model); rewind() })

            // The runtime boundary is intentionally conservative. A model-specific
            // preprocessor/output decoder must be supplied with the verified artifact.
            interpreter.close()

            FoodVisionResult(
                candidates = emptyList(),
                modelId = modelId,
                modelVersion = modelVersion,
                inferenceMs = System.currentTimeMillis() - started,
                status = VisionInferenceStatus.ERROR,
                errorMessage = "Model artifact loaded, but its input/output contract has not been validated for this adapter."
            )
        } catch (e: Exception) {
            FoodVisionResult(
                candidates = emptyList(),
                modelId = modelId,
                modelVersion = modelVersion,
                inferenceMs = System.currentTimeMillis() - started,
                status = VisionInferenceStatus.MODEL_UNAVAILABLE,
                errorMessage = "Verified model artifact unavailable: ${e.message}"
            )
        }
    }

    companion object {
        fun fromVerifiedAsset(
            context: Context,
            modelAsset: String,
            labels: List<String>,
            modelId: String,
            modelVersion: String,
            license: String
        ): TfliteFoodVisionModelAdapter =
            TfliteFoodVisionModelAdapter(
                context, modelAsset, labels, modelId, modelVersion, license
            )
    }
}
