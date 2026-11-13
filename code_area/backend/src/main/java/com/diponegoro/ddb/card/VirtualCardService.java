package com.diponegoro.ddb.card;

import com.diponegoro.ddb.auth.PinService;
import com.diponegoro.ddb.auth.TransactionGuard;
import com.diponegoro.ddb.card.VirtualCard.Scope;
import com.diponegoro.ddb.card.VirtualCard.Status;
import com.diponegoro.ddb.common.ApiException;
import com.diponegoro.ddb.common.DataCipher;
import com.diponegoro.ddb.common.Ids;
import com.diponegoro.ddb.common.Money;
import com.diponegoro.ddb.common.TimeConfig;
import com.diponegoro.ddb.ledger.LedgerAccount;
import com.diponegoro.ddb.ledger.LedgerModels.External;
import com.diponegoro.ddb.ledger.LedgerModels.Line;
import com.diponegoro.ddb.ledger.LedgerModels.PostingRequest;
import com.diponegoro.ddb.ledger.LedgerModels.TxCategory;
import com.diponegoro.ddb.ledger.LedgerService;
import com.diponegoro.ddb.ledger.LedgerTransaction;
import com.diponegoro.ddb.ledger.TransactionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Modul Virtual Card: FR-21, FR-22 (simulator penerbit kartu). */
@Service
public class VirtualCardService {
    public static final long DEFAULT_LIMIT = 5_000_000L;
    public static final long MIN_LIMIT = 10_000L;
    public static final long MAX_LIMIT = 50_000_000L;
    public static final int MAX_CARDS = 5;
    private static final String BIN_PREFIX = "489512";   // prefix fiktif untuk simulasi
    private static final int VALIDITY_YEARS = 3;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Ringkasan kartu: nomor tersamar, tanpa CVV. */
    public record CardDto(String id, String cardholderName, String maskedNumber, String last4, String expiry,
                          Status status, Scope usageScope, long transactionLimit, Instant createdAt) {}

    /** Detail lengkap; hanya dikirim setelah PIN valid (FR-22). */
    public record CardDetails(String id, String cardholderName, String cardNumber, String expiry, String cvv,
                              Status status, Scope usageScope, long transactionLimit) {}

    private final VirtualCard.Repo cards;
    private final TransactionGuard guard;
    private final PinService pins;
    private final LedgerService ledger;
    private final DataCipher cipher;
    private final Clock clock;

    public VirtualCardService(VirtualCard.Repo cards, TransactionGuard guard, PinService pins, LedgerService ledger,
                              DataCipher cipher, Clock clock) {
        this.cards = cards;
        this.guard = guard;
        this.pins = pins;
        this.ledger = ledger;
        this.cipher = cipher;
        this.clock = clock;
    }

    public List<CardDto> list(String userId) {
        return cards.findByUserIdOrderByCreatedAtDesc(userId).stream().map(VirtualCardService::toDto).toList();
    }

    /** Create Virtual Card «include» Select Card Usage Scope (FR-21). */
    @Transactional
    public CardDto create(String userId, Scope scope) {
        guard.requireFinancial(userId);
        if (scope == null) throw ApiException.badRequest("INVALID_SCOPE", "Pilih cakupan penggunaan kartu");
        if (cards.findByUserIdOrderByCreatedAtDesc(userId).size() >= MAX_CARDS) {
            throw ApiException.unprocessable("CARD_LIMIT", "Maksimal " + MAX_CARDS + " kartu virtual");
        }
        LedgerAccount main = ledger.requireMainAccount(userId);
        String number = luhnNumber();
        ZonedDateTime exp = clock.instant().atZone(TimeConfig.ZONE).plusYears(VALIDITY_YEARS);
        VirtualCard c = new VirtualCard();
        c.id = Ids.newId("card");
        c.userId = userId;
        c.cardholderName = main.name;
        c.cardNumberEnc = cipher.encrypt(number);
        c.last4 = number.substring(12);
        c.cvvEnc = cipher.encrypt(String.valueOf(100 + RANDOM.nextInt(900)));
        c.expiryMonth = exp.getMonthValue();
        c.expiryYear = exp.getYear();
        c.status = Status.ACTIVE;
        c.usageScope = scope;
        c.transactionLimit = DEFAULT_LIMIT;
        c.createdAt = clock.instant();
        cards.save(c);
        return toDto(c);
    }

    /** View Details «include» Verify Transaction PIN (FR-22). */
    public CardDetails details(String userId, String cardId, String pin) {
        VirtualCard c = card(userId, cardId);
        pins.verify(userId, pin);
        return new CardDetails(c.id, c.cardholderName, cipher.decrypt(c.cardNumberEnc), expiry(c), cipher.decrypt(c.cvvEnc),
                c.status, c.usageScope, c.transactionLimit);
    }

