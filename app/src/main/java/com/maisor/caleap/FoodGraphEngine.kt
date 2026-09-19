package com.maisor.caleap

data class FoodGraphNode(
    val id: String,
    val type: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val countryIso2: List<String> = emptyList(),
    val cuisines: List<String> = emptyList()
)

data class FoodGraphEdge(
    val from: String,
    val type: String,
    val to: String
)

/**
 * Relationship-first food graph.
 *
 * The graph deliberately separates:
 * - canonical identity
 * - aliases
 * - ingredients
 * - cuisine/country context
 * - future nutrition/source records
 *
 * This prevents the AI classifier from being coupled directly to nutrition data.
 */
object FoodGraphEngine {
    private val nodes = listOf(
        FoodGraphNode(
            "food:rice", "ingredient", "Rice",
            aliases = listOf("arroz", "reis"),
            tags = listOf("grain")
        ),
        FoodGraphNode(
            "food:urad_dal", "ingredient", "Urad dal",
            aliases = listOf("black gram", "urad"),
            tags = listOf("legume")
        ),
        FoodGraphNode(
            "food:idli", "dish", "Idli",
            aliases = listOf("idly", "ಇಡ್ಲಿ", "इडली"),
            tags = listOf("fermented", "breakfast"),
            countryIso2 = listOf("IN"),
            cuisines = listOf("Indian")
        ),
        FoodGraphNode(
            "food:dosa", "dish", "Dosa",
            aliases = listOf("dosai", "ದೋಸೆ", "डोसा"),
            tags = listOf("fermented", "breakfast"),
            countryIso2 = listOf("IN"),
            cuisines = listOf("Indian")
        ),
        FoodGraphNode(
            "food:sushi", "dish", "Sushi",
            aliases = listOf("寿司"),
            tags = listOf("rice", "seafood"),
            countryIso2 = listOf("JP"),
            cuisines = listOf("Japanese")
        ),
        FoodGraphNode(
            "food:tacos", "dish", "Tacos",
            aliases = listOf("tacos"),
            tags = listOf("corn", "street-food"),
            countryIso2 = listOf("MX"),
            cuisines = listOf("Mexican")
        )
    )

    private val edges = listOf(
        FoodGraphEdge("food:idli", "CONTAINS_INGREDIENT", "food:rice"),
        FoodGraphEdge("food:idli", "CONTAINS_INGREDIENT", "food:urad_dal"),
        FoodGraphEdge("food:dosa", "CONTAINS_INGREDIENT", "food:rice"),
        FoodGraphEdge("food:dosa", "CONTAINS_INGREDIENT", "food:urad_dal"),
        FoodGraphEdge("food:sushi", "CONTAINS_INGREDIENT", "food:rice")
    )

    fun find(query: String, iso2: String? = null): List<FoodGraphNode> {
        val q = query.trim().lowercase()
        return nodes.filter { node ->
            val countryOk = iso2.isNullOrBlank() ||
                node.countryIso2.any { it.equals(iso2, ignoreCase = true) }
            val textOk = q.isBlank() ||
                node.name.lowercase().contains(q) ||
                node.aliases.any { it.lowercase().contains(q) } ||
                node.tags.any { it.lowercase().contains(q) }
            countryOk && textOk
        }
    }

    fun ingredientsFor(foodId: String): List<FoodGraphNode> {
        val ingredientIds = edges
            .filter { it.from == foodId && it.type == "CONTAINS_INGREDIENT" }
            .map { it.to }
            .toSet()
        return nodes.filter { it.id in ingredientIds }
    }
}
