package com.diponegoro.ddb.auth;

/**
 * Enum modul Authentication (mengikuti class diagram & aplikasi Android).
 *
 * Catatan gaya entitas di backend ini: entitas JPA memakai field publik tanpa relasi lazy
 * (relasi disimpan sebagai id String), sehingga aman dari masalah proxy Hibernate dan tetap ringkas.
 */
public final class AuthEntities {
    private AuthEntities() {}

    public enum AccountStatus { PENDING_OTP, ACTIVE, FINANCIAL_ENABLED, KYC_REJECTED }

    public enum OtpPurpose { REGISTRATION, PASSWORD_RECOVERY, PIN_RECOVERY, PROFILE_CHANGE }

    public enum OtpStatus { PENDING, VERIFIED, USED, EXPIRED }

    public enum EkycStatus { PROCESSING, APPROVED, REJECTED }

    public enum SessionStatus { ACTIVE, TERMINATED, EXPIRED }

    public enum AuthMethod { PASSWORD, BIOMETRIC }

    public enum RecoveryType {
        PASSWORD(OtpPurpose.PASSWORD_RECOVERY), PIN(OtpPurpose.PIN_RECOVERY);
        public final OtpPurpose purpose;
        RecoveryType(OtpPurpose purpose) { this.purpose = purpose; }
    }
}
