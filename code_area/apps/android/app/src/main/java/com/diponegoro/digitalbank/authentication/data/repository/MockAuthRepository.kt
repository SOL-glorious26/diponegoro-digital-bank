package com.diponegoro.digitalbank.authentication.data.repository

import com.diponegoro.digitalbank.authentication.domain.model.AccountStatus
import com.diponegoro.digitalbank.authentication.domain.model.AuthMethod
import com.diponegoro.digitalbank.authentication.domain.model.AuthResult
import com.diponegoro.digitalbank.authentication.domain.model.Credential
import com.diponegoro.digitalbank.authentication.domain.model.EkycStatus
import com.diponegoro.digitalbank.authentication.domain.model.EkycVerification
import com.diponegoro.digitalbank.authentication.domain.model.OtpPurpose
import com.diponegoro.digitalbank.authentication.domain.model.OtpStatus
import com.diponegoro.digitalbank.authentication.domain.model.OtpVerification
import com.diponegoro.digitalbank.authentication.domain.model.PinStatus
import com.diponegoro.digitalbank.authentication.domain.model.Session
import com.diponegoro.digitalbank.authentication.domain.model.SessionStatus
import com.diponegoro.digitalbank.authentication.domain.model.TransactionPin
import com.diponegoro.digitalbank.authentication.domain.model.User
import com.diponegoro.digitalbank.authentication.domain.repository.AuthRepository
import com.diponegoro.digitalbank.core.util.HashUtil
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.ceil

/**
 * Implementasi in-memory. Data hilang saat proses aplikasi dimatikan.
 *
 * Akun demo : username "demo" / password "Demo@1234" / PIN "123456"
 * e-KYC     : NIK yang berakhiran "0000" akan DITOLAK, selain itu DISETUJUI.
 */
