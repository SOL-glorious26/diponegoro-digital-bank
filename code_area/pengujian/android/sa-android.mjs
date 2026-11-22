// Uji sistem Android di emulator (STD-DDB-001 Tabel 5.3, SA-xx), varian online terhadap backend lokal.
// Prasyarat: backend berjalan (port 8080), emulator dengan APK varian online terpasang.
// Jalankan: node android/sa-android.mjs hasil/sa [SA-03,SA-05,...]   (tanpa daftar = semua)
// SA-18 (diam > 5 menit) dan SA-20 (backend dimatikan) hanya dijalankan bila disebut eksplisit.
import path from "node:path";
import { writeFileSync, mkdirSync } from "node:fs";
import * as A from "../lib/adbui.mjs";
import * as F from "../lib/android-flows.mjs";
const { sleep } = A;

const OUT = path.resolve(process.argv[2] || "hasil/sa");
const ONLY = process.argv[3] ? new Set(process.argv[3].split(",")) : null;
const API = process.env.DDB_API_URL || "http://localhost:8080/api/v1";
mkdirSync(OUT, { recursive: true });
const log = [];
const rp = (n) => "Rp" + new Intl.NumberFormat("id-ID").format(n);
const shot = (name) => A.screenshot(path.join(OUT, name + ".png"));

async function api(user, method, p, body, extraHeaders = {}) {
  const tok = (await (await fetch(API + "/auth/login", { method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ identifier: user, password: "Demo@1234", deviceId: "sa-probe-" + user }) })).json()).accessToken;
  const r = await fetch(API + p, { method, headers: { "Content-Type": "application/json", Authorization: "Bearer " + tok, ...extraHeaders }, body: body ? JSON.stringify(body) : undefined });
  return r.headers.get("content-type")?.includes("json") ? r.json() : r.text();
}
const balanceOf = async (user) => (await api(user, "GET", "/accounts/me")).balance;

async function run(id, title, fn, { explicit = false } = {}) {
  if (ONLY ? !ONLY.has(id) : explicit) return;
  const t0 = Date.now();
  try {
    const note = await fn();
    log.push(`PASS  ${id}  ${title}${note ? "  — " + note : ""}`);
  } catch (e) {
    log.push(`FAIL  ${id}  ${title}  — ${e.message.replace(/\s+/g, " ").slice(0, 260)}`);
    try { shot(id + "-gagal"); } catch { /* */ }
  }
  console.log(log.at(-1), `(${Math.round((Date.now() - t0) / 1000)} dtk)`);
}
const expect = (cond, msg) => { if (!cond) throw new Error(msg); };

async function transferForm(bankRe, account, amount, note) {
  await F.home();
  await A.tap(/^Transfer$/);
  if (bankRe) { await A.tap(/^Diponegoro Digital Bank$/); await A.tap(bankRe); }
  await A.typeInto(/^Nomor rekening$/, account); A.back(); await sleep(300);
  await A.tap(/^Cek rekening$/);
  await A.waitText(/^Nominal transfer$/);
  await A.typeInto(/^Nominal transfer$/, String(amount));
  if (note) await A.typeInto(/^Catatan \(opsional\)$/, note);
  A.back(); await sleep(300);
  await A.tap(/^Lanjut$/);
}


// Pengguna baru siap transaksi lewat API (registrasi → OTP → e-KYC → PIN), agar kasus tidak bergantung pada data akun demo.
async function apiNewUser(pin = "135790") {
  const u = "sx" + Date.now().toString(36), pw = "Rahasia123";
  const post = (p, b, t) => fetch(API + p, { method: "POST", headers: { "Content-Type": "application/json", ...(t ? { Authorization: "Bearer " + t } : {}) }, body: JSON.stringify(b) }).then((r) => r.json());
  const reg = await post("/auth/register", { username: u, email: u + "@mail.test", password: pw });
  const tok = (await post("/auth/register/verify-otp", { userId: reg.userId, code: reg.otp.demoCode, deviceId: "sa-setup" })).accessToken;
  await post("/auth/ekyc", { nik: "3374010101010001", selfieReference: "s.jpg" }, tok);
  for (let i = 0; i < 20; i++) { const e = await fetch(API + "/auth/ekyc", { headers: { Authorization: "Bearer " + tok } }).then((r) => r.json()); if (e.status === "APPROVED") break; await sleep(500); }
  await post("/auth/pin", { pin }, tok);
  return { u, pw, pin };
}

