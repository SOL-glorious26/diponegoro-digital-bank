package com.diponegoro.digitalbank.history

import com.diponegoro.digitalbank.TestEnv
import com.diponegoro.digitalbank.assertFailure
import com.diponegoro.digitalbank.core.ledger.Direction
import com.diponegoro.digitalbank.core.ledger.TxCategory
import com.diponegoro.digitalbank.core.ledger.TxStatus
import com.diponegoro.digitalbank.core.notification.NotificationChannel
import com.diponegoro.digitalbank.core.notification.NotificationSettings
import com.diponegoro.digitalbank.core.notification.NotificationType
import com.diponegoro.digitalbank.history.data.DateRange
import com.diponegoro.digitalbank.history.data.HistoryFilter
import com.diponegoro.digitalbank.history.data.HistoryRepository
import com.diponegoro.digitalbank.history.data.LocalHistoryRepository
import com.diponegoro.digitalbank.ok
import com.diponegoro.digitalbank.profile.data.ProfileDraft
import com.diponegoro.digitalbank.profile.data.LocalProfileRepository
import com.diponegoro.digitalbank.transfer.data.LocalTransferRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryAndProfileTest {
    private val env = TestEnv()
    private val history = LocalHistoryRepository(env.ledger)
    private val profile = LocalProfileRepository(env.auth, env.notifications)

    @Test
    fun `history newest first with paging`() = runBlocking<Unit> {
        val page1 = history.page(env.user, HistoryFilter(), 1)
        assertEquals(HistoryRepository.PAGE_SIZE, page1.items.size)
        assertTrue(page1.hasMore)
        val times = page1.items.map { it.tx.createdAt }
        assertEquals(times.sortedDescending(), times)
        assertTrue(history.page(env.user, HistoryFilter(), 2).items.size > page1.items.size)
    }

    @Test
    fun `filters and search combine`() = runBlocking<Unit> {
        val filter = HistoryFilter(
            range = DateRange.ALL, category = TxCategory.TOPUP, direction = Direction.OUT,
            status = TxStatus.SUCCESS, query = "pulsa"
        )
        val items = history.page(env.user, filter, 10).items
        assertTrue(items.isNotEmpty())
        assertTrue(items.all { it.tx.category == TxCategory.TOPUP && it.tx.title.contains("Pulsa") })
        assertTrue(history.page(env.user, filter.copy(query = "tidak-ada-xyz"), 1).items.isEmpty())
    }

    @Test
    fun `monthly summary matches ledger transactions`() = runBlocking<Unit> {
        val series = history.monthSeries(env.user, 6)
        assertEquals(6, series.size)
        // Seed: gaji Rp8,5 jt tiap 30 hari selama 5 bulan → minimal 4 gaji jatuh di 6 bulan kalender terakhir.
        assertTrue(series.sumOf { it.income } >= 4 * 8_500_000L)
        assertTrue(series.all { it.expense >= 0 } && series.sumOf { it.expense } > 0)
    }

    @Test
    fun `detail exposes balanced journal`() = runBlocking<Unit> {
        val tx = history.recent(env.user, 1).first().tx
        val detail = history.detail(env.user, tx.id)!!
        assertEquals(0L, detail.journal.sumOf { it.amount })
        assertEquals(null, history.detail("orang-lain", tx.id))
    }

    @Test
    fun `profile change applies only after otp`() = runBlocking<Unit> {
        val pending = profile.requestProfileChange(env.user, ProfileDraft("demo", "baru@diponegoro.bank", "081234567890")).ok()
        assertEquals("baru@diponegoro.bank", pending.sentTo)
        profile.confirmProfileChange(env.user, pending, "000000").assertFailure()
        assertEquals("demo@diponegoro.bank", env.auth.getUser(env.user)!!.email)
        profile.confirmProfileChange(env.user, pending, pending.demoOtp).ok()
        assertEquals("baru@diponegoro.bank", env.auth.getUser(env.user)!!.email)
    }

    @Test
    fun `change pin and password`() = runBlocking<Unit> {
        profile.changePin(env.user, "123456", "654321", "654320").assertFailure("tidak cocok")
        profile.changePin(env.user, "123456", "654321", "654321").ok()
        env.guard.verifyPin(env.user, "654321").ok()
        profile.changePassword(env.user, "salah", "Baru12345", "Baru12345").assertFailure("lama salah")
        profile.changePassword(env.user, "Demo@1234", "Baru12345", "Baru12345").ok()
    }

    @Test
    fun `transaction notifications follow user settings`() = runBlocking<Unit> {
        val transfer = LocalTransferRepository(env.ledger, env.guard)
        profile.updateNotificationSettings(
            env.user, NotificationSettings(channels = setOf(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
        ).ok()
        val r = transfer.inquire(env.user, "BCA", "1234567890").ok()
        transfer.execute(env.user, transfer.quote(env.user, r, "10000", "").ok(), env.pin, "n1").ok()
        delay(100) // notifikasi diproses asinkron

        val inbox = env.notifications.list(env.user)
        assertTrue(inbox.any { it.type == NotificationType.OUTGOING })
        assertTrue(env.notifications.emails(env.user).isNotEmpty())
        assertTrue(env.pushed.isEmpty()) // push tidak dipilih

        profile.updateNotificationSettings(env.user, NotificationSettings(channels = emptySet()))
            .assertFailure("minimal satu")
    }
}
