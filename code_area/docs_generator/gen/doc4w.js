// SDD-DDB-001-WEB – Detailed Design Web. Usage: node doc4w.js <fig6_dir> <out.docx> <logo.jpeg>
const S = require("./ddbstyle");
const { D, P, H1, H2, H3, B, CAP, NOTE, table, img, PW } = S;
const { Paragraph, TextRun, Table, TableRow, TableCell, WidthType, ShadingType, BorderStyle } = D;
const [, , FIG, OUT, LOGO] = process.argv;
const f = (n) => `${FIG}/${n}`;

function code(src) {
  const bd = { style: BorderStyle.SINGLE, size: 4, color: "808080" };
  return new Table({ width: { size: PW, type: WidthType.DXA }, columnWidths: [PW], rows: [new TableRow({ children: [new TableCell({
    width: { size: PW, type: WidthType.DXA }, borders: { top: bd, bottom: bd, left: bd, right: bd },
    shading: { fill: "F7F7F7", type: ShadingType.CLEAR, color: "auto" }, margins: { top: 80, bottom: 80, left: 140, right: 140 },
    children: src.replace(/^\n/, "").replace(/\s+$/, "").split("\n").map((l) => new Paragraph({ spacing: { after: 0, line: 240 },
      children: [new TextRun({ text: l.length ? l : " ", font: "Consolas", size: 18, color: "000000" })] })) })] })] });
}

const meta = {
  docLabel: "Dokumen 4 – Volume Web", title: "Detailed Design", subtitle: "Rancangan Rinci Aplikasi Web Nasabah (ddb-web)",
  code: "SDD-DDB-001-WEB", version: "0.5", statusShort: "Draf Awal untuk PIC",
  status: "Draf awal disiapkan PM dari kode aktual – untuk ditelaah, dilengkapi, dan diambil alih PIC (Rafi Fadhillah)",
  module: "Detailed Design – Web (React + Vite)", createdBy: "Salomo Putra Glorious Hokoyoku (Project Manager) – draf awal",
  updatedBy: "Salomo Putra Glorious Hokoyoku (Project Manager)", team: "Kelompok Jaran Goyang – Web Application Team (PIC: Rafi Fadhillah)",
  refs: "SRS-DDB-001 v0.1; SAD-DDB-001 v0.9.4 Bab VIII; FSD-DDB-001 v0.9.2; SDD-DDB-001-BE v0.9.2; UIM-DDB-001-WEB v0.3; kode apps/web per 14 November 2026",
  date: "14 November 2026", logo: LOGO,
  revisions: [
    ["0.5", "14 Nov 2026", "Salomo P.G. Hokoyoku", "Draf awal dari kode aktual apps/web: struktur, UML (komponen, route, sequence, state), workflow, pseudocode, perancangan software/hardware. Diserahkan ke PIC untuk ditelaah dan dilengkapi."],
  ],
  extraControl: [
    P("**Catatan untuk PIC (hapus setelah ditelaah):** draf ini ditulis dari kode yang sudah berjalan, bukan dari rencana. Tugas PIC: (1) membaca kode di code_area/apps/web sambil mencocokkan setiap bagian dokumen; (2) menambah penjelasan dengan bahasa sendiri bila perlu; (3) memperbarui tabel revisi dan menaikkan versi; (4) meminta review ke Salomo."),
  ],
};

let fig = {};
const G = (ch) => { fig[ch] = (fig[ch] || 0) + 1; return `Gambar ${ch}.${fig[ch]}`; };
let tab = {};
const T = (ch) => { tab[ch] = (tab[ch] || 0) + 1; return `Tabel ${ch}.${tab[ch]}`; };

