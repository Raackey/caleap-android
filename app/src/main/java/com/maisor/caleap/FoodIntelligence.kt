package com.maisor.caleap

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlin.math.roundToInt

/** Nutrition is deliberately represented as a range rather than false precision. */
data class NutritionRange(
    val kcalMin: Int,
    val kcalMax: Int,
    val proteinMin: Double,
    val proteinMax: Double,
    val carbsMin: Double,
    val carbsMax: Double,
    val fatMin: Double,
    val fatMax: Double,
    val fibreMin: Double,
    val fibreMax: Double
)

data class FoodItem(
    val name: String,
    val unit: String,
    val nutrition: NutritionRange,
    val aliases: List<String> = emptyList()
)

data class FoodAnalysis(
    val source: String,
    val labels: List<String> = emptyList(),
    val candidates: List<FoodItem> = emptyList(),
    val confidence: Int? = null,
    val needsConfirmation: Boolean = true
)

object IndianFoodCatalog {
    val items = listOf(
        FoodItem("Rice", "1 cup", NutritionRange(200, 240, 4.0, 5.0, 43.0, 52.0, 0.3, 0.7, 0.5, 1.0), listOf("rice", "anna", "bhat", "bath")),
        FoodItem("Dal", "1 bowl", NutritionRange(150, 210, 8.0, 11.0, 20.0, 30.0, 2.0, 5.0, 5.0, 8.0), listOf("dal", "dhal", "sambar", "lentil")),
        FoodItem("Idli", "2 pieces", NutritionRange(110, 160, 4.0, 6.0, 22.0, 30.0, 0.5, 2.0, 1.0, 2.0), listOf("idli", "idly")),
        FoodItem("Dosa", "1 medium", NutritionRange(170, 260, 4.0, 7.0, 25.0, 38.0, 5.0, 10.0, 1.0, 3.0), listOf("dosa", "dosai")),
        FoodItem("Roti", "2 pieces", NutritionRange(140, 220, 5.0, 8.0, 24.0, 36.0, 2.0, 5.0, 3.0, 6.0), listOf("roti", "chapati", "chapathi")),
        FoodItem("Vegetable curry", "1 serving", NutritionRange(70, 150, 2.0, 5.0, 8.0, 18.0, 3.0, 9.0, 3.0, 6.0), listOf("vegetable", "vegetables", "sabzi", "curry")),
        FoodItem("Curd", "1 cup", NutritionRange(90, 140, 4.0, 7.0, 5.0, 9.0, 4.0, 8.0, 0.0, 0.5), listOf("curd", "yogurt", "yoghurt", "mosaru")),
        FoodItem("Egg", "2 eggs", NutritionRange(140, 180, 12.0, 14.0, 1.0, 2.0, 9.0, 12.0, 0.0, 0.0), listOf("egg", "eggs", "anda")),
        FoodItem("Banana", "1 medium", NutritionRange(90, 120, 1.0, 2.0, 23.0, 31.0, 0.2, 0.5, 2.0, 4.0), listOf("banana", "kele")),
        FoodItem("Groundnut", "30 g", NutritionRange(160, 190, 7.0, 9.0, 5.0, 8.0, 13.0, 16.0, 2.0, 4.0), listOf("groundnut", "peanut", "peanuts", "shenga")),
        FoodItem("Sambar", "1 bowl", NutritionRange(90, 150, 4.0, 7.0, 12.0, 20.0, 2.0, 6.0, 3.0, 6.0), listOf("sambar", "sambhar")),
        FoodItem("Coconut chutney", "2 tbsp", NutritionRange(70, 120, 1.0, 2.0, 3.0, 6.0, 6.0, 10.0, 1.0, 2.0), listOf("coconut chutney", "chutney")),
        FoodItem("Poha", "1 bowl", NutritionRange(180, 260, 4.0, 7.0, 32.0, 44.0, 5.0, 9.0, 2.0, 5.0), listOf("poha", "avalakki", "aval")),
        FoodItem("Upma", "1 bowl", NutritionRange(190, 280, 5.0, 8.0, 30.0, 45.0, 5.0, 10.0, 2.0, 5.0), listOf("upma", "uppittu")),
        FoodItem("Pongal", "1 bowl", NutritionRange(220, 330, 7.0, 10.0, 32.0, 48.0, 7.0, 13.0, 3.0, 6.0), listOf("pongal", "ven pongal")),
        FoodItem("Lemon rice", "1 bowl", NutritionRange(240, 340, 4.0, 7.0, 40.0, 55.0, 7.0, 13.0, 2.0, 4.0), listOf("lemon rice", "chitranna")),
        FoodItem("Biryani", "1 serving", NutritionRange(420, 650, 14.0, 24.0, 55.0, 82.0, 12.0, 25.0, 3.0, 6.0), listOf("biryani", "biriyani")),
        FoodItem("Paneer", "100 g", NutritionRange(240, 320, 16.0, 22.0, 4.0, 8.0, 17.0, 25.0, 0.0, 1.0), listOf("paneer", "cottage cheese")),
        FoodItem("Chicken curry", "1 serving", NutritionRange(220, 380, 20.0, 32.0, 5.0, 14.0, 12.0, 25.0, 1.0, 3.0), listOf("chicken curry", "chicken")),
        FoodItem("Fish curry", "1 serving", NutritionRange(180, 320, 18.0, 30.0, 4.0, 12.0, 8.0, 20.0, 1.0, 3.0), listOf("fish curry", "fish")),
        FoodItem("Apple", "1 medium", NutritionRange(80, 110, 0.0, 1.0, 20.0, 30.0, 0.0, 1.0, 3.0, 5.0), listOf("apple")),
        FoodItem("Orange", "1 medium", NutritionRange(55, 85, 1.0, 2.0, 12.0, 20.0, 0.0, 1.0, 2.0, 4.0), listOf("orange")),
        FoodItem("Milk", "1 cup", NutritionRange(100, 160, 6.0, 9.0, 9.0, 13.0, 4.0, 8.0, 0.0, 0.5), listOf("milk")),
        FoodItem("Oats", "1 bowl", NutritionRange(180, 280, 6.0, 10.0, 28.0, 45.0, 4.0, 9.0, 4.0, 7.0), listOf("oats", "oatmeal"))
    )

