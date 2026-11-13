package com.diponegoro.digitalbank.core.notification

import com.diponegoro.digitalbank.core.ledger.Direction
import com.diponegoro.digitalbank.core.ledger.Ledger
import com.diponegoro.digitalbank.core.ledger.LedgerEvent
import com.diponegoro.digitalbank.core.ledger.TxCategory
import com.diponegoro.digitalbank.core.ledger.TxStatus
import com.diponegoro.digitalbank.core.util.formatRupiah
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

enum class NotificationType(val label: String) {
    INCOMING("Transaksi masuk"),
    OUTGOING("Transaksi keluar"),
    SECURITY("Keamanan akun"),
    INFO("Info & promo")
}

enum class NotificationChannel(val label: String) {
    PUSH("Push notification"),
    IN_APP("Dalam aplikasi"),
    EMAIL("Email")
}

data class NotificationSettings(
    val types: Set<NotificationType> = NotificationType.entries.toSet(),
    val channels: Set<NotificationChannel> = setOf(NotificationChannel.PUSH, NotificationChannel.IN_APP)
)

data class AppNotification(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val createdAt: Long,
    val read: Boolean = false,
    val transactionId: String? = null
)

/** Email disimulasikan: disimpan ke outbox dan dapat dilihat di aplikasi. */
data class EmailMessage(
    val id: String,
    val userId: String,
    val to: String,
    val subject: String,
    val body: String,
    val sentAt: Long
)

fun interface PushSender {
    fun send(notification: AppNotification)
}

/**
 * FR-29: notifikasi transaksi masuk & keluar lewat kanal yang dipilih pengguna (FR-28).
 * Pengiriman asinkron: ledger hanya memancarkan event, pusat notifikasi memprosesnya di coroutine terpisah
 * sehingga transaksi tidak menunggu notifikasi terkirim.
 */
class NotificationCenter(
    ledger: Ledger,
    scope: CoroutineScope,
    private val emailOf: suspend (userId: String) -> String?,
    private val push: PushSender?,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val settings = ConcurrentHashMap<String, NotificationSettings>()
    private val inbox = CopyOnWriteArrayList<AppNotification>()
    private val outbox = CopyOnWriteArrayList<EmailMessage>()

    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision.asStateFlow()

    init {
        scope.launch { ledger.events.collect { handle(it) } }
    }

    fun settings(userId: String): NotificationSettings = settings[userId] ?: NotificationSettings()

    fun updateSettings(userId: String, value: NotificationSettings) {
        settings[userId] = value
        bump()
    }

    fun list(userId: String): List<AppNotification> =
        inbox.filter { it.userId == userId }.sortedByDescending { it.createdAt }

    fun unreadCount(userId: String): Int = inbox.count { it.userId == userId && !it.read }

    fun emails(userId: String): List<EmailMessage> =
        outbox.filter { it.userId == userId }.sortedByDescending { it.sentAt }

    fun markRead(id: String) {
        val index = inbox.indexOfFirst { it.id == id }
        if (index >= 0) inbox[index] = inbox[index].copy(read = true)
        bump()
    }

    fun markAllRead(userId: String) {
        inbox.replaceAll { if (it.userId == userId) it.copy(read = true) else it }
        bump()
    }

    /** Dipakai juga oleh modul lain untuk notifikasi keamanan (ubah password/PIN, profil). */
    suspend fun notify(
        userId: String, type: NotificationType, title: String, message: String, transactionId: String? = null
    ) {
        val pref = settings(userId)
        if (type !in pref.types) return
        val notification = AppNotification(
            id = "notif-${UUID.randomUUID()}",
            userId = userId,
            type = type,
            title = title,
            message = message,
            createdAt = clock(),
            transactionId = transactionId
        )
        // Riwayat notifikasi selalu tersimpan; kanal "dalam aplikasi" menentukan apakah ditandai belum dibaca.
        inbox.add(if (NotificationChannel.IN_APP in pref.channels) notification else notification.copy(read = true))
        if (NotificationChannel.PUSH in pref.channels) runCatching { push?.send(notification) }
        if (NotificationChannel.EMAIL in pref.channels) {
            emailOf(userId)?.takeIf { it.isNotBlank() }?.let { address ->
                outbox.add(
                    EmailMessage("mail-${UUID.randomUUID()}", userId, address, title, message, clock())
                )
            }
        }
        bump()
    }

    private suspend fun handle(event: LedgerEvent) {
        val tx = event.tx
        if (tx.category == TxCategory.POCKET) return // perpindahan antar-kantong milik sendiri
        val debit = tx.debitUserId
        val credit = tx.creditUserId

        when (event) {
            is LedgerEvent.Posted -> {
                if (debit != null && tx.directionFor(debit) == Direction.OUT) {
                    val status = if (tx.status == TxStatus.PROCESSING) " sedang diproses" else " berhasil"
                    notify(
                        debit, NotificationType.OUTGOING, "${tx.title}$status",
                        "${formatRupiah(tx.total)} ke ${tx.toName}. Ref ${tx.reference}", tx.id
                    )
                }
                if (credit != null && credit != debit && tx.status == TxStatus.SUCCESS) {
                    notify(
                        credit, NotificationType.INCOMING, "Dana masuk ${formatRupiah(tx.amount)}",
                        "Dari ${tx.fromName} (${tx.title}). Ref ${tx.reference}", tx.id
                    )
                }
            }
            is LedgerEvent.StatusChanged -> {
                if (debit != null) {
                    val (title, body) = when (tx.status) {
                        TxStatus.FAILED -> "${tx.title} gagal" to
                            "${tx.statusReason ?: "Transaksi ditolak"}. Dana ${formatRupiah(tx.total)} dikembalikan ke rekening Anda."
                        else -> "${tx.title} berhasil" to "${formatRupiah(tx.total)} ke ${tx.toName}. Ref ${tx.reference}"
                    }
                    notify(debit, NotificationType.OUTGOING, title, body, tx.id)
                }
                if (credit != null && credit != debit && tx.status == TxStatus.SUCCESS) {
                    notify(
                        credit, NotificationType.INCOMING, "Dana masuk ${formatRupiah(tx.amount)}",
                        "Dari ${tx.fromName}. Ref ${tx.reference}", tx.id
                    )
                }
            }
        }
    }

    private fun bump() {
        _revision.value = _revision.value + 1
    }
}
