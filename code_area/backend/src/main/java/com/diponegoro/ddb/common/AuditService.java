package com.diponegoro.ddb.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Jejak audit keamanan (NFR-07): login, PIN salah/diblokir, perubahan kredensial, dll.
 * Ditulis di transaksi terpisah (REQUIRES_NEW) agar tetap tercatat walau transaksi utama gagal.
 */
@Service
public class AuditService {

    public interface AuditRepository extends JpaRepository<AuditEvent, Long> {
        List<AuditEvent> findTop50ByUserIdOrderByCreatedAtDesc(String userId);

        long countByUserIdAndActionAndCreatedAtAfter(String userId, String action, Instant after);

        java.util.Optional<AuditEvent> findFirstByUserIdAndActionOrderByCreatedAtDesc(String userId, String action);
    }

    private final AuditRepository repo;
    private final Clock clock;

    public AuditService(AuditRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String userId, String action, String detail) {
        AuditEvent e = new AuditEvent();
        e.userId = userId;
        e.action = action;
        e.detail = detail == null ? null : detail.substring(0, Math.min(detail.length(), 500));
        e.ipAddress = RequestContext.clientIp();
        e.createdAt = clock.instant();
        repo.save(e);
    }
}
