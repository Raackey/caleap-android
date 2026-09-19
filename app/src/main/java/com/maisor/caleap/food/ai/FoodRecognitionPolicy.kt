package com.maisor.caleap.food.ai

data class FoodRecognitionDecision(
    val state: RecognitionState,
    val candidates: List<RankedFoodCandidate>,
    val reason: String
)

enum class RecognitionState {
    CONFIDENT,
    REVIEW_REQUIRED,
    NO_RELIABLE_MATCH,
    MODEL_UNAVAILABLE
}

class FoodRecognitionPolicy(
    private val confidentThreshold: Float = 0.82f,
    private val reviewThreshold: Float = 0.55f
) {
    fun decide(
        candidates: List<RankedFoodCandidate>,
        modelStatus: VisionInferenceStatus
    ): FoodRecognitionDecision {
        if (modelStatus == VisionInferenceStatus.MODEL_UNAVAILABLE) {
            return FoodRecognitionDecision(
                RecognitionState.MODEL_UNAVAILABLE,
                candidates,
                "A verified food-vision model is not available."
            )
        }

        if (candidates.isEmpty() ||
            modelStatus == VisionInferenceStatus.NO_FOOD) {
            return FoodRecognitionDecision(
                RecognitionState.NO_RELIABLE_MATCH,
                candidates,
                "No reliable food candidate was produced."
            )
        }

        val top = candidates.first()

        return when {
            top.confidence >= confidentThreshold ->
                FoodRecognitionDecision(
                    RecognitionState.CONFIDENT,
                    candidates,
                    "Top candidate passed the confidence threshold."
                )

            top.confidence >= reviewThreshold ->
                FoodRecognitionDecision(
                    RecognitionState.REVIEW_REQUIRED,
                    candidates,
                    "Candidate is plausible but needs user confirmation."
                )

            else ->
                FoodRecognitionDecision(
                    RecognitionState.NO_RELIABLE_MATCH,
                    candidates,
                    "Confidence is too low for a reliable identification."
                )
        }
    }
}
