package com.diponegoro.ddb.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "transaction_pins")
public class TransactionPin {
    @Id
    @Column(name = "user_id")
    public String userId;
    @Column(name = "pin_hash")
    public String pinHash;
    @Column(name = "failed_attempts")
    public int failedAttempts;
    @Column(name = "blocked_until")
    public Instant blockedUntil;
    @Column(name = "updated_at")
    public Instant updatedAt;

    public boolean isBlocked(Instant now) {
        return blockedUntil != null && blockedUntil.isAfter(now);
    }
}
