package com.maisor.caleap.food.ai

data class BenchmarkCase(
    val id: String,
    val expectedLabels: Set<String>,
    val imagePath: String
)

data class BenchmarkPrediction(
    val caseId: String,
    val candidates: List<RankedFoodCandidate>
)

data class BenchmarkMetrics(
    val totalCases: Int,
    val top1Correct: Int,
    val top5Correct: Int,
    val abstentions: Int,
    val top1Accuracy: Float,
    val top5Accuracy: Float,
    val abstentionRate: Float
)

class FoodVisionBenchmarkEvaluator {

    fun evaluate(
        cases: List<BenchmarkCase>,
        predictions: List<BenchmarkPrediction>
    ): BenchmarkMetrics {
        var top1 = 0
        var top5 = 0
        var abstain = 0

        val byId = predictions.associateBy { it.caseId }

        for (case in cases) {
            val p = byId[case.id]?.candidates.orEmpty()
            if (p.isEmpty()) {
                abstain++
                continue
            }

            if (case.expectedLabels.any {
                    p.first().label.equals(it, ignoreCase = true)
                }) top1++

            if (p.take(5).any { candidate ->
                    case.expectedLabels.any {
                        candidate.label.equals(it, ignoreCase = true)
                    }
                }) top5++
        }

        val n = cases.size.coerceAtLeast(1)
        return BenchmarkMetrics(
            totalCases = cases.size,
            top1Correct = top1,
            top5Correct = top5,
            abstentions = abstain,
            top1Accuracy = top1.toFloat() / n,
            top5Accuracy = top5.toFloat() / n,
            abstentionRate = abstain.toFloat() / n
        )
    }
}
