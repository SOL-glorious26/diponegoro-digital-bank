package com.diponegoro.ddb.ledger;

import com.diponegoro.ddb.common.ApiException;
import com.diponegoro.ddb.common.Ids;
import com.diponegoro.ddb.common.TimeConfig;
import com.diponegoro.ddb.ledger.LedgerModels.AccountKind;
import com.diponegoro.ddb.ledger.LedgerModels.External;
import com.diponegoro.ddb.ledger.LedgerModels.Line;
import com.diponegoro.ddb.ledger.LedgerModels.PostingRequest;
import com.diponegoro.ddb.ledger.LedgerModels.TransactionPostedEvent;
import com.diponegoro.ddb.ledger.LedgerModels.TransactionStatusChangedEvent;
import com.diponegoro.ddb.ledger.LedgerModels.TxCategory;
import com.diponegoro.ddb.ledger.LedgerModels.TxStatus;
import com.diponegoro.ddb.ledger.LedgerRepositories.AccountRepository;
import com.diponegoro.ddb.ledger.LedgerRepositories.JournalRepository;
import com.diponegoro.ddb.ledger.LedgerRepositories.TransactionRepository;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Financial Transaction Core (BR-03, BR-04, BR-05, NFR-07).
 *
 * <ul>
 *   <li><b>Double-entry</b>: jumlah baris jurnal setiap transaksi wajib 0.</li>
 *   <li><b>Atomik</b>: satu transaksi database; rekening yang terlibat dikunci (SELECT … FOR UPDATE)
 *       berurutan id, divalidasi, lalu jurnal + cache saldo ditulis. Gagal = rollback total.</li>
 *   <li><b>Idempotensi</b>: kunci yang sama mengembalikan transaksi yang sudah ada (tanpa debit ganda).
 *       Pemeriksaan dilakukan <i>setelah</i> mengunci rekening sehingga dua permintaan paralel dengan
 *       kunci sama berjalan berurutan.</li>
 *   <li><b>Jejak audit</b>: jurnal append-only; pengembalian dana = baris pembalik.</li>
 * </ul>
 */
@Service
public class LedgerService {
    private static final DateTimeFormatter REF_DATE = DateTimeFormatter.ofPattern("yyMMdd").withZone(TimeConfig.ZONE);

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final JournalRepository journal;
    private final ApplicationEventPublisher events;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final EntityManager em;

    public LedgerService(AccountRepository accounts, TransactionRepository transactions, JournalRepository journal,
                         ApplicationEventPublisher events, ObjectMapper mapper, Clock clock, EntityManager em) {
        this.em = em;
        this.accounts = accounts;
        this.transactions = transactions;
        this.journal = journal;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ rekening

    public Optional<LedgerAccount> mainAccount(String userId) {
        return accounts.findFirstByOwnerUserIdAndKind(userId, AccountKind.MAIN);
    }

    public LedgerAccount requireMainAccount(String userId) {
        return mainAccount(userId).orElseThrow(() -> ApiException.forbidden("ACCOUNT_NOT_OPEN",
                "Rekening belum dibuka. Selesaikan verifikasi e-KYC terlebih dahulu."));
    }

    public Optional<LedgerAccount> findMainByNumber(String number) {
        return accounts.findByNumberAndKindAndClosedFalse(number, AccountKind.MAIN);
    }

    public List<LedgerAccount> pockets(String userId) {
        return accounts.findByOwnerUserIdAndKindAndClosedFalseOrderByCreatedAt(userId, AccountKind.POCKET);
    }

    public LedgerAccount account(String id) {
        return accounts.findById(id).orElseThrow(() -> ApiException.notFound("Rekening tidak ditemukan"));
    }

    /** Membuka rekening utama bila belum ada, dengan setoran awal simulasi. */
    @Transactional
    public LedgerAccount openMainAccount(String userId, String ownerName, long openingBalance, String fixedNumber,
                                         Instant at) {
        Optional<LedgerAccount> existing = mainAccount(userId);
        if (existing.isPresent()) return existing.get();
        LedgerAccount a = newAccount(userId, AccountKind.MAIN, fixedNumber != null ? fixedNumber : newNumber("1400", 6),
                ownerName.toUpperCase(), at);
        if (openingBalance > 0) {
            post(PostingRequest.builder("opening:" + userId, TxCategory.DEPOSIT, "Setoran awal (simulasi)", openingBalance)
                    .lines(new Line(External.FUNDING, -openingBalance), new Line(a.id, openingBalance))
                    .creditUser(userId)
                    .from("Setoran awal", "Saldo simulasi")
                    .to(a.name, a.number)
                    .at(at)
                    .build());
        }
        return a;
    }

    @Transactional
    public LedgerAccount openPocket(String userId, String name) {
        return newAccount(userId, AccountKind.POCKET, newNumber("P", 8), name, clock.instant());
    }

    @Transactional
    public void closeAccount(String accountId) {
        LedgerAccount a = lock(List.of(accountId)).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("Rekening tidak ditemukan"));
        if (a.balance != 0) throw ApiException.unprocessable("BALANCE_NOT_ZERO", "Saldo harus Rp0 sebelum ditutup");
        a.closed = true;
    }

