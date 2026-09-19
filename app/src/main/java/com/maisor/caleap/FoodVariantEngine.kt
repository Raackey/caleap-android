package com.maisor.caleap

data class FoodVariant(
    val variantId: String,
    val canonicalFoodId: String,
    val name: String,
    val aliases: List<String>,
    val countryIso2: List<String>,
    val regions: List<String>,
    val cuisines: List<String>,
    val ingredients: List<String>,
    val tags: List<String>,
    val preparationMethods: List<String>,
    val servingUnits: List<String>
)

data class VariantCandidate(
    val variant: FoodVariant,
    val confidence: Double,
    val reasons: List<String>
)

object FoodVariantEngine {
    private val variants = listOf(
        FoodVariant(
            "food:dosa:plain","food:dosa","Plain Dosa",
            listOf("plain dosa","sada dosa"),listOf("IN"),
            listOf("South Asian"),listOf("Indian"),
            listOf("food:rice","food:urad_dal"),
            listOf("fermented","breakfast"),listOf("griddle-cooked"),
            listOf("piece","plate")
        ),
        FoodVariant(
            "food:dosa:masala","food:dosa","Masala Dosa",
            listOf("masala dosa"),listOf("IN"),
            listOf("South Asian"),listOf("Indian"),
            listOf("food:rice","food:urad_dal","food:potato"),
            listOf("fermented","breakfast","stuffed"),listOf("griddle-cooked","stuffed"),
            listOf("piece","plate")
        ),
        FoodVariant(
            "food:dosa:ragi","food:dosa","Ragi Dosa",
            listOf("finger millet dosa"),listOf("IN"),
            listOf("South Asian"),listOf("Indian"),
            listOf("food:ragi","food:rice","food:urad_dal"),
            listOf("millet","fermented","breakfast"),listOf("griddle-cooked"),
            listOf("piece","plate")
        ),
        FoodVariant(
            "food:dosa:neer","food:dosa","Neer Dosa",
            listOf("neer dosai"),listOf("IN"),
            listOf("South Asian"),listOf("Indian"),
            listOf("food:rice"),listOf("thin","breakfast"),listOf("griddle-cooked"),
            listOf("piece","plate")
        ),
        FoodVariant(
            "food:idli:plain","food:idli","Plain Idli",
            listOf("idli"),listOf("IN"),
            listOf("South Asian"),listOf("Indian"),
            listOf("food:rice","food:urad_dal"),listOf("steamed","fermented","breakfast"),
            listOf("steamed"),listOf("piece","plate")
        ),
        FoodVariant(
            "food:idli:ragi","food:idli","Ragi Idli",
            listOf("finger millet idli"),listOf("IN"),
            listOf("South Asian"),listOf("Indian"),
            listOf("food:ragi","food:rice","food:urad_dal"),
            listOf("millet","steamed","fermented"),listOf("steamed"),
            listOf("piece","plate")
        ),
        FoodVariant(
            "food:rice:biryani","food:rice","Biryani",
            listOf("biriyani"),listOf("IN","PK","BD"),
            listOf("South Asian"),listOf("Indian","Pakistani","Bangladeshi"),
            listOf("food:rice"),listOf("rice-dish","mixed-dish"),
            listOf("layered","cooked"),listOf("bowl","plate")
        )
    )

    fun candidates(
        query: String,
        iso2: String? = null,
        cuisine: String? = null
    ): List<VariantCandidate> {
        val q = query.trim().lowercase()
        return variants.mapNotNull { v ->
            if (iso2 != null && v.countryIso2.none { it.equals(iso2, true) }) return@mapNotNull null
            if (cuisine != null && v.cuisines.none { it.equals(cuisine, true) }) return@mapNotNull null

            var score = 0.0
            val reasons = mutableListOf<String>()
            when {
                v.name.lowercase() == q -> { score += 0.65; reasons += "exact variant name" }
                v.name.lowercase().contains(q) && q.isNotBlank() -> { score += 0.45; reasons += "variant name match" }
                v.aliases.any { it.lowercase().contains(q) } && q.isNotBlank() -> { score += 0.50; reasons += "alias match" }
                v.canonicalFoodId.removePrefix("food:").contains(q) && q.isNotBlank() -> { score += 0.30; reasons += "canonical food match" }
                else -> return@mapNotNull null
            }
            if (iso2 != null) { score += 0.10; reasons += "country context" }
            if (cuisine != null) { score += 0.10; reasons += "cuisine context" }
            VariantCandidate(v, score.coerceAtMost(0.99), reasons)
        }.sortedByDescending { it.confidence }
    }

    fun variantsForCanonical(foodId: String): List<FoodVariant> =
        variants.filter { it.canonicalFoodId == foodId }
}
