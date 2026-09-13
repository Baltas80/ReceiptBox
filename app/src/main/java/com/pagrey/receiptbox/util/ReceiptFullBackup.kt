package com.pagrey.receiptbox.util

import com.pagrey.receiptbox.data.Receipt
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Portable backup containing receipt metadata and any available receipt images. */
object ReceiptFullBackup {
    private const val VERSION = 1
    private const val METADATA_ENTRY = "backup.json"
    private const val IMAGE_PREFIX = "images/"

    data class RestoreResult(
        val receipts: List<Receipt>,
        val restoredImages: Int,
        val missingImages: Int,
        val stagingDirectory: File
    ) {
        fun cleanupStaging() {
            stagingDirectory.deleteRecursively()
        }
    }

    fun export(receipts: List<Receipt>, imageLoader: (String) -> ByteArray?): ByteArray {
        val metadata = JSONArray()
        val images = mutableListOf<Pair<String, ByteArray>>()
        receipts.forEachIndexed { index, receipt ->
            val imageBytes = receipt.imagePath.takeIf { it.isNotBlank() }?.let(imageLoader)
            val imageEntry = if (imageBytes != null) {
                val entryName = "$IMAGE_PREFIX$index.jpg"
                images += entryName to imageBytes
                entryName
            } else ""
            metadata.put(JSONObject().apply {
                put("merchant", receipt.merchant)
                put("date", receipt.date)
                put("total", receipt.total ?: JSONObject.NULL)
                put("tax", receipt.tax ?: JSONObject.NULL)
                put("receiptNumber", receipt.receiptNumber)
                put("category", receipt.category)
                put("imageEntry", imageEntry)
                put("rawText", receipt.rawText)
                put("createdAt", receipt.createdAt)
            })
        }
        val root = JSONObject().put("version", VERSION).put("receipts", metadata)
        return ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry(METADATA_ENTRY))
                zip.write(root.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                images.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            output.toByteArray()
        }
    }

    /** Reads and stages a backup. No files are written to the live receipt directory. */
    fun import(zipBytes: ByteArray, stagingDirectory: File): RestoreResult {
        require(zipBytes.isNotEmpty()) { "La copia está vacía" }
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory) { "La copia contiene una carpeta no válida" }
                require(isSafeEntry(entry.name)) { "Entrada de copia no válida" }
                require(entries.put(entry.name, zip.readBytes()) == null) { "Entrada duplicada: ${entry.name}" }
                zip.closeEntry()
            }
        }
        val metadataBytes = entries[METADATA_ENTRY] ?: error("Falta backup.json")
        val root = JSONObject(String(metadataBytes, Charsets.UTF_8))
        require(root.optInt("version", -1) == VERSION) { "Versión de copia no compatible" }
        val items = root.optJSONArray("receipts") ?: JSONArray()
        val parsed = buildList(items.length()) {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val imageEntry = item.optString("imageEntry")
                if (imageEntry.isNotBlank()) require(isSafeImageEntry(imageEntry)) { "Referencia de imagen no válida" }
                val imageBytes = imageEntry.takeIf { it.isNotBlank() }?.let { entries[it] }
                add(ParsedReceipt(
                    receipt = Receipt(
                        merchant = item.optString("merchant"),
                        date = item.optString("date"),
                        total = item.optNullableDouble("total"),
                        tax = item.optNullableDouble("tax"),
                        receiptNumber = item.optString("receiptNumber"),
                        category = item.optString("category", "Otros"),
                        imagePath = "",
                        rawText = item.optString("rawText"),
                        createdAt = item.optLong("createdAt", System.currentTimeMillis())
                    ),
                    imageBytes = imageBytes
                ))
            }
        }

        stagingDirectory.deleteRecursively()
        stagingDirectory.mkdirs()
        val restored = mutableListOf<Receipt>()
        var restoredImages = 0
        try {
            parsed.forEachIndexed { index, item ->
                val imagePath = item.imageBytes?.let { bytes ->
                    val target = File(stagingDirectory, "restored_$index.jpg")
                    FileOutputStream(target).use { it.write(bytes) }
                    restoredImages++
                    target.absolutePath
                }.orEmpty()
                restored += item.receipt.copy(imagePath = imagePath)
            }
        } catch (error: Throwable) {
            stagingDirectory.deleteRecursively()
            throw error
        }
        val expectedImages = parsed.count { it.receipt.imagePath.isNotBlank() || it.imageBytes != null }
        return RestoreResult(restored, restoredImages, (expectedImages - restoredImages).coerceAtLeast(0), stagingDirectory)
    }

    /** Moves staged images into the app-private receipt directory and returns final receipt paths. */
    fun commitRestore(result: RestoreResult, imageDirectory: File): List<Receipt> {
        imageDirectory.mkdirs()
        return try {
            result.receipts.map { receipt ->
                if (receipt.imagePath.isBlank()) receipt else {
                    val source = File(receipt.imagePath)
                    require(source.parentFile?.canonicalFile == result.stagingDirectory.canonicalFile) { "Archivo de imagen no válido" }
                    val target = File(imageDirectory, "restored_${UUID.randomUUID()}.jpg")
                    require(source.renameTo(target)) { "No se pudo guardar la imagen restaurada" }
                    receipt.copy(imagePath = target.absolutePath)
                }
            }
        } catch (error: Throwable) {
            result.cleanupStaging()
            throw error
        } finally {
            result.stagingDirectory.deleteRecursively()
        }
    }

    private data class ParsedReceipt(val receipt: Receipt, val imageBytes: ByteArray?)

    private fun isSafeEntry(name: String): Boolean = name == METADATA_ENTRY || (name.startsWith(IMAGE_PREFIX) && isSafeImageEntry(name))

    private fun isSafeImageEntry(name: String): Boolean {
        if (!name.startsWith(IMAGE_PREFIX) || name.contains("\\") || name.contains("..")) return false
        val fileName = name.removePrefix(IMAGE_PREFIX)
        return fileName.matches(Regex("\\d+\\.jpg"))
    }

    private fun JSONObject.optNullableDouble(name: String): Double? =
        if (!has(name) || isNull(name)) null else optDouble(name).takeUnless { it.isNaN() }
}
