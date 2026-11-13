package com.diponegoro.digitalbank.authentication.domain.usecase

import com.diponegoro.digitalbank.authentication.domain.model.AuthResult
import com.diponegoro.digitalbank.authentication.domain.model.EkycVerification
import com.diponegoro.digitalbank.authentication.domain.model.OtpPurpose
import com.diponegoro.digitalbank.authentication.domain.model.User
import com.diponegoro.digitalbank.authentication.domain.repository.AuthRepository

class RegisterUserUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        username: String, email: String, phone: String, password: String
    ): AuthResult<User> {
        val u = username.trim()
        val e = email.trim()
        val p = phone.trim()
        if (u.length < 4) return AuthResult.Failure("Username minimal 4 karakter")
        if (e.isBlank() && p.isBlank()) return AuthResult.Failure("Isi email atau nomor telepon")
        if (e.isNotBlank() && !AuthValidators.isValidEmail(e))
            return AuthResult.Failure("Format email tidak valid")
        if (p.isNotBlank() && !AuthValidators.isValidPhone(p))
            return AuthResult.Failure("Nomor telepon harus 9-15 digit")
        if (!AuthValidators.isStrongPassword(password))
            return AuthResult.Failure("Password minimal 8 karakter dan mengandung huruf serta angka")
        return repository.register(u, e, p, password)
    }
}

class SendOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(userId: String, purpose: OtpPurpose): AuthResult<String> =
        repository.generateOtp(userId, purpose)
}

class VerifyOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        userId: String, purpose: OtpPurpose, code: String
    ): AuthResult<Unit> {
        if (!AuthValidators.isValidOtp(code)) return AuthResult.Failure("OTP harus 6 digit angka")
        return repository.verifyOtp(userId, purpose, code)
    }
}

class SubmitEkycUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        userId: String, nik: String, selfieCaptured: Boolean
    ): AuthResult<EkycVerification> {
        if (nik.length != 16 || !nik.all { it.isDigit() })
            return AuthResult.Failure("NIK harus 16 digit angka")
        if (!selfieCaptured) return AuthResult.Failure("Ambil foto selfie terlebih dahulu")
        return repository.submitEkyc(
            userId = userId,
            ktpDataReference = "ktp_$nik",
            selfieReference = "selfie_${System.currentTimeMillis()}.jpg"
        )
    }
}

class CheckEkycStatusUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(userId: String): AuthResult<EkycVerification> =
        repository.checkEkycStatus(userId)
}