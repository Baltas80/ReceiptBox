package com.pagrey.receiptbox.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/** PocketScan-inspired local preprocessing: resize, contrast stretch and text emphasis. */
object PocketScanImageEnhancer {
    private const val MAX_DIMENSION = 2200

    fun enhanceToJpeg(context: Context, uri: Uri, output: File): Boolean = runCatching {
        val source = decode(context, uri) ?: return false
        val bitmap = resizeIfNeeded(source)
        if (bitmap !== source) source.recycle()
        try {
            val enhanced = enhance(bitmap)
            try {
                output.parentFile?.mkdirs()
                FileOutputStream(output).use { stream ->
                    if (!enhanced.compress(Bitmap.CompressFormat.JPEG, 94, stream)) return false
                }
                true
            } finally { enhanced.recycle() }
        } finally { bitmap.recycle() }
    }.getOrElse { false }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > MAX_DIMENSION || bounds.outHeight / sample > MAX_DIMENSION) sample *= 2
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            })
        }
    }

    private fun resizeIfNeeded(source: Bitmap): Bitmap {
        val max = maxOf(source.width, source.height)
        if (max <= MAX_DIMENSION) return source
        val scale = MAX_DIMENSION.toFloat() / max
        return Bitmap.createScaledBitmap(source, (source.width * scale).roundToInt().coerceAtLeast(1), (source.height * scale).roundToInt().coerceAtLeast(1), true)
    }

    private fun enhance(source: Bitmap): Bitmap {
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        val histogram = IntArray(256)
        for (pixel in pixels) histogram[luminance(pixel)]++
        val low = percentile(histogram, pixels.size, 0.02f)
        val high = percentile(histogram, pixels.size, 0.98f).coerceAtLeast(low + 1)
        for (i in pixels.indices) {
            val y = luminance(pixels[i])
            val stretched = ((y - low) * 255f / (high - low)).roundToInt().coerceIn(0, 255)
            val adjusted = when {
                stretched < 90 -> (stretched * 0.78f).roundToInt()
                stretched < 180 -> (stretched * 0.90f).roundToInt()
                else -> stretched
            }.coerceIn(0, 255)
            pixels[i] = Color.rgb(adjusted, adjusted, adjusted)
        }
        return Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888).also {
            it.setPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        }
    }

    private fun luminance(pixel: Int): Int = (0.299f * Color.red(pixel) + 0.587f * Color.green(pixel) + 0.114f * Color.blue(pixel)).roundToInt().coerceIn(0, 255)

    private fun percentile(histogram: IntArray, total: Int, fraction: Float): Int {
        val target = (total * fraction).roundToInt().coerceIn(1, total)
        var count = 0
        for (value in histogram.indices) {
            count += histogram[value]
            if (count >= target) return value
        }
        return 255
    }
}
