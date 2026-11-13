package com.diponegoro.digitalbank.authentication.domain.model

enum class AccountStatus { PENDING_OTP, ACTIVE, FINANCIAL_ENABLED, KYC_REJECTED }
enum class OtpPurpose { REGISTRATION, PASSWORD_RECOVERY, PIN_RECOVERY, PROFILE_CHANGE }
enum class OtpStatus { PENDING, VERIFIED, EXPIRED }
enum class EkycStatus { PROCESSING, APPROVED, REJECTED }
enum class PinStatus { ACTIVE, BLOCKED }
enum class SessionStatus { ACTIVE, TERMINATED, EXPIRED }
enum class AuthMethod { PASSWORD, BIOMETRIC }

enum class RecoveryType(val otpPurpose: OtpPurpose) {
    PASSWORD(OtpPurpose.PASSWORD_RECOVERY),
    PIN(OtpPurpose.PIN_RECOVERY)
}