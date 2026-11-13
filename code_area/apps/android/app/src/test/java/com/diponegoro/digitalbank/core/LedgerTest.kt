package com.diponegoro.digitalbank.core

import com.diponegoro.digitalbank.TestEnv
import com.diponegoro.digitalbank.assertFailure
import com.diponegoro.digitalbank.core.ledger.ExternalAccounts
import com.diponegoro.digitalbank.core.ledger.PostingRequest
import com.diponegoro.digitalbank.core.ledger.TxCategory
import com.diponegoro.digitalbank.core.ledger.TxStatus
import com.diponegoro.digitalbank.ok
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerTest {
    private val env = TestEnv()
    private val ledger = env.ledger

    private fun request(key: String, amount: Long, lines: List<Pair<String, Long>>) = PostingRequest(
        key, TxCategory.TRANSFER, "Uji", amount, lines = lines,
        debitUserId = env.user, creditUserId = null,
        fromName = "A", fromDetail = "", toName = "B", toDetail = ""
    )

    @Test
    fun `seeded journal is balanced and demo balance positive`() {
        assertTrue(env.mainBalance() > 0)
        val all = ledger.transactionsFor(env.user)
        assertTrue(all.size > 20)
        all.forEach { tx -> assertEquals(0L, ledger.journalFor(tx.id).sumOf { it.amount }) }
    }

    @Test
    fun `unbalanced posting is rejected`() = runBlocking {
        val main = ledger.mainAccount(env.user)!!.id
        ledger.post(request("k1", 1000, listOf(main to -1000, ExternalAccounts.MERCHANT to 900))).assertFailure("seimbang")
    }

    @Test
    fun `insufficient balance writes nothing (atomic)`() = runBlocking {
        val main = ledger.mainAccount(env.user)!!.id
        val before = env.mainBalance()
        val huge = before + 1
        ledger.post(request("k2", huge, listOf(main to -huge, ExternalAccounts.MERCHANT to huge)))
            .assertFailure("tidak mencukupi")
        assertEquals(before, env.mainBalance())
        assertEquals(null, ledger.findByIdempotencyKey("k2"))
    }

    @Test
    fun `same idempotency key never debits twice`() = runBlocking {
        val main = ledger.mainAccount(env.user)!!.id
        val before = env.mainBalance()
        val first = ledger.post(request("k3", 10_000, listOf(main to -10_000, ExternalAccounts.MERCHANT to 10_000))).ok()
        val second = ledger.post(request("k3", 10_000, listOf(main to -10_000, ExternalAccounts.MERCHANT to 10_000))).ok()
        assertEquals(first.id, second.id)
        assertEquals(before - 10_000, env.mainBalance())
    }

    @Test
    fun `refund appends reversing lines and restores balance`() = runBlocking {
        val main = ledger.mainAccount(env.user)!!.id
        val before = env.mainBalance()
        val tx = ledger.post(request("k4", 50_000, listOf(main to -50_000, ExternalAccounts.INTERBANK to 50_000))).ok()
        val failed = ledger.failWithRefund(tx.id, "Ditolak").ok()
        assertEquals(TxStatus.FAILED, failed.status)
        assertEquals(before, env.mainBalance())
        assertEquals(4, ledger.journalFor(tx.id).size) // 2 baris asli + 2 pembalik (jejak audit)
    }

    @Test
    fun `new customer gets account with opening balance once`() = runBlocking {
        val a = ledger.ensureMainAccount("user-new", "BARU", 5_000_000)
        val b = ledger.ensureMainAccount("user-new", "BARU", 5_000_000)
        assertEquals(a.id, b.id)
        assertEquals(5_000_000L, ledger.balance(a.id))
    }
}
