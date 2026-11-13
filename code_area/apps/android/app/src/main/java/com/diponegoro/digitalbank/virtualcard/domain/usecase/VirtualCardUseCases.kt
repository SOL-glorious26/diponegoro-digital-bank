package com.diponegoro.digitalbank.virtualcard.domain.usecase

import com.diponegoro.digitalbank.virtualcard.domain.model.CardResult
import com.diponegoro.digitalbank.virtualcard.domain.model.CardScope
import com.diponegoro.digitalbank.virtualcard.domain.model.CardStatus
import com.diponegoro.digitalbank.virtualcard.domain.model.VirtualCard
import com.diponegoro.digitalbank.virtualcard.domain.model.VirtualCardLimits
import com.diponegoro.digitalbank.virtualcard.domain.repository.AuthGateway
import com.diponegoro.digitalbank.virtualcard.domain.repository.VirtualCardRepository

private const val KYC_REQUIRED = "Transaksi finansial belum aktif. Selesaikan e-KYC terlebih dahulu."

class GetVirtualCardsUseCase(private val repository: VirtualCardRepository) {
    suspend operator fun invoke(userId: String): List<VirtualCard> = repository.getCards(userId)
}

/** UC: Create Virtual Card «include» Select Card Usage Scope (FR-21). */
class CreateVirtualCardUseCase(
    private val repository: VirtualCardRepository,
    private val authGateway: AuthGateway
) {
    suspend operator fun invoke(
        userId: String, cardholderName: String, scope: CardScope
    ): CardResult<VirtualCard> {
        if (!authGateway.isFinancialEnabled(userId)) return CardResult.Failure(KYC_REQUIRED)
        return repository.createCard(userId, cardholderName.trim().uppercase(), scope)
    }
}

/** UC: View Virtual Card Details «include» Verify Transaction PIN (FR-22). */
class ViewCardDetailsUseCase(
    private val repository: VirtualCardRepository,
    private val authGateway: AuthGateway
) {
    suspend operator fun invoke(userId: String, cardId: String, pin: String): CardResult<VirtualCard> {
        if (pin.length != 6 || !pin.all { it.isDigit() }) return CardResult.Failure("PIN harus 6 digit angka")
        repository.revealDetails(userId, cardId, pin)?.let { return it }
        val card = repository.getCard(userId, cardId) ?: return CardResult.Failure("Kartu tidak ditemukan")
        return when (val verified = authGateway.verifyPin(userId, pin)) {
            is CardResult.Failure -> verified // akses ke detail kartu ditolak
            is CardResult.Success -> CardResult.Success(card)
        }
    }
}

/** UC: Freeze Virtual Card. Kartu beku menolak seluruh transaksi. */
class FreezeCardUseCase(private val repository: VirtualCardRepository) {
    suspend operator fun invoke(userId: String, cardId: String): CardResult<VirtualCard> {
        val card = repository.getCard(userId, cardId) ?: return CardResult.Failure("Kartu tidak ditemukan")
        if (card.status == CardStatus.FROZEN) return CardResult.Failure("Kartu sudah dalam status beku")
        return repository.updateStatus(userId, cardId, CardStatus.FROZEN)
    }
}

/** UC: Activate Virtual Card. */
class ActivateCardUseCase(
    private val repository: VirtualCardRepository,
    private val authGateway: AuthGateway
) {
    suspend operator fun invoke(userId: String, cardId: String): CardResult<VirtualCard> {
        if (!authGateway.isFinancialEnabled(userId)) return CardResult.Failure(KYC_REQUIRED)
        val card = repository.getCard(userId, cardId) ?: return CardResult.Failure("Kartu tidak ditemukan")
        if (card.status == CardStatus.ACTIVE) return CardResult.Failure("Kartu sudah aktif")
        return repository.updateStatus(userId, cardId, CardStatus.ACTIVE)
    }
}

/** UC: Set Transaction Limit. Limit berlaku untuk transaksi berikutnya. */
class SetTransactionLimitUseCase(private val repository: VirtualCardRepository) {
    suspend operator fun invoke(userId: String, cardId: String, rawLimit: String): CardResult<VirtualCard> {
        val limit = parseRupiah(rawLimit) ?: return CardResult.Failure("Limit harus berupa angka")
        if (limit < VirtualCardLimits.MIN_LIMIT)
            return CardResult.Failure("Limit minimal ${formatRupiah(VirtualCardLimits.MIN_LIMIT)}")
        if (limit > VirtualCardLimits.MAX_LIMIT)
            return CardResult.Failure("Limit maksimal ${formatRupiah(VirtualCardLimits.MAX_LIMIT)}")
        return repository.updateLimit(userId, cardId, limit)
    }
}

/** Simulasi transaksi merchant untuk menguji kriteria penerimaan FR-22 (beku & limit). */
class SimulateCardTransactionUseCase(private val repository: VirtualCardRepository) {
    suspend operator fun invoke(
        userId: String, cardId: String, rawAmount: String, international: Boolean
    ): CardResult<Long> {
        val amount = parseRupiah(rawAmount) ?: return CardResult.Failure("Nominal harus berupa angka")
        if (amount <= 0) return CardResult.Failure("Nominal harus lebih dari 0")
        return when (val r = repository.authorizeTransaction(userId, cardId, amount, international)) {
            is CardResult.Failure -> r
            is CardResult.Success -> CardResult.Success(amount)
        }
    }
}

internal fun parseRupiah(raw: String): Long? {
    val digits = raw.filter { it.isDigit() }
    if (digits.isEmpty() || digits.length > 15) return null
    return digits.toLongOrNull()
}

fun formatRupiah(amount: Long): String {
    val grouped = amount.toString().reversed().chunked(3).joinToString(".").reversed()
    return "Rp$grouped"
}
