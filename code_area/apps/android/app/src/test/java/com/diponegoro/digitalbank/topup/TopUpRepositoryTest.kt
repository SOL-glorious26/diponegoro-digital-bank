package com.diponegoro.digitalbank.topup

import com.diponegoro.digitalbank.TestEnv
import com.diponegoro.digitalbank.assertFailure
import com.diponegoro.digitalbank.ok
import com.diponegoro.digitalbank.topup.data.LocalTopUpRepository
import com.diponegoro.digitalbank.topup.domain.Biller
import com.diponegoro.digitalbank.topup.domain.EWallet
import com.diponegoro.digitalbank.topup.domain.TopUpCatalog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopUpRepositoryTest {
    private val env = TestEnv()
    private val repo = LocalTopUpRepository(env.ledger, env.guard)

    @Test
    fun `operator detected from prefix`() = runBlocking<Unit> {
        assertEquals("Telkomsel", TopUpCatalog.detectOperator("081234567890")?.name)
        assertEquals("XL", TopUpCatalog.detectOperator("087712345678")?.name)
        repo.orderPulsa("0712345678", 10_000).assertFailure("08")
    }

    @Test
    fun `pulsa debits nominal plus fee after valid pin`() = runBlocking<Unit> {
        val order = repo.orderPulsa("081234567890", 50_000).ok()
        val before = env.mainBalance()
        repo.execute(env.user, order, "000000", "u1").assertFailure("PIN salah")
        assertEquals(before, env.mainBalance())
        val tx = repo.execute(env.user, order, env.pin, "u1").ok()
        assertEquals(before - 50_000 - TopUpCatalog.PULSA_FEE, env.mainBalance())
        assertTrue(tx.extra.containsKey("Serial number"))
    }

    @Test
    fun `data package must match operator`() = runBlocking<Unit> {
        val xlPackage = TopUpCatalog.dataPackages(TopUpCatalog.operators.first { it.code == "XL" }).first()
        repo.orderData("081234567890", xlPackage).assertFailure("tidak tersedia")
    }

    @Test
    fun `pln token shown after success`() = runBlocking<Unit> {
        repo.inquirePln("123").assertFailure("11–12")
        val customer = repo.inquirePln("12345678901").ok()
        val order = repo.orderPln(customer, 100_000).ok()
        val tx = repo.execute(env.user, order, env.pin, "u2").ok()
        val token = tx.extra.getValue("Token")
        assertEquals(24, token.length) // 20 digit + 4 pemisah
    }

    @Test
    fun `bill can only be paid once per period`() = runBlocking<Unit> {
        repo.inquireBill(Biller.PDAM, "1234560000").assertFailure("tidak ditemukan")
        val bill = repo.inquireBill(Biller.PDAM, "12345678").ok()
        repo.execute(env.user, repo.orderBill(bill).ok(), env.pin, "u3").ok()
        repo.inquireBill(Biller.PDAM, "12345678").assertFailure("lunas")
        repo.execute(env.user, repo.orderBill(bill).ok(), env.pin, "u4").assertFailure("sudah dibayar")
    }

    @Test
    fun `ewallet amount range enforced`() = runBlocking<Unit> {
        repo.orderEwallet(EWallet.DANA, "081234567890", "5000").assertFailure()
        assertEquals(100_000L, repo.orderEwallet(EWallet.DANA, "081234567890", "100000").ok().amount)
    }
}
