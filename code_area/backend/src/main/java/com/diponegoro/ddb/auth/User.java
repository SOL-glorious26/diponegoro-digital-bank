package com.diponegoro.ddb.auth;

import com.diponegoro.ddb.auth.AuthEntities.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id
    public String id;
    /** Selalu huruf kecil. */
    public String username;
    /** Selalu huruf kecil; boleh null bila mendaftar dengan nomor telepon saja. */
    public String email;
    @Column(name = "phone_number")
    public String phoneNumber;
    @Column(name = "password_hash")
    public String passwordHash;
    @Enumerated(EnumType.STRING)
    public AccountStatus status;
    @Column(name = "biometric_enabled")
    public boolean biometricEnabled;
    @Column(name = "balance_hidden")
    public boolean balanceHidden;
    @Column(name = "photo_url")
    public String photoUrl;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;

    public boolean financialEnabled() {
        return status == AccountStatus.FINANCIAL_ENABLED;
    }
}