// Kotak centang swafoto hanya merespons ketukan pada kotaknya (di kiri label).
async function tickSelfie() {
  const label = await A.waitText(/^Selfie sudah diambil \(simulasi\)$/);
  A.tapXY(Math.max(10, label.x1 - 70), label.y); await sleep(600);
}
const isRejection = (t) => /ditolak/i.test(t) && !/akan ditolak/i.test(t);
// =============================================================================
let newUser = null;
const pwNew = "Rahasia123";

await run("SA-21", "registrasi → OTP → e-KYC → PIN sampai transaksi aktif (+ tombol Kembali setelah OTP, DEF-07)", async () => {
  newUser = "sa" + Date.now().toString(36);
  F.freshStart();
  await A.waitText(/^Masuk$/, 30000);
  await A.tap(/^Buka rekening$/);
  await A.typeInto(/^Username$/, newUser);
  await A.typeInto(/^Email$/, newUser + "@mail.test");
  await A.typeInto(/^Password$/, pwNew); A.back(); await sleep(300);
  await A.tap(/^Daftar$/);
  await A.waitText(/^Verifikasi OTP$/);
  const code = A.dump().map((n) => n.text).find((t) => /^\d{6}$/.test(t));
  await A.typeInto(/^Kode OTP$/, code); A.back(); await sleep(300);
  await A.tap(/^Verifikasi$/);
  await A.waitText(/^Verifikasi e-KYC$/);
  // DEF-07: Kembali dari e-KYC tidak boleh membawa ke layar OTP yang sudah selesai
  A.back(); await sleep(1200);
  const afterBack = A.screenText();
  expect(!/Verifikasi OTP/.test(afterBack), "Kembali dari e-KYC masih menuju layar OTP");
  if (!/Verifikasi e-KYC/.test(afterBack)) { A.restartApp(); await sleep(2500); }
  if (!A.find(/^Verifikasi e-KYC$/)) {
    // aplikasi tertutup oleh tombol Kembali: masuk lagi, alur harus berlanjut ke e-KYC
    await A.waitText(/^Masuk$/, 20000);
    await A.typeInto(/^Username, email/, newUser); await A.typeInto(/^Password$/, pwNew); A.back(); await sleep(300);
    await A.tap(/^Masuk$/);
    await A.waitText(/^Verifikasi e-KYC$/);
  }
  await A.typeInto(/^NIK KTP \(16 digit\)$/, "3374010101010001"); A.back(); await sleep(400);
  await tickSelfie();
  await A.tap(/^Kirim KTP & Selfie$/);
  await A.waitText(/^Lanjut ke Login$/, 25000);
  shot("SA21-1-ekyc-disetujui");
  await A.tap(/^Lanjut ke Login$/);
  await A.waitText(/^Masuk$/);
  await A.typeInto(/^Username, email/, newUser); await A.typeInto(/^Password$/, pwNew); A.back(); await sleep(300);
  await A.tap(/^Masuk$/);
  await A.waitText(/^Buat PIN Transaksi$/, 20000);
  const fields = A.dump().filter((n) => n.editable);
  expect(fields.length >= 2, "layar buat PIN tidak muncul: " + A.screenText().slice(0, 200));
  for (const f of fields.slice(0, 2)) { A.tapXY(f.x, f.y); await sleep(300); A.typeText("246810"); await sleep(300); }
  A.back(); await sleep(300);
  await A.tap(/^(Simpan PIN|Buat PIN|Simpan|Lanjut)$/);
  await F.keypadPin("246810");
  await A.waitText(/^Saldo rekening utama$/, 25000);
  shot("SA21-2-beranda-pengguna-baru");
  return `pengguna ${newUser}; Kembali dari e-KYC tidak menuju OTP; saldo awal ${F.balanceText()}`;
});

