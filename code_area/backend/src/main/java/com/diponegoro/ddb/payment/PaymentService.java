package com.diponegoro.ddb.payment;

import com.diponegoro.ddb.auth.TransactionGuard;
import com.diponegoro.ddb.common.ApiException;
import com.diponegoro.ddb.common.Money;
import com.diponegoro.ddb.ledger.LedgerAccount;
import com.diponegoro.ddb.ledger.LedgerModels.External;
import com.diponegoro.ddb.ledger.LedgerModels.Line;
import com.diponegoro.ddb.ledger.LedgerModels.PostingRequest;
import com.diponegoro.ddb.ledger.LedgerModels.TxCategory;
import com.diponegoro.ddb.ledger.LedgerService;
import com.diponegoro.ddb.ledger.LedgerTransaction;
import com.diponegoro.ddb.ledger.TransactionDto;
import com.diponegoro.ddb.payment.QrisCodec.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Modul Payment (QRIS): FR-15, FR-16, FR-17. */
@Service
public class PaymentService {
    public static final long MIN_AMOUNT = 1_000L;
    /** Batas nominal per transaksi QRIS (ketentuan umum QRIS; nilai usulan). */
    public static final long MAX_AMOUNT = 10_000_000L;
    /** Rekening demo ANDI PRATAMA untuk contoh QR sesama bank. */
    static final String DEMO_PERSONAL_NUMBER = "1400000002";

    public record ScanResult(String merchantName, String merchantId, String city, String mcc, boolean dynamic,
                             Long amount, boolean sameBank, String issuer, String payload) {}

    public record MyQr(String payload, boolean dynamic, Long amount, String accountName, String accountNumber) {}

    public record DemoQr(String label, String payload) {}

    private final LedgerService ledger;
    private final TransactionGuard guard;

    public PaymentService(LedgerService ledger, TransactionGuard guard) {
        this.ledger = ledger;
        this.guard = guard;
    }

    private static void requireRange(long amount) {
        if (amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
            throw ApiException.badRequest("INVALID_AMOUNT",
                    "Nominal QRIS " + Money.rupiah(MIN_AMOUNT) + " – " + Money.rupiah(MAX_AMOUNT));
        }
    }

    // ------------------------------------------------------------------ FR-16 / FR-17

    /** QRIS pribadi; amount != null menghasilkan QRIS dinamis yang nominalnya tidak bisa diubah pembayar. */
    public MyQr personalQr(String userId, Long amount) {
        LedgerAccount main = ledger.requireMainAccount(userId);
        if (amount != null) requireRange(amount);
        String payload = QrisCodec.encode(new Payload(QrisCodec.DDB_GUID, main.number, "DDB" + main.number, main.name,
                "SEMARANG", QrisCodec.PERSONAL_MCC, amount));
        return new MyQr(payload, amount != null, amount, main.name, main.number);
    }

    /**
     * Simulator: pengguna bank/e-wallet lain membayar ke QRIS pribadi (FR-16).
     * Di produksi, ini digantikan notifikasi (webhook) dari switching QRIS.
     */
    @Transactional
    public TransactionDto simulateIncoming(String userId, long amount, String payerName, String idempotencyKey) {
        requireRange(amount);
        LedgerAccount main = ledger.requireMainAccount(userId);
        String payer = payerName == null || payerName.isBlank() ? "PENGGUNA BANK LAIN" : payerName.trim().toUpperCase();
        LedgerTransaction tx = ledger.post(PostingRequest.builder(idempotencyKey, TxCategory.QRIS, "Terima dana QRIS", amount)
                .lines(new Line(External.INTERBANK, -amount), new Line(main.id, amount))
                .creditUser(userId)
                .from(payer, "QRIS antarbank (simulasi)").to(main.name, main.number)
                .build());
        return ledger.dto(tx, userId);
    }

    // ------------------------------------------------------------------ FR-15

