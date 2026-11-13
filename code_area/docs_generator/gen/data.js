// Data for DWP-DDB-001. All statuses are "Belum Dimulai": no evidence of progress was supplied.
const M = {
  SAL: "Salomo", CEC: "Cecilia", GRA: "Gracia", GRC: "Grace",
  NAB: "Nabila", STE: "Steven", RAF: "Rafi", PER: "Permata",
};

const team = [
  ["622024022", "Salomo Putra Glorious Hokoyoku", "Salomo", "Mobile Application", "Project Manager, Full Stack Developer, pemegang tunggal Backend dan Database, final reviewer seluruh dokumentasi"],
  ["622024019", "Cecilia Adelaide Penlaana", "Cecilia", "Mobile Application", "Mobile Developer (Android); fokus desain antarmuka"],
  ["622024006", "Gracia Riyanto", "Gracia", "Mobile Application", "Mobile Developer (Android)"],
  ["6220204010", "Grace", "Grace", "Mobile Application", "Mobile Developer (Android)"],
  ["622024008", "Nabila Puspa Annastasya", "Nabila", "Web Application", "Web Developer; dokumentasi UI dan dukungan teknis"],
  ["622024003", "Steven Wibisana", "Steven", "Web Application", "Web Developer; analisis, logika, dan validasi"],
  ["622024005", "Rafi Fadhillah", "Rafi", "Web Application", "Web Developer; organisasi dan struktur dokumentasi"],
  ["622024016", "Permata Surya Cahyana", "Permata", "Web Application", "Web Developer; dokumentasi terstruktur berorientasi pengguna"],
];

// Deliverable volumes (Level 1 + platform split)
const volumes = [
  { id: "D01", no: "1", code: "PRJ-DDB-001", name: "Project Document", scope: "Seluruh proyek", pic: "Salomo", sup: "Steven (C: rencana pengujian, risiko, biaya); Rafi (format dan blok administratif)",
    deliv: "Dokumen proyek: judul, tanda tangan, history, daftar isi, deskripsi proyek, Gantt chart, model dan rencana pengujian, manajemen risiko, estimasi biaya",
    l2: "Judul; Tanda Tangan; History; Daftar Isi; Deskripsi Project; Gantt Chart; Rencana Testing dan Model Proses Testing; Manajemen Risiko; Estimasi Biaya" },
  { id: "D02", no: "2", code: "SAD-DDB-001", name: "Architectural Design", scope: "Android, Web, Backend, Database, integrasi", pic: "Salomo", sup: "Gracia (C: arsitektur Android); Steven (C: arsitektur Web)",
    deliv: "Dokumen arsitektur: konteks sistem, gaya arsitektur, dekomposisi modul, arsitektur klien, data, integrasi, deployment, keputusan arsitektur",
    l2: "Blok kontrol dokumen; Daftar Isi; Architectural Design" },
  { id: "D03", no: "3", code: "FSD-DDB-001", name: "Functional Design", scope: "8 modul bisnis SRS (FR-01 s.d. FR-29)", pic: "Salomo", sup: "Steven (review keterujian spesifikasi fungsi)",
    deliv: "Dokumen desain fungsional per modul: deskripsi modul, batas sistem, antarmuka eksternal, spesifikasi fungsi, workflow",
    l2: "Blok kontrol dokumen; Daftar Isi; Description Module; Sistem Boundaries; Eksternal Interface; Functions Specification; Workflow" },
  { id: "D04A", no: "4", code: "SDD-DDB-001-AND", name: "Detailed Design – Android", scope: "Aplikasi Android", pic: "Gracia", sup: "Salomo (C: konsistensi arsitektur); Cecilia (C: alur layar)",
    deliv: "UML, workflow diagram, pseudocode, dan perancangan software/hardware sisi Android",
    l2: "Blok kontrol dokumen; UML Diagram; Workflow Diagram; Algoritma Pseudocode; Perancangan Software/Hardware" },
  { id: "D04W", no: "4", code: "SDD-DDB-001-WEB", name: "Detailed Design – Web", scope: "Aplikasi Web", pic: "Rafi", sup: "Steven (C: logika dan sesi); Nabila (C: komponen UI)",
    deliv: "UML, workflow diagram, pseudocode, dan perancangan software/hardware sisi Web",
    l2: "Blok kontrol dokumen; UML Diagram; Workflow Diagram; Algoritma Pseudocode; Perancangan Software/Hardware" },
  { id: "D04B", no: "4", code: "SDD-DDB-001-BE", name: "Detailed Design – Backend & Database", scope: "Backend, Database, adapter simulator", pic: "Salomo", sup: "Steven (peer review teknis)",
    deliv: "UML backend, ERD/skema basis data, sequence transaksi, pseudocode ledger/idempotensi/refund, spesifikasi API, perancangan server",
    l2: "Blok kontrol dokumen; UML Diagram; Workflow Diagram; Algoritma Pseudocode; Perancangan Software/Hardware" },
  { id: "D05A", no: "5", code: "UIM-DDB-001-AND", name: "User Interface Map – Android", scope: "Aplikasi Android", pic: "Cecilia", sup: "Grace (pengumpulan tangkapan layar)",
    deliv: "Inventaris layar berkode, peta navigasi global, peta per modul, kartu layar",
    l2: "Blok kontrol dokumen; Daftar Isi; User Interface Map" },
  { id: "D05W", no: "5", code: "UIM-DDB-001-WEB", name: "User Interface Map – Web", scope: "Aplikasi Web", pic: "Nabila", sup: "Permata (pengumpulan tangkapan layar)",
    deliv: "Inventaris halaman berkode, peta navigasi global, peta per modul, kartu halaman",
    l2: "Blok kontrol dokumen; Daftar Isi; User Interface Map" },
  { id: "D06", no: "6", code: "STD-DDB-001", name: "System Testing (konsolidasi)", scope: "Web, Android, Backend/API, lintas platform", pic: "Steven", sup: "Salomo (C: data uji dan endpoint); Gracia dan Grace (eksekusi Android); Permata (eksekusi Web)",
    deliv: "Strategi uji, matriks keterlacakan, kasus uji per platform, hasil eksekusi aktual, log defect, ringkasan",
    l2: "Blok kontrol dokumen; Daftar Isi; Pengujian Sistem" },
  { id: "D07A", no: "7", code: "DVM-DDB-001-AND", name: "Developer Manual – Android", scope: "Kode sumber Android", pic: "Salomo", sup: "Gracia (penulisan bagian modul dan uji keterikutan); Cecilia dan Grace (C: catatan modul)",
    deliv: "Setup lingkungan, struktur proyek, keterangan source code per modul, build/run/debug",
    l2: "Blok kontrol dokumen; Daftar Isi; Developer Manual / Keterangan Source Code" },
  { id: "D07W", no: "7", code: "DVM-DDB-001-WEB", name: "Developer Manual – Web", scope: "Kode sumber Web", pic: "Steven", sup: "Nabila (komponen UI dan modul Web); Rafi dan Permata (C: catatan modul)",
    deliv: "Setup lingkungan, struktur proyek, keterangan source code per modul, build/deploy",
    l2: "Blok kontrol dokumen; Daftar Isi; Developer Manual / Keterangan Source Code" },
  { id: "D07B", no: "7", code: "DVM-DDB-001-BE", name: "Developer Manual – Backend", scope: "Kode sumber Backend dan Database", pic: "Salomo", sup: "Steven (uji keterikutan setup)",
    deliv: "Setup lingkungan, arsitektur kode, referensi API, migrasi basis data, simulator, pengujian, troubleshooting",
    l2: "Blok kontrol dokumen; Daftar Isi; Developer Manual / Keterangan Source Code" },
  { id: "D08", no: "8", code: "DEM-DDB-001", name: "System Program Demo", scope: "Android, Web, Backend (lintas platform)", pic: "Rafi", sup: "Nabila (skenario Web); Cecilia (skenario Android); Permata (lingkungan, data demo, bukti); Salomo (C: lingkungan backend)",
    deliv: "Persiapan, lingkungan demo, skenario dan naskah, perilaku sistem aktual, bukti pendukung",
    l2: "Blok kontrol dokumen; Daftar Isi; Demo Program Sistem" },
  { id: "D09A", no: "9", code: "USM-DDB-001-AND", name: "User Manual – Android", scope: "Pengguna aplikasi Android", pic: "Grace", sup: "Cecilia (C: alur layar dan verifikasi langkah)",
    deliv: "Panduan langkah demi langkah per modul dengan tangkapan layar, FAQ, troubleshooting",
    l2: "Blok kontrol dokumen; Daftar Isi; User Manual" },
  { id: "D09W", no: "9", code: "USM-DDB-001-WEB", name: "User Manual – Web", scope: "Pengguna aplikasi Web", pic: "Permata", sup: "Nabila (C: alur halaman dan verifikasi langkah)",
    deliv: "Panduan langkah demi langkah per modul dengan tangkapan layar, FAQ, troubleshooting",
    l2: "Blok kontrol dokumen; Daftar Isi; User Manual" },
];

