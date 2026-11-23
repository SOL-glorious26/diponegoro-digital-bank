package com.diponegoro.digitalbank.core.di

import android.content.Context
import com.diponegoro.digitalbank.authentication.di.AuthModule
import com.diponegoro.digitalbank.core.ledger.DemoData
import com.diponegoro.digitalbank.core.ledger.Ledger
import com.diponegoro.digitalbank.core.notification.AndroidPushSender
import com.diponegoro.digitalbank.core.notification.LocalNotificationSource
import com.diponegoro.digitalbank.core.notification.NotificationCenter
import com.diponegoro.digitalbank.core.notification.NotificationSource
import com.diponegoro.digitalbank.core.notification.RemoteNotificationSource
import com.diponegoro.digitalbank.core.security.TransactionGuard
import com.diponegoro.digitalbank.core.storage.InMemoryStore
import com.diponegoro.digitalbank.core.storage.KeyValueStore
import com.diponegoro.digitalbank.core.storage.SharedPrefsStore
import com.diponegoro.digitalbank.BuildConfig
import com.diponegoro.digitalbank.core.network.ApiClient
import com.diponegoro.digitalbank.core.network.InMemoryTokenStore
import com.diponegoro.digitalbank.core.network.SecureTokenStore
import com.diponegoro.digitalbank.core.network.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Komponen lintas modul (MDTA 2.2): Financial Transaction Core, notifikasi, keamanan transaksi. */
object CoreModule {

    private var appContext: Context? = null

    /** Scope aplikasi untuk pekerjaan asinkron (mis. pengiriman notifikasi). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val ledger: Ledger by lazy { Ledger().also { DemoData.seed(it) } }

    val guard: TransactionGuard by lazy { TransactionGuard(AuthModule.repository) }

    val notifications: NotificationCenter by lazy {
        NotificationCenter(
            ledger = ledger,
            scope = appScope,
            emailOf = { userId -> AuthModule.repository.getUser(userId)?.email },
            push = appContext?.let { AndroidPushSender(it) }
        )
    }

    /** Sumber notifikasi untuk UI: ledger lokal (demo) atau server + polling push (online). */
    val notificationSource: NotificationSource by lazy {
        if (online) {
            RemoteNotificationSource(api, appContext?.let { AndroidPushSender(it) }, ::dataChanged)
                .also { it.startPolling(appScope, { tokens.session()?.userId }) }
        } else LocalNotificationSource(notifications)
    }

    val preferences: KeyValueStore by lazy {
        appContext?.let { SharedPrefsStore(it) } ?: InMemoryStore()
    }

    // ---------------------------------------------------------------- Varian online (Fase 5)

    /** true pada flavor "online": data dari backend bersama, bukan ledger in-memory. */
    val online: Boolean get() = onlineOverride ?: BuildConfig.ONLINE

    /** Untuk uji: memaksa mode tanpa mengganti flavor. */
    @Volatile var onlineOverride: Boolean? = null

    /** Base URL backend; dapat diganti saat uji (MockWebServer). */
    @Volatile var baseUrl: String = BuildConfig.BASE_URL

    val tokens: TokenStore by lazy { appContext?.let { SecureTokenStore.create(it) } ?: InMemoryTokenStore() }

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Dipancarkan saat refresh token ditolak server → UI kembali ke layar login. */
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    val api: ApiClient by lazy { ApiClient(baseUrl, tokens, onSessionExpired = { _sessionExpired.tryEmit(Unit) }) }

    private val remoteRevision = MutableStateFlow(0L)

    /**
     * Sinyal "data berubah" untuk ViewModel. Demo: revisi ledger lokal.
     * Online: dinaikkan setiap mutasi berhasil dan saat ada notifikasi baru dari server.
     */
    val revision: StateFlow<Long> get() = if (online) remoteRevision.asStateFlow() else ledger.revision

    fun dataChanged() = remoteRevision.update { it + 1 }

    /** Dipanggil sekali dari Application.onCreate. */
    fun init(context: Context) {
        appContext = context.applicationContext
        if (!online) notifications // mulai mendengarkan event ledger sejak awal
    }

    /** Saldo awal simulasi untuk pengguna baru hasil registrasi. */
    const val NEW_USER_OPENING_BALANCE = 50_000L

    /** Memastikan pengguna memiliki rekening utama. Aman dipanggil berulang. */
    suspend fun ensureCustomer(userId: String, displayName: String) =
        ledger.ensureMainAccount(userId, displayName.uppercase(), NEW_USER_OPENING_BALANCE)
}
