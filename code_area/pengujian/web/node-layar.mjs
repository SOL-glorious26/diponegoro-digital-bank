// Tangkapan layar Web tim (Node/Express/EJS) WEB-01 s.d. WEB-11 untuk D07W, D08W, D09W (Chrome headless lewat CDP).
// Prasyarat: backend :8080 dengan data demo bersih, Web Node :3000. Jalankan: node web/node-layar.mjs <folder keluaran>
import path from "node:path";
import { mkdirSync, writeFileSync } from "node:fs";
import { launch, sleep } from "../lib/cdp.mjs";

const OUT = path.resolve(process.argv[2] || "hasil/layar-web");
mkdirSync(OUT, { recursive: true });
const b = await launch(OUT, { port: 9336, base: "http://localhost:3000" });
await b.send("Emulation.setDeviceMetricsOverride", { width: 1280, height: 860, deviceScaleFactor: 1, mobile: false });
const log = {};
const ready = async () => { await sleep(350); await b.waitFor("document.readyState === 'complete'"); };
const go = async (u) => { await b.go(u); await sleep(500); await ready(); };
const shot = async (n, full = false) => {
  await ready();
  const h = full ? await b.evalJs("Math.min(document.documentElement.scrollHeight, 1700)") : 860;
  await b.send("Emulation.setDeviceMetricsOverride", { width: 1280, height: Math.max(860, h), deviceScaleFactor: 1, mobile: false });
  await sleep(250);
  await b.shot(n + ".png");
  log[n] = { path: await b.evalJs("location.pathname + location.search"), text: (await b.text()).replace(/\s+/g, " ").slice(0, 700) };
  console.log(n, log[n].path);
};
const fill = (name, v) => b.evalJs(`(() => { const e = [...document.querySelectorAll('[name="${name}"]')].find((x) => x.type !== 'hidden' && x.type !== 'radio'); if (!e) return false; e.value = ${JSON.stringify(v)}; e.dispatchEvent(new Event('input', { bubbles: true })); e.dispatchEvent(new Event('change', { bubbles: true })); return true; })()`);
const click = async (t) => { const ok = await b.evalJs(`(() => { const e = [...document.querySelectorAll('button')].find((x) => x.textContent.trim().includes(${JSON.stringify(t)})); if (!e) return false; e.click(); return true; })()`); if (!ok) throw new Error("tombol tidak ada: " + t + " di " + (await b.evalJs("location.pathname"))); await sleep(900); await ready(); };
const step = async (n, fn) => { try { await fn(); } catch (e) { console.log("GAGAL " + n + ": " + e.message.slice(0, 250)); await b.shot("zz-" + n + ".png"); } };
const firstOption = (name) => b.evalJs(`(() => { const s = document.querySelector('select[name="${name}"]'); if (!s) return null; const o = [...s.options].find((x) => x.value); s.value = o.value; return o.textContent; })()`);

