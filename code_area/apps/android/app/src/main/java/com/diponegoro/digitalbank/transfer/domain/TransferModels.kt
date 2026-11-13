package com.diponegoro.digitalbank.transfer.domain

data class Bank(val code: String, val name: String) {
    val isInternal: Boolean get() = code == INTERNAL_CODE

    companion object {
        const val INTERNAL_CODE = "DDB"
    }
}

/** Hasil validasi rekening tujuan (FR-12): nama penerima tampil sebelum PIN diminta. */
data class Recipient(
    val bank: Bank,
    val accountNumber: String,
    val name: String
)

data class FavoriteRecipient(
    val id: String,
    val recipient: Recipient,
    val addedAt: Long
)

data class TransferQuote(
    val recipient: Recipient,
    val amount: Long,
    val fee: Long,
    val note: String,
    val balance: Long
) {
    val total: Long get() = amount + fee
}

object TransferRules {
    /** BR-01: minimum transfer Rp5.000 untuk sesama dan antarbank. */
    const val MIN_AMOUNT = 5_000L
    /** BR-07 belum ditetapkan SRS; nilai usulan untuk simulasi. */
    const val MAX_AMOUNT_PER_TRANSACTION = 25_000_000L
    /** Biaya antarbank (setara BI-FAST); sesama bank gratis. */
    const val INTERBANK_FEE = 2_500L
    const val MAX_NOTE = 50
    const val MAX_FAVORITES = 20
}

/** Perilaku simulator jaringan antarbank untuk menguji alur alternatif UC-01. */
enum class NetworkBehavior { ACCEPT, REJECT, TIMEOUT }
