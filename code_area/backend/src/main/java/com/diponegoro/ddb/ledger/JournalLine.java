package com.diponegoro.ddb.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "journal_lines")
public class JournalLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "tx_id")
    public String txId;
    @Column(name = "account_id")
    public String accountId;
    /** Positif menambah saldo, negatif mengurangi. */
    public long amount;
    public String memo;
    @Column(name = "posted_at")
    public Instant postedAt;
}
