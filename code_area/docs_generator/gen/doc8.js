// D08A Demo Program Sistem – Android (DEM-DDB-001-AND): skenario Android, backend, dan lintas platform.
// Usage: node doc8.js <folder tangkapan layar> <folder bukti 2026-11-22> <out.docx> <logo.jpeg> <tanda_tangan.png>
const S = require("./ddbstyle");
const { D, PW, P, H1, H2, H3, B, NL, CAP, table, cell } = S;
const [, , LAYAR, BUKTI, OUT, LOGO, SIGN] = process.argv;
const { Paragraph, Table, TableRow, WidthType, ImageRun, AlignmentType } = D;
const fs = require("fs"), path = require("path");

const meta = {
  docLabel: "Dokumen 8 – Volume Android", title: "Demo Program Sistem", subtitle: "Lingkungan, Skenario, Naskah, dan Bukti Demo – Android, Backend, dan Lintas Platform",
  code: "DEM-DDB-001-AND", version: "1.0", statusShort: "Final",
  status: "Final v1.0 – diterbitkan Project Manager 22 November 2026; pre-review rekan (R1) dan gladi bersama seluruh tim belum dilakukan sampai tenggat",
  module: "Demo Program Sistem (8 modul bisnis; volume Android dan lintas platform)", createdBy: "Salomo Putra Glorious Hokoyoku (Project Manager)",
  updatedBy: "Salomo Putra Glorious Hokoyoku (Project Manager)", team: "Kelompok Jaran Goyang – PIC volume menurut DWP: Rafi Fadhillah; skenario Android: Cecilia; skenario Web: Nabila; lingkungan: Salomo, Permata",
  refs: "SRS-DDB-001 v0.1; DWP-DDB-001 v1.4; UIM-DDB-001-AND v1.0; STD-DDB-001 v1.0; DVM-DDB-001-AND v1.0; DVM-DDB-001-BE v1.0; build 22 November 2026",
  date: "22 November 2026", logo: LOGO,
  revisions: [["1.0", "22 Nov 2026", "Salomo P.G. Hokoyoku", "Versi pertama, disusun Project Manager agar tersedia sebelum tenggat: lingkungan dan persiapan demo, 10 skenario Android, 3 skenario lintas platform dengan Web Node, naskah, bukti, rencana cadangan, dan keterbatasan. Disetujui PM."]],
  approval: { sign: SIGN, date: "22 Nov 2026", pic: [["PIC volume (DWP)", "Rafi Fadhillah"]] },
};
const pic = (f, w = 150) => { const d = fs.readFileSync(f); return new ImageRun({ type: "png", data: d, transformation: { width: w, height: Math.round(w * d.readUInt32BE(20) / d.readUInt32BE(16)) }, altText: { title: path.basename(f), description: "Tangkapan layar demo", name: path.basename(f) } }); };
let g = 0;
const strip = (files, t) => {
  const w = Math.floor(PW / files.length), ws = files.map((_, i) => i === files.length - 1 ? PW - w * (files.length - 1) : w);
  return [new Table({ width: { size: PW, type: WidthType.DXA }, columnWidths: ws, rows: [new TableRow({ cantSplit: true, children: files.map((f, i) => cell("", ws[i], { noBorder: true, children: [new Paragraph({ alignment: AlignmentType.CENTER, keepNext: true, children: [pic(f.includes("/") || f.includes("\\") ? f : path.join(LAYAR, f + ".png"))] })] })) })] }), CAP(`Gambar 3.${++g} ${t}`)];
};
const scene = (id, judul, dur, fr, steps, say, hasil) => [
  H2(`${id} ${judul}`),
  table(null, [["Durasi", dur], ["Kebutuhan", fr], ["Yang disampaikan", say], ["Hasil yang terlihat", hasil]].map(([a, b]) => [{ t: a, bold: true, fill: S.KEY_FILL }, b]), [2300, PW - 2300], { size: 19 }),
  P("**Langkah:**", { after: 40 }), ...NL(steps)];

