package com.diponegoro.ddb.demo;

import com.diponegoro.ddb.auth.AuthEntities.AccountStatus;
import com.diponegoro.ddb.auth.AuthEntities.EkycStatus;
import com.diponegoro.ddb.auth.AuthRepositories.EkycRepository;
import com.diponegoro.ddb.auth.AuthRepositories.UserRepository;
import com.diponegoro.ddb.auth.EkycVerification;
import com.diponegoro.ddb.auth.PinService;
import com.diponegoro.ddb.auth.User;
import com.diponegoro.ddb.common.DdbProperties;
import com.diponegoro.ddb.ledger.LedgerAccount;
import com.diponegoro.ddb.ledger.LedgerModels.External;
import com.diponegoro.ddb.ledger.LedgerModels.Line;
import com.diponegoro.ddb.ledger.LedgerModels.PostingRequest;
import com.diponegoro.ddb.ledger.LedgerModels.TxCategory;
import com.diponegoro.ddb.ledger.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data simulasi (SRS 2.5) agar riwayat, grafik saldo, dan dashboard langsung berisi — sama dengan
 * aplikasi Android. Hanya berjalan bila ddb.seed-demo-data=true dan akun "demo" belum ada.
 *
 * Akun: demo / Demo@1234 / PIN 123456 (rekening 1400123456).
 * Nasabah lain: andi, budi, citra (password & PIN sama) — rekening 1400000002–1400000004.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    public static final String PASSWORD = "Demo@1234";
    public static final String PIN = "123456";

    private record Person(String id, String username, String name, String number, String phone, long opening, int daysAgo) {}

    private static final List<Person> PEOPLE = List.of(
            new Person("cust-andi", "andi", "ANDI PRATAMA", "1400000002", "081311110002", 2_000_000, 200),
            new Person("cust-budi", "budi", "BUDI SANTOSO", "1400000003", "081311110003", 2_000_000, 200),
            new Person("cust-citra", "citra", "CITRA LESTARI", "1400000004", "081311110004", 2_000_000, 200),
            new Person("user-demo", "demo", "DEMO", "1400123456", "081234567890", 3_000_000, 150));

    private final DdbProperties props;
    private final UserRepository users;
    private final EkycRepository ekycs;
    private final PinService pins;
    private final LedgerService ledger;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final TransactionTemplate tx;

    public DemoDataSeeder(DdbProperties props, UserRepository users, EkycRepository ekycs, PinService pins,
                          LedgerService ledger, PasswordEncoder encoder, Clock clock, PlatformTransactionManager tm) {
        this.tx = new TransactionTemplate(tm);
        this.props = props;
        this.users = users;
        this.ekycs = ekycs;
        this.pins = pins;
        this.ledger = ledger;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.seedDemoData() || users.existsByUsername("demo")) return;
        tx.executeWithoutResult(status -> seed());   // atomik: gagal di tengah = tidak ada data setengah jadi
        log.info("Data demo dibuat: login demo / {} / PIN {}", PASSWORD, PIN);
    }

    public void seed() {
        Instant now = clock.instant();
        String hash = encoder.encode(PASSWORD);
        for (Person p : PEOPLE) {
            Instant at = daysAgo(now, p.daysAgo(), 10);
            User u = new User();
            u.id = p.id();
            u.username = p.username();
            u.email = p.username() + "@diponegoro.bank";
            u.phoneNumber = p.phone();
            u.passwordHash = hash;
            u.status = AccountStatus.FINANCIAL_ENABLED;
            u.createdAt = at;
            u.updatedAt = at;
            users.saveAndFlush(u);
            EkycVerification e = new EkycVerification();
            e.id = "ekyc-" + p.username();
            e.userId = p.id();
            e.nikMasked = "************0001";
            e.nikHash = hash;
            e.fullName = p.name();
            e.selfieReference = "selfie_" + p.username() + ".jpg";
            e.status = EkycStatus.APPROVED;
            e.submittedAt = at;
            e.decidedAt = at;
            ekycs.save(e);
            pins.reset(p.id(), PIN);
            ledger.openMainAccount(p.id(), p.name(), p.opening(), p.number(), at);
        }
        LedgerAccount main = ledger.mainAccount("user-demo").orElseThrow();
        int[] seq = {0};

        // Lima bulan terakhir: gaji bulanan + pengeluaran rutin.
        for (int month = 4; month >= 0; month--) {
            int base = month * 30 + 25;
            income(main, seq, now, base, 8_500_000, "PT SINAR TEMBALANG", "Gaji bulanan");
            expense(main, seq, now, base - 1, 1_500_000, 2_500, TxCategory.TRANSFER, "Transfer antarbank", "IBU KOS MAWAR",
                    "BCA • 1234567890", External.INTERBANK);
            expense(main, seq, now, base - 3, 100_000, 1_500, TxCategory.TOPUP, "Pulsa Telkomsel Rp100.000", "Telkomsel",
                    "Pulsa • 081234567890", External.BILLER);
            expense(main, seq, now, base - 5, 200_000, 2_500, TxCategory.TOPUP, "Token listrik Rp200.000", "SRI WAHYUNI",
                    "Token Listrik • 12345678901", External.BILLER);
            expense(main, seq, now, base - 8, 45_000 + month * 7_000L, 0, TxCategory.QRIS, "Pembayaran QRIS", "WARUNG BU SRI",
                    "QRIS statis • M0001", External.MERCHANT);
            expense(main, seq, now, base - 12, 1_200_000 + month * 150_000L, 0, TxCategory.QRIS, "Pembayaran QRIS",
                    "TOKO BUKU DIPONEGORO", "QRIS dinamis • M0003", External.MERCHANT);
            expense(main, seq, now, base - 18, 750_000, 2_500, TxCategory.TRANSFER, "Transfer antarbank", "ORANG TUA",
                    "BRI • 0012345678", External.INTERBANK);
        }
        income(main, seq, now, 3, 350_000, "ANDI PRATAMA", "Transfer masuk (patungan)");
    }

    private void income(LedgerAccount main, int[] seq, Instant now, int day, long amount, String from, String title) {
        if (day < 0) return;
        ledger.post(PostingRequest.builder("seed:" + seq[0]++, TxCategory.TRANSFER, title, amount)
                .lines(new Line(External.FUNDING, -amount), new Line(main.id, amount))
                .creditUser("user-demo")
                .from(from, "Bank lain").to(main.name, main.number)
                .at(daysAgo(now, day, 9))
                .build());
    }

    private void expense(LedgerAccount main, int[] seq, Instant now, int day, long amount, long fee, TxCategory category,
                         String title, String to, String detail, String external) {
        if (day < 0) return;
        List<Line> lines = new ArrayList<>(List.of(new Line(main.id, -(amount + fee)), new Line(external, amount)));
        if (fee > 0) lines.add(new Line(External.FEE_INCOME, fee));
        ledger.post(PostingRequest.builder("seed:" + seq[0]++, category, title, amount)
                .fee(fee).lines(lines)
                .debitUser("user-demo")
                .from(main.name, main.number).to(to, detail)
                .extra(Map.of())
                .at(daysAgo(now, day, 15))
                .build());
    }

    private static Instant daysAgo(Instant now, int days, int hour) {
        return now.minus(Duration.ofDays(days)).plus(Duration.ofHours(hour - 12L));
    }
}
