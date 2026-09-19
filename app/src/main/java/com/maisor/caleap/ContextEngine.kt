package com.maisor.caleap

data class DailyContextInput(
    val meals: Int,
    val calories: Double,
    val proteinGrams: Double,
    val fibreGrams: Double,
    val waterMl: Int,
    val steps: Long?,
    val sleepMinutes: Long?,
    val weightKg: Double?
)

data class DailyInsight(
    val score: Int,
    val headline: String,
    val explanation: String,
    val action: String
)

object ContextEngine {
    fun evaluate(input: DailyContextInput): DailyInsight {
        var score = 70
        val notes = mutableListOf<String>()
        var action = "Keep your next choice simple and consistent."

        if (input.meals == 0) {
            score -= 10
            notes += "No meal has been logged yet."
            action = "Show CaLeap your next meal so your day has more context."
        } else {
            score += 5
        }

        if (input.waterMl < 1200) {
            score -= 8
            notes += "Water intake is still relatively low."
            action = "Have a glass of water and continue logging naturally."
        } else if (input.waterMl >= 2000) {
            score += 5
            notes += "Hydration logging looks consistent."
        }

        if (input.proteinGrams > 0 && input.proteinGrams < 45) {
            score -= 5
            notes += "Protein intake is currently modest."
            action = "Consider adding a protein-rich food to your next meal."
        }

        if (input.fibreGrams > 0 && input.fibreGrams < 15) {
            score -= 4
            notes += "Fibre intake is currently modest."
        }

        input.steps?.let {
            when {
                it >= 8000 -> score += 7
                it < 3000 -> {
                    score -= 5
                    notes += "Activity is still low today."
                    action = "A short comfortable walk could add useful movement."
                }
            }
        }

        input.sleepMinutes?.let {
            when {
                it >= 420 -> score += 5
                it in 0..359 -> {
                    score -= 7
                    notes += "Sleep duration was relatively short."
                }
            }
        }

        val bounded = score.coerceIn(0, 100)
        val headline = when {
            bounded >= 85 -> "Your day is building a good rhythm."
            bounded >= 70 -> "Your health picture is taking shape."
            else -> "There are a few simple signals worth noticing."
        }

        val explanation = if (notes.isEmpty()) {
            "CaLeap is using the information available today and keeping the interpretation conservative."
        } else {
            notes.take(2).joinToString(" ")
        }

        return DailyInsight(bounded, headline, explanation, action)
    }
}
