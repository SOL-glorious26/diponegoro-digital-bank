package com.diponegoro.ddb.topup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

/** Tagihan yang sudah dibayar per periode — mencegah pembayaran ganda (FR-20). */
@Entity
@Table(name = "paid_bills")
public class PaidBill {
    @Id
    @Column(name = "bill_key")
    public String billKey;
    @Column(name = "tx_id")
    public String txId;
    @Column(name = "paid_at")
    public Instant paidAt;

    public interface Repo extends JpaRepository<PaidBill, String> {}
}
