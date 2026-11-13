const path = require("path");
const S = require("./ddbstyle");
const { D, P, H1, H2, H3, B, NL, CAP, NOTE, table, kv, img, cell, PW } = S;
const { Paragraph, TextRun, ImageRun, Table, TableRow, AlignmentType, WidthType } = D;
const fs = require("fs");

const [, , FIG, OUT, LOGO, SIGN] = process.argv;
const fig = (n) => path.join(FIG, n);
const rp = (n) => "Rp" + Math.round(n).toLocaleString("id-ID");
const f2 = (n) => n.toLocaleString("id-ID", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

// ---------------- COCOMO II (Post-Architecture, nominal drivers)
const A = 2.94, Bc = 0.91, SF = 18.97, E = Bc + 0.01 * SF, C = 3.67, Dd = 0.28, F = Dd + 0.2 * (E - Bc), EAF = 1.0;
const sizes = { be: 4.699, sql: 0.218, and: 9.197, web: 0.073, webAdd: 4.0, intAdd: 0 };
const kReal = sizes.be + sizes.sql + sizes.and + sizes.web;
const kDone = kReal + sizes.webAdd + sizes.intAdd;
const coc = (k) => { const pm = A * Math.pow(k, E) * EAF; const t = C * Math.pow(pm, F); return { pm, t, n: pm / t }; };
const R1 = coc(kReal), R2 = coc(kDone);
const RATE = 10_000_000;
const nonLabor = [
  ["Sewa Cloud/VPS untuk staging dan demo (rencana Fase 4.5)", "3 bulan × Rp150.000", 450_000],
  ["Domain untuk akses HTTPS (opsional)", "1 tahun", 200_000],
  ["Internet/kuota anggota", "8 orang × 3 bulan × Rp100.000", 2_400_000],
  ["Perangkat (laptop, ponsel Android) milik anggota", "tidak dibebankan", 0],
  ["Perangkat lunak (Android Studio, JDK, Spring Boot, PostgreSQL, Nginx, draw.io)", "lisensi gratis/sumber terbuka", 0],
];
const nonLaborTotal = nonLabor.reduce((a, r) => a + r[2], 0);
const labor = R2.pm * RATE;
const contingency = 0.10 * (labor + nonLaborTotal);
const grand = labor + nonLaborTotal + contingency;

// ---------------- Front matter meta
const meta = {
  docLabel: "Dokumen 1", title: "Project Document", subtitle: "Deskripsi Proyek, Jadwal, Rencana Pengujian, Manajemen Risiko, dan Estimasi Biaya",
  code: "PRJ-DDB-001", version: "0.9.1", statusShort: "Draf Lengkap untuk Review",
  status: "Draf lengkap – menunggu peer review (Steven Wibisana) dan persetujuan Project Manager",
  module: "Project Document – Diponegoro Digital Bank", createdBy: "Salomo Putra Glorious Hokoyoku (Project Manager)",
  updatedBy: "Salomo Putra Glorious Hokoyoku (Project Manager)", team: "Kelompok Jaran Goyang – Mobile Application Team dan Web Application Team",
  refs: "SRS-DDB-001 v0.1; Project Brief & Scope Document v1.0; MDTA-DDB-001 v0.1; DWP-DDB-001 v1.0; repositori kode per 11 November 2026",
  date: "11 November 2026", logo: LOGO,
  revisions: [
    ["0.1", "11 Nov 2026", "Salomo P.G. Hokoyoku", "Outline sesuai sembilan butir template Dokumen 1."],
    ["0.9", "11 Nov 2026", "Salomo P.G. Hokoyoku", "Draf lengkap: deskripsi proyek, Gantt chart 15 Sep–23 Nov 2026, model dan rencana pengujian, register risiko R1–R15, estimasi biaya COCOMO II."],
    ["0.9.1", "11 Nov 2026", "Salomo P.G. Hokoyoku", "Pembaruan setelah integrasi Android–backend (Fase 5) 11 Nov sore: status Android online, Gantt (integrasi selesai), jumlah test (63/58 Android, 45 backend), 73 operasi API, risiko R12 termitigasi, ukuran kode COCOMO diukur ulang."],
  ],
};

// ---------------- Signature page
const sigW = [PW / 2, PW / 2];
const sigImg = fs.readFileSync(SIGN);
const para = (t, o = {}) => new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: o.after ?? 0 }, children: [new TextRun({ text: t, font: S.FONT, size: 24, bold: o.bold, color: "000000" })] });
const sigTable = new Table({
  width: { size: PW, type: WidthType.DXA }, columnWidths: sigW,
  rows: [new TableRow({ children: [
    cell("", sigW[0], { noBorder: true, children: [para("Salatiga, 11 November 2026"), para("Disusun oleh,"), para("Project Manager", { after: 60 }),
      new Paragraph({ alignment: AlignmentType.CENTER, children: [new ImageRun({ type: "png", data: sigImg, transformation: { width: 110, height: 111 }, altText: { title: "Tanda tangan", description: "Tanda tangan Project Manager", name: "ttd" } })] }),
      para("Salomo Putra Glorious Hokoyoku", { bold: true }), para("NIM 622024022")] }),
    cell("", sigW[1], { noBorder: true, children: [para(" "), para("Disetujui oleh,"), para("Dosen Pengampu", { after: 60 }),
      new Paragraph({ spacing: { before: 1500 }, alignment: AlignmentType.CENTER, children: [new TextRun({ text: "(_______________________________)", font: S.FONT, size: 24 })] }),
      para("NIP/NIDN: ____________________")] }),
  ] })],
});
const signPage = [
  new Paragraph({ pageBreakBefore: true, alignment: AlignmentType.CENTER, spacing: { after: 360 }, children: [new TextRun({ text: "LEMBAR PENGESAHAN", font: S.FONT, size: 32, bold: true })] }),
  P("Dokumen Project (PRJ-DDB-001) untuk proyek **End-to-End Development of Greenfield Retail Digital Banking Platform (Web & Mobile Apps)** – PT Diponegoro Digital Bank, yang disusun oleh Kelompok Jaran Goyang pada mata kuliah Software Engineering, Universitas Kristen Satya Wacana, telah diperiksa dan disahkan sebagai berikut.", { after: 480 }),
  sigTable,
];

