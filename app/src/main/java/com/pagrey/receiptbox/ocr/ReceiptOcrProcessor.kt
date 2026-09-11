package com.pagrey.receiptbox.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ReceiptOcrProcessor {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun process(bitmap: Bitmap, rotationDegrees: Int = 0): Result<OcrResult> =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(InputImage.fromBitmap(bitmap, rotationDegrees))
                .addOnSuccessListener { result ->
                    continuation.resume(Result.success(OcrResult(result.text, ReceiptParser.parse(result.text))))
                }
                .addOnFailureListener { error ->
                    continuation.resume(Result.failure(error))
                }
        }

    suspend fun process(context: Context, file: java.io.File): Result<OcrResult> =
        suspendCancellableCoroutine { continuation ->
            runCatching { InputImage.fromFilePath(context, Uri.fromFile(file)) }
                .onSuccess { image ->
                    recognizer.process(image)
                        .addOnSuccessListener { result ->
                            continuation.resume(Result.success(OcrResult(result.text, ReceiptParser.parse(result.text))))
                        }
                        .addOnFailureListener { error -> continuation.resume(Result.failure(error)) }
                }
                .onFailure { error -> continuation.resume(Result.failure(error)) }
        }

    fun close() = recognizer.close()
}

data class OcrResult(val rawText: String, val parsed: ParsedReceipt)
