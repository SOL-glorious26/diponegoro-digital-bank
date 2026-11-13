import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Eye, EyeOff, ShieldCheck } from "lucide-react";
import { Brand } from "../components/Layout";
import { Alert, Button, Field } from "../components/ui";
import { api, errorCode, errorMessage } from "../api/client";
import { onboardingStep, useAuth } from "../auth/AuthContext";
import { deviceId, deviceName, onlyDigits } from "../lib/format";

function AuthFrame({ title, subtitle, children, footer }) {
  return (
    <div className="auth-page">
      <div className="auth-side">
        <Brand light />
        <h1>Bank digital untuk mahasiswa, profesional, dan UMKM.</h1>
        <p>Transfer, QRIS, top-up, dan riwayat transaksi dalam satu akun yang sama dengan aplikasi Android.</p>
        <div className="auth-note"><ShieldCheck size={18} aria-hidden /> Lingkungan simulasi – data dan transaksi bukan uang sungguhan.</div>
      </div>
      <div className="auth-main">
        <div className="auth-card">
          <h2>{title}</h2>
          {subtitle && <p className="muted">{subtitle}</p>}
          {children}
          {footer && <div className="auth-footer">{footer}</div>}
        </div>
      </div>
    </div>
  );
}

function PasswordInput({ id, value, onChange, autoComplete = "current-password" }) {
  const [show, setShow] = useState(false);
  return (
    <div className="input-wrap">
      <input id={id} type={show ? "text" : "password"} value={value} onChange={onChange} autoComplete={autoComplete} required />
      <button type="button" className="icon-btn" onClick={() => setShow(!show)} aria-label={show ? "Sembunyikan" : "Tampilkan"}>
        {show ? <EyeOff size={18} /> : <Eye size={18} />}
      </button>
    </div>
  );
}

// ---------------- Login (FR-03): tanpa OTP ----------------
export function LoginPage() {
  const { login, notice, setNotice } = useAuth();
  const nav = useNavigate();
  const loc = useLocation();
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);

  async function submit(e) {
    e.preventDefault();
    setBusy(true); setErr(null);
    try {
      const user = await login(identifier.trim(), password);
      const step = onboardingStep(user);
      nav(step === "ekyc" ? "/ekyc" : step === "pin" ? "/pin/buat" : loc.state?.from || "/", { replace: true });
    } catch (ex) {
      setErr(errorCode(ex) === "OTP_REQUIRED"
        ? "Akun belum diverifikasi. Selesaikan verifikasi OTP saat pendaftaran atau daftar ulang."
        : errorMessage(ex));
    } finally { setBusy(false); }
  }

  return (
    <AuthFrame title="Masuk" subtitle="Gunakan username, email, atau nomor HP."
      footer={<>Belum punya akun? <Link to="/daftar">Daftar</Link></>}>
      <Alert kind="info" onClose={() => setNotice(null)}>{notice}</Alert>
      <form onSubmit={submit}>
        <Field label="Username / email / nomor HP" id="id">
          <input id="id" value={identifier} onChange={(e) => setIdentifier(e.target.value)} autoComplete="username" required autoFocus />
        </Field>
        <Field label="Password" id="pw">
          <PasswordInput id="pw" value={password} onChange={(e) => setPassword(e.target.value)} />
        </Field>
        <Alert>{err}</Alert>
        <Button type="submit" loading={busy} className="block">Masuk</Button>
      </form>
      <p className="center small"><Link to="/lupa">Lupa password atau PIN?</Link></p>
      <p className="center small muted">Akun demo: demo / Demo@1234 · PIN 123456</p>
    </AuthFrame>
  );
}

