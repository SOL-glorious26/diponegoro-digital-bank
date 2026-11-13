package com.diponegoro.ddb.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "biometric_devices")
public class BiometricDevice {
    @Id
    public String id;
    @Column(name = "user_id")
    public String userId;
    @Column(name = "device_id")
    public String deviceId;
    @Column(name = "token_hash")
    public String tokenHash;
    public boolean revoked;
    @Column(name = "created_at")
    public Instant createdAt;

    public interface Repo extends JpaRepository<BiometricDevice, String> {
        Optional<BiometricDevice> findByTokenHash(String tokenHash);
        Optional<BiometricDevice> findByUserIdAndDeviceId(String userId, String deviceId);
        List<BiometricDevice> findByUserIdAndRevokedFalse(String userId);
    }
}
