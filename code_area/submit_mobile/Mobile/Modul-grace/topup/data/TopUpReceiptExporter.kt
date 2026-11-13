package com.diponegoro.digitalbank.topup.data

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.diponegoro.digitalbank.account.domain.formatRupiah
import com.diponegoro.digitalbank.topup.domain.TopUpTransaction
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TopUpReceiptExporter {
    fun generate(transaction: TopUpTransaction): ByteArray {
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("id-ID"))
        val lines = listOf(
            "Bukti Top-up Simulasi Digital Bank", "Bukan bukti pembayaran resmi", "",
            "Referensi: ${transaction.reference}", "Waktu permintaan: ${date.format(Date(transaction.createdAt))}",
            "Waktu selesai: ${transaction.completedAt?.let { date.format(Date(it)) } ?: "—"}",
            "Status: ${transaction.status.label}", "Layanan: ${transaction.serviceName}",
            "Nomor tujuan: ${transaction.target}", "Rekening: ${transaction.accountNumber}",
            "Metode: ${transaction.methodName}", "Arah saldo rekening: ${transaction.direction}",
            "Nominal: ${formatRupiah(transaction.amount)}", "Biaya: ${formatRupiah(transaction.fee)}",
            "Total: ${formatRupiah(transaction.total)}", "Saldo setelah: ${transaction.balanceAfter?.let(::formatRupiah) ?: "—"}",
            "", transaction.note
        ).flatMap { it.chunked(80).ifEmpty { listOf("") } }
        val pdf = PdfDocument()
        try {
            val paint = Paint().apply { textSize = 12f; isAntiAlias = true }
            lines.chunked(38).forEachIndexed { index, pageLines ->
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
                pageLines.forEachIndexed { row, text -> page.canvas.drawText(text, 32f, 45f + row * 19f, paint) }
                pdf.finishPage(page)
            }
            return ByteArrayOutputStream().use { output -> pdf.writeTo(output); output.toByteArray() }
        } finally { pdf.close() }
    }
}
