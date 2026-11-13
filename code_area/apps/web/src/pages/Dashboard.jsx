import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Copy, Eye, EyeOff, QrCode, Send, Smartphone, History } from "lucide-react";
import { api, errorMessage } from "../api/client";
import { rupiah } from "../lib/format";
import { Alert, Card, Empty, Spinner } from "../components/ui";
import { TxDetailModal, TxRow } from "../components/TxDetail";
import BalanceChart from "../components/BalanceChart";

export default function Dashboard() {
  const [acc, setAcc] = useState(null);
  const [months, setMonths] = useState([]);
  const [recent, setRecent] = useState(null);
  const [err, setErr] = useState(null);
  const [detail, setDetail] = useState(null);
  const [copied, setCopied] = useState(false);

  const load = useCallback(() => {
    api.get("/accounts/me").then((r) => setAcc(r.data)).catch((ex) => setErr(errorMessage(ex)));
    api.get("/history/monthly-summary", { params: { months: 6 } }).then((r) => setMonths(r.data)).catch(() => {});
    api.get("/history", { params: { size: 5 } }).then((r) => setRecent(r.data.items)).catch(() => setRecent([]));
  }, []);
  useEffect(load, [load]);

  // FR-06: pilihan sembunyikan saldo disimpan di server sehingga sama di Android.
  async function toggleHidden() {
    try {
      const { data } = await api.put("/accounts/me/balance-visibility", { hidden: !acc.balanceHidden });
      setAcc(data);
    } catch (ex) { setErr(errorMessage(ex)); }
  }

  function copyAccount() {
    navigator.clipboard?.writeText(acc.accountNumber).then(() => { setCopied(true); setTimeout(() => setCopied(false), 1500); });
  }

  return (
    <div className="page">
      <Alert>{err}</Alert>
      <section className="balance-card">
        {!acc ? <Spinner label="Memuat saldo…" /> : (
          <>
            <div className="balance-top">
              <div>
                <div className="balance-label">Saldo rekening utama</div>
                <div className="balance-value">{acc.balanceHidden ? "Rp ••••••" : rupiah(acc.balance)}</div>
              </div>
              <button className="icon-btn light" onClick={toggleHidden} aria-label={acc.balanceHidden ? "Tampilkan saldo" : "Sembunyikan saldo"}>
                {acc.balanceHidden ? <Eye /> : <EyeOff />}
              </button>
            </div>
            <div className="balance-meta">
              <span>{acc.ownerName}</span>
              <button className="acct" onClick={copyAccount} title="Salin nomor rekening">
                {acc.accountNumber} <Copy size={14} aria-hidden /> {copied && <em>disalin</em>}
              </button>
              {acc.pocketsTotal > 0 && <span>Kantong: {acc.balanceHidden ? "••••" : rupiah(acc.pocketsTotal)}</span>}
            </div>
          </>
        )}
      </section>

      <div className="quick">
        <Link to="/transfer"><Send aria-hidden /> Transfer</Link>
        <Link to="/topup"><Smartphone aria-hidden /> Top-up</Link>
        <Link to="/qris"><QrCode aria-hidden /> QRIS Saya</Link>
        <Link to="/riwayat"><History aria-hidden /> Riwayat</Link>
      </div>

      {acc && <BalanceChart hidden={acc.balanceHidden} />}

      <div className="grid-2">
        <Card title="Pemasukan & pengeluaran (6 bulan)">
          <MonthlyChart data={months} hidden={acc?.balanceHidden} />
        </Card>
        <Card title="Transaksi terakhir" actions={<Link to="/riwayat" className="small">Lihat semua</Link>}>
          {recent == null ? <Spinner /> : recent.length === 0 ? <Empty>Belum ada transaksi.</Empty> : (
            <div className="tx-list">{recent.map((t) => <TxRow key={t.id} tx={t} onClick={() => setDetail(t.id)} />)}</div>
          )}
        </Card>
      </div>
      {detail && <TxDetailModal txId={detail} onClose={() => setDetail(null)} onChanged={load} />}
    </div>
  );
}

// Grafik batang sederhana pemasukan vs pengeluaran per bulan (FR-25).
function MonthlyChart({ data, hidden }) {
  if (!data?.length) return <Empty>Belum ada data.</Empty>;
  const max = Math.max(1, ...data.flatMap((m) => [m.income, m.expense]));
  const W = 520, H = 200, pad = 24, bw = (W - pad * 2) / data.length;
  return (
    <figure className="chart">
      <svg viewBox={`0 0 ${W} ${H + 28}`} role="img" aria-label="Grafik pemasukan dan pengeluaran per bulan">
        <line x1={pad} x2={W - pad} y1={H} y2={H} className="axis" />
        {data.map((m, i) => {
          const x = pad + i * bw, hi = (m.income / max) * (H - 16), he = (m.expense / max) * (H - 16);
          return (
            <g key={m.label}>
              <rect x={x + bw * 0.18} y={H - hi} width={bw * 0.28} height={hi} rx="3" className="bar-in">
                <title>{`${m.label} – pemasukan ${rupiah(m.income)}`}</title>
              </rect>
              <rect x={x + bw * 0.52} y={H - he} width={bw * 0.28} height={he} rx="3" className="bar-out">
                <title>{`${m.label} – pengeluaran ${rupiah(m.expense)}`}</title>
              </rect>
              <text x={x + bw / 2} y={H + 18} textAnchor="middle" className="tick">{m.label}</text>
            </g>
          );
        })}
      </svg>
      <figcaption className="legend">
        <span><i className="sw in" /> Pemasukan</span>
        <span><i className="sw out" /> Pengeluaran</span>
        {!hidden && data.at(-1) && <span className="muted">Bulan ini: +{rupiah(data.at(-1).income)} / −{rupiah(data.at(-1).expense)}</span>}
      </figcaption>
    </figure>
  );
}
