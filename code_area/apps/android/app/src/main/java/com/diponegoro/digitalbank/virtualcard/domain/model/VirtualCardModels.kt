package com.diponegoro.digitalbank.virtualcard.domain.model

/** Sesuai class diagram Virtual Card: «enumeration» CardStatus. */
enum class CardStatus { ACTIVE, FROZEN }

/** Sesuai class diagram Virtual Card: «enumeration» CardScope. */
enum class CardScope { DOMESTIC, INTERNATIONAL }

/**
 * Entitas VirtualCard (FR-21, FR-22).
 *
 * Catatan desain:
 * - transactionLimit disimpan sebagai Long dalam satuan rupiah (bukan Real/Double)
 *   agar tidak ada kesalahan pembulatan pada nilai uang.
 * - expiry & CVV ditambahkan karena "lihat detail kartu" pada FR-22 butuh data tersebut.
 */
data class VirtualCard(
    val cardId: String,
    val userId: String,
    val cardholderName: String,
    val cardNumber: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
    val status: CardStatus,
    val usageScope: CardScope,
    val transactionLimit: Long,
    val createdAt: Long
) {
    val maskedNumber: String get() = "•••• •••• •••• ${cardNumber.takeLast(4)}"
    val formattedNumber: String get() = cardNumber.chunked(4).joinToString(" ")
    val expiry: String get() = "%02d/%02d".format(expiryMonth, expiryYear % 100)
}

/** Batas limit transaksi per transaksi (rupiah). Nilai usulan, SRS belum menetapkan (BR-07). */
object VirtualCardLimits {
    const val DEFAULT_LIMIT = 5_000_000L
    const val MIN_LIMIT = 10_000L
    const val MAX_LIMIT = 50_000_000L
}

sealed class CardResult<out T> {
    data class Success<out T>(val data: T) : CardResult<T>()
    data class Failure(val message: String) : CardResult<Nothing>()
}
