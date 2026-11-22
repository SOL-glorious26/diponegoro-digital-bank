// D05A User Interface Map – Android (UIM-DDB-001-AND). Dasar: draf v0.2 Cecilia + REVIEW_D05A_Cecilia_revisi1.md + build 22 Nov 2026.
// Usage: node doc5a.js <folder tangkapan layar> <fig7> <out.docx> <logo.jpeg> <tanda_tangan.png>
const S = require("./ddbstyle");
const { D, PW, SZ, P, H1, H2, H3, B, NL, CAP, table, cell, img, KEY_FILL } = S;
const [, , LAYAR, FIG7, OUT, LOGO, SIGN] = process.argv;
const { Paragraph, Table, TableRow, WidthType, ImageRun, AlignmentType, VerticalAlign } = D;
const fs = require("fs"), path = require("path");

const meta = {
  docLabel: "Dokumen 5 – Volume Android", title: "User Interface Map", subtitle: "Inventaris Layar, Peta Navigasi, dan Kartu Layar – Aplikasi Android Diponegoro Digital Bank",
  code: "UIM-DDB-001-AND", version: "1.0", statusShort: "Final",
  status: "Final v1.0 – diterbitkan Project Manager 22 November 2026; wewenang review dan persetujuan dijalankan Project Manager",
  module: "User Interface Map – Android (23 layar, 8 modul bisnis)", createdBy: "Cecilia Adelaide Penlaana (PIC Dokumen 5 Volume Android)",
  updatedBy: "Salomo Putra Glorious Hokoyoku (Project Manager)", team: "Kelompok Jaran Goyang – Mobile Application Team (PIC: Cecilia Adelaide Penlaana; pendukung: Grace)",
  refs: "SRS-DDB-001 v0.1; FSD-DDB-001 v1.0; STD-DDB-001 v1.0; DWP-DDB-001 v1.5; AuthRoutes.kt, AppRoutes.kt, dan AuthNavGraph.kt per 22 November 2026",
  date: "22 November 2026", logo: LOGO,
  revisions: [
    ["0.1", "16 Nov 2026", "Cecilia Adelaide Penlaana", "Draf awal berdasarkan inventaris Functional Design."],
    ["0.2", "16 Nov 2026", "Cecilia Adelaide Penlaana", "Revisi berdasarkan review Salomo: inventaris 23 rute aplikasi, alur, kartu layar, dan acuan."],
    ["1.0", "22 Nov 2026", "Salomo P.G. Hokoyoku", "Penyelesaian oleh Project Manager sesuai review revisi 1: peta navigasi global dan delapan peta modul digambar sebagai diagram; jalur navigasi disamakan dengan AuthNavGraph.kt; elemen dan kondisi kartu layar diisi dari aplikasi; tangkapan layar build 22 November 2026 disisipkan; bottom navigation lima tab, PIN keypad, dan layar Kartu. Disetujui PM."],
  ],
  approval: { newPage: true, sign: SIGN, date: "22 Nov 2026" },
};

const shot = (f) => path.join(LAYAR, f + ".png");
// Kartu: teks di kiri, satu atau dua tangkapan layar di kanan.
function card(code, nama, rows, shots) {
  const iw = shots.length > 1 ? 4500 : 2750, tw = PW - iw;
  const pics = new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 60, after: 60 }, children: shots.flatMap((f, i) => {
    const r = new ImageRun({ type: "png", data: fs.readFileSync(shot(f)), transformation: { width: 138, height: 307 }, altText: { title: f, description: `Tangkapan layar ${code}`, name: f } });
    return i ? [new D.TextRun({ text: "  " }), r] : [r];
  }) });
  const info = rows.flatMap(([k, v]) => [new Paragraph({ spacing: { after: 70, line: 250 }, children: S.runs(`**${k}:** ${v}`, { size: 19 }) })]);
  return new Table({ width: { size: PW, type: WidthType.DXA }, columnWidths: [tw, iw], rows: [
    new TableRow({ cantSplit: true, children: [cell("", tw, { children: info }), cell("", iw, { v: VerticalAlign.CENTER, children: [pics] })] })] });
}
const fig = (f, t, w = 600) => [img(path.join(FIG7, f), w, { alt: t }), CAP(t)];

