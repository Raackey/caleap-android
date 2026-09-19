package com.maisor.caleap.food.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.tensorflow.lite.Interpreter

/**
 * V23 validated image-classification adapter boundary.
 *
 * The model manifest supplies tensor dimensions and preprocessing.
 * This implementation supports float32 RGB image classifiers with a
 * single float output tensor. A model-specific manifest must be present.
 */
class ValidatedTfliteFoodVisionModelAdapter(
    private val context: Context,
    private val manifest: TfliteModelManifest
) : FoodVisionModelAdapter {

    override val modelId: String = manifest.modelId
    override val modelVersion: String = manifest.modelVersion
    override val license: String = manifest.license

    override suspend fun infer(input: FoodVisionInput): FoodVisionResult {
        val started = System.currentTimeMillis()

        return try {
            val bitmap = BitmapFactory.decodeStream(ByteArrayInputStream(input.imageBytes))
                ?: return errorResult(started, "Unable to decode image.")

            require(manifest.inputWidth > 0 && manifest.inputHeight > 0)
            require(manifest.outputLabels.isNotEmpty())

            val modelBytes = context.assets.open(manifest.modelAsset).use { it.readBytes() }
            val buffer = ByteBuffer.allocateDirect(modelBytes.size).order(ByteOrder.nativeOrder())
            buffer.put(modelBytes)
            buffer.rewind()

            val interpreter = Interpreter(buffer)
            val inputTensor = FloatArray(manifest.inputWidth * manifest.inputHeight * 3)
            val scaled = Bitmap.createScaledBitmap(
                bitmap, manifest.inputWidth, manifest.inputHeight, true
            )

            var k = 0
            for (y in 0 until manifest.inputHeight) {
                for (x in 0 until manifest.inputWidth) {
                    val pixel = scaled.getPixel(x, y)
                    inputTensor[k++] = ((pixel shr 16 and 0xff) / 255f - manifest.mean[0]) / manifest.std[0]
                    inputTensor[k++] = ((pixel shr 8 and 0xff) / 255f - manifest.mean[1]) / manifest.std[1]
                    inputTensor[k++] = ((pixel and 0xff) / 255f - manifest.mean[2]) / manifest.std[2]
                }
            }

            val output = Array(1) { FloatArray(manifest.outputLabels.size) }
            interpreter.run(arrayOf(inputTensor), output)
            interpreter.close()

            val candidates = output[0]
                .mapIndexed { index, score ->
                    FoodVisionCandidate(
                        label = manifest.outputLabels.getOrElse(index) { "unknown" },
                        confidence = score.coerceIn(0f, 1f),
                        modelId = modelId,
                        modelVersion = modelVersion,
                        license = license
                    )
                }
                .sortedByDescending { it.confidence }
                .take(manifest.topK)

            FoodVisionResult(
                candidates = candidates,
                modelId = modelId,
                modelVersion = modelVersion,
                inferenceMs = System.currentTimeMillis() - started,
                status = if (candidates.isEmpty()) VisionInferenceStatus.NO_FOOD
                         else VisionInferenceStatus.SUCCESS
            )
        } catch (e: Exception) {
            errorResult(started, e.message ?: "Inference failed.")
        }
    }

    private fun errorResult(started: Long, message: String) =
        FoodVisionResult(
            candidates = emptyList(),
            modelId = modelId,
            modelVersion = modelVersion,
            inferenceMs = System.currentTimeMillis() - started,
            status = VisionInferenceStatus.ERROR,
            errorMessage = message
        )
}

data class TfliteModelManifest(
    val modelId: String,
    val modelVersion: String,
    val modelAsset: String,
    val license: String,
    val inputWidth: Int,
    val inputHeight: Int,
    val mean: List<Float>,
    val std: List<Float>,
    val outputLabels: List<String>,
    val topK: Int = 5
)
