package com.diponegoro.digitalbank.transfer

import com.diponegoro.digitalbank.TestEnv
import com.diponegoro.digitalbank.assertFailure
import com.diponegoro.digitalbank.core.ledger.DemoData
import com.diponegoro.digitalbank.core.ledger.TxStatus
import com.diponegoro.digitalbank.ok
import com.diponegoro.digitalbank.transfer.data.LocalTransferRepository
import com.diponegoro.digitalbank.transfer.domain.Bank
import com.diponegoro.digitalbank.transfer.domain.TransferRules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferRepositoryTest {
    private val env = TestEnv()
    private val repo = LocalTransferRepository(env.ledger, env.guard)
    private val andi = DemoData.customers.first()

    @Test
    fun `account validation shows recipient name or rejects invalid`() = runBlocking<Unit> {
        assertEquals("ANDI PRATAMA", repo.inquire(env.user, Bank.INTERNAL_CODE, andi.number).ok().name)
        assertEquals("ANDI WIJAYA", repo.inquire(env.user, "BCA", "1234567890").ok().name)
        repo.inquire(env.user, "BCA", "1111111111").assertFailure("tidak valid")
        repo.inquire(env.user, "BCA", "123").assertFailure("10–16")
        repo.inquire(env.user, Bank.INTERNAL_CODE, DemoData.DEMO_ACCOUNT).assertFailure("sendiri")
    }

    @Test
    fun `amount rules min max and insufficient balance`() = runBlocking<Unit> {
        val r = repo.inquire(env.user, "BCA", "1234567890").ok()
        repo.quote(env.user, r, "4999", "").assertFailure("minimal")
        repo.quote(env.user, r, (TransferRules.MAX_AMOUNT_PER_TRANSACTION + 1).toString(), "").assertFailure("maksimal")
        val balance = env.mainBalance()
        if (balance < TransferRules.MAX_AMOUNT_PER_TRANSACTION)
            repo.quote(env.user, r, balance.toString(), "").assertFailure("tidak mencukupi") // + biaya
        val q = repo.quote(env.user, r, "100000", "kos").ok()
        assertEquals(TransferRules.INTERBANK_FEE, q.fee)
    }

    @Test
    fun `internal transfer debits and credits atomically without fee`() = runBlocking<Unit> {
        val r = repo.inquire(env.user, Bank.INTERNAL_CODE, andi.number).ok()
        val q = repo.quote(env.user, r, "250000", "patungan").ok()
        assertEquals(0L, q.fee)
        val before = env.mainBalance()
        val andiBefore = env.mainBalance(andi.userId)
        val tx = repo.execute(env.user, q, env.pin, "t1").ok()
        assertEquals(TxStatus.SUCCESS, tx.status)
        assertEquals(before - 250_000, env.mainBalance())
        assertEquals(andiBefore + 250_000, env.mainBalance(andi.userId))
    }

    @Test
    fun `interbank transfer charges fee`() = runBlocking<Unit> {
        val r = repo.inquire(env.user, "BNI", "1122334455").ok()
        val q = repo.quote(env.user, r, "100000", "").ok()
        val before = env.mainBalance()
        repo.execute(env.user, q, env.pin, "t2").ok()
        assertEquals(before - 100_000 - TransferRules.INTERBANK_FEE, env.mainBalance())
    }

    @Test
    fun `rejected by network is refunded automatically`() = runBlocking<Unit> {
        val r = repo.inquire(env.user, "BRI", "9999999999").ok()
        val q = repo.quote(env.user, r, "100000", "").ok()
        val before = env.mainBalance()
        val tx = repo.execute(env.user, q, env.pin, "t3").ok()
        assertEquals(TxStatus.FAILED, tx.status)
        assertEquals(before, env.mainBalance())
    }

    @Test
    fun `timeout stays processing then completes without double debit`() = runBlocking<Unit> {
        val r = repo.inquire(env.user, "MANDIRI", "8888888888").ok()
        val q = repo.quote(env.user, r, "100000", "").ok()
        val before = env.mainBalance()
        val tx = repo.execute(env.user, q, env.pin, "t4").ok()
        assertEquals(TxStatus.PROCESSING, tx.status)
        // Pengguna menekan ulang (retry) dengan kunci yang sama → transaksi yang sama.
        assertEquals(tx.id, repo.execute(env.user, q, env.pin, "t4").ok().id)
        assertEquals(TxStatus.SUCCESS, repo.checkStatus(env.user, tx.id).ok().status)
        assertEquals(before - q.total, env.mainBalance())
    }

    @Test
    fun `wrong PIN shows remaining attempts and blocks after three`() = runBlocking<Unit> {
        val r = repo.inquire(env.user, "BCA", "1234567890").ok()
        val q = repo.quote(env.user, r, "10000", "").ok()
        repo.execute(env.user, q, "111111", "t5").assertFailure("Sisa percobaan: 2")
        repo.execute(env.user, q, "111111", "t5").assertFailure("Sisa percobaan: 1")
        repo.execute(env.user, q, "111111", "t5").assertFailure("diblokir")
        repo.execute(env.user, q, env.pin, "t5").assertFailure("diblokir")
    }

    @Test
    fun `transfer requires approved e-KYC`() = runBlocking<Unit> {
        val registered = env.auth.register("pengguna", "baru@mail.com", "", "Rahasia123").ok()
        env.ledger.ensureMainAccount(registered.userId, "PENGGUNA", 1_000_000)
        val r = repo.inquire(registered.userId, "BCA", "1234567890").ok()
        val q = repo.quote(registered.userId, r, "10000", "").ok()
        repo.execute(registered.userId, q, "123456", "t6").assertFailure("e-KYC")
    }

    @Test
    fun `favorites can be added once and removed`() = runBlocking<Unit> {
        val r = repo.inquire(env.user, "BCA", "1234567890").ok()
        val fav = repo.addFavorite(env.user, r).ok()
        repo.addFavorite(env.user, r).assertFailure("sudah")
        assertTrue(repo.isFavorite(env.user, r))
        repo.removeFavorite(env.user, fav.id)
        assertTrue(repo.favorites(env.user).isEmpty())
    }
}

private fun <T> com.diponegoro.digitalbank.authentication.domain.model.AuthResult<T>.ok(): T =
    (this as com.diponegoro.digitalbank.authentication.domain.model.AuthResult.Success).data
