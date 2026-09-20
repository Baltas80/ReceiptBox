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
            applyOnlineSecondPass(localResult, bitmap)
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
            if (localResult.isFailure || !OnlineReceiptAi.isConfigured) {
                localResult.map { it.copy(analysisSource = if (OnlineReceiptAi.isConfigured) OcrAnalysisSource.LOCAL_FALLBACK else OcrAnalysisSource.LOCAL) }
            } else {
                val local = localResult.getOrThrow()
                val bitmap = runCatching { android.graphics.BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
                    ?: return@let Result.success(local.copy(analysisSource = OcrAnalysisSource.LOCAL_FALLBACK, onlineFailure = "No se pudo preparar la imagen para la IA online"))
                applyOnlineSecondPass(Result.success(local), bitmap)
            }
        }

    private suspend fun applyOnlineSecondPass(localResult: Result<OcrResult>, bitmap: Bitmap): Result<OcrResult> {
        if (localResult.isFailure) return localResult
        if (!OnlineReceiptAi.isConfigured) {
            return localResult.map { it.copy(analysisSource = OcrAnalysisSource.LOCAL) }
        }

        val local = localResult.getOrThrow()
        val online = OnlineReceiptAi.analyze(bitmap, local.rawText)
        val ai = online.getOrNull()
        if (ai != null && isPlausible(ai, local.parsed)) {
            return Result.success(
                local.copy(
                    parsed = ai,
                    analysisSource = OcrAnalysisSource.ONLINE_AI,
                    onlineFailure = null
                )
            )
        }

        val reason = when {
            online.isFailure -> online.exceptionOrNull()?.javaClass?.simpleName ?: "error"
            ai == null -> "respuesta vacía"
            else -> "resultado rechazado por validación"
        }
        return Result.success(
            local.copy(
                analysisSource = OcrAnalysisSource.LOCAL_FALLBACK,
                onlineFailure = reason
            )
        )
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

enum class OcrAnalysisSource {
    LOCAL,
    ONLINE_AI,
    LOCAL_FALLBACK
}

data class OcrResult(
    val rawText: String,
    val parsed: ParsedReceipt,
    val analysisSource: OcrAnalysisSource = OcrAnalysisSource.LOCAL,
    val onlineFailure: String? = null
)
