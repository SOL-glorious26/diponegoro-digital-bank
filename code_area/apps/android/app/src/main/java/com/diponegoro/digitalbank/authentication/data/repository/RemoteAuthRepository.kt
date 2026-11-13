package com.diponegoro.digitalbank.authentication.data.repository

import com.diponegoro.digitalbank.authentication.domain.model.AccountStatus
import com.diponegoro.digitalbank.authentication.domain.model.AuthMethod
import com.diponegoro.digitalbank.authentication.domain.model.AuthResult
import com.diponegoro.digitalbank.authentication.domain.model.EkycStatus
import com.diponegoro.digitalbank.authentication.domain.model.EkycVerification
import com.diponegoro.digitalbank.authentication.domain.model.OtpPurpose
import com.diponegoro.digitalbank.authentication.domain.model.PinStatus
import com.diponegoro.digitalbank.authentication.domain.model.Session
import com.diponegoro.digitalbank.authentication.domain.model.SessionStatus
import com.diponegoro.digitalbank.authentication.domain.model.TransactionPin
import com.diponegoro.digitalbank.authentication.domain.model.User
import com.diponegoro.digitalbank.authentication.domain.repository.AuthRepository
import com.diponegoro.digitalbank.core.network.ApiClient
import com.diponegoro.digitalbank.core.network.AuthTokens
import com.diponegoro.digitalbank.core.network.BiometricEnrollRequest
import com.diponegoro.digitalbank.core.network.BiometricLoginRequest
import com.diponegoro.digitalbank.core.network.EkycDto
import com.diponegoro.digitalbank.core.network.EkycRequest
import com.diponegoro.digitalbank.core.network.IsoTime
import com.diponegoro.digitalbank.core.network.LoginRequest
import com.diponegoro.digitalbank.core.network.PasswordChange
import com.diponegoro.digitalbank.core.network.PhotoRequest
import com.diponegoro.digitalbank.core.network.PinChange
import com.diponegoro.digitalbank.core.network.PinRequest
import com.diponegoro.digitalbank.core.network.RecoveryRequest
import com.diponegoro.digitalbank.core.network.RecoveryResetRequest
import com.diponegoro.digitalbank.core.network.RecoveryVerifyRequest
import com.diponegoro.digitalbank.core.network.RegisterRequest
import com.diponegoro.digitalbank.core.network.ResendOtpRequest
import com.diponegoro.digitalbank.core.network.TokenStore
import com.diponegoro.digitalbank.core.network.UserDto
import com.diponegoro.digitalbank.core.network.VerifyRegistrationRequest
import com.diponegoro.digitalbank.core.util.AppResult
import java.util.concurrent.ConcurrentHashMap

/**
 * AuthRepository versi online (FR-01 – FR-05): seluruh aturan (OTP, e-KYC, blokir PIN, sesi)
 * dijalankan backend; kelas ini hanya memetakan alur aplikasi ke endpoint /api/v1/auth dan /me.
 *
 * Perbedaan alur yang dijembatani di sini:
 *  - Registrasi: OTP pertama ikut di respons register (demoCode), jadi generateOtp() pertama memakainya.
 *  - Pemulihan: backend memakai identifier + recoveryToken, bukan userId. findUserByIdentifier() memberi
 *    "userId" sementara berawalan [RECOVERY]; token hasil verifikasi OTP disimpan di [recoveryTokens].
 */
