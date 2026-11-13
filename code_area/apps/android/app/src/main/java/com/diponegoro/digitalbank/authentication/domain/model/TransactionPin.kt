package com.diponegoro.digitalbank.authentication.domain.model

data class TransactionPin(
    val pinId: String,
    val userId: String,
    val pinHash: String,
    val failedAttempts: Int = 0,
    val blockedUntil: Long? = null,
    val status: PinStatus = PinStatus.ACTIVE
) {
    fun isBlocked(now: Long = System.currentTimeMillis()): Boolean =
        status == PinStatus.BLOCKED && (blockedUntil ?: 0L) > now
}