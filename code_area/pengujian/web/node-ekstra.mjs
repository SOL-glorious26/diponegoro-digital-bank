// Uji lanjutan Web tim (Node/Express/EJS) terhadap backend asli: WN-15 s.d. WN-25.
// Prasyarat: backend :8080 (data demo) dan Web Node :3000. WN-21 memakai pasangan kedua:
//   backend :8081 dengan --ddb.security.access-token-minutes=1 dan Web Node :3001 (DDB_WEB_NODE2, DDB_API_URL2).
// Jalankan: node web/node-ekstra.mjs hasil/web-node-ekstra [WN-15,WN-16,...]
import path from "node:path";
import { mkdirSync, writeFileSync } from "node:fs";
import { Browser } from "../lib/web-node.mjs";

const OUT = path.resolve(process.argv[2] || "hasil/web-node-ekstra");
const only = process.argv[3]?.split(",");
const API = process.env.DDB_API_URL || "http://localhost:8080/api/v1";
const API2 = process.env.DDB_API_URL2 || "http://localhost:8081/api/v1";
const WEB2 = process.env.DDB_WEB_NODE2 || "http://localhost:3001";
mkdirSync(OUT, { recursive: true });
const log = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const expect = (c, m) => { if (!c) throw new Error(m); };
async function run(id, title, fn) {
  if (only && !only.includes(id)) return;
  try { const n = await fn(); log.push(`PASS  ${id}  ${title}${n ? "  — " + n : ""}`); }
  catch (e) { log.push(`FAIL  ${id}  ${title}  — ${String(e.message).replace(/\s+/g, " ").slice(0, 400)}`); }
  console.log(log.at(-1));
}
async function token(user, pw = "Demo@1234", base = API) {
  const r = await fetch(base + "/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ identifier: user, password: pw, deviceId: "wnx-" + user }) });
  return (await r.json()).accessToken;
}
const api = async (user, p) => (await fetch(API + p, { headers: { Authorization: "Bearer " + await token(user) } })).json();
const bal = async (u) => (await api(u, "/accounts/me")).balance;
const otp = (b) => { const c = b.text.match(/\b(\d{6})\b/)?.[1]; expect(c, "OTP demo tidak tampil: " + b.text.slice(0, 200)); return c; };
const wrongOtp = (c) => (c === "111111" ? "222222" : "111111");
const uid = (p) => p + Date.now().toString(36) + Math.floor(Math.random() * 90 + 10);
async function daftar(u, { ekyc = true, pin = "246810" } = {}) {
  const n = new Browser();
  await n.get("/register");
  await n.post("/register", { username: u, email: u + "@mail.test", password: "Rahasia123", password2: "Rahasia123" });
  expect(n.path === "/register/otp", "registrasi: " + n.text.slice(0, 200));
  await n.post("/register/otp", { kode: otp(n) }); expect(n.path === "/ekyc", "OTP: " + n.text.slice(0, 200));
  if (!ekyc) return n;
  await n.post("/ekyc", { nik: "3374010101010001", fullName: "Uji Web", selfie: "s.jpg" });
  for (let i = 0; i < 15 && n.path === "/ekyc"; i++) { await sleep(1000); await n.get("/ekyc"); }
  await n.post("/pin", { pin, pin2: pin }); expect(n.path === "/dashboard", "PIN: " + n.text.slice(0, 200));
  return n;
}
async function topup(n, nominal) {
  await n.get("/pembayaran?tab=topup");
  const sumber = n.html.match(/name="sumber"[\s\S]*?<option[^>]*>([^<]+)</)?.[1];
  expect(sumber, "sumber dana tidak ada");
  return { sumber, kirim: (x) => n.post("/pembayaran?tab=topup", { sumber, nominal: String(x ?? nominal), key: n.html.match(/name="key"\s+value="([^"]+)"/)?.[1] || "" }) };
}

const b = new Browser();
await b.login("budi");
await run("WN-15", "rekening tujuan tidak valid ditolak sebelum PIN", async () => {
  const b0 = await bal("budi");
  await b.get("/transfer?tab=antar");
  await b.post("/transfer?tab=antar", { stage: "confirm", metode: "antar", rekening_tujuan: "1400999999", nominal: "20000" });
  expect(/tidak valid|tidak ditemukan/i.test(b.text) && !/name="pin"/.test(b.html), "tidak ditolak: " + b.text.slice(0, 300));
  expect(await bal("budi") === b0, "saldo berubah");
  return b.text.match(/Rekening tujuan tidak \w+/)?.[0];
});
await run("WN-16", "token PLN ID berakhiran 0000 ditolak", async () => {
  const b0 = await bal("budi");
  await b.get("/pembayaran?tab=pln");
  await b.post("/pembayaran?tab=pln", { stage: "confirm", meterNumber: "12345670000", nominal: "20000" });
  expect(/tidak ditemukan/i.test(b.text) && !/name="pin"/.test(b.html), "tidak ditolak: " + b.text.slice(0, 300));
  expect(await bal("budi") === b0, "saldo berubah");
});
await run("WN-17", "Top Up menambah saldo tepat; nominal di bawah minimum ditolak", async () => {
  const b0 = await bal("budi"), t = await topup(b);
  await t.kirim(500);
  expect(/Nominal top up/i.test(b.text) && await bal("budi") === b0, "nominal kecil tidak ditolak");
  await t.kirim(50000);
  expect(await bal("budi") - b0 === 50000, "saldo tidak bertambah Rp50.000");
  return "sumber " + t.sumber;
});
await run("WN-18", "ubah email: OTP salah ditolak, OTP benar mengubah profil", async () => {
  const u = uid("wp"), n = await daftar(u), baru = u + "-baru@mail.test";
  await n.get("/profil");
  await n.post("/profil", { aksi: "profil", username: u, email: baru, no_hp: "0812" + String(Date.now()).slice(-8) });
  expect(n.path === "/profil/otp", "tidak ke OTP: " + n.path + " " + n.text.slice(0, 250));
  const c = otp(n);
  await n.post("/profil/otp", { kode: wrongOtp(c) });
  expect(/OTP/i.test(n.text) && !/berhasil diperbarui/i.test(n.text), "OTP salah tidak ditolak: " + n.text.slice(0, 250));
  await n.post("/profil/otp", { kode: c });
  expect(n.path === "/profil" && n.html.includes(baru), "email tidak berubah: " + n.text.slice(0, 250));
});
await run("WN-19", "ubah kata sandi: sandi lama salah ditolak; sandi baru dipakai login", async () => {
  const u = uid("ws"), n = await daftar(u);
  await n.get("/profil");
  await n.post("/profil", { aksi: "sandi", sandi_lama: "Salah12345", sandi_baru: "Baru12345", sandi_baru2: "Baru12345" });
  expect(!/berhasil diubah/i.test(n.text), "sandi lama salah diterima");
  await n.post("/profil", { aksi: "sandi", sandi_lama: "Rahasia123", sandi_baru: "Baru12345", sandi_baru2: "Baru12345" });
  expect(/berhasil diubah/i.test(n.text), "gagal: " + n.text.slice(0, 250));
  const lama = new Browser(); await lama.login(u, "Rahasia123"); expect(lama.path === "/login", "sandi lama masih berlaku");
  const baru = new Browser(); await baru.login(u, "Baru12345"); expect(baru.path === "/dashboard", "sandi baru gagal: " + baru.path);
});
await run("WN-20", "pemulihan kata sandi dan PIN dengan OTP", async () => {
  const u = uid("wr"), n = await daftar(u); await n.get("/logout");
  await n.get("/lupa-sandi");
  await n.post("/lupa-sandi", { identitas: u, type: "PASSWORD" });
  await n.post("/lupa-sandi", { kode: otp(n), password: "Pulih12345", password2: "Pulih12345" });
  expect(n.path === "/login" && /berhasil/i.test(n.text), "pemulihan sandi gagal: " + n.text.slice(0, 250));
  const r = new Browser(); await r.get("/lupa-sandi");
  await r.post("/lupa-sandi", { identitas: u, type: "PIN" });
  await r.post("/lupa-sandi", { kode: otp(r), password: "135790", password2: "135790" });
  expect(/PIN berhasil diubah/i.test(r.text), "pemulihan PIN gagal: " + r.text.slice(0, 250));
  const m = new Browser(); await m.login(u, "Pulih12345"); expect(m.path === "/dashboard", "login sandi baru gagal: " + m.path + " " + m.text.slice(0, 150));
  await (await topup(m, 100000)).kirim();
  await m.get("/pembayaran?tab=pulsa");
  await m.post("/pembayaran?tab=pulsa", { stage: "confirm", phone: "081234567890", nominal: "10000" });
  await m.post("/pembayaran?tab=pulsa", { stage: "pay", pin: "246810" });
  expect(/PIN salah/i.test(m.text), "PIN lama masih diterima: " + m.text.slice(0, 200));
  await m.post("/pembayaran?tab=pulsa", { stage: "pay", pin: "135790" });
  expect(/berhasil/i.test(m.text), "PIN baru tidak diterima: " + m.text.slice(0, 250));
});
await run("WN-21", "access token 1 menit diperbarui otomatis; setelah dicabut kembali ke login", async () => {
  const x = new Browser(), asli = x.req.bind(x);
  x.req = (u, o) => asli(new URL(u, WEB2).href, o);
  await x.login("budi"); expect(x.path === "/dashboard", "login pasangan kedua gagal: " + x.path);
  await sleep(75000);
  await x.get("/riwayat"); expect(x.path === "/riwayat", "token tidak diperbarui: " + x.path);
  await fetch(API2 + "/auth/logout-all", { method: "POST", headers: { Authorization: "Bearer " + await token("budi", "Demo@1234", API2) } });
  await x.get("/riwayat"); expect(x.path === "/login" && /berakhir/i.test(x.text), "sesi tidak berakhir: " + x.path);
  return "aksi setelah 75 detik tetap berjalan; setelah dicabut tampil pesan sesi berakhir";
});
await run("WN-22", "filter riwayat (jenis, kata kunci, tanggal) dan detail dengan jurnal", async () => {
  const baris = () => (b.html.match(/detail=/g) || []).length;
  await b.get("/riwayat"); const semua = baris();
  await b.get("/riwayat?jenis=TRANSFER"); const tr = baris();
  expect(tr > 0 && tr < semua, `filter jenis: ${tr} dari ${semua}`);
  await b.get("/riwayat?q=zzzz-tidak-ada"); expect(/Tidak ada transaksi/.test(b.text), "kata kunci tidak menyaring");
  await b.get("/riwayat?dari=2020-01-01&sampai=2020-01-31"); expect(/Tidak ada transaksi/.test(b.text), "tanggal tidak menyaring");
  await b.get("/riwayat?jenis=TRANSFER"); const id = b.html.match(/detail=([^"&]+)"/)[1];
  await b.get("/riwayat?detail=" + id); expect(/Detail Transaksi/.test(b.text) && /Jurnal/.test(b.text), "detail/jurnal tidak tampil");
  return `transfer ${tr} dari ${semua} baris pada halaman pertama`;
});
await run("WN-23", "akun belum e-KYC dialihkan dari halaman transaksi", async () => {
  const n = await daftar(uid("wk"), { ekyc: false });
  for (const p of ["/transfer", "/pembayaran", "/dashboard"]) { await n.get(p); expect(n.path === "/ekyc", p + " terbuka: " + n.path); }
});
await run("WN-24", "tiga kali PIN salah memblokir PIN", async () => {
  const n = await daftar(uid("wb"));
  await (await topup(n, 100000)).kirim();
  await n.get("/transfer?tab=antar");
  await n.post("/transfer?tab=antar", { stage: "confirm", metode: "antar", rekening_tujuan: "1400000002", nominal: "20000" });
  for (let i = 0; i < 3; i++) await n.post("/transfer?tab=antar", { stage: "verify", pin: "000000" });
  expect(/blokir/i.test(n.text), "tidak diblokir: " + n.text.slice(0, 300));
  await n.post("/transfer?tab=antar", { stage: "verify", pin: "246810" });
  expect(/blokir/i.test(n.text) && !/berhasil/i.test(n.text), "PIN benar diterima saat blokir");
});
await run("WN-25", "POST tanpa token CSRF ditolak", async () => {
  const b0 = await bal("budi");
  await b.req("/transfer?tab=antar", { method: "POST", body: new URLSearchParams({ stage: "confirm", metode: "antar", rekening_tujuan: "1400000002", nominal: "20000" }), headers: { "Content-Type": "application/x-www-form-urlencoded" } });
  expect(b.res.status === 403, "status " + b.res.status); expect(await bal("budi") === b0, "saldo berubah");
  return "HTTP 403";
});
writeFileSync(path.join(OUT, "hasil.txt"), log.join("\n") + "\n");
console.log(`\n${log.filter((l) => l.startsWith("PASS")).length}/${log.length} PASS`);
