package com.diponegoro.digitalbank.payment

import com.diponegoro.digitalbank.TestEnv
import com.diponegoro.digitalbank.assertFailure
import com.diponegoro.digitalbank.core.ledger.DemoData
import com.diponegoro.digitalbank.ok
import com.diponegoro.digitalbank.payment.data.LocalPaymentRepository
import com.diponegoro.digitalbank.payment.domain.QrisCodec
import com.diponegoro.digitalbank.payment.domain.QrisPayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentRepositoryTest {
    private val env = TestEnv()
    private val repo = LocalPaymentRepository(env.ledger, env.guard)

    @Test
    fun `qris encode decode roundtrip with valid crc`() = runBlocking<Unit> {
        val payload = QrisPayload("ID.CO.QRIS.WWW", "93600001", "M1", "WARUNG", "SEMARANG", "5812", 28_000)
        val raw = QrisCodec.encode(payload)
        assertTrue(raw.startsWith("000201010212"))
        assertEquals(payload, QrisCodec.decode(raw).getOrThrow())
    }

    @Test
    fun `tampered qris is rejected`() = runBlocking<Unit> {
        val raw = QrisCodec.encode(QrisPayload("ID.CO.QRIS.WWW", "1", "M1", "WARUNG", "SMG", "5812", 28_000))
        val tampered = raw.replace("28000", "10000")
        assertTrue(QrisCodec.decode(tampered).isFailure)
        repo.scan(env.user, "bukan qris").assertFailure()
    }

    @Test
    fun `dynamic qris amount cannot be changed by payer`() = runBlocking<Unit> {
        val qr = repo.demoQrs().first { it.label.contains("Kopi") }
        val scanned = repo.scan(env.user, qr.raw).ok()
        val before = env.mainBalance()
        val tx = repo.pay(env.user, scanned, "1000", env.pin, "p1").ok()
        assertEquals(28_000L, tx.amount)
        assertEquals(before - 28_000, env.mainBalance())
    }

    @Test
    fun `static qris uses payer amount and wrong pin does not debit`() = runBlocking<Unit> {
        val scanned = repo.scan(env.user, repo.demoQrs().first().raw).ok()
        assertNull(scanned.payload.amount)
        val before = env.mainBalance()
        repo.pay(env.user, scanned, "15000", "000000", "p2").assertFailure("PIN salah")
        assertEquals(before, env.mainBalance())
        repo.pay(env.user, scanned, "15000", env.pin, "p2").ok()
        assertEquals(before - 15_000, env.mainBalance())
    }

    @Test
    fun `paying personal qris of same bank credits that customer`() = runBlocking<Unit> {
        val andi = DemoData.customers.first()
        val scanned = repo.scan(env.user, repo.demoQrs().last().raw).ok()
        val andiBefore = env.mainBalance(andi.userId)
        repo.pay(env.user, scanned, "20000", env.pin, "p3").ok()
        assertEquals(andiBefore + 20_000, env.mainBalance(andi.userId))
    }

    @Test
    fun `personal qris can be received and own qris cannot be paid`() = runBlocking<Unit> {
        val own = repo.personalQr(env.user, null).ok()
        repo.scan(env.user, own).assertFailure("sendiri")
        val dynamic = QrisCodec.decode(repo.personalQr(env.user, 75_000).ok()).getOrThrow()
        assertEquals(75_000L, dynamic.amount)
        val before = env.mainBalance()
        repo.simulateIncoming(env.user, "50000", "Rina", "in1").ok()
        assertEquals(before + 50_000, env.mainBalance())
    }
}
