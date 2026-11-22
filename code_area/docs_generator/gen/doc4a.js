// D04A Detailed Design – Android (SDD-DDB-001-AND). Isi: draf v0.4 Gracia Riyanto (d04a_blocks.json, hasil ekstraksi docx)
// + perbaikan sesuai REVIEW_D04A_Gracia_revisi2.md dan tampilan Android 22 Nov 2026.
// Usage: node doc4a.js <fig_d04a> <fig7> <out.docx> <logo.jpeg> <tanda_tangan.png>
const S = require("./ddbstyle");
const { D, FONT, PW, SZ, P, H1, H2, H3, B, CAP, table, img } = S;
const [, , FIG, FIG7, OUT, LOGO, SIGN] = process.argv;
const { Paragraph, TextRun, Table, TableRow, TableCell, WidthType, ShadingType, BorderStyle } = D;
const X = require("./d04a_blocks.json");
const path = require("path");

const meta = {
  docLabel: "Dokumen 4 – Volume Android", title: "Detailed Design", subtitle: "UML, Workflow, Algoritma Pseudocode, dan Perancangan Software/Hardware – Aplikasi Android",
  code: "SDD-DDB-001-AND", version: "1.0", statusShort: "Final",
  status: "Final v1.0 – diterbitkan Project Manager 22 November 2026; wewenang review dan persetujuan dijalankan Project Manager",
  module: "Detailed Design – Android (Kotlin, Jetpack Compose; 8 modul bisnis)", createdBy: "Gracia Riyanto (PIC Dokumen 4 Volume Android)",
  updatedBy: "Salomo Putra Glorious Hokoyoku (Project Manager)", team: "Kelompok Jaran Goyang – Mobile Application Team (PIC: Gracia Riyanto)",
  refs: "SRS-DDB-001 v0.1; SAD-DDB-001 v1.0; FSD-DDB-001 v1.0; SDD-DDB-001-BE v1.0; STD-DDB-001 v1.0; UIM-DDB-001-AND v1.0; kode apps/android per 22 November 2026",
  date: "22 November 2026", logo: LOGO,
  revisions: [
    ["0.1", "14 Nov 2026", "Gracia Riyanto", "Kerangka dan draf awal dari dokumen proyek. Belum diverifikasi terhadap kode."],
    ["0.2", "14 Nov 2026", "Gracia Riyanto", "Diverifikasi terhadap kode sumber; ditambahkan class, sequence, activity, dan state diagram; pseudocode disesuaikan dengan kode."],
    ["0.3", "16 Nov 2026", "Gracia Riyanto", "Menindaklanjuti review Salomo: pelengkapan diagram modul, sequence Transfer/Top-up, workflow tambahan, dan pembaruan data uji/perangkat."],
    ["0.4", "16 Nov 2026", "Gracia Riyanto", "Pemeriksaan ulang review; pembaruan referensi; pemetaan endpoint empat sequence diagram; pembaruan daftar isi."],
    ["1.0", "22 Nov 2026", "Salomo P.G. Hokoyoku", "Penyelesaian oleh Project Manager sesuai review revisi 2: catatan verifikasi sumber dihapus; kode layar AND-01–AND-23 ditambahkan (Tabel 3.1); sequence Transfer dan Top-up digambar ulang dengan lifeline dan dipindah ke 3.4; class diagram Lampiran A digambar ulang (arah Screen → ViewModel → Repository); pseudocode 5.7 dirapikan; tampilan 21–22 Nov (navigasi lima tab, PIN keypad, layar Kartu) dan hasil uji STD-DDB-001 v1.0 dimasukkan. Disetujui PM."],
  ],
  approval: { sign: SIGN, date: "22 Nov 2026" },
};

