import { useCallback, useEffect, useState } from "react";
import { Lock, Pencil, Plus, Trash2, Unlock } from "lucide-react";
import { api, errorCode, errorMessage, newIdempotencyKey, withKey } from "../api/client";
import { dateOnly, formatAmountInput, parseAmount, rupiah } from "../lib/format";
import { Alert, Button, Card, Empty, Field, Modal, Spinner } from "../components/ui";
import PinDialog from "../components/PinDialog";

// Kantong simpanan dan penguncian (FR-08, FR-09, BR-06).
export default function Pockets() {
  const [pockets, setPockets] = useState(null);
  const [err, setErr] = useState(null);
  const [creating, setCreating] = useState(false);
  const [move, setMove] = useState(null); // {pocket, mode: deposit|withdraw}
  const [editing, setEditing] = useState(null);
  const [locking, setLocking] = useState(null);

  const load = useCallback(() => api.get("/accounts/pockets").then((r) => setPockets(r.data)).catch((ex) => setErr(errorMessage(ex))), []);
  useEffect(() => { load(); }, [load]);

  async function remove(p) {
    if (!window.confirm(`Hapus kantong "${p.name}"? Sisa saldo dipindahkan ke rekening utama.`)) return;
    try { await api.delete(`/accounts/pockets/${p.id}`, withKey(newIdempotencyKey())); load(); }
    catch (ex) { setErr(errorMessage(ex)); }
  }

  return (
    <div className="page">
      <Card title="Kantong" actions={<Button onClick={() => setCreating(true)}><Plus size={16} aria-hidden /> Kantong baru</Button>}>
        <Alert>{err}</Alert>
        {pockets == null ? <Spinner /> : pockets.length === 0 ? <Empty>Belum ada kantong. Buat kantong untuk menyisihkan dana.</Empty> : (
          <div className="pocket-grid">
            {pockets.map((p) => (
              <article key={p.id} className="pocket">
                <header>
                  <strong>{p.name}</strong>
                  {p.locked ? <span className="badge badge-warn"><Lock size={12} aria-hidden /> Terkunci</span> : <span className="badge badge-ok"><Unlock size={12} aria-hidden /> Bebas</span>}
                </header>
                {p.purpose && <p className="small muted">{p.purpose}</p>}
                <div className="pocket-balance">{rupiah(p.balance)}</div>
                {p.target ? (
                  <>
                    <div className="progress" aria-label={`Progres ${Math.round((p.progress ?? 0) * 100)}%`}><span style={{ width: `${Math.min(100, (p.progress ?? 0) * 100)}%` }} /></div>
                    <p className="small muted">Target {rupiah(p.target)}</p>
                  </>
                ) : null}
                {p.locked && <p className="small">{p.lockDescription || (p.lockUntil ? `Terkunci sampai ${dateOnly(p.lockUntil)}` : "")}</p>}
                <div className="row-actions">
                  <Button variant="secondary" onClick={() => setMove({ pocket: p, mode: "deposit" })}>Isi</Button>
                  <Button variant="secondary" onClick={() => setMove({ pocket: p, mode: "withdraw" })}>Tarik</Button>
                  <Button variant="secondary" onClick={() => setEditing(p)}><Pencil size={14} aria-hidden /> Ubah</Button>
                  {!p.locked && <Button variant="secondary" onClick={() => setLocking(p)}><Lock size={14} aria-hidden /> Kunci</Button>}
                  <button className="icon-btn" onClick={() => remove(p)} aria-label={`Hapus ${p.name}`}><Trash2 size={16} /></button>
                </div>
              </article>
            ))}
          </div>
        )}
      </Card>
      {creating && <CreatePocket onClose={() => setCreating(false)} onDone={() => { setCreating(false); load(); }} />}
      {move && <MoveDialog {...move} onClose={() => setMove(null)} onDone={() => { setMove(null); load(); }} />}
      {editing && <EditPocket pocket={editing} onClose={() => setEditing(null)} onDone={() => { setEditing(null); load(); }} />}
      {locking && <LockPocket pocket={locking} onClose={() => setLocking(null)} onDone={() => { setLocking(null); load(); }} />}
    </div>
  );
}

