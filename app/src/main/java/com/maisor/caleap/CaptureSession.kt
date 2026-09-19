package com.maisor.caleap

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

/** V18: one lifecycle for every SHOW CALEAP input source. */
enum class CaptureStage { IDLE, CAPTURING, PREPARING, UNDERSTANDING, REVIEW, SAVED, ERROR }

data class CaptureSession(
    val id: Long,
    val source: CaptureSource? = null,
    val stage: CaptureStage = CaptureStage.IDLE,
    val rawText: String = "",
    val visualLabels: List<String> = emptyList(),
    val confidence: Int? = null,
    val coreResult: CoreFoodResult? = null,
    val error: String? = null
)

object CaptureSessionCoordinator {
    fun begin(): CaptureSession = CaptureSession(System.currentTimeMillis(), stage = CaptureStage.CAPTURING)

    fun voice(session: CaptureSession, text: String): CaptureSession {
        val result = CaLeapCoreCaptureEngine.fromVoice(text)
        return session.copy(
            source = CaptureSource.VOICE,
            stage = stageFor(result),
            rawText = text,
            confidence = result.observation.confidence,
            coreResult = result,
            error = (result.uiState as? ShowCaLeapUiState.Error)?.message
        )
    }

    fun vision(session: CaptureSession, labels: List<String>, confidence: Int?): CaptureSession {
        val result = CaLeapCoreCaptureEngine.fromVision(labels, confidence)
        return session.copy(
            source = CaptureSource.CAMERA,
            stage = stageFor(result),
            visualLabels = labels,
            confidence = confidence,
            coreResult = result,
            error = (result.uiState as? ShowCaLeapUiState.Error)?.message
        )
    }

    fun gallery(session: CaptureSession, labels: List<String>, confidence: Int?): CaptureSession =
        vision(session, labels, confidence).copy(source = CaptureSource.GALLERY)

    fun preparing(session: CaptureSession, source: CaptureSource): CaptureSession =
        session.copy(source = source, stage = CaptureStage.PREPARING, error = null)

    fun understanding(session: CaptureSession): CaptureSession =
        session.copy(stage = CaptureStage.UNDERSTANDING)

    fun review(session: CaptureSession): CaptureSession =
        session.copy(stage = CaptureStage.REVIEW)

    fun saved(session: CaptureSession): CaptureSession =
        session.copy(stage = CaptureStage.SAVED)

    private fun stageFor(result: CoreFoodResult): CaptureStage = when (result.uiState) {
        is ShowCaLeapUiState.Error -> CaptureStage.ERROR
        else -> CaptureStage.UNDERSTANDING
    }
}

/** Converts a camera frame into a model-ready payload without coupling capture to a model. */
fun Bitmap.toModelJpeg(maxDimension: Int = 1280, quality: Int = 88): ByteArray {
    val scale = minOf(1f, maxDimension.toFloat() / maxOf(width, height).toFloat())
    val target = if (scale < 1f) Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true) else this
    return ByteArrayOutputStream().use { out ->
        target.compress(Bitmap.CompressFormat.JPEG, quality, out)
        out.toByteArray()
    }
}