// ---------------- Registrasi + OTP (FR-01) ----------------
export function RegisterPage() {
  const { acceptTokens } = useAuth();
  const nav = useNavigate();
  const [f, setF] = useState({ username: "", email: "", phoneNumber: "", password: "", confirm: "" });
  const [reg, setReg] = useState(null); // {userId, otp}
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function register(e) {
    e.preventDefault();
    if (f.password !== f.confirm) return setErr("Konfirmasi password tidak sama.");
    if (!f.email && !f.phoneNumber) return setErr("Isi email atau nomor HP untuk menerima OTP.");
    setBusy(true); setErr(null);
    try {
      const { data } = await api.post("/auth/register", {
        username: f.username.trim().toLowerCase(), email: f.email.trim() || undefined,
        phoneNumber: f.phoneNumber.trim() || undefined, password: f.password,
      });
      setReg(data);
    } catch (ex) { setErr(errorMessage(ex)); } finally { setBusy(false); }
  }

  async function verify(e) {
    e.preventDefault();
    setBusy(true); setErr(null);
    try {
      const { data } = await api.post("/auth/register/verify-otp", { userId: reg.userId, code, deviceId: deviceId(), deviceName: deviceName() });
      acceptTokens(data);
      nav("/ekyc", { replace: true });
    } catch (ex) { setErr(errorMessage(ex)); setCode(""); } finally { setBusy(false); }
  }

  async function resend() {
    setErr(null);
    try {
      const { data } = await api.post("/auth/register/resend-otp", { userId: reg.userId });
      setReg({ ...reg, otp: data });
    } catch (ex) { setErr(errorMessage(ex)); }
  }

  if (reg) {
    return (
      <AuthFrame title="Verifikasi OTP" subtitle={`Kode dikirim ke ${reg.otp?.sentTo ?? "kontak Anda"}.`}>
        {reg.otp?.demoCode && <Alert kind="info">Mode simulasi – kode OTP: <strong>{reg.otp.demoCode}</strong></Alert>}
        <form onSubmit={verify}>
          <Field label="Kode OTP (6 digit)" id="otp">
            <input id="otp" inputMode="numeric" maxLength={6} value={code} onChange={(e) => setCode(onlyDigits(e.target.value))} autoFocus required />
          </Field>
          <Alert>{err}</Alert>
          <Button type="submit" loading={busy} className="block" disabled={code.length !== 6}>Verifikasi</Button>
        </form>
        <p className="center small"><button className="link" onClick={resend}>Kirim ulang kode</button></p>
      </AuthFrame>
    );
  }

  return (
    <AuthFrame title="Buat akun" subtitle="Langkah 1 dari 3: data akun. Berikutnya verifikasi identitas (e-KYC) dan PIN."
      footer={<>Sudah punya akun? <Link to="/login">Masuk</Link></>}>
      <form onSubmit={register}>
        <Field label="Username" id="u" hint="4–30 karakter: huruf kecil, angka, titik, garis bawah">
          <input id="u" value={f.username} onChange={set("username")} autoComplete="username" required autoFocus />
        </Field>
        <Field label="Email" id="e"><input id="e" type="email" value={f.email} onChange={set("email")} autoComplete="email" /></Field>
        <Field label="Nomor HP" id="p" hint="Isi email atau nomor HP (minimal salah satu)">
          <input id="p" inputMode="tel" value={f.phoneNumber} onChange={set("phoneNumber")} autoComplete="tel" />
        </Field>
        <Field label="Password" id="pw" hint="Minimal 8 karakter, berisi huruf dan angka">
          <PasswordInput id="pw" value={f.password} onChange={set("password")} autoComplete="new-password" />
        </Field>
        <Field label="Ulangi password" id="pw2">
          <PasswordInput id="pw2" value={f.confirm} onChange={set("confirm")} autoComplete="new-password" />
        </Field>
        <Alert>{err}</Alert>
        <Button type="submit" loading={busy} className="block">Daftar</Button>
      </form>
    </AuthFrame>
  );
}

