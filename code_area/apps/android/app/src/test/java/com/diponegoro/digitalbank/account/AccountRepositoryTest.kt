package com.diponegoro.digitalbank.account

import com.diponegoro.digitalbank.TestEnv
import com.diponegoro.digitalbank.account.data.LocalAccountRepository
import com.diponegoro.digitalbank.account.domain.BalancePeriod
import com.diponegoro.digitalbank.account.domain.LockRule
import com.diponegoro.digitalbank.assertFailure
import com.diponegoro.digitalbank.ok
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountRepositoryTest {
    private val env = TestEnv()
    private val repo = LocalAccountRepository(env.ledger, env.guard, env.prefs)
    private val day = 24 * 60 * 60 * 1000L

    @Test
    fun `hide balance preference is stored`() = runBlocking<Unit> {
        assertFalse(repo.overview(env.user).ok().balanceHidden)
        repo.setBalanceHidden(env.user, true)
        assertTrue(repo.overview(env.user).ok().balanceHidden)
    }

    @Test
    fun `balance chart is consistent with ledger`() = runBlocking<Unit> {
        BalancePeriod.entries.forEach { period ->
            val series = repo.balanceSeries(env.user, period)
            assertEquals(period.points, series.size)
            assertEquals(env.mainBalance(), series.last().balance)
        }
    }

    @Test
    fun `pocket create update deposit withdraw delete`() = runBlocking<Unit> {
        val pocket = repo.createPocket(env.user, "Liburan", "Bali 2027", 2_000_000, null).ok()
        val before = env.mainBalance()
        repo.deposit(env.user, pocket.id, 500_000, "d1").ok()
        assertEquals(before - 500_000, env.mainBalance())
        assertEquals(500_000L, repo.pocket(env.user, pocket.id)!!.balance)
        assertEquals("Liburan Bali", repo.updatePocket(env.user, pocket.id, "Liburan Bali", "", 3_000_000).ok().name)
        repo.withdraw(env.user, pocket.id, 100_000, "w1").ok()
        repo.deletePocket(env.user, pocket.id, "del1").ok()
        assertEquals(before, env.mainBalance())
        assertTrue(repo.pockets(env.user).isEmpty())
    }

    @Test
    fun `locked pocket needs confirmation and pin for early withdrawal`() = runBlocking<Unit> {
        val until = System.currentTimeMillis() + 30 * day
        val pocket = repo.createPocket(env.user, "Dana Darurat", "", null, LockRule.UntilDate(until)).ok()
        repo.deposit(env.user, pocket.id, 300_000, "d2").ok()
        repo.withdraw(env.user, pocket.id, 100_000, "w2").assertFailure("konfirmasi")
        repo.withdraw(env.user, pocket.id, 100_000, "w2", earlyWithdrawalConfirmed = true, pin = "000000")
            .assertFailure("PIN salah")
        repo.withdraw(env.user, pocket.id, 100_000, "w2", earlyWithdrawalConfirmed = true, pin = env.pin).ok()
        repo.deletePocket(env.user, pocket.id, "del2").assertFailure("terkunci")
    }

    @Test
    fun `target lock opens automatically when target reached`() = runBlocking<Unit> {
        val pocket = repo.createPocket(env.user, "Laptop", "", 200_000, LockRule.UntilTarget).ok()
        assertTrue(pocket.locked)
        repo.deposit(env.user, pocket.id, 150_000, "d3").ok()
        assertTrue(repo.pocket(env.user, pocket.id)!!.locked)
        repo.deposit(env.user, pocket.id, 50_000, "d4").ok()
        assertFalse(repo.pocket(env.user, pocket.id)!!.locked)
    }

    @Test
    fun `invalid pocket input rejected`() = runBlocking<Unit> {
        repo.createPocket(env.user, "", "", null, null).assertFailure("wajib")
        repo.createPocket(env.user, "A", "", null, LockRule.UntilTarget).assertFailure("target")
        repo.createPocket(env.user, "A", "", null, LockRule.UntilDate(0)).assertFailure("masa depan")
    }
}