class RemoteAuthRepository(
    private val client: ApiClient,
    private val tokens: TokenStore,
    private val deviceName: String = android.os.Build.MODEL ?: "Android"
) : AuthRepository {

    private val pendingRegistrationOtp = ConcurrentHashMap<String, String>()
    private val pendingProfile = ConcurrentHashMap<String, User>()
    private val recoveryTokens = ConcurrentHashMap<String, String>()
    private val users = ConcurrentHashMap<String, UserDto>()
    /** Waktu akhir blokir PIN yang diketahui dari respons PIN_BLOCKED (untuk tampilan layar PIN). */
    @Volatile private var pinBlockedUntil: Long = 0L

    private fun <T> AppResult<T>.auth(): AuthResult<T> = when (this) {
        is AppResult.Success -> AuthResult.Success(data)
        is AppResult.Failure -> AuthResult.Failure(message)
    }

    private fun UserDto.toUser() = User(
        userId = id, email = email.orEmpty(), phoneNumber = phoneNumber.orEmpty(), username = username,
        accountStatus = runCatching { AccountStatus.valueOf(status) }.getOrDefault(AccountStatus.ACTIVE),
        photoUri = photoUrl
    )

    private fun onTokens(t: AuthTokens, method: AuthMethod): Session {
        client.saveSession(t)
        users[t.user.id] = t.user
        return Session(
            sessionId = t.sessionId, userId = t.user.id, deviceId = tokens.deviceId(), authMethod = method,
            createdAt = System.currentTimeMillis(),
            expiresAt = IsoTime.parse(t.refreshTokenExpiresAt).takeIf { it > 0 } ?: (System.currentTimeMillis() + 7 * 86_400_000L),
            status = SessionStatus.ACTIVE
        )
    }

    // ---------------------------------------------------------------- Register & OTP

    override suspend fun register(username: String, email: String, phoneNumber: String, password: String): AuthResult<User> =
        client.call { register(RegisterRequest(username, email.ifBlank { null }, phoneNumber.ifBlank { null }, password)) }
            .auth().let { r ->
                when (r) {
                    is AuthResult.Failure -> r
                    is AuthResult.Success -> {
                        r.data.otp?.demoCode?.let { pendingRegistrationOtp[r.data.userId] = it }
                        AuthResult.Success(User(r.data.userId, email, phoneNumber, username, AccountStatus.PENDING_OTP))
                    }
                }
            }

    override suspend fun generateOtp(userId: String, purpose: OtpPurpose): AuthResult<String> {
        if (userId.startsWith(RECOVERY)) {
            val type = if (purpose == OtpPurpose.PIN_RECOVERY) "PIN" else "PASSWORD"
            return client.call { startRecovery(RecoveryRequest(userId.removePrefix(RECOVERY), type)) }.auth().let { r ->
                when (r) {
                    is AuthResult.Failure -> r
                    is AuthResult.Success -> r.data.otp?.demoCode?.let { AuthResult.Success(it) }
                        ?: AuthResult.Failure(r.data.message.ifBlank { "Akun tidak ditemukan" })
                }
            }
        }
        if (purpose == OtpPurpose.REGISTRATION) {
            pendingRegistrationOtp.remove(userId)?.let { return AuthResult.Success(it) }
            return client.call { resendRegistrationOtp(ResendOtpRequest(userId)) }.auth().let { r ->
                when (r) {
                    is AuthResult.Failure -> r
                    is AuthResult.Success -> AuthResult.Success(r.data.demoCode.orEmpty())
                }
            }
        }
        if (purpose == OtpPurpose.PROFILE_CHANGE) {
            // Dipakai modul Profile: alur online ditangani RemoteProfileRepository.
            return AuthResult.Failure("Gunakan menu Ubah Profil")
        }
        return AuthResult.Failure("Tujuan OTP tidak didukung")
    }

    override suspend fun verifyOtp(userId: String, purpose: OtpPurpose, code: String): AuthResult<Unit> {
        if (userId.startsWith(RECOVERY)) {
            val identifier = userId.removePrefix(RECOVERY)
            val type = if (purpose == OtpPurpose.PIN_RECOVERY) "PIN" else "PASSWORD"
            return when (val r = client.call { verifyRecovery(RecoveryVerifyRequest(identifier, type, code)) }) {
                is AppResult.Failure -> AuthResult.Failure(r.message)
                is AppResult.Success -> { recoveryTokens["$userId:$type"] = r.data.recoveryToken; AuthResult.Success(Unit) }
            }
        }
        if (purpose != OtpPurpose.REGISTRATION) return AuthResult.Failure("Tujuan OTP tidak didukung")
        return when (val r = client.call { verifyRegistration(VerifyRegistrationRequest(userId, code, tokens.deviceId(), deviceName)) }) {
            is AppResult.Failure -> AuthResult.Failure(r.message)
            is AppResult.Success -> { onTokens(r.data, AuthMethod.PASSWORD); AuthResult.Success(Unit) }
        }
    }

    // ---------------------------------------------------------------- e-KYC

    private fun EkycDto.toModel(userId: String, ktp: String = "", selfie: String = "") = EkycVerification(
        ekycId = id, userId = userId, ktpDataReference = ktp.ifBlank { nikMasked.orEmpty() }, selfieReference = selfie,
        status = runCatching { EkycStatus.valueOf(status) }.getOrDefault(EkycStatus.PROCESSING)
    )

    override suspend fun submitEkyc(userId: String, ktpDataReference: String, selfieReference: String): AuthResult<EkycVerification> {
        val nik = ktpDataReference.removePrefix("ktp_")
        val name = users[userId]?.username?.let { "Nasabah $it" }
        return client.call { submitEkyc(EkycRequest(nik, name, selfieReference)) }.auth().let { r ->
            when (r) {
                is AuthResult.Failure -> r
                is AuthResult.Success -> AuthResult.Success(r.data.toModel(userId, ktpDataReference, selfieReference))
            }
        }
    }

    override suspend fun checkEkycStatus(userId: String): AuthResult<EkycVerification> =
        client.call { ekyc() }.auth().let { r ->
            when (r) {
                is AuthResult.Failure -> r
                is AuthResult.Success -> AuthResult.Success(r.data.toModel(userId))
            }
        }

    // ---------------------------------------------------------------- Login

    override suspend fun login(identifier: String, password: String, deviceId: String): AuthResult<Session> =
        when (val r = client.call { login(LoginRequest(identifier, password, tokens.deviceId(), deviceName)) }) {
            is AppResult.Failure -> AuthResult.Failure(r.message)
            is AppResult.Success -> AuthResult.Success(onTokens(r.data, AuthMethod.PASSWORD))
        }

    override suspend fun loginWithBiometric(deviceId: String): AuthResult<Session> {
        val token = tokens.biometricToken() ?: return AuthResult.Failure("Biometrik belum diaktifkan di perangkat ini")
        return when (val r = client.call { biometricLogin(BiometricLoginRequest(token, tokens.deviceId(), deviceName)) }) {
            is AppResult.Failure -> AuthResult.Failure(r.message)
            is AppResult.Success -> AuthResult.Success(onTokens(r.data, AuthMethod.BIOMETRIC))
        }
    }

    override suspend fun isBiometricAvailable(): Boolean = tokens.biometricToken() != null

    // ---------------------------------------------------------------- User

    override suspend fun getUser(userId: String): User? {
        if (tokens.session()?.userId == userId) {
            when (val r = client.call { me() }) {
                is AppResult.Success -> { users[userId] = r.data; return r.data.toUser() }
                is AppResult.Failure -> Unit
            }
        }
        return users[userId]?.toUser()
    }

    /** Backend tidak membuka pencarian akun (mencegah enumerasi); identifier dipakai langsung saat pemulihan. */
    override suspend fun findUserByIdentifier(identifier: String): User? =
        User(RECOVERY + identifier.trim(), "", "", identifier.trim(), AccountStatus.ACTIVE)

    // ---------------------------------------------------------------- Transaction PIN

    override suspend fun getTransactionPin(userId: String): TransactionPin? {
        val hasPin = users[userId]?.hasPin ?: getUser(userId)?.let { users[userId]?.hasPin } ?: false
        if (!hasPin) return null
        val blocked = pinBlockedUntil > System.currentTimeMillis()
        return TransactionPin(
            pinId = "pin-$userId", userId = userId, pinHash = "",
            blockedUntil = pinBlockedUntil.takeIf { blocked }, status = if (blocked) PinStatus.BLOCKED else PinStatus.ACTIVE
        )
    }

    override suspend fun createPin(userId: String, pin: String): AuthResult<Unit> =
        when (val r = client.call { createPin(PinRequest(pin)) }) {
            is AppResult.Failure -> AuthResult.Failure(r.message)
            is AppResult.Success -> { users[userId]?.let { users[userId] = it.copy(hasPin = true) }; AuthResult.Success(Unit) }
        }

    override suspend fun validatePin(userId: String, pin: String): AuthResult<Unit> =
        when (val r = client.call { verifyPin(PinRequest(pin)) }) {
            is AppResult.Success -> { pinBlockedUntil = 0L; AuthResult.Success(Unit) }
            is AppResult.Failure -> {
                if (r.message.contains("diblokir", ignoreCase = true)) pinBlockedUntil = System.currentTimeMillis() + PIN_BLOCK_MS
                AuthResult.Failure(r.message)
            }
        }

    // ---------------------------------------------------------------- Recovery

    private suspend fun reset(userId: String, type: String, secret: String): AuthResult<Unit> {
        val token = recoveryTokens.remove("$userId:$type")
            ?: return AuthResult.Failure("Verifikasi OTP diperlukan sebelum reset ${if (type == "PIN") "PIN" else "password"}")
        return client.call { resetRecovery(RecoveryResetRequest(userId.removePrefix(RECOVERY), type, token, secret)) }
            .auth().let {
                if (it is AuthResult.Success) {
                    // DEF-10: PIN baru dari pemulihan langsung berlaku; hapus status blokir yang di-cache.
                    if (type == "PIN") pinBlockedUntil = 0L
                    AuthResult.Success(Unit)
                } else it as AuthResult.Failure
            }
    }

    override suspend fun resetPassword(userId: String, newPassword: String) = reset(userId, "PASSWORD", newPassword)
    override suspend fun resetPin(userId: String, newPin: String) = reset(userId, "PIN", newPin)

    // ---------------------------------------------------------------- Session

    override suspend fun getActiveSessions(userId: String): List<Session> =
        when (val r = client.call { sessions() }) {
            is AppResult.Failure -> emptyList()
            is AppResult.Success -> r.data.map {
                Session(
                    sessionId = it.id, userId = userId, deviceId = it.deviceName ?: it.deviceId,
                    authMethod = runCatching { AuthMethod.valueOf(it.authMethod) }.getOrDefault(AuthMethod.PASSWORD),
                    createdAt = IsoTime.parse(it.createdAt), expiresAt = IsoTime.parse(it.expiresAt), status = SessionStatus.ACTIVE
                )
            }
        }

    override suspend fun logout(sessionId: String): AuthResult<Unit> {
        client.call { logout() }   // sesi lokal tetap dihapus walau server tidak terjangkau
        tokens.clearSession()
        return AuthResult.Success(Unit)
    }

    override suspend fun logoutAllDevices(userId: String): AuthResult<Unit> {
        val r = client.call { logoutAll() }
        tokens.clearSession()
        return if (r is AppResult.Failure) AuthResult.Failure(r.message) else AuthResult.Success(Unit)
    }

    // ---------------------------------------------------------------- Profile & keamanan

    /** Perubahan profil online memakai OTP server (RemoteProfileRepository); tidak lewat AuthRepository. */
    override suspend fun updateProfile(userId: String, username: String, email: String, phoneNumber: String): AuthResult<User> =
        pendingProfile.remove(userId)?.let { AuthResult.Success(it) } ?: AuthResult.Failure("Verifikasi OTP diperlukan sebelum mengubah profil")

    override suspend fun updatePhoto(userId: String, photoUri: String?): AuthResult<User> =
        when (val r = client.call { updatePhoto(PhotoRequest(photoUri)) }) {
            is AppResult.Failure -> AuthResult.Failure(r.message)
            is AppResult.Success -> { users[userId] = r.data; AuthResult.Success(r.data.toUser()) }
        }

    override suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): AuthResult<Unit> =
        client.call { changePassword(PasswordChange(oldPassword, newPassword)) }.auth().let { if (it is AuthResult.Success) AuthResult.Success(Unit) else it as AuthResult.Failure }

    override suspend fun changePin(userId: String, oldPin: String, newPin: String): AuthResult<Unit> =
        client.call { changePin(PinChange(oldPin, newPin)) }.auth().let { if (it is AuthResult.Success) AuthResult.Success(Unit) else it as AuthResult.Failure }

    override suspend fun setBiometricEnabled(userId: String, enabled: Boolean): AuthResult<Unit> =
        if (enabled) {
            when (val r = client.call { enrollBiometric(BiometricEnrollRequest(tokens.deviceId())) }) {
                is AppResult.Failure -> AuthResult.Failure(r.message)
                is AppResult.Success -> { tokens.saveBiometricToken(r.data.biometricToken); AuthResult.Success(Unit) }
            }
        } else {
            val r = client.call { disableBiometric() }
            tokens.saveBiometricToken(null)
            if (r is AppResult.Failure) AuthResult.Failure(r.message) else AuthResult.Success(Unit)
        }

    override suspend fun isBiometricEnabled(userId: String): Boolean = tokens.biometricToken() != null

    /** Dipakai RemoteProfileRepository setelah konfirmasi OTP profil berhasil. */
    fun cacheUser(dto: UserDto): User { users[dto.id] = dto; return dto.toUser() }

    companion object {
        const val RECOVERY = "recovery:"
        private const val PIN_BLOCK_MS = 5 * 60 * 1000L
    }
}
