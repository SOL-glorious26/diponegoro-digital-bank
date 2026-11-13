package com.diponegoro.digitalbank.online

import com.diponegoro.digitalbank.account.data.RemoteAccountRepository
import com.diponegoro.digitalbank.authentication.data.repository.RemoteAuthRepository
import com.diponegoro.digitalbank.authentication.domain.model.AccountStatus
import com.diponegoro.digitalbank.authentication.domain.model.AuthResult
import com.diponegoro.digitalbank.authentication.domain.model.EkycStatus
import com.diponegoro.digitalbank.authentication.domain.model.OtpPurpose
import com.diponegoro.digitalbank.core.ledger.Direction
import com.diponegoro.digitalbank.core.ledger.TxCategory
import com.diponegoro.digitalbank.core.ledger.TxStatus
import com.diponegoro.digitalbank.core.network.ApiClient
import com.diponegoro.digitalbank.core.network.InMemoryTokenStore
import com.diponegoro.digitalbank.core.notification.RemoteNotificationSource
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.history.data.HistoryFilter
import com.diponegoro.digitalbank.history.data.RemoteHistoryRepository
import com.diponegoro.digitalbank.ok
import com.diponegoro.digitalbank.payment.data.RemotePaymentRepository
import com.diponegoro.digitalbank.topup.data.RemoteTopUpRepository
import com.diponegoro.digitalbank.transfer.data.RemoteTransferRepository
import com.diponegoro.digitalbank.virtualcard.data.repository.RemoteVirtualCardRepository
import com.diponegoro.digitalbank.virtualcard.domain.model.CardResult
import com.diponegoro.digitalbank.virtualcard.domain.model.CardScope
import com.diponegoro.digitalbank.virtualcard.domain.model.CardStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

/**
 * Uji end-to-end varian online terhadap backend sungguhan (DAFTAR_PEKERJAAN 6.3).
 * Dilewati kecuali dijalankan dengan:  ./gradlew testOnlineDebugUnitTest -Pddb.liveUrl=http://localhost:8080/
 * Backend lokal: cd backend; .\run-local.ps1 -H2   (akun demo & data simulasi dibuat otomatis)
 */
class LiveBackendTest {
    private val url: String? = System.getProperty("ddb.liveUrl")

    private fun <T> AuthResult<T>.ok(): T = (this as? AuthResult.Success)?.data ?: throw AssertionError("Expected Success but was $this")
    private fun <T> CardResult<T>.ok(): T = (this as? CardResult.Success)?.data ?: throw AssertionError("Expected Success but was $this")