class MockAuthRepository(
    private val networkDelayMs: Long = 300L
) : AuthRepository {

    private val users = mutableMapOf<String, User>()
    private val credentials = mutableMapOf<String, Credential>()
    private val pins = mutableMapOf<String, TransactionPin>()
    private val otps = mutableListOf<OtpVerification>()
    private val sessions = mutableListOf<Session>()
    private val ekycs = mutableMapOf<String, EkycVerification>()
    private val ekycPollCount = mutableMapOf<String, Int>()
    private var rememberedUserId: String? = null

    init {
        seedDemoUser()
    }

    private companion object {
        const val OTP_TTL_MS = 2 * 60 * 1000L
        const val SESSION_TTL_MS = 30 * 60 * 1000L
        const val PIN_BLOCK_MS = 5 * 60 * 1000L
        const val MAX_PIN_ATTEMPTS = 3
        const val GENERIC_LOGIN_ERROR = "Username/email atau password salah"
    }

    private fun now() = System.currentTimeMillis()
    private fun newId(prefix: String) = "$prefix-${UUID.randomUUID()}"

    private fun seedDemoUser() {
        val id = "user-demo"
        users[id] = User(id, "demo@diponegoro.bank", "081234567890", "demo", AccountStatus.FINANCIAL_ENABLED)
        credentials[id] = Credential("cred-demo", id, HashUtil.sha256("Demo@1234"), biometricEnabled = true)
        pins[id] = TransactionPin("pin-demo", id, HashUtil.sha256("123456"))
        ekycs[id] = EkycVerification("ekyc-demo", id, "ktp_demo", "selfie_demo.jpg", EkycStatus.APPROVED)
        rememberedUserId = id
    }

    private fun findUser(identifier: String): User? {
        val key = identifier.trim()
        return users.values.firstOrNull {
            it.username.equals(key, ignoreCase = true) ||
                    (it.email.isNotBlank() && it.email.equals(key, ignoreCase = true)) ||
                    (it.phoneNumber.isNotBlank() && it.phoneNumber == key)
        }
    }

    private fun createSession(userId: String, deviceId: String, method: AuthMethod): Session {
        val t = now()
        val session = Session(newId("sess"), userId, deviceId, method, t, t + SESSION_TTL_MS, SessionStatus.ACTIVE)
        sessions.add(session)
        return session
    }

    /** Menandai OTP recovery yang sudah VERIFIED sebagai terpakai. */
    private fun consumeVerifiedOtp(userId: String, purpose: OtpPurpose): Boolean {
        val idx = otps.indexOfLast {
            it.userId == userId && it.purpose == purpose && it.status == OtpStatus.VERIFIED
        }
        if (idx < 0) return false
        otps[idx] = otps[idx].copy(status = OtpStatus.EXPIRED)
        return true
    }

    // ---------- Register & OTP ----------

    override suspend fun register(
        username: String, email: String, phoneNumber: String, password: String
    ): AuthResult<User> {
        delay(networkDelayMs)
        val taken = users.values.any {
            it.username.equals(username, ignoreCase = true) ||
                    (email.isNotBlank() && it.email.equals(email, ignoreCase = true)) ||
                    (phoneNumber.isNotBlank() && it.phoneNumber == phoneNumber)
        }
        if (taken) return AuthResult.Failure("Username, email, atau nomor telepon sudah terdaftar")

        val id = newId("user")
        val user = User(id, email, phoneNumber, username, AccountStatus.PENDING_OTP)
        users[id] = user
        credentials[id] = Credential(newId("cred"), id, HashUtil.sha256(password), biometricEnabled = true)
        return AuthResult.Success(user)
    }

    override suspend fun generateOtp(userId: String, purpose: OtpPurpose): AuthResult<String> {
        delay(networkDelayMs)
        if (users[userId] == null) return AuthResult.Failure("Pengguna tidak ditemukan")
        for (i in otps.indices) {
            val o = otps[i]
            if (o.userId == userId && o.purpose == purpose && o.status == OtpStatus.PENDING) {
                otps[i] = o.copy(status = OtpStatus.EXPIRED)
            }
        }
        val code = (100000..999999).random().toString()
        otps.add(
            OtpVerification(newId("otp"), userId, purpose, HashUtil.sha256(code), now() + OTP_TTL_MS, OtpStatus.PENDING)
        )
        return AuthResult.Success(code)
    }

    override suspend fun verifyOtp(userId: String, purpose: OtpPurpose, code: String): AuthResult<Unit> {
        delay(networkDelayMs)
        val idx = otps.indexOfLast {
            it.userId == userId && it.purpose == purpose && it.status == OtpStatus.PENDING
        }
        if (idx < 0) return AuthResult.Failure("OTP tidak ditemukan atau sudah tidak berlaku. Silakan kirim ulang.")
        val otp = otps[idx]
        if (otp.isExpired()) {
            otps[idx] = otp.copy(status = OtpStatus.EXPIRED)
            return AuthResult.Failure("OTP kedaluwarsa. Silakan kirim ulang.")
        }
        if (otp.otpCodeHash != HashUtil.sha256(code)) return AuthResult.Failure("Kode OTP salah.")

        otps[idx] = otp.copy(status = OtpStatus.VERIFIED)
        if (purpose == OtpPurpose.REGISTRATION) {
            users[userId]?.let { users[userId] = it.copy(accountStatus = AccountStatus.ACTIVE) }
        }
        return AuthResult.Success(Unit)
    }

    // ---------- e-KYC ----------

    override suspend fun submitEkyc(
        userId: String, ktpDataReference: String, selfieReference: String
    ): AuthResult<EkycVerification> {
        delay(networkDelayMs)
        val user = users[userId] ?: return AuthResult.Failure("Pengguna tidak ditemukan")
        if (user.accountStatus == AccountStatus.PENDING_OTP)
            return AuthResult.Failure("Akun belum diverifikasi OTP")
        val ekyc = EkycVerification(newId("ekyc"), userId, ktpDataReference, selfieReference, EkycStatus.PROCESSING)
        ekycs[userId] = ekyc
        ekycPollCount[userId] = 0
        return AuthResult.Success(ekyc)
    }

    override suspend fun checkEkycStatus(userId: String): AuthResult<EkycVerification> {
        delay(networkDelayMs)
        val ekyc = ekycs[userId] ?: return AuthResult.Failure("Data e-KYC tidak ditemukan")
        if (ekyc.status != EkycStatus.PROCESSING) return AuthResult.Success(ekyc)

        val count = (ekycPollCount[userId] ?: 0) + 1
        ekycPollCount[userId] = count
        if (count < 2) return AuthResult.Success(ekyc) // masih processing

        val approved = !ekyc.ktpDataReference.endsWith("0000")
        val updated = ekyc.copy(status = if (approved) EkycStatus.APPROVED else EkycStatus.REJECTED)
        ekycs[userId] = updated
        users[userId]?.let {
            users[userId] = it.copy(
                accountStatus = if (approved) AccountStatus.FINANCIAL_ENABLED else AccountStatus.KYC_REJECTED
            )
        }
        return AuthResult.Success(updated)
    }

    // ---------- Login ----------

    override suspend fun login(identifier: String, password: String, deviceId: String): AuthResult<Session> {
        delay(networkDelayMs)
        val user = findUser(identifier)
        val credential = user?.let { credentials[it.userId] }
        if (user == null || credential == null || credential.passwordHash != HashUtil.sha256(password))
            return AuthResult.Failure(GENERIC_LOGIN_ERROR)
        if (user.accountStatus == AccountStatus.PENDING_OTP)
            return AuthResult.Failure("Akun belum diverifikasi OTP")

        rememberedUserId = user.userId
        return AuthResult.Success(createSession(user.userId, deviceId, AuthMethod.PASSWORD))
    }

    override suspend fun loginWithBiometric(deviceId: String): AuthResult<Session> {
        delay(networkDelayMs)
        val userId = rememberedUserId ?: return AuthResult.Failure("Biometrik tidak tersedia")
        val credential = credentials[userId]
        if (credential?.biometricEnabled != true) return AuthResult.Failure("Biometrik tidak aktif")
        return AuthResult.Success(createSession(userId, deviceId, AuthMethod.BIOMETRIC))
    }

    override suspend fun isBiometricAvailable(): Boolean {
        val userId = rememberedUserId ?: return false
        return credentials[userId]?.biometricEnabled == true
    }

    override suspend fun getUser(userId: String): User? = users[userId]

    override suspend fun findUserByIdentifier(identifier: String): User? = findUser(identifier)

    // ---------- Transaction PIN ----------

    override suspend fun getTransactionPin(userId: String): TransactionPin? = pins[userId]

    override suspend fun createPin(userId: String, pin: String): AuthResult<Unit> {
        delay(networkDelayMs)
        if (pins.containsKey(userId)) return AuthResult.Failure("PIN transaksi sudah dibuat")
        pins[userId] = TransactionPin(newId("pin"), userId, HashUtil.sha256(pin))
        return AuthResult.Success(Unit)
    }

    override suspend fun validatePin(userId: String, pin: String): AuthResult<Unit> {
        delay(networkDelayMs)
        var current = pins[userId] ?: return AuthResult.Failure("PIN belum dibuat")
        val t = now()

        if (current.status == PinStatus.BLOCKED) {
            if (current.isBlocked(t)) {
                val minutes = ceil(((current.blockedUntil ?: t) - t) / 60000.0).toInt().coerceAtLeast(1)
                return AuthResult.Failure("PIN diblokir sementara. Coba lagi dalam $minutes menit.")
            }
            current = current.copy(status = PinStatus.ACTIVE, failedAttempts = 0, blockedUntil = null)
        }

        if (current.pinHash == HashUtil.sha256(pin)) {
            pins[userId] = current.copy(failedAttempts = 0)
            return AuthResult.Success(Unit)
        }

        val failed = current.failedAttempts + 1
        return if (failed >= MAX_PIN_ATTEMPTS) {
            pins[userId] = current.copy(
                failedAttempts = failed, status = PinStatus.BLOCKED, blockedUntil = t + PIN_BLOCK_MS
            )
            AuthResult.Failure("PIN salah $MAX_PIN_ATTEMPTS kali. Penggunaan PIN diblokir sementara 5 menit.")
        } else {
            pins[userId] = current.copy(failedAttempts = failed)
            AuthResult.Failure("PIN salah. Sisa percobaan: ${MAX_PIN_ATTEMPTS - failed}")
        }
    }

    // ---------- Recovery ----------

    override suspend fun resetPassword(userId: String, newPassword: String): AuthResult<Unit> {
        delay(networkDelayMs)
        if (!consumeVerifiedOtp(userId, OtpPurpose.PASSWORD_RECOVERY))
            return AuthResult.Failure("Verifikasi OTP diperlukan sebelum reset password")
        val credential = credentials[userId] ?: return AuthResult.Failure("Kredensial tidak ditemukan")
        credentials[userId] = credential.copy(passwordHash = HashUtil.sha256(newPassword))
        return AuthResult.Success(Unit)
    }

    override suspend fun resetPin(userId: String, newPin: String): AuthResult<Unit> {
        delay(networkDelayMs)
        if (!consumeVerifiedOtp(userId, OtpPurpose.PIN_RECOVERY))
            return AuthResult.Failure("Verifikasi OTP diperlukan sebelum reset PIN")
        val existing = pins[userId]
        pins[userId] = TransactionPin(
            pinId = existing?.pinId ?: newId("pin"),
            userId = userId,
            pinHash = HashUtil.sha256(newPin)
        )
        return AuthResult.Success(Unit)
    }

    // ---------- Session ----------

    override suspend fun getActiveSessions(userId: String): List<Session> {
        val t = now()
        for (i in sessions.indices) {
            val s = sessions[i]
            if (s.status == SessionStatus.ACTIVE && s.isExpired(t)) {
                sessions[i] = s.copy(status = SessionStatus.EXPIRED)
            }
        }
        return sessions.filter { it.userId == userId && it.status == SessionStatus.ACTIVE }
    }

    override suspend fun logout(sessionId: String): AuthResult<Unit> {
        delay(networkDelayMs)
        val idx = sessions.indexOfFirst { it.sessionId == sessionId }
        if (idx < 0) return AuthResult.Failure("Sesi tidak ditemukan")
        sessions[idx] = sessions[idx].copy(status = SessionStatus.TERMINATED)
        return AuthResult.Success(Unit)
    }

    // ---------- Profile & keamanan ----------

    override suspend fun updateProfile(
        userId: String, username: String, email: String, phoneNumber: String
    ): AuthResult<User> {
        delay(networkDelayMs)
        val user = users[userId] ?: return AuthResult.Failure("Pengguna tidak ditemukan")
        val taken = users.values.any {
            it.userId != userId && (
                it.username.equals(username, ignoreCase = true) ||
                    (email.isNotBlank() && it.email.equals(email, ignoreCase = true)) ||
                    (phoneNumber.isNotBlank() && it.phoneNumber == phoneNumber)
                )
        }
        if (taken) return AuthResult.Failure("Username, email, atau nomor telepon sudah dipakai akun lain")
        if (!consumeVerifiedOtp(userId, OtpPurpose.PROFILE_CHANGE))
            return AuthResult.Failure("Verifikasi OTP diperlukan sebelum mengubah profil")
        val updated = user.copy(username = username, email = email, phoneNumber = phoneNumber)
        users[userId] = updated
        return AuthResult.Success(updated)
    }

    override suspend fun updatePhoto(userId: String, photoUri: String?): AuthResult<User> {
        val user = users[userId] ?: return AuthResult.Failure("Pengguna tidak ditemukan")
        val updated = user.copy(photoUri = photoUri)
        users[userId] = updated
        return AuthResult.Success(updated)
    }

    override suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): AuthResult<Unit> {
        delay(networkDelayMs)
        val credential = credentials[userId] ?: return AuthResult.Failure("Kredensial tidak ditemukan")
        if (credential.passwordHash != HashUtil.sha256(oldPassword)) return AuthResult.Failure("Password lama salah")
        if (oldPassword == newPassword) return AuthResult.Failure("Password baru harus berbeda dari password lama")
        credentials[userId] = credential.copy(passwordHash = HashUtil.sha256(newPassword))
        return AuthResult.Success(Unit)
    }

    override suspend fun changePin(userId: String, oldPin: String, newPin: String): AuthResult<Unit> {
        // Validasi PIN lama memakai aturan blokir yang sama (3x salah).
        when (val check = validatePin(userId, oldPin)) {
            is AuthResult.Failure -> return check
            is AuthResult.Success -> Unit
        }
        if (oldPin == newPin) return AuthResult.Failure("PIN baru harus berbeda dari PIN lama")
        val existing = pins[userId] ?: return AuthResult.Failure("PIN belum dibuat")
        pins[userId] = existing.copy(pinHash = HashUtil.sha256(newPin), failedAttempts = 0)
        return AuthResult.Success(Unit)
    }

    override suspend fun setBiometricEnabled(userId: String, enabled: Boolean): AuthResult<Unit> {
        val credential = credentials[userId] ?: return AuthResult.Failure("Kredensial tidak ditemukan")
        credentials[userId] = credential.copy(biometricEnabled = enabled)
        if (enabled) rememberedUserId = userId
        return AuthResult.Success(Unit)
    }

    override suspend fun isBiometricEnabled(userId: String): Boolean =
        credentials[userId]?.biometricEnabled == true

    override suspend fun logoutAllDevices(userId: String): AuthResult<Unit> {
        delay(networkDelayMs)
        for (i in sessions.indices) {
            val s = sessions[i]
            if (s.userId == userId && s.status == SessionStatus.ACTIVE) {
                sessions[i] = s.copy(status = SessionStatus.TERMINATED)
            }
        }
        return AuthResult.Success(Unit)
    }
}