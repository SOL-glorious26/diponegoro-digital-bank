// Uji lintas platform Android <-> Web tim (Node/Express/EJS): LPN-01, LPN-02, LPN-03 (padanan LP-01, LP-03, LP-04).
// LP-02 (QRIS dinamis di Web dibayar Android) tidak ada padanannya: Web Node belum menampilkan QRIS pribadi/dinamis.
// Prasyarat: backend :8080, Web Node :3000, emulator dengan APK varian online.
// Jalankan: node lintas-platform/lp-node.mjs hasil/lp-node
import path from "node:path";
import { mkdirSync, writeFileSync } from "node:fs";
import * as A from "../lib/adbui.mjs";
import * as F from "../lib/android-flows.mjs";
import { Browser } from "../lib/web-node.mjs";
const { sleep } = A;
const OUT = path.resolve(process.argv[2] || "hasil/lp-node");
mkdirSync(OUT, { recursive: true });
const log = [];
const rp = (n) => new Intl.NumberFormat("id-ID").format(n);
const expect = (c, m) => { if (!c) throw new Error(m); };
async function run(id, t, fn) {
  try { const n = await fn(); log.push(`PASS  ${id}  ${t}${n ? "  — " + n : ""}`); }
  catch (e) { log.push(`FAIL  ${id}  ${t}  — ${e.message.replace(/\s+/g, " ").slice(0, 300)}`); try { A.screenshot(path.join(OUT, id + "-gagal.png")); } catch { /* */ } }
  console.log(log.at(-1));
}
const saldo = (b) => Number((b.text.match(/Rp ?([\d.]+)/)?.[1] || "0").replace(/\./g, ""));
const andi = new Browser(), demo = new Browser();

await run("LPN-01", "transfer Android (demo) tampil di Web (andi)", async () => {
  const amt = 11000 + Math.floor(Math.random() * 8000);
  await andi.login("andi"); const s0 = saldo(andi);
  await F.login("demo");
  await A.tap(/^Transfer$/);
  await A.typeInto(/^Nomor rekening$/, "1400000002"); A.back(); await sleep(300);
  await A.tap(/^Cek rekening$/); await A.waitText(/^ANDI PRATAMA$/);
  await A.typeInto(/^Nominal transfer$/, String(amt)); A.back(); await sleep(300);
  await A.tap(/^Lanjut$/); await A.tap(/^Transfer sekarang$/); await F.pin(); await A.waitText(/^Transfer berhasil$/);
  A.screenshot(path.join(OUT, "LPN01-android-bukti.png"));
  await andi.get("/dashboard"); const s1 = saldo(andi);
  expect(s1 - s0 === amt, `saldo Web andi ${s0} -> ${s1}, harapan +${amt}`);
  await andi.get("/riwayat"); expect(andi.text.includes(rp(amt)), "transaksi tidak ada di riwayat Web");
  return `Rp${rp(amt)}; saldo Web ${rp(s0)} -> ${rp(s1)}`;
});
await run("LPN-02", "e-wallet di Web (demo) tampil di riwayat Android", async () => {
  const amt = 20000 + Math.floor(Math.random() * 9000);
  await demo.login("demo"); await demo.get("/pembayaran?tab=ewallet");
  const wallet = demo.html.match(/name="wallet"[\s\S]*?<option[^>]*value="([^"]+)"/)?.[1] || "GOPAY";
  await demo.post("/pembayaran?tab=ewallet", { stage: "confirm", wallet, phone: "081234567890", nominal: String(amt) });
  const total = demo.text.match(/Total\s+Rp ?([\d.]+)/)?.[1]; expect(total, "konfirmasi tidak tampil: " + demo.text.slice(0, 200));
  await demo.post("/pembayaran?tab=ewallet", { stage: "pay", pin: "123456" }); expect(/berhasil/i.test(demo.text), "bayar gagal");
  await F.home(); await A.tap(/^Riwayat$/); await A.waitText(/transaksi ditemukan/); await sleep(1500);
  A.screenshot(path.join(OUT, "LPN02-android-riwayat.png"));
  expect(A.screenText().includes("\u2212Rp" + total), "total tidak ada di riwayat Android");
  return `total Rp${total}`;
});
await run("LPN-03", "logout semua perangkat dari Android mengakhiri sesi Web", async () => {
  await demo.get("/dashboard"); expect(demo.path === "/dashboard", "sesi Web demo tidak aktif");
  await A.tap(/^Profil$/); await A.tap(/^Sesi & perangkat aktif$/); await A.waitText(/^Sesi aktif/);
  const seen = /Web diponegoro corp bank/.test(A.screenText());
  A.screenshot(path.join(OUT, "LPN03-android-sesi.png"));
  await A.scrollTo(/^Logout dari semua perangkat$/, 12); await A.tap(/^Logout dari semua perangkat$/); await sleep(1000);
  await A.tapIfPresent(/^(Ya|Logout|Keluar|Lanjutkan)$/); await A.waitText(/^Masuk$/, 20000);
  await demo.get("/riwayat");
  expect(demo.path === "/login" && /berakhir/i.test(demo.text), "sesi Web masih aktif: " + demo.path);
  return `sesi Web terlihat di Android: ${seen}; Web kembali ke login dengan pesan sesi berakhir`;
});
writeFileSync(path.join(OUT, "hasil.txt"), log.join("\n") + "\n");
console.log(`${log.filter((l) => l.startsWith("PASS")).length}/${log.length} PASS`);
