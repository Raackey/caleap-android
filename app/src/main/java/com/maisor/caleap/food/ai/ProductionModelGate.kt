package com.maisor.caleap.food.ai

data class ProductionModelGate(
    val artifactPresent: Boolean,
    val checksumVerified: Boolean,
    val licenseVerified: Boolean,
    val tensorContractVerified: Boolean,
    val preprocessingVerified: Boolean,
    val labelMapVerified: Boolean,
    val benchmarkCompleted: Boolean,
    val top1Accuracy: Float?,
    val top5Accuracy: Float?,
    val abstentionRate: Float?
) {
    val eligible: Boolean
        get() =
            artifactPresent &&
            checksumVerified &&
            licenseVerified &&
            tensorContractVerified &&
            preprocessingVerified &&
            labelMapVerified &&
            benchmarkCompleted &&
            top1Accuracy != null &&
            top5Accuracy != null
}

object ProductionModelGatePolicy {
    fun evaluate(gate: ProductionModelGate): String =
        when {
            !gate.artifactPresent -> "BLOCKED: model artifact is not bundled."
            !gate.checksumVerified -> "BLOCKED: artifact checksum is not verified."
            !gate.licenseVerified -> "BLOCKED: license is not verified."
            !gate.tensorContractVerified -> "BLOCKED: tensor contract is not verified."
            !gate.preprocessingVerified -> "BLOCKED: preprocessing is not verified."
            !gate.labelMapVerified -> "BLOCKED: label map is not verified."
            !gate.benchmarkCompleted -> "BLOCKED: CaLeap benchmark is incomplete."
            gate.top1Accuracy == null || gate.top5Accuracy == null ->
                "BLOCKED: benchmark metrics are missing."
            else -> "ELIGIBLE: model passed the structural production gate."
        }
}
