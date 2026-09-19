package com.maisor.caleap.food.ai

class FoodVisionService(
    private val adapter: FoodVisionModelAdapter
) {
    suspend fun recognize(input: FoodVisionInput): FoodVisionResult {
        if (input.imageBytes.isEmpty()) {
            return FoodVisionResult(
                candidates = emptyList(),
                modelId = adapter.modelId,
                modelVersion = adapter.modelVersion,
                inferenceMs = 0L,
                status = VisionInferenceStatus.ERROR,
                errorMessage = "Image data is empty."
            )
        }
        return adapter.infer(input)
    }
}
