// Uji alur inti Web tim (Node/Express/EJS, digitalbanking-node) terhadap backend asli: WN-01 s.d. WN-14.
// Prasyarat: backend http://localhost:8080 (data demo) dan Web Node http://localhost:3000.
// Jalankan: node web/node-smoke.mjs hasil/web-node
import path from "node:path";
import { mkdirSync, writeFileSync } from "node:fs";
import { Browser } from "../lib/web-node.mjs";

const OUT = path.resolve(process.argv[2] || "hasil/web-node");
const WEB = process.env.DDB_WEB_NODE || "http://localhost:3000";
const API = process.env.DDB_API_URL || "http://localhost:8080/api/v1";
mkdirSync(OUT, { recursive: true });
const log = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const expect = (c, m) => { if (!c) throw new Error(m); };
async function run(id, title, fn) {
  try { const n = await fn(); log.push(`PASS  ${id}  ${title}${n ? "  — " + n : ""}`); }
  catch (e) { log.push(`FAIL  ${id}  ${title}  — ${String(e.message).replace(/\s+/g, " ").slice(0, 300)}`); }
  console.log(log.at(-1));
}

async function token(user) {
  const r = await fetch(API + "/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ identifier: user, password: "Demo@1234", deviceId: "wn-probe-" + user }) });
  return (await r.json()).accessToken;
}
async function api(user, p, opt = {}) {
  const r = await fetch(API + p, { ...opt, headers: { "Content-Type": "application/json", Authorization: "Bearer " + await token(user), ...(opt.headers || {}) } });
  return r.headers.get("content-type")?.includes("json") ? r.json() : r.text();
}
const bal = async (u) => (await api(u, "/accounts/me")).balance;
const rp = (n) => new Intl.NumberFormat("id-ID").format(n);

const b = new Browser();
await run("WN-01", "login salah ditolak, login benar masuk Dashboard", async () => {
  await b.login("budi", "Salah@123");
  expect(b.path === "/login" && /salah/i.test(b.text), "login salah tidak ditolak: " + b.path);
  await b.login("budi");
  expect(b.path === "/dashboard", "tidak masuk dashboard: " + b.path + " " + b.text.slice(0, 160));
  expect(b.text.includes(rp(await bal("budi"))), "saldo Web tidak sama dengan backend");
  return "saldo Web = backend";
});
await run("WN-02", "halaman terlindungi menolak tanpa login", async () => {
  const x = new Browser();
  for (const p of ["/dashboard", "/transfer", "/pembayaran", "/riwayat", "/profil"]) { await x.get(p); expect(x.path === "/login", p + " terbuka tanpa login"); }
});
await run("WN-03", "transfer sesama bank dengan PIN; saldo kedua akun tepat", async () => {
  const b0 = await bal("budi"), c0 = await bal("citra"), to = (await api("citra", "/accounts/me")).accountNumber;
  await b.get("/transfer?tab=antar");
  await b.post("/transfer?tab=antar", { stage: "confirm", metode: "antar", rekening_tujuan: to, nominal: "15000", catatan: "WN03" });
  expect(/PIN/.test(b.text), "konfirmasi tidak tampil: " + b.text.slice(0, 300));
  await b.post("/transfer?tab=antar", { stage: "verify", pin: "123456" });
  expect(/berhasil/i.test(b.text) && b.path.startsWith("/riwayat"), "transfer tidak berhasil: " + b.text.slice(0, 300));
  const b1 = await bal("budi"), c1 = await bal("citra");
  expect(b0 - b1 === 15000 && c1 - c0 === 15000, `saldo budi ${b0}→${b1}, citra ${c0}→${c1}`);
  return "budi −Rp15.000, citra +Rp15.000";
});
await run("WN-04", "nominal di bawah Rp5.000 dan PIN salah ditolak", async () => {
  const b0 = await bal("budi"), to = (await api("citra", "/accounts/me")).accountNumber;
  await b.get("/transfer?tab=antar");
  await b.post("/transfer?tab=antar", { stage: "confirm", metode: "antar", rekening_tujuan: to, nominal: "1000" });
  expect(/minimal/i.test(b.text), "nominal kecil tidak ditolak");
  await b.post("/transfer?tab=antar", { stage: "confirm", metode: "antar", rekening_tujuan: to, nominal: "20000" });
  await b.post("/transfer?tab=antar", { stage: "verify", pin: "000000" });
  expect(/PIN salah/i.test(b.text), "PIN salah tidak ditolak: " + b.text.slice(0, 200));
  await b.post("/transfer?tab=antar", { stage: "cancel" });
  expect(await bal("budi") === b0, "saldo berubah");
  return "saldo tidak berubah";
});
await run("WN-05", "transfer antarbank ditolak (BRI 9999999999), dana kembali", async () => {
  const b0 = await bal("budi");
  const bri = (await api("budi", "/transfers/banks")).find((x) => /BRI|Rakyat Indonesia/.test(x.name + x.code)).code;
  await b.get("/transfer?tab=bank");
  await b.post("/transfer?tab=bank", { stage: "confirm", metode: "bank_lain", bank_tujuan: bri, rekening_tujuan: "9999999999", nominal: "20000" });
  await b.post("/transfer?tab=bank", { stage: "verify", pin: "123456" });
  expect(/gagal/i.test(b.text), "tidak gagal: " + b.text.slice(0, 250));
  expect(await bal("budi") === b0, "dana tidak kembali");
  return "status Gagal; saldo tetap";
});
await run("WN-06", "transfer antarbank diproses lalu final lewat Cek status (Mandiri 8888888888)", async () => {
  const b0 = await bal("budi");
  const m = (await api("budi", "/transfers/banks")).find((x) => /Mandiri/i.test(x.name)).code;
  await b.get("/transfer?tab=bank");
  await b.post("/transfer?tab=bank", { stage: "confirm", metode: "bank_lain", bank_tujuan: m, rekening_tujuan: "8888888888", nominal: "10000" });
  await b.post("/transfer?tab=bank", { stage: "verify", pin: "123456" });
  expect(/diproses/i.test(b.text), "tidak diproses: " + b.text.slice(0, 250));
  const id = new URL(b.path, WEB).searchParams.get("detail");
  await b.post("/riwayat/cek-status", { id });
  expect(/Status transaksi: Berhasil/i.test(b.text), "status tidak final: " + b.text.slice(0, 250));
  expect(b0 - await bal("budi") === 12500, "debit tidak tepat sekali");
  return "Diproses → Berhasil; debit tepat Rp12.500";
});
for (const [id, title, tab, form, total] of [
  ["WN-07", "pulsa dengan PIN", "pulsa", { phone: "081234567890", nominal: "10000" }, 11500],
  ["WN-08", "token PLN dengan PIN", "pln", { meterNumber: "12345678901", nominal: "20000" }, null],
  ["WN-09", "e-wallet dengan PIN", "ewallet", { phone: "081234567890", nominal: "20000" }, null],
]) {
  await run(id, title, async () => {
    const b0 = await bal("budi");
    await b.get("/pembayaran?tab=" + tab);
    const f = { ...form };
    if (tab === "ewallet") f.wallet = b.html.match(/name="wallet"[\s\S]*?<option[^>]*value="([^"]+)"/)?.[1] || "GOPAY";
    await b.post("/pembayaran?tab=" + tab, { stage: "confirm", ...f });
    expect(/PIN/.test(b.text) && /Total/.test(b.text), "konfirmasi tidak tampil: " + b.text.slice(0, 350));
    await b.post("/pembayaran?tab=" + tab, { stage: "pay", pin: "123456" });
    expect(/berhasil/i.test(b.text), "tidak berhasil: " + b.text.slice(0, 300));
    const d = b0 - await bal("budi");
    expect(d > 0 && (total === null || d === total), "debit " + d);
    return `debit Rp${rp(d)}`;
  });
}
await run("WN-10", "tagihan: pembayaran kedua di periode sama ditolak", async () => {
  const no = "666" + String(Date.now()).slice(-7);
  const pay = async () => {
    await b.get("/pembayaran?tab=tagihan");
    const biller = b.html.match(/name="biller"[\s\S]*?<option[^>]*value="([^"]+)"/)?.[1];
    await b.post("/pembayaran?tab=tagihan", { stage: "confirm", biller, customerNumber: no });
  };
  await pay(); expect(/PIN/.test(b.text), "tagihan tidak tampil: " + b.text.slice(0, 300));
  await b.post("/pembayaran?tab=tagihan", { stage: "pay", pin: "123456" });
  expect(/berhasil/i.test(b.text), "bayar pertama gagal: " + b.text.slice(0, 250));
  await pay(); expect(/lunas|sudah dibayar/i.test(b.text), "bayar kedua tidak ditolak: " + b.text.slice(0, 250));
});
await run("WN-11", "QRIS contoh dibayar dengan PIN", async () => {
  const b0 = await bal("budi");
  const q = await api("budi", "/qris/demo");
  const item = (Array.isArray(q) ? q : q.items)[0];
  await b.get("/pembayaran?tab=qris");
  await b.post("/pembayaran?tab=qris", { stage: "confirm", payload: item.payload ?? item, nominal: "12000" });
  expect(/Merchant/.test(b.text) && /PIN/.test(b.text), "hasil pindai tidak tampil: " + b.text.slice(0, 250));
  await b.post("/pembayaran?tab=qris", { stage: "pay", pin: "123456" });
  expect(/berhasil/i.test(b.text) && await bal("budi") < b0, "QRIS tidak berhasil: " + b.text.slice(0, 250));
});
await run("WN-12", "riwayat, ekspor CSV dan PDF", async () => {
  await b.get("/riwayat"); expect(/WN03|Transfer/.test(b.text), "riwayat kosong");
  const csv = await b.get("/riwayat/export?format=csv"); expect(csv.res.ok && /csv/.test(csv.res.headers.get("content-type")), "CSV gagal");
  const pdf = await b.get("/riwayat/export?format=pdf"); const buf = Buffer.from(await pdf.res.arrayBuffer());
  expect(buf.subarray(0, 4).toString() === "%PDF", "PDF gagal");
  return `PDF ${buf.length} byte`;
});
await run("WN-13", "registrasi → OTP → e-KYC → PIN → Dashboard", async () => {
  const n = new Browser(), u = "wn" + Date.now().toString(36), pw = "Rahasia123";
  await n.get("/register");
  await n.post("/register", { username: u, email: u + "@mail.test", password: pw, password2: pw });
  expect(n.path === "/register/otp", "registrasi: " + n.text.slice(0, 200));
  const code = n.text.match(/\b(\d{6})\b/)?.[1]; expect(code, "OTP demo tidak tampil");
  await n.post("/register/otp", { kode: code }); expect(n.path === "/ekyc", "OTP: " + n.text.slice(0, 200));
  await n.post("/ekyc", { nik: "3374010101010001", fullName: "Uji Web Node", selfie: "s.jpg" });
  for (let i = 0; i < 15 && n.path === "/ekyc"; i++) { await sleep(1000); await n.get("/ekyc"); }
  expect(n.path === "/pin", "e-KYC: " + n.path + " " + n.text.slice(0, 200));
  await n.post("/pin", { pin: "246810", pin2: "246810" }); expect(n.path === "/dashboard", "PIN: " + n.text.slice(0, 200));
  return "saldo awal tampil " + (n.text.match(/Rp ?[\d.]+/)?.[0] || "-");
});
await run("WN-14", "logout semua perangkat lewat API mengakhiri sesi Web", async () => {
  const x = new Browser(); await x.login("citra"); expect(x.path === "/dashboard", "login citra gagal");
  await api("citra", "/auth/logout-all", { method: "POST" });
  await x.get("/riwayat"); expect(x.path === "/login" && /berakhir/i.test(x.text), "sesi Web masih aktif: " + x.path);
  return "kembali ke login dengan pesan sesi berakhir";
});
writeFileSync(path.join(OUT, "hasil.txt"), log.join("\n") + "\n");
console.log(`\n${log.filter((l) => l.startsWith("PASS")).length}/${log.length} PASS`);
