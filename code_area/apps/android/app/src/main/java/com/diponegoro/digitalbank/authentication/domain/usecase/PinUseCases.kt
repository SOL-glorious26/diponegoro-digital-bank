package com.diponegoro.digitalbank.authentication.domain.usecase

import com.diponegoro.digitalbank.authentication.domain.model.AuthResult
import com.diponegoro.digitalbank.authentication.domain.model.TransactionPin
import com.diponegoro.digitalbank.authentication.domain.repository.AuthRepository

class GetTransactionPinUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(userId: String): TransactionPin? =
        repository.getTransactionPin(userId)
}

class CreateTransactionPinUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        userId: String, pin: String, confirmPin: String
    ): AuthResult<Unit> {
        if (!AuthValidators.isValidPin(pin)) return AuthResult.Failure("PIN harus 6 digit angka")
        if (pin != confirmPin) return AuthResult.Failure("Konfirmasi PIN tidak cocok")
        return repository.createPin(userId, pin)
    }
}

class ValidateTransactionPinUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(userId: String, pin: String): AuthResult<Unit> {
        if (!AuthValidators.isValidPin(pin)) return AuthResult.Failure("PIN harus 6 digit angka")
        return repository.validatePin(userId, pin)
    }
}