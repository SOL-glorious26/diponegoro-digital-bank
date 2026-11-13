package com.diponegoro.ddb.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.diponegoro.ddb.ledger.LedgerModels.AccountKind;

import java.time.Instant;

@Entity
@Table(name = "ledger_accounts")
public class LedgerAccount {
    @Id
    public String id;
    @Column(name = "owner_user_id")
    public String ownerUserId;
    @Enumerated(EnumType.STRING)
    public AccountKind kind;
    public String number;
    public String name;
    /** Cache saldo; selalu = SUM(journal_lines.amount) untuk rekening ini. */
    public long balance;
    public boolean closed;
    @Column(name = "created_at")
    public Instant createdAt;
}
