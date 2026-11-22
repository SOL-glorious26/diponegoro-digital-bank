import { useCallback, useEffect, useState } from "react";
import { Eye, Plus, Snowflake, Zap } from "lucide-react";
import { api, errorMessage } from "../api/client";
import { formatAmountInput, parseAmount, rupiah } from "../lib/format";
import { Alert, Button, Card, Empty, Field, Modal, Rows, Spinner } from "../components/ui";
import PinDialog from "../components/PinDialog";

// Virtual card (FR-21, FR-22): nomor lengkap dan CVV hanya tampil setelah PIN.
export default function Cards() {
  const [cards, setCards] = useState(null);
  const [err, setErr] = useState(null);
  const [creating, setCreating] = useState(false);
  const [scope, setScope] = useState("DOMESTIC");
  const [pinFor, setPinFor] = useState(null);
  const [details, setDetails] = useState(null);
  const [limitFor, setLimitFor] = useState(null);

  const load = useCallback(() => api.get("/cards").then((r) => setCards(r.data)).catch((ex) => setErr(errorMessage(ex))), []);
  useEffect(() => { load(); }, [load]);

  const act = async (fn) => { setErr(null); try { await fn(); load(); } catch (ex) { setErr(errorMessage(ex)); } };

  return (
    <div className="page">
      <Card title="Virtual card" actions={<Button onClick={() => setCreating(true)}><Plus size={16} aria-hidden /> Buat kartu</Button>}>
        <Alert>{err}</Alert>
        {cards == null ? <Spinner /> : cards.length === 0 ? <Empty>Belum ada virtual card.</Empty> : (
          <div className="card-grid">
            {cards.map((c) => (
              <article key={c.id} className={`vcard ${c.status === "FROZEN" ? "frozen" : ""}`}>
                <div className="vcard-face">
                  <span className="vcard-brand">Diponegoro · {c.usageScope === "INTERNATIONAL" ? "Internasional" : "Domestik"}</span>
                  <span className="vcard-number">{c.maskedNumber}</span>
                  <span className="vcard-meta"><span>{c.cardholderName}</span><span>{c.expiry}</span></span>
                  {c.status === "FROZEN" && <span className="vcard-frozen"><Snowflake size={14} aria-hidden /> Dibekukan</span>}
                </div>
                <p className="small muted">Limit per transaksi: {rupiah(c.transactionLimit)}</p>
                <div className="row-actions">
                  <Button variant="secondary" onClick={() => setPinFor(c)}><Eye size={16} aria-hidden /> Detail</Button>
                  {c.status === "ACTIVE"
                    ? <Button variant="secondary" onClick={() => act(() => api.post(`/cards/${c.id}/freeze`))}><Snowflake size={16} aria-hidden /> Bekukan</Button>
                    : <Button variant="secondary" onClick={() => act(() => api.post(`/cards/${c.id}/activate`))}><Zap size={16} aria-hidden /> Aktifkan</Button>}
                  <Button variant="secondary" onClick={() => setLimitFor(c)}>Limit</Button>
                </div>
              </article>
            ))}
          </div>
        )}
      </Card>

      {creating && (
        <Modal title="Buat virtual card" onClose={() => setCreating(false)}>
          <Field label="Cakupan pemakaian" id="sc">
            <select id="sc" value={scope} onChange={(e) => setScope(e.target.value)}>
              <option value="DOMESTIC">Domestik</option>
              <option value="INTERNATIONAL">Internasional</option>
            </select>
          </Field>
          <Button className="block" onClick={() => act(async () => { await api.post("/cards", { usageScope: scope }); setCreating(false); })}>Buat</Button>
        </Modal>
      )}
      {pinFor && (
        <PinDialog title="PIN untuk melihat detail kartu" onClose={() => setPinFor(null)}
          onSubmit={async (pin) => { const { data } = await api.post(`/cards/${pinFor.id}/details`, { pin }); setPinFor(null); setDetails(data); }} />
      )}
      {details && (
        <Modal title="Detail kartu" onClose={() => setDetails(null)}>
          <Rows items={[["Nama", details.cardholderName], ["Nomor kartu", <code key="n">{details.cardNumber}</code>], ["Berlaku sampai", details.expiry], ["CVV", <code key="c">{details.cvv}</code>]]} />
          <p className="small muted">Jangan bagikan nomor kartu dan CVV kepada siapa pun.</p>
        </Modal>
      )}
      {limitFor && <LimitDialog card={limitFor} onClose={() => setLimitFor(null)} onDone={() => { setLimitFor(null); load(); }} />}
    </div>
  );
}

function LimitDialog({ card, onClose, onDone }) {
  const [v, setV] = useState(formatAmountInput(String(card.transactionLimit ?? "")));
  const [err, setErr] = useState(null);
  const [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault(); setBusy(true); setErr(null);
    try { await api.put(`/cards/${card.id}/limit`, { limit: parseAmount(v) }); onDone(); }
    catch (ex) { setErr(errorMessage(ex)); } finally { setBusy(false); }
  }
  return (
    <Modal title="Limit per transaksi" onClose={onClose}>
      <form onSubmit={submit}>
        <Field label="Limit" id="lm" hint="Berlaku untuk transaksi berikutnya">
          <div className="input-prefix"><span>Rp</span><input id="lm" inputMode="numeric" value={v} onChange={(e) => setV(formatAmountInput(e.target.value))} required /></div>
        </Field>
        <Alert>{err}</Alert>
        <Button type="submit" loading={busy} className="block">Simpan</Button>
      </form>
    </Modal>
  );
}
