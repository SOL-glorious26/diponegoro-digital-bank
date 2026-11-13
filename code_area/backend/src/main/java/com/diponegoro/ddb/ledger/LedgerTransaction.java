package com.diponegoro.ddb.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.diponegoro.ddb.ledger.LedgerModels.Direction;
import com.diponegoro.ddb.ledger.LedgerModels.TxCategory;
import com.diponegoro.ddb.ledger.LedgerModels.TxStatus;

import java.time.Instant;

@Entity
@Table(name = "ledger_transactions")
public class LedgerTransaction {
    @Id
    public String id;
    public String reference;
    @Column(name = "idempotency_key")
    public String idempotencyKey;
    @Enumerated(EnumType.STRING)
    public TxCategory category;
    @Enumerated(EnumType.STRING)
    public TxStatus status;
    public long amount;
    public long fee;
    public String title;
    @Column(name = "debit_user_id")
    public String debitUserId;
    @Column(name = "credit_user_id")
    public String creditUserId;
    @Column(name = "from_name")
    public String fromName;
    @Column(name = "from_detail")
    public String fromDetail;
    @Column(name = "to_name")
    public String toName;
    @Column(name = "to_detail")
    public String toDetail;
    public String note;
    @Column(name = "status_reason")
    public String statusReason;
    /** JSON objek string→string. */
    public String extra;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;

    public long total() { return amount + fee; }

    public Direction directionFor(String userId) {
        if (userId.equals(debitUserId) && userId.equals(creditUserId)) return Direction.INTERNAL;
        if (userId.equals(debitUserId)) return Direction.OUT;
        return Direction.IN;
    }
}
