package com.diponegoro.digitalbank

import com.diponegoro.digitalbank.authentication.data.repository.MockAuthRepository
import com.diponegoro.digitalbank.core.ledger.DemoData
import com.diponegoro.digitalbank.core.ledger.Ledger
import com.diponegoro.digitalbank.core.notification.AppNotification
import com.diponegoro.digitalbank.core.notification.NotificationCenter
import com.diponegoro.digitalbank.core.security.TransactionGuard
import com.diponegoro.digitalbank.core.storage.InMemoryStore
import com.diponegoro.digitalbank.core.util.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue

/** Lingkungan uji lengkap: auth demo + ledger ber-seed + notifikasi sinkron. */
class TestEnv {
    val auth = MockAuthRepository(networkDelayMs = 0)
    val ledger = Ledger().also { DemoData.seed(it) }
    val guard = TransactionGuard(auth)
    val prefs = InMemoryStore()
    val pushed = mutableListOf<AppNotification>()
    val notifications = NotificationCenter(
        ledger, CoroutineScope(Dispatchers.Unconfined),
        emailOf = { auth.getUser(it)?.email }, push = { pushed.add(it) }
    )
    val user = DemoData.DEMO_USER_ID
    val pin = "123456"

    fun mainBalance(userId: String = user) = ledger.balance(ledger.mainAccount(userId)!!.id)
}

fun <T> AppResult<T>.ok(): T = (this as? AppResult.Success)?.data
    ?: throw AssertionError("Expected Success but was $this")

fun AppResult<*>.assertFailure(contains: String? = null) {
    assertTrue("Expected Failure but was $this", this is AppResult.Failure)
    if (contains != null) {
        val msg = (this as AppResult.Failure).message
        assertTrue("Message '$msg' should contain '$contains'", msg.contains(contains, ignoreCase = true))
    }
}
