// Uji lintas platform Android ↔ Web (STD-DDB-001 LP-01 s.d. LP-04).
// Prasyarat: backend berjalan (run-local.ps1 -H2), `npm run dev` aktif, emulator dengan APK varian online terpasang.
// Jalankan: node lintas-platform/lp-cross.mjs hasil/lp
import path from "node:path";
import { writeFileSync } from "node:fs";
import * as A from "../lib/adbui.mjs";
import { keypadPin } from "../lib/android-flows.mjs";
import { launch, sleep } from "../lib/cdp.mjs";

const OUT = path.resolve(process.argv[2] || "hasil/lp");
const API = "http://localhost:5173/api/v1";
const log = [];
const step = (id, s, ok, extra = "") => { log.push(`${ok ? "PASS" : "FAIL"}  ${id}  ${s}${extra ? "  — " + extra : ""}`); console.log(log.at(-1)); };
const rp = (n) => "Rp" + new Intl.NumberFormat("id-ID").format(n);

async function api(user, pathName) {
  const t = (await (await fetch(API + "/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ identifier: user, password: "Demo@1234", deviceId: "lp-probe-" + user }) })).json()).accessToken;
  return (await fetch(API + pathName, { headers: { Authorization: "Bearer " + t } })).json();
}

// ---------- Android ----------
async function androidLogin(user = "demo") {
  A.clearApp();
  for (const p of ["ACCESS_LOCAL_NETWORK", "POST_NOTIFICATIONS"]) { try { A.adb("shell", "pm", "grant", A.PKG, "android.permission." + p); } catch { /* */ } }
  A.restartApp();
  await A.waitText(/^Masuk$/, 30000);
  await A.typeInto(/^Username, email/, user);
  await A.typeInto(/^Password$/, "Demo@1234");
  A.back(); await sleep(400);
  await A.tap(/^Masuk$/);
  await keypadPin("123456");
  await A.waitText(/^Saldo rekening utama$/, 20000);
}
async function androidPin() {
  await A.waitText(/^PIN 6 digit$/);
  await A.typeInto(/^PIN 6 digit$/, "123456");
  await A.tap(/^Konfirmasi$/);
}
async function androidHome() {
  for (let i = 0; i < 5 && !A.find(/^Saldo rekening utama$/); i++) { A.back(); await sleep(700); }
  await A.tapIfPresent(/^Beranda$/);
  await A.waitText(/^Saldo rekening utama$/);
}

const web = await launch(OUT);
try {
  // ================= LP-01: transfer di Android → tampil di Web penerima =================
  const amount1 = 10000 + Math.floor(Math.random() * 9000);
  const andiBefore = (await api("andi", "/accounts/me")).balance;
  await androidLogin("demo");
  await A.tap(/^Transfer$/);
  await A.typeInto(/^Nomor rekening$/, "1400000002"); A.back(); await sleep(300);
  await A.tap(/^Cek rekening$/);
  await A.waitText(/^ANDI PRATAMA$/);
  await A.typeInto(/^Nominal transfer$/, String(amount1));
  await A.typeInto(/^Catatan \(opsional\)$/, "LP01-Android"); A.back(); await sleep(300);
  await A.tap(/^Lanjut$/);
  await A.tap(/^Transfer sekarang$/);
  await androidPin();
  await A.waitText(/^Transfer berhasil$/);
  const ref1 = A.dump().map((n) => n.text).find((t) => /^DDB[0-9A-F]{10,}$/.test(t));
  A.screenshot(path.join(OUT, "LP01-1-android-bukti.png"));

  await web.login("andi");
  const webBal = await web.evalJs("document.querySelector('.balance-value').textContent");
  const expected = rp(andiBefore + amount1);
  await web.shot("LP01-2-web-andi-beranda.png");
  await web.go("/riwayat");
  await web.waitFor("document.querySelectorAll('.tx-row').length > 0");
  await web.evalJs("document.querySelector('.tx-row').click()");
  await web.waitFor("document.body.innerText.includes('ID transaksi')");
  const detail = await web.text();
  await web.shot("LP01-3-web-andi-detail.png");
  step("LP-01", "transfer Android (demo) → saldo & riwayat andi di Web", webBal === expected && detail.includes(ref1) && detail.includes(rp(amount1)),
    `nominal ${rp(amount1)}, ref ${ref1}, saldo Web ${webBal} (harapan ${expected})`);

  // ================= LP-02: QRIS dinamis Web (andi) dibayar Android (demo) =================
  const amount2 = 5000 + Math.floor(Math.random() * 4000);
  await web.go("/qris");
  await web.waitFor("!!document.querySelector('#dyn')");
  await web.fill("#dyn", String(amount2));
  await web.clickText("Buat");
  await web.waitFor("document.body.innerText.includes('Nominal tetap')");
  await web.shot("LP02-1-web-andi-qris-dinamis.png");
  // Gambar QR yang dirender Web dipotret, dikirim ke galeri emulator, lalu dipindai Android dari galeri.
  const box = JSON.parse(await web.evalJs("JSON.stringify(document.querySelector('.qr-frame').getBoundingClientRect())"));
  const qrShot = await web.send("Page.captureScreenshot", { format: "png", clip: { x: box.x, y: box.y, width: box.width, height: box.height, scale: 2 } });
  const qrFile = path.join(OUT, "LP02-qr-web.png");
  writeFileSync(qrFile, Buffer.from(qrShot.result.data, "base64"));
  const qrName = `lp02-${Date.now()}.png`;
  A.adb("push", qrFile, `/sdcard/Pictures/${qrName}`);
  try { A.adb("shell", "content", "call", "--uri", "content://media", "--method", "scan_volume", "--arg", "external_primary"); } catch { /* */ }
  await sleep(1500);
  const andiBefore2 = (await api("andi", "/accounts/me")).balance;
  await androidHome();
  await A.tap(/^Bayar QRIS$/);
  await A.tap(/Pilih gambar QR dari galeri/);
  await A.waitText(/^Photo taken on/, 20000);
  const newest = A.dump().find((n) => /^Photo taken on/.test(n.desc || n.text));
  A.tapXY(newest.x, newest.y); await sleep(800);
  await A.tap(/^Done$/);
  await A.waitText(/^Bayar$/, 20000);
  const scanScreen = A.screenText();
  A.screenshot(path.join(OUT, "LP02-2-android-konfirmasi.png"));
  await A.tap(/^Bayar$/);
  await androidPin();
  await A.waitText(/berhasil/i);
  A.screenshot(path.join(OUT, "LP02-3-android-bukti.png"));
  await web.go("/"); await web.waitFor("!!document.querySelector('.balance-value')");
  await sleep(500);
  const webBal2 = await web.evalJs("document.querySelector('.balance-value').textContent");
  await web.shot("LP02-4-web-andi-saldo.png");
  step("LP-02", "QRIS dinamis Web (andi) dibayar Android (demo)", scanScreen.includes("ANDI") && /dinamis/.test(scanScreen) && scanScreen.includes(String(amount2)) && webBal2 === rp(andiBefore2 + amount2),
    `nominal ${rp(amount2)} tampil di Android; saldo andi Web ${webBal2} (harapan ${rp(andiBefore2 + amount2)})`);

  // ================= LP-03: top-up e-wallet di Web (demo) → tampil di Android =================
  await web.login("demo"); // login() keluar dulu dari sesi andi
  const amount3 = 20000 + Math.floor(Math.random() * 9000);
  await web.go("/topup");
  await web.waitFor("document.querySelectorAll('.tabs button').length === 5");
  await web.clickText("E-wallet");
  await web.waitFor("!!document.querySelector('#ea')");
  await web.fill("#ph", "081234567890"); await web.fill("#ea", String(amount3));
  await web.clickText("Lanjut");
  await web.waitFor("document.body.innerText.includes('Konfirmasi pembayaran')");
  await web.clickText("Bayar dengan PIN");
  await web.waitFor("!!document.querySelector('#pin')");
  await web.fill("#pin", "123456"); await web.clickText("Konfirmasi");
  await web.waitFor("document.body.innerText.includes('Bukti pembayaran')");
  const webRcp = await web.text();
  const ref3 = webRcp.match(/DDB[0-9A-F]{10,}/)?.[0];
  const total3 = webRcp.match(/Total\s*\n?\s*(Rp[\d.]+)/)?.[1];
  await web.shot("LP03-1-web-demo-ewallet.png");
  await androidHome();
  await A.tap(/^Riwayat$/);
  await A.waitText(/transaksi ditemukan/);
  await sleep(800);
  const histTxt = A.screenText();
  A.screenshot(path.join(OUT, "LP03-2-android-riwayat.png"));
  step("LP-03", "top-up e-wallet di Web (demo) tampil di riwayat Android", !!total3 && histTxt.includes("−" + total3),
    `ref ${ref3}, total ${total3}`);

  // ================= LP-04: logout semua perangkat dari Android → sesi Web berakhir =================
  await web.go("/"); await web.waitFor("!!document.querySelector('.balance-value')");
  await A.tap(/^Profil$/);
  await A.tap(/^Sesi & perangkat aktif$/);
  await A.waitText(/^Sesi aktif/);
  const sessTxt = A.screenText();
  await A.scrollTo(/^Logout dari semua perangkat$/, 12);
  A.screenshot(path.join(OUT, "LP04-1-android-sesi.png"));
  await A.tap(/^Logout dari semua perangkat$/);
  await sleep(1000);
  await A.tapIfPresent(/^(Ya|Logout|Keluar|Lanjutkan)$/);
  await A.waitText(/^Masuk$/, 20000);
  // Aksi berikutnya di Web. Bila polling notifikasi (30 detik) sudah lebih dulu mengakhiri sesi, pesan sudah
  // tampil dan akan hilang bila halaman dimuat ulang, jadi jangan navigasi lagi.
  if (await web.evalJs("location.pathname !== '/login'")) await web.go("/riwayat");
  await web.waitFor("location.pathname === '/login'", 15000);
  await web.waitFor("/berakhir/i.test(document.body.innerText)", 5000).catch(() => {});
  const webMsg = await web.text();
  await web.shot("LP04-2-web-sesi-berakhir.png");
  step("LP-04", "logout semua perangkat dari Android → Web kembali ke login", sessTxt.includes("Chrome (Web)") && /berakhir/i.test(webMsg),
    `sesi Web terlihat di Android: ${sessTxt.includes("Chrome (Web)")}; pesan Web: ${/berakhir/i.test(webMsg) ? "Sesi Anda telah berakhir" : "-"}`);
} catch (e) {
  step("ERROR", e.message.slice(0, 300), false);
  try { A.screenshot(path.join(OUT, "zz-android-error.png")); await web.shot("zz-web-error.png"); } catch { /* */ }
}
step("—", "tanpa error JavaScript di Web", web.errors.length === 0, web.errors.slice(0, 2).join(" | "));
writeFileSync(path.join(OUT, "hasil.txt"), log.join("\n") + "\n");
await web.close();
process.exit(0);
