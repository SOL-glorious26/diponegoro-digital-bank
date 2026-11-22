import { useEffect, useState } from "react";
import { ArrowDownLeft, ArrowUpRight, Printer, Repeat } from "lucide-react";
import { api, errorMessage } from "../api/client";
import { dateTime, rupiah, signedRupiah } from "../lib/format";
import { Alert, Button, Modal, Rows, Spinner, StatusBadge } from "./ui";

export function TxIcon({ direction }) {
  const Icon = direction === "IN" ? ArrowDownLeft : direction === "OUT" ? ArrowUpRight : Repeat;
  return <span className={`tx-icon tx-${(direction || "").toLowerCase()}`}><Icon size={18} aria-hidden /></span>;
}

export function TxRow({ tx, onClick }) {
  return (
    <button className="tx-row" onClick={onClick}>
      <TxIcon direction={tx.direction} />
      <div className="tx-main">
        <div className="tx-title">{tx.title}</div>
        <div className="tx-sub">{tx.categoryLabel} · {dateTime(tx.createdAt)}</div>
      </div>
      <div className="tx-right">
        <div className={`tx-amount ${tx.direction === "IN" ? "pos" : ""}`}>{signedRupiah(tx.total ?? tx.amount, tx.direction)}</div>
        {tx.status !== "SUCCESS" && <StatusBadge status={tx.status} label={tx.statusLabel} />}
      </div>
    </button>
  );
}

// Isi bukti transaksi (FR-14 / FR-24). Dipakai setelah transaksi dan di detail riwayat.
export function Receipt({ tx, journal }) {
  const extra = tx.extra && typeof tx.extra === "object" ? Object.entries(tx.extra).filter(([, v]) => v != null && typeof v !== "object") : [];
  return (
    <div className="receipt" id="receipt">
      <div className="receipt-head">
        <StatusBadge status={tx.status} label={tx.statusLabel} />
        <div className="receipt-amount">{rupiah(tx.total ?? tx.amount)}</div>
        <div className="muted">{tx.title}</div>
      </div>
      <Rows items={[
        ["ID transaksi", <code key="id">{tx.reference || tx.id}</code>],
        ["Waktu", dateTime(tx.createdAt)],
        ["Jenis", tx.categoryLabel],
        tx.fromName && ["Dari", `${tx.fromName}${tx.fromDetail ? ` · ${tx.fromDetail}` : ""}`],
        tx.toName && ["Ke", `${tx.toName}${tx.toDetail ? ` · ${tx.toDetail}` : ""}`],
        ["Nominal", rupiah(tx.amount)],
        ["Biaya", rupiah(tx.fee ?? 0)],
        ["Total", rupiah(tx.total ?? tx.amount)],
        tx.note && ["Catatan", tx.note],
        tx.statusReason && ["Keterangan", tx.statusReason],
        ...extra.map(([k, v]) => [labelize(k), String(v)]),
      ]} />
      {journal?.length > 0 && (
        <details className="journal">
          <summary>Jurnal ledger ({journal.length} baris)</summary>
          <table>
            <thead><tr><th>Akun</th><th className="num">Jumlah</th><th>Memo</th></tr></thead>
            <tbody>
              {journal.map((j, i) => (
                <tr key={i}><td>{j.account}</td><td className="num">{rupiah(j.amount)}</td><td>{j.memo}</td></tr>
              ))}
            </tbody>
          </table>
        </details>
      )}
    </div>
  );
}

const labelize = (k) => k.replace(/([A-Z])/g, " $1").replace(/^./, (c) => c.toUpperCase());

export function TxDetailModal({ txId, onClose, onChanged }) {
  const [data, setData] = useState(null);
  const [err, setErr] = useState(null);
  const [checking, setChecking] = useState(false);

  useEffect(() => {
    api.get(`/history/${txId}`).then((r) => setData(r.data)).catch((ex) => setErr(errorMessage(ex)));
  }, [txId]);

  async function checkStatus() {
    setChecking(true);
    try {
      const { data: tx } = await api.post(`/transfers/${txId}/check-status`);
      setData((d) => ({ ...d, transaction: tx }));
      onChanged?.();
    } catch (ex) { setErr(errorMessage(ex)); } finally { setChecking(false); }
  }

  return (
    <Modal title="Detail transaksi" onClose={onClose} width={520}>
      <Alert>{err}</Alert>
      {!data && !err && <Spinner label="Memuat…" />}
      {data && (
        <>
          <Receipt tx={data.transaction} journal={data.journal} />
          <div className="modal-actions no-print">
            {data.transaction.status === "PROCESSING" && data.transaction.category === "TRANSFER" && (
              <Button variant="secondary" loading={checking} onClick={checkStatus}>Cek status</Button>
            )}
            <Button variant="secondary" onClick={() => window.print()}><Printer size={16} aria-hidden /> Cetak / simpan PDF</Button>
          </div>
        </>
      )}
    </Modal>
  );
}
