package com.diponegoro.digitalbank.virtualcard.data.repository

import com.diponegoro.digitalbank.virtualcard.domain.model.CardResult
import com.diponegoro.digitalbank.virtualcard.domain.model.CardScope
import com.diponegoro.digitalbank.virtualcard.domain.model.CardStatus
import com.diponegoro.digitalbank.virtualcard.domain.model.VirtualCard
import com.diponegoro.digitalbank.virtualcard.domain.model.VirtualCardLimits
import com.diponegoro.digitalbank.virtualcard.domain.repository.VirtualCardRepository
import com.diponegoro.digitalbank.virtualcard.domain.usecase.formatRupiah
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.UUID

/**
 * Implementasi in-memory (simulator penerbit kartu). Data hilang saat proses aplikasi dimatikan.
 * Nomor kartu dibuat acak dengan check digit Luhn agar formatnya realistis, tetapi bukan kartu nyata.
 */
class MockVirtualCardRepository(
    private val networkDelayMs: Long = 300L
) : VirtualCardRepository {

    private val cards = linkedMapOf<String, VirtualCard>()

    private companion object {
        const val BIN_PREFIX = "489512" // prefix fiktif untuk simulasi
        const val VALIDITY_YEARS = 3
    }

    override suspend fun getCards(userId: String): List<VirtualCard> {
        delay(networkDelayMs)
        return cards.values.filter { it.userId == userId }.sortedByDescending { it.createdAt }
    }

    override suspend fun getCard(userId: String, cardId: String): VirtualCard? =
        cards[cardId]?.takeIf { it.userId == userId }

    override suspend fun createCard(
        userId: String, cardholderName: String, scope: CardScope
    ): CardResult<VirtualCard> {
        delay(networkDelayMs)
        val cal = Calendar.getInstance().apply { add(Calendar.YEAR, VALIDITY_YEARS) }
        val card = VirtualCard(
            cardId = "card-${UUID.randomUUID()}",
            userId = userId,
            cardholderName = cardholderName,
            cardNumber = generateCardNumber(),
            expiryMonth = cal.get(Calendar.MONTH) + 1,
            expiryYear = cal.get(Calendar.YEAR),
            cvv = (100..999).random().toString(),
            status = CardStatus.ACTIVE,
            usageScope = scope,
            transactionLimit = VirtualCardLimits.DEFAULT_LIMIT,
            createdAt = System.currentTimeMillis()
        )
        cards[card.cardId] = card
        return CardResult.Success(card)
    }

    override suspend fun updateStatus(userId: String, cardId: String, status: CardStatus): CardResult<VirtualCard> {
        delay(networkDelayMs)
        val card = getCard(userId, cardId) ?: return CardResult.Failure("Kartu tidak ditemukan")
        val updated = card.copy(status = status)
        cards[cardId] = updated
        return CardResult.Success(updated)
    }

    override suspend fun updateLimit(userId: String, cardId: String, limit: Long): CardResult<VirtualCard> {
        delay(networkDelayMs)
        val card = getCard(userId, cardId) ?: return CardResult.Failure("Kartu tidak ditemukan")
        val updated = card.copy(transactionLimit = limit)
        cards[cardId] = updated
        return CardResult.Success(updated)
    }

    override suspend fun authorizeTransaction(
        userId: String, cardId: String, amount: Long, international: Boolean
    ): CardResult<Unit> {
        delay(networkDelayMs)
        val card = getCard(userId, cardId) ?: return CardResult.Failure("Kartu tidak ditemukan")
        if (card.status == CardStatus.FROZEN)
            return CardResult.Failure("Transaksi ditolak: kartu sedang dibekukan")
        if (international && card.usageScope == CardScope.DOMESTIC)
            return CardResult.Failure("Transaksi ditolak: kartu hanya berlaku domestik")
        if (amount > card.transactionLimit)
            return CardResult.Failure(
                "Transaksi ditolak: melebihi limit ${formatRupiah(card.transactionLimit)}"
            )
        return CardResult.Success(Unit)
    }

    private fun generateCardNumber(): String {
        while (true) {
            val body = BIN_PREFIX + (1..9).joinToString("") { (0..9).random().toString() }
            val number = body + luhnCheckDigit(body)
            if (cards.values.none { it.cardNumber == number }) return number
        }
    }
}

/** Menghitung check digit Luhn untuk 15 digit pertama. */
internal fun luhnCheckDigit(body: String): Int {
    var sum = 0
    body.reversed().forEachIndexed { i, c ->
        var d = c.digitToInt()
        if (i % 2 == 0) { d *= 2; if (d > 9) d -= 9 }
        sum += d
    }
    return (10 - sum % 10) % 10
}

internal fun isLuhnValid(number: String): Boolean =
    number.length > 1 && number.all { it.isDigit() } &&
        luhnCheckDigit(number.dropLast(1)) == number.last().digitToInt()