// FR-08: ubah nama, tujuan, dan target kantong.
function EditPocket({ pocket, onClose, onDone }) {
  const [f, setF] = useState({ name: pocket.name, purpose: pocket.purpose || "", target: pocket.target ? formatAmountInput(String(pocket.target)) : "" });
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);
  async function submit(e) {
    e.preventDefault(); setBusy(true); setErr(null);
    try {
      await api.put(`/accounts/pockets/${pocket.id}`, { name: f.name.trim(), purpose: f.purpose.trim() || null, target: parseAmount(f.target) || null });
      onDone();
    } catch (ex) { setErr(errorMessage(ex)); } finally { setBusy(false); }
  }
  return (
    <Modal title={`Ubah ${pocket.name}`} onClose={onClose}>
      <form onSubmit={submit}>
        <Field label="Nama" id="en" hint="Maksimal 30 karakter"><input id="en" maxLength={30} value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} required /></Field>
        <Field label="Tujuan (opsional)" id="ep"><input id="ep" maxLength={60} value={f.purpose} onChange={(e) => setF({ ...f, purpose: e.target.value })} /></Field>
        <Field label="Target (opsional)" id="et" hint={pocket.locked && pocket.lockType === "TARGET" ? "Kantong terkunci sampai target; target tidak boleh dikosongkan" : "Minimal Rp10.000"}>
          <div className="input-prefix"><span>Rp</span><input id="et" inputMode="numeric" value={f.target} onChange={(e) => setF({ ...f, target: formatAmountInput(e.target.value) })} /></div>
        </Field>
        <Alert>{err}</Alert>
        <Button type="submit" loading={busy} className="block">Simpan perubahan</Button>
      </form>
    </Modal>
  );
}

// FR-09: kunci kantong yang belum terkunci, sampai tanggal tertentu atau sampai target tercapai.
function LockPocket({ pocket, onClose, onDone }) {
  const [type, setType] = useState("DATE");
  const [until, setUntil] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);
  const tomorrow = new Date(Date.now() + 86400000).toISOString().slice(0, 10);
  async function submit(e) {
    e.preventDefault(); setBusy(true); setErr(null);
    try { await api.post(`/accounts/pockets/${pocket.id}/lock`, { lockType: type, lockUntil: type === "DATE" ? until : null }); onDone(); }
    catch (ex) { setErr(errorMessage(ex)); } finally { setBusy(false); }
  }
  return (
    <Modal title={`Kunci ${pocket.name}`} onClose={onClose}>
      <form onSubmit={submit}>
        <p className="small muted">Dana yang terkunci tidak dapat dipakai bertransaksi. Penarikan sebelum waktunya memerlukan konfirmasi dan PIN.</p>
        <Field label="Jenis kunci" id="lt">
          <select id="lt" value={type} onChange={(e) => setType(e.target.value)}>
            <option value="DATE">Sampai tanggal tertentu</option>
            <option value="TARGET" disabled={!pocket.target}>Sampai target tercapai{pocket.target ? ` (${rupiah(pocket.target)})` : " – atur target dulu"}</option>
          </select>
        </Field>
        {type === "DATE" && <Field label="Terkunci sampai" id="lu"><input id="lu" type="date" min={tomorrow} value={until} onChange={(e) => setUntil(e.target.value)} required /></Field>}
        <Alert>{err}</Alert>
        <Button type="submit" loading={busy} className="block"><Lock size={14} aria-hidden /> Kunci kantong</Button>
      </form>
    </Modal>
  );
}

