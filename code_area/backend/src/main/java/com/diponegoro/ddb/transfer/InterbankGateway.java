package com.diponegoro.ddb.transfer;

import java.util.List;
import java.util.Optional;

/**
 * Adapter jaringan antarbank (SRS 2.1 & 2.5). Implementasi sekarang adalah simulator;
 * integrasi produksi cukup mengganti implementasi ini tanpa mengubah modul Transfer.
 */
public interface InterbankGateway {

    record Bank(String code, String name) {
        public boolean internal() { return TransferService.INTERNAL_BANK.equals(code); }
    }

    enum Outcome { ACCEPTED, REJECTED, TIMEOUT }

    List<Bank> banks();

    Optional<Bank> bank(String code);

    /** Validasi rekening di bank lain → nama pemilik (FR-12). */
    Optional<String> inquiry(String bankCode, String accountNumber);

    /** Mengirim dana ke bank tujuan. */
    Outcome send(String bankCode, String accountNumber, long amount, String reference);

    /** Cek ulang status transaksi yang sebelumnya timeout (UC-01 A5). */
    Outcome status(String reference);
}
