// DVM-DDB-001-WEB – Developer Manual Web. Usage: node doc7w.js <out.docx> <logo.jpeg>
const S = require("./ddbstyle");
const { D, P, H1, H2, H3, B, NL, CAP, NOTE, table, PW } = S;
const { Paragraph, TextRun, Table, TableRow, TableCell, WidthType, ShadingType, BorderStyle } = D;
const [, , OUT, LOGO] = process.argv;

function code(src) {
  const bd = { style: BorderStyle.SINGLE, size: 4, color: "808080" };
  return new Table({ width: { size: PW, type: WidthType.DXA }, columnWidths: [PW], rows: [new TableRow({ children: [new TableCell({
    width: { size: PW, type: WidthType.DXA }, borders: { top: bd, bottom: bd, left: bd, right: bd },
    shading: { fill: "F7F7F7", type: ShadingType.CLEAR, color: "auto" }, margins: { top: 80, bottom: 80, left: 140, right: 140 },
    children: src.replace(/^\n/, "").replace(/\s+$/, "").split("\n").map((l) => new Paragraph({ spacing: { after: 0, line: 240 },
      children: [new TextRun({ text: l.length ? l : " ", font: "Consolas", size: 18, color: "000000" })] })) })] })] });
}
let n = {};
const C = (kind, ch) => { const k = kind + ch; n[k] = (n[k] || 0) + 1; return `${kind} ${ch}.${n[k]}`; };

const meta = {
  docLabel: "Dokumen 7 – Volume Web", title: "Developer Manual", subtitle: "Panduan Pengembang dan Keterangan Source Code – Aplikasi Web ddb-web",
  code: "DVM-DDB-001-WEB", version: "0.5", statusShort: "Draf Awal untuk PIC",
  status: "Draf awal disiapkan PM dari kode aktual – untuk ditelaah dan dilengkapi PIC (Steven Wibisana; bagian komponen UI: Nabila Puspa Annastasya)",
  module: "Developer Manual – Web (React + Vite)", createdBy: "Salomo Putra Glorious Hokoyoku (Project Manager) – draf awal",
  updatedBy: "Salomo Putra Glorious Hokoyoku (Project Manager)", team: "Kelompok Jaran Goyang – Web Application Team (PIC: Steven Wibisana; komponen UI: Nabila)",
  refs: "SDD-DDB-001-WEB v0.5; SAD-DDB-001 v0.9.4 Bab VIII; DVM-DDB-001-BE v0.9.2; apps/web/README.md; kode apps/web per 14 November 2026",
  date: "14 November 2026", logo: LOGO,
  revisions: [
    ["0.5", "14 Nov 2026", "Salomo P.G. Hokoyoku", "Draf awal dari kode aktual: setup, menjalankan, konfigurasi, keterangan source code per berkas, pola pengembangan, integrasi API, pengujian, build/deploy, troubleshooting. Diserahkan ke PIC."],
  ],
  extraControl: [
    P("**Catatan untuk PIC (hapus setelah ditelaah):** Steven memverifikasi bahwa setiap langkah Bab II–III dapat diikuti di laptop sendiri (uji keterikutan setup) dan mencatat hasilnya. Nabila memeriksa dan melengkapi Bab V bagian 5.3 (komponen UI). Setelah itu naikkan versi dan minta review Salomo."),
  ],
};

