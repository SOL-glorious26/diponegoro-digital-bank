package com.diponegoro.ddb.auth;

import com.diponegoro.ddb.common.ApiException;
import com.diponegoro.ddb.common.AuditService.AuditRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Deteksi login mencurigakan (NFR-06): setelah {@link AuthService#MAX_LOGIN_FAILURES} password salah
 * dalam {@link AuthService#LOGIN_LOCK_WINDOW} (dihitung sejak login berhasil terakhir), login akun
 * tersebut ditahan sementara. Rate limit per IP dilakukan di API Gateway.
 */
@Component
public class LoginThrottle {
    private final AuditRepository audit;
    private final Clock clock;

    public LoginThrottle(AuditRepository audit, Clock clock) {
        this.audit = audit;
        this.clock = clock;
    }

    public void check(String userId) {
        Instant now = clock.instant();
        Instant since = now.minus(AuthService.LOGIN_LOCK_WINDOW);
        Instant lastSuccess = audit.findFirstByUserIdAndActionOrderByCreatedAtDesc(userId, "LOGIN_SUCCESS")
                .map(e -> e.createdAt).orElse(Instant.EPOCH);
        if (lastSuccess.isAfter(since)) since = lastSuccess;
        long failures = audit.countByUserIdAndActionAndCreatedAtAfter(userId, "LOGIN_FAILED", since);
        if (failures >= AuthService.MAX_LOGIN_FAILURES) {
            throw ApiException.locked("LOGIN_LOCKED",
                    "Terlalu banyak percobaan login. Coba lagi dalam 15 menit atau gunakan Lupa password.");
        }
    }
}