const body = [
  H1("BAB I  PENDAHULUAN"),
  H2("1.1 Tujuan"),
  P("Dokumen ini menjadi panduan mendemonstrasikan Diponegoro Digital Bank: apa yang disiapkan, urutan skenario, apa yang diucapkan, hasil yang harus terlihat, dan apa yang dilakukan bila terjadi gangguan. Seluruh skenario memakai data simulasi; tidak ada uang sungguhan."),
  H2("1.2 Ruang Lingkup"),
  P("Volume ini mencakup aplikasi Android, backend, dan skenario lintas platform dengan aplikasi Web tim (Node/Express/EJS). Demo halaman Web dibahas pada volume Web yang disusun Rafi Fadhillah (DEM-DDB-001, skenario DEM-01 sampai DEM-14)."),
  H2("1.3 Pembagian Tugas Demo"),
  table(["Peran", "Pelaksana menurut DWP", "Tugas saat demo"], [["Pengarah demo", "Rafi Fadhillah (PIC D08)", "Membuka, mengatur waktu, menutup"], ["Operator Android", "Cecilia (skenario Android)", "Menjalankan skenario DA-01 – DA-10"], ["Operator Web", "Nabila (skenario Web)", "Menjalankan sisi Web pada DL-01 – DL-03"], ["Lingkungan", "Salomo dan Permata", "Backend, emulator/ponsel, database bersih, cadangan"]], [2200, 3000, 3870]),
  CAP("Tabel 1.1 Pembagian tugas demo"),
  H1("BAB II  LINGKUNGAN DAN PERSIAPAN"),
  H2("2.1 Lingkungan Demo"),
  table(["Komponen", "Spesifikasi", "Alamat"], [["Backend", "Spring Boot (Java 21), profil H2", "http://localhost:8080"], ["Android", "APK varian online build 22 November 2026; emulator Pixel 8 (Android 17) atau ponsel Android 7.0 ke atas", "http://10.0.2.2:8080 (emulator) atau adb reverse (ponsel)"], ["Web", "Aplikasi tim Web (Node.js 18+, Express, EJS)", "http://localhost:3000"], ["Cadangan", "APK varian demo (tanpa server, data di dalam aplikasi)", "–"]], [1500, 4570, 3000]),
  CAP("Tabel 2.1 Lingkungan demo"),
  H2("2.2 Akun Demo"),
  table(["Akun", "Password", "PIN", "Dipakai untuk"], [["demo", "Demo@1234", "123456", "Operator Android"], ["andi", "Demo@1234", "123456", "Penerima transfer dan operator Web"], ["budi, citra", "Demo@1234", "123456", "Cadangan"]], [1700, 2000, 1500, 3870]),
  CAP("Tabel 2.2 Akun demo (data simulasi)"),
  H2("2.3 Persiapan (30 menit sebelum demo)"),
  ...NL(["Matikan backend lama, hapus folder code_area/backend/data agar data demo bersih, lalu jalankan: powershell -ExecutionPolicy Bypass -File .\\run-local.ps1 -H2.",
    "Buka http://localhost:8080/swagger-ui.html; pastikan halaman tampil.",
    "Nyalakan emulator dan pasang APK online (gradlew :app:installOnlineDebug), atau colokkan ponsel dan jalankan adb reverse tcp:8080 tcp:8080.",
    "Jalankan Web tim: npm install, lalu npm start; buka http://localhost:3000.",
    "Login akun demo di Android satu kali dan izinkan notifikasi; kemudian keluar lagi.",
    "Pasang juga APK varian demo sebagai cadangan."]),
  H2("2.4 Susunan Waktu"),
  table(["Bagian", "Skenario", "Waktu"], [["Pembukaan dan arsitektur singkat", "–", "2 menit"], ["Akun dan keamanan", "DA-01, DA-02", "3 menit"], ["Transaksi", "DA-03, DA-04, DA-05", "5 menit"], ["Pengelolaan dana dan kartu", "DA-06, DA-07", "3 menit"], ["Riwayat, profil, notifikasi", "DA-08, DA-09, DA-10", "3 menit"], ["Lintas platform", "DL-01, DL-02, DL-03", "4 menit"], ["Penutup dan tanya jawab", "–", "5 menit"]], [4070, 3000, 2000]),
  CAP("Tabel 2.3 Susunan waktu demo (25 menit)"),
  H1("BAB III  SKENARIO DEMO ANDROID"),
  ...scene("DA-01", "Pembukaan Rekening", "2 menit", "FR-01, FR-02, FR-04", ["Ketuk Buka rekening; isi username, email, dan password; ketuk Daftar.", "Salin kode OTP yang tampil, isi, lalu ketuk Verifikasi.", "Isi NIK 3374010101010001, centang selfie, ketuk Kirim KTP & Selfie, tunggu disetujui.", "Ketuk Lanjut ke Login, login, buat PIN, lalu masukkan PIN."], "Nasabah membuka rekening sendiri: OTP, e-KYC, dan PIN sebelum boleh bertransaksi.", "Beranda terbuka dengan saldo awal Rp5.000.000."),
  ...strip(["AND-02-daftar", "AND-04-ekyc", "AND-05-buat-pin"], "Pembukaan rekening"),
  ...scene("DA-02", "Login dan PIN", "1 menit", "FR-03, FR-04, FR-06", ["Login dengan akun demo.", "Masukkan PIN lewat keypad; putar ponsel untuk menunjukkan tata letak miring.", "Ketuk ikon mata untuk menyembunyikan saldo."], "Setiap login melewati PIN. Salah tiga kali memblokir PIN lima menit.", "Beranda tampil; saldo dapat disembunyikan."),
  ...strip(["AND-01-login", "AND-06-masukkan-pin", "AND-08-beranda"], "Login, PIN, dan Beranda"),
  ...scene("DA-03", "Transfer Sesama Bank", "2 menit", "FR-10, FR-12, FR-14", ["Transfer → rekening 1400000002 → Cek rekening; nama ANDI PRATAMA tampil.", "Isi Rp150.000 dan catatan, ketuk Lanjut.", "Periksa ringkasan, ketuk Transfer sekarang, masukkan PIN.", "Tunjukkan tombol Bagikan gambar dan Bagikan PDF."], "Nama penerima diverifikasi sebelum uang dikirim; setiap transaksi memakai PIN dan kunci idempotensi sehingga tidak terdebit dua kali.", "Bukti transfer berstatus Berhasil."),
  ...strip(["AND-11b-transfer-nominal", "AND-11c-transfer-konfirmasi", "AND-11e-transfer-bukti"], "Transfer sesama bank"),
  ...scene("DA-04", "Transfer Antarbank Gagal dan Diproses", "1 menit", "FR-11, BR-04, BR-05", ["Transfer ke bank BRI rekening 9999999999 sebesar Rp20.000: status Gagal.", "Transfer ke Mandiri rekening 8888888888: status Diproses, lalu ketuk Periksa ulang status."], "Kegagalan jaringan antarbank tidak merugikan nasabah: dana kembali, dan status Diproses diselesaikan tanpa debit ganda.", "Saldo kembali untuk transfer gagal; status Diproses menjadi Berhasil."),
  ...scene("DA-05", "QRIS dan Top-up", "2 menit", "FR-15 – FR-18", ["Tab QRIS → pilih QR contoh Kopi Senja (nominal terkunci) atau pindai kamera → Bayar → PIN.", "Buka Terima QRIS dan tunjukkan QRIS pribadi.", "Top-up → Pulsa → nomor 081234567890 → Rp10.000 → Bayar → PIN."], "QRIS mengikuti format EMVCo; nominal QRIS dinamis tidak dapat diubah pembayar.", "Bukti pembayaran QRIS dan bukti pulsa."),
  ...strip(["AND-12b-qris-konfirmasi", "AND-13-terima-qris", "AND-14c-topup-bukti"], "QRIS dan top-up"),
  ...scene("DA-06", "Kantong Simpanan", "1,5 menit", "FR-08, FR-09", ["Tab Kantong → Buat kantong \"Dana Liburan\", kunci 1 bulan.", "Isi Rp500.000.", "Coba Tarik dini: konfirmasi dan PIN diminta."], "Dana dapat dipisah dan dikunci; penarikan dini dijaga PIN.", "Saldo utama berkurang; kantong bertanda terkunci."),
  ...scene("DA-07", "Kartu Virtual", "1,5 menit", "FR-21, FR-22", ["Kartu → Buat kartu virtual → Domestik → Buat kartu.", "Lihat detail → PIN: nomor lengkap dan CVV tampil.", "Bekukan, lalu Uji transaksi: ditolak."], "Nomor dan CVV terenkripsi di server dan hanya dibuka dengan PIN.", "Kartu aktif, detail tampil, lalu beku."),
  ...strip(["AND-10-kantong", "AND-15-kartu", "AND-15c-kartu-detail"], "Kantong dan kartu virtual"),
  ...scene("DA-08", "Riwayat, Analitik, dan Grafik Saldo", "1 menit", "FR-07, FR-23 – FR-25", ["Tab Riwayat → Filter jenis & status → Keluar.", "Buka satu transaksi; tunjukkan catatan buku besar.", "Buka Analitik dan Grafik Saldo."], "Setiap transaksi tercatat double-entry; jumlah baris selalu nol.", "Daftar tersaring, detail jurnal, dan grafik."),
  ...strip(["AND-16-riwayat", "AND-17-detail", "AND-18-analitik"], "Riwayat, detail, dan analitik"),
  ...scene("DA-09", "Profil dan Keamanan", "1 menit", "FR-27, FR-28", ["Profil → Ubah data → Simpan: OTP diminta.", "Buka Keamanan: biometrik, password, PIN.", "Buka Pengaturan notifikasi."], "Perubahan data penting dijaga OTP; login biometrik masih simulasi.", "Dialog OTP dan layar keamanan."),
  ...scene("DA-10", "Notifikasi Dana Masuk", "1 menit", "FR-29", ["Dari Swagger atau Web, andi mengirim Rp7.000 ke 1400123456.", "Tunggu notifikasi di Android, lalu buka ikon lonceng."], "Notifikasi diambil dari server setiap 15 detik; FCM belum dipakai.", "Notifikasi dana masuk paling lambat sekitar 15 detik."),
  ...strip(["AND-19-profil", "AND-20-keamanan", "AND-23-notifikasi"], "Profil, keamanan, dan notifikasi"),
  H1("BAB IV  SKENARIO LINTAS PLATFORM"),
  P("Android dan Web tim memakai backend yang sama, sehingga saldo, riwayat, dan sesi selalu sama."),
  table(["ID", "Skenario", "Langkah", "Hasil yang terlihat", "Bukti uji"], [
    ["DL-01", "Transfer Android tampil di Web", "Android (demo) transfer ke andi; Web (andi) muat ulang Dashboard", "Saldo dan riwayat Web bertambah sesuai nominal", "LPN-01 lulus"],
    ["DL-02", "E-wallet di Web tampil di Android", "Web (demo): Pembayaran → E-wallet → PIN; Android: Riwayat", "Transaksi dengan total sama muncul di Riwayat", "LPN-02 lulus"],
    ["DL-03", "Logout semua perangkat", "Android: Profil → Sesi & perangkat → Logout dari semua perangkat; Web: buka Riwayat", "Web kembali ke halaman masuk dengan pesan sesi berakhir", "LPN-03 lulus"]], [800, 1900, 2870, 2300, 1200], { size: 17, center: [0] }),
  CAP("Tabel 4.1 Skenario lintas platform"),
  H1("BAB V  BUKTI PELAKSANAAN"),
  P("Alur utama setiap skenario dijalankan lebih dahulu dengan skrip otomatis pada build yang sama, 22 November 2026; langkah peragaan tambahan (misalnya uji transaksi pada kartu beku dan membuka layar Keamanan) belum memiliki kasus uji sistem tersendiri. Tangkapan layar dan log tersimpan di code_area/pengujian/bukti/2026-11-22/."),
  table(["Skenario", "Kasus uji (STD-DDB-001)", "Hasil"], [["DA-01", "SA-21, SA-22", "Lulus"], ["DA-02", "SA-01, SA-02, SA-03; uji rotasi layar PIN", "Lulus"], ["DA-03", "SA-04 (melalui LP-01), SA-07, SA-24", "Lulus"], ["DA-04", "SA-05, SA-06", "Lulus"], ["DA-05", "SA-09, SA-25, SA-10; SA-08 (kamera) di ponsel fisik 14 Nov 2026 pada build sebelumnya", "Lulus"], ["DA-06", "SA-12", "Lulus"], ["DA-07", "SA-13 (pemeriksaan diperketat setelah DEF-12)", "Lulus"], ["DA-08", "SA-14, SA-27", "Lulus"], ["DA-09", "SA-15, SA-28", "Lulus"], ["DA-10", "SA-16", "Lulus"], ["DL-01 – DL-03", "LPN-01 – LPN-03", "Lulus"]], [1800, 5270, 2000]),
  CAP("Tabel 5.1 Keterkaitan skenario demo dengan kasus uji"),
  P("Gladi bersama seluruh anggota dan rekaman video demo belum dilakukan sampai dokumen ini diterbitkan."),
  H1("BAB VI  RENCANA CADANGAN DAN KETERBATASAN"),
  H2("6.1 Rencana Cadangan"),
  table(["Gangguan", "Tindakan"], [["\"Tidak dapat terhubung ke server\"", "Periksa backend; untuk ponsel jalankan ulang adb reverse tcp:8080 tcp:8080. Bila tetap gagal, pindah ke APK varian demo"], ["Emulator lambat atau tidak merespons", "Tutup aplikasi lain; pakai ponsel fisik atau varian demo"], ["PIN terblokir", "Tunggu 5 menit atau pakai akun budi"], ["Kamera tidak dapat memindai", "Pilih gambar QR dari galeri atau QR contoh"], ["Web tim tidak berjalan", "Tunjukkan sisi Web melalui Swagger UI"], ["Listrik atau jaringan padam", "Tunjukkan tangkapan layar pada Bab III"]], [3300, 5770]),
  CAP("Tabel 6.1 Rencana cadangan"),
  H2("6.2 Keterbatasan yang Disampaikan Saat Demo"),
  ...["Seluruh layanan luar (OTP, e-KYC, antarbank, QRIS, biller) adalah simulator.", "Login biometrik masih simulasi.", "Push notification memakai polling 15 detik.", "Web tim belum memiliki kantong, kartu virtual, notifikasi, grafik saldo, penerima favorit, QRIS pribadi, dan paket data; fitur tersebut tersedia di Android.", "Rate limit gateway (NF-04) belum diuji karena Docker belum terpasang."].map((t) => B(t)),
  H2("6.3 Pertanyaan yang Mungkin Muncul"),
  table(["Pertanyaan", "Jawaban singkat"], [["Bagaimana mencegah debit ganda?", "Setiap transaksi memakai Idempotency-Key; permintaan ulang dengan kunci sama mengembalikan transaksi yang sama."], ["Bagaimana menjaga saldo tetap benar?", "Ledger double-entry dalam satu transaksi basis data dan penguncian baris rekening."], ["Mengapa login tanpa OTP?", "SRS menetapkan OTP untuk registrasi, ubah profil, dan pemulihan; transaksi dijaga PIN."], ["Apakah Android menyimpan data?", "Varian online hanya menyimpan token terenkripsi; data ada di server."]], [3300, 5770]),
  CAP("Tabel 6.2 Pertanyaan yang mungkin muncul"),
  H2("Referensi"),
  ...NL(["SRS-DDB-001 v0.1, Software Requirements Specification.", "STD-DDB-001 v1.0, Pengujian Sistem.", "UIM-DDB-001-AND v1.0, User Interface Map Android.", "DEM-DDB-001, System Program Demo (volume Web, Rafi Fadhillah).", "DVM-DDB-001-AND v1.0 dan DVM-DDB-001-BE v1.0, Developer Manual.", "README.md aplikasi Web tim (digitalbanking-node)."]),
];
S.build(meta, body, OUT);
