package com.diponegoro.ddb.topup;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Katalog produk top-up (sama dengan aplikasi Android). Nilai biaya adalah usulan. */
public final class TopUpCatalog {
    private TopUpCatalog() {}

    public enum Kind {
        PULSA("Pulsa"), DATA("Paket Data"), PLN("Token Listrik"), BILL("Tagihan"), EWALLET("Top-up E-Wallet");
        public final String label;
        Kind(String label) { this.label = label; }
    }

    public enum Biller {
        PDAM("Air PDAM", "Nomor pelanggan PDAM (8–12 digit)", 8, 12),
        BPJS("BPJS Kesehatan", "Nomor VA BPJS (13–16 digit)", 13, 16),
        INTERNET("Internet Rumah", "Nomor pelanggan (10–12 digit)", 10, 12);
        public final String label, numberHint;
        public final int minLength, maxLength;
        Biller(String label, String hint, int min, int max) { this.label = label; this.numberHint = hint; minLength = min; maxLength = max; }
    }

    public enum EWallet {
        GOPAY("GoPay"), OVO("OVO"), DANA("DANA"), SHOPEEPAY("ShopeePay");
        public final String label;
        EWallet(String label) { this.label = label; }
    }

    public record Operator(String code, String name, List<String> prefixes) {}

    public record Product(String id, String name, String description, long price) {}

    public static final List<Operator> OPERATORS = List.of(
            new Operator("TSEL", "Telkomsel", List.of("0811", "0812", "0813", "0821", "0822", "0823", "0851", "0852", "0853")),
            new Operator("ISAT", "Indosat Ooredoo", List.of("0814", "0815", "0816", "0855", "0856", "0857", "0858")),
            new Operator("XL", "XL", List.of("0817", "0818", "0819", "0859", "0877", "0878")),
            new Operator("AXIS", "AXIS", List.of("0831", "0832", "0833", "0838")),
            new Operator("TRI", "Tri", List.of("0895", "0896", "0897", "0898", "0899")),
            new Operator("SF", "Smartfren", List.of("0881", "0882", "0883", "0884", "0885", "0886", "0887", "0888", "0889")));

    public static final long PULSA_FEE = 1_500L;
    public static final long PLN_ADMIN_FEE = 2_500L;
    public static final long BILL_ADMIN_FEE = 2_500L;
    public static final long EWALLET_FEE = 1_000L;
    /** Tarif simulasi per kWh. */
    public static final double PLN_RATE_PER_KWH = 1_444.70;
    public static final List<Long> PULSA_NOMINALS = List.of(5_000L, 10_000L, 20_000L, 25_000L, 50_000L, 100_000L, 200_000L);
    public static final List<Long> PLN_NOMINALS = List.of(20_000L, 50_000L, 100_000L, 200_000L, 500_000L, 1_000_000L);
    public static final long EWALLET_MIN = 10_000L;
    public static final long EWALLET_MAX = 2_000_000L;

    private static final Pattern PHONE = Pattern.compile("^08[0-9]{8,11}$");

    public static List<Product> dataPackages(Operator op) {
        return List.of(
                new Product(op.code() + "-D1", "Harian 1 GB", "Berlaku 1 hari", 5_000),
                new Product(op.code() + "-W5", "Mingguan 5 GB", "Berlaku 7 hari", 25_000),
                new Product(op.code() + "-M15", "Bulanan 15 GB", "Berlaku 30 hari", 75_000),
                new Product(op.code() + "-M30", "Bulanan 30 GB", "Berlaku 30 hari", 110_000));
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE.matcher(phone).matches();
    }

    public static Optional<Operator> detectOperator(String phone) {
        if (phone == null || phone.length() < 4) return Optional.empty();
        return OPERATORS.stream().filter(o -> o.prefixes().stream().anyMatch(phone::startsWith)).findFirst();
    }
}