// ---------------- e-KYC (FR-02) ----------------
export function EkycPage() {
  const { user, reloadUser, logout } = useAuth();
  const nav = useNavigate();
  const [ekyc, setEkyc] = useState(null);
  const [f, setF] = useState({ nik: "", fullName: "", selfie: null });
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);

  useEffect(() => { api.get("/auth/ekyc").then((r) => setEkyc(r.data)).catch(() => setEkyc(null)); }, []);

  // Selama PROCESSING, cek status tiap 1,5 detik (simulator memutuskan ±3 detik).
  useEffect(() => {
    if (ekyc?.status !== "PROCESSING") return;
    const t = setInterval(async () => {
      try {
        const { data } = await api.get("/auth/ekyc");
        setEkyc(data);
        if (data.status === "APPROVED") {
          const u = await reloadUser();
          nav(u.hasPin ? "/" : "/pin/buat", { replace: true });
        }
      } catch { /* coba lagi */ }
    }, 1500);
    return () => clearInterval(t);
  }, [ekyc?.status, reloadUser, nav]);

  async function submit(e) {
    e.preventDefault();
    if (f.nik.length !== 16) return setErr("NIK terdiri atas 16 digit.");
    if (!f.selfie) return setErr("Unggah foto swafoto.");
    setBusy(true); setErr(null);
    try {
      // Simulasi: hanya nama berkas swafoto yang dikirim sebagai referensi.
      const { data } = await api.post("/auth/ekyc", { nik: f.nik, fullName: f.fullName || undefined, selfieReference: f.selfie.name });
      setEkyc(data);
    } catch (ex) { setErr(errorMessage(ex)); } finally { setBusy(false); }
  }

  const rejected = ekyc?.status === "REJECTED";
  return (
    <AuthFrame title="Verifikasi identitas (e-KYC)"
      subtitle="Langkah 2 dari 3. Transaksi keuangan hanya aktif setelah e-KYC disetujui."
      footer={<button className="link" onClick={() => logout()}>Keluar ({user?.username})</button>}>
      {ekyc?.status === "PROCESSING" ? (
        <div className="center"><p>Data e-KYC ({ekyc.nikMasked}) sedang diperiksa…</p></div>
      ) : (
        <form onSubmit={submit}>
          {rejected && <Alert>e-KYC ditolak{ekyc.reason ? `: ${ekyc.reason}` : ""}. Periksa data lalu kirim ulang.</Alert>}
          <Field label="NIK (16 digit)" id="nik" hint="Simulasi: NIK berakhiran 0000 akan ditolak">
            <input id="nik" inputMode="numeric" maxLength={16} value={f.nik} onChange={(e) => setF({ ...f, nik: onlyDigits(e.target.value) })} required />
          </Field>
          <Field label="Nama lengkap sesuai KTP" id="fn">
            <input id="fn" value={f.fullName} onChange={(e) => setF({ ...f, fullName: e.target.value })} />
          </Field>
          <Field label="Swafoto" id="sf" hint="Foto wajah Anda (JPG/PNG)">
            <input id="sf" type="file" accept="image/*" onChange={(e) => setF({ ...f, selfie: e.target.files?.[0] ?? null })} />
          </Field>
          <Alert>{err}</Alert>
          <Button type="submit" loading={busy} className="block">Kirim data e-KYC</Button>
        </form>
      )}
    </AuthFrame>
  );
}

// ---------------- Buat PIN (FR-04) ----------------
export function CreatePinPage() {
  const { reloadUser } = useAuth();
  const nav = useNavigate();
  const [pin, setPin] = useState("");
  const [pin2, setPin2] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);

  async function submit(e) {
    e.preventDefault();
    if (pin.length !== 6) return setErr("PIN terdiri atas 6 digit.");
    if (pin !== pin2) return setErr("Konfirmasi PIN tidak sama.");
    setBusy(true); setErr(null);
    try {
      await api.post("/auth/pin", { pin });
      await reloadUser();
      nav("/", { replace: true });
    } catch (ex) { setErr(errorMessage(ex)); } finally { setBusy(false); }
  }
  const pinInput = (id, v, s) => (
    <input id={id} className="pin-input" type="password" inputMode="numeric" maxLength={6} value={v}
      onChange={(e) => s(onlyDigits(e.target.value).slice(0, 6))} autoComplete="off" />
  );
  return (
    <AuthFrame title="Buat PIN transaksi" subtitle="Langkah 3 dari 3. PIN dipakai untuk mengonfirmasi setiap transaksi; berbeda dari password.">
      <form onSubmit={submit}>
        <Field label="PIN baru (6 digit)" id="p1">{pinInput("p1", pin, setPin)}</Field>
        <Field label="Ulangi PIN" id="p2">{pinInput("p2", pin2, setPin2)}</Field>
        <Alert>{err}</Alert>
        <Button type="submit" loading={busy} className="block">Simpan PIN</Button>
      </form>
    </AuthFrame>
  );
}

