// D05W User Interface Map – Web (UIM-DDB-001-WEB). Dasar: draf v0.7 Nabila + REVIEW_D05W_Nabila_revisi4.md + kode digitalbanking-node v2.0.0.
// Usage: node doc5w.js <folder tangkapan layar Web> <fig_d05w> <out.docx> <logo.jpeg> <tanda_tangan.png>
const S = require("./ddbstyle");
const { D, P, H1, H2, H3, B, NL, CAP, table, img } = S;
const { Paragraph } = D;
const path = require("path");
const [, , LAYAR, FIG, OUT, LOGO, SIGN] = process.argv;

const meta = {
  docLabel: "Dokumen 5 – Volume Web", title: "User Interface Map", subtitle: "Inventaris Halaman, Peta Navigasi, dan Kartu Halaman – Aplikasi Web Diponegoro Digital Bank",
  code: "UIM-DDB-001-WEB", version: "1.0", statusShort: "Final",
  status: "Final v1.0 – diterbitkan Project Manager 22 November 2026; wewenang review dan persetujuan dijalankan Project Manager",
  module: "User Interface Map – Web (11 halaman; Node.js, Express 5, EJS)", createdBy: "Nabila Puspa Annastasya (PIC Dokumen 5 Volume Web)",
  updatedBy: "Salomo Putra Glorious Hokoyoku (Project Manager)", team: "Kelompok Jaran Goyang – Web Application Team (PIC: Nabila Puspa Annastasya)",
  refs: "SRS-DDB-001 v0.1; SAD-DDB-001 v1.0 Bab VIII; FSD-DDB-001 v1.0; SDD-DDB-001-WEB v1.0; STD-DDB-001 v1.0; kode apps/web/digitalbanking-node v2.0.0 (routes/, views/, lib/)",
  date: "22 November 2026", logo: LOGO,
  revisions: [
    ["0.1", "11 Nov 2026", "Nabila Puspa Annastasya", "Draf awal berdasarkan SRS-DDB-001 v0.1."],
    ["0.2", "11 Nov 2026", "Nabila Puspa Annastasya", "Penyesuaian sementara pada SRS Web v1.0 tim Web."],
    ["0.3", "11 Nov 2026", "Nabila Puspa Annastasya", "Kembali mengacu pada SRS-DDB-001 v0.1; rincian halaman dan alur transfer disusun ulang."],
    ["0.5", "16 Nov 2026", "Nabila Puspa Annastasya", "Peta navigasi, route guard, peta modul, inventaris, dan kartu halaman."],
    ["0.6", "16 Nov 2026", "Nabila Puspa Annastasya", "Perbaikan alur OTP/e-KYC, kode dokumen, endpoint tambahan, dan format tabel."],
    ["0.7", "22 Nov 2026", "Nabila Puspa Annastasya", "Penyusunan ulang untuk Web Node/Express/EJS: kode halaman WEB-xx, rute, endpoint, dan cakupan fitur aktual."],
    ["1.0", "22 Nov 2026", "Salomo P.G. Hokoyoku", "Penyelesaian oleh Project Manager sesuai review revisi 4: butir terbuka D-01 sampai D-10 ditutup, tangkapan layar WEB-01 sampai WEB-11 disisipkan, rujukan uji WN dan LPN ditambahkan, dan isi dicocokkan dengan kode. Diagram dan struktur dari draf v0.7. Disetujui PM."],
  ],
  approval: { sign: SIGN, date: "22 Nov 2026" },
};