await run("SA-22", "e-KYC NIK berakhiran 0000 ditolak, transaksi tetap terkunci", async () => {
  const u = "sb" + Date.now().toString(36);
  F.freshStart();
  await A.waitText(/^Masuk$/, 30000);
  await A.tap(/^Buka rekening$/);
  await A.typeInto(/^Username$/, u); await A.typeInto(/^Email$/, u + "@mail.test"); await A.typeInto(/^Password$/, pwNew); A.back(); await sleep(300);
  await A.tap(/^Daftar$/);
  await A.waitText(/^Verifikasi OTP$/);
  const code = A.dump().map((n) => n.text).find((t) => /^\d{6}$/.test(t));
  await A.typeInto(/^Kode OTP$/, code); A.back(); await sleep(300);
  await A.tap(/^Verifikasi$/);
  await A.waitText(/^Verifikasi e-KYC$/);
  await A.typeInto(/^NIK KTP \(16 digit\)$/, "3374010101010000"); A.back(); await sleep(400);
  await tickSelfie();
  await A.tap(/^Kirim KTP & Selfie$/);
  const t0 = Date.now(); let msg = null;
  while (!msg && Date.now() - t0 < 25000) { msg = A.dump().map((n) => n.text).find(isRejection); if (!msg) await sleep(700); }
  shot("SA22-ekyc-ditolak");
  expect(msg, "pesan penolakan e-KYC tidak muncul: " + A.screenText().slice(0, 200));
  expect(!A.find(/^Saldo rekening utama$/), "Beranda terbuka meski e-KYC ditolak");
  return msg;
});

await run("SA-01", "login dan PIN (akun demo)", async () => {
  await F.login();
  shot("SA01-beranda");
  return `saldo ${F.balanceText()}`;
});

await run("SA-03", "sembunyikan saldo bertahan setelah aplikasi dibuka ulang", async () => {
  await F.home();
  if (await A.tapIfPresent(/^Tampilkan saldo$/)) await sleep(800); // mulai dari kondisi tampil
  await A.tap(/^Sembunyikan saldo$/);
  await sleep(800);
  A.restartApp(); await sleep(2500);
  await F.relogin(); // aplikasi meminta login/PIN lagi setelah ditutup paksa

  await sleep(800);
  const hidden = F.balanceText();
  shot("SA03-saldo-tersembunyi");
  await A.tap(/^Tampilkan saldo$/);
  await sleep(800);
  expect(hidden && /•|\*/.test(hidden), "saldo tidak tersembunyi setelah dibuka ulang: " + hidden);
  return `tampilan setelah dibuka ulang: ${hidden}`;
});

await run("SA-07", "saldo tidak cukup ditolak sebelum PIN", async () => {
  const bal = await balanceOf("demo");
  const amount = bal + 100000; // melebihi saldo, tetapi masih di bawah batas Rp25.000.000 per transaksi
  expect(amount <= 25000000, `saldo demo ${rp(bal)} terlalu besar untuk menguji saldo tidak cukup`);
  await transferForm(null, "1400000002", amount, null);
  await A.waitText(/saldo tidak (cukup|mencukupi)/i);
  expect(!A.find(/^PIN 6 digit$/), "dialog PIN muncul");
  shot("SA07-saldo-tidak-cukup");
  return A.dump().map((n) => n.text).find((t) => /saldo tidak/i.test(t));
});

await run("SA-05", "transfer antarbank ditolak (BRI 9999999999) → gagal, dana kembali", async () => {
  const before = await balanceOf("demo");
  await transferForm(/^BRI$/, "9999999999", 10000, null);
  await A.tap(/^Transfer sekarang$/);
  await F.pin();
  await A.waitText(/Gagal|ditolak/i, 20000);
  shot("SA05-antarbank-ditolak");
  const after = await balanceOf("demo");
  expect(after === before, `saldo berubah ${before} → ${after}`);
  return `saldo tetap ${rp(after)}`;
});

await run("SA-06", "transfer antarbank timeout (Mandiri 8888888888) → diproses, lalu final via cek status", async () => {
  const before = await balanceOf("demo");
  await transferForm(/^Bank Mandiri$/, "8888888888", 10000, null);
  await A.tap(/^Transfer sekarang$/);
  await F.pin();
  await A.waitText(/[Dd]iproses|tertunda/, 20000);
  shot("SA06-1-diproses");
  let status = A.screenText();
  await A.tap(/^Periksa ulang status$/);
  await A.waitText(/^Berhasil$|Transfer berhasil/, 20000);
  status = A.screenText();
  shot("SA06-2-setelah-periksa-ulang");
  // periksa ulang sekali lagi: tidak boleh ada debit ganda
  await A.tapIfPresent(/^Periksa ulang status$/); await sleep(1500);
  const after = await balanceOf("demo");
  expect(before - after === 12500, `total debit ${before - after}, seharusnya Rp12.500 sekali`);
  return `Diproses → Berhasil setelah "Periksa ulang status"; total debit tepat ${rp(before - after)}`;
});

