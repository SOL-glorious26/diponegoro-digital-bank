package com.diponegoro.digitalbank.authentication.domain.usecase

import com.diponegoro.digitalbank.authentication.domain.model.AuthResult
import com.diponegoro.digitalbank.authentication.domain.model.RecoveryType
import com.diponegoro.digitalbank.authentication.domain.repository.AuthRepository

data class RecoveryRequest(val userId: String, val otpCode: String)

class RequestRecoveryUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        identifier: String, type: RecoveryType
    ): AuthResult<RecoveryRequest> {
        if (identifier.isBlank())
            return AuthResult.Failure("Isi username, email, atau nomor telepon")
        val user = repository.findUserByIdentifier(identifier.trim())
            ?: return AuthResult.Failure("Akun tidak ditemukan")
        return when (val result = repository.generateOtp(user.userId, type.otpPurpose)) {
            is AuthResult.Success -> AuthResult.Success(RecoveryRequest(user.userId, result.data))
            is AuthResult.Failure -> result
        }
    }
}

class ResetPasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(userId: String, newPassword: String): AuthResult<Unit> {
        if (!AuthValidators.isStrongPassword(newPassword))
            return AuthResult.Failure("Password minimal 8 karakter dan mengandung huruf serta angka")
        return repository.resetPassword(userId, newPassword)
    }
}

class ResetPinUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(userId: String, newPin: String): AuthResult<Unit> {
        if (!AuthValidators.isValidPin(newPin)) return AuthResult.Failure("PIN harus 6 digit angka")
        return repository.resetPin(userId, newPin)
    }
}