    private LedgerAccount newAccount(String userId, AccountKind kind, String number, String name, Instant at) {
        LedgerAccount a = new LedgerAccount();
        a.id = Ids.newId(kind == AccountKind.POCKET ? "pocket" : "acc");
        a.ownerUserId = userId;
        a.kind = kind;
        a.number = number;
        a.name = name;
        a.balance = 0;
        a.createdAt = at;
        em.persist(a);
        em.flush();
        return a;
    }

    private String newNumber(String prefix, int digits) {
        for (int i = 0; i < 20; i++) {
            String n = prefix + Ids.digits(digits);
            if (!accounts.existsByNumber(n)) return n;
        }
        throw new IllegalStateException("Gagal membuat nomor rekening unik");
    }

    // ------------------------------------------------------------------ saldo

    public long balance(String accountId) {
        return account(accountId).balance;
    }

    /** Saldo pada suatu waktu (untuk grafik saldo FR-07), dihitung langsung dari jurnal. */
    public long balanceAt(String accountId, Instant at) {
        return journal.balanceAt(accountId, at);
    }

    // ------------------------------------------------------------------ transaksi

    public LedgerTransaction transaction(String id) {
        return transactions.findById(id).orElseThrow(() -> ApiException.notFound("Transaksi tidak ditemukan"));
    }

    /** Transaksi yang melibatkan pengguna; transaksi milik orang lain dianggap tidak ada. */
    public LedgerTransaction visibleTransaction(String txId, String userId) {
        LedgerTransaction t = transactions.findById(txId).orElse(null);
        if (t == null || !(userId.equals(t.debitUserId) || userId.equals(t.creditUserId))) {
            throw ApiException.notFound("Transaksi tidak ditemukan");
        }
        return t;
    }

    public TransactionDto dto(LedgerTransaction t, String viewerUserId) {
        return TransactionDto.of(t, viewerUserId, extra(t));
    }

    public Optional<LedgerTransaction> findByIdempotencyKey(String key) {
        return transactions.findByIdempotencyKey(key);
    }

    public List<JournalLine> journalFor(String txId) {
        return journal.findByTxIdOrderById(txId);
    }

    public Map<String, String> extra(LedgerTransaction tx) {
        if (tx.extra == null || tx.extra.isBlank()) return Map.of();
        return mapper.readValue(tx.extra, new TypeReference<LinkedHashMap<String, String>>() {});
    }

