package com.diponegoro.digitalbank.authentication.domain.model

data class Session(
    val sessionId: String,
    val userId: String,
    val deviceId: String,
    val authMethod: AuthMethod,
    val createdAt: Long,
    val expiresAt: Long,
    val status: SessionStatus
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = now >= expiresAt
}