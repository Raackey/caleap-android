package com.maisor.caleap.food.ai

data class ModelValidationReport(
    val modelId: String,
    val modelVersion: String,
    val artifactSha256: String,
    val licenseVerified: Boolean,
    val tensorContractVerified: Boolean,
    val preprocessingVerified: Boolean,
    val labelMapVerified: Boolean,
    val benchmarkImages: Int,
    val benchmarkTop1Accuracy: Float?,
    val benchmarkTop5Accuracy: Float?,
    val notes: String
) {
    val productionEligible: Boolean
        get() = licenseVerified &&
            tensorContractVerified &&
            preprocessingVerified &&
            labelMapVerified &&
            benchmarkImages > 0 &&
            benchmarkTop1Accuracy != null
}