    /**
     * Memposting transaksi. Melempar {@link ApiException} (422) bila saldo tidak cukup / rekening tutup;
     * pemanggil yang sudah berada dalam transaksi ikut di-rollback.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public LedgerTransaction post(PostingRequest r) {
        if (r.lines().isEmpty()) throw new IllegalArgumentException("Transaksi tidak memiliki baris jurnal");
        if (r.lines().stream().mapToLong(Line::amount).sum() != 0) throw new IllegalArgumentException("Jurnal tidak seimbang");
        if (r.amount() <= 0) throw ApiException.unprocessable("INVALID_AMOUNT", "Nominal harus lebih dari 0");

        // 1) Kunci semua rekening yang terlibat (urut id → bebas deadlock).
        Map<String, Long> deltas = new LinkedHashMap<>();
        r.lines().forEach(l -> deltas.merge(l.accountId(), l.amount(), Long::sum));
        List<LedgerAccount> locked = lock(deltas.keySet());
        if (locked.size() != deltas.size()) throw ApiException.notFound("Rekening tidak ditemukan");

        // 2) BR-04: kunci idempotensi yang sama → kembalikan hasil sebelumnya.
        Optional<LedgerTransaction> existing = transactions.findByIdempotencyKey(r.idempotencyKey());
        if (existing.isPresent()) return existing.get();

        // 3) Validasi seluruh rekening sebelum menulis apa pun.
        for (LedgerAccount a : locked) {
            long delta = deltas.get(a.id);
            if (a.closed) throw ApiException.unprocessable("ACCOUNT_CLOSED", "Rekening " + a.number + " sudah ditutup");
            if (a.kind != AccountKind.EXTERNAL && a.balance + delta < 0) {
                throw ApiException.unprocessable("INSUFFICIENT_BALANCE",
                        a.kind == AccountKind.POCKET ? "Saldo kantong tidak mencukupi" : "Saldo tidak mencukupi");
            }
        }

        // 4) Tulis transaksi, jurnal, dan cache saldo.
        Instant now = r.at() != null ? r.at() : clock.instant();
        LedgerTransaction tx = new LedgerTransaction();
        tx.id = Ids.newId("trx");
        tx.reference = newReference(now);
        tx.idempotencyKey = r.idempotencyKey();
        tx.category = r.category();
        tx.status = r.status();
        tx.amount = r.amount();
        tx.fee = r.fee();
        tx.title = r.title();
        tx.debitUserId = r.debitUserId();
        tx.creditUserId = r.creditUserId();
        tx.fromName = r.fromName();
        tx.fromDetail = r.fromDetail();
        tx.toName = r.toName();
        tx.toDetail = r.toDetail();
        tx.note = r.note() == null ? "" : r.note();
        tx.extra = r.extra() == null || r.extra().isEmpty() ? null : mapper.writeValueAsString(r.extra());
        tx.createdAt = now;
        tx.updatedAt = now;
        em.persist(tx);

        List<JournalLine> lines = new ArrayList<>();
        for (Line l : r.lines()) {
            if (l.amount() == 0) continue;
            JournalLine j = new JournalLine();
            j.txId = tx.id;
            j.accountId = l.accountId();
            j.amount = l.amount();
            j.memo = truncate(r.title(), 120);
            j.postedAt = now;
            lines.add(j);
        }
        lines.forEach(em::persist);
        for (LedgerAccount a : locked) a.balance += deltas.get(a.id);
        em.flush();

        postedInThisTransaction().add(tx.id);
        events.publishEvent(new TransactionPostedEvent(tx.id));
        return tx;
    }

    private static final Object POSTED_KEY = new Object();

    /**
     * Id transaksi yang diposting di transaksi database yang sedang berjalan. Perubahan status di
     * transaksi DB yang sama tidak perlu event terpisah: event "posted" diproses setelah commit dan
     * sudah membaca status akhirnya (mencegah notifikasi ganda).
     */
    @SuppressWarnings("unchecked")
    private static Set<String> postedInThisTransaction() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return new HashSet<>();
        Set<String> set = (Set<String>) TransactionSynchronizationManager.getResource(POSTED_KEY);
        if (set == null) {
            Set<String> created = new HashSet<>();
            TransactionSynchronizationManager.bindResource(POSTED_KEY, created);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    TransactionSynchronizationManager.unbindResourceIfPossible(POSTED_KEY);
                }
            });
            set = created;
        }
        return set;
    }

    private void publishStatusChanged(String txId) {
        if (!postedInThisTransaction().contains(txId)) events.publishEvent(new TransactionStatusChangedEvent(txId));
    }

    /** Menyelesaikan transaksi berstatus PROCESSING (hasil cek ulang status antarbank, UC-01 A5). */
    @Transactional
    public LedgerTransaction complete(String txId) {
        LedgerTransaction tx = transaction(txId);
        if (tx.status != TxStatus.PROCESSING) return tx;
        tx.status = TxStatus.SUCCESS;
        tx.statusReason = null;
        tx.updatedAt = clock.instant();
        publishStatusChanged(tx.id);
        return tx;
    }

    /** Menandai gagal dan mengembalikan dana dengan baris jurnal pembalik (BR-05, UC-01 A6). */
    @Transactional
    public LedgerTransaction failWithRefund(String txId, String reason) {
        LedgerTransaction tx = transaction(txId);
        if (tx.status == TxStatus.FAILED) return tx;
        List<JournalLine> original = journal.findByTxIdOrderById(txId);
        Map<String, Long> deltas = new LinkedHashMap<>();
        original.forEach(l -> deltas.merge(l.accountId, -l.amount, Long::sum));
        List<LedgerAccount> locked = lock(deltas.keySet());
        Instant now = clock.instant();
        List<JournalLine> reversal = new ArrayList<>();
        for (JournalLine l : original) {
            JournalLine j = new JournalLine();
            j.txId = txId;
            j.accountId = l.accountId;
            j.amount = -l.amount;
            j.memo = "Pengembalian dana";
            j.postedAt = now;
            reversal.add(j);
        }
        reversal.forEach(em::persist);
        for (LedgerAccount a : locked) a.balance += deltas.get(a.id);
        tx.status = TxStatus.FAILED;
        tx.statusReason = reason;
        tx.updatedAt = now;
        accounts.flush();
        publishStatusChanged(tx.id);
        return tx;
    }

    /**
     * Mengunci baris rekening (SELECT … FOR UPDATE) berurutan id agar bebas deadlock, lalu memuat ulang
     * isinya dari database. Refresh WAJIB: entitas yang sudah ada di persistence context (mis. dibaca
     * saat validasi) menyimpan saldo lama, dan tanpa refresh dua transaksi paralel bisa menimpa saldo
     * satu sama lain (lost update). Dibuktikan oleh test transfer paralel.
     */
    private List<LedgerAccount> lock(java.util.Collection<String> ids) {
        List<LedgerAccount> out = new ArrayList<>();
        for (String id : new java.util.TreeSet<>(ids)) {
            LedgerAccount a = em.find(LedgerAccount.class, id);
            if (a == null) continue;
            em.refresh(a, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            out.add(a);
        }
        return out;
    }

    private static String newReference(Instant at) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "DDB" + REF_DATE.format(at) + suffix;
    }

    private static String truncate(String s, int max) {
        return s == null ? "" : s.length() <= max ? s : s.substring(0, max);
    }
}