// ---------------- Body
const body = [
  ...signPage,

  // BAB I
  H1("BAB I  DESKRIPSI PROJECT"),
  H2("1.1 Latar Belakang"),
  P("Masyarakat semakin terbiasa memenuhi kebutuhan finansial melalui ponsel. Meski demikian, pengguna harian, khususnya mahasiswa, profesional muda, dan pelaku usaha mikro, masih menghadapi aplikasi perbankan dengan antarmuka yang membingungkan, waktu muat yang lama, dan jadwal pemeliharaan yang mengganggu. Diponegoro Corporation (holding company) merencanakan anak perusahaan **PT Diponegoro Digital Bank**, yaitu bank digital ritel yang dibangun dari nol (greenfield) dan menempatkan kecepatan serta keandalan sebagai keunggulan utama."),
  H2("1.2 Pernyataan Masalah"),
  table(["No", "Masalah", "Dampak bagi pengguna", "Arah solusi"], [
    ["1", "Pengguna membutuhkan layanan perbankan harian untuk transaksi kecil hingga besar tanpa harus datang ke ATM atau kantor bank.", "Waktu terbuang, transaksi tertunda, biaya dan usaha tambahan.", "Layanan transaksi penuh secara digital: transfer, QRIS, top-up, riwayat, dan pengelolaan simpanan."],
    ["2", "Aplikasi bank yang ada kurang andal: UI/UX membingungkan, loading lama, sebagian fitur lambat, pemeliharaan pada waktu yang tidak tepat.", "Frustrasi, hilangnya kepercayaan, transaksi gagal pada saat mendesak.", "Performa sebagai prioritas desain, UI/UX sederhana, pemeliharaan terjadwal di luar jam sibuk dengan pemberitahuan."],
  ], [500, 3000, 2500, 3070], { center: [0] }),
  CAP("Tabel 1.1 Pernyataan masalah (sumber: Project Brief v1.0 §3.2)"),
  H2("1.3 Tujuan Proyek"),
  P("Tujuan proyek adalah membangun platform digital banking yang memungkinkan pengguna untuk:"),
  ...NL([
    "melihat saldo (dengan opsi menyembunyikan saldo), grafik perubahan saldo, dan jenis transaksi;",
    "mentransfer dana mulai Rp5.000 ke sesama PT Diponegoro Digital Bank maupun bank dalam negeri yang terdaftar;",
    "membayar dengan memindai QRIS dan menampilkan QRIS pribadi untuk menerima dana;",
    "membeli pulsa, paket data, token listrik, dan membayar tagihan rumah tangga;",
    "membuat dan mengelola virtual card untuk transaksi domestik maupun internasional;",
    "mengelola profil, keamanan, dan notifikasi;",
    "melihat riwayat transaksi lengkap dan dashboard pemasukan/pengeluaran bulanan;",
    "mengelola beberapa kantong simpanan beserta fitur locking.",
  ]),
  P("Indikator keberhasilan proyek mengikuti target usulan SRS Bagian 4 (NFR-01 s.d. NFR-12), antara lain waktu muat layar utama ≤ 2 detik, respons API baca (p95) ≤ 500 ms, transfer dari konfirmasi PIN sampai sukses ≤ 3 detik, ketersediaan ≥ 99,5%, dan cakupan unit test logika inti backend ≥ 70%."),
  H2("1.4 Target Pengguna"),
  table(["Segmen", "Karakteristik", "Fitur kunci"], [
    ["Mahasiswa", "Usia ±18–24 tahun; transaksi kecil namun sering (jajan, kos, patungan).", "Transfer, QRIS, riwayat, kantong tabungan"],
    ["Profesional", "Berpenghasilan rutin; tagihan bulanan; sering bertransaksi antarbank dan daring.", "Transfer antarbank, kantong dan locking, virtual card, top-up, dashboard"],
    ["UMKM", "Pemilik usaha mikro dan kecil; menerima pembayaran pelanggan dan membayar pemasok.", "Tampil QRIS, riwayat dan ekspor, kantong usaha, dashboard"],
  ], [1800, 4070, 3200], { boldFirst: true }),
  CAP("Tabel 1.2 Segmen pengguna (sumber: SRS 2.3)"),
  H2("1.5 Ruang Lingkup"),
  P("Ruang lingkup fungsional ditetapkan dalam SRS-DDB-001 v0.1 sebagai **29 kebutuhan fungsional** (15 Must, 11 Should, 3 Could) pada delapan modul bisnis. Seluruh data bersifat simulasi, dan integrasi eksternal memakai simulator melalui lapisan adapter."),
  table(["No", "Modul", "Cakupan", "FR", "Platform"], [
    ["1", "Authentication", "Registrasi + OTP, e-KYC (simulasi), login (+ biometrik di mobile), PIN 6 digit, sesi, pemulihan", "FR-01 – FR-05", "Mobile & Web"],
    ["2", "Account", "Saldo (sembunyikan/tampilkan), grafik saldo, kantong simpanan, locking", "FR-06 – FR-09", "Mobile & Web"],
    ["3", "Transfer", "Sesama bank, antarbank, validasi rekening, favorit, bukti transfer", "FR-10 – FR-14", "Mobile & Web"],
    ["4", "Payment (QRIS)", "Pindai QRIS (mobile), QRIS pribadi, QRIS dinamis", "FR-15 – FR-17", "Mobile; Web tanpa pindai"],
    ["5", "Top-up", "Pulsa, paket data, token listrik, tagihan rumah tangga", "FR-18 – FR-20", "Mobile & Web"],
    ["6", "Virtual Card", "Buat kartu, detail dengan PIN, bekukan/aktifkan, limit", "FR-21 – FR-22", "Mobile & Web"],
    ["7", "Transaction History & Dashboard", "Riwayat + filter, detail, dashboard bulanan, ekspor PDF/CSV (Web)", "FR-23 – FR-26", "Mobile & Web; ekspor Web"],
    ["8", "Profile & Notification", "Profil + OTP, pengaturan keamanan, notifikasi transaksi", "FR-27 – FR-29", "Mobile & Web"],
  ], [450, 1900, 3770, 1450, 1500], { center: [0] }),
  CAP("Tabel 1.3 Modul bisnis dan cakupan kebutuhan fungsional"),
  P("**Di luar ruang lingkup rilis ini:** produk kredit/pinjaman dan investasi nyata; kartu debit fisik dan penarikan tunai ATM; integrasi produksi dengan core banking, jaringan antarbank, QRIS, dan penyedia tagihan; dukungan multibahasa; aplikasi iOS (proyek mengembangkan Native Android walaupun SRS menyebut Android dan iOS); serta fitur diferensiasi (Budgeting Tracker, Smart Split Bill, Round-Up, Quest Keuangan) yang belum dirinci di SRS."),
  H2("1.6 Arsitektur dan Teknologi"),
  P("Aplikasi Android dan aplikasi Web memakai **satu backend bersama** melalui API Gateway, sehingga logika bisnis tidak terduplikasi. Backend dibangun sebagai modular monolith dengan satu paket per modul SRS. Rincian arsitektur dibahas dalam Dokumen 2 (SAD-DDB-001)."),
  table(["Lapisan", "Teknologi yang dipakai", "Status per 11 Nov 2026"], [
    ["Aplikasi Android", "Kotlin 2.2, Jetpack Compose, MVVM, Repository Pattern; Retrofit/OkHttp; minSdk 24, targetSdk 37", "8 modul; varian demo (data simulasi) dan online (terhubung backend lewat REST)"],
    ["Aplikasi Web", "Prototipe awal berbasis React; framework final menunggu konfirmasi tim Web", "Prototipe antarmuka; belum terhubung ke backend"],
    ["API Gateway", "Nginx 1.27 (rate limiting, CORS)", "Konfigurasi tersedia; belum diuji"],
    ["Backend", "Java 21, Spring Boot 4.1.2, Spring Security + JWT, Spring Data JPA, OpenAPI", "Delapan modul selesai; 73 operasi API"],
    ["Basis data", "PostgreSQL 17/18 + migrasi Flyway (17 tabel); H2 untuk pengujian", "Skema tersedia; uji pada PostgreSQL lokal menunggu konfigurasi"],
    ["Deployment", "Docker, docker-compose (gateway + backend + PostgreSQL), Cloud/VPS", "Berkas tersedia; belum dijalankan"],
  ], [1900, 4170, 3000], { boldFirst: true }),
  CAP("Tabel 1.4 Teknologi dan status per lapisan"),
  H2("1.7 Organisasi Tim"),
  P("Tim terdiri atas delapan anggota dalam dua kelompok platform. Pembagian modul Android mengikuti README tim Mobile (submit_mobile/README.md). Seluruh Backend dan Database dikerjakan oleh Salomo."),
  table(["NIM", "Nama", "Tim", "Peran pengembangan", "PIC dokumentasi"], [
    ["622024022", "Salomo Putra Glorious Hokoyoku", "Mobile", "Project Manager; Android: Authentication, Virtual Card, integrasi aplikasi; seluruh Backend & Database", "Dok. 1, 2, 3, 4 (Backend), 7 (Android, Backend); final reviewer"],
    ["622024019", "Cecilia Adelaide Penlaana", "Mobile", "Android: Transfer, Transaction History & Dashboard", "Dok. 5 (Android), Dok. 8 (pendukung)"],
    ["622024006", "Gracia Riyanto", "Mobile", "Android: Payment (QRIS), Profile & Notification", "Dok. 4 (Android)"],
    ["6220204010", "Grace", "Mobile", "Android: Account, Top-up", "Dok. 9 (Android)"],
    ["622024008", "Nabila Puspa Annastasya", "Web", "Web Developer", "Dok. 5 (Web), Dok. 7 (Web, pendukung), Dok. 8 (pendukung)"],
    ["622024003", "Steven Wibisana", "Web", "Web Developer", "Dok. 6, Dok. 7 (Web)"],
    ["622024005", "Rafi Fadhillah", "Web", "Web Developer", "Dok. 4 (Web), Dok. 8"],
    ["622024016", "Permata Surya Cahyana", "Web", "Web Developer", "Dok. 9 (Web), Dok. 8 (pendukung)"],
  ], [1150, 2050, 800, 2770, 2300], { size: 18 }),
  CAP("Tabel 1.5 Organisasi tim, peran pengembangan, dan PIC dokumentasi"),
  NOTE("Pembagian halaman/modul pada tim Web belum dilaporkan; penugasan rinci tim Web akan ditambahkan setelah konfirmasi."),
  H2("1.8 Metodologi"),
  P("Proyek memakai Agile Scrum yang disesuaikan dengan kalender akademik: perencanaan per iterasi, sinkronisasi singkat, review hasil, dan retrospektif. Backlog diprioritaskan dengan metode MoSCoW sesuai SRS. Kebutuhan Must dikerjakan lebih dahulu, sedangkan Should dan Could dikerjakan bila kapasitas memungkinkan."),
  H2("1.9 Status Pengembangan per 11 November 2026"),
  P("Status berikut disusun berdasarkan bukti di repositori proyek, bukan perkiraan."),
  table(["Komponen", "Hasil yang telah dicapai", "Bukti", "Pekerjaan tersisa"], [
    ["Android", "8 modul (FR-01–FR-25, FR-27–FR-29); varian demo dan online; integrasi REST ke backend selesai 11 Nov; di emulator: login, PIN, Beranda dari server, transfer ke nasabah lain terverifikasi di akun penerima", "63 test demo dan 58 test online lulus, termasuk uji end-to-end ke backend (11 Nov 2026)", "Uji di ponsel fisik (kamera QRIS, push, berbagi berkas); uji dua perangkat"],
    ["Backend", "8 modul + Financial Transaction Core; 73 operasi API (termasuk verifikasi PIN); data demo; bug lost update saldo pada transfer paralel ditemukan dan diperbaiki", "45 test integrasi lulus (11 Nov 2026); openapi.json dan API.md dibangkitkan ulang", "Laporan cakupan JaCoCo; uji pada PostgreSQL lokal"],
    ["API Gateway & Docker", "nginx.conf, Dockerfile, docker-compose.yml, .env.example", "Berkas di repositori", "Uji menjalankan (Docker belum terpasang); HTTPS; deploy Cloud/VPS"],
    ["Web", "Prototipe antarmuka awal (login, dashboard, transfer, pembayaran, riwayat, profil) dengan data tiruan", "main.jsx (7 Okt 2026)", "Konfirmasi framework, integrasi API, cakupan FR Web"],
    ["Dokumentasi", "Rencana kerja dokumentasi (DWP-DDB-001); Dokumen 1 dan 2 (draf)", "Folder documents_area", "Dokumen 3–9 sesuai jadwal DWP"],
  ], [1400, 3170, 2300, 2200], { boldFirst: true, size: 18 }),
  CAP("Tabel 1.6 Status pengembangan berbasis bukti"),

  // BAB II Gantt
  H1("BAB II  GANTT CHART"),
  P("Proyek berlangsung **15 September 2026 sampai 23 November 2026** (10 minggu). Jadwal indikatif 16 minggu pada Project Brief v1.0 §10.2 disesuaikan dengan rentang ini. Gantt chart membedakan aktivitas yang **sudah terealisasi dan berbukti** (blok penuh) dari aktivitas **yang direncanakan atau sedang berjalan** (blok arsir). Tanggal realisasi diambil dari tanggal dokumen dan berkas di repositori. Tanggal mulai aktivitas awal yang tidak memiliki bukti berkas memakai tanggal rencana."),
  img(fig("d1_gantt.png"), 605, { alt: "Gantt chart proyek" }),
  CAP("Gambar 2.1 Gantt chart proyek, 15 September – 23 November 2026"),
  table(["No", "Aktivitas", "Mulai", "Selesai", "Status", "Bukti / keterangan"], [
    ["1", "Inisiasi dan riset kebutuhan", "15 Sep", "2 Okt", "Selesai", "Project Brief v1.0 (2 Okt 2026)"],
    ["2", "Analisis kebutuhan", "22 Sep", "2 Okt", "Selesai", "SRS-DDB-001 v0.1 (2 Okt 2026)"],
    ["3", "Perencanaan teknis dan alokasi modul", "3 Okt", "4 Okt", "Selesai", "MDTA-DDB-001 v0.1 (4 Okt 2026)"],
    ["4", "Desain UML per modul + prototipe awal", "3 Okt", "7 Okt", "Selesai", "Berkas UML per PIC (7 Okt 2026)"],
    ["5", "Pengembangan modul Android per PIC", "3 Okt", "7 Okt", "Selesai (versi awal)", "jarang-goyang-mobile-app.zip (7 Okt 2026)"],
    ["6", "Integrasi aplikasi Android (8 modul)", "8 Okt", "10 Nov", "Selesai", "54 test lulus; uji emulator (10 Nov 2026)"],
    ["7", "Pengembangan aplikasi Web", "3 Okt", "20 Nov", "Berjalan", "Prototipe awal (7 Okt 2026); progres belum dilaporkan"],
    ["8", "Pengembangan Backend & Database", "10 Nov", "11 Nov", "Selesai", "44 test lulus; 72 operasi API (11 Nov 2026)"],
    ["9", "Integrasi Android – Backend", "11 Nov", "11 Nov", "Selesai", "Flavor online; 58 test online lulus termasuk uji live (11 Nov 2026)"],
    ["10", "API Gateway dan deployment", "12 Nov", "18 Nov", "Rencana", "DAFTAR_PEKERJAAN Fase 4"],
    ["11", "Pengujian sistem", "17 Nov", "21 Nov", "Rencana", "Dokumen 6 (PIC Steven)"],
    ["12", "Dokumentasi 9 kategori", "11 Nov", "23 Nov", "Berjalan", "DWP-DDB-001; pengumpulan 24 Nov 2026 23.59 WIB"],
  ], [450, 2700, 900, 900, 1500, 2620], { center: [0, 2, 3], size: 18 }),
  CAP("Tabel 2.1 Rincian aktivitas Gantt chart"),
  P("**Milestone:** Project Brief dan SRS (2 Okt), pengumpulan progres awal (7 Okt), backend 8 modul (11 Nov), build baseline dokumentasi (18 Nov), proyek selesai (23 Nov), dan pengumpulan dokumentasi (24 Nov 2026 pukul 23.59 WIB)."),

  // BAB III Testing
  H1("BAB III  RENCANA TESTING DAN MODEL PROSES TESTING"),
  H2("3.1 Model Proses Testing"),
  P("Proyek memakai **V-Model** sebagai model proses pengujian. Pada V-Model, setiap tahap spesifikasi dan desain dipasangkan dengan satu tingkat pengujian yang memverifikasinya. Model ini dipilih karena tiga alasan. Pertama, SRS sudah memuat kriteria penerimaan per FR sehingga dapat langsung menjadi dasar uji penerimaan. Kedua, dokumentasi wajib (Dokumen 2, 3, dan 4) memetakan secara alami ke pengujian sistem dan integrasi. Ketiga, sifat domain perbankan menuntut keterlacakan antara kebutuhan, desain, dan hasil uji. Pengujian unit tetap dijalankan setiap iterasi Scrum sehingga V-Model diterapkan secara iteratif, bukan berurutan kaku."),
  img(fig("d1_vmodel.png"), 560, { alt: "V-Model pengujian" }),
  CAP("Gambar 3.1 V-Model pengujian Diponegoro Digital Bank"),
  H2("3.2 Tingkat dan Jenis Pengujian"),
  table(["Tingkat", "Tujuan", "Teknik dan alat", "Penanggung jawab"], [
    ["Unit", "Memverifikasi logika per kelas/use case: ledger, PIN, OTP, aturan transfer, QRIS codec", "JUnit (Android, backend); Robolectric untuk UI Android", "Pengembang modul"],
    ["Integrasi", "Memverifikasi alur API end-to-end di backend dan integrasi klien–backend", "Spring Boot Test + H2 (MockMvc); uji klien dengan MockWebServer (rencana)", "Salomo (backend); Steven (kasus uji)"],
    ["Sistem", "Memverifikasi seluruh FR dan NFR terpilih pada lingkungan mendekati produksi", "Kasus uji manual + Swagger UI; emulator dan ponsel; peramban", "Steven (PIC Dok. 6); eksekusi Gracia, Grace, Permata"],
    ["Penerimaan (UAT)", "Memvalidasi bahwa sistem memenuhi kriteria penerimaan SRS dari sudut pandang pengguna", "Skenario demo (Dok. 8) dengan akun dan data demo", "Rafi (PIC Dok. 8); disaksikan dosen"],
  ], [1400, 3000, 2670, 2000], { boldFirst: true, size: 18 }),
  CAP("Tabel 3.1 Tingkat pengujian"),
  P("Jenis pengujian yang dilakukan pada tingkat sistem:"),
  B("**Fungsional:** seluruh kriteria penerimaan FR-01 s.d. FR-29, termasuk alur alternatif UC-01 A1–A6."),
  B("**Negatif dan batas:** nominal di bawah Rp5.000, OTP salah/kedaluwarsa, PIN salah tiga kali, kartu beku, dana kantong terkunci."),
  B("**Integritas data:** tidak ada debit ganda pada permintaan berulang (BR-04), pengembalian dana otomatis (BR-05), konsistensi saldo antara Android dan Web."),
  B("**Keamanan dasar:** akses tanpa token ditolak, logout semua perangkat membatalkan token, rate limit pada login (NFR-06)."),
  B("**Performa dasar:** waktu respons API baca dan transfer terhadap target NFR-02 dan NFR-03 (pengukuran sederhana, bukan uji beban penuh)."),
  B("**Kompatibilitas:** Android minSdk 24 sampai versi terbaru; peramban Chrome, Firefox, Edge, Safari (NFR-10)."),
  H2("3.3 Cakupan Pengujian"),
  P("**Diuji:** delapan modul bisnis pada Android, Web, dan Backend; Financial Transaction Core; API Gateway (setelah dapat dijalankan). **Tidak diuji pada rilis ini:** integrasi produksi dengan jaringan antarbank, QRIS, dan biller (digantikan simulator); aplikasi iOS; uji beban skala produksi; uji penetrasi formal; fitur diferensiasi."),
  H2("3.4 Lingkungan dan Data Uji"),
  table(["Lingkungan", "Konfigurasi", "Kegunaan"], [
    ["Test otomatis backend", "Profil test, H2 in-memory mode PostgreSQL", "45 test integrasi API (±40 detik)"],
    ["Pengembangan lokal", "Profil dev (PostgreSQL lokal) atau h2 (berkas H2), run-local.ps1", "Uji manual API melalui Swagger UI"],
    ["Kontainer", "docker-compose: gateway Nginx + backend + PostgreSQL 17", "Uji sistem dan demo (setelah Docker terpasang)"],
    ["Android", "Emulator Pixel 8 (Android 37) dan ponsel Android fisik", "Uji UI, kamera QRIS, notifikasi, berbagi berkas"],
    ["Web", "Peramban modern di laptop anggota tim", "Uji halaman Web (setelah terintegrasi)"],
  ], [2300, 4070, 2700], { boldFirst: true, size: 18 }),
  CAP("Tabel 3.2 Lingkungan pengujian"),
  P("Data uji bersifat simulasi. Akun demo `demo` / `Demo@1234` dengan PIN `123456` (rekening 1400123456) dan nasabah ANDI, BUDI, CITRA tersedia otomatis. Simulator menyediakan data uji khusus: rekening BRI 9999999999 menghasilkan penolakan antarbank (A6), Mandiri 8888888888 menghasilkan timeout (A5), dan NIK atau ID pelanggan berakhiran 0000 menghasilkan penolakan e-KYC atau 'pelanggan tidak ditemukan'."),
  H2("3.5 Kriteria Masuk dan Keluar"),
  table(["Kriteria", "Uraian"], [
    ["Masuk (pengujian sistem)", "Build baseline dokumentasi tersedia (target 18 Nov 2026); seluruh test unit/integrasi lulus; kasus uji Dokumen 6 sudah direview; akun dan data uji siap."],
    ["Keluar (pengujian sistem)", "Seluruh kasus uji FR Must telah dieksekusi; tidak ada defect berkategori Kritis atau Tinggi yang terbuka; defect lain tercatat beserta keputusan penanganannya; laporan hasil disetujui Project Manager."],
    ["Penangguhan", "Lingkungan uji tidak tersedia, atau ditemukan defect Kritis yang memblokir lebih dari satu modul."],
  ], [2500, 6570], { boldFirst: true }),
  CAP("Tabel 3.3 Kriteria masuk, keluar, dan penangguhan"),
  H2("3.6 Manajemen Defect"),
  P("Setiap defect dicatat dengan ID, modul, platform, langkah reproduksi, hasil yang diharapkan dan aktual, bukti, serta tingkat keparahan: **Kritis** (kehilangan dana, data rusak, aplikasi tidak dapat dipakai), **Tinggi** (fungsi Must gagal), **Sedang** (fungsi Should/Could gagal atau ada solusi sementara), dan **Rendah** (kosmetik). Defect Kritis dan Tinggi wajib diperbaiki dan diuji ulang sebelum demo."),
  H2("3.7 Jadwal dan Status Pengujian Saat Ini"),
  P("Desain kasus uji dijadwalkan 13–17 November 2026, eksekusi 19–22 November 2026, dan laporan hasil pada Dokumen 6. Sampai 11 November 2026 telah tersedia bukti pengujian berikut: **63 test Android varian demo** dan **58 test varian online lulus** (termasuk uji end-to-end ke backend sungguhan: registrasi sampai transfer, QRIS, top-up, kartu, notifikasi), **45 test integrasi backend lulus** (termasuk uji transfer paralel yang menemukan dan memperbaiki bug lost update saldo), serta uji manual pada emulator untuk varian demo dan online. Pengujian sistem lintas platform Android–Web belum dapat dimulai karena aplikasi Web belum terintegrasi."),

  // BAB IV Risk
  H1("BAB IV  MANAJEMEN RISIKO"),
  H2("4.1 Metode"),
  P("Manajemen risiko dilakukan dalam empat langkah: **identifikasi** dari Project Brief §12 dan kondisi proyek terkini; **analisis** dengan skala peluang dan dampak 1–3 (skor = peluang × dampak); **mitigasi** dengan tindakan dan pemilik yang jelas; serta **pemantauan** yang ditinjau Project Manager setiap minggu sampai proyek selesai. Risiko R1–R9 berasal dari Project Brief. R10–R15 adalah risiko baru yang teridentifikasi per 11 November 2026."),
  H2("4.2 Register Risiko"),
  table(["ID", "Risiko", "P", "D", "Skor", "Mitigasi", "Pemilik", "Status"], [
    ["R1", "Pembengkakan ruang lingkup akibat fitur diferensiasi", "2", "3", "6", "MoSCoW; fitur diferensiasi ditunda; perubahan lingkup lewat persetujuan PM", "Salomo", "Terkendali"],
    ["R2", "Integrasi eksternal (QRIS, antarbank, pulsa/PLN) kompleks", "1", "2", "2", "Simulator: InterbankSimulator, QRIS codec EMVCo, katalog top-up", "Salomo", "Termitigasi"],
    ["R3", "Kebocoran atau penyalahgunaan data sensitif", "1", "3", "3", "Data simulasi; BCrypt; AES-GCM untuk data kartu; rahasia lewat environment variable", "Salomo", "Termitigasi"],
    ["R4", "Inkonsistensi transaksi (debit ganda, race condition)", "1", "3", "3", "Ledger double-entry atomik, kunci baris, kunci idempotensi; uji transfer paralel", "Salomo", "Termitigasi"],
    ["R5", "Performa tidak memenuhi target NFR", "2", "2", "4", "Paginasi, notifikasi asinkron; pengukuran di Dokumen 6", "Steven", "Terbuka"],
    ["R6", "Keterbatasan waktu karena beban akademik", "3", "2", "6", "Jadwal DWP dengan penyangga; prioritas FR Must", "Salomo", "Terbuka"],
    ["R7", "Kesenjangan keterampilan anggota", "2", "2", "4", "Modul awal yang tidak dapat digabung diintegrasikan ulang oleh PM; berbagi pengetahuan lewat Developer Manual", "Salomo", "Terkendali"],
    ["R8", "Perilaku Web dan Android tidak konsisten", "3", "2", "6", "Satu backend dan kontrak OpenAPI; uji lintas platform", "Steven", "Terbuka"],
    ["R9", "Anggota tidak dapat berkontribusi sementara", "1", "2", "2", "Dokumentasi, pembagian tugas yang tidak bergantung pada satu orang", "Salomo", "Terbuka"],
    ["R10", "Backend dan Database bergantung pada satu orang (Salomo)", "3", "3", "9", "README backend, Dokumen 4 dan 7 Backend, peer review oleh Steven", "Salomo", "Terbuka"],
    ["R11", "Aplikasi Web masih prototipe awal dan progres belum dilaporkan", "3", "3", "9", "Permintaan laporan progres dan konfirmasi teknis; kontrak API dan CORS sudah siap", "Salomo", "Terbuka"],
    ["R12", "Integrasi Android–backend (semula belum dimulai)", "1", "3", "3", "Fase 5 selesai 11 Nov: flavor online terhubung REST dan diuji end-to-end; flavor demo tetap cadangan demo", "Salomo", "Termitigasi"],
    ["R13", "API Gateway dan Docker belum pernah dijalankan", "2", "2", "4", "Alternatif run-local.ps1; uji compose sebelum build baseline 18 Nov", "Salomo", "Terbuka"],
    ["R14", "Nilai usulan (limit, biaya, saldo awal) belum dikonfirmasi", "2", "1", "2", "Ditandai sebagai usulan dan dikonfigurasi terpusat", "Salomo", "Terbuka"],
    ["R15", "SRS v0.1 belum lengkap (antarmuka eksternal, data, traceability)", "2", "2", "4", "Butir yang belum ada ditandai 'Perlu Ditentukan' di Dokumen 3; traceability di Dokumen 6", "Salomo", "Terbuka"],
  ], [550, 2350, 350, 350, 550, 2950, 900, 1070], { center: [0, 2, 3, 4], size: 17 }),
  CAP("Tabel 4.1 Register risiko (P = peluang, D = dampak; skala 1–3)"),
  img(fig("d1_riskmap.png"), 400, { alt: "Matriks risiko" }),
  CAP("Gambar 4.1 Matriks peluang–dampak risiko"),
  H2("4.3 Prioritas Penanganan"),
  P("Risiko dengan skor 9 (R10, R11) dan skor 6 (R1, R6, R8) menjadi prioritas. Untuk R10, ketergantungan pada satu orang dikurangi melalui dokumentasi backend yang dapat diikuti anggota lain dan peer review oleh Steven. Untuk R11, Project Manager meminta laporan progres dan konfirmasi teknis dari tim Web sebelum 13 November 2026 agar arsitektur Web dapat difinalkan. R12 telah termitigasi karena integrasi Android–backend selesai pada 11 November 2026. Temuan baru dari integrasi: Android 17 memblokir akses aplikasi ke backend di jaringan lokal tanpa izin ACCESS_LOCAL_NETWORK; izin tersebut kini diminta saat aplikasi varian online dibuka."),

  // BAB V Cost
  H1("BAB V  ESTIMASI BIAYA"),
  H2("5.1 Metode"),
  P("Estimasi biaya memakai model **COCOMO II Post-Architecture** (Boehm et al., 2000). Ukuran perangkat lunak diambil dari jumlah baris kode sumber aktual (SLOC: baris non-kosong dan non-komentar, tanpa kode uji dan berkas hasil build) yang dihitung dari repositori pada 11 November 2026. Biaya tenaga kerja diperoleh dari effort (person-month) dikali tarif per person-month. Biaya non-tenaga kerja dan cadangan ditambahkan secara terpisah. Seluruh nilai yang bukan hasil pengukuran ditandai sebagai **asumsi**."),
  P("Rumus yang digunakan:"),
  B("Effort: PM = A × Size^E × EAF, dengan E = B + 0,01 × ΣSF."),
  B("Durasi: TDEV = C × PM^F, dengan F = D + 0,2 × (E − B)."),
  B("Rata-rata staf: PM / TDEV."),
  P(`Parameter kalibrasi standar COCOMO II.2000: A = 2,94; B = 0,91; C = 3,67; D = 0,28. Kelima scale factor (PREC, FLEX, RESL, TEAM, PMAT) dan ketujuh belas effort multiplier diasumsikan bernilai **nominal**, sehingga ΣSF = 18,97, E = ${E.toFixed(4).replace(".", ",")}, F = ${F.toFixed(4).replace(".", ",")}, dan EAF = 1,00.`),
  H2("5.2 Ukuran Perangkat Lunak"),
  table(["Komponen", "Berkas", "KSLOC", "Sumber"], [
    ["Backend (Java, main)", "72", "4,699", "Diukur"],
    ["Skema basis data (SQL migrasi)", "1", "0,218", "Diukur"],
    ["Aplikasi Android (Kotlin, main, termasuk lapisan REST Fase 5)", "110", "9,197", "Diukur"],
    ["Aplikasi Web (prototipe JSX)", "1", "0,073", "Diukur"],
    [{ t: "Subtotal ukuran terealisasi", bold: true }, "", { t: kReal.toFixed(3).replace(".", ","), bold: true }, ""],
    ["Penyelesaian aplikasi Web (thin client, 28 FR)", "–", "4,000", "Asumsi: ±50% ukuran Android, karena Web tidak memuat ledger in-memory, simulator, kamera, dan biometrik"],
    [{ t: "Total ukuran untuk penyelesaian", bold: true }, "", { t: kDone.toFixed(3).replace(".", ","), bold: true }, ""],
  ], [3400, 900, 1100, 3670], { center: [1, 2], size: 18 }),
  CAP("Tabel 5.1 Ukuran perangkat lunak (KSLOC = ribuan baris kode); kode uji (1,934 KSLOC) tidak dihitung"),
  H2("5.3 Hasil Perhitungan Effort dan Durasi"),
  table(["Skenario", "KSLOC", "Effort (PM)", "Durasi nominal (bulan)", "Rata-rata staf"], [
    ["Terealisasi per 11 Nov 2026", kReal.toFixed(3).replace(".", ","), f2(R1.pm), f2(R1.t), f2(R1.n)],
    ["Penyelesaian (termasuk aplikasi Web)", kDone.toFixed(3).replace(".", ","), f2(R2.pm), f2(R2.t), f2(R2.n)],
  ], [3270, 1200, 1500, 1700, 1400], { center: [1, 2, 3, 4], boldFirst: true }),
  CAP("Tabel 5.2 Hasil COCOMO II (seluruh driver nominal)"),
  H2("5.4 Rincian Biaya"),
  P(`Tarif tenaga kerja diasumsikan **${rp(RATE)} per person-month**, setara gaji pengembang tingkat junior di Indonesia (asumsi). Tabel 5.3 merangkum biaya untuk skenario penyelesaian.`),
  table(["No", "Komponen biaya", "Dasar perhitungan", "Jumlah"], [
    ["1", "Tenaga kerja pengembangan", `${f2(R2.pm)} PM × ${rp(RATE)}`, rp(labor)],
    ...nonLabor.map((r, i) => [String(i + 2), r[0], r[1], rp(r[2])]),
    ["7", "Cadangan risiko (contingency)", "10% × (tenaga kerja + non-tenaga kerja)", rp(contingency)],
    [{ t: "", bold: true }, { t: "Total estimasi biaya proyek", bold: true }, "", { t: rp(grand), bold: true }],
  ], [500, 3700, 2770, 2100], { center: [0], right: [3], size: 18 }),
  CAP("Tabel 5.3 Rincian estimasi biaya (skenario penyelesaian)"),
  table(["Tarif per PM (asumsi)", "Biaya tenaga kerja", "Total termasuk non-tenaga kerja + 10% cadangan"], [7_500_000, 10_000_000, 12_500_000].map((r) => {
    const l = R2.pm * r; return [rp(r), rp(l), rp((l + nonLaborTotal) * 1.1)];
  }), [3000, 3000, 3070], { center: [0, 1, 2] }),
  CAP("Tabel 5.4 Analisis sensitivitas terhadap tarif tenaga kerja"),
  H2("5.5 Interpretasi"),
  P(`Menurut COCOMO II, proyek dengan ukuran ±${kDone.toFixed(1).replace(".", ",")} KSLOC memerlukan sekitar ${f2(R2.pm)} person-month dalam ${f2(R2.t)} bulan. Nilai ini jauh lebih besar daripada kapasitas nyata tim (8 anggota paruh waktu selama ±2,3 bulan). Perbedaan tersebut wajar karena tiga hal. Pertama, COCOMO II dikalibrasi pada proyek industri dengan proses penuh (manajemen konfigurasi, QA formal, dokumentasi produksi). Kedua, sebagian besar fungsionalitas berasal dari framework dan pustaka (Spring Boot, Jetpack Compose) serta alat bantu pengembangan modern yang tidak tercermin dalam asumsi driver nominal. Ketiga, integrasi eksternal memakai simulator, bukan sistem produksi. Oleh karena itu, angka pada Tabel 5.3 dibaca sebagai **estimasi biaya bila proyek dikerjakan secara komersial**, bukan biaya yang dikeluarkan tim akademik. Biaya nyata yang dikeluarkan tim hanya komponen non-tenaga kerja (${rp(nonLaborTotal)}), dan biaya VPS serta domain baru akan terjadi bila deployment cloud dilakukan.`),
  H2("5.6 Asumsi"),
  ...NL([
    "Seluruh scale factor dan effort multiplier COCOMO II bernilai nominal; kalibrasi lokal belum dilakukan.",
    `Tarif ${rp(RATE)} per person-month adalah asumsi; sensitivitas disajikan pada Tabel 5.4.`,
    "Ukuran penyelesaian Web (4,000 KSLOC) adalah perkiraan dan akan diganti dengan ukuran aktual setelah implementasi; ukuran integrasi Android sudah diukur (termasuk dalam 9,197 KSLOC).",
    "Perangkat milik anggota dan perangkat lunak gratis tidak dibebankan.",
    "Biaya cloud dihitung untuk tiga bulan operasi staging dan demo.",
  ]),

  // BAB VI Penutup
  H1("BAB VI  PENUTUP"),
  P("Dokumen ini merangkum deskripsi, jadwal, rencana pengujian, risiko, dan estimasi biaya proyek Diponegoro Digital Bank per 11 November 2026. Backend dan aplikasi Android telah mencapai cakupan fungsional SRS dengan bukti pengujian otomatis. Tiga pekerjaan menentukan keberhasilan sampai 23 November 2026: integrasi Android–backend, kepastian dan percepatan aplikasi Web, serta pengujian sistem lintas platform. Dokumen ini diperbarui setiap kali status proyek berubah dan dicatat pada revision control."),
  H2("Referensi"),
  ...NL([
    "Boehm, B. W., et al. (2000). Software Cost Estimation with COCOMO II. Prentice Hall.",
    "ISO/IEC/IEEE 29119-1:2022. Software and systems engineering – Software testing – Part 1: General concepts.",
    "Kelompok Jaran Goyang. (2026). Software Requirements Specification PT Diponegoro Digital Bank (SRS-DDB-001) v0.1.",
    "Kelompok Jaran Goyang. (2026). Project Brief & Scope Document v1.0.",
    "Kelompok Jaran Goyang. (2026). Module Decomposition & Task Allocation (MDTA-DDB-001) v0.1.",
    "Kelompok Jaran Goyang. (2026). Software Engineering Documentation Work Plan (DWP-DDB-001) v1.0.",
  ]),
];

S.build(meta, body, OUT);
