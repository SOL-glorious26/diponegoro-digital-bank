package com.diponegoro.digitalbank.profile.data

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
