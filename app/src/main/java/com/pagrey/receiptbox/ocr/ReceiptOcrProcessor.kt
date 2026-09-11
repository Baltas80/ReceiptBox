package com.pagrey.receiptbox.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ReceiptOcrProcessor {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun process(bitmap: Bitmap): Result<OcrResult> =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { result ->
                    continuation.resume(Result.success(OcrResult(result.text, ReceiptParser.parse(result.text))))
                }
                .addOnFailureListener { error ->
                    continuation.resume(Result.failure(error))
                }
        }

    fun close() = recognizer.close()
}

data class OcrResult(val rawText: String, val parsed: ParsedReceipt)
