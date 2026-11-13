package com.diponegoro.ddb.notification;

import com.diponegoro.ddb.notification.NotificationEntities.Type;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    public String id;
    @Column(name = "user_id")
    public String userId;
    @Enumerated(EnumType.STRING)
    public Type type;
    public String title;
    public String message;
    @Column(name = "tx_id")
    public String txId;
    @Column(name = "is_read")
    public boolean read;
    /** false = menunggu diambil perangkat untuk ditampilkan sebagai push notification. */
    public boolean pushed;
    @Column(name = "created_at")
    public Instant createdAt;

    public interface Repo extends JpaRepository<Notification, String> {
        Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
        long countByUserIdAndReadFalse(String userId);
        List<Notification> findTop20ByUserIdAndPushedFalseOrderByCreatedAt(String userId);
        Optional<Notification> findByIdAndUserId(String id, String userId);

        @Modifying
        @Query("update Notification n set n.read = true where n.userId = :userId and n.read = false")
        int markAllRead(String userId);
    }
}