// kode, nama, rute, modul, FR, fungsi, elemen, navigasi, kondisi khas, tangkapan layar
const L = [
  ["AND-01", "Masuk", "auth/login", "Authentication", "FR-03", "Masuk ke akun; layar pertama saat aplikasi dibuka (tanpa splash).", "Logo; kolom \"Username, email, atau nomor telepon\"; kolom \"Password\" dengan tombol Tampilkan; tautan \"Lupa password atau PIN?\"; tombol Masuk; tombol \"Masuk dengan biometrik\" (hanya bila biometrik diaktifkan); tautan \"Buka rekening\". Varian demo juga menampilkan akun contoh.", "Berhasil: e-KYC belum disetujui → AND-04; PIN belum ada → AND-05; PIN ada → AND-06. Lupa → AND-07. Buka rekening → AND-02.", "Pesan kesalahan login bersifat umum. Setelah 5 menit tidak aktif, pengguna kembali ke layar ini dengan pesan sesi berakhir. Biometrik masih simulasi.", ["AND-01-login"]],
  ["AND-02", "Daftar", "auth/register", "Authentication", "FR-01", "Membuka akun baru.", "Kolom Username, Email, Nomor telepon, dan Password (tombol Tampilkan); tombol Daftar; tautan \"Sudah punya akun? Login\".", "Berhasil → AND-03. Tautan login → AND-01.", "Email atau nomor telepon wajib diisi salah satu; kesalahan validasi tampil di atas tombol.", ["AND-02-daftar"]],
  ["AND-03", "Verifikasi OTP", "auth/otp", "Authentication", "FR-01", "Mengaktifkan akun dengan OTP.", "Panel kode OTP simulasi beserta masa berlaku; kolom \"Kode OTP\"; tombol Verifikasi; tombol \"Kirim ulang OTP\".", "OTP valid → AND-04. Tombol Kembali dari AND-04 tidak membuka layar ini lagi.", "OTP ditampilkan di layar karena SMS/email belum diintegrasikan; berlaku 2 menit.", ["AND-03-otp"]],
  ["AND-04", "e-KYC", "auth/ekyc", "Authentication", "FR-02", "Verifikasi identitas (simulasi).", "Kolom \"NIK KTP (16 digit)\"; kotak centang \"Selfie sudah diambil (simulasi)\"; tombol \"Kirim KTP & Selfie\"; setelah disetujui tampil tombol \"Lanjut ke Login\".", "Disetujui → AND-01. Ditolak: tombol \"Coba kirim ulang data\".", "NIK berakhiran 0000 ditolak dan transaksi tetap nonaktif; selain itu disetujui setelah status diproses.", ["AND-04-ekyc", "AND-04b-ekyc-disetujui"]],
  ["AND-05", "Buat PIN", "auth/create_pin", "Authentication", "FR-04", "Membuat PIN transaksi pada login pertama.", "Kolom \"PIN baru (6 digit)\" dan \"Konfirmasi PIN\"; tombol Simpan PIN.", "PIN tersimpan → AND-06.", "PIN harus 6 digit dan kedua kolom sama.", ["AND-05-buat-pin"]],
  ["AND-06", "Masukkan PIN", "auth/enter_pin", "Authentication", "FR-04", "Verifikasi PIN pada setiap login.", "Logo; enam titik penanda; keypad 0–9 dan tombol hapus; tautan \"Lupa PIN?\".", "PIN valid (dikirim otomatis pada digit keenam) → AND-08. Lupa PIN → AND-07.", "PIN salah menampilkan sisa percobaan; salah 3 kali memblokir PIN 5 menit. Saat ponsel miring, keypad berada di samping judul.", ["AND-06-masukkan-pin"]],
  ["AND-07", "Pemulihan password/PIN", "auth/recovery", "Authentication", "FR-05", "Mengatur ulang password atau PIN.", "Pilihan Password atau PIN transaksi; kolom username/email/nomor telepon; tombol Kirim OTP; langkah OTP dan nilai baru; tombol Batal.", "Selesai atau batal → kembali ke layar asal (AND-01 atau AND-06).", "OTP wajib benar sebelum nilai baru dapat disimpan.", ["AND-07-pemulihan"]],
  ["AND-08", "Beranda", "home", "Account", "FR-06, FR-25", "Ringkasan akun dan pintu ke seluruh fitur.", "Ikon lonceng dan avatar; saldo dengan ikon mata; nomor rekening; saldo kantong; delapan aksi cepat (Transfer, Top-up, Kantong, Kartu, Bayar QRIS, Terima QRIS, Grafik Saldo, Analitik); pemasukan dan pengeluaran bulan ini; transaksi terakhir.", "Aksi cepat → layar terkait; lonceng → AND-23; avatar → AND-19; Lihat semua → AND-16; transaksi → AND-17; ringkasan bulan → AND-18.", "Saldo tersembunyi tersimpan di server dan tetap berlaku setelah aplikasi dibuka ulang. Titik emas pada lonceng menandai notifikasi belum dibaca.", ["AND-08-beranda", "AND-08b-saldo-tersembunyi"]],
  ["AND-09", "Grafik saldo", "account/chart", "Account", "FR-07", "Melihat perkembangan saldo rekening utama.", "Pilihan Harian, Mingguan, Bulanan; grafik garis; tombol \"Lihat sebagai tabel\"; perubahan periode ini.", "Panah kembali → AND-08.", "Data dihitung dari buku besar; titik terakhir sama dengan saldo.", ["AND-09-grafik-saldo"]],
  ["AND-10", "Kantong", "account/pockets", "Account", "FR-08, FR-09", "Mengelola kantong simpanan; juga tab bottom navigation.", "Ringkasan saldo; tombol \"+ Buat kantong\"; kartu kantong dengan tombol Isi, Tarik atau Tarik dini, dan Ubah; dialog buat kantong (nama, fungsi, target, kunci 1/3/6 bulan atau sampai target).", "Seluruh aksi melalui dialog di layar ini.", "Kantong terkunci menampilkan ikon gembok dan tanggal; penarikan dini memerlukan konfirmasi dan PIN.", ["AND-10-kantong", "AND-10b-buat-kantong"]],
  ["AND-11", "Transfer", "transfer", "Transfer", "FR-10–FR-14", "Transfer sesama dan antarbank dalam satu layar bertahap.", "Tahap Penerima (bank, nomor rekening, favorit), Nominal (nama penerima, nominal, catatan), Konfirmasi (biaya, total, saldo setelah), dialog PIN, dan Bukti (Bagikan gambar, Bagikan PDF).", "Bukti → Lihat detail (AND-17), Transfer lagi, atau Kembali ke beranda (AND-08).", "Minimal Rp5.000; saldo kurang ditolak sebelum PIN. Status Diproses menyediakan tombol \"Periksa ulang status\"; transfer gagal mengembalikan dana.", ["AND-11-transfer-penerima", "AND-11c-transfer-konfirmasi"]],
  ["AND-12", "Bayar QRIS", "qris/pay", "Payment", "FR-15", "Membayar QRIS; tab tengah bottom navigation.", "Tombol \"Pindai dengan kamera\" dan \"Pilih gambar QR dari galeri\"; QR contoh; kolom tempel payload; detail pembayaran (merchant, kota, jenis, nominal); tombol Bayar.", "Bayar → dialog PIN → bukti → AND-08. \"Terima dana dengan QRIS saya\" → AND-13.", "QRIS dinamis: nominal terkunci. QRIS milik sendiri atau kode rusak ditolak.", ["AND-12-bayar-qris", "AND-12b-qris-konfirmasi"]],
  ["AND-13", "Terima QRIS", "qris/receive", "Payment", "FR-16, FR-17", "Menampilkan QRIS pribadi untuk menerima dana.", "Gambar QRIS statis; kolom nominal dan tombol \"Buat QRIS dinamis\"; simulasi dana masuk dari bank lain.", "Panah kembali → layar asal (AND-08 atau AND-12).", "QRIS dinamis tidak dapat diubah nominalnya oleh pembayar.", ["AND-13-terima-qris"]],
  ["AND-14", "Top-up & tagihan", "topup", "Top-up", "FR-18–FR-20", "Membeli pulsa, paket data, token listrik, membayar tagihan, dan e-wallet.", "Tab Pulsa, Paket Data, Token Listrik, Tagihan, E-wallet; kolom nomor; pilihan nominal atau produk; konfirmasi; dialog PIN; bukti.", "Bukti → Transaksi lain atau Kembali ke beranda (AND-08).", "Operator terdeteksi otomatis. ID pelanggan tidak ditemukan dan tagihan yang sudah lunas menampilkan pesan.", ["AND-14-topup", "AND-14b-topup-konfirmasi"]],
  ["AND-15", "Kartu", "virtualcard/home", "Virtual Card", "FR-21, FR-22", "Membuat dan mengelola kartu virtual.", "Tombol \"+ Buat kartu virtual\"; kartu dengan nomor tersamar, status, dan limit; tombol Lihat detail/Sembunyikan, Bekukan/Aktifkan, Atur limit, Uji transaksi.", "Panah kembali → AND-08.", "Nomor lengkap dan CVV hanya tampil setelah PIN benar. Kartu beku menolak uji transaksi. Maksimal lima kartu.", ["AND-15-kartu", "AND-15c-kartu-detail"]],
  ["AND-16", "Riwayat", "history", "History", "FR-23", "Mencari dan menyaring transaksi; tab bottom navigation.", "Kolom pencarian; pilihan periode; Filter jenis & status, Reset, Analitik; jumlah transaksi; daftar transaksi; Muat lebih banyak.", "Transaksi → AND-17; Analitik → AND-18.", "Server tidak terjangkau menampilkan pesan \"Tidak dapat terhubung ke server\". Hasil kosong menampilkan pesan tidak ada yang cocok.", ["AND-16-riwayat", "AND-16b-riwayat-filter"]],
  ["AND-17", "Detail transaksi", "history/detail/{txId}", "History", "FR-24, FR-14", "Rincian satu transaksi.", "Status, nominal, ID, waktu, pihak terkait, biaya; Bagikan gambar/PDF; catatan buku besar (double-entry).", "Panah kembali → layar asal.", "Transaksi Diproses menyediakan \"Periksa ulang status\".", ["AND-17-detail"]],
  ["AND-18", "Analitik bulanan", "history/analytics", "History", "FR-25", "Pemasukan dan pengeluaran per bulan.", "Pemilih bulan; total pemasukan, pengeluaran, selisih; grafik batang enam bulan; Lihat sebagai tabel.", "Panah kembali → layar asal (AND-16 atau AND-08).", "Perpindahan antar-kantong tidak dihitung.", ["AND-18-analitik"]],
  ["AND-19", "Profil", "profile", "Profile", "FR-27", "Data pribadi dan pengaturan; tab bottom navigation.", "Foto (Ganti/Hapus foto); data pribadi dengan Ubah data; menu Keamanan, Pengaturan notifikasi, Sesi & perangkat aktif, Keluar.", "Menu → AND-20, AND-22, AND-21; Keluar → AND-01.", "Perubahan data memerlukan OTP.", ["AND-19-profil", "AND-19b-ubah-data"]],
  ["AND-20", "Keamanan", "profile/security", "Profile", "FR-28, FR-03", "Mengatur biometrik, password, dan PIN.", "Sakelar login biometrik; formulir ubah password; formulir ubah PIN transaksi; tautan kelola sesi.", "Kelola sesi → AND-21; panah kembali → AND-19.", "PIN lama salah 3 kali memblokir PIN sementara.", ["AND-20-keamanan"]],
  ["AND-21", "Sesi & perangkat", "profile/sessions", "Authentication", "FR-05", "Melihat sesi aktif dan keluar dari perangkat lain.", "Status akun; daftar sesi (perangkat, metode, mulai, berakhir); Muat ulang sesi; Logout; Logout dari semua perangkat; Pemulihan password/PIN.", "Logout → AND-01; Pemulihan → AND-07.", "Sesi Web dan Android tampil bersama. Daftar yang gagal dimuat menampilkan pesan koneksi.", ["AND-21-sesi"]],
  ["AND-22", "Pengaturan notifikasi", "profile/notification-settings", "Notification", "FR-28", "Memilih jenis dan kanal notifikasi.", "Kotak centang jenis (Transaksi masuk, Transaksi keluar, Keamanan akun, Info & promo) dan kanal (Push, Dalam aplikasi, Email simulasi).", "Panah kembali → AND-19.", "Minimal satu kanal harus dipilih.", ["AND-22-pengaturan-notifikasi"]],
  ["AND-23", "Kotak notifikasi", "notifications", "Notification", "FR-29", "Membaca notifikasi dan email simulasi.", "Tab Dalam aplikasi dan Email; daftar notifikasi dengan tanda Baru; tombol \"Tandai dibaca\".", "Notifikasi transaksi → AND-17; panah kembali → AND-08.", "Dibuka dari ikon lonceng di Beranda. Email hanya outbox simulasi.", ["AND-23-notifikasi"]],
];
const MOD = ["Authentication", "Account", "Transfer", "Payment", "Top-up", "Virtual Card", "History", "Profile", "Notification"];

