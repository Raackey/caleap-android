package com.maisor.caleap

data class PortionOption(
    val label: String,
    val gramsEstimate: Double? = null,
    val requiresConfirmation: Boolean = true
)

object FoodPortionEngine {
    fun options(food: FoodGraphNode): List<PortionOption> {
        return when (food.type) {
            "dish" -> listOf(
                PortionOption("Small", null),
                PortionOption("Regular", null),
                PortionOption("Large", null)
            )
            "ingredient" -> listOf(
                PortionOption("1 tbsp", null),
                PortionOption("1/2 cup", null),
                PortionOption("1 cup", null)
            )
            else -> listOf(PortionOption("Standard serving", null))
        }
    }
}
