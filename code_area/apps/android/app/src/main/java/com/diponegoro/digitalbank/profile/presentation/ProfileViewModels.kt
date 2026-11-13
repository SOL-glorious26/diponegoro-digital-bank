package com.diponegoro.digitalbank.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diponegoro.digitalbank.authentication.domain.model.User
import com.diponegoro.digitalbank.core.di.CoreModule
import com.diponegoro.digitalbank.core.di.FeatureModule
import com.diponegoro.digitalbank.core.notification.AppNotification
import com.diponegoro.digitalbank.core.notification.EmailMessage
import com.diponegoro.digitalbank.core.notification.NotificationSource
import com.diponegoro.digitalbank.core.notification.NotificationChannel
import com.diponegoro.digitalbank.core.notification.NotificationSettings
import com.diponegoro.digitalbank.core.notification.NotificationType
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.profile.data.PendingProfileChange
import com.diponegoro.digitalbank.profile.data.ProfileDraft
import com.diponegoro.digitalbank.profile.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val editing: Boolean = false,
    val pending: PendingProfileChange? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

/** FR-27: lihat & ubah profil; perubahan aktif setelah OTP. */
class ProfileViewModel(
    private val userId: String,
    private val repo: ProfileRepository,
    private val onUserChanged: (User) -> Unit
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { refresh() }

    private fun refresh() {
        viewModelScope.launch { _state.update { it.copy(user = repo.profile(userId)) } }
    }

    fun startEdit() = _state.update { it.copy(editing = true, error = null, info = null) }
    fun cancelEdit() = _state.update { it.copy(editing = false, pending = null, error = null) }

    fun submit(draft: ProfileDraft) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            when (val r = repo.requestProfileChange(userId, draft)) {
                is AppResult.Failure -> _state.update { it.copy(busy = false, error = r.message) }
                is AppResult.Success -> _state.update { it.copy(busy = false, pending = r.data) }
            }
        }
    }

    fun resendOtp() {
        val pending = _state.value.pending ?: return
        viewModelScope.launch {
            when (val r = repo.resendOtp(userId)) {
                is AppResult.Failure -> _state.update { it.copy(error = r.message) }
                is AppResult.Success -> _state.update { it.copy(pending = pending.copy(demoOtp = r.data), info = "OTP baru dikirim") }
            }
        }
    }

    fun confirm(code: String) {
        val pending = _state.value.pending ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            when (val r = repo.confirmProfileChange(userId, pending, code)) {
                is AppResult.Failure -> _state.update { it.copy(busy = false, error = r.message) }
                is AppResult.Success -> {
                    onUserChanged(r.data)
                    _state.update {
                        it.copy(busy = false, user = r.data, pending = null, editing = false, info = "Profil berhasil diperbarui")
                    }
                }
            }
        }
    }

    fun setPhoto(uri: String?) {
        viewModelScope.launch {
            when (val r = repo.updatePhoto(userId, uri)) {
                is AppResult.Failure -> _state.update { it.copy(error = r.message) }
                is AppResult.Success -> {
                    onUserChanged(r.data)
                    _state.update { it.copy(user = r.data, info = if (uri == null) "Foto dihapus" else "Foto profil diperbarui") }
                }
            }
        }
    }

    companion object {
        fun factory(userId: String, onUserChanged: (User) -> Unit) = viewModelFactory {
            initializer { ProfileViewModel(userId, FeatureModule.profile, onUserChanged) }
        }
    }
}

data class SecurityUiState(
    val biometric: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

/** FR-28: ubah password / PIN, atur biometrik. */
class SecurityViewModel(private val userId: String, private val repo: ProfileRepository) : ViewModel() {
    private val _state = MutableStateFlow(SecurityUiState())
    val state: StateFlow<SecurityUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.update { it.copy(biometric = repo.isBiometricEnabled(userId)) } }
    }

    private fun run(success: String, block: suspend () -> AppResult<Unit>) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, info = null) }
            when (val r = block()) {
                is AppResult.Failure -> _state.update { it.copy(busy = false, error = r.message) }
                is AppResult.Success -> _state.update { it.copy(busy = false, info = success) }
            }
        }
    }

    fun changePassword(old: String, new: String, confirm: String) =
        run("Password berhasil diubah") { repo.changePassword(userId, old, new, confirm) }

    fun changePin(old: String, new: String, confirm: String) =
        run("PIN transaksi berhasil diubah") { repo.changePin(userId, old, new, confirm) }

    fun setBiometric(enabled: Boolean) {
        run(if (enabled) "Login biometrik diaktifkan" else "Login biometrik dinonaktifkan") {
            repo.setBiometric(userId, enabled).also { r ->
                if (r is AppResult.Success) _state.update { it.copy(biometric = enabled) }
            }
        }
    }

    companion object {
        fun factory(userId: String) = viewModelFactory { initializer { SecurityViewModel(userId, FeatureModule.profile) } }
    }
}

data class NotificationSettingsUiState(
    val settings: NotificationSettings = NotificationSettings(),
    val error: String? = null,
    val info: String? = null
)

class NotificationSettingsViewModel(private val userId: String, private val repo: ProfileRepository) : ViewModel() {
    private val _state = MutableStateFlow(NotificationSettingsUiState())
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
    }

    fun toggleType(type: NotificationType) {
        val s = _state.value.settings
        save(s.copy(types = if (type in s.types) s.types - type else s.types + type))
    }

    fun toggleChannel(channel: NotificationChannel) {
        val s = _state.value.settings
        save(s.copy(channels = if (channel in s.channels) s.channels - channel else s.channels + channel))
    }

    companion object {
        fun factory(userId: String) = viewModelFactory {
            initializer { NotificationSettingsViewModel(userId, FeatureModule.profile) }
        }
    }
}

data class NotificationsUiState(
    val inbox: List<AppNotification> = emptyList(),
    val emails: List<EmailMessage> = emptyList()
)

/** FR-29: notifikasi dalam aplikasi + outbox email (simulasi). */
class NotificationsViewModel(private val userId: String, private val center: NotificationSource) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            center.revision.collect {
                val inbox = center.list(userId)
                val emails = center.emails(userId)
                _state.update { NotificationsUiState(inbox, emails) }
            }
        }
    }

    fun markRead(id: String) { viewModelScope.launch { center.markRead(id) } }
    fun markAllRead() { viewModelScope.launch { center.markAllRead(userId) } }

    companion object {
        fun factory(userId: String) = viewModelFactory {
            initializer { NotificationsViewModel(userId, CoreModule.notificationSource) }
        }
    }
}
