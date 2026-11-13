package com.diponegoro.ddb.ledger;


import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Model Financial Transaction Core (sama dengan core/ledger di aplikasi Android). */
public final class LedgerModels {
    private LedgerModels() {}

    public enum AccountKind { MAIN, POCKET, EXTERNAL }

    public enum TxCategory {
        DEPOSIT("Setoran"), TRANSFER("Transfer"), QRIS("QRIS"), TOPUP("Top-up & Tagihan"),
        POCKET("Kantong"), CARD("Kartu Virtual");
        public final String label;
        TxCategory(String label) { this.label = label; }
    }

    public enum TxStatus {
        SUCCESS("Berhasil"), PROCESSING("Diproses"), FAILED("Gagal");
        public final String label;
        TxStatus(String label) { this.label = label; }
    }

    /** Arah transaksi dari sudut pandang satu pengguna. */
    public enum Direction { IN, OUT, INTERNAL }

    /** Rekening eksternal milik simulator (dibuat oleh migrasi V1). */
    public static final class External {
        public static final String INTERBANK = "ext:interbank";
        public static final String MERCHANT = "ext:merchant";
        public static final String BILLER = "ext:biller";
        public static final String CARD = "ext:card";
        public static final String FEE_INCOME = "ext:fee";
        public static final String FUNDING = "ext:funding";
        private External() {}
    }

    /** Satu baris permintaan posting. */
    public record Line(String accountId, long amount) {}

    /**
     * Permintaan posting. Jumlah seluruh {@code lines} wajib 0 (double-entry).
     * {@code at} hanya untuk data seed historis.
     */
    public record PostingRequest(
            String idempotencyKey, TxCategory category, String title, long amount, long fee, List<Line> lines,
            String debitUserId, String creditUserId, String fromName, String fromDetail, String toName,
            String toDetail, String note, TxStatus status, Map<String, String> extra, Instant at) {

        public static Builder builder(String idempotencyKey, TxCategory category, String title, long amount) {
            return new Builder(idempotencyKey, category, title, amount);
        }

        public static final class Builder {
            private final String key; private final TxCategory category; private final String title;
            private final long amount;
            private long fee; private List<Line> lines = List.of();
            private String debitUserId, creditUserId, fromName = "", fromDetail = "", toName = "", toDetail = "";
            private String note = ""; private TxStatus status = TxStatus.SUCCESS;
            private Map<String, String> extra = Map.of(); private Instant at;

            private Builder(String key, TxCategory category, String title, long amount) {
                this.key = key; this.category = category; this.title = title; this.amount = amount;
            }
            public Builder fee(long v) { fee = v; return this; }
            public Builder lines(Line... v) { lines = List.of(v); return this; }
            public Builder lines(List<Line> v) { lines = List.copyOf(v); return this; }
            public Builder debitUser(String v) { debitUserId = v; return this; }
            public Builder creditUser(String v) { creditUserId = v; return this; }
            public Builder from(String name, String detail) { fromName = name; fromDetail = detail; return this; }
            public Builder to(String name, String detail) { toName = name; toDetail = detail; return this; }
            public Builder note(String v) { note = v == null ? "" : v; return this; }
            public Builder status(TxStatus v) { status = v; return this; }
            public Builder extra(Map<String, String> v) { extra = v; return this; }
            public Builder at(Instant v) { at = v; return this; }

            public PostingRequest build() {
                return new PostingRequest(key, category, title, amount, fee, lines, debitUserId, creditUserId,
                        fromName, fromDetail, toName, toDetail, note, status, extra, at);
            }
        }
    }

    /** Event setelah transaksi tersimpan (didengar modul notifikasi setelah commit). */
    public record TransactionPostedEvent(String txId) {}

    /** Event saat status transaksi berubah (PROCESSING → SUCCESS / FAILED). */
    public record TransactionStatusChangedEvent(String txId) {}
}
