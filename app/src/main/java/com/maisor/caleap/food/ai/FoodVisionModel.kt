package com.maisor.caleap.food.ai

/**
 * CaLeap V20 canonical food-vision model contract.
 *
 * One boundary for every food vision model:
 * model inference -> candidates -> fusion -> recognition policy.
 *
 * Nutrition is intentionally outside this layer.
 */
data class FoodVisionInput(
    val imageBytes: ByteArray,
    val sourceLabel: String? = null
)

data class FoodVisionCandidate(
    val label: String,
    val confidence: Float,
    val boundingBox: BoundingBox? = null,
    val modelId: String,
    val modelVersion: String,
    val license: String
)

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class FoodVisionResult(
    val candidates: List<FoodVisionCandidate>,
    val modelId: String,
    val modelVersion: String,
    val inferenceMs: Long,
    val status: VisionInferenceStatus,
    val errorMessage: String? = null
)

enum class VisionInferenceStatus {
    SUCCESS,
    NO_FOOD,
    LOW_CONFIDENCE,
    MODEL_UNAVAILABLE,
    ERROR
}

interface FoodVisionModelAdapter {
    val modelId: String
    val modelVersion: String
    val license: String

    suspend fun infer(input: FoodVisionInput): FoodVisionResult
}

/**
 * Safe adapter used until a verified mobile model artifact is bundled.
 * Never fabricates food predictions.
 */
class UnavailableFoodVisionModelAdapter : FoodVisionModelAdapter {
    override val modelId = "none"
    override val modelVersion = "0"
    override val license = "N/A"

    override suspend fun infer(input: FoodVisionInput): FoodVisionResult =
        FoodVisionResult(
            candidates = emptyList(),
            modelId = modelId,
            modelVersion = modelVersion,
            inferenceMs = 0L,
            status = VisionInferenceStatus.MODEL_UNAVAILABLE,
            errorMessage = "No verified food-vision model artifact is bundled."
        )
}
