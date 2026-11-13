// Kasus uji Web tambahan (STD-DDB-001): WB-11 profil dengan OTP, WB-12 token kedaluwarsa,
// WB-13 grafik saldo, WB-14 foto profil, WB-15 ubah dan kunci kantong.
// WB-12 butuh backend dengan masa berlaku access token 1 menit:
//   $env:DDB_SECURITY_ACCESS_TOKEN_MINUTES="1"; .\run-local.ps1 -H2
// Jalankan: node web/wb-extra.mjs hasil/wb WB-11[,WB-12,WB-13,WB-14,WB-15]
import path from "node:path";
import { writeFileSync } from "node:fs";
import { launch, sleep } from "../lib/cdp.mjs";

const OUT = path.resolve(process.argv[2] || "hasil/wb");
const ONLY = new Set((process.argv[3] || "WB-11").split(","));
const API = "http://localhost:5173/api/v1";
const log = [];
const step = (id, s, ok, extra = "") => { log.push(`${ok ? "PASS" : "FAIL"}  ${id}  ${s}${extra ? "  — " + extra : ""}`); console.log(log.at(-1)); };

// Pengguna baru siap transaksi (registrasi → OTP → e-KYC → PIN) lewat API
async function newUser() {
  const u = "wb" + Date.now().toString(36), pw = "Rahasia123";
  const post = (p, b, t) => fetch(API + p, { method: "POST", headers: { "Content-Type": "application/json", ...(t ? { Authorization: "Bearer " + t } : {}) }, body: JSON.stringify(b) }).then((r) => r.json());
  const reg = await post("/auth/register", { username: u, email: u + "@mail.test", password: pw });
  const tok = (await post("/auth/register/verify-otp", { userId: reg.userId, code: reg.otp.demoCode, deviceId: "wb-setup" })).accessToken;
  await post("/auth/ekyc", { nik: "3374010101010001", selfieReference: "s.jpg" }, tok);
  for (let i = 0; i < 20; i++) { const e = await fetch(API + "/auth/ekyc", { headers: { Authorization: "Bearer " + tok } }).then((r) => r.json()); if (e.status === "APPROVED") break; await sleep(500); }
  await post("/auth/pin", { pin: "135790" }, tok);
  return { u, pw };
}

