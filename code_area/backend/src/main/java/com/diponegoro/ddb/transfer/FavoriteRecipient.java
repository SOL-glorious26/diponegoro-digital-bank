package com.diponegoro.ddb.transfer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Penerima favorit (FR-13). */
@Entity
@Table(name = "favorite_recipients")
public class FavoriteRecipient {
    @Id
    public String id;
    @Column(name = "user_id")
    public String userId;
    @Column(name = "bank_code")
    public String bankCode;
    @Column(name = "account_number")
    public String accountNumber;
    public String name;
    @Column(name = "added_at")
    public Instant addedAt;

    public interface Repo extends JpaRepository<FavoriteRecipient, String> {
        List<FavoriteRecipient> findByUserIdOrderByName(String userId);
        Optional<FavoriteRecipient> findByUserIdAndBankCodeAndAccountNumber(String userId, String bankCode, String number);
        Optional<FavoriteRecipient> findByIdAndUserId(String id, String userId);
        long countByUserId(String userId);
    }
}