await step("onboarding", async () => {
  const u = "sinta" + Date.now().toString(36).slice(-4);
  await go("/login"); await shot("WEB-01-masuk");
  await go("/lupa-sandi"); await shot("WEB-11-lupa-sandi");
  await go("/register");
  await fill("username", u); await fill("email", u + "@mail.test"); await fill("no_hp", "081200001111"); await fill("password", "Rahasia123"); await fill("password2", "Rahasia123");
  await shot("WEB-02-daftar");
  await click("Daftar");
  const code = await b.evalJs("(document.body.innerText.match(/\\b\\d{6}\\b/) || [])[0]");
  await fill("kode", code); await shot("WEB-03-otp");
  await click("Verifikasi");
  await fill("nik", "3374010101010001"); await fill("fullName", "Sinta Uji"); await fill("selfie", "selfie.jpg");
  await shot("WEB-04-ekyc");
  await click("Kirim Data e-KYC");
  for (let i = 0; i < 15 && (await b.evalJs("location.pathname")) !== "/pin"; i++) { await sleep(1000); await go("/ekyc"); }
  await fill("pin", "246810"); await fill("pin2", "246810"); await shot("WEB-05-pin");
  await click("Simpan PIN"); await go("/logout");
});
await step("login", async () => {
  await go("/login"); await fill("identitas", "demo"); await fill("password", "Demo@1234"); await click("Login");
  await go("/dashboard"); await shot("WEB-06-dashboard", true);
});
await step("transfer", async () => {
  await go("/transfer?tab=antar");
  await fill("rekening_tujuan", "1400000002"); await fill("nominal", "150000"); await fill("catatan", "Patungan makan");
  await shot("WEB-07-transfer");
  await click("Lanjut ke Konfirmasi"); await shot("WEB-07b-transfer-konfirmasi", true);
  await fill("pin", "123456"); await click("Konfirmasi & Kirim"); await shot("WEB-07c-transfer-hasil", true);
});
await step("antarbank", async () => {
  await go("/transfer?tab=bank");
  await b.evalJs("(() => { const s = document.querySelector('select[name=\"bank_tujuan\"]'); const o = [...s.options].find((x) => /BRI/i.test(x.textContent + x.value)); s.value = o.value; return o.value; })()");
  await fill("rekening_tujuan", "9999999999"); await fill("nominal", "50000"); await click("Lanjut ke Konfirmasi");
  await fill("pin", "123456"); await click("Konfirmasi & Kirim"); await shot("WEB-09b-antarbank-gagal", true);
  await go("/transfer?tab=bank");
  await b.evalJs("(() => { const s = document.querySelector('select[name=\"bank_tujuan\"]'); const o = [...s.options].find((x) => /Mandiri/i.test(x.textContent)); s.value = o.value; return o.value; })()");
  await fill("rekening_tujuan", "8888888888"); await fill("nominal", "50000"); await click("Lanjut ke Konfirmasi");
  await fill("pin", "123456"); await click("Konfirmasi & Kirim"); await shot("WEB-09c-antarbank-diproses", true);
  await click("Cek status"); await shot("WEB-09d-cek-status", true);
});
await step("bayar", async () => {
  await go("/pembayaran?tab=topup"); await shot("WEB-08-topup");
  const before = await b.evalJs("document.body.innerText.match(/Rp ?[\\d.]+/)?.[0]");
  await b.evalJs("(() => { const s = document.querySelector('select[name=\"sumber\"]'); if (s) { s.value = 'Kartu Debit'; return 'select'; } const r = [...document.querySelectorAll('input[name=\"sumber\"]')].find((x) => x.value === 'Kartu Debit'); if (r) r.checked = true; return 'radio'; })()");
  await fill("nominal", "50000"); await click("Top Up Sekarang"); await shot("WEB-08e-topup-hasil"); log.topupBefore = before;
  await go("/pembayaran?tab=pulsa"); await fill("phone", "081234567890");
  await b.evalJs("(() => { const s = document.querySelector('select[name=\"nominal\"]'); if (s) { s.value = [...s.options].find((o) => /10000$/.test(o.value))?.value ?? s.options[1].value; return s.value; } const r = document.querySelector('input[name=\"nominal\"][type=radio]'); if (r) { r.checked = true; return r.value; } })()");
  await shot("WEB-08b-pulsa"); await click("Lanjut"); await shot("WEB-08c-pulsa-konfirmasi", true);
  await fill("pin", "123456"); await click("Bayar"); await shot("WEB-08d-pulsa-hasil", true);
  await go("/pembayaran?tab=pln"); await fill("meterNumber", "123456780000");
  await b.evalJs("(() => { const s = document.querySelector('select[name=\"nominal\"]'); if (s) s.value = [...s.options].find((o) => o.value).value; })()");
  await click("Lanjut"); await shot("WEB-08f-pln-ditolak");
  await go("/pembayaran?tab=qris"); await shot("WEB-08g-qris", true);
});
await step("riwayat", async () => {
  await go("/riwayat"); await shot("WEB-09-riwayat", true);
  await go("/riwayat?jenis=TRANSFER&q=Patungan"); log.filter = (await b.text()).replace(/\s+/g, " ").slice(0, 900);
});
await step("profil", async () => {
  await go("/profil"); await shot("WEB-10-profil", true);
  await fill("email", "demo.baru@diponegoro.bank"); await click("Simpan");
  log.profilOtp = await b.evalJs("location.pathname"); await shot("WEB-03b-otp-profil");
});
await step("tidakvalid", async () => {
  await go("/transfer?tab=antar"); await fill("rekening_tujuan", "1400999999"); await fill("nominal", "20000"); await click("Lanjut ke Konfirmasi"); await shot("WEB-07d-rekening-tidak-valid");
});
writeFileSync(path.join(OUT, "teks.json"), JSON.stringify(log, null, 2));
console.log("error JS:", b.errors.length);
await b.close();
process.exit(0);