// Detailed to-do list. [id, doc, wp, task, pic, sup, dep, output, criteria]
const T = [];
const add = (id, doc, wp, task, pic, sup, dep, out, crit) => T.push({ id, doc, wp, task, pic, sup, dep, out, crit, status: "Belum Dimulai" });

// D00 cross-document management
add("D00-WP1-T01", "D00", "WP1 Standar dokumentasi", "Menetapkan standar penulisan, kode dokumen, konvensi nama berkas, siklus status, dan penomoran versi (Bab V dokumen ini).", M.SAL, "Rafi", "–", "Standar dokumentasi yang diumumkan ke tim", "Diterima dan dikonfirmasi oleh 8 anggota");
add("D00-WP1-T02", "D00", "WP1 Standar dokumentasi", "Menyusun master template .docx: sampul, blok kontrol dokumen (Nama Module, Status, Created by, Last updated by, Responsible team), revision control, daftar isi, gaya heading, tabel, dan keterangan gambar.", M.RAF, "–", "D00-WP1-T01", "Template_DDB_Master.docx", "Memuat semua field wajib template dosen; disetujui Salomo");
add("D00-WP1-T03", "D00", "WP1 Standar dokumentasi", "Menyusun register istilah dan referensi silang dari SRS v0.1: nama 8 modul, FR-01 s.d. FR-29, BR-01 s.d. BR-07, NFR-01 s.d. NFR-12, glosarium.", M.PER, "Rafi", "D00-WP1-T01", "Register Istilah dan Referensi (tabel)", "Seluruh ID dan nama sama persis dengan SRS v0.1");
add("D00-WP2-T01", "D00", "WP2 Keputusan teknis", "Membuka decision log dan menetapkan keputusan terbuka yang memblokir dokumentasi: framework Web, produk basis data, butir kontrak integrasi pokok.", M.SAL, "Steven (C), Gracia (C)", "–", "Decision Log v1", "Setiap keputusan memiliki tanggal, pemilik, dan dokumen terdampak");
add("D00-WP2-T02", "D00", "WP2 Keputusan teknis", "Mengonfirmasi status implementasi tiap modul per platform (Android, Web, Backend) berdasarkan bukti: build, commit, atau tangkapan layar.", M.SAL, "Semua PIC platform", "–", "Matriks Status Implementasi", "Setiap sel berisi status dan bukti; tidak ada klaim tanpa bukti");
add("D00-WP2-T03", "D00", "WP2 Keputusan teknis", "Menetapkan dan membekukan build baseline dokumentasi (APK Android, akses Web, lingkungan Backend dan DB, akun dan data simulasi).", M.SAL, "–", "D00-WP2-T02", "Build Baseline Dokumentasi (versi, tanggal, cara akses)", "Build dapat dijalankan minimal oleh dua anggota selain Salomo");
add("D00-WP3-T01", "D00", "WP3 Pengendalian", "Memantau progres dan blocker harian melalui laporan harian anggota; memperbarui Checklist Penyelesaian.", M.SAL, "Semua anggota", "D00-WP1-T01", "Checklist Penyelesaian yang mutakhir", "Diperbarui setiap hari sampai pengumpulan");
add("D00-WP3-T02", "D00", "WP3 Pengendalian", "Melaksanakan review konsistensi lintas dokumen (R3) terhadap matriks konsistensi (Bagian 5.3).", M.SAL, "Rafi (format), Permata (register istilah)", "Seluruh volume lulus R2", "Laporan Temuan Konsistensi", "Semua temuan ditutup atau dicatat sebagai keterbatasan");
add("D00-WP3-T03", "D00", "WP3 Pengendalian", "Mengemas paket final: penamaan berkas, pemeriksaan versi 1.0, pengumpulan sebelum tenggat.", M.SAL, "Rafi", "D00-WP3-T02", "Paket 9 dokumentasi final", "Terkumpul sebelum 24 Nov 2026 23.59 WIB");

