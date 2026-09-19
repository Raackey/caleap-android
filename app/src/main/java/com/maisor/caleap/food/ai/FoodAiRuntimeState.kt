package com.maisor.caleap.food.ai

enum class FoodAiRuntimeStatus {
    SPECIALIZED_MODEL_ACTIVE,
    GENERAL_VISION_FALLBACK,
    MODEL_MISSING,
    MODEL_ERROR
}

data class FoodAiRuntimeState(
    val status: FoodAiRuntimeStatus,
    val modelId: String?,
    val modelVersion: String?,
    val message: String
)
