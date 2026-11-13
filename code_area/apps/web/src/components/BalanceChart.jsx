import { useEffect, useState } from "react";
import { api, errorMessage } from "../api/client";
import { rupiah } from "../lib/format";
import { Alert, Card, Empty, Spinner } from "./ui";

const PERIODS = [["DAILY", "Harian"], ["WEEKLY", "Mingguan"], ["MONTHLY", "Bulanan"]];
// Label sumbu ringkas; jumlah desimal mengikuti rentang data agar label tidak kembar (mis. "2,0 jt" dua kali).
const short = (n, span) => {
  const a = Math.abs(n);
  const unit = a >= 1e9 ? [1e9, " M"] : a >= 1e6 ? [1e6, " jt"] : a >= 1e3 ? [1e3, " rb"] : [1, ""];
  const step = span / unit[0];
  const dec = step >= 1 ? 1 : step >= 0.1 ? 2 : 3;
  return (n / unit[0]).toFixed(unit[0] === 1 ? 0 : dec).replace(".", ",") + unit[1];
};

// FR-07: grafik perubahan saldo rekening utama. Titik dihitung backend dari jurnal ledger.
export default function BalanceChart({ hidden }) {
  const [period, setPeriod] = useState("DAILY");
  const [data, setData] = useState(null);
  const [err, setErr] = useState(null);
  const [asTable, setAsTable] = useState(false);

  useEffect(() => {
    let alive = true;
    setData(null); setErr(null);
    api.get("/accounts/me/balance-chart", { params: { period } })
      .then((r) => alive && setData(r.data.points || []))
      .catch((ex) => alive && setErr(errorMessage(ex)));
    return () => { alive = false; };
  }, [period]);

  const change = data?.length > 1 ? data.at(-1).balance - data[0].balance : 0;

  return (
    <Card title="Grafik saldo" actions={
      <div className="segmented compact" role="radiogroup" aria-label="Periode grafik saldo">
        {PERIODS.map(([v, l]) => (
          <button key={v} type="button" role="radio" aria-checked={period === v} className={period === v ? "on" : ""} onClick={() => setPeriod(v)}>{l}</button>
        ))}
      </div>
    }>
      <Alert>{err}</Alert>
      {!data && !err && <Spinner label="Memuat grafik…" />}
      {data && data.length === 0 && <Empty>Belum ada data saldo.</Empty>}
      {data && data.length > 0 && (hidden ? <Empty>Saldo disembunyikan. Tampilkan saldo untuk melihat grafik.</Empty> : (
        <>
          {asTable ? <PointTable points={data} /> : <LineChart points={data} />}
          <div className="chart-foot">
            <span className="small">Perubahan periode ini: <strong className={change >= 0 ? "pos" : "neg"}>{change >= 0 ? "+" : "−"}{rupiah(Math.abs(change))}</strong></span>
            <button type="button" className="link small" onClick={() => setAsTable(!asTable)}>{asTable ? "Lihat sebagai grafik" : "Lihat sebagai tabel"}</button>
          </div>
        </>
      ))}
    </Card>
  );
}

function LineChart({ points }) {
  const W = 900, H = 230, padL = 64, padR = 14, padT = 12, padB = 26;
  const vals = points.map((p) => p.balance);
  let min = Math.min(...vals), max = Math.max(...vals);
  if (min === max) { min -= 1; max += 1; }
  const span = max - min, lo = min - span * 0.1, hi = max + span * 0.1;
  const x = (i) => padL + (points.length === 1 ? (W - padL - padR) / 2 : (i * (W - padL - padR)) / (points.length - 1));
  const y = (v) => padT + (1 - (v - lo) / (hi - lo)) * (H - padT - padB);
  const line = points.map((p, i) => `${i ? "L" : "M"}${x(i).toFixed(1)},${y(p.balance).toFixed(1)}`).join(" ");
  const area = `${line} L${x(points.length - 1).toFixed(1)},${H - padB} L${x(0).toFixed(1)},${H - padB} Z`;
  const ticks = [lo + (hi - lo) * 0.15, (lo + hi) / 2, hi - (hi - lo) * 0.15];
  const every = Math.max(1, Math.ceil(points.length / 7));
  return (
    <figure className="chart">
      <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-label={`Grafik saldo dari ${rupiah(points[0].balance)} ke ${rupiah(points.at(-1).balance)}`}>
        {ticks.map((t) => (
          <g key={t}>
            <line x1={padL} x2={W - padR} y1={y(t)} y2={y(t)} className="gridline" />
            <text x={padL - 6} y={y(t) + 4} textAnchor="end" className="tick">{short(Math.round(t), (hi - lo) * 0.35)}</text>
          </g>
        ))}
        <path d={area} className="area" />
        <path d={line} className="line" />
        {points.map((p, i) => (
          <g key={i}>
            <circle cx={x(i)} cy={y(p.balance)} r={points.length > 20 ? 2.2 : 3.2} className="dot">
              <title>{`${p.label}: ${rupiah(p.balance)}`}</title>
            </circle>
            {i % every === 0 || i === points.length - 1
              ? <text x={x(i)} y={H - 8} textAnchor="middle" className="tick">{p.label}</text> : null}
          </g>
        ))}
      </svg>
    </figure>
  );
}

function PointTable({ points }) {
  return (
    <div className="table-scroll">
      <table className="data-table">
        <thead><tr><th>Periode</th><th className="num">Saldo</th></tr></thead>
        <tbody>{points.map((p, i) => <tr key={i}><td>{p.label}</td><td className="num">{rupiah(p.balance)}</td></tr>)}</tbody>
      </table>
    </div>
  );
}
