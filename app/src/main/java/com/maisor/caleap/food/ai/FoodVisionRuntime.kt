package com.maisor.caleap.food.ai

import android.content.Context

class FoodVisionRuntime(
    private val context: Context
) {
    fun createVerifiedModelOrFallback(): FoodVisionModelAdapter {
        val registry = VerifiedFoodModelRegistry(context)
        val verified = registry.findBundledVerifiedModel()
        return verified ?: UnavailableFoodVisionModelAdapter()
    }
}

class VerifiedFoodModelRegistry(
    private val context: Context
) {
    fun findBundledVerifiedModel(): FoodVisionModelAdapter? {
        // V22 deliberately returns null until a model artifact has passed
        // checksum/license/input-output validation and is listed as bundled.
        return null
    }
}
