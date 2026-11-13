package com.diponegoro.ddb.topup;

import com.diponegoro.ddb.auth.TransactionGuard;
import com.diponegoro.ddb.common.ApiException;
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
import com.diponegoro.ddb.topup.TopUpCatalog.Biller;
import com.diponegoro.ddb.topup.TopUpCatalog.EWallet;
import com.diponegoro.ddb.topup.TopUpCatalog.Kind;
import com.diponegoro.ddb.topup.TopUpCatalog.Operator;
import com.diponegoro.ddb.topup.TopUpCatalog.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Modul Top-up: FR-18 – FR-20. Penyedia pulsa, PLN, dan biller diganti simulator (SRS 2.5).
 * Hasil inquiry deterministik dari nomor pelanggan (sama dengan aplikasi Android) agar mudah diuji.
 * Pesanan selalu disusun ulang di server dari parameter — nominal dari klien tidak dipercaya.
 */
@Service
public class TopUpService {
    private static final Locale ID = Locale.of("id", "ID");
    private static final List<String> NAMES = List.of("SRI WAHYUNI", "AGUS SETIAWAN", "DEWI KARTIKA", "HENDRA GUNAWAN",
            "LINA MARLINA", "RUDI HARTONO");
    private static final List<String> TARIFFS = List.of("R1/900VA", "R1/1300VA", "R1/2200VA", "R2/3500VA");

    // ------------------------------------------------------------------ model

    /** Parameter pesanan; field yang dipakai bergantung pada kind. */
    public record OrderRequest(Kind kind, String phone, Long nominal, String productId, String meterNumber,
                               Biller biller, String customerNumber, EWallet wallet, Long amount) {}

    /** Ringkasan pesanan untuk layar konfirmasi sebelum PIN. */
    public record Order(Kind kind, String title, String target, String targetName, long amount, long fee, long total,
                        Map<String, String> details, String billKey) {}

    public record PlnCustomer(String meterNumber, String name, String tariff) {}

    public record BillInquiry(Biller biller, String billerLabel, String customerNumber, String customerName,
                              String period, long amount, long fee, long total) {}

    public record DataPackages(Operator operator, List<Product> packages) {}

    private final LedgerService ledger;
    private final TransactionGuard guard;
    private final PaidBill.Repo paidBills;
    private final Clock clock;

    public TopUpService(LedgerService ledger, TransactionGuard guard, PaidBill.Repo paidBills, Clock clock) {
        this.ledger = ledger;
        this.guard = guard;
        this.paidBills = paidBills;
        this.clock = clock;
    }

    private static int seed(String number) {
        return Math.floorMod(number.hashCode(), 1_000_003);
    }

