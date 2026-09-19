package com.maisor.caleap.food.ai

data class RankedFoodCandidate(
    val label: String,
    val score: Float,
    val source: String,
    val confidence: Float
)

/**
 * V19 candidate fusion keeps model output separate from food knowledge.
 * It ranks candidates but does not invent nutrition or identity certainty.
 */
class FoodCandidateFusionEngine {

    fun rank(
        vision: List<FoodVisionCandidate>,
        lexicalMatches: List<RankedFoodCandidate> = emptyList()
    ): List<RankedFoodCandidate> {
        val visionRanked = vision.map {
            RankedFoodCandidate(
                label = it.label,
                score = it.confidence.coerceIn(0f, 1f),
                source = it.modelId,
                confidence = it.confidence.coerceIn(0f, 1f)
            )
        }

        return (visionRanked + lexicalMatches)
            .groupBy { it.label.trim().lowercase() }
            .map { (_, items) ->
                val best = items.maxByOrNull { it.score }!!
                val agreementBonus =
                    if (items.map { it.source }.distinct().size > 1) 0.05f else 0f

                best.copy(
                    score = (best.score + agreementBonus).coerceAtMost(1f),
                    confidence = (best.confidence + agreementBonus).coerceAtMost(1f)
                )
            }
            .sortedByDescending { it.score }
    }
}
