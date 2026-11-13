package com.diponegoro.ddb.history;

import com.diponegoro.ddb.common.ApiException;
import com.diponegoro.ddb.common.TimeConfig;
import com.diponegoro.ddb.ledger.JournalLine;
import com.diponegoro.ddb.ledger.LedgerAccount;
import com.diponegoro.ddb.ledger.LedgerModels.AccountKind;
import com.diponegoro.ddb.ledger.LedgerModels.Direction;
import com.diponegoro.ddb.ledger.LedgerModels.TxCategory;
import com.diponegoro.ddb.ledger.LedgerModels.TxStatus;
import com.diponegoro.ddb.ledger.LedgerRepositories.TransactionRepository;
import com.diponegoro.ddb.ledger.LedgerService;
import com.diponegoro.ddb.ledger.LedgerTransaction;
import com.diponegoro.ddb.ledger.TransactionDto;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Modul Transaction History & Dashboard: FR-23 – FR-26. Semua angka dihitung dari ledger. */
@Service
public class HistoryService {
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_EXPORT_ROWS = 5_000;
    private static final Locale ID = Locale.of("id", "ID");

    public enum DateRange { ALL, LAST_7_DAYS, LAST_30_DAYS, THIS_MONTH, LAST_MONTH, CUSTOM }

    /** Filter dapat dikombinasikan (FR-23). from/to dipakai bila range = CUSTOM (tanggal WIB, inklusif). */
    public record Filter(DateRange range, LocalDate from, LocalDate to, TxCategory category, Direction direction,
                         TxStatus status, String q) {}

    public record HistoryPage(List<TransactionDto> items, int page, int size, long totalItems, boolean hasMore) {}

    public record JournalLineDto(String account, long amount, String memo, Instant postedAt) {}

    public record TransactionDetail(TransactionDto transaction, List<JournalLineDto> journal) {}

    public record MonthSummary(String label, LocalDate month, long income, long expense, long net) {}

    private final TransactionRepository transactions;
    private final LedgerService ledger;
    private final Clock clock;

