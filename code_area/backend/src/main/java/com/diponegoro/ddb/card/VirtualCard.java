package com.diponegoro.ddb.card;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Virtual card (FR-21, FR-22). Nomor kartu & CVV disimpan terenkripsi AES-GCM. */
@Entity
@Table(name = "virtual_cards")
public class VirtualCard {
    public enum Status { ACTIVE, FROZEN }

    public enum Scope { DOMESTIC, INTERNATIONAL }

    @Id
    public String id;
    @Column(name = "user_id")
    public String userId;
    @Column(name = "cardholder_name")
    public String cardholderName;
    @Column(name = "card_number_enc")
    public String cardNumberEnc;
    public String last4;
    @Column(name = "cvv_enc")
    public String cvvEnc;
    @Column(name = "expiry_month")
    public int expiryMonth;
    @Column(name = "expiry_year")
    public int expiryYear;
    @Enumerated(EnumType.STRING)
    public Status status;
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_scope")
    public Scope usageScope;
    @Column(name = "transaction_limit")
    public long transactionLimit;
    @Column(name = "created_at")
    public Instant createdAt;

    public interface Repo extends JpaRepository<VirtualCard, String> {
        List<VirtualCard> findByUserIdOrderByCreatedAtDesc(String userId);
        Optional<VirtualCard> findByIdAndUserId(String id, String userId);
    }
}