// D01 Project Document
add("D01-WP1-T01", "D01", "WP1 Outline", "Menyusun outline Dokumen 1 sesuai 9 butir template dosen.", M.SAL, "–", "D00-WP1-T02", "Outline D01", "Lulus R0");
add("D01-WP2-T01", "D01", "WP2 Administratif", "Menyusun halaman judul, lembar tanda tangan, history dokumen, dan daftar isi.", M.RAF, "Salomo (C)", "D01-WP1-T01", "Bagian administratif D01", "Sesuai template master; data tim sesuai Tabel 1.2");
add("D01-WP3-T01", "D01", "WP3 Deskripsi project", "Menyusun deskripsi proyek dari Project Brief v1.0 dan SRS v0.1: latar belakang, masalah, tujuan, pengguna, ruang lingkup, 8 modul, organisasi tim.", M.SAL, "–", "D01-WP1-T01", "Bab Deskripsi Project", "Tidak ada fitur di luar SRS; fitur diferensiasi ditandai di luar rilis");
add("D01-WP4-T01", "D01", "WP4 Gantt chart", "Menyusun Gantt chart proyek berbasis fase Project Brief §10.2 dan jadwal yang telah ditetapkan; selisih antara rencana dan realisasi diberi tanda.", M.SAL, "–", "D00-WP2-T02", "Gantt chart proyek", "Setiap tanggal bersumber dari rencana atau realisasi tercatat");
add("D01-WP5-T01", "D01", "WP5 Testing", "Menetapkan model proses pengujian dan rencana pengujian tingkat tinggi: level uji, cakupan, lingkungan, kriteria masuk/keluar, peran.", M.SAL, "Steven (C)", "D01-WP1-T01", "Bab Rencana Testing dan Model Proses Testing", "Selaras dengan struktur D06; disetujui Steven sebagai PIC D06");
add("D01-WP6-T01", "D01", "WP6 Risiko", "Memutakhirkan register risiko R1–R9 Project Brief: status saat ini, pemilik, mitigasi, dan risiko baru (termasuk beban Backend pada satu orang).", M.SAL, "Steven (C)", "D01-WP1-T01", "Register risiko mutakhir", "Setiap risiko memiliki peluang, dampak, pemilik, mitigasi");
add("D01-WP7-T01", "D01", "WP7 Estimasi biaya", "Menetapkan metode estimasi biaya dan asumsinya (komponen SDM, perangkat, layanan/infrastruktur, lain-lain).", M.SAL, "Steven (C)", "D01-WP1-T01", "Metode dan asumsi estimasi biaya", "Setiap asumsi tertulis dan dapat ditelusuri");
add("D01-WP7-T02", "D01", "WP7 Estimasi biaya", "Menghitung dan menyajikan estimasi biaya dalam tabel rincian.", M.SAL, "Steven (pemeriksaan perhitungan)", "D01-WP7-T01", "Tabel estimasi biaya", "Perhitungan diperiksa ulang oleh Steven");
add("D01-WP8-T01", "D01", "WP8 Finalisasi", "Peer review (R1/R2) oleh Steven dan Rafi, revisi, dan persetujuan.", M.SAL, "Steven, Rafi", "D01-WP2 s.d. WP7", "D01 v1.0", "Lulus Definition of Done");

// D02 Architectural Design
add("D02-WP1-T01", "D02", "WP1 Outline", "Menyusun outline D02 termasuk blok kontrol dokumen.", M.SAL, "–", "D00-WP1-T02", "Outline D02", "Lulus R0");
add("D02-WP2-T01", "D02", "WP2 Arsitektur sistem", "Menetapkan konteks sistem dan gaya arsitektur (klien Android dan Web, API Gateway, modular monolith, data, adapter simulator) dari SRS 2.1 dan Project Brief §9.", M.SAL, "–", "D00-WP2-T01", "Diagram konteks dan arsitektur tingkat tinggi", "Konsisten dengan SRS 2.1, 2.5");
add("D02-WP2-T02", "D02", "WP2 Arsitektur sistem", "Menyusun dekomposisi komponen backend: 8 modul bisnis dan Financial Transaction Core, beserta tanggung jawabnya.", M.SAL, "–", "D02-WP2-T01", "Diagram komponen dan tabel tanggung jawab", "Nama modul sama dengan SRS");
add("D02-WP3-T01", "D02", "WP3 Arsitektur klien", "Menetapkan arsitektur aplikasi Android dan Web (lapisan, pola, pengelolaan state, komunikasi API).", M.SAL, "Gracia (C), Steven (C)", "D02-WP2-T01", "Bagian arsitektur klien", "Dikonfirmasi oleh Gracia dan Steven sesuai kode aktual");
add("D02-WP3-T02", "D02", "WP3 Data dan deployment", "Menyusun arsitektur data dan integrasi (basis data relasional, cache, penyimpanan berkas, adapter) serta deployment view sesuai realisasi.", M.SAL, "–", "D00-WP2-T01", "Diagram data/integrasi dan deployment", "Produk dan versi sesuai decision log");
add("D02-WP4-T01", "D02", "WP4 Keputusan arsitektur", "Mendokumentasikan keputusan arsitektur dan keterlacakannya ke NFR-01 s.d. NFR-12.", M.SAL, "–", "D02-WP2, D02-WP3", "Tabel keputusan arsitektur", "Setiap NFR relevan memiliki keputusan pendukung");
add("D02-WP5-T01", "D02", "WP5 Finalisasi", "Peer review oleh Steven, revisi, dan penetapan baseline untuk D04.", M.SAL, "Steven", "D02-WP4-T01", "D02 baseline, kemudian v1.0", "Baseline ditetapkan sebelum D04 masuk tahap UML");

