package com.maisor.caleap.food.ai

data class CalibrationBucket(
    val lowerInclusive: Float,
    val upperExclusive: Float,
    val samples: Int,
    val correct: Int
) {
    val empiricalAccuracy: Float
        get() = if (samples == 0) 0f else correct.toFloat() / samples
}

class ConfidenceCalibration {
    fun bucket(
        confidence: Float,
        correct: Boolean,
        buckets: Int = 10
    ): Int {
        val c = confidence.coerceIn(0f, 0.999999f)
        return (c * buckets).toInt().coerceIn(0, buckets - 1)
    }
}
