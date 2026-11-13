package com.diponegoro.digitalbank.core.notification

import com.diponegoro.digitalbank.core.network.ApiClient
import com.diponegoro.digitalbank.core.network.IsoTime
import com.diponegoro.digitalbank.core.network.NotificationDto
import com.diponegoro.digitalbank.core.network.SettingsDto
import com.diponegoro.digitalbank.core.util.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Sumber notifikasi untuk layar Beranda, kotak notifikasi, dan pengaturan (FR-28, FR-29).
 *  - Demo   : [LocalNotificationSource] membungkus [NotificationCenter] yang mendengarkan ledger lokal.
 *  - Online : [RemoteNotificationSource] membaca /api/v1/me/notifications; notifikasi dibuat server
 *             secara asinkron setelah transaksi (SAD-DDB-001 ADR-08).
 */
interface NotificationSource {
    val revision: StateFlow<Long>
    suspend fun list(userId: String): List<AppNotification>
    suspend fun unreadCount(userId: String): Int
    suspend fun emails(userId: String): List<EmailMessage>
    suspend fun markRead(id: String)
    suspend fun markAllRead(userId: String)
    suspend fun settings(userId: String): NotificationSettings
    suspend fun updateSettings(userId: String, value: NotificationSettings): AppResult<Unit>
}

class LocalNotificationSource(private val center: NotificationCenter) : NotificationSource {
    override val revision: StateFlow<Long> get() = center.revision
    override suspend fun list(userId: String) = center.list(userId)
    override suspend fun unreadCount(userId: String) = center.unreadCount(userId)
    override suspend fun emails(userId: String) = center.emails(userId)
    override suspend fun markRead(id: String) = center.markRead(id)
    override suspend fun markAllRead(userId: String) = center.markAllRead(userId)
    override suspend fun settings(userId: String) = center.settings(userId)
    override suspend fun updateSettings(userId: String, value: NotificationSettings): AppResult<Unit> {
        if (value.channels.isEmpty()) return AppResult.Failure("Pilih minimal satu kanal notifikasi")
        center.updateSettings(userId, value)
        return AppResult.Success(Unit)
    }
}

class RemoteNotificationSource(
    private val client: ApiClient,
    private val push: PushSender?,
    private val onDataChanged: () -> Unit
) : NotificationSource {

    private val _revision = MutableStateFlow(0L)
    override val revision: StateFlow<Long> = _revision.asStateFlow()
    private fun bump() = _revision.update { it + 1 }

    private fun NotificationDto.toModel(userId: String) = AppNotification(
        id = id, userId = userId,
        type = runCatching { NotificationType.valueOf(type) }.getOrDefault(NotificationType.INFO),
        title = title, message = message, createdAt = IsoTime.parse(createdAt), read = read, transactionId = transactionId
    )

    override suspend fun list(userId: String): List<AppNotification> =
        when (val r = client.call { notifications(0) }) {
            is AppResult.Success -> r.data.items.map { it.toModel(userId) }
            is AppResult.Failure -> emptyList()
        }

    override suspend fun unreadCount(userId: String): Int =
        when (val r = client.call { notifications(0) }) {
            is AppResult.Success -> r.data.unread.toInt()
            is AppResult.Failure -> 0
        }

    override suspend fun emails(userId: String): List<EmailMessage> =
        when (val r = client.call { emails() }) {
            is AppResult.Success -> r.data.map { EmailMessage(it.id, userId, it.to, it.subject, it.body, IsoTime.parse(it.sentAt)) }
            is AppResult.Failure -> emptyList()
        }

    override suspend fun markRead(id: String) { client.call { markRead(id) }; bump() }
    override suspend fun markAllRead(userId: String) { client.call { markAllRead() }; bump() }

    override suspend fun settings(userId: String): NotificationSettings =
        when (val r = client.call { notificationSettings() }) {
            is AppResult.Success -> NotificationSettings(
                types = r.data.types.mapNotNull { runCatching { NotificationType.valueOf(it) }.getOrNull() }.toSet(),
                channels = r.data.channels.mapNotNull { runCatching { NotificationChannel.valueOf(it) }.getOrNull() }.toSet()
            )
            is AppResult.Failure -> NotificationSettings()
        }

    override suspend fun updateSettings(userId: String, value: NotificationSettings): AppResult<Unit> =
        when (val r = client.call { updateNotificationSettings(SettingsDto(value.types.map { it.name }, value.channels.map { it.name })) }) {
            is AppResult.Failure -> r
            is AppResult.Success -> { bump(); AppResult.Success(Unit) }
        }

    private var poller: Job? = null

    /**
     * Mengambil antrean push server secara berkala (FCM belum dipakai, lihat SAD-DDB-001 Tabel 6.2).
     * Notifikasi baru → push Android lokal + sinyal data berubah (mis. dana masuk dari pengguna lain).
     */
    fun startPolling(scope: CoroutineScope, userId: () -> String?, intervalMs: Long = 15_000) {
        if (poller?.isActive == true) return
        poller = scope.launch {
            while (isActive) {
                val uid = userId()
                if (uid != null) {
                    val r = client.call { pendingPush() }
                    if (r is AppResult.Success && r.data.isNotEmpty()) {
                        r.data.forEach { n -> runCatching { push?.send(n.toModel(uid)) } }
                        bump()
                        onDataChanged()
                    }
                }
                delay(intervalMs)
            }
        }
    }
}