await run("SA-24", "simpan penerima favorit, pakai, lalu hapus", async () => {
  await F.home();
  await A.tap(/^Transfer$/);
  await A.typeInto(/^Nomor rekening$/, "1400000003"); A.back(); await sleep(300);
  await A.tap(/^Cek rekening$/);
  await A.waitText(/^Nominal transfer$/);
  await A.tap(/^Simpan ke favorit$/);
  await sleep(1000);
  A.back(); await sleep(1000);
  const favNode = await A.waitText(/BUDI/i);
  shot("SA24-favorit");
  A.tapXY(favNode.x, favNode.y); await sleep(1500);
  const used = A.screenText();
  expect(/BUDI/i.test(used) && /Nominal transfer/.test(used), "favorit tidak membuka langkah nominal");
  A.back(); await sleep(1000);
  // hapus favorit lewat API (ikon hapus tidak berlabel teks) dan pastikan hilang dari Android
  const favs = await api("demo", "GET", "/transfers/favorites");
  for (const f of favs.filter((x) => x.accountNumber === "1400000003")) await api("demo", "DELETE", "/transfers/favorites/" + f.id);
  await F.home(); await A.tap(/^Transfer$/); await sleep(1500);
  expect(!A.find(/BUDI/i), "favorit masih tampil setelah dihapus");
  return "favorit tersimpan, dipakai untuk transfer, dan hilang setelah dihapus";
});

await run("SA-09", "bayar QRIS contoh (statis) dengan PIN", async () => {
  await F.home();
  await A.tap(/^Bayar QRIS$/);
  await A.tap(/Warung Bu Sri/);
  await A.waitText(/^Bayar$/);
  await A.typeInto(/^Nominal$/, "12000"); A.back(); await sleep(300);
  await A.tap(/^Bayar$/);
  await F.pin();
  await A.waitText(/berhasil/i, 20000);
  shot("SA09-qris-statis");
  return "Warung Bu Sri Rp12.000";
});

await run("SA-25", "QRIS dinamis: nominal dari kode tidak dapat diubah", async () => {
  await F.home();
  await A.tap(/^Bayar QRIS$/);
  await A.tap(/Kopi Senja Tembalang/);
  await A.waitText(/^Bayar$/);
  const scr = A.screenText();
  shot("SA25-qris-dinamis");
  expect(/dinamis/i.test(scr) && /28[.]?000/.test(scr) && /tidak dapat diubah/i.test(scr), "nominal dinamis tidak terkunci: " + scr.slice(0, 200));
  await A.tap(/^Bayar$/);
  await F.pin();
  await A.waitText(/berhasil/i, 20000);
  return "Rp28.000 terkunci, pembayaran berhasil";
});

await run("SA-10", "top-up pulsa", async () => {
  await F.home();
  await A.tap(/^Top-up$/);
  await A.typeInto(/^Nomor HP$/, "081234567890"); A.back(); await sleep(400);
  await A.tap(/^10\.000$/);
  await A.waitText(/^Konfirmasi Pulsa$/);
  await A.tap(/^Bayar$/);
  await F.pin();
  await A.waitText(/berhasil/i, 20000);
  shot("SA10-pulsa");
  const h = await api("demo", "GET", "/history?size=1");
  expect(/Pulsa/.test(h.items[0].title), "transaksi terakhir bukan pulsa: " + h.items[0].title);
  return `${h.items[0].title}, total ${rp(h.items[0].total)}`;
});

await run("SA-11", "token PLN: ID berakhiran 0000 tidak ditemukan", async () => {
  await F.home();
  await A.tap(/^Top-up$/);
  await A.tap(/^Token Listrik$/);
  await sleep(800);
  const field = A.dump().find((n) => n.editable);
  A.tapXY(field.x, field.y); await sleep(300); A.typeText("123456780000"); A.back(); await sleep(400);
  await A.tap(/^(Cek|Cek pelanggan|Cek ID pelanggan|Lanjut)$/);
  await A.waitText(/tidak ditemukan/i);
  shot("SA11-pln-tidak-ditemukan");
  return A.dump().map((n) => n.text).find((t) => /tidak ditemukan/i.test(t));
});

