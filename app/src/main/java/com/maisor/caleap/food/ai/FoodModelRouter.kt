package com.maisor.caleap.food.ai

class FoodModelRouter(
    private val productionAdapter: FoodVisionModelAdapter?,
    private val fallbackAdapter: FoodVisionModelAdapter
) {
    fun activeAdapter(productionGate: ProductionModelGate): FoodVisionModelAdapter {
        return if (productionGate.eligible && productionAdapter != null) {
            productionAdapter
        } else {
            fallbackAdapter
        }
    }
}