    /** Validasi kode QR → info merchant & nominal ditampilkan sebelum PIN. */
    public ScanResult scan(String userId, String raw) {
        Payload p = QrisCodec.decode(raw);
        boolean sameBank = QrisCodec.DDB_GUID.equals(p.issuerGuid());
        if (sameBank) {
            LedgerAccount target = ledger.findMainByNumber(p.accountId())
                    .orElseThrow(() -> ApiException.unprocessable("QRIS_RECIPIENT_NOT_FOUND", "Penerima QRIS tidak ditemukan"));
            if (userId.equals(target.ownerUserId)) {
                throw ApiException.unprocessable("OWN_QRIS", "Tidak dapat membayar ke QRIS milik sendiri");
            }
        }
        return new ScanResult(p.merchantName(), p.merchantId(), p.city(), p.mcc(), p.dynamic(), p.amount(), sameBank,
                sameBank ? "Diponegoro Digital Bank" : "Bank / PJSP lain", raw.trim());
    }

    @Transactional
    public TransactionDto pay(String userId, String raw, Long inputAmount, String pin, String idempotencyKey) {
        Optional<LedgerTransaction> existing = ledger.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) return ledger.dto(existing.get(), userId);

        ScanResult s = scan(userId, raw);
        // QRIS dinamis: nominal dari kode, input pembayar diabaikan (FR-17).
        Long amount = s.dynamic() ? s.amount() : inputAmount;
        if (amount == null) throw ApiException.badRequest("AMOUNT_REQUIRED", "Masukkan nominal pembayaran");
        requireRange(amount);
        LedgerAccount main = ledger.requireMainAccount(userId);
        if (main.balance < amount) throw ApiException.unprocessable("INSUFFICIENT_BALANCE", "Saldo tidak mencukupi");
        guard.authorize(userId, pin);

        String creditAccount = External.MERCHANT;
        String creditUser = null;
        if (s.sameBank()) {
            LedgerAccount target = ledger.findMainByNumber(QrisCodec.decode(raw).accountId()).orElseThrow();
            creditAccount = target.id;
            creditUser = target.ownerUserId;
        }
        Map<String, String> extra = new LinkedHashMap<>();
        extra.put("Kota merchant", s.city());
        extra.put("MCC", s.mcc());
        LedgerTransaction tx = ledger.post(PostingRequest.builder(idempotencyKey, TxCategory.QRIS, "Pembayaran QRIS", amount)
                .lines(new Line(main.id, -amount), new Line(creditAccount, amount))
                .debitUser(userId).creditUser(creditUser)
                .from(main.name, main.number)
                .to(s.merchantName(), (s.dynamic() ? "QRIS dinamis" : "QRIS statis") + " • " + s.merchantId())
                .extra(extra)
                .build());
        return ledger.dto(tx, userId);
    }

    /** Kode QR contoh untuk pengujian di emulator tanpa kamera. */
    public List<DemoQr> demoQrs() {
        return List.of(
                new DemoQr("Warung Bu Sri (QRIS statis)", merchant("WARUNG BU SRI", "M0001", "5812", null)),
                new DemoQr("Kopi Senja Tembalang (dinamis Rp28.000)", merchant("KOPI SENJA TEMBALANG", "M0002", "5814", 28_000L)),
                new DemoQr("Toko Buku Diponegoro (dinamis Rp150.000)", merchant("TOKO BUKU DIPONEGORO", "M0003", "5942", 150_000L)),
                new DemoQr("QRIS pribadi ANDI PRATAMA (sesama bank)", QrisCodec.encode(new Payload(QrisCodec.DDB_GUID,
                        DEMO_PERSONAL_NUMBER, "DDB" + DEMO_PERSONAL_NUMBER, "ANDI PRATAMA", "SEMARANG",
                        QrisCodec.PERSONAL_MCC, null))));
    }

    private static String merchant(String name, String id, String mcc, Long amount) {
        return QrisCodec.encode(new Payload("ID.CO.QRIS.WWW", "9360000" + id, id, name, "SEMARANG", mcc, amount));
    }
}
