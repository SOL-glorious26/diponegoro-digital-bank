package com.diponegoro.ddb.auth;

import com.diponegoro.ddb.auth.AuthEntities.OtpPurpose;
import com.diponegoro.ddb.auth.AuthEntities.OtpStatus;
import com.diponegoro.ddb.auth.AuthEntities.SessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Repository Spring Data untuk modul Authentication. */
public final class AuthRepositories {
    private AuthRepositories() {}

    public interface UserRepository extends JpaRepository<User, String> {
        Optional<User> findByUsername(String username);
        Optional<User> findByEmail(String email);
        Optional<User> findByPhoneNumber(String phoneNumber);
        boolean existsByUsername(String username);
        boolean existsByEmail(String email);
        boolean existsByPhoneNumber(String phoneNumber);

        /** Login boleh memakai username, email, atau nomor telepon (FR-03). */
        default Optional<User> findByIdentifier(String identifier) {
            String key = identifier == null ? "" : identifier.trim();
            String lower = key.toLowerCase();
            return findByUsername(lower)
                    .or(() -> findByEmail(lower))
                    .or(() -> findByPhoneNumber(key));
        }
    }

    public interface OtpRepository extends JpaRepository<OtpCode, String> {
        List<OtpCode> findByUserIdAndPurposeAndStatus(String userId, OtpPurpose purpose, OtpStatus status);
        Optional<OtpCode> findFirstByUserIdAndPurposeAndStatusOrderByCreatedAtDesc(
                String userId, OtpPurpose purpose, OtpStatus status);
    }

    public interface EkycRepository extends JpaRepository<EkycVerification, String> {
        Optional<EkycVerification> findByUserId(String userId);
    }

    public interface PinRepository extends JpaRepository<TransactionPin, String> {
        /** Dikunci saat validasi agar 2 permintaan paralel tidak melewati batas 3 kali salah. */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select p from TransactionPin p where p.userId = :userId")
        Optional<TransactionPin> lockByUserId(String userId);
    }

    public interface SessionRepository extends JpaRepository<UserSession, String> {
        List<UserSession> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, SessionStatus status);
        Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);
    }
}
