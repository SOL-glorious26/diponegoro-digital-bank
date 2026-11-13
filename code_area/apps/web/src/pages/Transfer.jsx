import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, Printer, Star, Trash2 } from "lucide-react";
import { api, errorMessage, newIdempotencyKey, withKey } from "../api/client";
import { formatAmountInput, onlyDigits, parseAmount, rupiah } from "../lib/format";
import { Alert, Button, Card, Field, Rows } from "../components/ui";
import PinDialog from "../components/PinDialog";
import { Receipt } from "../components/TxDetail";

const MIN = 5000;

// Transfer sesama bank dan antarbank (FR-10, FR-11, FR-12, FR-13, FR-14; UC-01 A1–A6).
export default function Transfer() {
  const [banks, setBanks] = useState([]);
  const [favorites, setFavorites] = useState([]);
  const [form, setForm] = useState({ bankCode: "", accountNumber: "", amount: "", note: "" });
  const [quote, setQuote] = useState(null);
  const [idemKey, setIdemKey] = useState(null);
  const [pinOpen, setPinOpen] = useState(false);
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);
  const [info, setInfo] = useState(null);

  const loadFavorites = () => api.get("/transfers/favorites").then((r) => setFavorites(r.data)).catch(() => {});
  useEffect(() => {
    api.get("/transfers/banks").then((r) => {
      setBanks(r.data);
      const internal = r.data.find((b) => b.internal);
      setForm((f) => ({ ...f, bankCode: f.bankCode || internal?.code || r.data[0]?.code || "" }));
    }).catch((ex) => setErr(errorMessage(ex)));
    loadFavorites();
  }, []);

  const bank = banks.find((b) => b.code === form.bankCode);
  const set = (k) => (e) => setForm({ ...form, [k]: k === "amount" ? formatAmountInput(e.target.value) : k === "accountNumber" ? onlyDigits(e.target.value) : e.target.value });

  // Langkah 1 → 2: validasi rekening tujuan (FR-12) lalu ringkasan biaya dan saldo (quote).
  async function toConfirm(e) {
    e.preventDefault();
    const amount = parseAmount(form.amount);
    if (amount < MIN) return setErr(`Nominal minimal ${rupiah(MIN)}.`);
    setBusy(true); setErr(null);
    try {
      await api.post("/transfers/inquiry", { bankCode: form.bankCode, accountNumber: form.accountNumber });
      const { data } = await api.post("/transfers/quote", { bankCode: form.bankCode, accountNumber: form.accountNumber, amount, note: form.note || undefined });
      setQuote(data);
      setIdemKey(newIdempotencyKey()); // satu kunci per layar konfirmasi (BR-04)
    } catch (ex) { setErr(errorMessage(ex)); } finally { setBusy(false); }
  }

  async function execute(pin) {
    const { data } = await api.post("/transfers", {
      bankCode: form.bankCode, accountNumber: form.accountNumber, amount: quote.amount, note: quote.note || undefined, pin,
    }, withKey(idemKey));
    setPinOpen(false);
    setResult(data);
  }

  async function checkStatus() {
    setBusy(true);
    try { setResult((await api.post(`/transfers/${result.id}/check-status`)).data); }
    catch (ex) { setErr(errorMessage(ex)); } finally { setBusy(false); }
  }

  async function addFavorite() {
    try {
      await api.post("/transfers/favorites", { bankCode: quote.recipient.bankCode, accountNumber: quote.recipient.accountNumber });
      setQuote({ ...quote, recipient: { ...quote.recipient, favorite: true } });
      setInfo("Penerima disimpan ke favorit.");
      loadFavorites();
    } catch (ex) { setErr(errorMessage(ex)); }
  }
  async function removeFavorite(id) {
    try { await api.delete(`/transfers/favorites/${id}`); loadFavorites(); } catch (ex) { setErr(errorMessage(ex)); }
  }

  function reset() {
    setForm((f) => ({ ...f, accountNumber: "", amount: "", note: "" }));
    setQuote(null); setResult(null); setIdemKey(null); setErr(null); setInfo(null);
  }

  if (result) {
    return (
      <div className="page narrow">
        <Card title="Bukti transfer">
          <Receipt tx={result} />
          {result.status === "PROCESSING" && (
            <Alert kind="info">Transfer sedang diproses oleh bank tujuan. Saldo tidak akan terdebit dua kali; cek status beberapa saat lagi.</Alert>
          )}
          {result.status === "FAILED" && <Alert>Transfer gagal. Dana yang sempat terdebit dikembalikan otomatis.</Alert>}
          <Alert>{err}</Alert>
          <div className="row-actions no-print">
            {result.status === "PROCESSING" && <Button variant="secondary" loading={busy} onClick={checkStatus}>Cek status</Button>}
            <Button variant="secondary" onClick={() => window.print()}><Printer size={16} aria-hidden /> Cetak / PDF</Button>
            <Button onClick={reset}>Transfer lagi</Button>
          </div>
        </Card>
      </div>
    );
  }

  if (quote) {
    const r = quote.recipient;
    return (
      <div className="page narrow">
        <Card title="Konfirmasi transfer" actions={<button className="link" onClick={() => setQuote(null)}><ArrowLeft size={14} aria-hidden /> Ubah</button>}>
          <Rows items={[
            ["Penerima", <strong key="n">{r.name}</strong>],
            ["Bank", r.bankName],
            ["Rekening", r.accountNumber],
            ["Nominal", rupiah(quote.amount)],
            ["Biaya", rupiah(quote.fee)],
            ["Total didebit", <strong key="t">{rupiah(quote.total)}</strong>],
            quote.note && ["Catatan", quote.note],
            ["Saldo setelah transfer", rupiah(quote.balanceAfter)],
          ]} />
          {!r.favorite && <button className="link" onClick={addFavorite}><Star size={14} aria-hidden /> Simpan sebagai favorit</button>}
          <Alert kind="success">{info}</Alert>
          <Alert>{err}</Alert>
          <Button className="block" onClick={() => setPinOpen(true)}>Lanjut ke PIN</Button>
        </Card>
        {pinOpen && (
          <PinDialog title="PIN transaksi" summary={<p className="center">Transfer {rupiah(quote.total)} ke <strong>{r.name}</strong></p>}
            onSubmit={execute} onClose={() => setPinOpen(false)} />
        )}
      </div>
    );
  }

  return (
    <div className="page">
      <div className="grid-2 wide-left">
        <Card title="Transfer">
          <form onSubmit={toConfirm}>
            <Field label="Bank tujuan" id="bank" hint={bank ? (bank.internal ? "Sesama Diponegoro Digital Bank – tanpa biaya" : `Biaya antarbank ${rupiah(bank.fee)}`) : null}>
              <select id="bank" value={form.bankCode} onChange={set("bankCode")} required>
                {banks.map((b) => <option key={b.code} value={b.code}>{b.name}</option>)}
              </select>
            </Field>
            <Field label="Nomor rekening tujuan" id="acc">
              <input id="acc" inputMode="numeric" value={form.accountNumber} onChange={set("accountNumber")} required />
            </Field>
            <Field label="Nominal" id="amt" hint={`Minimal ${rupiah(MIN)}`}>
              <div className="input-prefix"><span>Rp</span>
                <input id="amt" inputMode="numeric" value={form.amount} onChange={set("amount")} required />
              </div>
            </Field>
            <Field label="Catatan (opsional)" id="note" hint="Maksimal 50 karakter">
              <input id="note" maxLength={50} value={form.note} onChange={set("note")} />
            </Field>
            <Alert>{err}</Alert>
            <Button type="submit" loading={busy} className="block">Lanjut</Button>
          </form>
        </Card>
        <Card title="Penerima favorit">
          {favorites.length === 0 ? <p className="muted small">Belum ada favorit. Simpan penerima di layar konfirmasi.</p> : (
            <ul className="fav-list">
              {favorites.map((f) => (
                <li key={f.id}>
                  <button className="fav" onClick={() => setForm({ ...form, bankCode: f.bankCode, accountNumber: f.accountNumber })}>
                    <strong>{f.name}</strong>
                    <span>{f.bankName} · {f.accountNumber}</span>
                  </button>
                  <button className="icon-btn" onClick={() => removeFavorite(f.id)} aria-label={`Hapus ${f.name}`}><Trash2 size={16} /></button>
                </li>
              ))}
            </ul>
          )}
          <p className="small muted">Data uji: 1400000002 (ANDI), BRI 9999999999 (ditolak), Mandiri 8888888888 (timeout). <Link to="/riwayat">Lihat riwayat</Link></p>
        </Card>
      </div>
    </div>
  );
}
