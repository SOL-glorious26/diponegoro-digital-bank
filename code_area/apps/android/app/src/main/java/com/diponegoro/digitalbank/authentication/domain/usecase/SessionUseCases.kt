package com.diponegoro.digitalbank.authentication.domain.usecase

import com.diponegoro.digitalbank.authentication.domain.model.AuthResult
import com.diponegoro.digitalbank.authentication.domain.model.Session
import com.diponegoro.digitalbank.authentication.domain.repository.AuthRepository

class GetActiveSessionsUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(userId: String): List<Session> =
        repository.getActiveSessions(userId)
}

class LogoutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(sessionId: String): AuthResult<Unit> =
        repository.logout(sessionId)
}

class LogoutAllDevicesUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(userId: String): AuthResult<Unit> =
        repository.logoutAllDevices(userId)
}