await run("SA-26", "bayar tagihan, pembayaran kedua di periode sama ditolak", async () => {
  const billNo = "555" + String(Date.now()).slice(-7); // nomor pelanggan baru tiap jalan (tagihan periode ini belum lunas)
  const pay = async () => {
    await F.home();
    await A.tap(/^Top-up$/);
    await A.tap(/^Tagihan$/);
    await sleep(800);
    const field = A.dump().find((n) => n.editable);
    A.tapXY(field.x, field.y); await sleep(300);
    A.key("KEYCODE_MOVE_END"); for (let i = 0; i < 2; i++) A.adb("shell", "input", "keyevent", ...Array(14).fill("KEYCODE_DEL"));
    A.typeText(billNo); A.back(); await sleep(400);
    await A.tap(/^(Cek tagihan|Cek|Lanjut)$/);
    await sleep(2500);
  };
  await pay();
  shot("SA26-1-tagihan");
  await A.tap(/^Lanjut bayar$/);
  await A.tap(/^Bayar$/);
  await F.pin();
  await A.waitText(/berhasil/i, 20000);
  await pay();
  const second = A.screenText();
  shot("SA26-2-tagihan-kedua");
  if (A.find(/^Lanjut bayar$/)) { await A.tap(/^Lanjut bayar$/); await sleep(1500); }
  const second2 = A.screenText();
  expect(/sudah dibayar|lunas|tidak ada tagihan/i.test(second + second2) && !A.find(/^PIN 6 digit$/), "tagihan dapat dibayar dua kali: " + second2.slice(0, 200));
  return ((second + " | " + second2).match(/[^|]*(sudah dibayar|lunas|tidak ada tagihan)[^|]*/i) || [""])[0].trim();
});

await run("SA-12", "kantong dikunci: dana dipindah, penarikan butuh PIN", async () => {
  const before = await balanceOf("demo");
  await F.home();
  await A.tap(/^Kantong$/);
  await A.tap(/Buat kantong/);
  await A.typeInto(/^Nama kantong$/, "Uji SA12"); A.back(); await sleep(300);
  await A.tap(/^1 bln$/);
  await A.tap(/^Buat$/);
  await A.waitText(/Uji SA12/);
  await A.tap(/Uji SA12/);
  await sleep(1200);
  shot("SA12-1-detail-kantong");
  await A.tap(/^(Isi|Isi kantong|Tambah dana)$/);
  await sleep(800);
  const f = A.dump().find((n) => n.editable); A.tapXY(f.x, f.y); await sleep(300); A.typeText("20000"); A.back(); await sleep(300);
  await A.tap(/^(Isi|Isi kantong|Simpan|Tambah|Pindahkan)$/);
  await sleep(2500);
  const mid = await balanceOf("demo");
  expect(before - mid === 20000, `saldo utama tidak berkurang 20.000 (${before} → ${mid})`);
  await A.tap(/^(Tarik|Tarik dana|Tarik dini)$/);
  await sleep(1200);
  const scr = A.screenText();
  shot("SA12-2-tarik-terkunci");
  expect(/terkunci|PIN|penarikan dini/i.test(scr), "penarikan kantong terkunci tidak meminta konfirmasi/PIN: " + scr.slice(0, 200));
  return `saldo utama −Rp20.000; penarikan saat terkunci meminta konfirmasi`;
});

await run("SA-13", "virtual card: buat, detail dengan PIN, bekukan", async () => {
  // pengguna baru: akun demo bisa sudah mencapai batas 5 kartu (FR-21) dari jalan uji sebelumnya
  const nu = await apiNewUser();
  await F.login(nu.u, nu.pw, nu.pin);
  await A.tap(/^Kartu$/);
  await A.tap(/Buat kartu virtual/);
  await A.tap(/^Domestik$/);
  await A.tap(/^Buat kartu$/);
  await sleep(2500);
  shot("SA13-1-kartu-baru");
  const masked = A.dump().map((n) => n.text).find((t) => /\*{4}|•{4}/.test(t));
  expect(masked, "kartu tersamar tidak tampil");
  await A.tap(/(Lihat detail|Detail)/);
  await F.pin(nu.pin);
  await sleep(2000);
  const det = A.screenText();
  shot("SA13-2-detail");
  // Nomor pengganti 0000… dan CVV kosong tidak boleh dianggap lulus (DEF-12).
  const num = det.match(/\d{4} ?\d{4} ?\d{4} ?\d{4}/)?.[0];
  expect(num && !num.startsWith("0000") && /CVV \| \d{3}\b/.test(det), "nomor lengkap/CVV asli tidak tampil setelah PIN: " + det.slice(0, 220));
  await F.home(nu.u, nu.pw, nu.pin); await A.tap(/^Kartu$/); await sleep(1200); // tombol Bekukan ada di halaman Kartu
  await A.tap(/^Bekukan$/);
  await sleep(1500);
  await A.tapIfPresent(/^(Ya|Lanjutkan)$/); // aplikasi tidak memakai dialog konfirmasi; jangan ketuk "Bekukan" kartu lain
  await A.waitText(/[Dd]ibekukan|[Bb]eku|FROZEN|Aktifkan/);
  shot("SA13-3-dibekukan");
  return `kartu ${masked}`;
});

