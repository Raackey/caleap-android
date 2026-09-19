package com.maisor.caleap.food.ai

import android.content.Context

object FoodVisionActivation {

    fun create(context: Context): FoodVisionModelAdapter {
        return try {
            context.assets.open("food_model.tflite").use { }
            ZeyuaiEfficientNetFoodAdapter(context)
        } catch (_: Exception) {
            UnavailableFoodVisionModelAdapter()
        }
    }
}