// D03 Functional Design
add("D03-WP1-T01", "D03", "WP1 Outline", "Menyusun outline D03 dengan struktur per modul.", M.SAL, "–", "D00-WP1-T02", "Outline D03", "Lulus R0");
add("D03-WP2-T01", "D03", "WP2 Modul dan batas", "Menulis deskripsi 8 modul (tujuan, FR, prioritas MoSCoW, platform).", M.SAL, "–", "D00-WP1-T03", "Bab Description Module", "FR dan prioritas sama dengan SRS 3.2");
add("D03-WP2-T02", "D03", "WP2 Modul dan batas", "Menetapkan batas sistem: aktor, cakupan dan di luar cakupan, diagram konteks/use case.", M.SAL, "–", "D03-WP2-T01", "Bab Sistem Boundaries", "Di luar cakupan sesuai SRS 1.2 dan Project Brief 6.4");
add("D03-WP3-T01", "D03", "WP3 Antarmuka eksternal", "Menyusun antarmuka eksternal: antarmuka pengguna, API klien–backend, adapter simulator (OTP, e-KYC, antarbank, QRIS, penyedia, kartu, notifikasi).", M.SAL, "–", "D00-WP2-T01", "Bab Eksternal Interface", "Butir yang belum ditetapkan SRS v0.1 diberi label Perlu Ditentukan");
add("D03-WP4-T01", "D03", "WP4 Spesifikasi fungsi", "Menyusun spesifikasi fungsi FR-01 s.d. FR-29: input, proses, output, aturan bisnis, kriteria penerimaan.", M.SAL, "Steven (review keterujian)", "D03-WP2-T01", "Bab Functions Specification", "Seluruh 29 FR tercakup; setiap fungsi dapat diuji");
add("D03-WP5-T01", "D03", "WP5 Workflow", "Menyusun workflow per modul, minimal UC-01 Transfer (alur utama dan A1–A6) serta alur utama 7 modul lain.", M.SAL, "–", "D03-WP4-T01", "Bab Workflow", "Konsisten dengan SRS 3.3");
add("D03-WP6-T01", "D03", "WP6 Finalisasi", "Peer review oleh Steven, revisi, dan penetapan baseline untuk D04, D05, D06.", M.SAL, "Steven", "D03-WP5-T01", "D03 baseline, kemudian v1.0", "Baseline ditetapkan sebelum D04, D05, D06 difinalkan");

// D04A Android
add("D04A-WP1-T01", "D04A", "WP1 Outline", "Menyusun outline D04A sesuai template: blok kontrol, UML, workflow, pseudocode, perancangan software/hardware.", M.GRA, "–", "D00-WP1-T02", "Outline D04A (1 halaman)", "Lulus R0");
add("D04A-WP1-T02", "D04A", "WP1 Outline", "Menginventaris kelas, layar, dan komponen Android per modul dari kode aktual (paket com.diponegoro.digitalbank) dengan bertanya kepada PIC modul: Salomo (Authentication, Virtual Card), Cecilia (Transfer, History), Gracia (QRIS, Profile), Grace (Account, Top-up).", M.GRA, "Salomo (C), Cecilia (C), Grace (C)", "D00-WP2-T02", "Tabel inventaris komponen Android per modul", "Seluruh 8 modul terisi atau ditandai belum diimplementasikan");
add("D04A-WP2-T01", "D04A", "WP2 UML", "Menggambar class diagram Android per modul.", M.GRA, "Salomo (C)", "D02-WP5-T01, D04A-WP1-T02", "Class diagram (8 modul)", "Nama kelas sesuai kode; konsisten dengan D02");
add("D04A-WP2-T02", "D04A", "WP2 UML", "Menggambar sequence diagram alur kritis: login dan PIN, Transfer UC-01, pembayaran QRIS, Top-up.", M.GRA, "Salomo (C)", "D04A-WP2-T01", "4 sequence diagram", "Pemanggilan API sesuai D04B");
add("D04A-WP3-T01", "D04A", "WP3 Workflow", "Menggambar workflow (activity) diagram per modul sisi Android.", M.GRA, "Cecilia (C)", "D03-WP6-T01", "Workflow diagram per modul", "Konsisten dengan workflow D03 dan peta D05A");
add("D04A-WP4-T01", "D04A", "WP4 Pseudocode", "Menulis pseudocode algoritma kunci Android: validasi nominal transfer (min. Rp5.000), percobaan PIN 3 kali, timeout sesi, pemindaian QRIS, penanganan status diproses (A5).", M.GRA, "Salomo (C)", "D04A-WP2-T02", "Pseudocode 5 algoritma", "Logika sesuai SRS BR-01, BR-02, UC-01 A5");
add("D04A-WP5-T01", "D04A", "WP5 Software/Hardware", "Menyusun perancangan software/hardware: versi Android minimum, SDK, pustaka, izin (kamera, biometrik), spesifikasi perangkat uji.", M.GRA, "–", "D00-WP2-T01", "Bab Perancangan Software/Hardware", "Sesuai konfigurasi proyek aktual");
add("D04A-WP6-T01", "D04A", "WP6 Review", "Pre-review oleh Cecilia (R1), review teknis Salomo (R2), revisi.", M.GRA, "Cecilia, Salomo", "D04A-WP2 s.d. WP5", "D04A v1.0", "Lulus Definition of Done");

