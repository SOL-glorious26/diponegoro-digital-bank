import { useState } from "react";
import { Lock } from "lucide-react";
import { Alert, Button, Modal } from "./ui";
import { errorMessage } from "../api/client";
import { onlyDigits } from "../lib/format";

// Konfirmasi transaksi dengan PIN 6 digit (FR-04). onSubmit(pin) melakukan panggilan API;
// bila gagal karena PIN salah atau diblokir (BR-02), pesan backend ditampilkan dan PIN dikosongkan.
export default function PinDialog({ title = "Masukkan PIN", summary, onSubmit, onClose }) {
  const [pin, setPin] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);

  async function submit(e) {
    e.preventDefault();
    if (pin.length !== 6) return setErr("PIN terdiri atas 6 digit.");
    setBusy(true);
    setErr(null);
    try {
      await onSubmit(pin);
    } catch (ex) {
      setErr(errorMessage(ex));
      setPin("");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title={title} onClose={busy ? undefined : onClose} width={400}>
      <form onSubmit={submit} className="pin-form">
        {summary}
        <div className="pin-icon"><Lock size={22} aria-hidden /></div>
        <label htmlFor="pin" className="sr-only">PIN transaksi</label>
        <input
          id="pin" className="pin-input" type="password" inputMode="numeric" autoComplete="off" maxLength={6}
          value={pin} onChange={(e) => setPin(onlyDigits(e.target.value).slice(0, 6))} placeholder="••••••" autoFocus
        />
        <Alert>{err}</Alert>
        <Button type="submit" loading={busy} disabled={pin.length !== 6} className="block">Konfirmasi</Button>
      </form>
    </Modal>
  );
}
