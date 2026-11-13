package com.diponegoro.digitalbank.account.data

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.diponegoro.digitalbank.account.domain.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatementExporter {
    fun generate(account: BankAccount, owner: String, start: Long, end: Long,
                 rows: List<AccountTransaction>, format: StatementFormat, id: String): StatementDocument {
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("id-ID"))
        val period = "${date.format(Date(start))} - ${date.format(Date(end - 1))}"
        val bytes: ByteArray = when (format) {
            StatementFormat.CSV -> buildString {
                append('\uFEFF')
                append("e-Statement Simulasi Digital Bank\r\n")
                append(listOf("Pemilik", owner, "Rekening", account.number, "Periode", period).joinToString(",", transform = ::csvCell)).append("\r\n")
                append("Tanggal,Jenis,Deskripsi,Nominal IDR,Saldo Setelah IDR,Referensi,Status\r\n")
                rows.forEach { row ->
                    append(listOf(date.format(Date(row.createdAt)), row.type.name, row.description,
                        row.amount.toString(), row.balanceAfter.toString(), row.reference, row.status.name)
                        .joinToString(",", transform = ::csvCell)).append("\r\n")
                }
            }.toByteArray(Charsets.UTF_8)
            StatementFormat.PDF -> {
                val lines = buildList {
                    add("e-Statement Simulasi Digital Bank")
                    add("Bukan dokumen resmi perbankan")
                    add("Pemilik: $owner"); add("Rekening: ${account.number} (${account.currency})")
                    add("Periode: $period"); add("ID: $id"); add("")
                    if (rows.isEmpty()) add("Tidak ada transaksi dalam periode ini.")
                    rows.forEach {
                        add("${date.format(Date(it.createdAt))} | ${it.type} | ${it.status}")
                        add(it.description); add("Nominal: ${formatRupiah(it.amount)} | Saldo setelah: ${formatRupiah(it.balanceAfter)}")
                        add("Referensi: ${it.reference}"); add("")
                    }
                }.flatMap { it.chunked(85).ifEmpty { listOf("") } }
                val pdf = PdfDocument()
                try {
                    val paint = Paint().apply { textSize = 11f; isAntiAlias = true }
                    lines.chunked(45).forEachIndexed { index, pageLines ->
                        val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
                        pageLines.forEachIndexed { line, value -> page.canvas.drawText(value, 32f, 45f + line * 16f, paint) }
                        page.canvas.drawText("Halaman ${index + 1}", 32f, 810f, paint)
                        pdf.finishPage(page)
                    }
                    ByteArrayOutputStream().use { output -> pdf.writeTo(output); output.toByteArray() }
                } finally { pdf.close() }
            }
        }
        return StatementDocument("statement-${account.number}-${id.take(8)}.${format.extension}", format, bytes)
    }
    // Jangan biarkan deskripsi dibaca spreadsheet sebagai formula.
    internal fun csvCell(value: String): String {
        val safe = if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@')) "'$value" else value
        return "\"${safe.replace("\"", "\"\"")}\""
    }
}
