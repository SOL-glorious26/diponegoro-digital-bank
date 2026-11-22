import { useEffect, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { Copy } from "lucide-react";
import { api, errorMessage, newIdempotencyKey, withKey } from "../api/client";
import { formatAmountInput, parseAmount, rupiah } from "../lib/format";
import { Alert, Button, Card, Field, Spinner } from "../components/ui";

// Web hanya menampilkan QRIS pribadi (FR-16) dan QRIS dinamis (FR-17). Pemindaian QRIS (FR-15) khusus Android.
export default function Qris() {
  const [qr, setQr] = useState(null);
  const [amount, setAmount] = useState("");
  const [err, setErr] = useState(null);
  const [busy, setBusy] = useState(false);
  const [sim, setSim] = useState({ amount: "", payerName: "" });
  const [simMsg, setSimMsg] = useState(null);
  const [copied, setCopied] = useState(false);

  async function load(dynAmount) {
    setBusy(true); setErr(null);
    try {
      const { data } = await api.get("/qris/my-qr", { params: dynAmount ? { amount: dynAmount } : {} });
      setQr(data);
    } catch (ex) { setErr(errorMessage(ex)); } finally { setBusy(false); }
  }
  useEffect(() => { load(); }, []);

  async function simulate(e) {
    e.preventDefault();
    setSimMsg(null); setErr(null);
    try {
      const { data } = await api.post("/qris/simulate-incoming",
        { amount: parseAmount(sim.amount), payerName: sim.payerName || undefined }, withKey(newIdempotencyKey()));
      setSimMsg(`Dana masuk ${rupiah(data.amount)} dari ${data.fromName || "bank lain"} tercatat.`);
      setSim({ amount: "", payerName: "" });
    } catch (ex) { setErr(errorMessage(ex)); }
  }

  return (
    <div className="page">
      <div className="grid-2">
        <Card title="QRIS pribadi">
          {!qr ? <Spinner /> : (
            <div className="qr-box">
              <div className="qr-frame"><QRCodeSVG value={qr.payload} size={220} level="M" marginSize={2} /></div>
              <strong>{qr.accountName}</strong>
              <span className="muted">{qr.accountNumber}</span>
              {qr.dynamic ? <span className="badge badge-warn">Nominal tetap {rupiah(qr.amount)}</span> : <span className="small muted">Pembayar mengisi nominal sendiri</span>}
              <button className="link" onClick={() => navigator.clipboard?.writeText(qr.payload).then(() => { setCopied(true); setTimeout(() => setCopied(false), 1500); })}>
                <Copy size={14} aria-hidden /> {copied ? "Disalin" : "Salin kode QRIS"}
              </button>
            </div>
          )}
          <form className="inline-form" onSubmit={(e) => { e.preventDefault(); load(parseAmount(amount) || undefined); }}>
            <Field label="QRIS dinamis (opsional)" id="dyn" hint="Isi nominal agar tidak dapat diubah pembayar">
              <div className="input-btn">
                <div className="input-prefix"><span>Rp</span><input id="dyn" inputMode="numeric" value={amount} onChange={(e) => setAmount(formatAmountInput(e.target.value))} /></div>
                <Button type="submit" variant="secondary" loading={busy}>Buat</Button>
              </div>
            </Field>
            {qr?.dynamic && <button type="button" className="link" onClick={() => { setAmount(""); load(); }}>Kembali ke QRIS statis</button>}
          </form>
          <Alert>{err}</Alert>
          <p className="small muted">Pindai kode ini dari aplikasi Android (menu Bayar QRIS) untuk menguji pembayaran lintas platform.</p>
        </Card>
        <Card title="Simulator: terima dana dari bank lain">
          <form onSubmit={simulate}>
            <Field label="Nominal" id="sa">
              <div className="input-prefix"><span>Rp</span><input id="sa" inputMode="numeric" value={sim.amount} onChange={(e) => setSim({ ...sim, amount: formatAmountInput(e.target.value) })} required /></div>
            </Field>
            <Field label="Nama pembayar (opsional)" id="sp"><input id="sp" value={sim.payerName} onChange={(e) => setSim({ ...sim, payerName: e.target.value })} /></Field>
            <Alert kind="success">{simMsg}</Alert>
            <Button type="submit" variant="secondary" className="block">Simulasikan pembayaran masuk</Button>
          </form>
          <p className="small muted">Hanya untuk lingkungan simulasi: meniru pengguna bank lain yang membayar ke QRIS Anda.</p>
        </Card>
      </div>
    </div>
  );
}
