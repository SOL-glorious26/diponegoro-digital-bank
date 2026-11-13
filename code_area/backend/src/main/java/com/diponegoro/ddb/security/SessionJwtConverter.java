package com.diponegoro.ddb.security;

import com.diponegoro.ddb.auth.AuthEntities.SessionStatus;
import com.diponegoro.ddb.auth.AuthRepositories.SessionRepository;
import com.diponegoro.ddb.auth.UserSession;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Mengubah JWT valid menjadi {@link AuthUser}, sekaligus memeriksa sesi di database (FR-05):
 * <ul>
 *   <li>sesi yang sudah logout / logout semua perangkat → ditolak;</li>
 *   <li>tidak aktif lebih dari {@link #IDLE_TIMEOUT} → sesi berakhir otomatis;</li>
 *   <li>selain itu last_seen_at diperbarui (paling sering tiap 30 detik).</li>
 * </ul>
 */
@Component
public class SessionJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    /** Timeout sesi karena tidak aktif (nilai usulan, sama dengan aplikasi Android). */
    public static final Duration IDLE_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration TOUCH_INTERVAL = Duration.ofSeconds(30);

    private final SessionRepository sessions;
    private final TransactionTemplate tx;
    private final Clock clock;

    public SessionJwtConverter(SessionRepository sessions, TransactionTemplate tx, Clock clock) {
        this.sessions = sessions;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String sid = jwt.getClaimAsString("sid");
        String userId = jwt.getSubject();
        Instant now = clock.instant();
        Boolean ok = tx.execute(status -> {
            UserSession s = sid == null ? null : sessions.findById(sid).orElse(null);
            if (s == null || !s.userId.equals(userId) || s.status != SessionStatus.ACTIVE) return false;
            if (s.expiresAt.isBefore(now) || s.lastSeenAt.plus(IDLE_TIMEOUT).isBefore(now)) {
                s.status = SessionStatus.EXPIRED;
                return false;
            }
            if (s.lastSeenAt.plus(TOUCH_INTERVAL).isBefore(now)) s.lastSeenAt = now;
            return true;
        });
        if (!Boolean.TRUE.equals(ok)) throw new CredentialsExpiredException("Sesi berakhir");
        return new AuthUser.Token(new AuthUser(userId, sid));
    }
}
