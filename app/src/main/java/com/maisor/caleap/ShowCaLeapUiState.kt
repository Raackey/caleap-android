package com.maisor.caleap

sealed class ShowCaLeapUiState {
    data object Ready : ShowCaLeapUiState()
    data object Capturing : ShowCaLeapUiState()
    data object Understanding : ShowCaLeapUiState()

    data class FoodCandidates(
        val candidates: List<FoodCandidate>
    ) : ShowCaLeapUiState()

    data class ChooseVariant(
        val candidates: List<VariantCandidate>
    ) : ShowCaLeapUiState()

    data class ChoosePortion(
        val food: FoodGraphNode,
        val options: List<PortionOption>
    ) : ShowCaLeapUiState()

    data class Review(
        val record: FoodKnowledgeRecord
    ) : ShowCaLeapUiState()

    data class Saved(
        val foodName: String,
        val portion: FoodServing
    ) : ShowCaLeapUiState()

    data class Error(
        val message: String
    ) : ShowCaLeapUiState()
}

object ShowCaLeapController {
    fun start() = ShowCaLeapUiState.Capturing

    fun understanding() = ShowCaLeapUiState.Understanding

    fun analyze(request: FoodCaptureRequest): ShowCaLeapUiState {
        return when (val stage = FoodCaptureOrchestrator.resolve(request)) {
            is FoodCaptureStage.Candidates ->
                if (stage.items.isEmpty()) ShowCaLeapUiState.Error("No strong food match yet. Try a food name, clearer photo, or another angle.")
                else ShowCaLeapUiState.FoodCandidates(stage.items)
            is FoodCaptureStage.VariantQuestion ->
                ShowCaLeapUiState.ChooseVariant(stage.candidates)
            is FoodCaptureStage.PortionQuestion ->
                ShowCaLeapUiState.ChoosePortion(
                    stage.food,
                    FoodPortionEngine.options(stage.food)
                )
            is FoodCaptureStage.NutritionReview ->
                ShowCaLeapUiState.Review(stage.record)
            is FoodCaptureStage.Confirmed ->
                ShowCaLeapUiState.Saved(stage.record.canonicalName, stage.portion)
            else -> ShowCaLeapUiState.Understanding
        }
    }
}