// ---------------- Pemulihan password/PIN (FR-05) ----------------
export function RecoveryPage() {
  const nav = useNavigate();
  const { setNotice } = useAuth();
  const [step, setStep] = useState(1);
  const [type, setType] = useState("PASSWORD");
  const [identifier, setIdentifier] = useState("");
  const [otp, setOtp] = useState(null);
  const [code, setCode] = useState("");
  const [token, setToken] = useState(null);
  const [secret, setSecret] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);

  const run = async (fn) => { setBusy(true); setErr(null); try { await fn(); } catch (ex) { setErr(errorMessage(ex)); } finally { setBusy(false); } };

  return (
    <AuthFrame title="Pemulihan akun" subtitle="Atur ulang password atau PIN dengan kode OTP."
      footer={<Link to="/login">Kembali ke halaman masuk</Link>}>
      {step === 1 && (
        <form onSubmit={(e) => { e.preventDefault(); run(async () => {
          const { data } = await api.post("/auth/recovery/request", { identifier: identifier.trim(), type });
          setOtp(data.otp); setStep(2);
        }); }}>
          <div className="segmented" role="radiogroup" aria-label="Yang dipulihkan">
            {["PASSWORD", "PIN"].map((t) => (
              <button type="button" key={t} role="radio" aria-checked={type === t} className={type === t ? "on" : ""} onClick={() => setType(t)}>
                {t === "PASSWORD" ? "Password" : "PIN"}
              </button>
            ))}
          </div>
          <Field label="Username / email / nomor HP" id="rid">
            <input id="rid" value={identifier} onChange={(e) => setIdentifier(e.target.value)} required autoFocus />
          </Field>
          <Alert>{err}</Alert>
          <Button type="submit" loading={busy} className="block">Kirim kode OTP</Button>
        </form>
      )}
      {step === 2 && (
        <form onSubmit={(e) => { e.preventDefault(); run(async () => {
          const { data } = await api.post("/auth/recovery/verify", { identifier: identifier.trim(), type, code });
          setToken(data.recoveryToken); setStep(3);
        }); }}>
          {otp?.demoCode && <Alert kind="info">Mode simulasi – kode OTP: <strong>{otp.demoCode}</strong></Alert>}
          <Field label="Kode OTP" id="rc"><input id="rc" inputMode="numeric" maxLength={6} value={code} onChange={(e) => setCode(onlyDigits(e.target.value))} required autoFocus /></Field>
          <Alert>{err}</Alert>
          <Button type="submit" loading={busy} className="block">Verifikasi</Button>
        </form>
      )}
      {step === 3 && (
        <form onSubmit={(e) => { e.preventDefault(); run(async () => {
          await api.post("/auth/recovery/reset", { identifier: identifier.trim(), type, recoveryToken: token, newSecret: secret });
          setNotice(type === "PASSWORD" ? "Password berhasil diubah. Silakan masuk." : "PIN berhasil diubah.");
          nav("/login", { replace: true });
        }); }}>
          <Field label={type === "PASSWORD" ? "Password baru" : "PIN baru (6 digit)"} id="ns">
            {type === "PASSWORD"
              ? <PasswordInput id="ns" value={secret} onChange={(e) => setSecret(e.target.value)} autoComplete="new-password" />
              : <input id="ns" className="pin-input" type="password" inputMode="numeric" maxLength={6} value={secret} onChange={(e) => setSecret(onlyDigits(e.target.value))} />}
          </Field>
          <Alert>{err}</Alert>
          <Button type="submit" loading={busy} className="block">Simpan</Button>
        </form>
      )}
    </AuthFrame>
  );
}