const body = [
  H1("BAB I  PENDAHULUAN"),
  H2("1.1 Tujuan dan Ruang Lingkup"),
  P("Dokumen ini memetakan layar aplikasi Android Diponegoro Digital Bank: inventaris layar, hubungan navigasi secara global dan per modul, elemen utama setiap layar, serta keterkaitannya dengan kebutuhan fungsional. Inventaris mengikuti aplikasi yang berjalan berdasarkan AuthRoutes.kt, AppRoutes.kt, dan AuthNavGraph.kt. Rincian visual seperti ukuran dan jarak tidak dibahas; tema dijelaskan pada DVM-DDB-001-AND."),
  P("Tangkapan layar diambil pada 22 November 2026 dari varian online di emulator Pixel 8 (Android 17) dengan database demo yang bersih. Nama, saldo, dan transaksi pada gambar adalah data simulasi."),
  H2("1.2 Legenda"),
  table(["Notasi", "Arti"], [["AND-01 … AND-23", "Kode unik layar atau rute Android"], ["Kotak hijau", "Titik masuk aplikasi atau kembali ke login"], ["Kotak kuning", "Layar yang menjadi tab bottom navigation"], ["Kotak biru", "Layar lain"], ["Kotak abu-abu", "Langkah atau dialog di dalam layar yang sama, bukan rute terpisah"], ["Panah", "Perpindahan; label menunjukkan aksi atau kondisinya"]], [2600, 6470]),
  CAP("Tabel 1.1 Legenda peta antarmuka"),
  H1("BAB II  INVENTARIS LAYAR"),
  P(`Aplikasi Android memiliki ${L.length} layar. FR-26 (ekspor riwayat) tidak termasuk karena SRS menetapkannya untuk platform Web.`),
  table(["Kode", "Nama layar", "Rute", "Modul", "FR"], L.map((r) => [r[0], r[1], r[2], r[3], r[4]]), [950, 2500, 2820, 1500, 1300], { size: 18, center: [0] }),
  CAP("Tabel 2.1 Inventaris layar Android"),
  table(["Tab", "Layar", "Keterangan"], [["Beranda", "AND-08", "Akar navigasi setelah PIN valid"], ["Riwayat", "AND-16", "Membuka AND-17 dan AND-18"], ["QRIS", "AND-12", "Tombol bulat di tengah; AND-13 dibuka dari layar ini atau dari Beranda"], ["Kantong", "AND-10", "Juga tersedia sebagai aksi cepat di Beranda"], ["Profil", "AND-19", "Membuka AND-20, AND-21, AND-22"]], [1700, 1700, 5670]),
  CAP("Tabel 2.2 Bottom navigation lima tab"),
  H1("BAB III  PETA NAVIGASI"),
  H2("3.1 Peta Navigasi Global"),
  P("Aplikasi selalu dibuka di layar Masuk. Setelah kredensial valid, status e-KYC dan PIN diperiksa di layar Masuk sebelum pengguna diarahkan ke layar berikutnya. Panah e-KYC yang belum disetujui (AND-01 → AND-04) tidak digambar agar peta mudah dibaca; jalur tersebut dijelaskan pada kartu AND-01."),
  ...fig("n0_global.png", "Gambar 3.1 Peta navigasi global aplikasi Android"),
  H2("3.2 Peta Navigasi per Modul"),
  ...[["n1_auth.png", "3.2 Modul Authentication"], ["n2_account.png", "3.3 Modul Account"], ["n3_transfer.png", "3.4 Modul Transfer"], ["n4_qris.png", "3.5 Modul Payment (QRIS)"], ["n5_topup.png", "3.6 Modul Top-up"], ["n6_card.png", "3.7 Modul Virtual Card"], ["n7_history.png", "3.8 Modul Transaction History & Dashboard"], ["n8_profile.png", "3.9 Modul Profile & Notification"]].flatMap(([f, t]) => fig(f, "Gambar " + t, 560)),
  H1("BAB IV  KARTU LAYAR"),
  P("Setiap kartu memuat tujuan, elemen yang tampil, aksi dan navigasi, kondisi khas, serta tangkapan layar."),
];
let n = 0;
MOD.forEach((m) => {
  const list = L.filter((r) => r[3] === m);
  body.push(H2(`4.${++n} Modul ${m}`));
  list.forEach((r) => body.push(H3(`${r[0]} ${r[1]}`), card(r[0], r[1], [["Rute", r[2]], ["Kebutuhan", r[4]], ["Tujuan", r[5]], ["Elemen utama", r[6]], ["Aksi dan navigasi", r[7]], ["Kondisi khas", r[8]]], r[9]), new Paragraph({ spacing: { after: 120 }, children: [] })));
});
body.push(
  H2(`4.${++n} Tampilan Bersama`),
  P("Dialog PIN transaksi, bukti transaksi, dan mode gelap dipakai oleh beberapa modul."),
  new Table({ width: { size: PW, type: WidthType.DXA }, columnWidths: [3023, 3023, 3024], rows: [new TableRow({ cantSplit: true, children: ["AND-11d-transfer-pin", "AND-11e-transfer-bukti", "GELAP-beranda"].map((f, i) => cell("", i === 2 ? 3024 : 3023, { children: [new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 60, after: 60 }, children: [new ImageRun({ type: "png", data: fs.readFileSync(shot(f)), transformation: { width: 160, height: 356 }, altText: { title: f, description: f, name: f } })] })] })) })] }),
  CAP("Gambar 4.1 Dialog PIN transaksi, bukti transaksi, dan Beranda pada mode gelap"),
  H1("BAB V  ATURAN NAVIGASI DAN KETERLACAKAN"),
  H2("5.1 Aturan Navigasi dan Konsistensi"),
  ...NL([
    "Setiap transaksi melewati ringkasan konfirmasi sebelum PIN diminta; transaksi yang gagal validasi tidak meminta PIN.",
    "Setiap login melewati AND-06. Halaman setelah login yang dibuka tanpa pengguna aktif mengarahkan kembali ke AND-01.",
    "Prasyarat transaksi: e-KYC disetujui, PIN sudah dibuat, dan saldo cukup. Bila e-KYC belum disetujui, Beranda menampilkan pemberitahuan dan transaksi dinonaktifkan.",
    "Tab Beranda mengembalikan pengguna ke akar; tab lain menyimpan dan memulihkan keadaan layarnya.",
    "Pesan kesalahan ditulis dengan bahasa yang jelas dan tidak membuka data sensitif.",
    "Status transaksi ditampilkan dengan ikon dan teks, tidak hanya warna; warna berbeda untuk mode terang dan gelap.",
    "Bayar QRIS dan transfer dapat dimulai dengan satu ketukan dari Beranda (NFR-08).",
  ]),
  H2("5.2 Keterlacakan Kebutuhan ke Layar"),
  table(["FR", "Layar"], [["FR-01", "AND-02, AND-03"], ["FR-02", "AND-04"], ["FR-03", "AND-01, AND-20"], ["FR-04", "AND-05, AND-06"], ["FR-05", "AND-07, AND-21"], ["FR-06", "AND-08"], ["FR-07", "AND-09"], ["FR-08, FR-09", "AND-10"], ["FR-10 – FR-14", "AND-11, AND-17"], ["FR-15", "AND-12"], ["FR-16, FR-17", "AND-13"], ["FR-18 – FR-20", "AND-14"], ["FR-21, FR-22", "AND-15"], ["FR-23", "AND-16"], ["FR-24", "AND-17"], ["FR-25", "AND-18, AND-08"], ["FR-26", "Tidak ada di Android (khusus Web)"], ["FR-27", "AND-19"], ["FR-28", "AND-20, AND-22"], ["FR-29", "AND-23"]], [3000, 6070]),
  CAP("Tabel 5.1 Keterlacakan kebutuhan fungsional ke layar"),
  H2("5.3 Keterbatasan"),
  B("Login biometrik masih simulasi; OTP, email, dan jaringan antarbank juga simulasi."),
  B("PIN setelah login memakai keypad, sedangkan PIN transaksi memakai dialog dengan kolom teks."),
  B("Tombol mata saldo dan avatar berukuran 36 dp, lebih kecil dari anjuran 48 dp."),
  H2("Referensi"),
  ...NL(["SRS-DDB-001 v0.1, Software Requirements Specification.", "FSD-DDB-001 v1.0, Functional Design.", "DWP-DDB-001 v1.5, Software Engineering Documentation Work Plan.", "STD-DDB-001 v1.0, Pengujian Sistem.", "Kode sumber code_area/apps/android: AuthRoutes.kt, AppRoutes.kt, AuthNavGraph.kt."]),
);
S.build(meta, body, OUT);