await run("SA-14", "riwayat: filter Keluar, detail, analitik", async () => {
  await F.login(); // SA-13 memakai pengguna baru; kembalikan sesi ke akun demo untuk kasus berikutnya
  await A.tap(/^Riwayat$/);
  await A.tap(/^Filter jenis & status$/);
  const keluar = A.dump().filter((n) => n.text === "Keluar").pop();
  A.tapXY(keluar.x, keluar.y); await sleep(1500);
  const items = A.dump().map((n) => n.text).filter((t) => /^[−+]Rp/.test(t));
  expect(items.length && items.every((t) => t.startsWith("−")), "filter Keluar memuat transaksi masuk: " + items.join(","));
  shot("SA14-1-filter-keluar");
  const first = A.dump().find((n) => /^[−]Rp/.test(n.text));
  A.tapXY(first.x - 300, first.y); await sleep(1800);
  const det = A.screenText();
  shot("SA14-2-detail");
  expect(/ID transaksi/i.test(det), "detail transaksi tidak terbuka");
  A.back(); await sleep(800);
  await A.tap(/^Analitik$/);
  await sleep(1500);
  shot("SA14-3-analitik");
  expect(/Pemasukan|Pengeluaran/.test(A.screenText()), "analitik tidak tampil");
  return `${items.length} transaksi keluar tampil; detail dan analitik terbuka`;
});

await run("SA-27", "grafik saldo sesuai saldo dan riwayat", async () => {
  const bal = await balanceOf("demo");
  await F.home();
  await A.tap(/^Grafik Saldo$/);
  await sleep(1500);
  const scr = A.screenText();
  shot("SA27-grafik-saldo");
  const last = scr.match(/Grafik saldo\..*?(\d{2}\/\d{2}): (Rp[\d.]+) \|/);
  const lastVal = [...scr.matchAll(/\d{2}\/\d{2}: (Rp[\d.]+)/g)].pop()?.[1];
  expect(lastVal === rp(bal), `titik terakhir ${lastVal} ≠ saldo ${rp(bal)}`);
  return `titik terakhir ${lastVal} = saldo backend`;
});

await run("SA-28", "matikan notifikasi transaksi keluar → tidak dikirim", async () => {
  await F.home();
  await A.tap(/^Profil$/);
  await A.tap(/^Pengaturan notifikasi$/);
  await A.tapControlFor(/^Transaksi keluar$/);
  await sleep(1500);
  const s = await api("demo", "GET", "/me/notification-settings");
  expect(!s.types.includes("OUTGOING"), "setelan OUTGOING tidak tersimpan: " + JSON.stringify(s));
  const before = (await api("demo", "GET", "/me/notifications")).items.filter((n) => n.type === "OUTGOING").length;
  await api("demo", "POST", "/transfers", { bankCode: "DDB", accountNumber: "1400000002", amount: 5000, pin: "123456" }, { "Idempotency-Key": "sa28-" + Date.now() });
  await sleep(2500);
  const after = (await api("demo", "GET", "/me/notifications")).items.filter((n) => n.type === "OUTGOING").length;
  shot("SA28-pengaturan");
  await F.home(); await A.tap(/^Profil$/); await A.tap(/^Pengaturan notifikasi$/);
  await A.tapControlFor(/^Transaksi keluar$/); await sleep(1200);
  const s2 = await api("demo", "GET", "/me/notification-settings");
  expect(after === before, `notifikasi keluar tetap dikirim (${before} → ${after})`);
  return `tidak ada notifikasi keluar baru; setelan dipulihkan (${s2.types.includes("OUTGOING") ? "OUTGOING aktif lagi" : "PERIKSA"})`;
});

