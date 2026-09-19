package com.maisor.caleap.openai

/**
 * Curated open-model registry for CaLeap.
 * Keep model, dataset and license provenance together before shipping a model binary.
 */
data class OpenFoodModel(
    val id: String,
    val displayName: String,
    val sourceUrl: String,
    val license: String,
    val task: String,
    val notes: String,
    val mobileReady: Boolean = false
)

object OpenFoodModelRegistry {
    val models = listOf(
        OpenFoodModel(
            id = "dima806/indian_food_image_detection",
            displayName = "Indian Food ViT — 80 classes",
            sourceUrl = "https://huggingface.co/dima806/indian_food_image_detection",
            license = "Apache-2.0",
            task = "Indian food image classification",
            notes = "Model card reports 80 Indian-food classes and validation accuracy; model is ~85.9M parameters.",
            mobileReady = false
        ),
        OpenFoodModel(
            id = "therealcyberlord/vit-indian-food",
            displayName = "ViT Indian Food",
            sourceUrl = "https://huggingface.co/therealcyberlord/vit-indian-food",
            license = "Apache-2.0",
            task = "Indian food image classification",
            notes = "Apache-2.0 model; large safetensors checkpoint, so it is kept as an evaluation candidate rather than bundled into the APK.",
            mobileReady = false
        ),
        OpenFoodModel(
            id = "Subhash5/indian-food-classifier",
            displayName = "Indian Food Classifier",
            sourceUrl = "https://huggingface.co/Subhash5/indian-food-classifier",
            license = "MIT",
            task = "Indian food image classification",
            notes = "20-class classifier. Model card lists common Indian dishes and several non-Indian foods.",
            mobileReady = false
        )
    )
}
