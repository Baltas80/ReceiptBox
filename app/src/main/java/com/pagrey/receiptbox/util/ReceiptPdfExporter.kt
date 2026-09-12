package com.pagrey.receiptbox.util

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.pagrey.receiptbox.data.Receipt
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale

object ReceiptPdfExporter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val LEFT = 40f
    private const val TOP = 48f
    private const val LINE = 22f
    private const val BOTTOM = 48f
    private val euro = NumberFormat.getCurrencyInstance(Locale("es", "ES"))

    fun export(receipts: List<Receipt>): ByteArray {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = TOP

        fun drawText(value: String, size: Float = 11f, bold: Boolean = false) {
            paint.textSize = size
            paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            canvas.drawText(value.take(90), LEFT, y, paint)
            y += LINE
        }

        fun newPage() {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = TOP
        }

        drawText("ReceiptBox", 20f, true)
        drawText("Exportación de tickets", 12f)
        drawText("Tickets: ${receipts.size}")
        y += 10f

        receipts.forEachIndexed { index, receipt ->
            if (y + 7 * LINE > PAGE_HEIGHT - BOTTOM) newPage()
            drawText("Ticket ${index + 1}", 13f, true)
            drawText("Comercio: ${receipt.merchant.ifBlank { "—" }}")
            drawText("Fecha: ${receipt.date.ifBlank { "—" }}")
            drawText("Total: ${receipt.total?.let(euro::format) ?: "—"}")
            drawText("IVA: ${receipt.tax?.let(euro::format) ?: "—"}")
            drawText("Número: ${receipt.receiptNumber.ifBlank { "—" }}")
            drawText("Categoría: ${receipt.category.ifBlank { "Otros" }}")
            y += 8f
        }

        document.finishPage(page)
        val output = ByteArrayOutputStream()
        document.writeTo(output)
        document.close()
        return output.toByteArray()
    }
}