    private static String digits(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    // ------------------------------------------------------------------ FR-18

    public Operator operatorFor(String phone) {
        if (!TopUpCatalog.isValidPhone(phone)) {
            throw ApiException.badRequest("INVALID_PHONE", "Nomor HP harus diawali 08 dan 10–13 digit");
        }
        return TopUpCatalog.detectOperator(phone)
                .orElseThrow(() -> ApiException.badRequest("UNKNOWN_OPERATOR", "Operator untuk nomor ini tidak dikenali"));
    }

    public DataPackages dataPackages(String phone) {
        Operator op = operatorFor(phone);
        return new DataPackages(op, TopUpCatalog.dataPackages(op));
    }

    // ------------------------------------------------------------------ FR-19

    public PlnCustomer inquirePln(String rawMeter) {
        String meter = digits(rawMeter);
        if (meter.length() < 11 || meter.length() > 12) {
            throw ApiException.badRequest("INVALID_METER", "ID pelanggan / nomor meter harus 11–12 digit");
        }
        if (meter.endsWith("0000")) throw ApiException.unprocessable("CUSTOMER_NOT_FOUND", "ID pelanggan tidak ditemukan");
        return new PlnCustomer(meter, NAMES.get(seed(meter) % NAMES.size()), TARIFFS.get(seed(meter) % TARIFFS.size()));
    }

    // ------------------------------------------------------------------ FR-20

    public BillInquiry inquireBill(Biller biller, String rawNumber) {
        if (biller == null) throw ApiException.badRequest("INVALID_BILLER", "Pilih layanan tagihan");
        String number = digits(rawNumber);
        if (number.length() < biller.minLength || number.length() > biller.maxLength) {
            throw ApiException.badRequest("INVALID_CUSTOMER_NUMBER", biller.numberHint);
        }
        if (number.endsWith("0000")) throw ApiException.unprocessable("CUSTOMER_NOT_FOUND", "Nomor pelanggan tidak ditemukan");
        String period = DateTimeFormatter.ofPattern("MMMM yyyy", ID).format(clock.instant().atZone(TimeConfig.ZONE));
        if (paidBills.existsById(billKey(biller, number, period))) {
            throw ApiException.conflict("BILL_PAID", "Tagihan " + biller.label + " periode " + period + " sudah lunas");
        }
        int s = seed(number);
        long amount = switch (biller) {
            case PDAM -> 45_000L + (s % 20) * 7_500L;
            case BPJS -> List.of(42_000L, 100_000L, 150_000L).get(s % 3);
            case INTERNET -> List.of(250_000L, 330_000L, 385_000L, 450_000L).get(s % 4);
        };
        long fee = TopUpCatalog.BILL_ADMIN_FEE;
        return new BillInquiry(biller, biller.label, number, NAMES.get(s % NAMES.size()), period, amount, fee, amount + fee);
    }

    private static String billKey(Biller biller, String number, String period) {
        return biller.name() + ":" + number + ":" + period;
    }

    // ------------------------------------------------------------------ pesanan

    public Order quote(OrderRequest r) {
        if (r.kind() == null) throw ApiException.badRequest("INVALID_KIND", "Pilih jenis top-up");
        return switch (r.kind()) {
            case PULSA -> {
                Operator op = operatorFor(r.phone());
                long n = r.nominal() == null ? -1 : r.nominal();
                if (!TopUpCatalog.PULSA_NOMINALS.contains(n)) throw ApiException.badRequest("INVALID_NOMINAL", "Pilih nominal pulsa");
                yield order(Kind.PULSA, "Pulsa " + op.name() + " " + Money.rupiah(n), r.phone(), op.name(), n,
                        TopUpCatalog.PULSA_FEE, map("Operator", op.name(), "Nomor", r.phone(), "Nominal pulsa", Money.rupiah(n)), null);
            }
            case DATA -> {
                Operator op = operatorFor(r.phone());
                Product p = TopUpCatalog.dataPackages(op).stream().filter(x -> x.id().equals(r.productId())).findFirst()
                        .orElseThrow(() -> ApiException.badRequest("INVALID_PRODUCT", "Paket tidak tersedia untuk operator " + op.name()));
                yield order(Kind.DATA, "Paket data " + op.name() + " " + p.name(), r.phone(), op.name(), p.price(),
                        TopUpCatalog.PULSA_FEE, map("Operator", op.name(), "Nomor", r.phone(), "Paket", p.name() + " (" + p.description() + ")"), null);
            }
            case PLN -> {
                PlnCustomer c = inquirePln(r.meterNumber());
                long n = r.nominal() == null ? -1 : r.nominal();
                if (!TopUpCatalog.PLN_NOMINALS.contains(n)) throw ApiException.badRequest("INVALID_NOMINAL", "Pilih nominal token");
                yield order(Kind.PLN, "Token listrik " + Money.rupiah(n), c.meterNumber(), c.name(), n, TopUpCatalog.PLN_ADMIN_FEE,
                        map("ID pelanggan", c.meterNumber(), "Nama", c.name(), "Tarif/Daya", c.tariff()), null);
            }
            case BILL -> {
                BillInquiry b = inquireBill(r.biller(), r.customerNumber());
                yield order(Kind.BILL, "Tagihan " + b.billerLabel(), b.customerNumber(), b.customerName(), b.amount(), b.fee(),
                        map("Layanan", b.billerLabel(), "Nomor pelanggan", b.customerNumber(), "Nama", b.customerName(),
                                "Periode", b.period()), billKey(b.biller(), b.customerNumber(), b.period()));
            }
            case EWALLET -> {
                if (r.wallet() == null) throw ApiException.badRequest("INVALID_WALLET", "Pilih e-wallet");
                if (!TopUpCatalog.isValidPhone(r.phone())) {
                    throw ApiException.badRequest("INVALID_PHONE", "Nomor HP harus diawali 08 dan 10–13 digit");
                }
                long a = r.amount() == null ? 0 : r.amount();
                if (a < TopUpCatalog.EWALLET_MIN || a > TopUpCatalog.EWALLET_MAX) {
                    throw ApiException.badRequest("INVALID_AMOUNT", "Nominal " + Money.rupiah(TopUpCatalog.EWALLET_MIN)
                            + " – " + Money.rupiah(TopUpCatalog.EWALLET_MAX));
                }
                yield order(Kind.EWALLET, "Top-up " + r.wallet().label + " " + Money.rupiah(a), r.phone(),
                        r.wallet().label + " " + NAMES.get(seed(r.phone()) % NAMES.size()), a, TopUpCatalog.EWALLET_FEE,
                        map("E-wallet", r.wallet().label, "Nomor", r.phone()), null);
            }
        };
    }

    /** Saldo terdebet hanya setelah PIN valid (FR-18); bukti tersimpan di riwayat (FR-20). */
    @Transactional
    public TransactionDto execute(String userId, OrderRequest r, String pin, String idempotencyKey) {
        Optional<LedgerTransaction> existing = ledger.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) return ledger.dto(existing.get(), userId);

        Order o = quote(r);
        LedgerAccount main = ledger.requireMainAccount(userId);
        if (main.balance < o.total()) throw ApiException.unprocessable("INSUFFICIENT_BALANCE", "Saldo tidak mencukupi");
        guard.authorize(userId, pin);

        Map<String, String> extra = new LinkedHashMap<>(o.details());
        if (o.kind() == Kind.PLN) {
            extra.put("Token", token(idempotencyKey));
            extra.put("Jumlah kWh", String.format(ID, "%.1f kWh", o.amount() / TopUpCatalog.PLN_RATE_PER_KWH));
        }
        if (o.kind() == Kind.PULSA || o.kind() == Kind.DATA) {
            extra.put("Serial number", "SN" + String.format("%010d", Math.abs((long) idempotencyKey.hashCode())));
        }
        LedgerTransaction tx = ledger.post(PostingRequest.builder(idempotencyKey, TxCategory.TOPUP, o.title(), o.amount())
                .fee(o.fee())
                .lines(new Line(main.id, -o.total()), new Line(External.BILLER, o.amount()), new Line(External.FEE_INCOME, o.fee()))
                .debitUser(userId)
                .from(main.name, main.number)
                .to(o.targetName(), o.kind().label + " • " + o.target())
                .extra(extra)
                .build());
        if (o.billKey() != null) {
            PaidBill pb = new PaidBill();
            pb.billKey = o.billKey();
            pb.txId = tx.id;
            pb.paidAt = clock.instant();
            paidBills.saveAndFlush(pb);    // PK unik → tagihan yang sama tidak bisa dibayar dua kali
        }
        return ledger.dto(tx, userId);
    }

    /** Token 20 digit, deterministik per transaksi agar retry menampilkan token yang sama. */
    private static String token(String key) {
        Random random = new Random(key.hashCode());
        String d = IntStream.range(0, 20).mapToObj(i -> String.valueOf(random.nextInt(10))).collect(Collectors.joining());
        return String.join("-", d.substring(0, 4), d.substring(4, 8), d.substring(8, 12), d.substring(12, 16), d.substring(16));
    }

    private static Order order(Kind kind, String title, String target, String targetName, long amount, long fee,
                               Map<String, String> details, String billKey) {
        return new Order(kind, title, target, targetName, amount, fee, amount + fee, details, billKey);
    }

    private static Map<String, String> map(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }
}
