import sys, subprocess
root = sys.argv[1]; gen = sys.argv[2]
P = lambda p: root + '/' + p
subprocess.check_call([sys.executable, '-I', gen + '/localize.py', P('profile/data/ProfileRepository.kt'), 'ProfileRepository',
                       'profile,requestProfileChange,confirmProfileChange,resendOtp,updatePhoto,changePassword,changePin,'
                       'isBiometricEnabled,setBiometric,notificationSettings,updateNotificationSettings'])
lp = P('profile/data/LocalProfileRepository.kt')
s = open(lp, encoding='utf8').read()
start = s.index('data class ProfileDraft'); end = s.index('/** Modul Profile & Notification')
models = s[start:end]; s = s[:start] + s[end:]
for a, b in [
    ('override suspend fun isBiometricEnabled(userId: String) = auth.isBiometricEnabled(userId)',
     'override suspend fun isBiometricEnabled(userId: String): Boolean = auth.isBiometricEnabled(userId)'),
    ('override suspend fun notificationSettings(userId: String): NotificationSettings = notifications.settings(userId)',
     'override suspend fun notificationSettings(userId: String): NotificationSettings = notifications.settings(userId)'),
]:
    assert a in s, a[:60]; s = s.replace(a, b)
# validasi dipakai juga oleh RemoteProfileRepository
s = s.replace('    private fun validate(d: ProfileDraft): String? = when {', '    private fun validate(d: ProfileDraft): String? = validateDraft(d)\n\n    private fun unused(d: ProfileDraft): String? = when {')
open(lp, 'w', encoding='utf8', newline='\n').write(s)

open(P('profile/data/ProfileRepository.kt'), 'w', encoding='utf8', newline='\n').write('''package com.diponegoro.digitalbank.profile.data

import com.diponegoro.digitalbank.authentication.domain.model.User
import com.diponegoro.digitalbank.authentication.domain.usecase.AuthValidators
import com.diponegoro.digitalbank.core.notification.NotificationSettings
import com.diponegoro.digitalbank.core.util.AppResult

''' + models + '''/**
 * Modul Profile & Notification (FR-27 – FR-29). Implementasi:
 *  - [LocalProfileRepository]  : repository auth in-memory + NotificationCenter (varian demo)
 *  - [RemoteProfileRepository] : backend /api/v1/me (varian online)
 */
interface ProfileRepository {
    suspend fun profile(userId: String): User?
    suspend fun requestProfileChange(userId: String, draft: ProfileDraft): AppResult<PendingProfileChange>
    suspend fun confirmProfileChange(userId: String, pending: PendingProfileChange, code: String): AppResult<User>
    suspend fun resendOtp(userId: String): AppResult<String>
    suspend fun updatePhoto(userId: String, uri: String?): AppResult<User>
    suspend fun changePassword(userId: String, old: String, new: String, confirm: String): AppResult<Unit>
    suspend fun changePin(userId: String, old: String, new: String, confirm: String): AppResult<Unit>
    suspend fun isBiometricEnabled(userId: String): Boolean
    suspend fun setBiometric(userId: String, enabled: Boolean): AppResult<Unit>
    suspend fun notificationSettings(userId: String): NotificationSettings
    suspend fun updateNotificationSettings(userId: String, settings: NotificationSettings): AppResult<Unit>
}

/** Validasi draf profil (sama untuk kedua varian). */
fun validateDraft(d: ProfileDraft): String? = when {
    d.username.length < 4 -> "Username minimal 4 karakter"
    d.email.isBlank() && d.phone.isBlank() -> "Isi email atau nomor telepon"
    d.email.isNotBlank() && !AuthValidators.isValidEmail(d.email) -> "Format email tidak valid"
    d.phone.isNotBlank() && !AuthValidators.isValidPhone(d.phone) -> "Nomor telepon harus 9–15 digit"
    else -> null
}
''')

