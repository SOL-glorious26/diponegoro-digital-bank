package com.diponegoro.digitalbank.virtualcard

import com.diponegoro.digitalbank.virtualcard.data.repository.MockVirtualCardRepository
import com.diponegoro.digitalbank.virtualcard.data.repository.isLuhnValid
import com.diponegoro.digitalbank.virtualcard.domain.model.CardResult
import com.diponegoro.digitalbank.virtualcard.domain.model.CardScope
import com.diponegoro.digitalbank.virtualcard.domain.model.CardStatus
import com.diponegoro.digitalbank.virtualcard.domain.model.VirtualCard
import com.diponegoro.digitalbank.virtualcard.domain.model.VirtualCardLimits
import com.diponegoro.digitalbank.virtualcard.domain.repository.AuthGateway
import com.diponegoro.digitalbank.virtualcard.domain.usecase.ActivateCardUseCase
import com.diponegoro.digitalbank.virtualcard.domain.usecase.CreateVirtualCardUseCase
import com.diponegoro.digitalbank.virtualcard.domain.usecase.FreezeCardUseCase
import com.diponegoro.digitalbank.virtualcard.domain.usecase.SetTransactionLimitUseCase
import com.diponegoro.digitalbank.virtualcard.domain.usecase.SimulateCardTransactionUseCase
import com.diponegoro.digitalbank.virtualcard.domain.usecase.ViewCardDetailsUseCase
import com.diponegoro.digitalbank.virtualcard.domain.usecase.formatRupiah
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtualCardUseCasesTest {

    private val userId = "user-1"

    private class FakeAuthGateway(var financialEnabled: Boolean = true) : AuthGateway {
        override suspend fun isFinancialEnabled(userId: String) = financialEnabled
        override suspend fun verifyPin(userId: String, pin: String): CardResult<Unit> =
            if (pin == "123456") CardResult.Success(Unit) else CardResult.Failure("PIN salah")
    }

    private val repo = MockVirtualCardRepository(networkDelayMs = 0)
    private val gateway = FakeAuthGateway()
    private val create = CreateVirtualCardUseCase(repo, gateway)
    private val viewDetails = ViewCardDetailsUseCase(repo, gateway)
    private val freeze = FreezeCardUseCase(repo)
    private val activate = ActivateCardUseCase(repo, gateway)
    private val setLimit = SetTransactionLimitUseCase(repo)
    private val pay = SimulateCardTransactionUseCase(repo)

    private fun <T> CardResult<T>.ok(): T = (this as? CardResult.Success)?.data
        ?: throw AssertionError("Expected Success but was $this")

    private fun CardResult<*>.assertFailure() =
        assertTrue("Expected Failure but was $this", this is CardResult.Failure)

    private suspend fun newCard(scope: CardScope = CardScope.DOMESTIC): VirtualCard =
        create(userId, "demo", scope).ok()

    @Test
    fun `create card produces active card with valid 16 digit number`() = runBlocking {
        val card = newCard(CardScope.INTERNATIONAL)
        assertEquals(16, card.cardNumber.length)
        assertTrue(isLuhnValid(card.cardNumber))
        assertEquals(CardStatus.ACTIVE, card.status)
        assertEquals(CardScope.INTERNATIONAL, card.usageScope)
        assertEquals("DEMO", card.cardholderName)
        assertEquals(VirtualCardLimits.DEFAULT_LIMIT, card.transactionLimit)
    }

    @Test
    fun `create card rejected when e-KYC not approved`() = runBlocking {
        gateway.financialEnabled = false
        create(userId, "demo", CardScope.DOMESTIC).assertFailure()
    }

    @Test
    fun `details require valid PIN`() = runBlocking {
        val card = newCard()
        viewDetails(userId, card.cardId, "000000").assertFailure()
        viewDetails(userId, card.cardId, "12").assertFailure()
        assertEquals(card.cardNumber, viewDetails(userId, card.cardId, "123456").ok().cardNumber)
    }

    @Test
    fun `other user cannot access card`() = runBlocking {
        val card = newCard()
        viewDetails("user-2", card.cardId, "123456").assertFailure()
        freeze("user-2", card.cardId).assertFailure()
    }

    @Test
    fun `frozen card rejects all transactions until activated`() = runBlocking {
        val card = newCard()
        freeze(userId, card.cardId).ok()
        pay(userId, card.cardId, "50000", international = false).assertFailure()
        freeze(userId, card.cardId).assertFailure() // sudah beku

        activate(userId, card.cardId).ok()
        assertEquals(50_000L, pay(userId, card.cardId, "50000", international = false).ok())
    }

    @Test
    fun `limit validation and enforcement on next transaction`() = runBlocking {
        val card = newCard()
        setLimit(userId, card.cardId, "5000").assertFailure()          // < minimum
        setLimit(userId, card.cardId, "60000000").assertFailure()      // > maksimum
        setLimit(userId, card.cardId, "abc").assertFailure()

        setLimit(userId, card.cardId, "100000").ok()
        pay(userId, card.cardId, "100000", international = false).ok()
        pay(userId, card.cardId, "100001", international = false).assertFailure()
    }

    @Test
    fun `domestic card rejects international transaction`() = runBlocking {
        val domestic = newCard(CardScope.DOMESTIC)
        val international = newCard(CardScope.INTERNATIONAL)
        pay(userId, domestic.cardId, "10000", international = true).assertFailure()
        pay(userId, international.cardId, "10000", international = true).ok()
        Unit
    }

    @Test
    fun `rupiah formatting`() {
        assertEquals("Rp5.000.000", formatRupiah(5_000_000))
        assertEquals("Rp10.000", formatRupiah(10_000))
        assertEquals("Rp999", formatRupiah(999))
    }
}
