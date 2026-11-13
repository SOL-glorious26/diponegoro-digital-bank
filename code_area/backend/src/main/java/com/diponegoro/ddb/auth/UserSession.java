package com.diponegoro.ddb.auth;

import com.diponegoro.ddb.auth.AuthEntities.AuthMethod;
import com.diponegoro.ddb.auth.AuthEntities.SessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sessions")
public class UserSession {
    @Id
    public String id;
    @Column(name = "user_id")
    public String userId;
    @Column(name = "device_id")
    public String deviceId;
    @Column(name = "device_name")
    public String deviceName;
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_method")
    public AuthMethod authMethod;
    @Column(name = "refresh_token_hash")
    public String refreshTokenHash;
    @Enumerated(EnumType.STRING)
    public SessionStatus status;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "last_seen_at")
    public Instant lastSeenAt;
    @Column(name = "expires_at")
    public Instant expiresAt;
}