const web = await launch(OUT, { port: 9337 });
try {
  if (ONLY.has("WB-13")) {
    // FR-07 grafik perubahan saldo
    await web.login("citra");
    await web.waitFor("!!document.querySelector('.chart path.line')", 15000);
    const balText = await web.evalJs("document.querySelector('.balance-value').textContent.trim()");
    await web.shot("WB13-1-grafik-harian.png");
    const periods = [];
    for (const p of ["Mingguan", "Bulanan", "Harian"]) {
      await web.clickText(p);
      await web.waitFor(`[...document.querySelectorAll('.segmented.compact button')].find(b => b.textContent === '${p}')?.getAttribute('aria-checked') === 'true' && !!document.querySelector('.chart path.line')`);
      periods.push(`${p}: ${await web.evalJs("document.querySelectorAll('.chart circle.dot').length")} titik`);
    }
    await web.clickText("Lihat sebagai tabel");
    await web.waitFor("!!document.querySelector('.data-table')");
    const lastRow = await web.evalJs("[...document.querySelectorAll('.data-table tbody tr')].at(-1).lastElementChild.textContent.trim()");
    await web.shot("WB13-2-tabel.png");
    step("WB-13", "grafik saldo harian/mingguan/bulanan; titik terakhir = saldo", lastRow === balText, `${periods.join(", ")}; titik terakhir ${lastRow}, saldo ${balText}`);
  }
  if (ONLY.has("WB-14")) {
    // FR-27 foto profil (URL gambar dari server lokal, tanpa permintaan keluar)
    const { u, pw } = await newUser();
    await web.login(u, pw);
    await web.go("/profil");
    await web.waitFor("[...document.querySelectorAll('button')].some(b => b.textContent.includes('Tambah foto'))");
    await web.clickText("Tambah foto");
    await web.waitFor("!!document.querySelector('#ph')");
    await web.fill("#ph", "ftp://contoh/foto.png");
    await web.clickText("Simpan foto");
    await web.waitFor("!!document.querySelector('.field-error')");
    const invalidMsg = await web.evalJs("document.querySelector('.field-error').textContent");
    const url = "http://localhost:5173/logo-mark.png";
    await web.fill("#ph", url);
    await web.clickText("Simpan foto");
    await web.waitFor(`document.querySelectorAll('img.avatar[src="${url}"]').length >= 2`);
    await web.shot("WB14-1-foto-tersimpan.png");
    await web.reload();
    await web.waitFor(`document.querySelectorAll('img.avatar[src="${url}"]').length >= 2`);
    await web.clickText("Hapus foto");
    await web.waitFor("!document.querySelector('img.avatar') && !!document.querySelector('.avatar-initials')");
    step("WB-14", "foto profil: URL tidak valid ditolak, simpan (tetap setelah muat ulang), hapus", /http|URL|alamat/i.test(invalidMsg), `pesan URL tidak valid: "${invalidMsg}"; foto tampil di profil dan bilah atas`);
  }
  if (ONLY.has("WB-15")) {
    // FR-08/FR-09 ubah dan kunci kantong yang sudah ada
    const { u, pw } = await newUser();
    await web.login(u, pw);
    await web.go("/kantong");
    await web.waitFor("[...document.querySelectorAll('button')].some(b => b.textContent.includes('Kantong baru'))");
    await web.clickText("Kantong baru");
    await web.waitFor("!!document.querySelector('#pn')");
    await web.fill("#pn", "Laptop"); await web.fill("#pt", "1000000");
    await web.clickText("Buat kantong");
    await web.waitFor("document.querySelectorAll('.pocket').length === 1");
    await web.clickText("Ubah");
    await web.waitFor("!!document.querySelector('#en')");
    await web.fill("#en", "Laptop Kuliah"); await web.fill("#ep", "Tugas akhir");
    await web.clickText("Simpan perubahan");
    await web.waitFor("document.querySelector('.pocket strong')?.textContent === 'Laptop Kuliah'");
    await web.clickText("Kunci");
    await web.waitFor("!!document.querySelector('#lu')");
    const until = new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10);
    await web.fill("#lu", until);
    await web.clickText("Kunci kantong");
    await web.waitFor("document.querySelector('.pocket .badge')?.textContent.includes('Terkunci')");
    const lockBtnGone = await web.evalJs("![...document.querySelectorAll('.pocket button')].some(b => b.textContent.trim() === 'Kunci')");
    await web.shot("WB15-kantong-diubah-dikunci.png");
    step("WB-15", "ubah nama/tujuan kantong lalu kunci sampai tanggal", lockBtnGone, `nama "Laptop Kuliah", terkunci sampai ${until}`);
  }
  if (ONLY.has("WB-11")) {
    const { u, pw } = await newUser();
    await web.login(u, pw);
    await web.go("/profil");
    await web.waitFor("document.body.innerText.includes('Profil') && [...document.querySelectorAll('button')].some(b => b.textContent.trim() === 'Ubah')");
    await web.clickText("Ubah");
    await web.waitFor("!!document.querySelector('#pe')");
    const email = u + ".baru@mail.test";
    await web.fill("#pe", email);
    await web.clickText("Kirim OTP");
    await web.waitFor("!!document.querySelector('#po')");
    const code = await web.evalJs("document.querySelector('.alert-info strong')?.textContent");
    await web.shot("WB11-1-otp.png");
    await web.fill("#po", code);
    await web.clickText("Konfirmasi perubahan");
    await web.waitFor("document.body.innerText.includes('Profil diperbarui')");
    const shown = await web.evalJs(`document.body.innerText.includes(${JSON.stringify(email)})`);
    await web.shot("WB11-2-profil-diperbarui.png");
    // email baru dapat dipakai untuk masuk
    const r = await fetch(API + "/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ identifier: email, password: pw, deviceId: "wb-check" }) });
    step("WB-11", "ubah email lewat Web dengan OTP", shown && r.status === 200, `email baru ${email} tampil di profil dan dapat dipakai login (HTTP ${r.status})`);
  }
  if (ONLY.has("WB-12")) {
    await web.login("budi");
    const t0 = Date.now();
    await sleep(75 * 1000); // access token 1 menit kedaluwarsa
    await web.go("/riwayat");
    await web.waitFor("document.querySelectorAll('.tx-row').length > 0 || location.pathname === '/login'", 20000);
    const okAfterExpiry = await web.evalJs("location.pathname === '/riwayat' && document.querySelectorAll('.tx-row').length > 0");
    // navigasi di dalam aplikasi (tanpa muat ulang) setelah token kedaluwarsa lagi → interceptor 401 → refresh
    await sleep(75 * 1000);
    await web.evalJs("document.querySelector('a[href=\"/\"]').click()");
    await web.waitFor("!!document.querySelector('.balance-value') || location.pathname === '/login'", 20000);
    const okInApp = await web.evalJs("location.pathname === '/' && !!document.querySelector('.balance-value')");
    await web.shot("WB12-1-setelah-token-kedaluwarsa.png");
    // refresh token dicabut (logout semua perangkat dari klien lain) → aksi berikutnya ke halaman masuk
    const tok = (await fetch(API + "/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ identifier: "budi", password: "Demo@1234", deviceId: "wb-revoker" }) }).then((r) => r.json())).accessToken;
    await fetch(API + "/auth/logout-all", { method: "POST", headers: { Authorization: "Bearer " + tok } });
    await web.evalJs("document.querySelector('a[href=\"/riwayat\"]').click()");
    await web.waitFor("location.pathname === '/login'", 20000);
    const msg = await web.text();
    await web.shot("WB12-2-sesi-dicabut.png");
    step("WB-12", "access token kedaluwarsa diperbarui otomatis; refresh ditolak → halaman masuk",
      okAfterExpiry && okInApp && /berakhir/i.test(msg), `muat ulang setelah ${Math.round((Date.now() - t0) / 60000)} menit tetap masuk; navigasi dalam aplikasi tetap berjalan; setelah dicabut → login dengan pesan sesi berakhir`);
  }
} catch (e) {
  step("ERROR", e.message.slice(0, 300), false);
  await web.shot("zz-wb-error.png");
}
step("—", "tanpa error JavaScript di Web", web.errors.length === 0, web.errors.slice(0, 2).join(" | "));
writeFileSync(path.join(OUT, "hasil.txt"), log.join("\n") + "\n");
await web.close();
process.exit(0);
