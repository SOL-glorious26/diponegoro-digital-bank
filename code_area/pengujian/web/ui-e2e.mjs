// Uji UI end-to-end aplikasi Web memakai Chrome headless + Chrome DevTools Protocol (tanpa pustaka tambahan).
import { spawn, spawnSync } from "node:child_process";
import { mkdirSync, writeFileSync, rmSync } from "node:fs";
import path from "node:path";

const OUT = path.resolve(process.argv[2] || "hasil/web-ui");
const CHROME = "C:/Program Files/Google/Chrome/Application/chrome.exe";
const PROFILE = path.join((await import("node:os")).tmpdir(), "ddb-web-test-chrome-9333");
mkdirSync(OUT, { recursive: true });
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

spawnSync("powershell", ["-NoProfile", "-Command", "Get-CimInstance Win32_Process -Filter \"Name='chrome.exe'\" | Where-Object { $_.CommandLine -like '*remote-debugging-port=9333*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }"]);
const proc = spawn(CHROME, ["--headless=new", "--remote-debugging-port=9333", `--user-data-dir=${PROFILE}`, "--no-first-run", "--window-size=1366,900", "about:blank"], { stdio: "ignore" });
let wsUrl;
for (let i = 0; i < 40 && !wsUrl; i++) {
  try { const t = await (await fetch("http://127.0.0.1:9333/json")).json(); wsUrl = t.find((x) => x.type === "page")?.webSocketDebuggerUrl; } catch { await sleep(250); }
}
const ws = new WebSocket(wsUrl);
await new Promise((r) => ws.addEventListener("open", r));
let id = 0; const pending = new Map(); const consoleErrors = [];
ws.addEventListener("message", (ev) => {
  const m = JSON.parse(ev.data);
  if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); }
  if (m.method === "Runtime.exceptionThrown") consoleErrors.push(m.params.exceptionDetails?.exception?.description || m.params.exceptionDetails?.text);
  if (m.method === "Runtime.consoleAPICalled" && m.params.type === "error") consoleErrors.push(m.params.args.map((a) => a.value ?? a.description).join(" "));
});
const send = (method, params = {}) => new Promise((r) => { const i = ++id; pending.set(i, r); ws.send(JSON.stringify({ id: i, method, params })); });
const evalJs = async (expr) => (await send("Runtime.evaluate", { expression: expr, awaitPromise: true, returnByValue: true })).result?.result?.value;
const waitFor = async (expr, ms = 10000) => { const t = Date.now(); while (Date.now() - t < ms) { if (await evalJs(expr)) return true; await sleep(150); } throw new Error("timeout: " + expr); };
const shot = async (name) => { const r = await send("Page.captureScreenshot", { format: "png", captureBeyondViewport: false }); writeFileSync(path.join(OUT, name + ".png"), Buffer.from(r.result.data, "base64")); };
// Isi input React: pakai setter asli agar onChange terpanggil.
const fill = (sel, val) => evalJs(`(() => { const el = document.querySelector(${JSON.stringify(sel)}); const p = el.tagName === 'SELECT' ? HTMLSelectElement.prototype : HTMLInputElement.prototype; Object.getOwnPropertyDescriptor(p, 'value').set.call(el, ${JSON.stringify(val)}); el.dispatchEvent(new Event(el.tagName === 'SELECT' ? 'change' : 'input', { bubbles: true })); return true; })()`);
const clickText = (text, tag = "button") => evalJs(`(() => { const el = [...document.querySelectorAll(${JSON.stringify(tag)})].find(b => b.textContent.trim().includes(${JSON.stringify(text)}) && !b.disabled); if (!el) return false; el.click(); return true; })()`);
const text = () => evalJs("document.body.innerText");
const log = [];
const step = (s, ok, extra = "") => { log.push(`${ok ? "PASS" : "FAIL"}  ${s}${extra ? "  — " + extra : ""}`); };

await send("Page.enable"); await send("Runtime.enable");
const go = (url) => send("Page.navigate", { url: "http://localhost:5173" + url });

