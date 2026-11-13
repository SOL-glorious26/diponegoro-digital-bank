package com.diponegoro.digitalbank.profile.data

import com.diponegoro.digitalbank.authentication.domain.model.AuthResult
import com.diponegoro.digitalbank.authentication.domain.model.OtpPurpose
import com.diponegoro.digitalbank.authentication.domain.model.User
import com.diponegoro.digitalbank.authentication.domain.repository.AuthRepository
import com.diponegoro.digitalbank.authentication.domain.usecase.AuthValidators
import com.diponegoro.digitalbank.core.notification.NotificationCenter
import com.diponegoro.digitalbank.core.notification.NotificationSettings
import com.diponegoro.digitalbank.core.notification.NotificationType
import com.diponegoro.digitalbank.core.util.AppResult

/** Modul Profile & Notification (FR-27 – FR-29). */
class LocalProfileRepository(
    private val auth: AuthRepository,
    private val notifications: NotificationCenter
) : ProfileRepository {
    override suspend fun profile(userId: String): User? = auth.getUser(userId)

    // ---------------------------------------------------------------- FR-27

    override suspend fun requestProfileChange(userId: String, draft: ProfileDraft): AppResult<PendingProfileChange> {
        val user = auth.getUser(userId) ?: return AppResult.Failure("Pengguna tidak ditemukan")
        val clean = ProfileDraft(draft.username.trim(), draft.email.trim(), draft.phone.trim())
        validate(clean)?.let { return AppResult.Failure(it) }
        if (clean.username == user.username && clean.email == user.email && clean.phone == user.phoneNumber)
            return AppResult.Failure("Tidak ada perubahan data")

        // OTP dikirim ke kontak baru agar kepemilikannya terverifikasi; bila tidak berubah, ke kontak lama.
        val sentTo = when {
            clean.phone != user.phoneNumber && clean.phone.isNotBlank() -> clean.phone
            clean.email != user.email && clean.email.isNotBlank() -> clean.email
            else -> user.phoneNumber.ifBlank { user.email }
        }
        return when (val otp = auth.generateOtp(userId, OtpPurpose.PROFILE_CHANGE)) {
            is AuthResult.Failure -> AppResult.Failure(otp.message)
            is AuthResult.Success -> AppResult.Success(PendingProfileChange(clean, sentTo, otp.data))
        }
    }

    /** Email/nomor telepon baru aktif hanya setelah OTP valid. */
    override suspend fun confirmProfileChange(userId: String, pending: PendingProfileChange, code: String): AppResult<User> {
        if (!AuthValidators.isValidOtp(code)) return AppResult.Failure("OTP harus 6 digit angka")
        when (val verified = auth.verifyOtp(userId, OtpPurpose.PROFILE_CHANGE, code)) {
            is AuthResult.Failure -> return AppResult.Failure(verified.message)
            is AuthResult.Success -> Unit
        }
        val d = pending.draft
        return when (val r = auth.updateProfile(userId, d.username, d.email, d.phone)) {
            is AuthResult.Failure -> AppResult.Failure(r.message)
            is AuthResult.Success -> {
                notifications.notify(userId, NotificationType.SECURITY, "Profil diperbarui",
                    "Data profil Anda berhasil diubah setelah verifikasi OTP.")
                AppResult.Success(r.data)
            }
        }
    }

    override suspend fun resendOtp(userId: String): AppResult<String> =
        when (val otp = auth.generateOtp(userId, OtpPurpose.PROFILE_CHANGE)) {
            is AuthResult.Failure -> AppResult.Failure(otp.message)
            is AuthResult.Success -> AppResult.Success(otp.data)
        }

    override suspend fun updatePhoto(userId: String, uri: String?): AppResult<User> =
        when (val r = auth.updatePhoto(userId, uri)) {
            is AuthResult.Failure -> AppResult.Failure(r.message)
            is AuthResult.Success -> AppResult.Success(r.data)
        }

    // ---------------------------------------------------------------- FR-28 (keamanan)

    override suspend fun changePassword(userId: String, old: String, new: String, confirm: String): AppResult<Unit> {
        if (!AuthValidators.isStrongPassword(new))
            return AppResult.Failure("Password minimal 8 karakter dan mengandung huruf serta angka")
        if (new != confirm) return AppResult.Failure("Konfirmasi password tidak cocok")
        return when (val r = auth.changePassword(userId, old, new)) {
            is AuthResult.Failure -> AppResult.Failure(r.message)
            is AuthResult.Success -> {
                notifications.notify(userId, NotificationType.SECURITY, "Password diubah",
                    "Password akun Anda baru saja diubah. Jika bukan Anda, segera lakukan pemulihan akun.")
                AppResult.Success(Unit)
            }
        }
    }

    override suspend fun changePin(userId: String, old: String, new: String, confirm: String): AppResult<Unit> {
        if (!AuthValidators.isValidPin(old)) return AppResult.Failure("PIN lama harus 6 digit angka")
        if (!AuthValidators.isValidPin(new)) return AppResult.Failure("PIN baru harus 6 digit angka")
        if (new != confirm) return AppResult.Failure("Konfirmasi PIN tidak cocok")
        return when (val r = auth.changePin(userId, old, new)) {
            is AuthResult.Failure -> AppResult.Failure(r.message)
            is AuthResult.Success -> {
                notifications.notify(userId, NotificationType.SECURITY, "PIN transaksi diubah",
                    "PIN transaksi Anda baru saja diubah.")
                AppResult.Success(Unit)
            }
        }
    }

    override suspend fun isBiometricEnabled(userId: String): Boolean = auth.isBiometricEnabled(userId)

    override suspend fun setBiometric(userId: String, enabled: Boolean): AppResult<Unit> =
        when (val r = auth.setBiometricEnabled(userId, enabled)) {
            is AuthResult.Failure -> AppResult.Failure(r.message)
            is AuthResult.Success -> AppResult.Success(Unit)
        }

    // ---------------------------------------------------------------- FR-28 (notifikasi)

    override suspend fun notificationSettings(userId: String): NotificationSettings = notifications.settings(userId)

    override suspend fun updateNotificationSettings(userId: String, settings: NotificationSettings): AppResult<Unit> {
        if (settings.channels.isEmpty()) return AppResult.Failure("Pilih minimal satu kanal notifikasi")
        notifications.updateSettings(userId, settings)
        return AppResult.Success(Unit)
    }

    private fun validate(d: ProfileDraft): String? = validateDraft(d)

}
