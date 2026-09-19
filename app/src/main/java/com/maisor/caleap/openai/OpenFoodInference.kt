package com.maisor.caleap.openai

import com.maisor.caleap.FoodItem

/**
 * Result returned by an open-model adapter. A model can abstain when confidence is low.
 */
data class OpenFoodCandidate(
    val label: String,
    val confidence: Float,
    val matchedFood: FoodItem? = null,
    val sourceModel: String
)

data class OpenFoodInference(
    val candidates: List<OpenFoodCandidate>,
    val modelId: String,
    val usedOnDevice: Boolean
)

interface OpenFoodModelAdapter {
    suspend fun analyze(imageBytes: ByteArray): OpenFoodInference
}

/**
 * Safe default until a verified, mobile-converted model artifact is benchmarked.
 * It prevents CaLeap from pretending that an unbundled Hugging Face checkpoint is
 * running on the phone.
 */
class NoBundledOpenModelAdapter : OpenFoodModelAdapter {
    override suspend fun analyze(imageBytes: ByteArray): OpenFoodInference =
        OpenFoodInference(
            candidates = emptyList(),
            modelId = "none",
            usedOnDevice = false
        )
}