const bd = { style: BorderStyle.SINGLE, size: 4, color: "808080" };
const code = (t) => new Table({ width: { size: PW, type: WidthType.DXA }, columnWidths: [PW], rows: [new TableRow({ children: [new TableCell({
  width: { size: PW, type: WidthType.DXA }, borders: { top: bd, bottom: bd, left: bd, right: bd }, shading: { fill: "F7F7F7", type: ShadingType.CLEAR, color: "auto" }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
  children: t.split("\n").map((l) => new Paragraph({ spacing: { after: 0, line: 228 }, children: [new TextRun({ text: l || " ", font: "Consolas", size: 15, color: "000000" })] })) })] })] });
const gap = () => new Paragraph({ spacing: { after: 100 }, children: [] });
const WIDTHS = { 2: [2600, 6470], 3: [2300, 3000, 3770], 4: [1350, 900, 5470, 1350], 5: [1300, 2100, 1900, 2000, 1770] };
const tbl = (rows, w) => table(rows[0], rows.slice(1), w ?? WIDTHS[rows[0].length], { size: rows[0].length >= 4 ? 16 : 18 });
const figure = (f, t, w = 560) => [img(f, w, { alt: t }), CAP(t)];
const fit = (f, w, maxH) => { const d = require("fs").readFileSync(f); return Math.min(w, Math.round(maxH * d.readUInt32BE(16) / d.readUInt32BE(20))); };
const media = (f, w) => { const p = path.join(FIG, f); return img(p, fit(p, w ?? 540, 560), { alt: f }); };

// Penggantian teks (indeks = posisi blok pada d04a_blocks.json)
const R = {
  34: X[34][1],
  35: "Aplikasi Android terdiri atas 112 berkas Kotlin pada kode utama (per 22 November 2026, setelah berkas Brand.kt dan Buttons.kt ditambahkan pada pembaruan tampilan). Ukuran kode untuk estimasi biaya pada PRJ-DDB-001 diukur pada tanggal yang sama, yaitu 9,935 KSLOC.",
  77: "Class diagram ringkas modul Authentication, Account, Transfer, Top-up, Virtual Card, serta History dan Home ada pada Lampiran A. Panah menunjukkan arah pemakaian: Screen memakai ViewModel, dan ViewModel memakai Repository yang memiliki dua implementasi.",
  79: "Navigasi didefinisikan di AuthNavGraph.kt dengan rute pada AuthRoutes dan AppRoutes. Bottom bar memuat lima tab: Beranda (home), Riwayat (history), QRIS (qris/pay, tombol bulat di tengah), Kantong (account/pockets), dan Profil (profile). Setelah login berhasil, AuthViewModel memeriksa status e-KYC dan PIN sebelum menentukan layar berikutnya. Kode layar mengikuti UIM-DDB-001-AND.",
  81: "Tabel 3.1 Kode layar dan rute navigasi (sumber: AuthRoutes.kt, AppRoutes.kt, UIM-DDB-001-AND v1.0)",
  93: X[93][1] + " PIN diisi melalui keypad enam digit di layar (EnterPinScreen, PinKeypad) dan dikirim otomatis pada digit keenam; saat ponsel miring, keypad ditampilkan di samping judul.",
  156: X[156][1].replace(/tur UI \d+ layar/, "tur UI"),
  157: "Hasil pada STD-DDB-001 v1.0 (22 November 2026): varian demo 63 lulus dan 1 dilewati dari 64 test, varian online 57 lulus dan 1 dilewati dari 58 test. Uji sistem Android: 28 dari 28 kasus lulus pada APK final di emulator, termasuk SA-08 (pindai QRIS) dengan kamera emulator; SA-08 juga lulus di ponsel fisik Realme RMX3867 pada 14 November 2026.",
  163: X[163][1],
  164: "Tombol Analitik di layar Riwayat dapat terlipat dua baris pada layar selebar 320 dp. PIN transaksi memakai dialog dengan kolom teks, sedangkan PIN setelah login memakai keypad.",
  165: "Uji pada ponsel fisik Realme RMX3867 (Android 16) lulus pada 14 November 2026 dengan build sebelum pembaruan tampilan: kamera QRIS, notifikasi, dan berbagi bukti. Pengulangan pada build terbaru dicatat sebagai tindak lanjut di STD-DDB-001.",
};
const CODES = [["AND-01", "auth/login", "Autentikasi"], ["AND-02", "auth/register", "Autentikasi"], ["AND-03", "auth/otp", "Autentikasi"], ["AND-04", "auth/ekyc", "Autentikasi"], ["AND-05", "auth/create_pin", "Autentikasi"], ["AND-06", "auth/enter_pin", "Autentikasi"], ["AND-07", "auth/recovery", "Autentikasi"],
  ["AND-08", "home", "Dashboard dan akun"], ["AND-09", "account/chart", "Dashboard dan akun"], ["AND-10", "account/pockets", "Dashboard dan akun"], ["AND-11", "transfer", "Transaksi"], ["AND-12", "qris/pay", "Transaksi"], ["AND-13", "qris/receive", "Transaksi"], ["AND-14", "topup", "Transaksi"],
  ["AND-15", "virtualcard/home", "Dashboard dan akun"], ["AND-16", "history", "Riwayat"], ["AND-17", "history/detail/{txId}", "Riwayat"], ["AND-18", "history/analytics", "Riwayat"], ["AND-19", "profile", "Profil dan notifikasi"], ["AND-20", "profile/security", "Profil dan notifikasi"], ["AND-21", "profile/sessions", "Profil dan notifikasi"], ["AND-22", "profile/notification-settings", "Profil dan notifikasi"], ["AND-23", "notifications", "Profil dan notifikasi"]];