await run("SA-16", "notifikasi push dana masuk ≤ 15 detik", async () => {
  await F.home();
  // Notifikasi baru dikenali dari key yang belum ada sebelum transfer.
  const keysOf = () => new Set([...A.adb("shell", "dumpsys", "notification", "--noredact").matchAll(/key=(\S*com\.diponegoro\.digitalbank\S*)/g)].map((m) => m[1]));
  const before = keysOf();
  const t0 = Date.now();
  await api("andi", "POST", "/transfers", { bankCode: "DDB", accountNumber: "1400123456", amount: 7000, note: "SA16", pin: "123456" }, { "Idempotency-Key": "sa16-" + t0 });
  let found = null;
  while (Date.now() - t0 < 20000 && !found) {
    found = [...keysOf()].find((k) => !before.has(k));
    if (!found) await sleep(1000);
  }
  const secs = ((Date.now() - t0) / 1000).toFixed(1);
  A.adb("shell", "cmd", "statusbar", "expand-notifications"); await sleep(1200);
  shot("SA16-notifikasi");
  A.adb("shell", "cmd", "statusbar", "collapse");
  expect(found, "notifikasi tidak muncul dalam 20 detik");
  expect(+secs <= 15, `notifikasi muncul setelah ${secs} detik`);
  return `muncul ±${secs} detik setelah transfer dari andi`;
});

await run("SA-17", "konsistensi dua perangkat: total saldo demo + andi tetap", async () => {
  const d0 = await balanceOf("demo"), a0 = await balanceOf("andi");
  await transferForm(null, "1400000002", 9000, null);
  await A.tap(/^Transfer sekarang$/);
  await F.pin();
  await A.waitText(/^Transfer berhasil$/);
  const d1 = await balanceOf("demo"), a1 = await balanceOf("andi");
  expect(d0 + a0 === d1 + a1 && d0 - d1 === 9000, `tidak konsisten: demo ${d0}→${d1}, andi ${a0}→${a1}`);
  return `demo −Rp9.000, andi +Rp9.000, total tetap ${rp(d1 + a1)}`;
});

