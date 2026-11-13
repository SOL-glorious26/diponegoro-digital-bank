package com.diponegoro.digitalbank.authentication.domain.repository

import com.diponegoro.digitalbank.authentication.domain.model.AuthMethod
import com.diponegoro.digitalbank.authentication.domain.model.AuthResult
import com.diponegoro.digitalbank.authentication.domain.model.EkycVerification
import com.diponegoro.digitalbank.authentication.domain.model.OtpPurpose
import com.diponegoro.digitalbank.authentication.domain.model.Session
import com.diponegoro.digitalbank.authentication.domain.model.TransactionPin
import com.diponegoro.digitalbank.authentication.domain.model.User

interface AuthRepository {
    // Register & OTP
    suspend fun register(
        username: String, email: String, phoneNumber: String, password: String
    ): AuthResult<User>

    /** Mengembalikan kode OTP plaintext (hanya untuk simulasi, karena belum ada SMS/email). */
    suspend fun generateOtp(userId: String, purpose: OtpPurpose): AuthResult<String>
    suspend fun verifyOtp(userId: String, purpose: OtpPurpose, code: String): AuthResult<Unit>

    // e-KYC
    suspend fun submitEkyc(
        userId: String, ktpDataReference: String, selfieReference: String
    ): AuthResult<EkycVerification>
    suspend fun checkEkycStatus(userId: String): AuthResult<EkycVerification>

    // Login
    suspend fun login(identifier: String, password: String, deviceId: String): AuthResult<Session>
    suspend fun loginWithBiometric(deviceId: String): AuthResult<Session>
    suspend fun isBiometricAvailable(): Boolean

    // User
    suspend fun getUser(userId: String): User?
    suspend fun findUserByIdentifier(identifier: String): User?

    // Transaction PIN
    suspend fun getTransactionPin(userId: String): TransactionPin?
    suspend fun createPin(userId: String, pin: String): AuthResult<Unit>
    suspend fun validatePin(userId: String, pin: String): AuthResult<Unit>

    // Recovery (membutuhkan OTP recovery yang sudah VERIFIED)
    suspend fun resetPassword(userId: String, newPassword: String): AuthResult<Unit>
    suspend fun resetPin(userId: String, newPin: String): AuthResult<Unit>

    // Session
    suspend fun getActiveSessions(userId: String): List<Session>
    suspend fun logout(sessionId: String): AuthResult<Unit>
    suspend fun logoutAllDevices(userId: String): AuthResult<Unit>

    // Profile & keamanan (FR-27, FR-28)
    /** Perubahan profil hanya diterapkan bila OTP PROFILE_CHANGE sudah VERIFIED. */
    suspend fun updateProfile(userId: String, username: String, email: String, phoneNumber: String): AuthResult<User>
    suspend fun updatePhoto(userId: String, photoUri: String?): AuthResult<User>
    suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): AuthResult<Unit>
    suspend fun changePin(userId: String, oldPin: String, newPin: String): AuthResult<Unit>
    suspend fun setBiometricEnabled(userId: String, enabled: Boolean): AuthResult<Unit>
    suspend fun isBiometricEnabled(userId: String): Boolean
}