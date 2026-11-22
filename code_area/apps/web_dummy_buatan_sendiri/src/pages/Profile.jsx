import { useCallback, useEffect, useState } from "react";
import { Laptop, LogOut, Smartphone } from "lucide-react";
import { api, errorMessage } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { dateTime, onlyDigits } from "../lib/format";
import { Alert, Button, Card, Field, Rows, Spinner } from "../components/ui";
import Avatar from "../components/Avatar";

export default function Profile() {
  return (
    <div className="page">
      <div className="grid-2">
        <ProfileCard />
        <SecurityCard />
        <NotificationSettings />
        <Sessions />
      </div>
    </div>
  );
}

// FR-27: foto profil. Backend menyimpan alamat gambar (URL http/https, maks. 500 karakter); unggah berkas belum tersedia.
function PhotoEditor() {
  const { user, setUser } = useAuth();
  const [editing, setEditing] = useState(false);
  const [url, setUrl] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);

  async function save(photoUrl) {
    setBusy(true); setErr(null);
    try { setUser((await api.put("/me/photo", { photoUrl })).data); setEditing(false); }
    catch (ex) { setErr(errorMessage(ex)); } finally { setBusy(false); }
  }
  return (
    <div className="photo-row">
      <Avatar user={user} size={64} />
      {!editing ? (
        <div className="row-actions">
          <Button variant="secondary" onClick={() => { setUrl(user.photoUrl || ""); setEditing(true); }}>{user.photoUrl ? "Ganti foto" : "Tambah foto"}</Button>
          {user.photoUrl && <Button variant="secondary" loading={busy} onClick={() => save(null)}>Hapus foto</Button>}
        </div>
      ) : (
        <form className="photo-form" onSubmit={(e) => { e.preventDefault(); save(url.trim() || null); }}>
          <Field label="Alamat gambar (URL)" id="ph" hint="Diawali https:// atau http://; maksimal 500 karakter" error={err}>
            <input id="ph" type="url" value={url} onChange={(e) => setUrl(e.target.value)} placeholder="https://…" maxLength={500} autoFocus />
          </Field>
          <div className="row-actions">
            <Button type="button" variant="secondary" onClick={() => { setEditing(false); setErr(null); }}>Batal</Button>
            <Button type="submit" loading={busy}>Simpan foto</Button>
          </div>
        </form>
      )}
    </div>
  );
}

// FR-27: perubahan username/email/HP aktif setelah OTP ke kontak baru.
function ProfileCard() {
  const { user, setUser } = useAuth();
  const [draft, setDraft] = useState(null);
  const [otp, setOtp] = useState(null);
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState(null);
  const [err, setErr] = useState(null);

  async function request(e) {
    e.preventDefault();
    // Backend menerima draf profil lengkap (bukan hanya kolom yang berubah).
    const body = {
      username: draft.username.trim(),
      email: draft.email.trim() || null,
      phoneNumber: draft.phoneNumber.trim() || null,
    };
    const changed = ["username", "email", "phoneNumber"].some((k) => (body[k] || "") !== (user[k] || ""));
    if (!changed) return setErr("Tidak ada perubahan.");
    setBusy(true); setErr(null);
    try { setOtp((await api.post("/me/profile-change", body)).data); } catch (ex) { setErr(errorMessage(ex)); } finally { setBusy(false); }
  }
  async function confirm(e) {
    e.preventDefault();
    setBusy(true); setErr(null);
    try {
      const { data } = await api.post("/me/profile-change/confirm", { code });
      setUser(data); setDraft(null); setOtp(null); setCode(""); setMsg("Profil diperbarui.");
    } catch (ex) { setErr(errorMessage(ex)); } finally { setBusy(false); }
  }

  return (
    <Card title="Profil" actions={!draft && <Button variant="secondary" onClick={() => { setMsg(null); setDraft({ username: user.username, email: user.email || "", phoneNumber: user.phoneNumber || "" }); }}>Ubah</Button>}>
      <PhotoEditor />
      {!draft ? (
        <Rows items={[
          ["Username", user.username], ["Email", user.email || "–"], ["Nomor HP", user.phoneNumber || "–"],
          ["Status e-KYC", user.ekycStatus === "APPROVED" ? "Disetujui" : user.ekycStatus || "–"],
        ]} />
      ) : !otp ? (
        <form onSubmit={request}>
          <Field label="Username" id="pu"><input id="pu" value={draft.username} onChange={(e) => setDraft({ ...draft, username: e.target.value })} /></Field>
          <Field label="Email" id="pe"><input id="pe" type="email" value={draft.email} onChange={(e) => setDraft({ ...draft, email: e.target.value })} /></Field>
          <Field label="Nomor HP" id="pp"><input id="pp" inputMode="tel" value={draft.phoneNumber} onChange={(e) => setDraft({ ...draft, phoneNumber: e.target.value })} /></Field>
          <Alert>{err}</Alert>
          <div className="row-actions"><Button variant="secondary" type="button" onClick={() => { setDraft(null); setErr(null); }}>Batal</Button><Button type="submit" loading={busy}>Kirim OTP</Button></div>
        </form>
      ) : (
        <form onSubmit={confirm}>
          <p className="small">Kode OTP dikirim ke {otp.sentTo}.</p>
          {otp.demoCode && <Alert kind="info">Mode simulasi – kode OTP: <strong>{otp.demoCode}</strong></Alert>}
          <Field label="Kode OTP" id="po"><input id="po" inputMode="numeric" maxLength={6} value={code} onChange={(e) => setCode(onlyDigits(e.target.value))} autoFocus /></Field>
          <Alert>{err}</Alert>
          <Button type="submit" loading={busy} className="block">Konfirmasi perubahan</Button>
        </form>
      )}
      <Alert kind="success">{msg}</Alert>
    </Card>
  );
}

