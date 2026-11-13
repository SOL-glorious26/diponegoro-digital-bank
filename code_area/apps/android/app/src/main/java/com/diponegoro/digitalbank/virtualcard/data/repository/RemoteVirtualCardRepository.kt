package com.diponegoro.digitalbank.virtualcard.data.repository

import com.diponegoro.digitalbank.core.network.ApiClient
import com.diponegoro.digitalbank.core.network.CardDto
import com.diponegoro.digitalbank.core.network.CreateCardRequest
import com.diponegoro.digitalbank.core.network.IsoTime
import com.diponegoro.digitalbank.core.network.LimitRequest
import com.diponegoro.digitalbank.core.network.PinRequest
import com.diponegoro.digitalbank.core.network.PurchaseRequest
import com.diponegoro.digitalbank.core.network.idemKey
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.virtualcard.domain.model.CardResult
import com.diponegoro.digitalbank.virtualcard.domain.model.CardScope
import com.diponegoro.digitalbank.virtualcard.domain.model.CardStatus
import com.diponegoro.digitalbank.virtualcard.domain.model.VirtualCard
import com.diponegoro.digitalbank.virtualcard.domain.repository.VirtualCardRepository
import java.util.UUID

/**
 * Virtual Card lewat backend (FR-21, FR-22). Server hanya mengirim nomor tersamar; nomor lengkap & CVV
 * (terenkripsi AES-GCM di server) dibuka lewat [revealDetails] setelah PIN valid.
 */
class RemoteVirtualCardRepository(
    private val client: ApiClient,
    private val onDataChanged: () -> Unit = {}
) : VirtualCardRepository {

    private fun <T> AppResult<T>.card(): CardResult<T> = when (this) {
        is AppResult.Success -> CardResult.Success(data)
        is AppResult.Failure -> CardResult.Failure(message)
    }

    private fun expiry(raw: String): Pair<Int, Int> {
        val (m, y) = raw.split("/").let { (it.getOrNull(0)?.toIntOrNull() ?: 1) to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
        return m to (if (y < 100) 2000 + y else y)
    }

    private fun CardDto.toModel(userId: String): VirtualCard {
        val (m, y) = expiry(expiry)
        return VirtualCard(
            cardId = id, userId = userId, cardholderName = cardholderName,
            cardNumber = "000000000000$last4", expiryMonth = m, expiryYear = y, cvv = "",
            status = runCatching { CardStatus.valueOf(status) }.getOrDefault(CardStatus.ACTIVE),
            usageScope = runCatching { CardScope.valueOf(usageScope) }.getOrDefault(CardScope.DOMESTIC),
            transactionLimit = transactionLimit, createdAt = IsoTime.parse(createdAt)
        )
    }

    override suspend fun getCards(userId: String): List<VirtualCard> =
        when (val r = client.call { cards() }) {
            is AppResult.Success -> r.data.map { it.toModel(userId) }
            is AppResult.Failure -> emptyList()
        }

    override suspend fun getCard(userId: String, cardId: String): VirtualCard? = getCards(userId).firstOrNull { it.cardId == cardId }

    override suspend fun createCard(userId: String, cardholderName: String, scope: CardScope): CardResult<VirtualCard> =
        client.call { createCard(CreateCardRequest(scope.name)) }.card().let { r ->
            if (r is CardResult.Success) CardResult.Success(r.data.toModel(userId)) else r as CardResult.Failure
        }

    override suspend fun updateStatus(userId: String, cardId: String, status: CardStatus): CardResult<VirtualCard> =
        client.call { if (status == CardStatus.FROZEN) freezeCard(cardId) else activateCard(cardId) }.card().let { r ->
            if (r is CardResult.Success) CardResult.Success(r.data.toModel(userId)) else r as CardResult.Failure
        }

    override suspend fun updateLimit(userId: String, cardId: String, limit: Long): CardResult<VirtualCard> =
        client.call { cardLimit(cardId, LimitRequest(limit)) }.card().let { r ->
            if (r is CardResult.Success) CardResult.Success(r.data.toModel(userId)) else r as CardResult.Failure
        }

    override suspend fun authorizeTransaction(userId: String, cardId: String, amount: Long, international: Boolean): CardResult<Unit> =
        client.call { simulatePurchase(cardId, idemKey(UUID.randomUUID().toString()), PurchaseRequest(amount, international, null)) }
            .card().let { r ->
                if (r is CardResult.Success) { onDataChanged(); CardResult.Success(Unit) } else r as CardResult.Failure
            }

    override suspend fun revealDetails(userId: String, cardId: String, pin: String): CardResult<VirtualCard> =
        client.call { cardDetails(cardId, PinRequest(pin)) }.card().let { r ->
            when (r) {
                is CardResult.Failure -> r
                is CardResult.Success -> {
                    val d = r.data
                    val (m, y) = expiry(d.expiry)
                    CardResult.Success(
                        VirtualCard(
                            cardId = d.id, userId = userId, cardholderName = d.cardholderName,
                            cardNumber = d.cardNumber.filter { it.isDigit() }, expiryMonth = m, expiryYear = y, cvv = d.cvv,
                            status = runCatching { CardStatus.valueOf(d.status) }.getOrDefault(CardStatus.ACTIVE),
                            usageScope = runCatching { CardScope.valueOf(d.usageScope) }.getOrDefault(CardScope.DOMESTIC),
                            transactionLimit = d.transactionLimit, createdAt = 0L
                        )
                    )
                }
            }
        }
}
