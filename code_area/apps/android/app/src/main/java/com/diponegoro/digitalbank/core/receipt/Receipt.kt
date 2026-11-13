package com.diponegoro.digitalbank.core.receipt

import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.util.formatDateTime
import com.diponegoro.digitalbank.core.util.formatRupiah

/** Isi bukti transaksi (FR-14 & FR-24): ID, waktu, pengirim, penerima, nominal, biaya, status. */
data class Receipt(val title: String, val status: String, val lines: List<Pair<String, String>>)

object ReceiptFormatter {
    fun from(tx: LedgerTransaction): Receipt {
        val lines = buildList {
            add("ID transaksi" to tx.reference)
            add("Waktu" to formatDateTime(tx.createdAt))
            add("Jenis" to "${tx.category.label} • ${tx.title}")
            add("Pengirim" to tx.fromName)
            if (tx.fromDetail.isNotBlank()) add("Dari" to tx.fromDetail)
            add("Penerima" to tx.toName)
            if (tx.toDetail.isNotBlank()) add("Tujuan" to tx.toDetail)
            add("Nominal" to formatRupiah(tx.amount))
            add("Biaya" to formatRupiah(tx.fee))
            add("Total" to formatRupiah(tx.total))
            if (tx.note.isNotBlank()) add("Catatan" to tx.note)
            tx.extra.forEach { (k, v) -> add(k to v) }
            add("Status" to tx.status.label)
            tx.statusReason?.let { add("Keterangan" to it) }
        }
        return Receipt("Bukti ${tx.title}", tx.status.label, lines)
    }
}