// D04W Web
add("D04W-WP1-T01", "D04W", "WP1 Outline", "Menyusun outline D04W sesuai template.", M.RAF, "–", "D00-WP1-T02", "Outline D04W", "Lulus R0");
add("D04W-WP1-T02", "D04W", "WP1 Outline", "Menginventaris komponen, halaman, dan service Web per modul dari kode aktual.", M.RAF, "Steven (C), Nabila (C), Permata (C)", "D00-WP2-T02", "Tabel inventaris komponen Web", "8 modul terisi atau ditandai belum diimplementasikan");
add("D04W-WP2-T01", "D04W", "WP2 UML", "Menggambar component/class diagram Web per modul.", M.RAF, "Steven (C)", "D02-WP5-T01, D04W-WP1-T02", "Diagram komponen/kelas Web", "Konsisten dengan D02 dan framework di decision log");
add("D04W-WP2-T02", "D04W", "WP2 UML", "Menggambar sequence diagram alur kritis Web: login dan route guard, Transfer UC-01, tampil QRIS pribadi, ekspor riwayat.", M.RAF, "Steven (C)", "D04W-WP2-T01", "4 sequence diagram", "Pemanggilan API sesuai D04B");
add("D04W-WP3-T01", "D04W", "WP3 Workflow", "Menggambar workflow diagram per modul sisi Web.", M.RAF, "Nabila (C)", "D03-WP6-T01", "Workflow diagram per modul", "Konsisten dengan D03 dan D05W");
add("D04W-WP4-T01", "D04W", "WP4 Pseudocode", "Menulis pseudocode: route guard e-KYC, validasi formulir transfer, penanganan token kedaluwarsa, ekspor PDF/CSV sesuai filter (FR-26).", M.RAF, "Steven (C)", "D04W-WP2-T02", "Pseudocode 4 algoritma", "Sesuai FR-03, FR-05, FR-11, FR-26");
add("D04W-WP5-T01", "D04W", "WP5 Software/Hardware", "Menyusun perancangan software/hardware Web: framework, dependensi, peramban yang didukung, kebutuhan server klien.", M.RAF, "–", "D00-WP2-T01", "Bab Perancangan Software/Hardware", "Sesuai decision log dan NFR-10");
add("D04W-WP6-T01", "D04W", "WP6 Review", "Pre-review oleh Nabila (R1), review teknis Salomo (R2), revisi.", M.RAF, "Nabila, Salomo", "D04W-WP2 s.d. WP5", "D04W v1.0", "Lulus Definition of Done");

// D04B Backend
add("D04B-WP1-T01", "D04B", "WP1 Outline", "Menyusun outline D04B sesuai template.", M.SAL, "–", "D00-WP1-T02", "Outline D04B", "Lulus R0");
add("D04B-WP2-T01", "D04B", "WP2 UML", "Menggambar class/component diagram backend per modul dan Financial Transaction Core.", M.SAL, "–", "D02-WP5-T01", "Diagram kelas/komponen backend", "Sesuai kode aktual");
add("D04B-WP3-T01", "D04B", "WP3 Data", "Menyusun ERD dan skema basis data, termasuk ledger double-entry dan kunci idempotensi.", M.SAL, "–", "D00-WP2-T01", "ERD dan kamus data", "Memenuhi BR-03, BR-04, NFR-07");
add("D04B-WP4-T01", "D04B", "WP4 Workflow dan pseudocode", "Menyusun sequence/workflow transaksi dan pseudocode: transaksi atomik, idempotensi, refund antarbank (BR-05), blokir PIN (BR-02).", M.SAL, "–", "D04B-WP2-T01", "Diagram dan pseudocode backend", "Sesuai SRS 3.3 dan 3.4");
add("D04B-WP5-T01", "D04B", "WP5 API dan server", "Menyusun ringkasan spesifikasi API (OpenAPI) dan perancangan software/hardware server.", M.SAL, "–", "D04B-WP2-T01", "Tabel endpoint dan bab perancangan server", "Dipakai sebagai acuan D04A, D04W, D06");
add("D04B-WP6-T01", "D04B", "WP6 Review", "Peer review teknis oleh Steven, revisi.", M.SAL, "Steven", "D04B-WP2 s.d. WP5", "D04B v1.0", "Lulus Definition of Done");

// D05A UI Map Android
add("D05A-WP1-T01", "D05A", "WP1 Outline", "Menyusun outline D05A dan legenda notasi peta UI (kode layar AND-xx, jenis navigasi).", M.CEC, "–", "D00-WP1-T02", "Outline dan legenda", "Lulus R0");
add("D05A-WP1-T02", "D05A", "WP1 Outline", "Menginventaris seluruh layar Android dari build atau prototipe dan memberi kode layar per modul.", M.CEC, "Grace (tangkapan layar)", "D00-WP2-T02", "Inventaris layar berkode", "Setiap layar memiliki kode unik dan modul");
add("D05A-WP2-T01", "D05A", "WP2 Peta navigasi", "Menggambar peta navigasi global: splash, autentikasi, dashboard, menu modul.", M.CEC, "–", "D05A-WP1-T02", "Diagram navigasi global", "Semua layar terhubung");
add("D05A-WP2-T02", "D05A", "WP2 Peta navigasi", "Menggambar peta per modul (8 modul) dengan transisi, kondisi (mis. e-KYC belum disetujui), dan referensi FR.", M.CEC, "–", "D05A-WP2-T01, D03-WP2-T01", "8 peta modul", "Setiap transisi berlabel aksi dan FR");
add("D05A-WP3-T01", "D05A", "WP3 Kartu layar", "Menyusun kartu layar: tangkapan layar, tujuan, elemen utama, aksi, layar tujuan.", M.CEC, "Grace", "D05A-WP1-T02", "Kartu layar untuk seluruh layar", "Tangkapan layar dari build baseline");
add("D05A-WP4-T01", "D05A", "WP4 Review", "Pre-review oleh Gracia (R1), review Salomo terhadap D03 (R2), revisi.", M.CEC, "Gracia, Salomo", "D05A-WP2, WP3", "D05A v1.0", "Lulus Definition of Done");

// D05W UI Map Web
add("D05W-WP1-T01", "D05W", "WP1 Outline", "Menyusun outline D05W dan legenda notasi (kode halaman WEB-xx).", M.NAB, "–", "D00-WP1-T02", "Outline dan legenda", "Lulus R0");
add("D05W-WP1-T02", "D05W", "WP1 Outline", "Menginventaris seluruh halaman Web dan memberi kode halaman per modul.", M.NAB, "Permata (tangkapan layar)", "D00-WP2-T02", "Inventaris halaman berkode", "Setiap halaman memiliki kode unik dan modul");
add("D05W-WP2-T01", "D05W", "WP2 Peta navigasi", "Menggambar peta navigasi global Web dan peta per modul dengan transisi, route guard, dan referensi FR.", M.NAB, "–", "D05W-WP1-T02, D03-WP2-T01", "Peta global dan 8 peta modul", "Pembatasan platform terlihat (tanpa pindai QRIS, dengan ekspor)");
add("D05W-WP3-T01", "D05W", "WP3 Kartu halaman", "Menyusun kartu halaman: tangkapan layar, tujuan, elemen, aksi, halaman tujuan.", M.NAB, "Permata", "D05W-WP1-T02", "Kartu halaman", "Tangkapan layar dari build baseline");
add("D05W-WP4-T01", "D05W", "WP4 Review", "Pre-review oleh Rafi (R1), review Salomo (R2), revisi.", M.NAB, "Rafi, Salomo", "D05W-WP2, WP3", "D05W v1.0", "Lulus Definition of Done");

