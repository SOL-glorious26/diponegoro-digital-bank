package com.diponegoro.ddb.auth;

import com.diponegoro.ddb.auth.AuthDtos.AuthTokens;
import com.diponegoro.ddb.auth.AuthDtos.BiometricEnrollment;
import com.diponegoro.ddb.auth.AuthDtos.BiometricLoginRequest;
import com.diponegoro.ddb.auth.AuthDtos.EkycDto;
import com.diponegoro.ddb.auth.AuthDtos.EkycRequest;
import com.diponegoro.ddb.auth.AuthDtos.LoginRequest;
import com.diponegoro.ddb.auth.AuthDtos.MessageResponse;
import com.diponegoro.ddb.auth.AuthDtos.OtpInfo;
import com.diponegoro.ddb.auth.AuthDtos.PinRequest;
import com.diponegoro.ddb.auth.AuthDtos.RecoveryRequest;
import com.diponegoro.ddb.auth.AuthDtos.RecoveryResetRequest;
import com.diponegoro.ddb.auth.AuthDtos.RecoveryStarted;
import com.diponegoro.ddb.auth.AuthDtos.RecoveryVerified;
import com.diponegoro.ddb.auth.AuthDtos.RecoveryVerifyRequest;
import com.diponegoro.ddb.auth.AuthDtos.RefreshRequest;
import com.diponegoro.ddb.auth.AuthDtos.RegisterRequest;
import com.diponegoro.ddb.auth.AuthDtos.RegisterResponse;
import com.diponegoro.ddb.auth.AuthDtos.ResendOtpRequest;
import com.diponegoro.ddb.auth.AuthDtos.SessionDto;
import com.diponegoro.ddb.auth.AuthDtos.VerifyRegistrationRequest;
import com.diponegoro.ddb.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "1. Authentication", description = "FR-01 – FR-05: registrasi, OTP, e-KYC, login, PIN, sesi, pemulihan")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrasi akun (FR-01). OTP dikirim ke email/nomor telepon.")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest r) {
        return auth.register(r);
    }

    @PostMapping("/register/verify-otp")
    @Operation(summary = "Verifikasi OTP registrasi → akun aktif dan langsung login")
    public AuthTokens verifyRegistration(@Valid @RequestBody VerifyRegistrationRequest r) {
        return auth.verifyRegistration(r.userId(), r.code(), r.deviceId(), r.deviceName());
    }

    @PostMapping("/register/resend-otp")
    @Operation(summary = "Kirim ulang OTP registrasi")
    public OtpInfo resend(@Valid @RequestBody ResendOtpRequest r) {
        return auth.resendRegistrationOtp(r.userId());
    }

    @PostMapping("/login")
    @Operation(summary = "Login dengan username/email/nomor telepon + password (FR-03)")
    public AuthTokens login(@Valid @RequestBody LoginRequest r) {
        return auth.login(r.identifier(), r.password(), r.deviceId(), r.deviceName());
    }

    @PostMapping("/biometric-login")
    @Operation(summary = "Login biometrik (FR-03) dengan token perangkat yang dibuka oleh sidik jari/wajah")
    public AuthTokens biometricLogin(@Valid @RequestBody BiometricLoginRequest r) {
        return auth.biometricLogin(r.biometricToken(), r.deviceId(), r.deviceName());
    }

    public record BiometricEnrollRequest(@NotBlank String deviceId) {}

    @PostMapping("/biometric/enroll")
    @Operation(summary = "Aktifkan login biometrik di perangkat ini (FR-28)")
    public BiometricEnrollment enrollBiometric(@AuthenticationPrincipal AuthUser me,
                                               @Valid @RequestBody BiometricEnrollRequest r) {
        return new BiometricEnrollment(auth.enrollBiometric(me.userId(), r.deviceId()));
    }

    @PostMapping("/biometric/disable")
    @Operation(summary = "Nonaktifkan login biometrik di semua perangkat")
    public MessageResponse disableBiometric(@AuthenticationPrincipal AuthUser me) {
        auth.disableBiometric(me.userId());
        return new MessageResponse("Login biometrik dinonaktifkan");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Tukar refresh token dengan access token baru (refresh token dirotasi)")
    public AuthTokens refresh(@Valid @RequestBody RefreshRequest r) {
        return auth.refresh(r.refreshToken());
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout dari perangkat ini")
    public MessageResponse logout(@AuthenticationPrincipal AuthUser me) {
        auth.logout(me.sessionId());
        return new MessageResponse("Berhasil logout");
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout dari semua perangkat (FR-05)")
    public MessageResponse logoutAll(@AuthenticationPrincipal AuthUser me) {
        int n = auth.logoutAll(me.userId());
        return new MessageResponse(n + " sesi diakhiri");
    }

    @GetMapping("/sessions")
    @Operation(summary = "Daftar sesi/perangkat aktif")
    public List<SessionDto> sessions(@AuthenticationPrincipal AuthUser me) {
        return auth.sessions(me.userId(), me.sessionId());
    }

    @PostMapping("/ekyc")
    @Operation(summary = "Kirim data e-KYC (FR-02, simulasi). NIK berakhiran 0000 akan ditolak.")
    public EkycDto submitEkyc(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody EkycRequest r) {
        return auth.submitEkyc(me.userId(), r.nik(), r.fullName(), r.selfieReference());
    }

    @GetMapping("/ekyc")
    @Operation(summary = "Status e-KYC (PROCESSING → APPROVED/REJECTED setelah ±3 detik)")
    public EkycDto ekycStatus(@AuthenticationPrincipal AuthUser me) {
        return auth.ekycStatus(me.userId());
    }

    @PostMapping("/pin")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Buat PIN transaksi 6 digit (FR-04)")
    public MessageResponse createPin(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody PinRequest r) {
        auth.createPin(me.userId(), r.pin());
        return new MessageResponse("PIN transaksi berhasil dibuat");
    }

    @PostMapping("/pin/verify")
    @Operation(summary = "Verifikasi PIN transaksi tanpa transaksi keuangan (layar PIN setelah login, FR-04). "
            + "Salah 3x berturut-turut memblokir PIN 5 menit (BR-02).")
    public MessageResponse verifyPin(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody PinRequest r) {
        auth.verifyPin(me.userId(), r.pin());
        return new MessageResponse("PIN valid");
    }

    @PostMapping("/recovery/request")
    @Operation(summary = "Mulai pemulihan password/PIN: kirim OTP (FR-05)")
    public RecoveryStarted startRecovery(@Valid @RequestBody RecoveryRequest r) {
        return auth.startRecovery(r.identifier(), r.type());
    }

    @PostMapping("/recovery/verify")
    @Operation(summary = "Verifikasi OTP pemulihan → recoveryToken")
    public RecoveryVerified verifyRecovery(@Valid @RequestBody RecoveryVerifyRequest r) {
        return auth.verifyRecovery(r.identifier(), r.type(), r.code());
    }

    @PostMapping("/recovery/reset")
    @Operation(summary = "Setel password/PIN baru dengan recoveryToken")
    public MessageResponse resetCredential(@Valid @RequestBody RecoveryResetRequest r) {
        auth.resetCredential(r.identifier(), r.type(), r.recoveryToken(), r.newSecret());
        return new MessageResponse(r.type() == AuthEntities.RecoveryType.PASSWORD
                ? "Password berhasil diubah. Silakan login kembali." : "PIN berhasil diubah.");
    }
}
