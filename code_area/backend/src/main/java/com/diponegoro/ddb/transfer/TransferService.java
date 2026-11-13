package com.diponegoro.ddb.transfer;

import com.diponegoro.ddb.auth.TransactionGuard;
import com.diponegoro.ddb.common.ApiException;
import com.diponegoro.ddb.common.Ids;
import com.diponegoro.ddb.common.Money;
import com.diponegoro.ddb.ledger.LedgerAccount;
import com.diponegoro.ddb.ledger.LedgerModels.External;
import com.diponegoro.ddb.ledger.LedgerModels.Line;
import com.diponegoro.ddb.ledger.LedgerModels.PostingRequest;
import com.diponegoro.ddb.ledger.LedgerModels.TxCategory;
import com.diponegoro.ddb.ledger.LedgerModels.TxStatus;
import com.diponegoro.ddb.ledger.LedgerService;
import com.diponegoro.ddb.ledger.LedgerTransaction;
import com.diponegoro.ddb.ledger.TransactionDto;
import com.diponegoro.ddb.transfer.InterbankGateway.Bank;
import com.diponegoro.ddb.transfer.InterbankGateway.Outcome;
import com.diponegoro.ddb.transfer.TransferDtos.BankDto;
import com.diponegoro.ddb.transfer.TransferDtos.FavoriteDto;
import com.diponegoro.ddb.transfer.TransferDtos.QuoteDto;
import com.diponegoro.ddb.transfer.TransferDtos.RecipientDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

/** Modul Transfer: FR-10 – FR-14, UC-01 (alur utama + A1–A6). */
@Service
public class TransferService {
    public static final String INTERNAL_BANK = "DDB";
    /** BR-01. */
    public static final long MIN_AMOUNT = 5_000L;
    /** BR-07 belum ditetapkan SRS; nilai usulan. */
    public static final long MAX_AMOUNT = 25_000_000L;
    /** Biaya antarbank (setara BI-FAST); sesama bank gratis. Nilai usulan. */
    public static final long INTERBANK_FEE = 2_500L;
    public static final int MAX_NOTE = 50;
    public static final int MAX_FAVORITES = 20;
    static final String INVALID_ACCOUNT = "Rekening tujuan tidak valid. Periksa kembali data penerima.";

    private final LedgerService ledger;
    private final InterbankGateway interbank;
    private final TransactionGuard guard;
    private final FavoriteRecipient.Repo favorites;
    private final Clock clock;

    public TransferService(LedgerService ledger, InterbankGateway interbank, TransactionGuard guard,
                           FavoriteRecipient.Repo favorites, Clock clock) {
        this.ledger = ledger;
        this.interbank = interbank;
        this.guard = guard;
        this.favorites = favorites;
        this.clock = clock;
    }

    public List<BankDto> banks() {
        return interbank.banks().stream()
                .map(b -> new BankDto(b.code(), b.name(), b.internal(), b.internal() ? 0 : INTERBANK_FEE)).toList();
    }

    // ------------------------------------------------------------------ FR-12 / A3

    public RecipientDto inquiry(String userId, String bankCode, String rawNumber) {
        Bank bank = interbank.bank(bankCode).orElseThrow(() -> ApiException.badRequest("INVALID_BANK", "Pilih bank tujuan"));
        String number = rawNumber == null ? "" : rawNumber.replaceAll("\\D", "");
        if (number.length() < 10 || number.length() > 16) {
            throw ApiException.badRequest("INVALID_ACCOUNT_NUMBER", "Nomor rekening harus 10–16 digit");
        }
        String name;
        if (bank.internal()) {
            LedgerAccount target = ledger.findMainByNumber(number)
                    .orElseThrow(() -> ApiException.unprocessable("INVALID_ACCOUNT", INVALID_ACCOUNT));
            if (userId.equals(target.ownerUserId)) {
                throw ApiException.unprocessable("OWN_ACCOUNT", "Tidak dapat transfer ke rekening sendiri");
            }
            name = target.name;
        } else {
            name = interbank.inquiry(bank.code(), number)
                    .orElseThrow(() -> ApiException.unprocessable("INVALID_ACCOUNT", INVALID_ACCOUNT));
        }
        boolean fav = favorites.findByUserIdAndBankCodeAndAccountNumber(userId, bank.code(), number).isPresent();
        return new RecipientDto(bank.code(), bank.name(), number, name, fav);
    }

    // ------------------------------------------------------------------ UC-01 langkah 3–4 / A1, A2

    public QuoteDto quote(String userId, String bankCode, String accountNumber, long amount, String note) {
        RecipientDto r = inquiry(userId, bankCode, accountNumber);
        String n = note == null ? "" : note.trim();
        if (amount < MIN_AMOUNT) throw ApiException.badRequest("AMOUNT_TOO_SMALL", "Nominal minimal " + Money.rupiah(MIN_AMOUNT));
        if (amount > MAX_AMOUNT) {
            throw ApiException.badRequest("AMOUNT_TOO_LARGE", "Nominal maksimal " + Money.rupiah(MAX_AMOUNT) + " per transaksi");
        }
        if (n.length() > MAX_NOTE) throw ApiException.badRequest("NOTE_TOO_LONG", "Catatan maksimal " + MAX_NOTE + " karakter");
        long fee = INTERNAL_BANK.equals(r.bankCode()) ? 0 : INTERBANK_FEE;
        long balance = ledger.requireMainAccount(userId).balance;
        if (amount + fee > balance) {
            throw ApiException.unprocessable("INSUFFICIENT_BALANCE", "Saldo tidak mencukupi. Saldo Anda "
                    + Money.rupiah(balance) + ", dibutuhkan " + Money.rupiah(amount + fee)
                    + " (termasuk biaya). Kurangi nominal atau tarik dana dari kantong.");
        }
        return new QuoteDto(r, amount, fee, amount + fee, n, balance, balance - amount - fee);
    }