open(P('profile/data/RemoteProfileRepository.kt'), 'w', encoding='utf8', newline='\n').write('''package com.diponegoro.digitalbank.profile.data

import com.diponegoro.digitalbank.authentication.domain.model.AuthResult
import com.diponegoro.digitalbank.authentication.domain.model.User
import com.diponegoro.digitalbank.authentication.domain.repository.AuthRepository
import com.diponegoro.digitalbank.authentication.domain.usecase.AuthValidators
import com.diponegoro.digitalbank.core.network.ApiClient
import com.diponegoro.digitalbank.core.network.OtpCodeRequest
import com.diponegoro.digitalbank.core.network.ProfileDraftDto
import com.diponegoro.digitalbank.core.network.toUser
import com.diponegoro.digitalbank.core.notification.NotificationSettings
import com.diponegoro.digitalbank.core.notification.NotificationSource
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.core.util.map

/**
 * Profile lewat backend: OTP perubahan profil dibuat & diverifikasi server (FR-27), notifikasi
 * keamanan dibuat server, pengaturan notifikasi disimpan server (FR-28).
 */
class RemoteProfileRepository(
    private val client: ApiClient,
    private val auth: AuthRepository,
    private val notifications: NotificationSource
) : ProfileRepository {

    @Volatile private var lastDraft: ProfileDraft? = null

    private fun <T> AuthResult<T>.app(): AppResult<T> = when (this) {
        is AuthResult.Success -> AppResult.Success(data)
        is AuthResult.Failure -> AppResult.Failure(message)
    }

    override suspend fun profile(userId: String): User? = auth.getUser(userId)

    override suspend fun requestProfileChange(userId: String, draft: ProfileDraft): AppResult<PendingProfileChange> {
        val clean = ProfileDraft(draft.username.trim(), draft.email.trim(), draft.phone.trim())
        validateDraft(clean)?.let { return AppResult.Failure(it) }
        lastDraft = clean
        return client.call { requestProfileChange(ProfileDraftDto(clean.username, clean.email.ifBlank { null }, clean.phone.ifBlank { null })) }
            .map { PendingProfileChange(clean, it.sentTo.orEmpty(), it.demoCode.orEmpty()) }
    }

    override suspend fun confirmProfileChange(userId: String, pending: PendingProfileChange, code: String): AppResult<User> {
        if (!AuthValidators.isValidOtp(code)) return AppResult.Failure("OTP harus 6 digit angka")
        return client.call { confirmProfileChange(OtpCodeRequest(code)) }.map { it.toUser() }
    }

    override suspend fun resendOtp(userId: String): AppResult<String> {
        val draft = lastDraft ?: return AppResult.Failure("Tidak ada perubahan profil yang menunggu OTP")
        return requestProfileChange(userId, draft).map { it.demoOtp }
    }

    override suspend fun updatePhoto(userId: String, uri: String?): AppResult<User> = auth.updatePhoto(userId, uri).app()

    override suspend fun changePassword(userId: String, old: String, new: String, confirm: String): AppResult<Unit> {
        if (!AuthValidators.isStrongPassword(new)) return AppResult.Failure("Password minimal 8 karakter dan mengandung huruf serta angka")
        if (new != confirm) return AppResult.Failure("Konfirmasi password tidak cocok")
        return auth.changePassword(userId, old, new).app()
    }

    override suspend fun changePin(userId: String, old: String, new: String, confirm: String): AppResult<Unit> {
        if (!AuthValidators.isValidPin(old)) return AppResult.Failure("PIN lama harus 6 digit angka")
        if (!AuthValidators.isValidPin(new)) return AppResult.Failure("PIN baru harus 6 digit angka")
        if (new != confirm) return AppResult.Failure("Konfirmasi PIN tidak cocok")
        return auth.changePin(userId, old, new).app()
    }

    override suspend fun isBiometricEnabled(userId: String): Boolean = auth.isBiometricEnabled(userId)

    override suspend fun setBiometric(userId: String, enabled: Boolean): AppResult<Unit> = auth.setBiometricEnabled(userId, enabled).app()

    override suspend fun notificationSettings(userId: String): NotificationSettings = notifications.settings(userId)

    override suspend fun updateNotificationSettings(userId: String, settings: NotificationSettings): AppResult<Unit> {
        if (settings.channels.isEmpty()) return AppResult.Failure("Pilih minimal satu kanal notifikasi")
        return notifications.updateSettings(userId, settings)
    }
}
''')

