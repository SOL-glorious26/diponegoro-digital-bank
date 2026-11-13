package com.diponegoro.ddb.auth;

import com.diponegoro.ddb.auth.AuthRepositories.UserRepository;
import com.diponegoro.ddb.common.ApiException;
import org.springframework.stereotype.Service;

/**
 * Pemeriksaan wajib sebelum setiap transaksi keuangan:
 * FR-02 (e-KYC disetujui) lalu FR-04 / BR-02 (PIN valid, blokir setelah 3x salah).
 */
@Service
public class TransactionGuard {
    public static final String KYC_REQUIRED = "Transaksi keuangan belum aktif. Selesaikan e-KYC terlebih dahulu.";

    private final UserRepository users;
    private final PinService pins;

    public TransactionGuard(UserRepository users, PinService pins) {
        this.users = users;
        this.pins = pins;
    }

    public User requireFinancial(String userId) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("Pengguna tidak ditemukan"));
        if (!u.financialEnabled()) throw ApiException.forbidden("KYC_REQUIRED", KYC_REQUIRED);
        return u;
    }

    /** e-KYC lalu PIN, berurutan. */
    public User authorize(String userId, String pin) {
        User u = requireFinancial(userId);
        pins.verify(userId, pin);
        return u;
    }
}