    public HistoryService(TransactionRepository transactions, LedgerService ledger, Clock clock) {
        this.transactions = transactions;
        this.ledger = ledger;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ FR-23

    public HistoryPage page(String userId, Filter f, int page, int size) {
        int s = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<LedgerTransaction> p = transactions.findAll(spec(userId, f),
                PageRequest.of(Math.max(page, 0), s, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<TransactionDto> items = p.getContent().stream().map(t -> ledger.dto(t, userId)).toList();
        return new HistoryPage(items, p.getNumber(), s, p.getTotalElements(), p.hasNext());
    }

    /** Untuk ekspor (FR-26): semua baris yang cocok dengan filter aktif, dibatasi MAX_EXPORT_ROWS. */
    public List<TransactionDto> all(String userId, Filter f) {
        return transactions.findAll(spec(userId, f), PageRequest.of(0, MAX_EXPORT_ROWS, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream().map(t -> ledger.dto(t, userId)).toList();
    }

    private Specification<LedgerTransaction> spec(String userId, Filter f) {
        Instant[] bounds = bounds(f);
        return (root, query, cb) -> {
            List<Predicate> and = new ArrayList<>();
            var debit = root.<String>get("debitUserId");
            var credit = root.<String>get("creditUserId");
            and.add(cb.or(cb.equal(debit, userId), cb.equal(credit, userId)));
            if (bounds[0] != null) and.add(cb.greaterThanOrEqualTo(root.get("createdAt"), bounds[0]));
            if (bounds[1] != null) and.add(cb.lessThan(root.get("createdAt"), bounds[1]));
            if (f.category() != null) and.add(cb.equal(root.get("category"), f.category()));
            if (f.status() != null) and.add(cb.equal(root.get("status"), f.status()));
            if (f.direction() != null) {
                Predicate isDebit = cb.equal(debit, userId);
                Predicate isCredit = cb.equal(credit, userId);
                and.add(switch (f.direction()) {
                    case INTERNAL -> cb.and(isDebit, isCredit);
                    case OUT -> cb.and(isDebit, cb.or(cb.isNull(credit), cb.notEqual(credit, userId)));
                    case IN -> cb.and(isCredit, cb.or(cb.isNull(debit), cb.notEqual(debit, userId)));
                });
            }
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
                and.add(cb.or(
                        cb.like(cb.lower(root.get("reference")), like, '\\'),
                        cb.like(cb.lower(root.get("title")), like, '\\'),
                        cb.like(cb.lower(root.get("fromName")), like, '\\'),
                        cb.like(cb.lower(root.get("toName")), like, '\\'),
                        cb.like(cb.lower(root.get("fromDetail")), like, '\\'),
                        cb.like(cb.lower(root.get("toDetail")), like, '\\'),
                        cb.like(cb.lower(root.get("note")), like, '\\')));
            }
            return cb.and(and.toArray(Predicate[]::new));
        };
    }

    private Instant[] bounds(Filter f) {
        DateRange range = f.range() == null ? DateRange.ALL : f.range();
        Instant now = clock.instant();
        return switch (range) {
            case ALL -> new Instant[]{null, null};
            case LAST_7_DAYS -> new Instant[]{now.minus(Duration.ofDays(7)), null};
            case LAST_30_DAYS -> new Instant[]{now.minus(Duration.ofDays(30)), null};
            case THIS_MONTH -> new Instant[]{monthStart(0), null};
            case LAST_MONTH -> new Instant[]{monthStart(1), monthStart(0)};
            case CUSTOM -> {
                if (f.from() == null || f.to() == null || f.to().isBefore(f.from())) {
                    throw ApiException.badRequest("INVALID_RANGE", "Isi tanggal awal dan akhir dengan benar");
                }
                yield new Instant[]{f.from().atStartOfDay(TimeConfig.ZONE).toInstant(),
                        f.to().plusDays(1).atStartOfDay(TimeConfig.ZONE).toInstant()};
            }
        };
    }

    // ------------------------------------------------------------------ FR-24

    /** Detail selalu dibaca langsung dari ledger, termasuk baris jurnalnya. */
    public TransactionDetail detail(String userId, String txId) {
        LedgerTransaction t = ledger.visibleTransaction(txId, userId);
        List<JournalLineDto> lines = ledger.journalFor(txId).stream().map(j -> line(j, userId)).toList();
        return new TransactionDetail(ledger.dto(t, userId), lines);
    }

    /** Rekening milik orang lain disamarkan agar detail jurnal tidak membocorkan data pihak lain. */
    private JournalLineDto line(JournalLine j, String userId) {
        LedgerAccount a = ledger.account(j.accountId);
        String label;
        if (a.kind == AccountKind.EXTERNAL) label = a.name;
        else if (userId.equals(a.ownerUserId)) label = (a.kind == AccountKind.POCKET ? "Kantong " : "Rekening ") + a.name + " • " + a.number;
        else label = "Rekening pihak lain • " + "******" + a.number.substring(Math.max(0, a.number.length() - 4));
        return new JournalLineDto(label, j.amount, j.memo, j.postedAt);
    }

    // ------------------------------------------------------------------ FR-25

    /** Pemasukan & pengeluaran per bulan (WIB), terlama → terbaru. Hanya transaksi berhasil. */
    public List<MonthSummary> monthSeries(String userId, int months, int endMonthsBack) {
        int m = Math.min(Math.max(months, 1), 24);
        List<MonthSummary> out = new ArrayList<>();
        for (int back = endMonthsBack + m - 1; back >= endMonthsBack; back--) out.add(month(userId, back));
        return out;
    }

    private MonthSummary month(String userId, int back) {
        Instant start = monthStart(back);
        Instant end = monthStart(back - 1);
        long income = 0, expense = 0;
        Filter f = new Filter(DateRange.ALL, null, null, null, null, TxStatus.SUCCESS, null);
        Specification<LedgerTransaction> s = spec(userId, f).and((root, q, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("createdAt"), start), cb.lessThan(root.get("createdAt"), end)));
        for (LedgerTransaction t : transactions.findAll(s)) {
            switch (t.directionFor(userId)) {
                case IN -> income += t.amount;
                case OUT -> expense += t.total();
                case INTERNAL -> { }
            }
        }
        LocalDate month = start.atZone(TimeConfig.ZONE).toLocalDate();
        return new MonthSummary(DateTimeFormatter.ofPattern("MMM yyyy", ID).format(month), month, income, expense, income - expense);
    }

    private Instant monthStart(int monthsBack) {
        ZonedDateTime now = clock.instant().atZone(TimeConfig.ZONE);
        return now.toLocalDate().withDayOfMonth(1).minusMonths(monthsBack).atStartOfDay(TimeConfig.ZONE).toInstant();
    }
}