const fig = (f, t, w = 600) => [img(path.join(FIG, f), w, { alt: t }), CAP(t)];
let g = 0;
const shot = (f, t, w = 470) => { const c = `Gambar 4.${++g} ${t}`; return [img(path.join(LAYAR, f + ".png"), w, { alt: c }), CAP(c)]; };
let tb = 0;
// kode, nama, rute, akses, templat, FR, endpoint, elemen, aksi, kondisi, uji, [[gambar, keterangan, lebar]]
const L = [
  ["WEB-01", "Masuk", "GET/POST /login; GET /logout", "Publik", "views/login.ejs", "FR-03, FR-05", "POST /auth/login; POST /auth/logout", "Kolom \"Email / No. HP\" dan \"Kata Sandi\" dengan ikon mata; tombol Login; tautan Daftar akun dan Lupa sandi.", "Berhasil → langkah yang masih kurang (WEB-04 atau WEB-05) atau WEB-06.", "Backend menerima username, email, atau nomor HP pada kolom yang sama. Login tanpa OTP. Kunci akun setelah lima kali salah adalah aturan backend dan tidak diuji lewat Web.", "WN-01, WN-02, WN-14, LPN-03", [["WEB-01-masuk", "WEB-01 Masuk"]], "Authentication"],
  ["WEB-02", "Daftar akun", "GET/POST /register", "Publik", "views/register.ejs", "FR-01", "POST /auth/register", "Kolom Username, Email, No. HP, Kata Sandi, Ulangi Kata Sandi; tombol Daftar.", "Data valid → WEB-03.", "Nama lengkap tidak diminta di sini; diisi (opsional) pada WEB-04.", "WN-13", [["WEB-02-daftar", "WEB-02 Daftar akun"]], "Authentication"],
  ["WEB-03", "Verifikasi OTP", "GET/POST /register/otp; GET/POST /profil/otp", "Publik saat pendaftaran; login saat ubah profil", "views/otp.ejs", "FR-01, FR-27", "POST /auth/register/verify-otp, /auth/register/resend-otp; POST /me/profile-change/confirm", "Kolom kode 6 digit; kode demo; tombol konfirmasi; Kirim ulang kode; tautan kembali.", "OTP pendaftaran valid → WEB-04. OTP profil valid → WEB-10.", "Kode demo tampil hanya bila backend mengirim demoCode (mode simulasi). OTP salah ditolak.", "WN-13, WN-18", [["WEB-03-otp", "WEB-03 OTP pendaftaran"], ["WEB-03b-otp-profil", "WEB-03 OTP perubahan profil"]], "Authentication"],
  ["WEB-04", "e-KYC", "GET/POST /ekyc", "Login saja", "views/ekyc.ejs", "FR-02", "GET/POST /auth/ekyc; GET /me", "NIK (16 digit); nama sesuai KTP (opsional); nama berkas swafoto (simulasi); tombol Kirim Data e-KYC; status proses.", "Disetujui → WEB-05.", "Halaman menyegarkan diri selama diproses. NIK berakhiran 0000 ditolak simulator. Swafoto hanya berupa teks referensi; tidak ada unggah berkas.", "WN-13, WN-23", [["WEB-04-ekyc", "WEB-04 e-KYC"]], "Authentication"],
  ["WEB-05", "Buat PIN transaksi", "GET/POST /pin", "Login saja", "views/pin.ejs", "FR-04", "POST /auth/pin; GET /me", "Kolom PIN (6 digit) dan Ulangi PIN; tombol Simpan PIN.", "PIN tersimpan → WEB-06.", "PIN harus 6 digit dan sama.", "WN-13", [["WEB-05-pin", "WEB-05 Buat PIN"]], "Authentication"],
  ["WEB-11", "Pemulihan kata sandi/PIN", "GET/POST /lupa-sandi", "Publik", "views/lupa_sandi.ejs", "FR-05", "POST /auth/recovery/request, /verify, /reset", "Kolom identitas; pilihan Password atau PIN transaksi; kode OTP; nilai baru dan ulangannya.", "Berhasil → WEB-01 dengan pesan.", "PIN baru harus 6 digit. Nilai lama tidak berlaku lagi.", "WN-20", [["WEB-11-lupa-sandi", "WEB-11 Pemulihan"]], "Authentication"],
  ["WEB-06", "Dashboard", "GET /dashboard", "Terlindungi penuh", "views/dashboard.ejs", "FR-06", "GET /accounts/me; GET /history?size=5", "Saldo, jenis dan nomor rekening; pintasan Transfer, Bayar, Top Up, QRIS; informasi rekening; lima transaksi terakhir.", "Pintasan dan menu samping → WEB-07, WEB-08, WEB-09, WEB-10.", "Saldo disamarkan bila disetel tersembunyi di backend; tombol untuk mengubahnya belum ada di Web. Grafik saldo (FR-07) dan ringkasan bulanan (FR-25) belum tersedia.", "WN-01, WN-23, LPN-01", [["WEB-06-dashboard", "WEB-06 Dashboard"]], "Account"],
  ["WEB-07", "Transfer", "GET/POST /transfer?tab=antar|bank", "Terlindungi penuh", "views/transfer.ejs", "FR-10 – FR-12, FR-14 (sebagian)", "GET /transfers/banks; POST /transfers/inquiry, /transfers/quote, /transfers", "Tab Sesama Bank (DDB) dan Ke Bank Lain; bank, nomor rekening, nominal, catatan; tahap konfirmasi berisi penerima, biaya, total, dan kolom PIN.", "Formulir → konfirmasi → PIN → detail pada WEB-09.", "Minimal Rp5.000; batas Rp25.000.000 per transaksi adalah aturan backend. Rekening tidak valid ditolak sebelum PIN. PIN salah tiga kali memblokir PIN. Penerima favorit (FR-13) belum tersedia; bukti dilihat dan dicetak dari detail riwayat.", "WN-03 – WN-06, WN-15, WN-24, LPN-01", [["WEB-07-transfer", "WEB-07 Formulir transfer"], ["WEB-07b-transfer-konfirmasi", "WEB-07 Konfirmasi dan PIN"], ["WEB-07d-rekening-tidak-valid", "WEB-07 Rekening tujuan tidak valid"]], "Transfer"],
  ["WEB-08", "Pembayaran dan Top Up", "GET/POST /pembayaran?tab=topup|pulsa|pln|tagihan|ewallet|qris", "Terlindungi penuh", "views/pembayaran.ejs", "FR-15 (sebagian), FR-18 (sebagian), FR-19, FR-20", "GET /topup/catalog, /qris/demo; POST /topup/quote, /topup/orders, /qris/scan, /qris/pay, /qris/simulate-incoming", "Tab Top Up, Pulsa, Token PLN, Tagihan, E-wallet, QRIS; formulir sesuai tab; konfirmasi dengan PIN; saldo saat ini.", "Formulir → konfirmasi → PIN → WEB-09. Top Up langsung kembali ke WEB-06.", "Top Up Rp1.000–Rp10.000.000 tanpa PIN dan tercatat sebagai dana masuk QRIS. ID PLN berakhiran 0000 ditolak. QRIS dipilih dari contoh atau ditempel, tanpa kamera. Paket data, QRIS pribadi (FR-16), dan QRIS dinamis (FR-17) belum tersedia.", "WN-07 – WN-11, WN-16, WN-17, LPN-02", [["WEB-08b-pulsa", "WEB-08 Tab Pulsa"], ["WEB-08c-pulsa-konfirmasi", "WEB-08 Konfirmasi pulsa"], ["WEB-08-topup", "WEB-08 Tab Top Up"], ["WEB-08g-qris", "WEB-08 Tab QRIS"], ["WEB-08f-pln-ditolak", "WEB-08 ID pelanggan PLN ditolak"]], "Payment dan Top-up"],
  ["WEB-09", "Riwayat dan detail", "GET /riwayat; /riwayat?detail={id}; POST /riwayat/cek-status; GET /riwayat/export", "Terlindungi penuh", "views/riwayat.ejs", "FR-23, FR-24, FR-26", "GET /history, /history/{id}, /history/export; POST /transfers/{id}/check-status", "Filter jenis, kata kunci, dan rentang tanggal; tautan CSV dan PDF; tabel transaksi; kartu detail dengan jurnal; tombol Cetak / PDF; tombol Cek status untuk transfer antarbank yang diproses.", "Detail → kartu pada halaman yang sama; Cek status memperbarui status.", "Ekspor mengikuti filter aktif. Dua puluh baris per halaman.", "WN-06, WN-12, WN-22", [["WEB-09-riwayat", "WEB-09 Riwayat", 400], ["WEB-09d-cek-status", "WEB-09 Detail setelah Cek status", 400]], "Transaction History"],
  ["WEB-10", "Profil", "GET/POST /profil", "Terlindungi penuh", "views/profil.ejs", "FR-27, FR-28 (sebagian)", "GET /me; POST /me/profile-change, /me/password", "Data profil (username, email, No. HP) dan Simpan Perubahan; formulir ubah kata sandi; informasi rekening.", "Simpan Perubahan → WEB-03 (OTP). Ubah Kata Sandi tetap di WEB-10 dengan pesan.", "Pengaturan PIN, sesi, dan notifikasi (FR-28, FR-29) belum tersedia di Web.", "WN-18, WN-19", [["WEB-10-profil", "WEB-10 Profil"]], "Profile"],
];
const MOD = ["Authentication", "Account", "Transfer", "Payment dan Top-up", "Transaction History", "Profile"];
const ORD = [...L].sort((a, b) => a[0].localeCompare(b[0]));