// ---------- versi dan status yang berubah di dalam tabel ----------
const fixTable = (i, rows) => rows.map((r) => r.map((c) => c
  .replace(/Emulator Pixel 8 \(Android 17, API 37\) dan ponsel Realme RMX3867 \(Android 16\), berdasarkan review 16 Nov 2026\./, "Emulator Pixel 8 (Android 17, API 37) dan ponsel Realme RMX3867 (Android 16).")
  .replace(/RAM minimum: Perlu Ditentukan \(pemilik: [^)]*\)\./, "RAM 8 GB terbukti cukup untuk build bila aplikasi lain ditutup; 16 GB disarankan bila emulator dan backend berjalan bersamaan.")
  .replace(/Diagram Account, Transfer, Top-up, Virtual Card, History & Dashboard, dan Authentication dilengkapi pada Lampiran A.*$/, "Activity diagram Authentication, Account, Transfer, Top-up, Virtual Card, History & Dashboard, serta pengaturan dan kotak notifikasi ada pada Lampiran A.2.")));

const body = [];
const add = (...a) => body.push(...a);
const T = (i) => R[i] ?? X[i][1];
for (let i = 20; i <= 165; i++) {
  const [k, v] = X[i];
  if (i === 80) { add(table(["Kode", "Rute", "Kelompok"], CODES, [1600, 4470, 3000], { size: 18, center: [0] })); continue; }
  if (i === 97) {
    add(H3("3.4.5 Transfer (UC-01)"), ...figure(path.join(FIG7, "a7_seq_transfer.png"), "Gambar 3.10 Sequence transfer (varian online)", 600),
      P("Seluruh path diawali /api/v1. Pada varian demo, LocalTransferRepository menggantikan RemoteTransferRepository dan memakai Ledger serta InterbankSimulator; status TIMEOUT dipetakan menjadi PROCESSING, sedangkan penolakan menjalankan pengembalian dana. Pada varian online, otorisasi, validasi, biaya, idempotensi, dan posting dilakukan backend."),
      H3("3.4.6 Top-up dan Tagihan"), ...figure(path.join(FIG7, "a8_seq_topup.png"), "Gambar 3.11 Sequence top-up dan tagihan (varian online)", 600),
      P("Endpoint inquiry yang dipanggil bergantung pada jenis layanan; pulsa dan e-wallet langsung meminta quote. Server menyusun ulang pesanan dari parameter quote, dan kunci idempotensi dikirim saat eksekusi. Pemetaan lengkap endpoint ada pada Lampiran A.3."));
    i = 98; continue;
  }
  if (k === "table") { add(tbl(fixTable(i, v))); continue; }
  if (k === "img") { add(media(v)); continue; }
  const t = T(i);
  if (k === "Image Caption") { add(CAP(t.replace("Gambar 3.10 State", "Gambar 3.12 State").replace("Gambar 3.11 State", "Gambar 3.13 State"))); continue; }
  if (k === "Heading 1") { add(H1(t.replace(/^(BAB \w+) /, "$1  "))); continue; }
  if (k === "Heading 2") { add(H2(t)); continue; }
  if (k === "Heading 3") { add(H3(t)); continue; }
  if (k === "Source Code") { add(code(i === 136 ? t.replace(/Diproses\s*FUNCTION/, "Diproses\n\nFUNCTION") : t), gap()); continue; }
  if (k.startsWith("Compact")) { add(B(t)); continue; }
  if (/^Tabel \d/.test(t)) { add(CAP(t)); continue; }
  add(P(t));
}
add(H1("REFERENSI"),
  ...["SRS-DDB-001 v0.1, Software Requirements Specification.", "SAD-DDB-001 v1.0 (Architectural Design) dan FSD-DDB-001 v1.0 (Functional Design).", "SDD-DDB-001-BE v1.0, Detailed Design Backend & Database.", "UIM-DDB-001-AND v1.0, User Interface Map Android; STD-DDB-001 v1.0, Pengujian Sistem.", "PRJ-DDB-001 v1.0, Project Document; TUT-DDB-001-AND v1.1, Panduan Uji Aplikasi di Ponsel.", "Kode sumber code_area/apps/android, termasuk README.md, NOTES.md, app/build.gradle.kts, dan gradle/libs.versions.toml."].map((t) => B(t)));

