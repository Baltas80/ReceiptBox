package com.pagrey.receiptbox.ocr

import android.graphics.Bitmap
import android.util.Base64
import com.pagrey.receiptbox.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Optional online second-pass receipt analysis using Gemini multimodal vision. */
object OnlineReceiptAi {
    val isConfigured: Boolean
        get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    suspend fun analyze(bitmap: Bitmap, ocrText: String): Result<ParsedReceipt> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("IA online no configurada"))

        runCatching {
            val image = bitmapToBase64(bitmap)
            val prompt = """
                Analiza esta fotografía de un ticket de compra español. Devuelve SOLO JSON válido, sin markdown.
                No inventes datos. Usa la imagen como fuente principal y el OCR como apoyo.
                Campos exactos: merchant (string), date (string DD/MM/YYYY), total (number), tax (number|null), receiptNumber (string).
                El total debe ser el importe de la línea TOTAL/TOTAL A PAGAR, no efectivo, cambio, subtotales ni artículos.
                Si hay tabla IMPUESTOS con columnas porcentaje, BASE y CUOTA, tax es la suma de las CUOTAS.
                Si un campo no puede determinarse con seguridad, usa string vacío o null.
                Valida que el total sea coherente con los importes del ticket antes de devolverlo.

                OCR auxiliar:
                $ocrText
            """.trimIndent()

            val imagePart = JSONObject()
                .put("inline_data", JSONObject()
                    .put("mime_type", "image/jpeg")
                    .put("data", image))
            val textPart = JSONObject().put("text", prompt)
            val parts = JSONArray().put(imagePart).put(textPart)
            val content = JSONObject().put("parts", parts)
            val contents = JSONArray().put(content)
            val generationConfig = JSONObject().put("responseMimeType", "application/json")
            val body = JSONObject()
                .put("contents", contents)
                .put("generationConfig", generationConfig)
                .toString()

            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/${BuildConfig.GEMINI_MODEL}:generateContent")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 40_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            }

            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            val responseText = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) error("Gemini HTTP $responseCode: $responseText")

            val root = JSONObject(responseText)
            val text = root.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
            val clean = text.trim()
                .removePrefix("```")
                .removePrefix("json")
                .removeSuffix("```")
                .trim()
            val json = JSONObject(clean)

            ParsedReceipt(
                merchant = json.optString("merchant"),
                date = json.optString("date"),
                total = if (json.isNull("total")) null else json.optDouble("total", Double.NaN).takeUnless { it.isNaN() },
                tax = if (json.isNull("tax")) null else json.optDouble("tax", Double.NaN).takeUnless { it.isNaN() },
                receiptNumber = json.optString("receiptNumber")
            )
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
}