// D06 System Testing
add("D06-WP1-T01", "D06", "WP1 Strategi", "Menyusun outline dan strategi pengujian sistem yang selaras dengan rencana pengujian D01.", M.STE, "Salomo (C)", "D00-WP1-T02", "Outline dan strategi uji", "Lulus R0; selaras D01-WP5");
add("D06-WP1-T02", "D06", "WP1 Strategi", "Menyusun matriks keterlacakan kebutuhan ke kasus uji (FR-01 s.d. FR-29, BR, NFR terpilih).", M.STE, "–", "D03-WP4-T01", "Matriks keterlacakan", "Setiap FR Must memiliki minimal satu kasus uji");
add("D06-WP2-T01", "D06", "WP2 Kasus uji", "Menyusun kasus uji Web per modul.", M.STE, "Permata (C)", "D06-WP1-T02, D05W-WP1-T02", "Kasus uji Web", "Langkah, data, hasil harapan lengkap");
add("D06-WP2-T02", "D06", "WP2 Kasus uji", "Menyusun kasus uji Android per modul, termasuk pindai QRIS (FR-15) dan biometrik (FR-03).", M.STE, "Gracia (C)", "D06-WP1-T02, D05A-WP1-T02", "Kasus uji Android", "Langkah, data, hasil harapan lengkap");
add("D06-WP2-T03", "D06", "WP2 Kasus uji", "Menyusun kasus uji Backend/API: kontrak, atomisitas, idempotensi (BR-04), blokir PIN (BR-02), refund (BR-05).", M.STE, "Salomo (C)", "D04B-WP5-T01", "Kasus uji Backend/API", "Setiap BR terkait transaksi teruji");
add("D06-WP2-T04", "D06", "WP2 Kasus uji", "Menyusun skenario integrasi lintas platform (transaksi di Android tampil konsisten di Web; saldo dan riwayat sesuai ledger).", M.STE, "–", "D06-WP2-T01 s.d. T03", "Skenario lintas platform", "Mencakup minimal Transfer, QRIS, Top-up");
add("D06-WP3-T01", "D06", "WP3 Lingkungan", "Menyiapkan lingkungan dan data uji berdasarkan build baseline.", M.STE, "Salomo", "D00-WP2-T03", "Deskripsi lingkungan dan data uji", "Lingkungan dapat dipakai seluruh penguji");
add("D06-WP4-T01", "D06", "WP4 Eksekusi", "Mengeksekusi kasus uji Android dan mencatat hasil aktual beserta bukti.", M.GRA, "Grace", "D06-WP2-T02, D06-WP3-T01", "Hasil eksekusi Android", "Setiap kasus berstatus Lulus/Gagal/Terblokir dengan bukti");
add("D06-WP4-T02", "D06", "WP4 Eksekusi", "Mengeksekusi kasus uji Web dan mencatat hasil aktual beserta bukti.", M.PER, "–", "D06-WP2-T01, D06-WP3-T01", "Hasil eksekusi Web", "Setiap kasus berstatus Lulus/Gagal/Terblokir dengan bukti");
add("D06-WP4-T03", "D06", "WP4 Eksekusi", "Mengeksekusi kasus uji Backend/API dan skenario lintas platform.", M.STE, "Salomo", "D06-WP2-T03, T04", "Hasil eksekusi API dan integrasi", "Hasil berasal dari eksekusi nyata");
add("D06-WP5-T01", "D06", "WP5 Pelaporan", "Mencatat defect, mengoordinasikan retest, dan menyusun ringkasan hasil pengujian.", M.STE, "–", "D06-WP4-T01 s.d. T03", "Log defect dan ringkasan hasil", "Angka ringkasan sama dengan hasil eksekusi");
add("D06-WP6-T01", "D06", "WP6 Review", "Review Salomo (R2), revisi, finalisasi.", M.STE, "Salomo", "D06-WP5-T01", "D06 v1.0", "Lulus Definition of Done");

// D07A Android DevMan
add("D07A-WP1-T01", "D07A", "WP1 Outline", "Menyusun outline Developer Manual Android.", M.SAL, "Gracia", "D00-WP1-T02", "Outline D07A", "Lulus R0");
add("D07A-WP2-T01", "D07A", "WP2 Setup dan struktur", "Menulis panduan setup lingkungan (Android Studio, JDK, Gradle, konfigurasi URL backend) dan struktur proyek.", M.SAL, "–", "D00-WP2-T03", "Bab setup dan struktur proyek", "Dapat diikuti dari nol oleh Gracia");
add("D07A-WP3-T01", "D07A", "WP3 Keterangan source code", "Menulis keterangan source code modul Payment (QRIS) dan Profile & Notification (modul Android yang dikembangkan Gracia).", M.GRA, "–", "D07A-WP2-T01", "Subbab 2 modul", "Nama kelas dan fungsi sesuai kode");
add("D07A-WP3-T02", "D07A", "WP3 Keterangan source code", "Menulis keterangan source code modul Authentication, Virtual Card, Transfer, Transaction History & Dashboard, Account, dan Top-up.", M.SAL, "Cecilia (catatan modul Transfer dan History), Grace (catatan modul Account dan Top-up); maks. 1 halaman per modul", "D07A-WP2-T01", "Subbab 6 modul", "Nama kelas dan fungsi sesuai kode");
add("D07A-WP4-T01", "D07A", "WP4 Build dan troubleshooting", "Menulis panduan build, run, debug, konvensi kode, dan troubleshooting.", M.SAL, "–", "D07A-WP2-T01", "Bab build dan troubleshooting", "Diuji ulang pada build baseline");
add("D07A-WP5-T01", "D07A", "WP5 Review", "Uji keterikutan oleh Gracia, revisi, finalisasi.", M.SAL, "Gracia", "D07A-WP3, WP4", "D07A v1.0", "Lulus Definition of Done");