const body = [
  H1("BAB I  PENDAHULUAN"),
  H2("1.1 Tujuan dan Ruang Lingkup"),
  P("Dokumen ini memetakan halaman aplikasi Web resmi Diponegoro Digital Bank, yaitu aplikasi tim Web (digitalbanking-node v2.0.0; Node.js, Express 5, EJS). Isinya mencakup rute, hak akses, elemen tampilan, aksi pengguna, endpoint backend, keterkaitan dengan kebutuhan fungsional, dan tangkapan layar. Peramban menerima HTML dari EJS dan mengirim formulir ke server Node, lalu server Node memanggil REST API backend Spring Boot yang juga dipakai Android. Web tidak memiliki basis data sendiri."),
  P("Tangkapan layar diambil pada 22 November 2026 di Chrome dari aplikasi yang terhubung ke backend dengan data demo bersih (pengujian/web/node-layar.mjs). Nama, saldo, dan transaksi pada gambar adalah data simulasi."),
  H2("1.2 Legenda dan Konvensi"),
  table(["Istilah", "Arti"], [["WEB-01 … WEB-11", "Kode unik halaman Web; satu templat dapat dipakai beberapa tahap"], ["Publik", "Dapat dibuka tanpa login"], ["Login saja", "Memerlukan sesi; langkah pembukaan akun (e-KYC dan PIN)"], ["Terlindungi penuh", "Memerlukan sesi, e-KYC disetujui, dan PIN sudah dibuat"], ["Panah putus-putus", "Kembali ke templat OTP bersama pada alur ubah profil"]], [2400, 6670], { boldFirst: true, size: 18 }),
  CAP("Tabel 1.1 Istilah dan konvensi"),
  H1("BAB II  INVENTARIS HALAMAN"),
  P("Rute / tidak memiliki halaman; rute ini mengarahkan ke login atau ke langkah akun yang masih kurang."),
  table(["Kode", "Halaman", "Rute utama", "Akses", "FR"], ORD.map((r) => [r[0], r[1], r[2].split(";")[0].replace("GET/POST ", "").replace("GET ", ""), r[3], r[5]]), [900, 2100, 2570, 1500, 2000], { size: 17, center: [0] }),
  CAP("Tabel 2.1 Inventaris halaman Web"),
  table(["Menu samping", "Halaman"], [["Dashboard", "WEB-06"], ["Transfer", "WEB-07"], ["Pembayaran", "WEB-08"], ["Riwayat", "WEB-09"], ["Profil", "WEB-10"], ["Keluar", "GET /logout, lalu WEB-01"]], [3000, 6070], { boldFirst: true, size: 18 }),
  CAP("Tabel 2.2 Menu samping"),
  H1("BAB III  PETA NAVIGASI"),
  H2("3.1 Peta Navigasi Global"),
  P("Setelah OTP pendaftaran valid, server membuat sesi dan mengarahkan pengguna ke e-KYC, lalu ke pembuatan PIN, lalu ke Dashboard. Pengguna yang sudah lengkap langsung masuk ke Dashboard."),
  ...fig("n0_global.png", "Gambar 3.1 Peta navigasi global"),
  H2("3.2 Penjaga Halaman (Route Guard)"),
  P("Middleware pada routes/app.js memeriksa sesi, lalu fungsi nextStep memeriksa e-KYC dan PIN untuk Dashboard, Transfer, Pembayaran, Riwayat, dan Profil (WN-02, WN-23)."),
  ...fig("n1_guard.png", "Gambar 3.2 Pemeriksaan akses halaman terlindungi", 330),
  H2("3.3 Peta Navigasi per Modul"),
  ...fig("n2_auth.png", "Gambar 3.3 Authentication"),
  ...fig("n3_dashboard.png", "Gambar 3.4 Dashboard dan pintasan", 230),
  ...fig("n4_transfer.png", "Gambar 3.5 Tahap transfer pada rute /transfer"),
  ...fig("n5_bayar.png", "Gambar 3.6 Pembayaran dan Top Up", 520),
  ...fig("n6_riwayat.png", "Gambar 3.7 Riwayat, detail, dan ekspor", 520),
  ...fig("n7_profil.png", "Gambar 3.8 Profil dan OTP perubahan data", 520),
  H1("BAB IV  KARTU HALAMAN"),
  P("Setiap kartu memuat rute, akses, endpoint, elemen, aksi, kondisi, kasus uji pada STD-DDB-001, dan tangkapan layar."),
];
MOD.forEach((m, i) => {
  body.push(H2(`4.${i + 1} Modul ${m}`));
  L.filter((r) => r[12] === m).forEach((r) => {
    body.push(H3(`${r[0]} ${r[1]}`),
      table(["Butir", "Rincian"], [["Rute", r[2]], ["Akses", r[3]], ["Templat", r[4]], ["Kebutuhan", r[5]], ["Endpoint backend", r[6]], ["Elemen utama", r[7]], ["Aksi dan navigasi", r[8]], ["Kondisi dan batasan", r[9]], ["Kasus uji", r[10] + " (lulus, 22 Nov 2026)"]], [1900, 7170], { boldFirst: true, size: 17 }),
      CAP(`Tabel 4.${++tb} Kartu halaman ${r[0]}`),
      ...r[11].flatMap(([f, t, w]) => shot(f, t, w)), new Paragraph({ spacing: { after: 80 }, children: [] }));
  });
});
body.push(
  H1("BAB V  KETERLACAKAN DAN BATASAN"),
  H2("5.1 Keterlacakan Kebutuhan ke Halaman"),
  table(["FR", "Halaman", "Cakupan di Web"], [
    ["FR-01", "WEB-02, WEB-03", "Ada"], ["FR-02", "WEB-04", "Ada (simulasi)"], ["FR-03", "WEB-01", "Ada (password; biometrik khusus Android)"], ["FR-04", "WEB-05, WEB-07, WEB-08", "Ada"], ["FR-05", "WEB-01, WEB-11", "Ada; daftar sesi tersedia di Android"], ["FR-06", "WEB-06", "Saldo tampil; tombol sembunyikan belum ada"],
    ["FR-07 – FR-09, FR-13", "–", "Belum tersedia"], ["FR-10 – FR-12", "WEB-07", "Ada"], ["FR-14", "WEB-09", "Sebagian: cetak dari detail"], ["FR-15", "WEB-08", "Sebagian: contoh atau payload, tanpa kamera"], ["FR-16, FR-17", "–", "Belum tersedia"], ["FR-18", "WEB-08", "Pulsa dan e-wallet; paket data belum ada"], ["FR-19, FR-20", "WEB-08", "Ada"],
    ["FR-21, FR-22, FR-25, FR-29", "–", "Belum tersedia"], ["FR-23, FR-24, FR-26", "WEB-09", "Ada"], ["FR-27", "WEB-10, WEB-03", "Ada; foto profil belum ada"], ["FR-28", "WEB-10", "Sebagian: ubah kata sandi"],
  ], [2500, 2200, 4370], { boldFirst: true, size: 17 }),
  CAP("Tabel 5.1 Keterlacakan FR ke halaman Web"),
  P("Fitur yang belum tersedia di Web tersedia di aplikasi Android (UIM-DDB-001-AND)."),
  H2("5.2 Butir Terbuka Draf v0.7 yang Ditutup"),
  table(["ID", "Keputusan"], [
    ["D-01, D-02", "Aplikasi Node/Express/EJS adalah Web resmi dan memakai kode WEB-xx (SAD-DDB-001 v1.0, ADR-13)"], ["D-03", "Formulir pendaftaran tidak memuat nama; nama diisi pada e-KYC"], ["D-04", "Kolom login menerima username, email, atau nomor HP; label pada layar tetap \"Email / No. HP\""], ["D-05", "Swafoto berupa teks referensi (simulasi)"],
    ["D-06", "Nominal minimum, rekening tidak valid, dan blokir PIN terbukti lewat uji (WN-04, WN-15, WN-24); batas maksimum dan kunci akun adalah aturan backend"], ["D-07", "Paket data dicatat belum tersedia di Web"], ["D-08", "Tangkapan layar diambil dari digitalbanking-node v2.0.0 dan disisipkan pada Bab IV"], ["D-09", "Uji dengan backend asli: WN 25/25 dan LPN 3/3 (STD-DDB-001 v1.0)"], ["D-10", "Endpoint dicocokkan dengan backend/docs/openapi.json (73 operasi)"],
  ], [1500, 7570], { boldFirst: true, size: 17 }),
  CAP("Tabel 5.2 Penutupan butir terbuka"),
  H2("5.3 Keterbatasan"),
  B("Tampilan diperiksa pada lebar desktop (1280–1366 piksel) di Chrome, Edge, dan Firefox; tampilan ponsel belum diuji."),
  B("Sesi tersimpan di memori server; pengguna harus masuk lagi bila server Web dimulai ulang."),
  H2("Referensi"),
  ...NL(["SRS-DDB-001 v0.1, Software Requirements Specification.", "SAD-DDB-001 v1.0, Architectural Design.", "SDD-DDB-001-WEB v1.0, Detailed Design Web.", "STD-DDB-001 v1.0, Pengujian Sistem.", "README.md dan kode digitalbanking-node v2.0.0."]),
);
S.build(meta, body, OUT);