await run("SA-15", "ubah profil (email) dengan OTP", async () => {
  expect(newUser, "SA-21 harus berjalan lebih dulu");
  await F.login(newUser, pwNew, "246810");
  await A.tap(/^Profil$/);
  await A.tap(/^Ubah data$/);
  const email = newUser + "+baru@mail.test";
  await A.typeInto(/^Email$/, email); A.back(); await sleep(300);
  await A.tap(/^Simpan$/);
  await A.waitText(/OTP/);
  await sleep(1000);
  const code = A.dump().map((n) => n.text).find((t) => /^\d{6}$/.test(t)) || (A.screenText().match(/\b(\d{6})\b/) || [])[1];
  expect(code, "kode OTP simulasi tidak tampil");
  const f = A.dump().filter((n) => n.editable).pop(); A.tapXY(f.x, f.y); await sleep(300); A.typeText(code); A.back(); await sleep(300);
  await A.tap(/^(Verifikasi|Konfirmasi|Simpan)$/);
  await sleep(2500);
  shot("SA15-profil");
  const me = await (async () => {
    const tok = (await (await fetch(API + "/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ identifier: newUser, password: pwNew, deviceId: "sa-probe-new" }) })).json()).accessToken;
    return (await fetch(API + "/me", { headers: { Authorization: "Bearer " + tok } })).json();
  })();
  expect(me.email === email, "email tidak berubah: " + me.email);
  return `email menjadi ${email}`;
});

await run("SA-02", "PIN salah 3 kali → diblokir sementara (akun baru)", async () => {
  expect(newUser, "SA-21 harus berjalan lebih dulu");
  F.freshStart();
  await A.waitText(/^Masuk$/, 30000);
  await A.typeInto(/^Username, email/, newUser); await A.typeInto(/^Password$/, pwNew); A.back(); await sleep(300);
  await A.tap(/^Masuk$/);
  const msgs = [];
  for (let i = 0; i < 3; i++) {
    await F.keypadPin("000000");
    await sleep(2000);
    // abaikan teks petunjuk statis "Salah 3 kali akan memblokir PIN sementara"
    msgs.push(A.dump().map((n) => n.text).find((t) => /PIN salah|sisa|diblokir/i.test(t) && !/akan memblokir/i.test(t)) || "(tidak ada pesan)");
  }
  shot("SA02-pin-diblokir");
  expect(/diblokir/i.test(msgs[2]) && !/diblokir/i.test(msgs[0]), "PIN tidak diblokir tepat setelah 3 kali: " + msgs.join(" / "));
  return msgs.join(" → ");
});

// Sakelar biometrik: View yang dapat diketuk, sebaris dengan teks "Izinkan login dengan biometrik".
const biometricSwitch = () => {
  const nodes = A.dump();
  const label = A.find(/Izinkan login dengan biometrik/, nodes);
  return label && nodes.find((n) => n.clickable && n.x > 700 && Math.abs(n.y - label.y) < 40);
};
await run("SA-23", "login biometrik (simulasi token perangkat) tanpa password", async () => {
  await F.login(); // SA-02 memblokir PIN pengguna baru; mulai lagi dari akun demo
  await A.tap(/^Profil$/);
  await A.tap(/^Keamanan/);
  await A.waitText(/^Login biometrik$/);
  const sw = biometricSwitch();
  expect(sw, "sakelar biometrik tidak ditemukan");
  if (!sw.checked) { A.tapXY(sw.x, sw.y); await sleep(2000); }
  expect(biometricSwitch()?.checked, "biometrik tidak aktif setelah sakelar diketuk");
  shot("SA23-1-biometrik-aktif");
  // keluar (bukan keluar semua perangkat), lalu masuk dengan biometrik
  await F.home(); await A.tap(/^Profil$/);
  await A.tap(/^Keluar$/);
  await sleep(1500);
  await A.tapIfPresent(/^(Ya|Keluar|Logout)$/);
  await A.waitText(/^Masuk$/, 20000);
  const btn = await A.waitText(/biometrik|sidik jari/i, 10000);
  shot("SA23-2-tombol-biometrik");
  A.tapXY(btn.x, btn.y); await sleep(1500);
  // prototipe: dialog "Autentikasi Biometrik (Simulasi)" menggantikan sensor sidik jari/wajah
  await A.waitText(/Autentikasi biometrik/i, 10000);
  shot("SA23-3-dialog-simulasi");
  await A.tap(/^Autentikasi$/); await sleep(2500);
  await A.waitText(/^Masukkan PIN$|^Saldo rekening utama$/, 20000);
  if (A.find(/^Masukkan PIN$/)) { await F.keypadPin("123456"); }
  await A.waitText(/^Saldo rekening utama$/, 20000);
  // matikan lagi agar kondisi akun demo kembali seperti semula
  await F.home(); await A.tap(/^Profil$/); await A.tap(/^Keamanan/); await A.waitText(/^Login biometrik$/);
  const sw2 = biometricSwitch(); if (sw2?.checked) { A.tapXY(sw2.x, sw2.y); await sleep(1500); }
  return "masuk tanpa password lewat tombol biometrik (simulasi: token perangkat, belum memakai sensor/BiometricPrompt)";
});

// ---- hanya bila diminta eksplisit ----
await run("SA-18", "diam > 5 menit → diminta login ulang", async () => {
  await F.login();
  await sleep(6 * 60 * 1000); // pengecekan aplikasi tiap 10 detik + panggilan logout; jangan menyentuh layar sebelum lewat
  // Aplikasi seharusnya sudah kembali ke halaman login sendiri; bila belum, aksi berikutnya harus ditolak.
  if (!A.find(/^Masuk$/)) { await A.tapIfPresent(/^Riwayat$/); await A.waitText(/^Masuk$/, 30000); }
  const msg = A.dump().map((n) => n.text).find((t) => /berakhir/i.test(t));
  shot("SA18-timeout-sesi");
  expect(msg, "halaman login tanpa pesan sesi berakhir");
  return msg;
}, { explicit: true });

await run("SA-20", "server tidak terjangkau → pesan, aplikasi tidak tertutup", async () => {
  await F.home();
  const { spawnSync } = await import("node:child_process");
  spawnSync("powershell", ["-NoProfile", "-Command", "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -like '*ddb-backend*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }"]);
  await sleep(2000);
  await A.tap(/^Riwayat$/);
  await A.waitText(/Tidak dapat terhubung/i, 20000);
  shot("SA20-offline");
  const alive = A.adb("shell", "pidof", A.PKG).trim();
  expect(alive, "aplikasi tertutup");
  return "pesan \"Tidak dapat terhubung ke server\"; aplikasi tetap berjalan (backend perlu dijalankan ulang)";
}, { explicit: true });

writeFileSync(path.join(OUT, "hasil.txt"), log.join("\n") + "\n");
console.log(`\n${log.filter((l) => l.startsWith("PASS")).length}/${log.length} PASS`);
