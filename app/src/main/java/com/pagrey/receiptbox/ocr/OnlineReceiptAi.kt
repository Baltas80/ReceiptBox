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

/** Direct multimodal receipt analysis. No local OCR is used. */
object OnlineReceiptAi {
    private val fallbackModels = listOf(
        BuildConfig.GEMINI_MODEL,
        "gemini-3.7-flash",
        "gemini-3.6-flash",
        "gemini-3.5-flash"
    ).distinct()

    val isConfigured: Boolean
        get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    suspend fun analyze(bitmap: Bitmap): Result<ParsedReceipt> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("IA online no configurada"))

        runCatching {
            var lastFailure: Throwable? = null
            for (model in fallbackModels) {
                var attempt = 0
                while (attempt < 2) {
                    val result = request(model, bitmap)
                    if (result.isSuccess) return@runCatching result.getOrThrow()
                    lastFailure = result.exceptionOrNull()
                    if (lastFailure !is ApiException || (lastFailure.code != 429 && lastFailure.code != 503) || attempt == 1) break
                    attempt++
                    delay(1_200L * attempt)
                }
            }
            error(lastFailure?.message ?: "Servicio de IA no disponible")
        }
    }

    private fun request(model: String, bitmap: Bitmap): Result<ParsedReceipt> {
        var connection: HttpURLConnection? = null
        return runCatching {
            val image = bitmapToBase64(bitmap)

            val prompt = """
                Analiza directamente esta fotografía de un ticket de compra español.
                NO uses OCR externo ni texto auxiliar: la imagen es la única fuente.
                Devuelve SOLO JSON válido, sin markdown.
                Campos exactos: merchant (string), date (string DD/MM/YYYY), total (number), tax (number|null), receiptNumber (string).
                El campo total DEBE ser el importe impreso en la línea TOTAL o TOTAL A PAGAR.
                NO confundas total con efectivo entregado, cambio, subtotal, precio de un artículo, base imponible o código de barras.
                Si aparece una tabla de impuestos, tax es la suma de las CUOTAS.
                Si un campo no puede determinarse con seguridad, usa string vacío o null.
                Lee la fotografía completa, incluyendo la parte inferior del ticket.
                Antes de responder, verifica visualmente que el total elegido corresponde a la línea TOTAL.
            """.trimIndent()

            val parts = JSONArray()
                .put(JSONObject().put("inline_data", JSONObject()
                    .put("mime_type", "image/jpeg")
                    .put("data", image)))
                .put(JSONObject().put("text", prompt))

            val body = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", parts)))
                .put("generationConfig", JSONObject()
                    .put("responseMimeType", "application/json"))
                .toString()

            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 60_000
                doOutput = true
                useCaches = false
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            }

            try {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (responseCode !in 200..299) {
                    throw ApiException(responseCode, responseText.take(500))
                }

                val root = JSONObject(responseText)
                val candidates = root.optJSONArray("candidates")
                    ?: error("Gemini: respuesta sin candidates")
                if (candidates.length() == 0) error("Gemini: respuesta sin candidatos")
                val partsResponse = candidates.getJSONObject(0)
                    .optJSONObject("content")?.optJSONArray("parts")
                    ?: error("Gemini: respuesta sin contenido")
                if (partsResponse.length() == 0) error("Gemini: respuesta sin partes")

                val text = partsResponse.getJSONObject(0).optString("text")
                if (text.isBlank()) error("Gemini: respuesta vacía")

                val clean = text.trim()
                    .removePrefix("```")
                    .removePrefix("json")
                    .removeSuffix("```")
                    .trim()
                val json = JSONObject(clean)
                val total = if (json.isNull("total")) null else json.optDouble("total", Double.NaN).takeUnless { it.isNaN() }
                if (total == null || total <= 0.0) error("Gemini: total no válido")

                ParsedReceipt(
                    merchant = json.optString("merchant"),
                    date = json.optString("date"),
                    total = total,
                    tax = if (json.isNull("tax")) null else json.optDouble("tax", Double.NaN).takeUnless { it.isNaN() },
                    receiptNumber = json.optString("receiptNumber")
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    private class ApiException(val code: Int, body: String) : IllegalStateException("Gemini HTTP $code: $body")

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
}
