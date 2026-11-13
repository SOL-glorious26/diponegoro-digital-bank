package com.diponegoro.ddb.auth;

import com.diponegoro.ddb.auth.AuthEntities.OtpPurpose;
import com.diponegoro.ddb.auth.AuthEntities.OtpStatus;
import com.diponegoro.ddb.auth.AuthRepositories.OtpRepository;
import com.diponegoro.ddb.common.ApiException;
import com.diponegoro.ddb.common.Ids;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * OTP 6 digit (FR-01, FR-05, FR-27). Kode disimpan sebagai hash, berlaku {@link #TTL},
 * maksimal {@link #MAX_ATTEMPTS} kali salah, dan OTP lama untuk tujuan yang sama dibatalkan saat kirim ulang.
 * Karena belum ada SMS/email sungguhan, kode dikembalikan ke pemanggil untuk ditampilkan (simulasi).
 */
@Service
public class OtpService {
    public static final Duration TTL = Duration.ofMinutes(2);
    public static final int MAX_ATTEMPTS = 5;

    private final OtpRepository repo;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public OtpService(OtpRepository repo, PasswordEncoder encoder, Clock clock) {
        this.repo = repo;
        this.encoder = encoder;
        this.clock = clock;
    }

    public record IssuedOtp(String otpId, String code, Instant expiresAt, String target) {}

    @Transactional
    public IssuedOtp issue(String userId, OtpPurpose purpose, String target, String payload) {
        repo.findByUserIdAndPurposeAndStatus(userId, purpose, OtpStatus.PENDING)
                .forEach(o -> o.status = OtpStatus.EXPIRED);
        String code = Ids.digits(6);
        OtpCode o = new OtpCode();
        o.id = Ids.newId("otp");
        o.userId = userId;
        o.purpose = purpose;
        o.codeHash = encoder.encode(code);
        o.target = target;
        o.payload = payload;
        o.status = OtpStatus.PENDING;
        o.createdAt = clock.instant();
        o.expiresAt = o.createdAt.plus(TTL);
        repo.save(o);
        return new IssuedOtp(o.id, code, o.expiresAt, target);
    }

    /**
     * Memverifikasi OTP dan menandainya VERIFIED. Berjalan di transaksi sendiri supaya hitungan
     * percobaan salah tetap tersimpan walaupun pemanggil melempar exception.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ApiException.class)
    public OtpCode verify(String userId, OtpPurpose purpose, String code) {
        if (code == null || !code.matches("\\d{6}")) {
            throw ApiException.badRequest("OTP_FORMAT", "OTP harus 6 digit angka");
        }
        OtpCode o = repo.findFirstByUserIdAndPurposeAndStatusOrderByCreatedAtDesc(userId, purpose, OtpStatus.PENDING)
                .orElseThrow(() -> ApiException.unprocessable("OTP_NOT_FOUND",
                        "OTP tidak ditemukan atau sudah tidak berlaku. Silakan kirim ulang."));
        if (!o.expiresAt.isAfter(clock.instant())) {
            o.status = OtpStatus.EXPIRED;
            throw ApiException.unprocessable("OTP_EXPIRED", "OTP kedaluwarsa. Silakan kirim ulang.");
        }
        if (!encoder.matches(code, o.codeHash)) {
            o.attempts++;
            if (o.attempts >= MAX_ATTEMPTS) {
                o.status = OtpStatus.EXPIRED;
                throw ApiException.unprocessable("OTP_EXPIRED", "Terlalu banyak percobaan. Silakan kirim ulang OTP.");
            }
            throw ApiException.unprocessable("OTP_INVALID", "Kode OTP salah.");
        }
        o.status = OtpStatus.VERIFIED;
        return o;
    }

    /** Memakai OTP yang sudah VERIFIED (sekali pakai), mis. untuk reset password. */
    @Transactional
    public OtpCode consumeVerified(String userId, OtpPurpose purpose, String otpId) {
        OtpCode o = repo.findById(otpId).orElse(null);
        if (o == null || !o.userId.equals(userId) || o.purpose != purpose || o.status != OtpStatus.VERIFIED
                || o.createdAt.plus(Duration.ofMinutes(10)).isBefore(clock.instant())) {
            throw ApiException.unprocessable("OTP_REQUIRED", "Verifikasi OTP diperlukan atau sudah kedaluwarsa");
        }
        o.status = OtpStatus.USED;
        return o;
    }
}