// FR-28: ubah password dan PIN.
function SecurityCard() {
  const [mode, setMode] = useState(null);
  const [a, setA] = useState("");
  const [b, setB] = useState("");
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState(null);
  const [err, setErr] = useState(null);
  const isPin = mode === "pin";

  async function submit(e) {
    e.preventDefault();
    setBusy(true); setErr(null);
    try {
      if (isPin) await api.post("/me/pin", { oldPin: a, newPin: b });
      else await api.post("/me/password", { oldPassword: a, newPassword: b });
      setMsg(isPin ? "PIN berhasil diubah." : "Password berhasil diubah."); setMode(null); setA(""); setB("");
    } catch (ex) { setErr(errorMessage(ex)); } finally { setBusy(false); }
  }
  const inp = (id, v, s) => isPin
    ? <input id={id} className="pin-input small-pin" type="password" inputMode="numeric" maxLength={6} value={v} onChange={(e) => s(onlyDigits(e.target.value))} required />
    : <input id={id} type="password" value={v} onChange={(e) => s(e.target.value)} required />;

  return (
    <Card title="Keamanan">
      {!mode ? (
        <div className="row-actions">
          <Button variant="secondary" onClick={() => { setMsg(null); setMode("password"); }}>Ubah password</Button>
          <Button variant="secondary" onClick={() => { setMsg(null); setMode("pin"); }}>Ubah PIN</Button>
        </div>
      ) : (
        <form onSubmit={submit}>
          <Field label={isPin ? "PIN lama" : "Password lama"} id="sa">{inp("sa", a, setA)}</Field>
          <Field label={isPin ? "PIN baru (6 digit)" : "Password baru"} id="sb" hint={isPin ? null : "Minimal 8 karakter, huruf dan angka"}>{inp("sb", b, setB)}</Field>
          <Alert>{err}</Alert>
          <div className="row-actions"><Button type="button" variant="secondary" onClick={() => { setMode(null); setErr(null); }}>Batal</Button><Button type="submit" loading={busy}>Simpan</Button></div>
        </form>
      )}
      <Alert kind="success">{msg}</Alert>
    </Card>
  );
}

const TYPES = [["INCOMING", "Dana masuk"], ["OUTGOING", "Dana keluar"], ["SECURITY", "Keamanan"], ["INFO", "Informasi"]];
const CHANNELS = [["IN_APP", "Kotak notifikasi"], ["PUSH", "Push (aplikasi Android)"], ["EMAIL", "Email"]];

function NotificationSettings() {
  const [s, setS] = useState(null);
  const [err, setErr] = useState(null);
  const [msg, setMsg] = useState(null);
  useEffect(() => { api.get("/me/notification-settings").then((r) => setS(r.data)).catch((ex) => setErr(errorMessage(ex))); }, []);

  async function toggle(group, value) {
    const cur = new Set(s[group]);
    cur.has(value) ? cur.delete(value) : cur.add(value);
    const next = { ...s, [group]: [...cur] };
    setErr(null); setMsg(null);
    try { setS((await api.put("/me/notification-settings", next)).data); setMsg("Tersimpan."); }
    catch (ex) { setErr(errorMessage(ex)); }
  }
  const box = (group, [v, l]) => (
    <label key={v} className="check"><input type="checkbox" checked={s[group].includes(v)} onChange={() => toggle(group, v)} /> {l}</label>
  );
  return (
    <Card title="Pengaturan notifikasi">
      {!s ? <Spinner /> : (
        <>
          <p className="small muted">Jenis</p>
          <div className="checks">{TYPES.map((t) => box("types", t))}</div>
          <p className="small muted">Kanal</p>
          <div className="checks">{CHANNELS.map((c) => box("channels", c))}</div>
        </>
      )}
      <Alert>{err}</Alert>
      <Alert kind="success">{msg}</Alert>
    </Card>
  );
}

// FR-05: daftar sesi dan logout semua perangkat.
function Sessions() {
  const { logout } = useAuth();
  const [list, setList] = useState(null);
  const load = useCallback(() => api.get("/auth/sessions").then((r) => setList(r.data)).catch(() => setList([])), []);
  useEffect(() => { load(); }, [load]);
  return (
    <Card title="Sesi aktif">
      {!list ? <Spinner /> : (
        <ul className="sessions">
          {list.map((s) => (
            <li key={s.id}>
              {/web/i.test(s.deviceName || s.deviceId) ? <Laptop size={18} aria-hidden /> : <Smartphone size={18} aria-hidden />}
              <div>
                <strong>{s.deviceName || s.deviceId}</strong> {s.current && <span className="badge badge-ok">Perangkat ini</span>}
                <div className="small muted">Aktif terakhir {dateTime(s.lastSeenAt)} · {s.authMethod === "BIOMETRIC" ? "biometrik" : "password"}</div>
              </div>
            </li>
          ))}
        </ul>
      )}
      <Button variant="danger" onClick={() => window.confirm("Keluar dari semua perangkat, termasuk aplikasi Android?") && logout(true)}>
        <LogOut size={16} aria-hidden /> Keluar dari semua perangkat
      </Button>
    </Card>
  );
}
