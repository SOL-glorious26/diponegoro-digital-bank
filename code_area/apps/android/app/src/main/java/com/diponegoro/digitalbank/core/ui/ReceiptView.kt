package com.diponegoro.digitalbank.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.diponegoro.digitalbank.core.ui.DdbOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.receipt.ReceiptFormatter
import com.diponegoro.digitalbank.core.receipt.ReceiptSharer
import com.diponegoro.digitalbank.core.util.formatRupiah

/** Kartu bukti transaksi + tombol bagikan sebagai gambar atau PDF (FR-14). */
@Composable
fun ReceiptCard(tx: LedgerTransaction, headline: String? = null) {
    val context = LocalContext.current
    val receipt = ReceiptFormatter.from(tx)
    SectionCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(headline ?: tx.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            StatusChip(tx.status)
        }
        Text(formatRupiah(tx.total), style = MaterialTheme.typography.headlineSmall)
        DividerLine()
        receipt.lines.filter { it.first != "Status" }.forEach { (k, v) -> InfoRow(k, v) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { ReceiptSharer.share(context, receipt, asPdf = false) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Image, contentDescription = null)
            Text(" Bagikan gambar")
        }
        OutlinedButton(onClick = { ReceiptSharer.share(context, receipt, asPdf = true) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
            Text(" Bagikan PDF")
        }
    }
}
