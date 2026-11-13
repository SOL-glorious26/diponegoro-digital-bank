package com.diponegoro.ddb.auth;

import com.diponegoro.ddb.auth.AuthEntities.AccountStatus;
import com.diponegoro.ddb.auth.AuthEntities.AuthMethod;
import com.diponegoro.ddb.auth.AuthEntities.EkycStatus;
import com.diponegoro.ddb.auth.AuthEntities.RecoveryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Request & response modul Authentication (JSON). */
public final class AuthDtos {
    private AuthDtos() {}

    // ------------------------------------------------------------------ request

    public record RegisterRequest(
            @NotBlank(message = "Username wajib diisi") @Size(min = 4, max = 30, message = "Username 4–30 karakter") String username,
            String email,
            String phoneNumber,
            @NotBlank(message = "Password wajib diisi") String password) {}

    public record VerifyRegistrationRequest(
            @NotBlank String userId,
            @NotBlank(message = "OTP wajib diisi") String code,
            @NotBlank(message = "deviceId wajib diisi") String deviceId,
            String deviceName) {}

    public record ResendOtpRequest(@NotBlank String userId) {}

    public record LoginRequest(
            @NotBlank(message = "Username/email/nomor telepon wajib diisi") String identifier,
            @NotBlank(message = "Password wajib diisi") String password,
            @NotBlank(message = "deviceId wajib diisi") String deviceId,
            String deviceName) {}

    public record BiometricLoginRequest(
            @NotBlank String biometricToken,
            @NotBlank(message = "deviceId wajib diisi") String deviceId,
            String deviceName) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record EkycRequest(
            @NotBlank(message = "NIK wajib diisi") String nik,
            String fullName,
            @NotBlank(message = "Ambil foto selfie terlebih dahulu") String selfieReference) {}

    public record PinRequest(@NotBlank(message = "PIN wajib diisi") String pin) {}

    public record RecoveryRequest(@NotBlank String identifier, @NotNull RecoveryType type) {}

    public record RecoveryVerifyRequest(@NotBlank String identifier, @NotNull RecoveryType type,
                                        @NotBlank String code) {}

    public record RecoveryResetRequest(@NotBlank String identifier, @NotNull RecoveryType type,
                                       @NotBlank String recoveryToken, @NotBlank String newSecret) {}

    // ------------------------------------------------------------------ response

    public record UserDto(String id, String username, String email, String phoneNumber, AccountStatus status,
                          EkycStatus ekycStatus, boolean hasPin, boolean biometricEnabled, String photoUrl) {}

    /** Informasi OTP yang dikirim. {@code demoCode} hanya terisi di mode simulasi. */
    public record OtpInfo(String sentTo, Instant expiresAt, String demoCode) {}

    public record RegisterResponse(String userId, OtpInfo otp) {}

    public record AuthTokens(String accessToken, Instant accessTokenExpiresAt, String refreshToken,
                             Instant refreshTokenExpiresAt, String sessionId, UserDto user) {}

    public record EkycDto(String id, EkycStatus status, String nikMasked, String reason, Instant submittedAt,
                          Instant decidedAt) {}

    public record SessionDto(String id, String deviceId, String deviceName, AuthMethod authMethod,
                             Instant createdAt, Instant lastSeenAt, Instant expiresAt, boolean current) {}

    public record RecoveryStarted(String message, OtpInfo otp) {}

    public record RecoveryVerified(String recoveryToken) {}

    public record BiometricEnrollment(String biometricToken) {}

    public record MessageResponse(String message) {}
}
