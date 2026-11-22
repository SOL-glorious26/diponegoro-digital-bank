import { useCallback, useEffect, useState } from "react";
import { Download, Search } from "lucide-react";
import { api, errorMessage } from "../api/client";
import { Alert, Button, Card, Empty, Spinner } from "../components/ui";
import { TxDetailModal, TxRow } from "../components/TxDetail";

const RANGES = [["ALL", "Semua waktu"], ["LAST_7_DAYS", "7 hari terakhir"], ["LAST_30_DAYS", "30 hari terakhir"], ["THIS_MONTH", "Bulan ini"], ["LAST_MONTH", "Bulan lalu"], ["CUSTOM", "Pilih tanggal"]];
const CATEGORIES = [["", "Semua jenis"], ["TRANSFER", "Transfer"], ["QRIS", "QRIS"], ["TOPUP", "Top-up & tagihan"], ["DEPOSIT", "Setoran"], ["POCKET", "Kantong"], ["CARD", "Kartu"]];
const DIRECTIONS = [["", "Masuk & keluar"], ["IN", "Masuk"], ["OUT", "Keluar"], ["INTERNAL", "Internal"]];
const STATUSES = [["", "Semua status"], ["SUCCESS", "Berhasil"], ["PROCESSING", "Diproses"], ["FAILED", "Gagal"]];
const EMPTY = { range: "ALL", from: "", to: "", category: "", direction: "", status: "", q: "" };

function cleanParams(f) {
  const p = {};
  Object.entries(f).forEach(([k, v]) => { if (v) p[k] = v; });
  if (f.range !== "CUSTOM") { delete p.from; delete p.to; }
  return p;
}

export default function History() {
  const [filter, setFilter] = useState(EMPTY);
  const [applied, setApplied] = useState(EMPTY);
  const [items, setItems] = useState(null);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(null);
  const [err, setErr] = useState(null);
  const [detail, setDetail] = useState(null);

  const load = useCallback(async (f, p) => {
    setLoading(true); setErr(null);
    try {
      const { data } = await api.get("/history", { params: { ...cleanParams(f), page: p, size: 20 } });
      setItems((prev) => (p === 0 ? data.items : [...(prev || []), ...data.items]));
      setHasMore(data.hasMore); setTotal(data.totalItems); setPage(p);
    } catch (ex) { setErr(errorMessage(ex)); } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(applied, 0); }, [applied, load]);

  const set = (k) => (e) => {
    const next = { ...filter, [k]: e.target.value };
    setFilter(next);
    if (k !== "q" && k !== "from" && k !== "to") setApplied(next); // filter pilihan langsung diterapkan
  };
  const submit = (e) => { e.preventDefault(); setApplied({ ...filter }); };

  // FR-26: ekspor sesuai filter yang sedang aktif.
  async function exportAs(format) {
    setExporting(format); setErr(null);
    try {
      const res = await api.get("/history/export", { params: { ...cleanParams(applied), format }, responseType: "blob" });
      const cd = res.headers["content-disposition"] || "";
      const name = /filename="?([^";]+)"?/.exec(cd)?.[1] || `riwayat-ddb.${format}`;
      const url = URL.createObjectURL(res.data);
      const a = Object.assign(document.createElement("a"), { href: url, download: name });
      document.body.appendChild(a); a.click(); a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (ex) {
      let msg = errorMessage(ex);
      if (ex.response?.data instanceof Blob) { try { msg = JSON.parse(await ex.response.data.text()).message || msg; } catch { /* */ } }
      setErr(msg);
    } finally { setExporting(null); }
  }

  const select = (k, opts, label) => (
    <select value={filter[k]} onChange={set(k)} aria-label={label}>{opts.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select>
  );

  return (
    <div className="page">
      <Card title="Riwayat transaksi" actions={
        <>
          <Button variant="secondary" loading={exporting === "csv"} onClick={() => exportAs("csv")}><Download size={16} aria-hidden /> CSV</Button>
          <Button variant="secondary" loading={exporting === "pdf"} onClick={() => exportAs("pdf")}><Download size={16} aria-hidden /> PDF</Button>
        </>
      }>
        <form className="filters" onSubmit={submit}>
          <div className="search">
            <Search size={16} aria-hidden />
            <input placeholder="Cari nama, catatan, atau ID…" value={filter.q} onChange={set("q")} aria-label="Pencarian" />
          </div>
          {select("range", RANGES, "Rentang waktu")}
          {filter.range === "CUSTOM" && (
            <>
              <input type="date" value={filter.from} onChange={set("from")} aria-label="Dari tanggal" />
              <input type="date" value={filter.to} onChange={set("to")} aria-label="Sampai tanggal" />
            </>
          )}
          {select("category", CATEGORIES, "Jenis")}
          {select("direction", DIRECTIONS, "Arah")}
          {select("status", STATUSES, "Status")}
          <Button type="submit">Terapkan</Button>
          <button type="button" className="link" onClick={() => { setFilter(EMPTY); setApplied(EMPTY); }}>Reset</button>
        </form>
        <Alert>{err}</Alert>
        {items == null ? <Spinner label="Memuat…" /> : items.length === 0 ? <Empty>Tidak ada transaksi yang sesuai filter.</Empty> : (
          <>
            <p className="small muted">{total} transaksi</p>
            <div className="tx-list">{items.map((t) => <TxRow key={t.id} tx={t} onClick={() => setDetail(t.id)} />)}</div>
            {hasMore && <div className="center"><Button variant="secondary" loading={loading} onClick={() => load(applied, page + 1)}>Muat lebih banyak</Button></div>}
          </>
        )}
      </Card>
      {detail && <TxDetailModal txId={detail} onClose={() => setDetail(null)} onChanged={() => load(applied, 0)} />}
    </div>
  );
}
