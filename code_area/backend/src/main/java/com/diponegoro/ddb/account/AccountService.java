package com.diponegoro.ddb.account;

import com.diponegoro.ddb.account.AccountDtos.AccountOverview;
import com.diponegoro.ddb.account.AccountDtos.BalanceChart;
import com.diponegoro.ddb.account.AccountDtos.BalancePeriod;
import com.diponegoro.ddb.account.AccountDtos.BalancePoint;
import com.diponegoro.ddb.account.AccountDtos.CreatePocketRequest;
import com.diponegoro.ddb.account.AccountDtos.PocketDto;
import com.diponegoro.ddb.account.AccountDtos.UpdatePocketRequest;
import com.diponegoro.ddb.account.Pocket.LockType;
import com.diponegoro.ddb.auth.AuthRepositories.UserRepository;
import com.diponegoro.ddb.auth.PinService;
import com.diponegoro.ddb.auth.TransactionGuard;
import com.diponegoro.ddb.auth.User;
import com.diponegoro.ddb.common.ApiException;
import com.diponegoro.ddb.common.Money;
import com.diponegoro.ddb.common.TimeConfig;
import com.diponegoro.ddb.ledger.LedgerAccount;
import com.diponegoro.ddb.ledger.LedgerModels.Line;
import com.diponegoro.ddb.ledger.LedgerModels.PostingRequest;
import com.diponegoro.ddb.ledger.LedgerModels.TxCategory;
import com.diponegoro.ddb.ledger.LedgerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Modul Account: FR-06 – FR-09. */
@Service
public class AccountService {
    public static final int MAX_POCKETS = 10;
    public static final int MAX_NAME = 30;
    public static final int MAX_PURPOSE = 60;
    public static final long MIN_TARGET = 10_000L;
    private static final Locale ID = Locale.of("id", "ID");
    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("d MMM yyyy", ID);

    private final LedgerService ledger;
    private final Pocket.Repo pockets;
    private final UserRepository users;
    private final TransactionGuard guard;
    private final PinService pins;
    private final Clock clock;

