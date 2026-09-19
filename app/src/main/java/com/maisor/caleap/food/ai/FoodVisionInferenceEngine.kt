package com.maisor.caleap.food.ai

/**
 * V21 single entry point for real image inference.
 */
class FoodVisionInferenceEngine(
    private val model: FoodVisionModelAdapter = MlKitVisionModelAdapter()
) {
    val modelId: String get() = model.modelId
    val modelVersion: String get() = model.modelVersion

    suspend fun run(imageBytes: ByteArray, sourceLabel: String? = null): FoodVisionResult {
        return FoodVisionService(model).recognize(
            FoodVisionInput(imageBytes = imageBytes, sourceLabel = sourceLabel)
        )
    }
}
