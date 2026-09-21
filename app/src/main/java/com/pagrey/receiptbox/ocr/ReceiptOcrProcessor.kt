package com.pagrey.receiptbox.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Receipt analysis entry point.
 * Deliberately contains no local OCR: the receipt image goes directly to the
 * configured multimodal vision model.
 */
class ReceiptOcrProcessor {

    suspend fun process(bitmap: Bitmap, rotationDegrees: Int = 0): Result<OcrResult> {
        val prepared = if (rotationDegrees == 0) bitmap else rotate(bitmap, rotationDegrees)
        return OnlineReceiptAi.analyze(prepared).map { ai ->
            OcrResult(
                rawText = "",
                parsed = ai,
                analysisSource = OcrAnalysisSource.ONLINE_AI,
                onlineFailure = null
            )
        }
    }

    suspend fun process(context: Context, file: File): Result<OcrResult> {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: return Result.failure(IllegalStateException("No se pudo abrir la imagen del ticket"))
        return process(bitmap)
    }

    fun close() = Unit

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}

enum class OcrAnalysisSource {
    LOCAL,
    ONLINE_AI,
    LOCAL_FALLBACK
}

data class OcrResult(
    val rawText: String,
    val parsed: ParsedReceipt,
    val analysisSource: OcrAnalysisSource = OcrAnalysisSource.ONLINE_AI,
    val onlineFailure: String? = null
)
