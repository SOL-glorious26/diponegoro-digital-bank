// DVM-DDB-001-WEB – Developer Manual Web (Node/Express/EJS). Versi React (v0.5) ada di doc7w_react_v05.js.bak
// Usage: node doc7w.js <folder tangkapan layar Web> <folder digitalbanking-node> <out.docx> <logo.jpeg> <tanda_tangan.png>
const S = require("./ddbstyle");
const { D, P, H1, H2, H3, B, NL, CAP, NOTE, table, img, PW } = S;
const { Paragraph, TextRun, Table, TableRow, TableCell, WidthType, ShadingType, BorderStyle } = D;
const fs = require("fs"), path = require("path");
const [, , LAYAR, SRC, OUT, LOGO, SIGN] = process.argv;

const lines = (f) => fs.readFileSync(path.join(SRC, f), "utf8").split(/\r?\n/).length - 1;
function code(src) {
  const bd = { style: BorderStyle.SINGLE, size: 4, color: "808080" };
  return new Table({ width: { size: PW, type: WidthType.DXA }, columnWidths: [PW], rows: [new TableRow({ children: [new TableCell({
    width: { size: PW, type: WidthType.DXA }, borders: { top: bd, bottom: bd, left: bd, right: bd }, shading: { fill: "F7F7F7", type: ShadingType.CLEAR, color: "auto" }, margins: { top: 80, bottom: 80, left: 140, right: 140 },
    children: src.replace(/^\n/, "").replace(/\s+$/, "").split("\n").map((l) => new Paragraph({ spacing: { after: 0, line: 240 }, children: [new TextRun({ text: l.length ? l : " ", font: "Consolas", size: 18, color: "000000" })] })) })] })] });
}
const gap = () => new Paragraph({ spacing: { after: 100 }, children: [] });
const shot = (f, t, w = 520) => [img(path.join(LAYAR, f + ".png"), w, { alt: t }), CAP(t)];

const meta = {
  docLabel: "Dokumen 7 – Volume Web", title: "Developer Manual", subtitle: "Panduan Pengembang dan Keterangan Source Code – Aplikasi Web digitalbanking-node",
  code: "DVM-DDB-001-WEB", version: "1.0", statusShort: "Final",
  status: "Final v1.0 – diterbitkan Project Manager 22 November 2026; wewenang review dan persetujuan dijalankan Project Manager",
  module: "Developer Manual – Web (Node.js, Express 5, EJS)", createdBy: "Salomo Putra Glorious Hokoyoku (Project Manager)",
  updatedBy: "Salomo Putra Glorious Hokoyoku (Project Manager)", team: "Kelompok Jaran Goyang – Web Application Team (PIC volume menurut DWP: Steven Wibisana; pendukung: Nabila Puspa Annastasya)",
  refs: "SDD-DDB-001-WEB v1.0; SAD-DDB-001 v1.0 Bab VIII; DVM-DDB-001-BE v1.0; STD-DDB-001 v1.0; UIM-DDB-001-WEB v1.0; kode apps/web/digitalbanking-node v2.0.0",
  date: "22 November 2026", logo: LOGO,
  revisions: [
    ["0.5", "14 Nov 2026", "Salomo P.G. Hokoyoku", "Draf awal untuk aplikasi Web React (tidak dipakai lagi)."],
    ["1.0", "22 Nov 2026", "Salomo P.G. Hokoyoku", "Ditulis ulang untuk Web resmi tim Web (Node/Express/EJS): setup, konfigurasi, struktur, keterangan setiap berkas, pola pengembangan, integrasi API, pengujian, dan troubleshooting; langkah menjalankan dicoba pada 22 November 2026. Disetujui PM."],
  ],
  approval: { sign: SIGN, date: "22 Nov 2026" },
};

