package com.diponegoro.ddb.auth;

import com.diponegoro.ddb.auth.AuthEntities.OtpPurpose;
import com.diponegoro.ddb.auth.AuthEntities.OtpStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "otp_codes")
public class OtpCode {
    @Id
    public String id;
    @Column(name = "user_id")
    public String userId;
    @Enumerated(EnumType.STRING)
    public OtpPurpose purpose;
    @Column(name = "code_hash")
    public String codeHash;
    public String target;
    /** Data tambahan (JSON) yang baru berlaku setelah OTP valid, mis. perubahan profil. */
    public String payload;
    public int attempts;
    @Enumerated(EnumType.STRING)
    public OtpStatus status;
    @Column(name = "expires_at")
    public Instant expiresAt;
    @Column(name = "created_at")
    public Instant createdAt;
}