    @Transactional
    public CardDto freeze(String userId, String cardId) {
        VirtualCard c = card(userId, cardId);
        if (c.status == Status.FROZEN) throw ApiException.conflict("ALREADY_FROZEN", "Kartu sudah dalam status beku");
        c.status = Status.FROZEN;
        return toDto(c);
    }

    @Transactional
    public CardDto activate(String userId, String cardId) {
        guard.requireFinancial(userId);
        VirtualCard c = card(userId, cardId);
        if (c.status == Status.ACTIVE) throw ApiException.conflict("ALREADY_ACTIVE", "Kartu sudah aktif");
        c.status = Status.ACTIVE;
        return toDto(c);
    }

    /** Limit berlaku untuk transaksi berikutnya. */
    @Transactional
    public CardDto setLimit(String userId, String cardId, long limit) {
        VirtualCard c = card(userId, cardId);
        if (limit < MIN_LIMIT) throw ApiException.badRequest("LIMIT_TOO_SMALL", "Limit minimal " + Money.rupiah(MIN_LIMIT));
        if (limit > MAX_LIMIT) throw ApiException.badRequest("LIMIT_TOO_LARGE", "Limit maksimal " + Money.rupiah(MAX_LIMIT));
        c.transactionLimit = limit;
        return toDto(c);
    }

    /**
     * [Simulator] Transaksi merchant memakai kartu: membuktikan kartu beku ditolak, limit berlaku, dan
     * kartu domestik menolak merchant luar negeri. Bila disetujui, rekening utama didebit (kartu debit virtual).
     */
    @Transactional
    public TransactionDto simulatePurchase(String userId, String cardId, long amount, boolean international,
                                           String merchant, String idempotencyKey) {
        Optional<LedgerTransaction> existing = ledger.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) return ledger.dto(existing.get(), userId);
        VirtualCard c = card(userId, cardId);
        if (amount <= 0) throw ApiException.badRequest("INVALID_AMOUNT", "Nominal harus lebih dari 0");
        if (c.status == Status.FROZEN) throw ApiException.unprocessable("CARD_FROZEN", "Transaksi ditolak: kartu sedang dibekukan");
        if (international && c.usageScope == Scope.DOMESTIC) {
            throw ApiException.unprocessable("CARD_DOMESTIC_ONLY", "Transaksi ditolak: kartu hanya berlaku domestik");
        }
        if (amount > c.transactionLimit) {
            throw ApiException.unprocessable("CARD_LIMIT_EXCEEDED",
                    "Transaksi ditolak: melebihi limit " + Money.rupiah(c.transactionLimit));
        }
        guard.requireFinancial(userId);
        LedgerAccount main = ledger.requireMainAccount(userId);
        String m = merchant == null || merchant.isBlank() ? (international ? "MERCHANT LUAR NEGERI" : "MERCHANT ONLINE") : merchant.trim().toUpperCase();
        LedgerTransaction tx = ledger.post(PostingRequest.builder(idempotencyKey, TxCategory.CARD, "Pembayaran kartu virtual", amount)
                .lines(new Line(main.id, -amount), new Line(External.CARD, amount))
                .debitUser(userId)
                .from(main.name, "Kartu •••• " + c.last4)
                .to(m, international ? "Merchant internasional (simulasi)" : "Merchant domestik (simulasi)")
                .extra(Map.of("Kartu", "•••• " + c.last4, "Cakupan", c.usageScope.name()))
                .build());
        return ledger.dto(tx, userId);
    }

    private VirtualCard card(String userId, String cardId) {
        return cards.findByIdAndUserId(cardId, userId).orElseThrow(() -> ApiException.notFound("Kartu tidak ditemukan"));
    }

    private static String expiry(VirtualCard c) {
        return String.format("%02d/%02d", c.expiryMonth, c.expiryYear % 100);
    }

    private static CardDto toDto(VirtualCard c) {
        return new CardDto(c.id, c.cardholderName, "•••• •••• •••• " + c.last4, c.last4, expiry(c), c.status,
                c.usageScope, c.transactionLimit, c.createdAt);
    }

    /** 16 digit dengan check digit Luhn agar formatnya realistis (bukan kartu nyata). */
    static String luhnNumber() {
        StringBuilder sb = new StringBuilder(BIN_PREFIX);
        while (sb.length() < 15) sb.append(RANDOM.nextInt(10));
        int sum = 0;
        for (int i = 0; i < 15; i++) {
            int d = sb.charAt(14 - i) - '0';
            if (i % 2 == 0) { d *= 2; if (d > 9) d -= 9; }
            sum += d;
        }
        return sb.append((10 - sum % 10) % 10).toString();
    }
}
