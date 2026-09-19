package com.maisor.caleap

data class FoodServing(
    val amount: Double,
    val unit: String
)

data class NutrientRange(
    val min: Double? = null,
    val max: Double? = null
)

data class FoodNutrition(
    val energyKcal: NutrientRange = NutrientRange(),
    val proteinG: NutrientRange = NutrientRange(),
    val carbohydrateG: NutrientRange = NutrientRange(),
    val fatG: NutrientRange = NutrientRange(),
    val fibreG: NutrientRange = NutrientRange()
)

data class FoodSource(
    val sourceId: String,
    val sourceName: String,
    val sourceVersion: String? = null,
    val license: String,
    val sourceUrl: String
)

data class FoodKnowledgeRecord(
    val foodId: String,
    val canonicalName: String,
    val aliases: List<String>,
    val countryIso2: String,
    val region: String,
    val cuisine: String,
    val foodType: String,
    val ingredients: List<String>,
    val defaultServing: FoodServing,
    val nutrition: FoodNutrition,
    val source: FoodSource,
    val confidence: Double,
    val lastVerified: String? = null
)

object FoodKnowledgeRepository {
    // V12 establishes the normalized contract. Nutrient records are only populated
    // after source/license verification; null is preferable to invented values.
    private val seed = listOf(
        FoodKnowledgeRecord(
            foodId = "IN-idli",
            canonicalName = "Idli",
            aliases = listOf("idly"),
            countryIso2 = "IN",
            region = "South Asian",
            cuisine = "Indian",
            foodType = "dish",
            ingredients = listOf("rice", "urad-dal"),
            defaultServing = FoodServing(100.0, "g"),
            nutrition = FoodNutrition(),
            source = FoodSource(
                "PENDING_IFCT",
                "ICMR-NIN Indian Food Composition Tables",
                "2017",
                "VERIFY_BEFORE_INGESTION",
                "https://www.nin.res.in/ebooks/IFCT2017.pdf"
            ),
            confidence = 0.0
        ),
        FoodKnowledgeRecord(
            foodId = "JP-sushi",
            canonicalName = "Sushi",
            aliases = emptyList(),
            countryIso2 = "JP",
            region = "East Asian",
            cuisine = "Japanese",
            foodType = "dish",
            ingredients = listOf("rice", "fish"),
            defaultServing = FoodServing(100.0, "g"),
            nutrition = FoodNutrition(),
            source = FoodSource("PENDING", "Source to be selected", license = "VERIFY_BEFORE_INGESTION", sourceUrl = ""),
            confidence = 0.0
        )
    )

    fun findById(foodId: String): FoodKnowledgeRecord? =
        seed.firstOrNull { it.foodId == foodId }

    fun search(query: String, iso2: String? = null): List<FoodKnowledgeRecord> {
        val q = query.trim().lowercase()
        return seed.filter { record ->
            val countryMatches = iso2.isNullOrBlank() || record.countryIso2.equals(iso2, true)
            val textMatches = q.isBlank() ||
                record.canonicalName.lowercase().contains(q) ||
                record.aliases.any { it.lowercase().contains(q) }
            countryMatches && textMatches
        }
    }
}
