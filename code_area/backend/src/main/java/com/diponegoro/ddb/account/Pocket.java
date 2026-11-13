package com.diponegoro.ddb.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/** Metadata kantong (FR-08, FR-09). Saldonya ada di ledger_accounts (kind = POCKET). */
@Entity
@Table(name = "pockets")
public class Pocket {
    public enum LockType { NONE, DATE, TARGET }

    @Id
    @Column(name = "account_id")
    public String accountId;
    @Column(name = "user_id")
    public String userId;
    public String purpose;
    public Long target;
    @Enumerated(EnumType.STRING)
    @Column(name = "lock_type")
    public LockType lockType;
    /** Kunci berlaku sampai akhir hari sebelum tanggal ini (WIB): terbuka mulai tanggal ini. */
    @Column(name = "lock_until")
    public LocalDate lockUntil;
    @Column(name = "created_at")
    public Instant createdAt;

    public interface Repo extends JpaRepository<Pocket, String> {
        Optional<Pocket> findByAccountIdAndUserId(String accountId, String userId);
    }
}
