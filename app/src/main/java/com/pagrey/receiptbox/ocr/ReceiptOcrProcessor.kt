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
        }.let { localResult ->
            if (localResult.isFailure || !OnlineReceiptAi.isConfigured) return@let localResult
            val local = localResult.getOrThrow()
            val ai = OnlineReceiptAi.analyze(bitmap, local.rawText).getOrNull()
            if (ai == null || !isPlausible(ai, local.parsed)) localResult
            else Result.success(local.copy(parsed = ai))
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
        }.let { localResult ->
            if (localResult.isFailure || !OnlineReceiptAi.isConfigured) return@let localResult
            val local = localResult.getOrThrow()
            val bitmap = runCatching { android.graphics.BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
                ?: return@let localResult
            val ai = OnlineReceiptAi.analyze(bitmap, local.rawText).getOrNull()
            if (ai == null || !isPlausible(ai, local.parsed)) localResult
            else Result.success(local.copy(parsed = ai))
        }

    private fun isPlausible(ai: ParsedReceipt, local: ParsedReceipt): Boolean {
        if (ai.merchant.trim().length < 2 || ai.date.isBlank()) return false
        val total = ai.total ?: return false
        if (total <= 0.0 || total > 100_000.0) return false
        if (local.total != null && local.total > 0.0 && total > local.total * 50.0 && local.total < 100.0) return false
        return true
    }

    fun close() = recognizer.close()
}

data class OcrResult(val rawText: String, val parsed: ParsedReceipt)
