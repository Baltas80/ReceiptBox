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
    private const val MAX_BACKUP_BYTES = 32 * 1024 * 1024
    private const val MAX_ENTRY_BYTES = 10 * 1024 * 1024
    private const val MAX_TOTAL_ENTRY_BYTES = 32 * 1024 * 1024
    private const val MAX_ENTRIES = 10_000

    data class RestoreResult(
        val receipts: List<Receipt>,
        val restoredImages: Int,
        val missingImages: Int,
        private val createdImagePaths: List<String> = emptyList()
    ) {
        /** Removes only images created by this restore operation, useful when the user cancels confirmation. */
        fun cleanupImages() {
            createdImagePaths.forEach { path -> File(path).delete() }
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

    /** Restores metadata and images into the supplied private receipt directory. */
    fun import(zipBytes: ByteArray, imageDirectory: File): RestoreResult {
        require(zipBytes.isNotEmpty()) { "La copia está vacía" }
        require(zipBytes.size <= MAX_BACKUP_BYTES) { "La copia supera el tamaño máximo permitido" }
        val entries = linkedMapOf<String, ByteArray>()
        var totalBytes = 0L
        var entryCount = 0
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                require(entryCount <= MAX_ENTRIES) { "La copia contiene demasiadas entradas" }
                require(!entry.isDirectory) { "La copia contiene una carpeta no válida" }
                require(isSafeEntry(entry.name)) { "Entrada de copia no válida" }
                val bytes = readEntryLimited(zip, MAX_ENTRY_BYTES) { read ->
                    totalBytes += read
                    require(totalBytes <= MAX_TOTAL_ENTRY_BYTES) { "La copia descomprimida supera el tamaño máximo permitido" }
                }
                require(entries.put(entry.name, bytes) == null) { "Entrada duplicada: ${entry.name}" }
                zip.closeEntry()
            }
        }
        val metadataBytes = entries[METADATA_ENTRY] ?: error("Falta backup.json")
        val root = JSONObject(String(metadataBytes, Charsets.UTF_8))
        require(root.optInt("version", -1) == VERSION) { "Versión de copia no compatible" }
        val items = root.optJSONArray("receipts") ?: JSONArray()
        imageDirectory.mkdirs()

        val parsed = buildList(items.length()) {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val imageEntry = item.optString("imageEntry")
                if (imageEntry.isNotBlank()) require(isSafeImageEntry(imageEntry)) { "Referencia de imagen no válida" }
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
                    imageBytes = imageEntry.takeIf { it.isNotBlank() }?.let { entries[it] },
                    hasImageReference = imageEntry.isNotBlank()
                ))
            }
        }

        val createdPaths = mutableListOf<String>()
        val restored = mutableListOf<Receipt>()
        var restoredImages = 0
        try {
            parsed.forEach { item ->
                val imagePath = item.imageBytes?.let { bytes ->
                    val target = File(imageDirectory, "restored_${UUID.randomUUID()}.jpg")
                    FileOutputStream(target).use { it.write(bytes) }
                    createdPaths += target.absolutePath
                    restoredImages++
                    target.absolutePath
                }.orEmpty()
                restored += item.receipt.copy(imagePath = imagePath)
            }
        } catch (error: Throwable) {
            createdPaths.forEach { File(it).delete() }
            throw error
        }
        val expectedImages = parsed.count { it.hasImageReference }
        return RestoreResult(restored, restoredImages, (expectedImages - restoredImages).coerceAtLeast(0), createdPaths.toList())
    }

    private fun readEntryLimited(input: ZipInputStream, maxBytes: Int, onRead: (Int) -> Unit): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= maxBytes) { "Una entrada de la copia supera el tamaño máximo permitido" }
            output.write(buffer, 0, count)
            onRead(count)
        }
        return output.toByteArray()
    }

    private data class ParsedReceipt(
        val receipt: Receipt,
        val imageBytes: ByteArray?,
        val hasImageReference: Boolean
    )

    private fun isSafeEntry(name: String): Boolean = name == METADATA_ENTRY || (name.startsWith(IMAGE_PREFIX) && isSafeImageEntry(name))

    private fun isSafeImageEntry(name: String): Boolean {
        if (!name.startsWith(IMAGE_PREFIX) || name.contains("\\") || name.contains("..")) return false
        val fileName = name.removePrefix(IMAGE_PREFIX)
        return fileName.matches(Regex("\\d+\\.jpg"))
    }

    private fun JSONObject.optNullableDouble(name: String): Double? =
        if (!has(name) || isNull(name)) null else optDouble(name).takeUnless { it.isNaN() }
}
