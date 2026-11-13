package com.diponegoro.ddb.auth;

import com.diponegoro.ddb.auth.AuthRepositories.PinRepository;
import com.diponegoro.ddb.common.ApiException;
import com.diponegoro.ddb.common.AuditService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** PIN transaksi 6 digit (FR-04, BR-02): hash BCrypt, terpisah dari password, blokir 5 menit setelah 3x salah. */
@Service
public class PinService {
    public static final int MAX_ATTEMPTS = 3;
    public static final Duration BLOCK = Duration.ofMinutes(5);

    private final PinRepository repo;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final Clock clock;

    public PinService(PinRepository repo, PasswordEncoder encoder, AuditService audit, Clock clock) {
        this.repo = repo;
        this.encoder = encoder;
        this.audit = audit;
        this.clock = clock;
    }

    public static void requireFormat(String pin) {
        if (pin == null || !pin.matches("\\d{6}")) throw ApiException.badRequest("PIN_FORMAT", "PIN harus 6 digit angka");
    }

    public boolean hasPin(String userId) {
        return repo.existsById(userId);
    }

    @Transactional
    public void create(String userId, String pin) {
        requireFormat(pin);
        if (repo.existsById(userId)) throw ApiException.conflict("PIN_EXISTS", "PIN transaksi sudah dibuat");
        save(userId, pin);
    }

    /** Mengganti PIN tanpa PIN lama (setelah recovery OTP) — membuka blokir. */
    @Transactional
    public void reset(String userId, String newPin) {
        requireFormat(newPin);
        save(userId, newPin);
    }

    private void save(String userId, String pin) {
        TransactionPin p = repo.findById(userId).orElseGet(TransactionPin::new);
        p.userId = userId;
        p.pinHash = encoder.encode(pin);
        p.failedAttempts = 0;
        p.blockedUntil = null;
        p.updatedAt = clock.instant();
        repo.save(p);
    }

    /**
     * Memvalidasi PIN. Berjalan di transaksi sendiri dan tidak di-rollback oleh exception-nya sendiri,
     * sehingga hitungan salah tetap tersimpan walaupun transaksi keuangan pemanggil dibatalkan.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ApiException.class)
    public void verify(String userId, String pin) {
        requireFormat(pin);
        TransactionPin p = repo.lockByUserId(userId)
                .orElseThrow(() -> ApiException.unprocessable("PIN_NOT_SET", "PIN transaksi belum dibuat"));
        Instant now = clock.instant();
        if (p.isBlocked(now)) {
            long minutes = Math.max(1, (long) Math.ceil(Duration.between(now, p.blockedUntil).toSeconds() / 60.0));
            throw ApiException.locked("PIN_BLOCKED", "PIN diblokir sementara. Coba lagi dalam " + minutes + " menit.");
        }
        if (p.blockedUntil != null) {           // masa blokir sudah lewat
            p.blockedUntil = null;
            p.failedAttempts = 0;
        }
        if (encoder.matches(pin, p.pinHash)) {
            p.failedAttempts = 0;
            return;
        }
        p.failedAttempts++;
        p.updatedAt = now;
        if (p.failedAttempts >= MAX_ATTEMPTS) {
            p.blockedUntil = now.plus(BLOCK);
            audit.record(userId, "PIN_BLOCKED", "PIN salah " + MAX_ATTEMPTS + " kali");
            throw ApiException.locked("PIN_BLOCKED",
                    "PIN salah " + MAX_ATTEMPTS + " kali. Penggunaan PIN diblokir sementara 5 menit.");
        }
        throw ApiException.unprocessable("PIN_INVALID", "PIN salah. Sisa percobaan: " + (MAX_ATTEMPTS - p.failedAttempts));
    }
}