add(H1("LAMPIRAN A  DIAGRAM TAMBAHAN MODUL ANDROID"),
  P("Lampiran ini melengkapi gambar pada Bab III dan Bab IV untuk modul selain QRIS dan Profile. Class diagram memuat kelas dan fungsi utama yang dikutip dari kode; atribut lengkap mengikuti implementasi."),
  H2("A.1 Class Diagram Ringkas Modul Lain"));
[["a1_auth.png", "Gambar A.1 Authentication", 430], ["a2_account.png", "Gambar A.2 Account", 430], ["a3_transfer.png", "Gambar A.3 Transfer", 400], ["a4_topup.png", "Gambar A.4 Top-up", 400], ["a5_card.png", "Gambar A.5 Virtual Card", 400], ["a6_history.png", "Gambar A.6 History dan Home", 600]]
  .forEach(([f, t, w]) => add(...figure(path.join(FIG7, f), t, w)));
add(H2("A.2 Activity Diagram Modul Lain"));
[[197, 198], [199, 200], [201, 202], [203, 204], [205, 206], [207, 208]].forEach(([a, b], n) => add(media(X[a][1], 400), CAP(X[b][1].replace(/Gambar A\.\d+/, `Gambar A.${7 + n}`))));
add(P(X[221][1]), media(X[222][1], 400), CAP("Gambar A.13 Activity diagram pengaturan dan kotak notifikasi"));
add(H2("A.3 Pemetaan Endpoint Sequence Diagram"),
  P("Tabel A.1 memuat path REST lengkap pada varian online menurut DdbApi.kt dan SDD-DDB-001-BE."),
  tbl(X[214][1]), CAP("Tabel A.1 Pemetaan endpoint empat sequence diagram"),
  ...[216, 217, 218, 219].map((i) => B(X[i][1].replace(/^•\s*/, ""))));

S.build(meta, body, OUT);