function CreatePocket({ onClose, onDone }) {
  const [f, setF] = useState({ name: "", purpose: "", target: "", lockType: "NONE", lockUntil: "" });
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);
  async function submit(e) {
    e.preventDefault();
    setBusy(true); setErr(null);
    try {
      await api.post("/accounts/pockets", {
        name: f.name, purpose: f.purpose || undefined, target: parseAmount(f.target) || undefined,
        lockType: f.lockType, lockUntil: f.lockType === "DATE" ? f.lockUntil : undefined,
      });
      onDone();
    } catch (ex) { setErr(errorMessage(ex)); } finally { setBusy(false); }
  }
  return (
    <Modal title="Kantong baru" onClose={onClose}>
      <form onSubmit={submit}>
        <Field label="Nama" id="pn" hint="Maksimal 30 karakter"><input id="pn" maxLength={30} value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} required /></Field>
        <Field label="Tujuan (opsional)" id="pp"><input id="pp" maxLength={60} value={f.purpose} onChange={(e) => setF({ ...f, purpose: e.target.value })} /></Field>
        <Field label="Target (opsional)" id="pt" hint="Minimal Rp10.000">
          <div className="input-prefix"><span>Rp</span><input id="pt" inputMode="numeric" value={f.target} onChange={(e) => setF({ ...f, target: formatAmountInput(e.target.value) })} /></div>
        </Field>
        <Field label="Kunci dana" id="pl">
          <select id="pl" value={f.lockType} onChange={(e) => setF({ ...f, lockType: e.target.value })}>
            <option value="NONE">Tidak dikunci</option>
            <option value="DATE">Sampai tanggal tertentu</option>
            <option value="TARGET">Sampai target tercapai</option>
          </select>
        </Field>
        {f.lockType === "DATE" && <Field label="Terkunci sampai" id="pd"><input id="pd" type="date" value={f.lockUntil} onChange={(e) => setF({ ...f, lockUntil: e.target.value })} required /></Field>}
        <Alert>{err}</Alert>
        <Button type="submit" loading={busy} className="block">Buat kantong</Button>
      </form>
    </Modal>
  );
}

function MoveDialog({ pocket, mode, onClose, onDone }) {
  const [amount, setAmount] = useState("");
  const [key] = useState(newIdempotencyKey);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);
  const [needPin, setNeedPin] = useState(false);

  async function submit(e) {
    e?.preventDefault();
    const value = parseAmount(amount);
    if (mode === "withdraw" && pocket.locked) return setNeedPin(true);
    setBusy(true); setErr(null);
    try {
      if (mode === "deposit") await api.post(`/accounts/pockets/${pocket.id}/deposit`, { amount: value }, withKey(key));
      else await api.post(`/accounts/pockets/${pocket.id}/withdraw`, { amount: value }, withKey(key));
      onDone();
    } catch (ex) {
      if (errorCode(ex)?.includes("LOCK")) setNeedPin(true);
      setErr(errorMessage(ex));
    } finally { setBusy(false); }
  }

  async function earlyWithdraw(pin) {
    await api.post(`/accounts/pockets/${pocket.id}/withdraw`, { amount: parseAmount(amount), earlyWithdrawal: true, pin }, withKey(key));
    onDone();
  }

  if (needPin) {
    return <PinDialog title="Penarikan dini"
      summary={<Alert kind="info">Kantong "{pocket.name}" masih terkunci. Penarikan dini {rupiah(parseAmount(amount))} memerlukan PIN.</Alert>}
      onSubmit={earlyWithdraw} onClose={onClose} />;
  }
  return (
    <Modal title={`${mode === "deposit" ? "Isi" : "Tarik dari"} ${pocket.name}`} onClose={onClose}>
      <form onSubmit={submit}>
        <Field label="Nominal" id="mv" hint={mode === "withdraw" ? `Saldo kantong ${rupiah(pocket.balance)}` : "Dipindahkan dari rekening utama"}>
          <div className="input-prefix"><span>Rp</span><input id="mv" inputMode="numeric" value={amount} onChange={(e) => setAmount(formatAmountInput(e.target.value))} required autoFocus /></div>
        </Field>
        <Alert>{err}</Alert>
        <Button type="submit" loading={busy} className="block">{mode === "deposit" ? "Isi kantong" : "Tarik ke rekening utama"}</Button>
      </form>
    </Modal>
  );
}
