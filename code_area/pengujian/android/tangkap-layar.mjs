// Mengambil tangkapan layar seluruh layar Android (AND-01 s.d. AND-23) untuk D05A, D08, dan D09A.
// Prasyarat sama dengan sa-android.mjs; pakai database demo yang bersih agar gambar rapi.
// Jalankan: node android/tangkap-layar.mjs <folder keluaran> [nama langkah,...]
import path from "node:path";
import { mkdirSync, writeFileSync } from "node:fs";
import * as A from "../lib/adbui.mjs";
import * as F from "../lib/android-flows.mjs";
const { sleep } = A;

const OUT = path.resolve(process.argv[2] || "hasil/layar");
const ONLY = process.argv[3] ? new Set(process.argv[3].split(",")) : null;
mkdirSync(OUT, { recursive: true });
const texts = {};
const shot = async (name, wait = 900) => {
  await sleep(wait);
  A.screenshot(path.join(OUT, name + ".png"));
  texts[name] = A.screenText();
  console.log("  " + name);
};
async function step(name, fn) {
  if (ONLY && !ONLY.has(name)) return;
  console.log(name);
  try { await fn(); } catch (e) { console.log("  GAGAL " + name + ": " + e.message.slice(0, 240)); A.screenshot(path.join(OUT, "zz-gagal-" + name + ".png")); }
}
const firstField = () => A.dump().find((n) => n.editable);
async function fillFirst(v) { const f = firstField(); A.tapXY(f.x, f.y); await sleep(300); A.typeText(v); A.back(); await sleep(400); }

A.adb("shell", "cmd", "uimode", "night", "no"); await sleep(2500);
await step("auth", async () => {
  const u = "rina" + Date.now().toString(36).slice(-4), pw = "Rahasia123";
  F.freshStart();
  await A.waitText(/^Masuk$/, 40000);
  await shot("AND-01-login");
  await A.tap(/^Lupa password atau PIN\?$/);
  await shot("AND-07-pemulihan");
  A.back(); await sleep(900);
  await A.tap(/^Buka rekening$/);
  await A.typeInto(/^Username$/, u); await A.typeInto(/^Email$/, u + "@mail.test"); await A.typeInto(/^Password$/, pw); A.back(); await sleep(400);
  await shot("AND-02-daftar");
  await A.tap(/^Daftar$/);
  await A.waitText(/^Verifikasi OTP$/);
  const code = A.dump().map((n) => n.text).find((t) => /^\d{6}$/.test(t));
  await A.typeInto(/^Kode OTP$/, code); A.back(); await sleep(300);
  await shot("AND-03-otp");
  await A.tap(/^Verifikasi$/);
  await A.waitText(/^Verifikasi e-KYC$/);
  await A.typeInto(/^NIK KTP \(16 digit\)$/, "3374010101010001"); A.back(); await sleep(400);
  const label = await A.waitText(/^Selfie sudah diambil \(simulasi\)$/);
  A.tapXY(Math.max(10, label.x1 - 70), label.y); await sleep(600);
  await shot("AND-04-ekyc");
  await A.tap(/^Kirim KTP & Selfie$/);
  await A.waitText(/^Lanjut ke Login$/, 25000);
  await shot("AND-04b-ekyc-disetujui");
  await A.tap(/^Lanjut ke Login$/);
  await A.waitText(/^Masuk$/);
  await A.typeInto(/^Username, email/, u); await A.typeInto(/^Password$/, pw); A.back(); await sleep(300);
  await A.tap(/^Masuk$/);
  await A.waitText(/^Buat PIN Transaksi$/, 20000);
  const fs = A.dump().filter((n) => n.editable);
  for (const f of fs.slice(0, 2)) { A.tapXY(f.x, f.y); await sleep(300); A.typeText("246810"); await sleep(300); }
  A.back(); await sleep(300);
  await shot("AND-05-buat-pin");
  await A.tap(/^Simpan PIN$/);
  await A.waitText(/^Masukkan PIN$/);
  await A.tap(/^2$/); await A.tap(/^4$/);
  await shot("AND-06-masukkan-pin");
  for (const d of "6810") await A.tap(new RegExp(`^${d}$`));
  await A.waitText(/^Saldo rekening utama$/, 25000);
});

await step("beranda", async () => {
  await F.login();
  await shot("AND-08-beranda", 1800);
  await A.tap(/^Sembunyikan saldo$/);
  await shot("AND-08b-saldo-tersembunyi");
  await A.tap(/^Tampilkan saldo$/);
});

await step("transfer", async () => {
  await F.home();
  await A.tap(/^Transfer$/);
  await A.typeInto(/^Nomor rekening$/, "1400000002"); A.back(); await sleep(300);
  await shot("AND-11-transfer-penerima");
  await A.tap(/^Cek rekening$/);
  await A.waitText(/^Nominal transfer$/);
  await A.typeInto(/^Nominal transfer$/, "150000"); await A.typeInto(/^Catatan \(opsional\)$/, "Patungan makan"); A.back(); await sleep(300);
  await shot("AND-11b-transfer-nominal");
  await A.tap(/^Lanjut$/);
  await A.waitText(/^Transfer sekarang$/);
  await shot("AND-11c-transfer-konfirmasi");
  await A.tap(/^Transfer sekarang$/);
  await A.waitText(/^PIN 6 digit$/);
  await shot("AND-11d-transfer-pin");
  await F.pin();
  await A.waitText(/^Transfer berhasil$/);
  await shot("AND-11e-transfer-bukti");
});

