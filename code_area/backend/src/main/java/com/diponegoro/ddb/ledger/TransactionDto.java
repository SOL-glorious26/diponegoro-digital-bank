package com.diponegoro.ddb.ledger;

import com.diponegoro.ddb.ledger.LedgerModels.Direction;
import com.diponegoro.ddb.ledger.LedgerModels.TxCategory;
import com.diponegoro.ddb.ledger.LedgerModels.TxStatus;

import java.time.Instant;
import java.util.Map;

/**
 * Bentuk transaksi yang dikirim ke klien — dipakai untuk bukti transaksi (FR-14), riwayat (FR-23)
 * dan detail (FR-24). Arah (masuk/keluar) dihitung dari sudut pandang pengguna yang meminta.
 */
public record TransactionDto(
        String id, String reference, TxCategory category, String categoryLabel, TxStatus status,
        String statusLabel, Direction direction, long amount, long fee, long total, String title,
        String fromName, String fromDetail, String toName, String toDetail, String note, String statusReason,
        Map<String, String> extra, Instant createdAt, Instant updatedAt) {

    public static TransactionDto of(LedgerTransaction t, String viewerUserId, Map<String, String> extra) {
        return new TransactionDto(t.id, t.reference, t.category, t.category.label, t.status, t.status.label,
                t.directionFor(viewerUserId), t.amount, t.fee, t.total(), t.title, t.fromName, t.fromDetail,
                t.toName, t.toDetail, t.note, t.statusReason, extra, t.createdAt, t.updatedAt);
    }
}