# mapper UserDto -> User (publik)
mp = P('core/network/Mappers.kt')
s = open(mp, encoding='utf8').read()
s = s.replace('import com.diponegoro.digitalbank.core.ledger.JournalLine',
              'import com.diponegoro.digitalbank.authentication.domain.model.AccountStatus\nimport com.diponegoro.digitalbank.authentication.domain.model.User\nimport com.diponegoro.digitalbank.core.ledger.JournalLine')
s += '''
fun UserDto.toUser() = User(
    userId = id, email = email.orEmpty(), phoneNumber = phoneNumber.orEmpty(), username = username,
    accountStatus = runCatching { AccountStatus.valueOf(status) }.getOrDefault(AccountStatus.ACTIVE),
    photoUri = photoUrl
)
'''
open(mp, 'w', encoding='utf8', newline='\n').write(s)

vp = P('profile/presentation/ProfileViewModels.kt')
s = open(vp, encoding='utf8').read()
pairs = [
    ('''    private val _state = MutableStateFlow(NotificationSettingsUiState(repo.notificationSettings(userId)))
    val state: StateFlow<NotificationSettingsUiState> = _state.asStateFlow()

    private fun save(settings: NotificationSettings) {
        when (val r = repo.updateNotificationSettings(userId, settings)) {
            is AppResult.Failure -> _state.update { it.copy(error = r.message, info = null) }
            is AppResult.Success -> _state.update { it.copy(settings = settings, error = null, info = "Pengaturan disimpan") }
        }
    }''', '''    private val _state = MutableStateFlow(NotificationSettingsUiState())
    val state: StateFlow<NotificationSettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = repo.notificationSettings(userId)
            _state.update { it.copy(settings = settings) }
        }
    }

    private fun save(settings: NotificationSettings) {
        viewModelScope.launch {
            when (val r = repo.updateNotificationSettings(userId, settings)) {
                is AppResult.Failure -> _state.update { it.copy(error = r.message, info = null) }
                is AppResult.Success -> _state.update { it.copy(settings = settings, error = null, info = "Pengaturan disimpan") }
            }
        }
    }'''),
    ('class NotificationsViewModel(private val userId: String, private val center: NotificationCenter) : ViewModel() {',
     'class NotificationsViewModel(private val userId: String, private val center: NotificationSource) : ViewModel() {'),
    ('''            center.revision.collect {
                _state.update { NotificationsUiState(center.list(userId), center.emails(userId)) }
            }''', '''            center.revision.collect {
                val inbox = center.list(userId)
                val emails = center.emails(userId)
                _state.update { NotificationsUiState(inbox, emails) }
            }'''),
    ('''    fun markRead(id: String) = center.markRead(id)
    fun markAllRead() = center.markAllRead(userId)''', '''    fun markRead(id: String) { viewModelScope.launch { center.markRead(id) } }
    fun markAllRead() { viewModelScope.launch { center.markAllRead(userId) } }'''),
    ('initializer { NotificationsViewModel(userId, CoreModule.notifications) }', 'initializer { NotificationsViewModel(userId, CoreModule.notificationSource) }'),
    ('import com.diponegoro.digitalbank.core.notification.NotificationCenter', 'import com.diponegoro.digitalbank.core.notification.NotificationSource'),
]
for a, b in pairs:
    assert a in s, a[:90]
    s = s.replace(a, b)
open(vp, 'w', encoding='utf8', newline='\n').write(s)
print('ok')