const body = [
  H1("BAB I  PENDAHULUAN"),
  H2("1.1 Tujuan dan Pembaca"),
  P("Manual ini membantu pengembang menyiapkan lingkungan, menjalankan, memahami, menguji, dan mengubah aplikasi Web Diponegoro Digital Bank. Pembacanya adalah pengembang Web, penguji, dan penyusun User Manual. Pembaca diasumsikan memahami dasar JavaScript, HTTP, dan HTML. Rancangan rinci (UML, workflow, pseudocode) ada pada SDD-DDB-001-WEB."),
  P("Pembagian kerja tim Web: Permata Surya Cahyana (backend Web), Rafi Fadhillah (front end dan backend Web), Steven Wibisana dan Nabila Puspa Annastasya (front end). Backend Spring Boot yang dipanggil Web dikerjakan Salomo dan dipakai bersama Android."),
  H2("1.2 Ringkasan Teknologi"),
  table(["Komponen", "Versi", "Fungsi"], [["Node.js", "18 atau lebih baru (dicoba pada 24.22)", "Menjalankan server Web"], ["express", "^5.1.0", "Rute dan middleware HTTP"], ["ejs", "^3.1.10", "Templat HTML sisi server"], ["express-session", "^1.18.1", "Sesi server (memori)"], ["dotenv", "^16.4.5", "Membaca berkas .env"], ["fetch", "bawaan Node.js", "Memanggil REST backend"]], [2200, 3370, 3500], { boldFirst: true, size: 18 }),
  CAP("Tabel 1.1 Teknologi aplikasi Web"),
  H2("1.3 Cara Kerja Singkat"),
  P("Web memakai pola backend for frontend (BFF). Peramban mengirim formulir ke server Node (port 3000) dan menerima HTML. Server Node memanggil REST /api/v1 pada backend Spring Boot (port 8080). Token JWT disimpan di sesi server, sehingga peramban hanya memegang cookie sesi dan CORS tidak diperlukan. Web tidak memiliki basis data."),
  H1("BAB II  PERSIAPAN LINGKUNGAN"),
  H2("2.1 Perangkat yang Dibutuhkan"),
  table(["Perangkat", "Wajib", "Keterangan"], [["Node.js 18+ dan npm", "Ya", "Periksa dengan node -v"], ["JDK 21+", "Ya (backend)", "Dapat memakai JDK bawaan Android Studio"], ["Git", "Disarankan", "Mengambil kode dari repositori"], ["Chrome, Edge, atau Firefox", "Ya", "Membuka aplikasi"]], [2800, 1700, 4570], { boldFirst: true, size: 18 }),
  CAP("Tabel 2.1 Perangkat pengembang"),
  H2("2.2 Menjalankan Backend"),
  code("cd code_area\\backend\npowershell -ExecutionPolicy Bypass -File .\\run-local.ps1 -H2"), gap(),
  P("Tunggu tulisan \"Backend jalan di http://localhost:8080\". Untuk data demo yang bersih, matikan backend, hapus folder code_area\\backend\\data, lalu jalankan lagi."),
  H2("2.3 Menjalankan Web"),
  code("cd code_area\\apps\\web\\digitalbanking-node\ncopy .env.example .env\nnpm.cmd install\nnpm.cmd start"), gap(),
  P("Buka http://localhost:3000 dan masuk dengan demo / Demo@1234 (PIN 123456). Akun lain: andi, budi, citra dengan password dan PIN yang sama."),
  ...shot("WEB-01-masuk", "Gambar 2.1 Halaman masuk setelah Web berjalan"),
  H2("2.4 Konfigurasi (.env)"),
  table(["Variabel", "Bawaan", "Kegunaan"], [["PORT", "3000", "Port Web"], ["BACKEND_URL", "http://localhost:8080/api/v1", "Alamat API backend"], ["SESSION_SECRET", "kosong (acak)", "Kunci cookie sesi; bila kosong, login hilang setiap server dimulai ulang"]], [2300, 3370, 3400], { boldFirst: true, size: 18 }),
  CAP("Tabel 2.2 Variabel konfigurasi"),
  P("Berkas .env, node_modules, dan database backend tidak boleh dimasukkan ke repositori."),
  H1("BAB III  STRUKTUR KODE"),
  H2("3.1 Struktur Folder"),
  code("digitalbanking-node/\n├── server.js            titik masuk: sesi, CSRF, flash, rute, error\n├── lib/                 backend.js (klien REST), helpers.js\n├── routes/              auth.js, app.js\n├── views/               11 templat EJS + partials/\n├── public/assets/       css, gambar, js/pwtoggle.js\n├── test/                backend.test.js\n└── backend-patch/       patch saldo awal (opsional, tidak dipasang)"), gap(),
  H2("3.2 Keterangan Berkas"),
  table(["Berkas", "Baris", "Keterangan"], [
    ["server.js", String(lines("server.js")), "Membuat aplikasi Express; cookie sesi httpOnly, SameSite=Lax, 2 jam; cookie perangkat ddb_device; pesan flash; pemeriksaan token CSRF pada setiap POST (403 bila salah); penanganan ApiError dan sesi berakhir"],
    ["lib/backend.js", String(lines("lib/backend.js")), "call(req, method, path, opts): satu-satunya klien backend. Menambah Authorization dan Idempotency-Key, batas waktu 20 detik, satu kali refresh saat 401, mengubah kesalahan menjadi ApiError"],
    ["lib/helpers.js", String(lines("lib/helpers.js")), "Format rupiah dan tanggal WIB, flash, regenerasi sesi, nextStep(user), ikon dan judul transaksi"],
    ["routes/auth.js", String(lines("routes/auth.js")), "Login, registrasi dan OTP, e-KYC, buat PIN, lupa sandi/PIN, logout"],
    ["routes/app.js", String(lines("routes/app.js")), "Penjaga halaman; Dashboard, Transfer, Pembayaran, Riwayat dan ekspor, Profil"],
    ["views/login.ejs, register.ejs, otp.ejs", "81", "Masuk, pendaftaran, dan OTP (dipakai pendaftaran dan ubah profil)"],
    ["views/ekyc.ejs, pin.ejs, lupa_sandi.ejs", "96", "e-KYC, pembuatan PIN, pemulihan"],
    ["views/dashboard.ejs, transfer.ejs", "101", "Ringkasan rekening; transfer dua tahap"],
    ["views/pembayaran.ejs, riwayat.ejs, profil.ejs", "181", "Enam tab pembayaran; daftar, filter, detail, cek status; data profil dan kata sandi"],
    ["views/partials/", "30", "header (menu samping), footer, csrf"],
    ["public/assets/css/style.css", String(lines("public/assets/css/style.css")), "Gaya global dan responsif"],
    ["public/assets/js/pwtoggle.js", String(lines("public/assets/js/pwtoggle.js")), "Ikon mata pada kolom kata sandi dan PIN"],
    ["test/backend.test.js", String(lines("test/backend.test.js")), "Uji lib/backend.js terhadap server tiruan"],
  ], [2900, 800, 5370], { boldFirst: true, center: [1], size: 17 }),
  CAP("Tabel 3.1 Keterangan berkas source code"),
  H2("3.3 Isi Sesi Server"),
  table(["Kunci", "Isi"], [["tokens", "accessToken dan refreshToken"], ["user", "Data pengguna untuk nextStep()"], ["csrf, flash", "Token CSRF dan pesan sekali tampil"], ["pendingTransfer, pendingPay", "Data konfirmasi beserta Idempotency-Key"], ["reg, rec, profilChange", "Data sementara pendaftaran, pemulihan, dan ubah profil"]], [3000, 6070], { boldFirst: true, size: 18 }),
  CAP("Tabel 3.2 Isi sesi server"),
  H1("BAB IV  HALAMAN DAN INTEGRASI API"),
  table(["Kode", "Halaman / rute", "Endpoint backend", "FR"], [
    ["WEB-01", "Masuk /login", "POST /auth/login", "FR-03"], ["WEB-02", "Daftar /register", "POST /auth/register", "FR-01"], ["WEB-03", "OTP /register/otp, /profil/otp", "POST /auth/register/verify-otp, /resend-otp; /me/profile-change/confirm", "FR-01, FR-27"],
    ["WEB-04", "e-KYC /ekyc", "GET, POST /auth/ekyc; GET /me", "FR-02"], ["WEB-05", "Buat PIN /pin", "POST /auth/pin", "FR-04"], ["WEB-06", "Dashboard", "GET /accounts/me; GET /history?size=5", "FR-06"],
    ["WEB-07", "Transfer", "GET /transfers/banks; POST /transfers/inquiry, /quote, /transfers", "FR-10 – FR-12"], ["WEB-08", "Pembayaran", "GET /topup/catalog, /qris/demo; POST /topup/quote, /topup/orders, /qris/scan, /qris/pay, /qris/simulate-incoming", "FR-15, FR-18 – FR-20 (sebagian)"],
    ["WEB-09", "Riwayat, /riwayat/export", "GET /history, /history/{id}, /history/export; POST /transfers/{id}/check-status", "FR-23, FR-24, FR-26"], ["WEB-10", "Profil", "GET /me; POST /me/profile-change, /me/password", "FR-27, FR-28 (sebagian)"], ["WEB-11", "Lupa sandi/PIN", "POST /auth/recovery/request, /verify, /reset", "FR-05"]], [900, 2300, 4370, 1500], { center: [0], size: 16 }),
  CAP("Tabel 4.1 Halaman, rute, dan endpoint (kode halaman menurut UIM-DDB-001-WEB v1.0)"),
  ...shot("WEB-07b-transfer-konfirmasi", "Gambar 4.1 Tahap konfirmasi transfer (WEB-07)"),
  H1("BAB V  POLA PENGEMBANGAN"),
  H2("5.1 Aturan yang Wajib Diikuti"),
  ...NL(["Semua pemanggilan backend lewat call() di lib/backend.js; jangan memakai fetch langsung di rute.", "Setiap formulir POST memuat partials/csrf.", "Transaksi keluar memakai dua tahap: konfirmasi, lalu PIN. Idempotency-Key dibuat sekali pada tahap konfirmasi dan dipakai ulang bila PIN salah.", "PIN dan kata sandi tidak disimpan di sesi.", "Tampilkan pesan kesalahan dari backend apa adanya; aturan bisnis tidak diduplikasi di Web.", "Nominal dibersihkan dengan digits() dan dikirim sebagai bilangan bulat rupiah."]),
  H2("5.2 Menambah Halaman Baru"),
  P("Contoh: halaman Kantong yang hanya membaca data (endpoint GET /accounts/pockets sudah ada di backend). Contoh berikut belum diterapkan pada kode."),
  code("// routes/app.js – tambahkan '/kantong' pada daftar router.use([...]) agar terlindungi\nrouter.get('/kantong', async (req, res) => {\n  const pockets = await call(req, 'GET', '/accounts/pockets');\n  res.render('kantong', { page: 'kantong', title: 'Kantong', pockets });\n});"), gap(),
  ...NL(["Tambahkan rute seperti contoh.", "Buat views/kantong.ejs dengan menyertakan partials/header dan partials/footer.", "Tambahkan menu pada views/partials/header.ejs.", "Tambahkan kasus pada pengujian/web/node-smoke.mjs."]),
  H2("5.3 Menangani Kesalahan"),
  P("Di dalam rute, tangkap ApiError dan tampilkan pesannya di formulir (lihat orRender di routes/app.js). Kode SESSION_ENDED dibiarkan naik ke server.js agar pengguna diarahkan ke halaman masuk dengan pesan sesi berakhir."),
  H1("BAB VI  PENGUJIAN"),
  table(["Uji", "Perintah", "Hasil 22 Nov 2026"], [["Klien API terhadap server tiruan", "node test/backend.test.js", "Lulus"], ["Alur inti terhadap backend asli (WN-01 – WN-14)", "node web/node-smoke.mjs (folder code_area/pengujian)", "14/14 lulus"], ["Uji lanjutan (WN-15 – WN-25)", "node web/node-ekstra.mjs", "11/11 lulus"], ["Chrome, Edge, Firefox (NF-09)", "node web/node-peramban.mjs", "24/24 pemeriksaan lulus"], ["Kinerja (NF-01 – NF-03)", "node performa/perf.mjs", "Lulus; Dashboard maks. 197 ms"], ["Lintas platform dengan Android (LPN-01 – LPN-03)", "node lintas-platform/lp-node.mjs", "3/3 lulus"], ["Tangkapan layar 11 halaman", "node web/node-layar.mjs", "Selesai; tanpa error JavaScript"]], [3300, 3870, 1900], { size: 17 }),
  CAP("Tabel 6.1 Pengujian aplikasi Web"),
  P("WN-21 memerlukan backend kedua (port 8081) dengan --ddb.security.access-token-minutes=1 dan Web kedua (port 3001). Tampilan ponsel belum memiliki uji otomatis. Rincian ada pada STD-DDB-001 bagian 5.4."),
  H1("BAB VII  TROUBLESHOOTING DAN KETERBATASAN"),
  table(["Gejala", "Penyebab", "Solusi"], [["\"Tidak dapat terhubung ke backend\"", "Backend belum jalan atau BACKEND_URL salah", "Jalankan backend; periksa .env"], ["403 \"Token keamanan tidak valid\"", "Formulir lama setelah server dimulai ulang", "Muat ulang halaman"], ["Kembali ke halaman masuk", "Sesi memori hilang atau refresh token ditolak", "Login ulang"], ["PIN diblokir", "PIN salah tiga kali", "Tunggu 5 menit atau pakai akun lain"], ["Port 3000 terpakai", "Aplikasi lain memakai port", "Ubah PORT di .env"], ["\"scripts is disabled\" di PowerShell", "ExecutionPolicy Restricted", "Pakai npm.cmd atau powershell -ExecutionPolicy Bypass"], ["Saldo awal akun baru Rp0", "Memakai salinan backend dengan DDB_OPENING_BALANCE=0", "Pakai backend bersama tanpa variabel itu (Rp5.000.000)"]], [2900, 2900, 3270], { size: 17 }),
  CAP("Tabel 7.1 Troubleshooting"),
  H2("7.1 Keterbatasan"),
  ...["Sesi disimpan di memori; tidak bertahan saat restart dan tidak dapat dipakai pada banyak proses.", "Belum ada kantong, kartu virtual, notifikasi, grafik saldo, penerima favorit, QRIS pribadi/dinamis, dan paket data.", "Top Up saldo memakai simulator dana masuk QRIS sehingga tercatat sebagai Terima dana QRIS di Android dan backend.", "Cookie belum memakai atribut secure karena berjalan di HTTP lokal.", "Logout memakai GET; sebaiknya diganti POST dengan token CSRF."].map((t) => B(t)),
  H2("Referensi"),
  ...NL(["SDD-DDB-001-WEB v1.0, Detailed Design Web.", "STD-DDB-001 v1.0, Pengujian Sistem.", "UIM-DDB-001-WEB v1.0.", "README.md dan kode digitalbanking-node v2.0.0.", "backend/docs/API.md dan openapi.json."]),
];
S.build(meta, body, OUT);
