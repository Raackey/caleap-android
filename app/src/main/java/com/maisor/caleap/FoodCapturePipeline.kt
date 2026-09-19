package com.maisor.caleap

/**
 * Orchestrates the CaLeap food capture journey without pretending that
 * individual stages are more certain than their evidence.
 */
sealed class FoodCaptureStage {
    data object Idle : FoodCaptureStage()
    data object Understanding : FoodCaptureStage()
    data class Candidates(val items: List<FoodCandidate>) : FoodCaptureStage()
    data class VariantQuestion(val candidates: List<VariantCandidate>) : FoodCaptureStage()
    data class PortionQuestion(val food: FoodGraphNode) : FoodCaptureStage()
    data class NutritionReview(val record: FoodKnowledgeRecord) : FoodCaptureStage()
    data class Confirmed(val record: FoodKnowledgeRecord, val portion: FoodServing) : FoodCaptureStage()
}

data class FoodCaptureRequest(
    val query: String,
    val countryIso2: String? = null,
    val cuisine: String? = null
)

object FoodCaptureOrchestrator {
    fun resolve(request: FoodCaptureRequest): FoodCaptureStage {
        val variants = FoodVariantEngine.candidates(
            query = request.query,
            iso2 = request.countryIso2,
            cuisine = request.cuisine
        )

        if (variants.size > 1) {
            return FoodCaptureStage.VariantQuestion(variants.take(5))
        }

        val graph = FoodGraphEngine.find(request.query, request.countryIso2)
        if (graph.isNotEmpty()) {
            return FoodCaptureStage.PortionQuestion(graph.first())
        }

        return FoodCaptureStage.Candidates(
            GlobalFoodEngine.search(request.query, request.countryIso2)
        )
    }
}
