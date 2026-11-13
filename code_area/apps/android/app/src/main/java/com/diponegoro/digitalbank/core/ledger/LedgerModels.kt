package com.diponegoro.digitalbank.core.ledger

enum class AccountKind { MAIN, POCKET, EXTERNAL }

/** Rekening pada buku besar. EXTERNAL mewakili pihak luar (jaringan antarbank, merchant, biller). */
data class LedgerAccount(
    val id: String,
    val ownerUserId: String?,
    val kind: AccountKind,
    val number: String,
    val name: String,
    val createdAt: Long,
    val closed: Boolean = false
)

enum class TxCategory(val label: String) {
    DEPOSIT("Setoran"),
    TRANSFER("Transfer"),
    QRIS("QRIS"),
    TOPUP("Top-up & Tagihan"),
    POCKET("Kantong"),
    /** Transaksi virtual card (backend); varian demo mencatat kartu di luar ledger. */
    CARD("Kartu Virtual")
}

enum class TxStatus(val label: String) {
    SUCCESS("Berhasil"),
    PROCESSING("Diproses"),
    FAILED("Gagal")
}

/** Arah transaksi dari sudut pandang satu pengguna. */
enum class Direction(val label: String) { IN("Masuk"), OUT("Keluar"), INTERNAL("Antar-kantong") }

/** Satu baris jurnal: amount positif menambah saldo rekening, negatif mengurangi. Append-only (jejak audit). */
data class JournalLine(
    val txId: String,
    val accountId: String,
    val amount: Long,
    val postedAt: Long,
    val memo: String
)

data class LedgerTransaction(
    val id: String,
    val reference: String,
    val idempotencyKey: String,
    val category: TxCategory,
    val status: TxStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val amount: Long,
    val fee: Long,
    val title: String,
    /** Pengguna yang saldonya berkurang (null bila pihak luar). */
    val debitUserId: String?,
    /** Pengguna yang saldonya bertambah (null bila pihak luar). */
    val creditUserId: String?,
    val fromName: String,
    val fromDetail: String,
    val toName: String,
    val toDetail: String,
    val note: String = "",
    val statusReason: String? = null,
    val extra: Map<String, String> = emptyMap()
) {
    val total: Long get() = amount + fee

    fun directionFor(userId: String): Direction = when {
        debitUserId == userId && creditUserId == userId -> Direction.INTERNAL
        debitUserId == userId -> Direction.OUT
        else -> Direction.IN
    }
}

/** Permintaan posting. lines = (accountId, amount); jumlah seluruh baris wajib 0 (double-entry). */
data class PostingRequest(
    val idempotencyKey: String,
    val category: TxCategory,
    val title: String,
    val amount: Long,
    val fee: Long = 0,
    val lines: List<Pair<String, Long>>,
    val debitUserId: String?,
    val creditUserId: String?,
    val fromName: String,
    val fromDetail: String,
    val toName: String,
    val toDetail: String,
    val note: String = "",
    val status: TxStatus = TxStatus.SUCCESS,
    val extra: Map<String, String> = emptyMap(),
    /** Hanya untuk data seed historis. */
    val at: Long? = null
)

sealed interface LedgerEvent {
    val tx: LedgerTransaction
    data class Posted(override val tx: LedgerTransaction) : LedgerEvent
    data class StatusChanged(override val tx: LedgerTransaction) : LedgerEvent
}

object ExternalAccounts {
    const val INTERBANK = "ext:interbank"
    const val MERCHANT = "ext:merchant"
    const val BILLER = "ext:biller"
    const val FEE_INCOME = "ext:fee"
    const val FUNDING = "ext:funding"
}