const body = [
  // ------------------------------------------------------------------ BAB I
  H1("BAB I  PENDAHULUAN"),
  H2("1.1 Tujuan"),
  P("Dokumen ini merinci rancangan aplikasi Web nasabah Diponegoro Digital Bank (ddb-web) sampai tingkat komponen, alur, dan algoritma. Rancangan arsitektur tingkat tinggi Web ditetapkan di SAD-DDB-001 Bab VIII; dokumen ini menurunkannya menjadi struktur berkas, diagram UML, workflow, pseudocode, serta kebutuhan perangkat lunak dan keras."),
  H2("1.2 Ruang Lingkup"),
  P("Aplikasi Web adalah **thin client**: seluruh aturan bisnis (validasi saldo, PIN, limit, idempotensi, ledger) dijalankan backend. Web bertugas menampilkan data, mengumpulkan masukan, mengirim permintaan ke API /api/v1, dan menampilkan pesan dari backend apa adanya. Fitur yang dicakup mengikuti SRS-DDB-001 dan keputusan PM:"),
  B("Login dengan username/email/nomor HP + password, **tanpa OTP**."),
  B("Setiap transaksi keuangan dikonfirmasi **PIN 6 digit** dan membawa header **Idempotency-Key**."),
  B("**Tidak ada Virtual Account.** QRIS di Web hanya **menampilkan QRIS pribadi/dinamis**; pemindaian QRIS (FR-15) khusus Android."),
  B("Halaman transaksi hanya dapat dibuka setelah e-KYC disetujui dan PIN dibuat."),
  H2("1.3 Notasi"),
  P("Diagram memakai notasi UML 2: kotak bertingkat untuk komponen/kelas dengan stereotip «component», «context», «page», «utility»; garis putus-putus berpanah untuk dependensi; belah ketupat hitam untuk komposisi. Sequence diagram memakai panah penuh untuk panggilan dan panah putus-putus abu-abu untuk balasan. Flowchart memakai kotak hijau untuk aksi, belah ketupat kuning untuk keputusan, dan kotak merah untuk jalur gagal."),

  // ------------------------------------------------------------------ BAB II
  H1("BAB II  STRUKTUR PERANGKAT LUNAK WEB"),
  H2("2.1 Struktur Berkas"),
  P("Kode sumber berada di code_area/apps/web/src (± 2.680 baris termasuk CSS). Struktur dipisah per tanggung jawab agar halaman tidak memanggil axios secara langsung tanpa interceptor."),
  table(["Folder / berkas", "Baris", "Tanggung jawab"], [
    ["main.jsx", "57", "Titik masuk: BrowserRouter, AuthProvider, definisi seluruh route"],
    ["api/client.js", "88", "Instance axios /api/v1, penyimpanan token, refresh otomatis saat 401, Idempotency-Key, pesan error"],
    ["auth/AuthContext.jsx", "72", "State sesi (loading/in/out), login, logout, logout semua perangkat, pemulihan sesi, onboardingStep()"],
    ["auth/ProtectedRoute.jsx", "22", "Route guard RequireAuth dan GuestOnly"],
    ["components/Layout.jsx", "81", "Kerangka halaman: sidebar 8 menu, topbar, lonceng notifikasi (polling 30 detik)"],
    ["components/PinDialog.jsx", "44", "Dialog konfirmasi PIN 6 digit untuk semua transaksi"],
    ["components/TxDetail.jsx", "105", "Baris transaksi, bukti (Receipt), modal detail + jurnal, cek status antarbank"],
    ["components/BalanceChart.jsx", "104", "Grafik saldo harian/mingguan/bulanan (SVG) + tabel titik data"],
    ["components/ui.jsx, Avatar.jsx", "107", "Elemen UI: Button, Field, Alert, Card, Modal, StatusBadge, Rows, Empty, Spinner, Avatar"],
    ["lib/format.js", "40", "Format rupiah dan tanggal WIB, parsing nominal, deviceId/deviceName"],
    ["pages/AuthPages.jsx", "338", "Masuk, Daftar + OTP, e-KYC, Buat PIN, Pemulihan password/PIN"],
    ["pages/Dashboard.jsx", "118", "Beranda: saldo + sembunyikan, grafik saldo, grafik bulanan, transaksi terakhir"],
    ["pages/Transfer.jsx", "175", "Transfer sesama/antarbank, validasi rekening, favorit, bukti"],
    ["pages/TopUp.jsx", "210", "Pulsa, paket data, token PLN, tagihan, e-wallet"],
    ["pages/Qris.jsx", "79", "QRIS pribadi dan dinamis, simulasi dana masuk"],
    ["pages/History.jsx", "110", "Riwayat: filter, pencarian, paginasi, detail, ekspor CSV/PDF"],
    ["pages/Pockets.jsx", "205", "Kantong: buat, isi, tarik, tarik dini, ubah, kunci, hapus"],
    ["pages/Cards.jsx", "97", "Virtual card: buat, detail dengan PIN, bekukan/aktifkan, limit"],
    ["pages/Profile.jsx", "225", "Profil + OTP, foto, ubah password/PIN, pengaturan notifikasi, sesi aktif"],
    ["pages/Notifications.jsx", "71", "Kotak notifikasi, tandai dibaca"],
    ["styles.css", "331", "Gaya global, tata letak responsif (sidebar menjadi menu geser di layar sempit)"],
  ], [2800, 800, 5470], { boldFirst: true, size: 18, center: [1] }),
  CAP(`${T(2)} Inventaris berkas source code Web`),
  H2("2.2 Konvensi Rancangan"),
  B("**Satu pintu API.** Semua halaman memakai objek api dari api/client.js; tidak ada fetch/axios langsung."),
  B("**Pesan error dari backend.** Backend mengirim {code, message, fields}; Web menampilkan message lewat errorMessage() tanpa menerjemahkan ulang, dan memakai code hanya untuk logika (mis. kantong terkunci)."),
  B("**Uang = bilangan bulat rupiah.** Input nominal diformat ribuan saat diketik (formatAmountInput) dan dikirim sebagai angka (parseAmount)."),
  B("**Waktu ditampilkan dalam WIB** (Intl.DateTimeFormat zona Asia/Jakarta); backend menyimpan UTC."),
  B("**State lokal per halaman** dengan useState/useEffect; state global hanya sesi pengguna (AuthContext). Tidak memakai Redux karena data selalu diambil ulang dari backend."),
  B("**Satu Idempotency-Key per layar konfirmasi.** Kunci dibuat saat masuk layar konfirmasi dan dipakai ulang saat PIN salah atau retry."),

  // ------------------------------------------------------------------ BAB III
  H1("BAB III  UML DIAGRAM"),
  H2("3.1 Component/Class Diagram – Inti Aplikasi"),
  P("Gambar 3.1 memperlihatkan hubungan antar bagian utama. App membungkus seluruh aplikasi dengan AuthProvider (context sesi) dan route guard. Halaman dirender di dalam Layout lewat <Outlet/>, memakai komponen bersama, dan memanggil backend hanya melalui api (client.js)."),
  img(f("cls_web_core.png"), 600, { alt: "Diagram komponen Web" }),
  CAP(`${G(3)} Component/class diagram inti aplikasi Web`),
  table(["Elemen", "Peran"], [
    ["AuthProvider", "Menyimpan user dan state sesi; menyediakan login(), logout(all), acceptTokens(), reloadUser() melalui hook useAuth()."],
    ["api (client.js)", "Menyisipkan Authorization: Bearer; saat 401 menjalankan satu refreshSession() untuk semua permintaan yang gagal lalu mengulang permintaan; bila refresh gagal mengakhiri sesi."],
    ["RequireAuth / GuestOnly", "Menentukan apakah halaman boleh dirender, diarahkan ke /login, ke tahap onboarding (/ekyc, /pin/buat), atau ke Beranda."],
    ["Layout", "Sidebar dan topbar; menghitung notifikasi belum dibaca setiap 30 detik."],
    ["PinDialog", "Satu-satunya tempat PIN diketik untuk transaksi; memanggil onSubmit(pin) milik halaman."],
  ], [2400, 6670], { boldFirst: true, size: 18 }),
  CAP(`${T(3)} Peran elemen pada diagram komponen`),

  H2("3.2 Peta Route"),
  P("Gambar 3.2 memetakan seluruh URL beserta kebutuhan fungsional (FR) yang dilayani. Route dikelompokkan menurut guard yang melindunginya."),
  img(f("route_map.png"), 600, { alt: "Peta route Web" }),
  CAP(`${G(3)} Peta route aplikasi Web`),

  H2("3.3 Sequence Diagram"),
  H3("Login"),
  img(f("seq_web_login.png"), 600, { alt: "Sequence login" }),
  CAP(`${G(3)} Sequence diagram login Web`),
  P("Setelah login, access token hanya disimpan di memori (hilang saat tab dimuat ulang) dan refresh token di sessionStorage (hilang saat tab ditutup). Saat halaman dimuat ulang, AuthProvider memulihkan sesi dengan refresh token."),
  H3("Pembaruan Token Otomatis"),
  img(f("seq_web_refresh.png"), 600, { alt: "Sequence refresh token" }),
  CAP(`${G(3)} Sequence diagram pembaruan token saat 401`),
  H3("Transfer"),
  img(f("seq_web_transfer.png"), 600, { alt: "Sequence transfer" }),
  CAP(`${G(3)} Sequence diagram transfer dari Web`),

  H2("3.4 State Diagram"),
  img(f("state_session.png"), 540, { alt: "State sesi" }),
  CAP(`${G(3)} State diagram sesi pengguna (AuthProvider.state)`),
  img(f("state_transfer.png"), 600, { alt: "State halaman transfer" }),
  CAP(`${G(3)} State diagram halaman Transfer (pola sama dipakai Top-up dan Kantong)`),

  // ------------------------------------------------------------------ BAB IV
  H1("BAB IV  WORKFLOW DIAGRAM"),
  H2("4.1 Route Guard"),
  img(f("act_guard.png"), 470, { alt: "Workflow route guard" }),
  CAP(`${G(4)} Workflow RequireAuth`),
  H2("4.2 Konfirmasi Transaksi dengan PIN"),
  img(f("act_pin_tx.png"), 470, { alt: "Workflow konfirmasi PIN" }),
  CAP(`${G(4)} Workflow PinDialog`),
  H2("4.3 Alur Fitur per Halaman"),
  table(["Halaman", "Alur utama", "API yang dipanggil"], [
    ["Daftar (FR-01)", "Isi data → kirim → masukkan OTP (bisa kirim ulang) → akun aktif dan langsung login", "POST /auth/register, /auth/register/verify-otp, /auth/register/resend-otp"],
    ["e-KYC (FR-02)", "Isi NIK + nama + foto → kirim → status PROCESSING dipantau sampai APPROVED/REJECTED", "POST, GET /auth/ekyc"],
    ["Buat PIN (FR-04)", "Ketik PIN 6 digit dua kali → simpan → Beranda", "POST /auth/pin"],
    ["Pemulihan (FR-05)", "3 langkah: minta OTP → verifikasi OTP → setel password/PIN baru", "POST /auth/recovery/request, /verify, /reset"],
    ["Beranda (FR-06, 07, 25)", "Saldo (bisa disembunyikan), grafik saldo, grafik bulanan, transaksi terakhir", "GET /accounts/me, /accounts/me/balance-chart, /history/monthly-summary, /history; PUT /accounts/me/balance-visibility"],
    ["Transfer (FR-10–14)", "Form → inquiry + quote → konfirmasi → PIN → bukti; favorit; cek status antarbank", "GET /transfers/banks, /transfers/favorites; POST /transfers/inquiry, /quote, /transfers, /{id}/check-status, /favorites; DELETE /favorites/{id}"],
    ["Top-up (FR-18–20)", "Pilih jenis → isi tujuan/produk (inquiry PLN/tagihan) → quote → PIN → bukti", "GET /topup/catalog, /data-packages, /pln/inquiry, /bills/inquiry; POST /topup/quote, /topup/orders"],
    ["QRIS (FR-16, 17)", "Tampilkan QRIS pribadi; isi nominal → QRIS dinamis; simulasi dana masuk", "GET /qris/my-qr; POST /qris/simulate-incoming"],
    ["Riwayat (FR-23, 24, 26)", "Filter/pencarian → daftar berhalaman (20) → detail + jurnal → ekspor CSV/PDF", "GET /history, /history/{id}, /history/export"],
    ["Kantong (FR-08, 09)", "Buat, ubah, kunci (tanggal/target), isi, tarik; kantong terkunci → tarik dini dengan PIN; hapus", "GET, POST /accounts/pockets; PUT, DELETE /{id}; POST /{id}/lock, /deposit, /withdraw"],
    ["Virtual Card (FR-21, 22)", "Buat kartu, lihat detail dengan PIN, bekukan/aktifkan, atur limit", "GET, POST /cards; POST /{id}/details, /freeze, /activate; PUT /{id}/limit"],
    ["Profil (FR-05, 27, 28)", "Ubah profil dengan OTP, foto (URL), ubah password/PIN, pengaturan notifikasi, sesi aktif, logout semua perangkat", "PUT /me/photo; POST /me/profile-change, /confirm, /me/password, /me/pin; GET, PUT /me/notification-settings; GET /auth/sessions"],
    ["Notifikasi (FR-29)", "Daftar notifikasi, tandai satu/semua dibaca", "GET /me/notifications; POST /{id}/read, /read-all"],
  ], [1900, 3570, 3600], { boldFirst: true, size: 17 }),
  CAP(`${T(4)} Alur dan API per halaman Web`),

  // ------------------------------------------------------------------ BAB V
  H1("BAB V  ALGORITMA PSEUDOCODE"),
  H2("5.1 Interceptor Permintaan dan Respons (api/client.js)"),
  code(`
ON REQUEST(config):
    IF accessToken ≠ null THEN config.headers.Authorization ← "Bearer " + accessToken
    RETURN config

ON RESPONSE ERROR(error):
    cfg ← error.config ; status ← error.response.status
    skip ← cfg.url diawali /auth/login | /auth/refresh | /auth/register | /auth/recovery
    IF status = 401 AND NOT cfg._retried AND NOT skip THEN
        cfg._retried ← true
        TRY
            refreshSession()            // satu promise dipakai bersama semua 401
            RETURN api(cfg)             // ulangi permintaan asli dengan token baru
        CATCH
            clearTokens() ; onSessionEnded()   // → state "out", pesan "Sesi berakhir"
    THROW error
`),
  CAP(`${G(5)} Pseudocode interceptor axios`),
  H2("5.2 Refresh Token Tunggal (refreshSession)"),
  code(`
FUNCTION refreshSession():
    rt ← sessionStorage["ddb.refreshToken"]
    IF rt = null THEN THROW "no refresh token"
    IF refreshing = null THEN
        refreshing ← POST /api/v1/auth/refresh {refreshToken: rt}
                     .then(r → setTokens(r.data); RETURN r.data)
                     .finally(→ refreshing ← null)
    RETURN refreshing          // permintaan lain menunggu promise yang sama
`),
  CAP(`${G(5)} Pseudocode refreshSession`),
  H2("5.3 Pemulihan Sesi saat Aplikasi Dimuat (AuthProvider)"),
  code(`
ON MOUNT:
    setSessionEndedHandler(→ endSession("Sesi Anda telah berakhir. Silakan masuk kembali."))
    IF NOT hasRefreshToken() THEN state ← "out" ; RETURN
    TRY
        t ← refreshSession()
        user ← t.user ?? GET /me
        state ← "in"
    CATCH ex
        endSession(ex.response ? pesan sesi berakhir : null)   // server mati → tanpa pesan

FUNCTION onboardingStep(user):
    IF user = null                 RETURN "login"
    IF user.ekycStatus ≠ APPROVED  RETURN "ekyc"
    IF NOT user.hasPin             RETURN "pin"
    RETURN "done"
`),
  CAP(`${G(5)} Pseudocode pemulihan sesi dan tahap onboarding`),
  H2("5.4 Transfer (Transfer.jsx)"),
  code(`
FUNCTION toConfirm():
    amount ← parseAmount(form.amount)
    IF amount < MIN THEN tampilkan "Nominal minimal ..." ; RETURN
    POST /transfers/inquiry {bankCode, accountNumber}          // FR-12: nama pemilik
    quote ← POST /transfers/quote {bankCode, accountNumber, amount, note}
    idemKey ← newIdempotencyKey()                              // satu kunci per layar konfirmasi

FUNCTION execute(pin):                                         // dipanggil PinDialog
    result ← POST /transfers {bankCode, accountNumber, amount: quote.amount, note, pin}
             HEADER Idempotency-Key: idemKey
    tutup PinDialog ; tampilkan bukti(result)
    // gagal (PIN salah, saldo kurang) → PinDialog menampilkan pesan, idemKey TIDAK diganti

FUNCTION reset():                                              // "Transfer lagi"
    kosongkan form, quote, result ; idemKey ← null
`),
  CAP(`${G(5)} Pseudocode transfer`),
  H2("5.5 Penarikan Kantong dan Tarik Dini (Pockets.jsx – MoveDialog)"),
  code(`
key ← newIdempotencyKey()   // dibuat sekali saat dialog dibuka
FUNCTION submit():
    IF mode = withdraw AND pocket.locked THEN needPin ← true ; RETURN
    TRY
        IF mode = deposit THEN POST /accounts/pockets/{id}/deposit  {amount} + key
        ELSE                  POST /accounts/pockets/{id}/withdraw {amount} + key
        onDone()
    CATCH ex
        IF errorCode(ex) memuat "LOCK" THEN needPin ← true      // kunci aktif di server
        tampilkan errorMessage(ex)

FUNCTION earlyWithdraw(pin):                                   // BR-06
    POST /accounts/pockets/{id}/withdraw {amount, earlyWithdrawal: true, pin} + key
    onDone()
`),
  CAP(`${G(5)} Pseudocode penarikan kantong`),
  H2("5.6 Ekspor Riwayat (History.jsx)"),
  code(`
FUNCTION exportAs(format):                    // format ∈ {csv, pdf}; FR-26
    params ← cleanParams(filterAktif) + {format}   // from/to hanya bila range = CUSTOM
    TRY
        res ← GET /history/export params, responseType = blob
        name ← nama dari header Content-Disposition ATAU "riwayat-ddb." + format
        url ← URL.createObjectURL(res.data)
        buat <a href=url download=name>, klik, hapus ; revokeObjectURL setelah 1 detik
    CATCH ex
        IF ex.response.data adalah Blob THEN baca JSON di dalamnya untuk message
        tampilkan pesan
`),
  CAP(`${G(5)} Pseudocode ekspor riwayat`),
  H2("5.7 Perubahan Profil dengan OTP (Profile.jsx)"),
  code(`
FUNCTION request():
    body ← {username, email | null, phoneNumber | null}   // draf lengkap, bukan hanya yang berubah
    IF tidak ada kolom yang berubah THEN tampilkan "Tidak ada perubahan." ; RETURN
    otp ← POST /me/profile-change body                   // backend mengirim OTP (demoCode saat simulasi)

FUNCTION confirm(code):
    user ← POST /me/profile-change/confirm {code}
    setUser(user) ; tutup form ; tampilkan "Profil diperbarui."
`),
  CAP(`${G(5)} Pseudocode perubahan profil`),

  // ------------------------------------------------------------------ BAB VI
  H1("BAB VI  PERANCANGAN SOFTWARE/HARDWARE"),
  H2("6.1 Perangkat Lunak"),
  table(["Komponen", "Versi", "Fungsi"], [
    ["React + react-dom", "18.3.1", "Pustaka antarmuka berbasis komponen"],
    ["react-router-dom", "6.28.0", "Routing sisi klien dan route guard"],
    ["axios", "1.7.9", "Klien HTTP dengan interceptor"],
    ["lucide-react", "0.468.0", "Ikon"],
    ["qrcode.react", "4.1.0", "Menggambar QRIS pribadi/dinamis"],
    ["Vite + @vitejs/plugin-react", "5.4.11 / 4.3.4", "Server pengembangan (port 5173, proxy /api) dan build produksi"],
    ["Node.js", "20 atau lebih baru", "Menjalankan Vite dan npm"],
    ["Peramban", "Chrome, Edge, Firefox terbaru", "Lingkungan eksekusi (diuji NFR-10)"],
  ], [2800, 2000, 4270], { boldFirst: true, size: 18 }),
  CAP(`${T(6)} Perangkat lunak aplikasi Web`),
  H2("6.2 Konfigurasi dan Lingkungan"),
  table(["Lingkungan", "Cara kerja"], [
    ["Pengembangan", "npm run dev → Vite di http://localhost:5173. Permintaan /api diteruskan (proxy) ke http://localhost:8080 sehingga peramban tidak memerlukan CORS. Alamat backend lain: variabel DDB_API."],
    ["Pratinjau build", "npm run build → folder dist/ (berkas statis); npm run preview di port 4173 dengan proxy yang sama."],
    ["Produksi (rencana)", "Isi dist/ dilayani di belakang API Gateway Nginx yang sama dengan backend (satu origin), atau di hosting statis dengan origin didaftarkan di DDB_CORS_ALLOWED_ORIGINS backend."],
  ], [2200, 6870], { boldFirst: true, size: 18 }),
  CAP(`${T(6)} Konfigurasi lingkungan Web`),
  H2("6.3 Keamanan Sisi Klien"),
  B("Access token hanya di memori JavaScript; tidak disimpan di localStorage sehingga tidak bertahan setelah tab ditutup."),
  B("Refresh token di sessionStorage; dihapus saat logout atau saat refresh ditolak (mis. setelah logout semua perangkat)."),
  B("PIN dan password tidak pernah disimpan; input PIN bertipe password, hanya angka, maksimal 6 digit."),
  B("Identitas perangkat (deviceId) dibuat sekali per peramban di localStorage agar daftar sesi aktif dapat dibedakan per perangkat."),
  B("Semua aturan keamanan transaksi (blokir PIN 3×, limit, idempotensi) ditegakkan backend; Web hanya meneruskan pesannya."),
  H2("6.4 Perangkat Keras"),
  table(["Kebutuhan", "Minimum"], [
    ["Pengguna", "Perangkat apa pun dengan peramban modern; layar mulai lebar 360 px (tata letak responsif, sidebar menjadi menu geser)"],
    ["Pengembang", "Laptop dengan Node.js 20+, RAM 8 GB (backend Java + Vite + peramban berjalan bersamaan)"],
    ["Server (produksi)", "Mengikuti backend (SDD-DDB-001-BE Bab VI); berkas Web statis ± 1 MB tidak menambah kebutuhan berarti"],
  ], [2200, 6870], { boldFirst: true, size: 18 }),
  CAP(`${T(6)} Kebutuhan perangkat keras`),
  H2("6.5 Rancangan Pengujian Web"),
  P("Pengujian Web dijalankan dari folder code_area/pengujian (Node.js, tanpa pustaka tambahan) dan hasilnya dicatat di STD-DDB-001 (D06)."),
  table(["Skrip", "Kasus", "Isi"], [
    ["npm run web:api", "Kontrak API", "38 pemeriksaan API lewat proxy Vite"],
    ["npm run web:ui", "WB-01 – WB-10", "Uji UI di Chrome headless (20 langkah, termasuk tampilan ponsel)"],
    ["npm run web:extra / web:fitur", "WB-11, WB-13 – WB-15", "Profil + OTP, grafik saldo, foto profil, ubah dan kunci kantong"],
    ["npm run web:token", "WB-12", "Token kedaluwarsa → refresh otomatis"],
    ["npm run web:browsers", "NFR-10", "Alur inti di Chrome, Edge, Firefox"],
    ["npm run lintas", "LP-01 – LP-04", "Transaksi Android terlihat di Web dan sebaliknya"],
  ], [2700, 2000, 4370], { boldFirst: true, size: 18 }),
  CAP(`${T(6)} Skrip pengujian Web`),
  P("Hasil terakhir (D06 v0.9.5): **Web 15/15 lulus**, lintas platform 4/4, tiga peramban lulus."),
  H2("6.6 Keterbatasan dan Rencana Perbaikan"),
  B("Pemindaian QRIS tidak tersedia di Web (butuh kamera; keputusan PM)."),
  B("Foto profil masih berupa URL; unggah berkas menunggu dukungan backend."),
  B("Notifikasi diperbarui dengan polling 30 detik; dapat diganti WebSocket/SSE bila backend menyediakannya."),
  B("Belum di-deploy ke server publik; saat ini dijalankan lokal."),
  H2("Referensi"),
  B("SRS-DDB-001 v0.1 – Software Requirements Specification."),
  B("SAD-DDB-001 v0.9.4 – Architectural Design, Bab VIII (arsitektur Web)."),
  B("SDD-DDB-001-BE v0.9.2 – Detailed Design Backend & Database (kontrak API)."),
  B("UIM-DDB-001-WEB v0.3 – User Interface Map Web."),
  B("Kode sumber code_area/apps/web dan code_area/backend/docs/API.md."),
];

S.build(meta, body, OUT);