await step("qris", async () => {
  await F.home();
  await A.tap(/^Bayar QRIS$/);
  await A.waitText(/Pilih gambar QR dari galeri/);
  await shot("AND-12-bayar-qris");
  await A.tap(/Kopi Senja Tembalang/);
  await A.waitText(/^Bayar$/);
  await shot("AND-12b-qris-konfirmasi");
  await A.tap(/^Bayar$/);
  await F.pin();
  await A.waitText(/berhasil/i, 20000);
  await shot("AND-12c-qris-bukti");
  await F.home();
  await A.tap(/^Terima QRIS$/);
  await shot("AND-13-terima-qris", 1800);
});

await step("topup", async () => {
  await F.home();
  await A.tap(/^Top-up$/);
  await A.typeInto(/^Nomor HP$/, "081234567890"); A.back(); await sleep(500);
  await shot("AND-14-topup");
  await A.tap(/^10\.000$/);
  await A.waitText(/^Konfirmasi Pulsa$/);
  await shot("AND-14b-topup-konfirmasi");
  await A.tap(/^Bayar$/);
  await F.pin();
  await A.waitText(/berhasil/i, 20000);
  await shot("AND-14c-topup-bukti");
  await F.home();
  await A.tap(/^Top-up$/);
  await A.tap(/^Tagihan$/); await sleep(800);
  await fillFirst("5551234567");
  await A.tap(/^Cek tagihan$/);
  await A.waitText(/^Lanjut bayar$/);
  await shot("AND-14d-tagihan");
});

await step("kantong", async () => {
  await F.home();
  await A.tap(/^Kantong$/);
  await A.tap(/Buat kantong/);
  await A.typeInto(/^Nama kantong$/, "Dana Liburan"); A.back(); await sleep(300);
  await shot("AND-10b-buat-kantong");
  await A.tap(/^1 bln$/);
  await A.tap(/^Buat$/);
  await A.waitText(/Dana Liburan/);
  await A.tap(/Dana Liburan/); await sleep(1200);
  await A.tap(/^Isi$/); await sleep(800);
  const f = firstField(); A.tapXY(f.x, f.y); await sleep(300); A.typeText("500000"); A.back(); await sleep(300);
  await A.tap(/^Pindahkan$/); await sleep(2500);
  await shot("AND-10-kantong");
  await F.home();
  await A.tap(/^Grafik Saldo$/);
  await shot("AND-09-grafik-saldo", 1800);
});

await step("kartu", async () => {
  await F.home();
  await A.tap(/^Kartu$/);
  await A.tap(/Buat kartu virtual/);
  await A.waitText(/^Domestik$/);
  await shot("AND-15b-buat-kartu");
  await A.tap(/^Domestik$/);
  await A.tap(/^Buat kartu$/);
  await sleep(2500);
  await shot("AND-15-kartu");
  await A.tap(/^Lihat detail$/);
  await F.pin();
  await A.waitText(/^Sembunyikan$/);
  await shot("AND-15c-kartu-detail", 1500);
});

await step("riwayat", async () => {
  await F.home();
  await A.tap(/^Riwayat$/);
  await A.waitText(/transaksi ditemukan/);
  await shot("AND-16-riwayat", 1500);
  await A.tap(/^Filter jenis & status$/);
  await shot("AND-16b-riwayat-filter");
  await A.tap(/^Sembunyikan filter lain$/);
  const first = A.dump().find((n) => /^[−]Rp/.test(n.text));
  A.tapXY(first.x - 300, first.y);
  await shot("AND-17-detail", 1800);
  A.back(); await sleep(900);
  await A.tap(/^Analitik$/);
  await shot("AND-18-analitik", 1800);
});

await step("profil", async () => {
  await F.home();
  await A.tap(/^Profil$/);
  await shot("AND-19-profil", 1500);
  await A.tap(/^Ubah data$/);
  await shot("AND-19b-ubah-data");
  A.back(); await sleep(700); A.back(); await sleep(700);
  await F.home(); await A.tap(/^Profil$/);
  await A.tap(/^Keamanan/);
  await shot("AND-20-keamanan", 1500);
  await F.home(); await A.tap(/^Profil$/);
  await A.tap(/^Sesi & perangkat aktif$/);
  await shot("AND-21-sesi", 1800);
  await F.home(); await A.tap(/^Profil$/);
  await A.tap(/^Pengaturan notifikasi$/);
  await shot("AND-22-pengaturan-notifikasi", 1500);
  await F.home();
  await A.tap(/^Notifikasi/);
  await shot("AND-23-notifikasi", 1500);
});

await step("gelap", async () => {
  A.adb("shell", "cmd", "uimode", "night", "yes"); await sleep(2500);
  await F.home();
  await shot("GELAP-beranda", 1500);
  A.adb("shell", "cmd", "uimode", "night", "no");
});

await step("keluar", async () => {
  await F.home(); await A.tap(/^Profil$/);
  await A.scrollTo(/^Keluar$/);
  await shot("AND-19c-profil-bawah");
});

writeFileSync(path.join(OUT, "teks.json"), JSON.stringify(texts, null, 2));
