package com.diponegoro.ddb.auth;

import com.diponegoro.ddb.auth.AuthEntities.EkycStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "ekyc_verifications")
public class EkycVerification {
    @Id
    public String id;
    @Column(name = "user_id")
    public String userId;
    /** Hanya 4 digit terakhir NIK yang disimpan terbaca (UU PDP). */
    @Column(name = "nik_masked")
    public String nikMasked;
    @Column(name = "nik_hash")
    public String nikHash;
    @Column(name = "full_name")
    public String fullName;
    @Column(name = "selfie_reference")
    public String selfieReference;
    @Enumerated(EnumType.STRING)
    public EkycStatus status;
    public String reason;
    @Column(name = "submitted_at")
    public Instant submittedAt;
    @Column(name = "decided_at")
    public Instant decidedAt;
}