    public AccountService(LedgerService ledger, Pocket.Repo pockets, UserRepository users, TransactionGuard guard,
                          PinService pins, Clock clock) {
        this.ledger = ledger;
        this.pockets = pockets;
        this.users = users;
        this.guard = guard;
        this.pins = pins;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ FR-06

    public AccountOverview overview(String userId) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("Pengguna tidak ditemukan"));
        LedgerAccount main = ledger.requireMainAccount(userId);
        long pocketsTotal = ledger.pockets(userId).stream().mapToLong(a -> a.balance).sum();
        return new AccountOverview(main.name, main.number, main.balance, pocketsTotal, u.balanceHidden,
                u.financialEnabled());
    }

    /** Pilihan sembunyikan/tampilkan saldo tersimpan di server, jadi sama di semua perangkat. */
    @Transactional
    public void setBalanceHidden(String userId, boolean hidden) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("Pengguna tidak ditemukan"));
        u.balanceHidden = hidden;
        u.updatedAt = clock.instant();
    }

    // ------------------------------------------------------------------ FR-07

    /** Saldo rekening utama di akhir setiap hari/minggu/bulan (WIB), konsisten dengan ledger. */
    public BalanceChart balanceChart(String userId, BalancePeriod period) {
        LedgerAccount main = ledger.requireMainAccount(userId);
        ZonedDateTime now = clock.instant().atZone(TimeConfig.ZONE);
        DateTimeFormatter label = DateTimeFormatter.ofPattern(period == BalancePeriod.MONTHLY ? "MMM yy" : "dd/MM", ID);
        List<BalancePoint> points = new ArrayList<>();
        for (int back = period.points - 1; back >= 0; back--) {
            ZonedDateTime end;
            if (back == 0) {
                end = now;
            } else {
                ZonedDateTime day = switch (period) {
                    case DAILY -> now.minusDays(back);
                    case WEEKLY -> now.minusWeeks(back);
                    case MONTHLY -> now.minusMonths(back).withDayOfMonth(1).plusMonths(1).minusDays(1);
                };
                end = day.truncatedTo(ChronoUnit.DAYS).plusDays(1).minusNanos(1);
            }
            Instant at = end.toInstant();
            points.add(new BalancePoint(label.format(end), at, ledger.balanceAt(main.id, at)));
        }
        return new BalanceChart(period, points);
    }

    // ------------------------------------------------------------------ FR-08 / FR-09

    public List<PocketDto> pockets(String userId) {
        return ledger.pockets(userId).stream().map(a -> toDto(a, meta(userId, a.id))).toList();
    }

    public PocketDto pocket(String userId, String pocketId) {
        return toDto(ledger.account(pocketId), meta(userId, pocketId));
    }

    @Transactional
    public PocketDto create(String userId, CreatePocketRequest r) {
        guard.requireFinancial(userId);
        String name = validateFields(r.name(), r.purpose(), r.target());
        LockType lock = r.lockType() == null ? LockType.NONE : r.lockType();
        validateLock(lock, r.lockUntil(), r.target());
        if (ledger.pockets(userId).size() >= MAX_POCKETS) {
            throw ApiException.unprocessable("POCKET_LIMIT", "Maksimal " + MAX_POCKETS + " kantong");
        }
        LedgerAccount a = ledger.openPocket(userId, name);
        Pocket p = new Pocket();
        p.accountId = a.id;
        p.userId = userId;
        p.purpose = r.purpose() == null ? "" : r.purpose().trim();
        p.target = r.target();
        p.lockType = lock;
        p.lockUntil = lock == LockType.DATE ? r.lockUntil() : null;
        p.createdAt = clock.instant();
        pockets.save(p);
        return toDto(a, p);
    }

    @Transactional
    public PocketDto update(String userId, String pocketId, UpdatePocketRequest r) {
        Pocket p = meta(userId, pocketId);
        LedgerAccount a = ledger.account(pocketId);
        String name = validateFields(r.name(), r.purpose(), r.target());
        if (isLocked(p, a.balance) && p.lockType == LockType.TARGET && r.target() == null) {
            throw ApiException.unprocessable("TARGET_REQUIRED", "Kantong terkunci sampai target; target tidak boleh dihapus");
        }
        a.name = name;
        p.purpose = r.purpose() == null ? "" : r.purpose().trim();
        p.target = r.target();
        return toDto(a, p);
    }

    @Transactional
    public PocketDto lock(String userId, String pocketId, LockType type, LocalDate until) {
        Pocket p = meta(userId, pocketId);
        LedgerAccount a = ledger.account(pocketId);
        if (isLocked(p, a.balance)) throw ApiException.conflict("ALREADY_LOCKED", "Kantong sudah terkunci");
        if (type == LockType.NONE) throw ApiException.badRequest("INVALID_LOCK", "Pilih jenis kunci");
        validateLock(type, until, p.target);
        if (type == LockType.TARGET && a.balance >= p.target) {
            throw ApiException.unprocessable("TARGET_REACHED", "Saldo kantong sudah mencapai target");
        }
        p.lockType = type;
        p.lockUntil = type == LockType.DATE ? until : null;
        return toDto(a, p);
    }

    @Transactional
    public void delete(String userId, String pocketId, String idempotencyKey) {
        Pocket p = meta(userId, pocketId);
        LedgerAccount a = ledger.account(pocketId);
        if (isLocked(p, a.balance)) throw ApiException.unprocessable("POCKET_LOCKED", "Kantong terkunci tidak dapat dihapus");
        if (a.balance > 0) move(userId, a, a.balance, false, idempotencyKey);
        ledger.closeAccount(pocketId);
        pockets.delete(p);
    }

    /** Isi kantong dari rekening utama. */
    @Transactional
    public PocketDto deposit(String userId, String pocketId, long amount, String idempotencyKey) {
        guard.requireFinancial(userId);
        Pocket p = meta(userId, pocketId);
        LedgerAccount a = ledger.account(pocketId);
        move(userId, a, amount, true, idempotencyKey);
        return toDto(a, p);
    }

    /** Tarik ke rekening utama. Kantong terkunci hanya lewat penarikan dini + konfirmasi + PIN (BR-06). */
    @Transactional
    public PocketDto withdraw(String userId, String pocketId, long amount, boolean earlyConfirmed, String pin,
                              String idempotencyKey) {
        guard.requireFinancial(userId);
        Pocket p = meta(userId, pocketId);
        LedgerAccount a = ledger.account(pocketId);
        if (isLocked(p, a.balance)) {
            if (!earlyConfirmed) {
                throw ApiException.unprocessable("POCKET_LOCKED",
                        "Kantong terkunci. Penarikan dini memerlukan konfirmasi dan PIN.");
            }
            pins.verify(userId, pin);
        }
        move(userId, a, amount, false, idempotencyKey);
        return toDto(a, p);
    }

    private void move(String userId, LedgerAccount pocket, long amount, boolean toPocket, String key) {
        if (amount <= 0) throw ApiException.badRequest("INVALID_AMOUNT", "Nominal harus lebih dari 0");
        LedgerAccount main = ledger.requireMainAccount(userId);
        String title = toPocket ? "Isi kantong " + pocket.name : "Tarik dari kantong " + pocket.name;
        ledger.post(PostingRequest.builder(key, TxCategory.POCKET, title, amount)
                .lines(toPocket
                        ? List.of(new Line(main.id, -amount), new Line(pocket.id, amount))
                        : List.of(new Line(pocket.id, -amount), new Line(main.id, amount)))
                .debitUser(userId).creditUser(userId)
                .from(toPocket ? "Rekening utama" : "Kantong " + pocket.name, toPocket ? main.number : pocket.name)
                .to(toPocket ? "Kantong " + pocket.name : "Rekening utama", toPocket ? pocket.name : main.number)
                .build());
    }

    // ------------------------------------------------------------------ helper

    private Pocket meta(String userId, String pocketId) {
        return pockets.findByAccountIdAndUserId(pocketId, userId)
                .orElseThrow(() -> ApiException.notFound("Kantong tidak ditemukan"));
    }

    /** Kunci terbuka otomatis saat tanggal/target tercapai (FR-09). */
    boolean isLocked(Pocket p, long balance) {
        return switch (p.lockType) {
            case NONE -> false;
            case DATE -> LocalDate.now(clock.withZone(TimeConfig.ZONE)).isBefore(p.lockUntil);
            case TARGET -> p.target == null || balance < p.target;
        };
    }

    private PocketDto toDto(LedgerAccount a, Pocket p) {
        boolean locked = isLocked(p, a.balance);
        String desc = !locked ? "Tidak terkunci" : p.lockType == LockType.DATE
                ? "Terkunci sampai " + DATE_LABEL.format(p.lockUntil)
                : "Terkunci sampai target " + Money.rupiah(p.target == null ? 0 : p.target);
        float progress = p.target == null || p.target <= 0 ? 0f : Math.min(1f, (float) a.balance / p.target);
        return new PocketDto(a.id, a.name, p.purpose, p.target, a.balance, locked ? p.lockType : LockType.NONE,
                locked && p.lockType == LockType.DATE ? p.lockUntil : null, locked, desc, progress, p.createdAt);
    }

    private static String validateFields(String name, String purpose, Long target) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) throw ApiException.badRequest("INVALID_POCKET", "Nama kantong wajib diisi");
        if (n.length() > MAX_NAME) throw ApiException.badRequest("INVALID_POCKET", "Nama kantong maksimal " + MAX_NAME + " karakter");
        if (purpose != null && purpose.trim().length() > MAX_PURPOSE) {
            throw ApiException.badRequest("INVALID_POCKET", "Tujuan maksimal " + MAX_PURPOSE + " karakter");
        }
        if (target != null && target < MIN_TARGET) throw ApiException.badRequest("INVALID_POCKET", "Target minimal Rp10.000");
        return n;
    }

    private void validateLock(LockType type, LocalDate until, Long target) {
        if (type == LockType.DATE && (until == null || !until.isAfter(LocalDate.now(clock.withZone(TimeConfig.ZONE))))) {
            throw ApiException.badRequest("INVALID_LOCK", "Tanggal kunci harus di masa depan");
        }
        if (type == LockType.TARGET && target == null) {
            throw ApiException.badRequest("INVALID_LOCK", "Isi target terlebih dahulu untuk mengunci sampai target");
        }
    }
}
