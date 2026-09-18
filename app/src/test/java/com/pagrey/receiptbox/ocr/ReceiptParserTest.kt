package com.pagrey.receiptbox.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptParserTest {
    @Test
    fun parsesTypicalSpanishReceipt() {
        val raw = """
            SUPERMERCADO EJEMPLO
            11/09/2026
            Factura: ABC123
            IVA: 2,50
            Total: 12,50
        """.trimIndent()
        val result = ReceiptParser.parse(raw)
        assertEquals("SUPERMERCADO EJEMPLO", result.merchant)
        assertEquals("11/09/2026", result.date)
        assertEquals(12.50, result.total!!, 0.001)
        assertEquals(2.50, result.tax!!, 0.001)
        assertEquals("", result.receiptNumber)
    }

    @Test
    fun detectsMerchantFromWebsiteWhenFirstOcrLineIsNoise() {
        val raw = """
            2,9%
            99
            2026/09/02
            TOTAL: 123,90
            www.familycash.es
            GRACIAS POR SU VISITA
        """.trimIndent()
        val result = ReceiptParser.parse(raw)
        assertEquals("Familycash", result.merchant)
        assertEquals(123.90, result.total!!, 0.001)
    }

    @Test
    fun parsesEuropeanThousandsSeparatorWithoutTruncatingDecimals() {
        val raw = """
            SUPERMERCADO EJEMPLO
            IVA: 234,56
            TOTAL A PAGAR: 1.234,56
        """.trimIndent()
        val result = ReceiptParser.parse(raw)
        assertEquals(1234.56, result.total!!, 0.001)
        assertEquals(234.56, result.tax!!, 0.001)
    }

    @Test
    fun parsesTaxLineWithRateAndAmountUsingLastAmount() {
        val raw = """
            PANADERIA EJEMPLO
            IVA 21% 2,10
            TOTAL 12,10
        """.trimIndent()
        val result = ReceiptParser.parse(raw)
        assertEquals(12.10, result.total!!, 0.001)
        assertEquals(2.10, result.tax!!, 0.001)
    }

    @Test
    fun parsesDotDecimalAmounts() {
        val raw = """
            STORE EXAMPLE
            VAT: 2.50
            TOTAL: 12.50
        """.trimIndent()
        val result = ReceiptParser.parse(raw)
        assertEquals(12.50, result.total!!, 0.001)
        assertEquals(2.50, result.tax!!, 0.001)
    }

    @Test
    fun doesNotTreatSubtotalAsTotal() {
        val raw = """
            SUPERMERCADO EJEMPLO
            SUBTOTAL 10,00
            DESCUENTO 1,00
        """.trimIndent()
        val result = ReceiptParser.parse(raw)
        assertNull(result.total)
    }

    @Test
    fun doesNotTreatSplitSubtotalAsTotal() {
        val raw = """
            SUPERMERCADO EJEMPLO
            SUB TOTAL 10,00
            TOTAL 12,00
        """.trimIndent()
        val result = ReceiptParser.parse(raw)
        assertEquals(12.00, result.total!!, 0.001)
    }

    @Test
    fun acceptsTotalInsideLongerLabeledLine() {
        val raw = """
            TIENDA EJEMPLO
            BASE IMPONIBLE 10,00
            TOTAL A PAGAR 12,10
        """.trimIndent()
        val result = ReceiptParser.parse(raw)
        assertEquals(12.10, result.total!!, 0.001)
    }

    @Test
    fun recoversTotalWhenOcrSplitsTheLabel() {
        val raw = """
            TIENDA EJEMPLO
            T O T A L : 123,96
        """.trimIndent()
        val result = ReceiptParser.parse(raw)
        assertEquals(123.96, result.total!!, 0.001)
    }

    @Test
    fun recoversTotalWhenLabelAndAmountAreOnDifferentLines() {
        val raw = """
            TIENDA EJEMPLO
            TOTAL A PAGAR
            17,75
        """.trimIndent()
        val result = ReceiptParser.parse(raw)
        assertEquals(17.75, result.total!!, 0.001)
    }

    @Test
    fun rejectsImplausiblyLargeAmount() {
        val raw = """
            TIENDA EJEMPLO
            TOTAL: 9999999,99
        """.trimIndent()
        val result = ReceiptParser.parse(raw)
        assertNull(result.total)
    }

    @Test
    fun ignoresDigitPrefixedOcrNoiseWhenChoosingMerchant() {
        val raw = """
            1URA DE DETALLISTAS DE ALIMENTACION
            27/08/2026
            TOTAL A PAGAR 29,04
        """.trimIndent()
        val result = ReceiptParser.parse(raw)
        assertEquals("", result.merchant)
    }

    @Test
    fun extractsFinalTotalInsteadOfFirstItemAmountOnSpanishReceipt() {
        val raw = """
            SUPER ALCOOP 03
            FECHA: 27/08/2026 HORA: 18:23:22
            BOLSA REUTILIZABLE, UN 0,12
            BALANZA CHARCUTERIA 10,79
            BARRA PRECOC 235G ALFARES 3,80
            TOTAL 17,79
            EFECTIVO 20,00
            CAMBIO EFECTIVO -2,21
            IMPUESTOS BASE CUOTA
            10,00 % 9,81 0,98
            21,00 % 1,66 0,35
            4,00 % 4,80 0,19
            IMPUESTOS INCLUIDOS
        """.trimIndent()
        val result = ReceiptParser.parse(raw)
        assertEquals("SUPER ALCOOP 03", result.merchant)
        assertEquals("27/08/2026", result.date)
        assertEquals(17.79, result.total!!, 0.001)
        assertEquals(1.52, result.tax!!, 0.001)
    }

    @Test
    fun extractsTaxBySummingSpanishImpuestosQuotaRows() {
        val raw = """
            TIENDA EJEMPLO
            TOTAL 17,79
            IMPUESTOS BASE CUOTA
            10,00 % 9,81 0,98
            21,00 % 1,66 0,35
            4,00 % 4,80 0,19
            IMPUESTOS INCLUIDOS
        """.trimIndent()
        val result = ReceiptParser.parse(raw)
        assertEquals(1.52, result.tax!!, 0.001)
    }

    @Test
    fun ignoresCashWhenTotalAndPaymentAreOnSameOcrLine() {
        val raw = """
            SUPER ALCOOP 03
            TOTAL 17,79 EFECTIVO 20,00 CAMBIO 2,21
        """.trimIndent()
        val result = ReceiptParser.parse(raw)
        assertEquals(17.79, result.total!!, 0.001)
    }

    @Test
    fun parsesSplitSpanishTaxRows() {
        val raw = """
            TIENDA EJEMPLO
            TOTAL 17,79
            IMPUESTOS BASE CUOTA
            10,00 %
            9,81 0,98
            21,00 %
            1,66 0,35
            4,00 %
            4,80 0,19
            IMPUESTOS INCLUIDOS
        """.trimIndent()
        val result = ReceiptParser.parse(raw)
        assertEquals(1.52, result.tax!!, 0.001)
    }

    @Test
    fun correctsKnownSuperAlcoopOcrLossWithoutChangingGenericMerchantNames() {
        val result = ReceiptParser.parse("SOR ALCOOP 03\n27/08/2026\nTOTAL 17,79")
        assertEquals("SUPER ALCOOP 03", result.merchant)

        val generic = ReceiptParser.parse("SOR EXAMPLE\n27/08/2026\nTOTAL 17,79")
        assertEquals("SOR EXAMPLE", generic.merchant)
    }
}