// D07W Web DevMan
add("D07W-WP1-T01", "D07W", "WP1 Outline", "Menyusun outline Developer Manual Web.", M.STE, "Nabila", "D00-WP1-T02", "Outline D07W", "Lulus R0");
add("D07W-WP2-T01", "D07W", "WP2 Setup dan struktur", "Menulis panduan setup (framework sesuai decision log, variabel lingkungan, koneksi API) dan struktur proyek Web.", M.STE, "–", "D00-WP2-T01, D00-WP2-T03", "Bab setup dan struktur", "Dapat diikuti dari nol oleh Rafi");
add("D07W-WP3-T01", "D07W", "WP3 Keterangan source code", "Menulis keterangan source code modul Authentication dan Profile & Notification sisi Web.", M.STE, "–", "D07W-WP2-T01", "Subbab 2 modul", "Sesuai kode");
add("D07W-WP3-T02", "D07W", "WP3 Keterangan source code", "Menulis keterangan komponen UI bersama (input PIN, grafik, layout) dan modul Transfer serta Top-up.", M.NAB, "–", "D07W-WP2-T01", "Subbab komponen dan 2 modul", "Sesuai kode");
add("D07W-WP3-T03", "D07W", "WP3 Keterangan source code", "Menulis keterangan modul Account, Payment (QRIS), History & Dashboard, Virtual Card sisi Web.", M.NAB, "Rafi, Permata (catatan modul)", "D07W-WP2-T01", "Subbab 4 modul", "Sesuai kode");
add("D07W-WP4-T01", "D07W", "WP4 Build dan deploy", "Menulis panduan build, deploy/jalankan, konvensi kode, troubleshooting.", M.STE, "–", "D07W-WP2-T01", "Bab build dan troubleshooting", "Diuji pada build baseline");
add("D07W-WP5-T01", "D07W", "WP5 Review", "Uji keterikutan oleh Rafi, review Salomo, revisi.", M.STE, "Rafi, Salomo", "D07W-WP3, WP4", "D07W v1.0", "Lulus Definition of Done");

// D07B Backend DevMan
add("D07B-WP1-T01", "D07B", "WP1 Outline", "Menyusun outline Developer Manual Backend.", M.SAL, "–", "D00-WP1-T02", "Outline D07B", "Lulus R0");
add("D07B-WP2-T01", "D07B", "WP2 Setup", "Menulis panduan setup: Java, Spring Boot, basis data, variabel lingkungan, menjalankan simulator.", M.SAL, "–", "D00-WP2-T03", "Bab setup", "Dapat diikuti dari nol oleh Steven");
add("D07B-WP3-T01", "D07B", "WP3 Keterangan source code", "Menulis arsitektur kode, keterangan per modul, Financial Transaction Core, dan adapter simulator.", M.SAL, "–", "D04B-WP2-T01", "Bab keterangan source code", "Sesuai kode dan D04B");
add("D07B-WP4-T01", "D07B", "WP4 Operasional", "Menulis referensi API, migrasi basis data, pengujian unit, logging, troubleshooting.", M.SAL, "–", "D04B-WP5-T01", "Bab operasional", "Endpoint sama dengan D04B");
add("D07B-WP5-T01", "D07B", "WP5 Review", "Uji keterikutan setup oleh Steven, revisi, finalisasi.", M.SAL, "Steven", "D07B-WP2 s.d. WP4", "D07B v1.0", "Lulus Definition of Done");

// D08 Demo
add("D08-WP1-T01", "D08", "WP1 Outline", "Menyusun outline dokumen demo: blok kontrol, persiapan, lingkungan, skenario, perilaku sistem, bukti.", M.RAF, "–", "D00-WP1-T02", "Outline D08", "Lulus R0");
add("D08-WP1-T02", "D08", "WP1 Outline", "Menyusun daftar skenario demo berprioritas (FR Must lebih dahulu) berdasarkan status implementasi.", M.RAF, "Salomo (C)", "D00-WP2-T02", "Daftar skenario berprioritas", "Hanya fitur yang terimplementasi yang didemokan");
add("D08-WP2-T01", "D08", "WP2 Lingkungan", "Mendokumentasikan lingkungan demo (backend, basis data, perangkat Android, peramban, jaringan) dan checklist kesiapan.", M.PER, "Salomo (C)", "D00-WP2-T03", "Bab lingkungan demo dan checklist", "Checklist dapat dipakai sebelum demo");
add("D08-WP2-T02", "D08", "WP2 Lingkungan", "Menyiapkan dan mendokumentasikan akun dan data demo simulasi.", M.PER, "Salomo", "D08-WP2-T01", "Daftar akun dan data demo", "Data bersifat simulasi; tidak memuat data pribadi nyata");
add("D08-WP3-T01", "D08", "WP3 Skenario", "Menyusun skenario dan naskah demo Android (langkah, input, hasil harapan).", M.CEC, "–", "D08-WP1-T02, D05A-WP1-T02", "Naskah demo Android", "Merujuk kode layar D05A");
add("D08-WP3-T02", "D08", "WP3 Skenario", "Menyusun skenario dan naskah demo Web.", M.NAB, "–", "D08-WP1-T02, D05W-WP1-T02", "Naskah demo Web", "Merujuk kode halaman D05W");
add("D08-WP3-T03", "D08", "WP3 Skenario", "Menyusun skenario lintas platform (mis. transfer di Android, verifikasi saldo dan riwayat di Web).", M.RAF, "Cecilia, Nabila", "D08-WP3-T01, T02", "Naskah demo lintas platform", "Selaras dengan D06-WP2-T04");
add("D08-WP4-T01", "D08", "WP4 Gladi dan bukti", "Melaksanakan gladi demo dan merekam bukti (tangkapan layar, rekaman, log).", M.RAF, "Nabila, Cecilia, Permata", "D08-WP3-T03", "Bukti demo terarsip", "Setiap skenario memiliki bukti");
add("D08-WP4-T02", "D08", "WP4 Gladi dan bukti", "Mendokumentasikan perilaku sistem aktual dibanding harapan; deviasi dicatat apa adanya.", M.RAF, "–", "D08-WP4-T01", "Bab perilaku sistem", "Tidak ada hasil yang diklaim tanpa bukti");
add("D08-WP5-T01", "D08", "WP5 Review", "Review Salomo (R2), revisi, finalisasi.", M.RAF, "Salomo", "D08-WP4-T02", "D08 v1.0", "Lulus Definition of Done");