    fun match(text: String): List<FoodItem> {
        val normalized = text.lowercase()
        return items.filter { item ->
            item.name.lowercase() in normalized || item.aliases.any { normalized.contains(it) }
        }.distinctBy { it.name }
    }

    fun combine(items: List<FoodItem>): NutritionRange {
        if (items.isEmpty()) return NutritionRange(0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        return NutritionRange(
            items.sumOf { it.nutrition.kcalMin },
            items.sumOf { it.nutrition.kcalMax },
            items.sumOf { it.nutrition.proteinMin },
            items.sumOf { it.nutrition.proteinMax },
            items.sumOf { it.nutrition.carbsMin },
            items.sumOf { it.nutrition.carbsMax },
            items.sumOf { it.nutrition.fatMin },
            items.sumOf { it.nutrition.fatMax },
            items.sumOf { it.nutrition.fibreMin },
            items.sumOf { it.nutrition.fibreMax }
        )
    }
}

class FoodVisionAnalyzer {
    private val inferenceEngine = com.maisor.caleap.food.ai.FoodVisionInferenceEngine()

    fun analyze(context: Context, bitmap: Bitmap, onResult: (FoodAnalysis) -> Unit) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val bytes = java.io.ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                out.toByteArray()
            }

            val result = inferenceEngine.run(bytes, "Photo")
            val ranked = com.maisor.caleap.food.ai.FoodCandidateFusionEngine().rank(
                vision = result.candidates.map {
                    com.maisor.caleap.food.ai.FoodVisionCandidate(
                        label = it.label,
                        confidence = it.confidence,
                        modelId = it.modelId,
                        modelVersion = it.modelVersion,
                        license = it.license
                    )
                }
            )

            val catalogCandidates = com.maisor.caleap.food.ai.FoodLabelFoodMapper.map(
                ranked,
                IndianFoodCatalog.items
            )

            val decision = com.maisor.caleap.food.ai.FoodRecognitionPolicy().decide(
                ranked,
                result.status
            )

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(
                    FoodAnalysis(
                        source = "Photo",
                        labels = result.candidates.take(6).map { it.label },
                        candidates = catalogCandidates,
                        confidence = ranked.firstOrNull()?.confidence?.times(100f)?.roundToInt(),
                        needsConfirmation = decision.state != com.maisor.caleap.food.ai.RecognitionState.CONFIDENT
                    )
                )
            }
        }
    }
}
