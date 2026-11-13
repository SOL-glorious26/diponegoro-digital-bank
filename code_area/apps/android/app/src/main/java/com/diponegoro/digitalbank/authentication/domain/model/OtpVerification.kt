package com.diponegoro.digitalbank.authentication.domain.model

data class OtpVerification(
    val otpId: String,
    val userId: String,
    val purpose: OtpPurpose,
    val otpCodeHash: String,
    val expiresAt: Long,
    val status: OtpStatus
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = now >= expiresAt
}