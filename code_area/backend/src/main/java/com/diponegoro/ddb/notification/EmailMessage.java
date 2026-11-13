package com.diponegoro.ddb.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

/** Email disimulasikan: disimpan ke outbox dan dapat dilihat di aplikasi (belum ada server email). */
@Entity
@Table(name = "email_outbox")
public class EmailMessage {
    @Id
    public String id;
    @Column(name = "user_id")
    public String userId;
    @Column(name = "to_address")
    public String toAddress;
    public String subject;
    public String body;
    @Column(name = "created_at")
    public Instant createdAt;

    public interface Repo extends JpaRepository<EmailMessage, String> {
        List<EmailMessage> findTop50ByUserIdOrderByCreatedAtDesc(String userId);
    }
}