    // ------------------------------------------------------------------ UC-01 langkah 5–7

    /**
     * Eksekusi transfer. Kunci idempotensi yang sama (retry, tombol ditekan dua kali, A5)
     * mengembalikan transaksi yang sudah ada tanpa debit ganda (BR-04).
     */
    @Transactional
    public TransactionDto execute(String userId, String bankCode, String accountNumber, long amount, String note,
                                  String pin, String idempotencyKey) {
        Optional<LedgerTransaction> existing = ledger.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) return ledger.dto(existing.get(), userId);

        QuoteDto q = quote(userId, bankCode, accountNumber, amount, note);   // validasi ulang di server
        guard.authorize(userId, pin);                                         // e-KYC + PIN (A4)
        LedgerAccount main = ledger.requireMainAccount(userId);
        RecipientDto r = q.recipient();
        String from = INTERNAL_BANK + " • " + main.number;
        String to = r.bankName() + " • " + r.accountNumber();

        if (INTERNAL_BANK.equals(r.bankCode())) {
            // FR-10: debit & kredit atomik dalam satu posting.
            LedgerAccount target = ledger.findMainByNumber(r.accountNumber())
                    .orElseThrow(() -> ApiException.unprocessable("INVALID_ACCOUNT", INVALID_ACCOUNT));
            LedgerTransaction tx = ledger.post(PostingRequest.builder(idempotencyKey, TxCategory.TRANSFER,
                            "Transfer sesama Diponegoro", q.amount())
                    .lines(new Line(main.id, -q.amount()), new Line(target.id, q.amount()))
                    .debitUser(userId).creditUser(target.ownerUserId)
                    .from(main.name, from).to(r.name(), to).note(q.note())
                    .build());
            return ledger.dto(tx, userId);
        }

        // FR-11: dana didebit dulu, lalu dikirim lewat adapter jaringan antarbank.
        LedgerTransaction tx = ledger.post(PostingRequest.builder(idempotencyKey, TxCategory.TRANSFER,
                        "Transfer antarbank", q.amount())
                .fee(q.fee())
                .lines(new Line(main.id, -q.total()), new Line(External.INTERBANK, q.amount()),
                        new Line(External.FEE_INCOME, q.fee()))
                .debitUser(userId)
                .from(main.name, from).to(r.name(), to).note(q.note())
                .status(TxStatus.PROCESSING)
                .build());
        Outcome outcome = interbank.send(r.bankCode(), r.accountNumber(), q.amount(), tx.reference);
        tx = switch (outcome) {
            case ACCEPTED -> ledger.complete(tx.id);
            case TIMEOUT -> tx;                                                    // A5: tetap diproses
            case REJECTED -> ledger.failWithRefund(tx.id, "Ditolak oleh jaringan antarbank"); // A6, BR-05
        };
        return ledger.dto(tx, userId);
    }

    /** A5: periksa ulang status transaksi yang masih diproses. */
    @Transactional
    public TransactionDto checkStatus(String userId, String txId) {
        LedgerTransaction tx = ledger.visibleTransaction(txId, userId);
        if (tx.status != TxStatus.PROCESSING) return ledger.dto(tx, userId);
        tx = switch (interbank.status(tx.reference)) {
            case ACCEPTED -> ledger.complete(tx.id);
            case REJECTED -> ledger.failWithRefund(tx.id, "Ditolak oleh jaringan antarbank");
            case TIMEOUT -> tx;
        };
        return ledger.dto(tx, userId);
    }

    // ------------------------------------------------------------------ FR-13

    public List<FavoriteDto> favorites(String userId) {
        return favorites.findByUserIdOrderByName(userId).stream().map(this::toDto).toList();
    }

    @Transactional
    public FavoriteDto addFavorite(String userId, String bankCode, String accountNumber) {
        RecipientDto r = inquiry(userId, bankCode, accountNumber);
        if (r.favorite()) throw ApiException.conflict("ALREADY_FAVORITE", "Penerima sudah ada di favorit");
        if (favorites.countByUserId(userId) >= MAX_FAVORITES) {
            throw ApiException.unprocessable("FAVORITE_LIMIT", "Maksimal " + MAX_FAVORITES + " penerima favorit");
        }
        FavoriteRecipient f = new FavoriteRecipient();
        f.id = Ids.newId("fav");
        f.userId = userId;
        f.bankCode = r.bankCode();
        f.accountNumber = r.accountNumber();
        f.name = r.name();
        f.addedAt = clock.instant();
        favorites.save(f);
        return toDto(f);
    }

    @Transactional
    public void removeFavorite(String userId, String favoriteId) {
        favorites.findByIdAndUserId(favoriteId, userId).ifPresent(favorites::delete);
    }

    private FavoriteDto toDto(FavoriteRecipient f) {
        String bankName = interbank.bank(f.bankCode).map(Bank::name).orElse(f.bankCode);
        return new FavoriteDto(f.id, f.bankCode, bankName, f.accountNumber, f.name, f.addedAt);
    }
}