// D09A Android User Manual (explicit, sequential)
add("D09A-WP1-T01", "D09A", "WP1 Persiapan", "Salin template master; isi blok kontrol dokumen; susun outline: Pendahuluan, Persyaratan, 8 bab modul, FAQ dan Troubleshooting.", M.GRC, "–", "D00-WP1-T02", "Outline D09A", "Struktur disetujui Salomo (R0)");
add("D09A-WP2-T01", "D09A", "WP2 Pendahuluan", "Tulis pendahuluan, persyaratan perangkat, instalasi APK, dan izin aplikasi (kamera, biometrik).", M.GRC, "–", "D00-WP2-T03", "Bab Pendahuluan dan Instalasi", "Langkah bernomor dengan tangkapan layar");
const mod9A = [
  ["T01", "Authentication: registrasi, OTP, e-KYC, login, biometrik, PIN, lupa password/PIN"],
  ["T02", "Account: saldo dan sembunyikan saldo, grafik saldo, kantong, locking"],
  ["T03", "Transfer: sesama bank, antarbank, favorit, bukti transfer"],
  ["T04", "Payment (QRIS): pindai untuk membayar, tampil QRIS pribadi, QRIS dinamis"],
  ["T05", "Top-up: pulsa/paket data, token listrik, tagihan lain"],
  ["T06", "Virtual Card: buat kartu, lihat detail dengan PIN, bekukan/aktifkan, limit"],
  ["T07", "Transaction History & Dashboard: riwayat, filter, detail, dashboard"],
  ["T08", "Profile & Notification: profil, pengaturan keamanan, notifikasi"],
];
mod9A.forEach(([t, m]) => add(`D09A-WP3-${t}`, "D09A", "WP3 Panduan per modul", `Modul ${m}. Untuk setiap fitur: (1) ambil tangkapan layar tiap langkah, (2) tulis langkah bernomor, (3) tulis hasil yang diharapkan, (4) tulis pesan kesalahan yang mungkin muncul.`, M.GRC, "Cecilia (C)", "D09A-WP2-T01, D05A-WP1-T02", "Subbab modul dengan langkah bernomor dan tangkapan layar berlabel kode layar", "Semua fitur pada build tercakup; fitur belum tersedia ditandai 'Belum tersedia pada build vX'"));
add("D09A-WP4-T01", "D09A", "WP4 FAQ", "Tulis FAQ dan troubleshooting dari pesan kesalahan yang dicatat di WP3.", M.GRC, "–", "D09A-WP3-T08", "Bab FAQ dan Troubleshooting", "Minimal mencakup OTP, PIN terblokir, saldo tidak cukup, koneksi terputus");
add("D09A-WP5-T01", "D09A", "WP5 Review", "Verifikasi langkah oleh Cecilia (R1), review Salomo (R2), revisi.", M.GRC, "Cecilia, Salomo", "D09A-WP4-T01", "D09A v1.0", "Lulus Definition of Done");

// D09W Web User Manual
add("D09W-WP1-T01", "D09W", "WP1 Persiapan", "Salin template master; isi blok kontrol; susun outline: Pendahuluan, Akses dan Persyaratan Peramban, bab modul, FAQ.", M.PER, "–", "D00-WP1-T02", "Outline D09W", "Lulus R0");
add("D09W-WP2-T01", "D09W", "WP2 Pendahuluan", "Tulis pendahuluan, cara akses, dan peramban yang didukung.", M.PER, "–", "D00-WP2-T03", "Bab Pendahuluan", "Sesuai NFR-10 dan build baseline");
add("D09W-WP3-T01", "D09W", "WP3 Panduan per modul", "Modul Authentication dan Profile & Notification (tanpa biometrik).", M.PER, "Nabila (C)", "D09W-WP2-T01, D05W-WP1-T02", "2 bab modul", "Langkah bernomor dengan tangkapan layar berlabel");
add("D09W-WP3-T02", "D09W", "WP3 Panduan per modul", "Modul Account dan Virtual Card.", M.PER, "Nabila (C)", "D09W-WP2-T01", "2 bab modul", "Langkah bernomor dengan tangkapan layar berlabel");
add("D09W-WP3-T03", "D09W", "WP3 Panduan per modul", "Modul Transfer, Payment (QRIS pribadi/dinamis saja), dan Top-up.", M.PER, "Nabila (C)", "D09W-WP2-T01", "3 bab modul", "Batasan platform QRIS dijelaskan");
add("D09W-WP3-T04", "D09W", "WP3 Panduan per modul", "Modul Transaction History & Dashboard termasuk ekspor PDF/CSV (FR-26).", M.PER, "Nabila (C)", "D09W-WP2-T01", "1 bab modul", "Langkah ekspor sesuai filter aktif");
add("D09W-WP4-T01", "D09W", "WP4 FAQ", "Tulis FAQ dan troubleshooting.", M.PER, "–", "D09W-WP3-T04", "Bab FAQ", "Mencakup kesalahan umum");
add("D09W-WP5-T01", "D09W", "WP5 Review", "Verifikasi langkah oleh Nabila (R1), review Salomo (R2), revisi.", M.PER, "Nabila, Salomo", "D09W-WP4-T01", "D09W v1.0", "Lulus Definition of Done");



// Status berbasis bukti per 11 Nov 2026 (v1.1): draf PRJ-DDB-001 v0.9 dan SAD-DDB-001 v0.9 tersedia.
const DRAFT = "Draf selesai (v0.9)";
T.forEach((t) => {
  if (/^D01-WP[1-7]-/.test(t.id) || /^D02-WP[1-4]-/.test(t.id)) t.status = DRAFT;
  if (t.id === "D01-WP8-T01" || t.id === "D02-WP5-T01") t.status = "Menunggu review (R2)";
  if (t.id === "D00-WP2-T02") t.status = "Sebagian (Android, Backend; Web belum)";
});

module.exports = { team, volumes, T };
