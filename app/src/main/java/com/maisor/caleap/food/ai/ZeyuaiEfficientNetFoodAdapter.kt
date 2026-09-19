package com.maisor.caleap.food.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.tensorflow.lite.Interpreter

class ZeyuaiEfficientNetFoodAdapter(
    private val context: Context
) : FoodVisionModelAdapter {

    override val modelId = "zeyuai/efficientnet-food-classifier"
    override val modelVersion = "main"
    override val license = "Apache-2.0"

    private val labels = listOf(
        "Baked Potato", "Burger", "Crispy Chicken", "Donut",
        "Fries", "Hot Dog", "Pizza", "Sandwich"
    )

    override suspend fun infer(input: FoodVisionInput): FoodVisionResult {
        val started = System.currentTimeMillis()

        return try {
            val bytes = context.assets.open("food_model.tflite").use { it.readBytes() }
            val modelBuffer = ByteBuffer.allocateDirect(bytes.size)
                .order(ByteOrder.nativeOrder())
            modelBuffer.put(bytes)
            modelBuffer.rewind()

            val interpreter = Interpreter(modelBuffer)
            val inputInfo = interpreter.getInputTensor(0)
            val outputInfo = interpreter.getOutputTensor(0)

            val bitmap = BitmapFactory.decodeStream(
                ByteArrayInputStream(input.imageBytes)
            ) ?: return fail(started, "Image could not be decoded.")

            val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
            val inputTensor = Array(1) { Array(224) { Array(224) { FloatArray(3) } } }

            for (y in 0 until 224) {
                for (x in 0 until 224) {
                    val p = resized.getPixel(x, y)
                    // The model card's TFLite example uses float32 RGB pixel values
                    // after resizing, without an additional normalization step.
                    inputTensor[0][y][x][0] = ((p shr 16) and 0xff).toFloat()
                    inputTensor[0][y][x][1] = ((p shr 8) and 0xff).toFloat()
                    inputTensor[0][y][x][2] = (p and 0xff).toFloat()
                }
            }

            val output = Array(1) { FloatArray(labels.size) }
            interpreter.run(inputTensor, output)
            interpreter.close()

            val candidates = output[0]
                .mapIndexed { index, score ->
                    FoodVisionCandidate(
                        label = labels[index],
                        confidence = score.coerceIn(0f, 1f),
                        modelId = modelId,
                        modelVersion = modelVersion,
                        license = license
                    )
                }
                .sortedByDescending { it.confidence }
                .take(5)

            FoodVisionResult(
                candidates = candidates,
                modelId = modelId,
                modelVersion = modelVersion,
                inferenceMs = System.currentTimeMillis() - started,
                status = if (candidates.isEmpty())
                    VisionInferenceStatus.NO_FOOD
                else
                    VisionInferenceStatus.SUCCESS
            )
        } catch (e: Exception) {
            fail(started, e.message ?: "TFLite inference failed.")
        }
    }

    private fun fail(started: Long, message: String) =
        FoodVisionResult(
            candidates = emptyList(),
            modelId = modelId,
            modelVersion = modelVersion,
            inferenceMs = System.currentTimeMillis() - started,
            status = VisionInferenceStatus.ERROR,
            errorMessage = message
        )
}
