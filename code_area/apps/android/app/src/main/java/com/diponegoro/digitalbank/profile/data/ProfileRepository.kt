package com.diponegoro.digitalbank.profile.data

import com.diponegoro.digitalbank.authentication.domain.model.User
import com.diponegoro.digitalbank.authentication.domain.usecase.AuthValidators
import com.diponegoro.digitalbank.core.notification.NotificationSettings
import com.diponegoro.digitalbank.core.util.AppResult

data class ProfileDraft(val username: String, val email: String, val phone: String)

/** Perubahan profil yang menunggu OTP. demoOtp ditampilkan karena belum ada SMS/email sungguhan. */
data class PendingProfileChange(val draft: ProfileDraft, val sentTo: String, val demoOtp: String)

/**
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
