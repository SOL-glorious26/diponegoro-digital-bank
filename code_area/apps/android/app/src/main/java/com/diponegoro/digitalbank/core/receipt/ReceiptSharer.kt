package com.diponegoro.digitalbank.core.receipt

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/** Membuat bukti transaksi sebagai gambar (PNG) atau PDF lalu membuka lembar "Bagikan". */
object ReceiptSharer {

    private const val WIDTH = 720
    private const val MARGIN = 40f
    private const val LINE = 38f

    fun share(context: Context, receipt: Receipt, asPdf: Boolean) {
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val safeId = receipt.lines.firstOrNull()?.second?.filter { it.isLetterOrDigit() } ?: "bukti"
        val file = File(dir, "bukti-$safeId.${if (asPdf) "pdf" else "png"}")
        if (asPdf) writePdf(receipt, file) else writePng(receipt, file)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (asPdf) "application/pdf" else "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, receipt.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan bukti").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun height(receipt: Receipt) = (MARGIN * 2 + 140 + receipt.lines.size * LINE + 60).toInt()

    private fun draw(canvas: Canvas, receipt: Receipt) {
        canvas.drawColor(Color.WHITE)
        val header = Paint().apply { color = Color.rgb(11, 61, 145) }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), 110f, header)
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 30f; typeface = Typeface.DEFAULT_BOLD }
        canvas.drawText("Diponegoro Digital Bank", MARGIN, 52f, white)
        white.textSize = 22f; white.typeface = Typeface.DEFAULT
        canvas.drawText(receipt.title, MARGIN, 88f, white)

        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 22f }
        val value = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 22f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT
        }
        var y = 160f
        receipt.lines.forEach { (k, v) ->
            canvas.drawText(k, MARGIN, y, label)
            canvas.drawText(v.take(40), WIDTH - MARGIN, y, value)
            y += LINE
        }
        val foot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = 18f }
        canvas.drawText("Dokumen simulasi akademik — bukan bukti transaksi resmi.", MARGIN, y + 30f, foot)
    }

    private fun writePng(receipt: Receipt, file: File) {
        val bitmap = Bitmap.createBitmap(WIDTH, height(receipt), Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), receipt)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun writePdf(receipt: Receipt, file: File) {
        val pdf = PdfDocument()
        try {
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(WIDTH, height(receipt), 1).create())
            draw(page.canvas, receipt)
            pdf.finishPage(page)
            FileOutputStream(file).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
    }
}
