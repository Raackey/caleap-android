package com.maisor.caleap

import android.graphics.Bitmap

/**
 * V17 core orchestration:
 * one input contract for voice, photo labels and future model inference.
 *
 * The engine separates perception from food identity, variants, portions and nutrition.
 * It never invents source-backed nutrition when evidence is unavailable.
 */
enum class CaptureSource { VOICE, CAMERA, GALLERY }

data class CaptureObservation(
    val source: CaptureSource,
    val query: String = "",
    val labels: List<String> = emptyList(),
    val confidence: Int? = null
)

data class CoreFoodResult(
    val observation: CaptureObservation,
    val uiState: ShowCaLeapUiState,
    val matchedLocalFoods: List<FoodItem> = emptyList(),
    val provenance: String
)

object CaLeapCoreCaptureEngine {

    fun fromVoice(text: String, countryIso2: String? = null, cuisine: String? = null): CoreFoodResult {
        val cleaned = normalize(text)
        val local = IndianFoodCatalog.match(cleaned)
        val state = resolve(cleaned, countryIso2, cuisine)
        return CoreFoodResult(
            CaptureObservation(CaptureSource.VOICE, query = cleaned),
            state,
            local,
            if (local.isNotEmpty()) "Voice → local food vocabulary → global food graph" else "Voice → global food graph"
        )
    }

    fun fromVision(labels: List<String>, confidence: Int?, countryIso2: String? = null): CoreFoodResult {
        val query = bestFoodQuery(labels)
        val local = if (query.isBlank()) emptyList() else IndianFoodCatalog.match(query)
        val state = resolve(query, countryIso2, null)
        return CoreFoodResult(
            CaptureObservation(CaptureSource.CAMERA, query = query, labels = labels, confidence = confidence),
            state,
            local,
            "Vision labels → food identity/variant graph"
        )
    }

    private fun resolve(query: String, countryIso2: String?, cuisine: String?): ShowCaLeapUiState {
        if (query.isBlank()) {
            return ShowCaLeapUiState.Error(
                "CaLeap could not identify enough food information. Try a clearer photo or tell me the food name."
            )
        }
        return ShowCaLeapController.analyze(
            FoodCaptureRequest(query = query, countryIso2 = countryIso2, cuisine = cuisine)
        )
    }

    private fun normalize(text: String): String =
        text.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun bestFoodQuery(labels: List<String>): String {
        val preferred = labels.firstOrNull { label ->
            IndianFoodCatalog.items.any { item ->
                (listOf(item.name) + item.aliases).any { key ->
                    label.equals(key, ignoreCase = true) ||
                        label.contains(key, ignoreCase = true) ||
                        key.contains(label, ignoreCase = true)
                }
            }
        }
        return preferred ?: labels.firstOrNull().orEmpty()
    }
}
