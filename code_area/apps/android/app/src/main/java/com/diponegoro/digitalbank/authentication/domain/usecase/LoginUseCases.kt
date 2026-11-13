package com.diponegoro.digitalbank.authentication.domain.usecase

import com.diponegoro.digitalbank.authentication.domain.model.AuthResult
import com.diponegoro.digitalbank.authentication.domain.model.Session
import com.diponegoro.digitalbank.authentication.domain.model.User
import com.diponegoro.digitalbank.authentication.domain.repository.AuthRepository

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        identifier: String, password: String, deviceId: String
    ): AuthResult<Session> {
        if (identifier.isBlank() || password.isBlank())
            return AuthResult.Failure("Username/email dan password wajib diisi")
        return repository.login(identifier.trim(), password, deviceId)
    }
}

class LoginWithBiometricUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(deviceId: String): AuthResult<Session> =
        repository.loginWithBiometric(deviceId)
}

class IsBiometricAvailableUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Boolean = repository.isBiometricAvailable()
}

class GetUserUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(userId: String): User? = repository.getUser(userId)
}