    @Test
    fun `nasabah baru dari registrasi sampai transaksi di backend sungguhan`() = runBlocking<Unit> {
        assumeTrue("Set -Pddb.liveUrl untuk menjalankan uji backend sungguhan", url != null)
        val tokens = InMemoryTokenStore()
        val client = ApiClient(url!!, tokens)
        val auth = RemoteAuthRepository(client, tokens, deviceName = "JVM test")
        val username = "live" + UUID.randomUUID().toString().take(6)

        // FR-01: registrasi + OTP (demoCode) → akun aktif
        val user = auth.register(username, "$username@mail.test", "", "Rahasia123").ok()
        val otp = auth.generateOtp(user.userId, OtpPurpose.REGISTRATION).ok()
        assertTrue(otp.matches(Regex("\\d{6}")))
        auth.verifyOtp(user.userId, OtpPurpose.REGISTRATION, otp).ok()

        // FR-02: e-KYC simulasi disetujui setelah ±3 detik
        auth.submitEkyc(user.userId, "ktp_3374010101" + (100000..999999).random(), "selfie.jpg").ok()
        var ekyc = auth.checkEkycStatus(user.userId).ok().status
        repeat(10) { if (ekyc == EkycStatus.PROCESSING) { delay(1_000); ekyc = auth.checkEkycStatus(user.userId).ok().status } }
        assertEquals(EkycStatus.APPROVED, ekyc)

        // FR-03/FR-04: login, buat PIN, verifikasi PIN (endpoint /auth/pin/verify)
        val session = auth.login(username, "Rahasia123", "ignored").ok()
        assertEquals(AccountStatus.FINANCIAL_ENABLED, auth.getUser(session.userId)!!.accountStatus)
        auth.createPin(session.userId, "246810").ok()
        auth.validatePin(session.userId, "246810").ok()
        assertTrue((auth.validatePin(session.userId, "000000") as AuthResult.Failure).message.contains("Sisa percobaan"))
        auth.validatePin(session.userId, "246810").ok() // reset hitungan salah

        val me = session.userId
        val changed = mutableListOf<String>()
        val accounts = RemoteAccountRepository(client) { changed += "account" }
        val transfer = RemoteTransferRepository(client) { changed += "transfer" }
        val history = RemoteHistoryRepository(client)
        val opening = accounts.overview(me).ok().balance
        assertTrue("saldo awal pengguna baru", opening > 0)

        // FR-10/FR-12 + BR-04: transfer sesama bank ke rekening demo, kirim ulang dengan kunci yang sama
        val recipient = transfer.inquire(me, "DDB", "1400123456").ok()
        val quote = transfer.quote(me, recipient, "150000", "uji live").ok()
        val key = UUID.randomUUID().toString()
        val tx = transfer.execute(me, quote, "246810", key).ok()
        val replay = transfer.execute(me, quote, "246810", key).ok()
        assertEquals(tx.id, replay.id)
        assertEquals(TxStatus.SUCCESS, tx.status)
        assertEquals(Direction.OUT, tx.directionFor(me))
        assertEquals(opening - 150_000, accounts.overview(me).ok().balance)

        // UC-01 A6: antarbank ditolak → dana dikembalikan
        val rejected = transfer.execute(me, transfer.quote(me, transfer.inquire(me, "BRI", "9999999999").ok(), "50000", "").ok(),
            "246810", UUID.randomUUID().toString()).ok()
        assertEquals(TxStatus.FAILED, rejected.status)
        assertEquals(opening - 150_000, accounts.overview(me).ok().balance)

        // FR-23/FR-24: riwayat & detail berjurnal
        val page = history.page(me, HistoryFilter(), 1)
        assertTrue(page.items.any { it.tx.id == tx.id })
        val detail = history.detail(me, tx.id)!!
        assertEquals(0L, detail.journal.sumOf { it.amount })

        // FR-08: kantong
        val pocket = accounts.createPocket(me, "Dana darurat", "cadangan", 1_000_000, null).ok()
        accounts.deposit(me, pocket.id, 100_000, UUID.randomUUID().toString()).ok()
        assertEquals(100_000L, accounts.pocket(me, pocket.id)!!.balance)

        // FR-15: bayar QRIS contoh (statis) dengan nominal
        val payment = RemotePaymentRepository(client)
        val demo = payment.demoQrs().first { it.label.contains("statis", ignoreCase = true) }
        val scanned = payment.scan(me, demo.raw).ok()
        val qris = payment.pay(me, scanned, "25000", "246810", UUID.randomUUID().toString()).ok()
        assertEquals(TxCategory.QRIS, qris.category)

        // FR-18: pulsa
        val topUp = RemoteTopUpRepository(client)
        val order = topUp.orderPulsa("081234567890", 20_000).ok()
        assertEquals(TxCategory.TOPUP, topUp.execute(me, order, "246810", UUID.randomUUID().toString()).ok().category)

        // FR-21/FR-22: kartu virtual, detail dengan PIN, bekukan menolak transaksi
        val cards = RemoteVirtualCardRepository(client)
        val card = cards.createCard(me, "", CardScope.DOMESTIC).ok()
        val details = cards.revealDetails(me, card.cardId, "246810")!!.ok()
        assertEquals(16, details.cardNumber.length)
        assertEquals(CardStatus.FROZEN, cards.updateStatus(me, card.cardId, CardStatus.FROZEN).ok().status)
        assertTrue(cards.authorizeTransaction(me, card.cardId, 10_000, false) is CardResult.Failure)

        // FR-29: notifikasi transaksi dibuat server secara asinkron
        delay(500)
        val inbox = RemoteNotificationSource(client, null) {}.list(me)
        assertTrue("notifikasi transaksi keluar", inbox.isNotEmpty())

        assertTrue(changed.isNotEmpty())
        assertNotNull(tokens.session())
        auth.logout(session.sessionId)
        assertEquals(null, tokens.session())
        assertTrue(accounts.overview(me) is AppResult.Failure)
    }
}