const body = [
  H1("BAB I  PENDAHULUAN"),
  H2("1.1 Tujuan"),
  P("Manual ini membantu pengembang menyiapkan lingkungan, menjalankan, memahami, menguji, dan mengubah aplikasi Web nasabah Diponegoro Digital Bank (ddb-web). Bab V menjelaskan setiap berkas source code. Bab VI menjelaskan pola yang wajib diikuti saat menambah halaman atau transaksi baru."),
  H2("1.2 Pembaca"),
  B("Anggota Web Application Team yang melanjutkan atau meninjau kode Web."),
  B("Penguji (D06) dan penyusun User Manual Web (D09W) yang perlu menjalankan aplikasi."),
  B("Penyusun System Program Demo (D08)."),
  P("Pembaca diasumsikan memahami dasar JavaScript, HTML/CSS, dan konsep komponen React. Rancangan rinci (UML, workflow, pseudocode) ada di SDD-DDB-001-WEB dan tidak diulang di sini."),
  H2("1.3 Ringkasan Teknologi"),
  table(["Komponen", "Versi"], [
    ["React + react-dom", "18.3.1"],
    ["react-router-dom", "6.28.0"],
    ["axios", "1.7.9"],
    ["Vite + @vitejs/plugin-react", "5.4.11 / 4.3.4"],
    ["Ikon / QR", "lucide-react 0.468.0, qrcode.react 4.1.0"],
    ["Bahasa", "JavaScript (JSX), tanpa TypeScript"],
  ], [3000, 6070], { boldFirst: true }),
  CAP(C("Tabel", 1) + " Teknologi aplikasi Web"),

  H1("BAB II  PERSIAPAN LINGKUNGAN"),
  H2("2.1 Perangkat yang Dibutuhkan"),
  table(["Perangkat", "Wajib?", "Keterangan"], [
    ["Node.js 20 atau lebih baru (termasuk npm)", "Wajib", "Unduh versi LTS dari nodejs.org; periksa dengan node -v"],
    ["Backend ddb-backend", "Wajib", "Web tidak bisa login tanpa backend. Lihat DVM-DDB-001-BE atau Bab III di bawah"],
    ["JDK 21+", "Wajib (untuk backend)", "Dapat memakai JDK bawaan Android Studio (jbr)"],
    ["Git", "Disarankan", "Mengambil kode dari repositori GitHub tim"],
    ["VS Code", "Disarankan", "Editor; ekstensi ES7 React snippets dan Prettier membantu"],
    ["Chrome / Edge / Firefox", "Wajib", "Menjalankan dan men-debug aplikasi (DevTools: tab Network dan Console)"],
  ], [3000, 1600, 4470], { boldFirst: true, size: 18 }),
  CAP(C("Tabel", 2) + " Perangkat pengembang Web"),
  H2("2.2 Mengambil Kode"),
  code(`
git clone https://github.com/SOL-glorious26/diponegoro-digital-bank.git
cd diponegoro-digital-bank\\code_area\\apps\\web
npm install          # sekali saja, mengunduh dependensi ke node_modules/
`),
  CAP(C("Gambar", 2) + " Mengambil kode dan memasang dependensi"),
  H2("2.3 Struktur Folder"),
  code(`
code_area/apps/web/
├── index.html            halaman induk (div#root)
├── package.json          dependensi + skrip npm (dev, build, preview)
├── vite.config.js        port 5173 dan proxy /api → backend
├── public/favicon.svg    logo
└── src/
    ├── main.jsx          titik masuk + daftar route
    ├── styles.css        seluruh gaya (responsif)
    ├── api/client.js     axios + token + refresh + Idempotency-Key
    ├── auth/             AuthContext (sesi) dan ProtectedRoute (guard)
    ├── components/       komponen yang dipakai bersama
    ├── lib/format.js     format rupiah/tanggal, util nominal & perangkat
    └── pages/            satu berkas per menu
`),
  CAP(C("Gambar", 2) + " Struktur folder aplikasi Web"),

  H1("BAB III  MENJALANKAN APLIKASI"),
  H2("3.1 Menjalankan Backend"),
  P("Cara tercepat memakai database H2 di memori (tanpa memasang PostgreSQL). Buka terminal PowerShell pertama:"),
  code(`
cd code_area\\backend
.\\run-local.ps1 -H2
# tunggu tulisan "Started DdbApplication" → backend aktif di http://localhost:8080
`),
  H2("3.2 Menjalankan Web"),
  P("Buka terminal kedua:"),
  code(`
cd code_area\\apps\\web
npm run dev
# buka http://localhost:5173
`),
  P("Vite meneruskan setiap permintaan /api ke http://localhost:8080, sehingga peramban hanya melihat satu origin dan tidak memerlukan CORS."),
  H2("3.3 Variasi"),
  table(["Kebutuhan", "Perintah"], [
    ["Backend di komputer lain", "$env:DDB_API=\"http://<ip>:8080\"; npm run dev"],
    ["Dibuka dari HP/laptop lain di Wi-Fi yang sama", "npm run dev -- --host   lalu buka http://<ip-laptop>:5173"],
    ["Build produksi", "npm run build   → folder dist/"],
    ["Mencoba hasil build", "npm run preview   → http://localhost:4173 (proxy sama)"],
  ], [3600, 5470], { boldFirst: true, size: 18 }),
  CAP(C("Tabel", 3) + " Variasi cara menjalankan"),
  H2("3.4 Akun Demo"),
  table(["Username", "Password", "PIN", "Rekening"], [
    ["demo", "Demo@1234", "123456", "1400123456 (riwayat 5 bulan)"],
    ["andi / budi / citra", "Demo@1234", "123456", "1400000002 / 03 / 04"],
  ], [2400, 1800, 1300, 3570], { boldFirst: true }),
  CAP(C("Tabel", 3) + " Akun demo (dibuat otomatis oleh backend)"),
  H2("3.5 Verifikasi"),
  NL([
    "Buka http://localhost:5173 → halaman Masuk tampil.",
    "Masuk sebagai demo / Demo@1234 → Beranda menampilkan saldo dan grafik.",
    "Transfer Rp10.000 ke 1400000002 dengan PIN 123456 → bukti transfer tampil.",
    "Keluar, masuk sebagai andi → transaksi masuk terlihat di Riwayat.",
  ]),

  H1("BAB IV  KONFIGURASI"),
  table(["Pengaturan", "Lokasi", "Nilai bawaan", "Keterangan"], [
    ["Alamat backend (dev)", "env DDB_API, vite.config.js", "http://localhost:8080", "Target proxy /api"],
    ["Port dev / preview", "vite.config.js", "5173 / 4173", "Ubah bila port sudah dipakai"],
    ["Base URL API", "src/api/client.js", "/api/v1", "Selalu relatif; host ditentukan proxy atau gateway"],
    ["Timeout permintaan", "src/api/client.js", "20000 ms", "Lebih dari ini → pesan gagal terhubung"],
    ["Kunci refresh token", "src/api/client.js", "sessionStorage ddb.refreshToken", "Hilang saat tab ditutup"],
    ["ID perangkat", "src/lib/format.js", "localStorage ddb.deviceId", "Membedakan sesi di menu Profil → Sesi aktif"],
    ["Polling notifikasi", "src/components/Layout.jsx", "30000 ms", "Jumlah belum dibaca di lonceng"],
  ], [2100, 2400, 2300, 2270], { boldFirst: true, size: 17 }),
  CAP(C("Tabel", 4) + " Konfigurasi aplikasi Web"),

  H1("BAB V  KETERANGAN SOURCE CODE"),
  H2("5.1 Berkas Kunci yang Perlu Dipahami Lebih Dulu"),
  P("Urutan membaca yang disarankan: main.jsx → api/client.js → auth/AuthContext.jsx → auth/ProtectedRoute.jsx → components/PinDialog.jsx → pages/Transfer.jsx. Setelah enam berkas ini dipahami, halaman lain mengikuti pola yang sama."),
  H2("5.2 Infrastruktur"),
  table(["Berkas", "Isi dan fungsi penting"], [
    ["main.jsx", "Mendaftarkan route: GuestOnly (/login, /daftar, /lupa), RequireAuth onboarding (/ekyc, /pin/buat), RequireAuth + Layout (9 halaman). Route tak dikenal diarahkan ke /."],
    ["api/client.js", "api = axios.create({baseURL: '/api/v1'}). setTokens/clearTokens; interceptor permintaan menambah Bearer token; interceptor respons menangani 401 dengan refreshSession() (satu promise bersama) lalu mengulang permintaan. errorMessage(err) mengambil message dari backend; errorCode(err); newIdempotencyKey() (UUID); withKey(key) membuat header Idempotency-Key."],
    ["auth/AuthContext.jsx", "AuthProvider: state loading/in/out, user, notice. login() memanggil /auth/login dengan deviceId/deviceName; logout(all) memanggil /auth/logout atau /auth/logout-all; acceptTokens() dipakai setelah verifikasi OTP registrasi; reloadUser() memuat /me. onboardingStep(user) → login | ekyc | pin | done."],
    ["auth/ProtectedRoute.jsx", "RequireAuth({onboarding}) dan GuestOnly. Menampilkan Spinner selama sesi dipulihkan."],
    ["lib/format.js", "rupiah(), signedRupiah(), dateTime() dan dateOnly() dalam WIB, onlyDigits(), parseAmount(), formatAmountInput(), deviceId(), deviceName()."],
  ], [2300, 6770], { boldFirst: true, size: 18 }),
  CAP(C("Tabel", 5) + " Berkas infrastruktur"),
  H2("5.3 Komponen UI Bersama (bagian Nabila)"),
  table(["Komponen", "Berkas", "Kegunaan"], [
    ["Layout, Brand", "components/Layout.jsx", "Sidebar 8 menu (NAV), topbar dengan avatar dan lonceng notifikasi, tombol Keluar; menu geser di layar sempit"],
    ["PinDialog", "components/PinDialog.jsx", "Modal PIN 6 digit; props title, summary, onSubmit(pin), onClose; menampilkan error backend dan mengosongkan PIN bila gagal"],
    ["TxIcon, TxRow, Receipt, TxDetailModal", "components/TxDetail.jsx", "Ikon arah transaksi, baris daftar, bukti transaksi (bisa dicetak), modal detail + jurnal + cek status"],
    ["BalanceChart", "components/BalanceChart.jsx", "Grafik garis SVG saldo harian/mingguan/bulanan + tabel titik data; menghormati sembunyikan saldo"],
    ["Avatar", "components/Avatar.jsx", "Foto profil atau inisial"],
    ["Spinner, Button, Field, Alert, Card, Modal, StatusBadge, Rows, Empty", "components/ui.jsx", "Elemen dasar dengan gaya seragam; Button mendukung variant dan loading; Alert kind error/info/success"],
  ], [2900, 2300, 3870], { boldFirst: true, size: 17 }),
  CAP(C("Tabel", 5) + " Komponen UI bersama"),
  H2("5.4 Halaman"),
  table(["Berkas", "Route", "FR", "Isi"], [
    ["AuthPages.jsx", "/login, /daftar, /lupa, /ekyc, /pin/buat", "01–05", "LoginPage, RegisterPage (+OTP, kirim ulang), EkycPage (memantau status), CreatePinPage, RecoveryPage (3 langkah)"],
    ["Dashboard.jsx", "/", "06, 07, 25", "Saldo + sembunyikan, BalanceChart, grafik bulanan (MonthlyChart), 5 transaksi terakhir"],
    ["Transfer.jsx", "/transfer", "10–14", "Form → konfirmasi (inquiry + quote) → PinDialog → bukti; favorit; cek status antarbank"],
    ["TopUp.jsx", "/topup", "18–20", "OrderForm per jenis (pulsa, data, PLN, tagihan, e-wallet), quote, PIN, bukti"],
    ["Qris.jsx", "/qris", "16, 17", "QRIS pribadi, QRIS dinamis dengan nominal, salin payload, simulasi dana masuk"],
    ["History.jsx", "/riwayat", "23, 24, 26", "Filter (jenis, arah, status, rentang), pencarian, muat lebih banyak (20/halaman), detail, ekspor CSV/PDF"],
    ["Pockets.jsx", "/kantong", "08, 09", "CreatePocket, EditPocket, LockPocket, MoveDialog (isi/tarik/tarik dini dengan PIN), hapus"],
    ["Cards.jsx", "/kartu", "21, 22", "Buat kartu, lihat detail dengan PIN, bekukan/aktifkan, LimitDialog"],
    ["Profile.jsx", "/profil", "05, 27, 28", "PhotoEditor, ProfileCard (+OTP), SecurityCard (password/PIN), NotificationSettings, Sessions + logout semua perangkat"],
    ["Notifications.jsx", "/notifikasi", "29", "Daftar notifikasi, tandai dibaca satu/semua, memperbarui lonceng"],
  ], [1700, 2200, 1000, 4170], { boldFirst: true, size: 17 }),
  CAP(C("Tabel", 5) + " Halaman dan kebutuhan fungsional"),

  H1("BAB VI  POLA PENGEMBANGAN"),
  H2("6.1 Menambah Halaman Baru"),
  NL([
    "Buat src/pages/NamaHalaman.jsx yang mengekspor komponen default.",
    "Daftarkan route di main.jsx di dalam <Route element={<Layout />}> agar terlindungi guard dan memakai sidebar.",
    "Bila perlu menu, tambahkan entri pada array NAV di components/Layout.jsx (to, label, icon dari lucide-react).",
    "Ambil data di useEffect dengan api.get(...); tampilkan Spinner saat memuat dan Alert berisi errorMessage(ex) saat gagal.",
  ]),
  code(`
import { useEffect, useState } from "react";
import { api, errorMessage } from "../api/client";
import { Alert, Card, Spinner } from "../components/ui";

export default function Contoh() {
  const [data, setData] = useState(null);
  const [err, setErr] = useState(null);
  useEffect(() => { api.get("/accounts/me").then((r) => setData(r.data)).catch((ex) => setErr(errorMessage(ex))); }, []);
  if (err) return <Alert>{err}</Alert>;
  if (!data) return <Spinner label="Memuat…" />;
  return <Card title="Contoh">{data.accountNumber}</Card>;
}
`),
  CAP(C("Gambar", 6) + " Kerangka halaman baru"),
  H2("6.2 Menambah Transaksi Keuangan (Wajib Diikuti)"),
  P("Setiap fitur yang memindahkan uang harus mengikuti pola Transfer agar tidak terjadi debit ganda:"),
  NL([
    "Minta ringkasan (quote) ke backend dan tampilkan layar konfirmasi.",
    "Saat masuk layar konfirmasi, buat **satu** kunci: setKey(newIdempotencyKey()).",
    "Tampilkan PinDialog; di onSubmit(pin) kirim permintaan dengan withKey(key).",
    "**Jangan** membuat kunci baru saat PIN salah atau saat mengulang; kunci baru hanya dibuat ketika pengguna memulai transaksi baru.",
    "Tampilkan bukti dari respons backend (komponen Receipt).",
  ]),
  code(`
const { data } = await api.post("/topup/orders", { ...order.request, pin }, withKey(key));
`),
  H2("6.3 Memanggil API Baru"),
  B("Lihat daftar endpoint di code_area/backend/docs/API.md atau Swagger UI http://localhost:8080/swagger-ui.html."),
  B("Tulis path tanpa /api/v1 (sudah menjadi baseURL), contoh api.get(\"/history\", { params: { page: 0, size: 20 } })."),
  B("Jangan menulis ulang pesan error; tampilkan errorMessage(ex). Gunakan errorCode(ex) bila perlu cabang logika."),
  H2("6.4 Catatan Gaya Kode"),
  B("Komponen fungsi + hooks; nama berkas PascalCase untuk komponen, camelCase untuk util."),
  B("Teks antarmuka berbahasa Indonesia; nominal selalu lewat rupiah(), tanggal lewat dateTime()."),
  B("Gaya ditulis di styles.css dengan nama kelas deskriptif; hindari gaya inline kecuali nilai dinamis."),
  B("Setiap input diberi label (Field) agar dapat diakses pembaca layar dan dikenali skrip uji."),

  H1("BAB VII  INTEGRASI DENGAN BACKEND"),
  table(["Aturan", "Penerapan di Web"], [
    ["Autentikasi JWT", "Access token 15 menit di memori; refresh token 7 hari di sessionStorage; 401 → refresh otomatis sekali → ulangi; gagal → kembali ke login."],
    ["Idempotency-Key", "Wajib untuk transfer, top-up, kantong (isi/tarik/hapus), simulasi QRIS masuk. Dibuat dengan newIdempotencyKey(), dikirim dengan withKey(key)."],
    ["PIN", "Transfer, top-up, tarik dini kantong, detail kartu, ubah PIN. Salah 3× → backend memblokir 5 menit dan mengirim pesan yang ditampilkan PinDialog."],
    ["Format error", "{code, message, fields}; message langsung ditampilkan."],
    ["Uang dan waktu", "Bilangan bulat rupiah; waktu ISO-8601 UTC ditampilkan WIB."],
    ["OTP simulasi", "Saat EXPOSE_OTP=true backend menyertakan demoCode; halaman Daftar/Profil/Pemulihan menampilkannya agar dapat diuji tanpa SMS."],
  ], [2200, 6870], { boldFirst: true, size: 18 }),
  CAP(C("Tabel", 7) + " Aturan kontrak API yang diterapkan Web"),

  H1("BAB VIII  PENGUJIAN"),
  H2("8.1 Menjalankan Uji Otomatis"),
  P("Pastikan backend (port 8080) dan Web (port 5173) berjalan, lalu dari folder code_area/pengujian:"),
  code(`
npm run web:api        # 38 pemeriksaan kontrak API
npm run web:ui         # WB-01..WB-10, Chrome headless
npm run web:extra      # WB-11 profil + OTP
npm run web:fitur      # WB-13..WB-15
npm run web:browsers   # Chrome, Edge, Firefox
npm run lintas         # LP-01..LP-04 (butuh emulator Android)
`),
  P("Hasil ditulis ke pengujian/hasil/; bukti resmi disalin ke pengujian/bukti/<tanggal>/ dan dilaporkan di D06. Hasil terakhir: Web 15/15 lulus."),
  H2("8.2 Uji Manual Cepat"),
  B("Login, refresh halaman (F5) → tetap masuk; tutup tab lalu buka lagi → harus login ulang."),
  B("Transfer dengan PIN salah 3× → pesan blokir 5 menit."),
  B("Perkecil jendela ke lebar ponsel → sidebar menjadi menu geser."),

  H1("BAB IX  BUILD DAN DEPLOYMENT"),
  code(`
cd code_area\\apps\\web
npm run build      # menghasilkan dist/ (index.html + assets/)
`),
  B("**Satu origin dengan gateway (disarankan):** salin dist/ ke server dan layani lewat Nginx yang sama dengan /api → backend. Tidak perlu CORS."),
  B("**Hosting statis terpisah** (Netlify/Vercel/GitHub Pages): atur rewrite /api ke backend, atau daftarkan origin Web di DDB_CORS_ALLOWED_ORIGINS backend; tambahkan fallback semua path ke index.html karena memakai BrowserRouter."),
  B("Wajib HTTPS di produksi agar token tidak terbaca di jaringan."),
  P("Status saat ini: aplikasi belum di-deploy ke server publik; dijalankan lokal untuk pengujian dan demo."),

  H1("BAB X  TROUBLESHOOTING"),
  table(["Gejala", "Penyebab", "Solusi"], [
    ["\"Tidak dapat terhubung ke server. Pastikan backend berjalan.\"", "Backend mati atau port berbeda", "Jalankan backend (Bab III); periksa http://localhost:8080/actuator/health; atur DDB_API"],
    ["npm : File ... npm.ps1 cannot be loaded", "Kebijakan eksekusi PowerShell", "Pakai npm.cmd run dev, atau Set-ExecutionPolicy -Scope CurrentUser RemoteSigned"],
    ["Port 5173 is in use", "Vite lain masih berjalan", "Tutup terminal lama atau jalankan npm run dev -- --port 5174"],
    ["Setelah buka tab baru harus login lagi", "Disengaja: refresh token di sessionStorage", "Bukan bug (SAD ADR-12)"],
    ["\"Sesi Anda telah berakhir\"", "Refresh token ditolak (logout semua perangkat, backend direstart dengan H2 sehingga data hilang)", "Login ulang; dengan profil H2 data kembali ke data demo setiap backend dijalankan"],
    ["Halaman putih setelah build di hosting", "Fallback route belum diatur", "Arahkan semua path ke index.html"],
    ["Error CORS di Console", "Web dibuka dari origin lain tanpa proxy", "Pakai proxy Vite/gateway atau tambahkan origin ke DDB_CORS_ALLOWED_ORIGINS"],
  ], [2700, 2900, 3470], { size: 17 }),
  CAP(C("Tabel", 10) + " Masalah umum dan solusinya"),
  H2("Keterbatasan yang Diketahui"),
  B("Pemindaian QRIS hanya di Android; foto profil berupa URL; notifikasi memakai polling."),
  H2("Referensi"),
  B("SDD-DDB-001-WEB v0.5 – Detailed Design Web."),
  B("DVM-DDB-001-BE v0.9.2 – Developer Manual Backend."),
  B("code_area/apps/web/README.md, code_area/backend/docs/API.md, code_area/pengujian/README.md."),
];

S.build(meta, body, OUT);