try {
  // 1. Halaman yang dilindungi mengarahkan ke login
  await go("/transfer");
  await waitFor("location.pathname === '/login' && !!document.querySelector('#id')");
  step("route guard: /transfer tanpa login → /login", true);
  await shot("01-login");

  // 2. Login demo
  await fill("#id", "demo"); await fill("#pw", "Demo@1234");
  await clickText("Masuk");
  await waitFor("location.pathname === '/transfer' && document.body.innerText.includes('Bank tujuan')");
  step("login → kembali ke halaman yang diminta (/transfer)", true);

  // 3. Beranda
  await go("/");
  await waitFor("document.body.innerText.includes('Saldo rekening utama') && document.body.innerText.includes('1400123456')");
  await waitFor("document.querySelectorAll('.tx-row').length > 0");
  await sleep(400);
  const saldo1 = await evalJs("document.querySelector('.balance-value').textContent");
  step("beranda: saldo, rekening, transaksi terakhir", true, saldo1);
  await shot("02-beranda");

  // 4. Muat ulang: sesi dipulihkan dari refresh token di sessionStorage
  await send("Page.reload");
  await waitFor("document.body.innerText.includes('Saldo rekening utama')");
  step("refresh halaman: sesi tetap (refresh token sessionStorage)", true);

  // 5. Transfer sesama bank
  await go("/transfer");
  await waitFor("!!document.querySelector('#acc') && document.querySelectorAll('#bank option').length > 0");
  await fill("#acc", "1400000002"); await fill("#amt", "12500"); await fill("#note", "uji UI web");
  await clickText("Lanjut");
  await waitFor("document.body.innerText.includes('Konfirmasi transfer')");
  const conf = await text();
  step("konfirmasi: nama penerima tampil sebelum PIN", conf.includes("ANDI PRATAMA"));
  await shot("03-transfer-konfirmasi");
  await clickText("Lanjut ke PIN");
  await waitFor("!!document.querySelector('#pin')");
  await fill("#pin", "123456");
  await shot("04-pin");
  await clickText("Konfirmasi");
  await waitFor("document.body.innerText.includes('Bukti transfer')");
  const rcp = await text();
  step("transfer berhasil + bukti", rcp.includes("Berhasil") || rcp.includes("SUCCESS"), rcp.match(/Rp12\.500/) ? "Rp12.500" : "");
  await shot("05-bukti-transfer");

  // 6. Riwayat + filter + detail
  await go("/riwayat");
  await waitFor("document.querySelectorAll('.tx-row').length > 0");
  await fill("select[aria-label='Arah']", "OUT");
  await sleep(800);
  const firstTitle = await evalJs("document.querySelector('.tx-row .tx-title')?.textContent");
  step("riwayat: filter Keluar, transaksi terbaru tampil", /Transfer/.test(firstTitle || ""), firstTitle);
  await shot("06-riwayat");
  await evalJs("document.querySelector('.tx-row').click()");
  await waitFor("document.body.innerText.includes('Detail transaksi') && document.body.innerText.includes('Jurnal ledger')");
  step("detail transaksi + jurnal ledger", true);
  await shot("07-detail");
  await evalJs("document.querySelector('.modal-head .icon-btn').click()");

  // 7. Top-up PLN tidak ditemukan
  await go("/topup");
  await waitFor("document.querySelectorAll('.tabs button').length === 5");
  await clickText("Token PLN");
  await waitFor("!!document.querySelector('#mt')");
  await fill("#mt", "123456780000");
  await clickText("Cek");
  await waitFor("document.body.innerText.includes('tidak ditemukan')");
  step("top-up PLN: ID berakhiran 0000 → pesan tidak ditemukan", true);
  await shot("08-topup-pln");

  // 8. QRIS pribadi
  await go("/qris");
  await waitFor("!!document.querySelector('.qr-frame svg')");
  step("QRIS pribadi tampil", true);
  await shot("09-qris");

  // 9. Profil & sesi
  await go("/profil");
  await waitFor("document.body.innerText.includes('Sesi aktif') && document.body.innerText.includes('Perangkat ini')");
  step("profil, pengaturan notifikasi, sesi aktif", true);
  await shot("10-profil");

  // 10. Tampilan ponsel
  await send("Emulation.setDeviceMetricsOverride", { width: 390, height: 844, deviceScaleFactor: 2, mobile: true });
  await go("/");
  await waitFor("document.body.innerText.includes('Saldo rekening utama')");
  await sleep(500);
  const overflow = await evalJs("document.documentElement.scrollWidth > window.innerWidth + 1");
  step("tampilan ponsel 390px tanpa scroll horizontal", !overflow);
  await shot("11-ponsel-beranda");
  await send("Emulation.clearDeviceMetricsOverride");

  // WB-04: sembunyikan saldo tersimpan di server (bertahan setelah muat ulang)
  await go("/");
  await waitFor("!!document.querySelector('.balance-value')");
  await evalJs("document.querySelector('.balance-top .icon-btn').click()");
  await waitFor("document.querySelector('.balance-value').textContent.includes('•')");
  await send("Page.reload");
  await waitFor("!!document.querySelector('.balance-value')");
  const hiddenAfter = await evalJs("document.querySelector('.balance-value').textContent.includes('•')");
  step("WB-04 sembunyikan saldo bertahan setelah muat ulang", hiddenAfter);
  await evalJs("document.querySelector('.balance-top .icon-btn').click()");
  await waitFor("!document.querySelector('.balance-value').textContent.includes('•')");

  // WB-06: antarbank ditolak → gagal, dana dikembalikan
  await go("/transfer");
  await waitFor("document.querySelectorAll('#bank option').length > 1");
  const bri = await evalJs("[...document.querySelectorAll('#bank option')].find(o => /BRI/i.test(o.textContent))?.value");
  await fill("#bank", bri); await fill("#acc", "9999999999"); await fill("#amt", "10000");
  await clickText("Lanjut");
  await waitFor("document.body.innerText.includes('Konfirmasi transfer')");
  await clickText("Lanjut ke PIN");
  await waitFor("!!document.querySelector('#pin')");
  await fill("#pin", "123456"); await clickText("Konfirmasi");
  await waitFor("document.body.innerText.includes('Bukti transfer')");
  const failTxt = await text();
  step("WB-06 antarbank ditolak → Gagal, pesan dana dikembalikan", /Gagal/.test(failTxt) && /dikembalikan/.test(failTxt));
  await shot("12-transfer-ditolak");

  // WB-08: ekspor CSV dan PDF terunduh
  await send("Browser.setDownloadBehavior", { behavior: "allow", downloadPath: path.join(OUT, "dl").replace(/\//g, "\\") });
  await go("/riwayat");
  await waitFor("document.querySelectorAll('.tx-row').length > 0");
  await clickText("CSV"); await sleep(1500); await clickText("PDF"); await sleep(2000);
  const { readdirSync } = await import("node:fs");
  let files = []; try { files = readdirSync(path.join(OUT, "dl")); } catch { /* */ }
  step("WB-08 ekspor CSV dan PDF terunduh", files.some((f) => f.endsWith(".csv")) && files.some((f) => f.endsWith(".pdf")), files.join(", "));

  // WB-10: beli pulsa
  await go("/topup");
  await waitFor("!!document.querySelector('#ph')");
  await fill("#ph", "081234567890");
  await evalJs("document.querySelector('.chip').click()");
  await clickText("Lanjut");
  await waitFor("document.body.innerText.includes('Konfirmasi pembayaran')");
  await clickText("Bayar dengan PIN");
  await waitFor("!!document.querySelector('#pin')");
  await fill("#pin", "123456"); await clickText("Konfirmasi");
  await waitFor("document.body.innerText.includes('Bukti pembayaran')");
  step("WB-10 pulsa berhasil dibayar dengan PIN", /Berhasil/.test(await text()));
  await shot("13-bukti-pulsa");

  // Logout
  await go("/");
  await waitFor("!!document.querySelector('.nav-logout')");
  await evalJs("document.querySelector('.nav-logout').click()");
  await waitFor("location.pathname === '/login'");
  step("logout → login", true);

  // WB-02 + WB-03: registrasi → OTP → (guard e-KYC) → e-KYC → PIN → Beranda
  const uname = "webui" + Date.now().toString(36);
  await go("/daftar");
  await waitFor("!!document.querySelector('#u')");
  await fill("#u", uname); await fill("#e", uname + "@mail.test"); await fill("#pw", "Rahasia123"); await fill("#pw2", "Rahasia123");
  await clickText("Daftar");
  await waitFor("!!document.querySelector('#otp')");
  const otpCode = await evalJs("document.querySelector('.alert-info strong')?.textContent");
  await fill("#otp", otpCode); await clickText("Verifikasi");
  await waitFor("location.pathname === '/ekyc'");
  await go("/transfer");
  await waitFor("location.pathname === '/ekyc'");
  step("WB-03 akun belum e-KYC membuka /transfer → diarahkan ke /ekyc", true);
  await shot("14-ekyc");
  await fill("#nik", "3374010101010001"); await fill("#fn", "Uji Web UI");
  // unggah berkas swafoto palsu lewat DataTransfer
  await evalJs(`(() => { const dt = new DataTransfer(); dt.items.add(new File([new Uint8Array([137,80,78,71])], 'selfie.png', { type: 'image/png' })); const el = document.querySelector('#sf'); el.files = dt.files; el.dispatchEvent(new Event('change', { bubbles: true })); return true; })()`);
  await clickText("Kirim data e-KYC");
  await waitFor("location.pathname === '/pin/buat'", 15000);
  await fill("#p1", "135790"); await fill("#p2", "135790");
  await clickText("Simpan PIN");
  await waitFor("location.pathname === '/' && document.body.innerText.includes('Saldo rekening utama')", 10000);
  step("WB-02 registrasi → OTP → e-KYC → PIN → Beranda", true, uname);
  await shot("15-pengguna-baru");
} catch (e) {
  step("ERROR", false, e.message);
  await shot("zz-error");
  log.push("Halaman saat error:\n" + (await text())?.slice(0, 800));
}
step("tanpa error JavaScript di konsol", consoleErrors.length === 0, consoleErrors.slice(0, 3).join(" | "));
console.log(log.join("\n"));
ws.close(); (await import("node:child_process")).spawnSync("taskkill", ["/PID", String(proc.pid), "/T", "/F"]);
await sleep(500);
try { rmSync(PROFILE, { recursive: true, force: true }); } catch { /* */ }
process.exit(0);
