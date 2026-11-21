const fs = require("fs");
const path = require("path");
const S = require("./ddbstyle");
const { D, P, H1, H2, H3, B, NL, CAP, NOTE, table, img, PW } = S;
const { Paragraph, TextRun, Table, TableRow, TableCell, WidthType, ShadingType, BorderStyle } = D;
const [, , KDOC, FIG, OUT, LOGO] = process.argv;
const f5 = (n) => path.join(FIG, n);

function code(src) {
  const bd = { style: BorderStyle.SINGLE, size: 4, color: "808080" };
  return new Table({ width: { size: PW, type: WidthType.DXA }, columnWidths: [PW], rows: [new TableRow({ children: [new TableCell({
    width: { size: PW, type: WidthType.DXA }, borders: { top: bd, bottom: bd, left: bd, right: bd },
    shading: { fill: "F7F7F7", type: ShadingType.CLEAR, color: "auto" }, margins: { top: 80, bottom: 80, left: 140, right: 140 },
    children: src.replace(/^\n/, "").replace(/\s+$/, "").split("\n").map((l) => new Paragraph({ spacing: { after: 0, line: 240 },
      children: [new TextRun({ text: l.length ? l : " ", font: "Consolas", size: 18, color: "000000" })] })) })] })] });
}

// ---------- inventaris berkas dari KDoc + keterangan manual
const manual = {
  "authentication/data/repository/MockAuthRepository.kt": "AuthRepository in-memory (varian demo): akun demo/Demo@1234/PIN 123456, OTP demo, e-KYC simulasi (NIK berakhiran 0000 ditolak), blokir PIN, sesi. Data hilang saat proses dimatikan.",
  "account/data/AccountRepository.kt": "Kontrak modul Account (FR-06 – FR-09): ringkasan rekening, grafik saldo, sembunyikan saldo, kantong.",
  "topup/data/TopUpRepository.kt": "Kontrak modul Top-up (FR-18 – FR-20): katalog, inquiry PLN/tagihan, pesanan, bayar dengan PIN.",
  "transfer/data/TransferRepository.kt": "Kontrak modul Transfer (FR-10 – FR-14, UC-01): bank, inquiry, quote, execute, cek status, favorit.",
  "transfer/data/LocalTransferRepository.kt": "Transfer varian demo: aturan nominal, biaya antarbank, saldo, idempotensi, posting ke ledger in-memory lewat InterbankSimulator.",
  "history/data/LocalHistoryRepository.kt": "Riwayat varian demo: filter, paginasi, detail, dan ringkasan bulanan dihitung dari ledger in-memory (FR-23 – FR-25).",
  "payment/data/LocalPaymentRepository.kt": "QRIS varian demo (FR-15 – FR-17): validasi kode, batas nominal, pembayaran dengan PIN, QRIS pribadi statis/dinamis.",
  "profile/data/LocalProfileRepository.kt": "Profile varian demo (FR-27 – FR-29): ubah profil dengan OTP, pengaturan notifikasi lewat NotificationCenter.",
  "core/di/CoreModule.kt": "Komponen lintas modul: pemilihan varian, ledger dan guard demo, ApiClient, TokenStore, sumber notifikasi, sinyal revision dan sessionExpired.",
  "core/security/TransactionGuard.kt": "Gerbang setiap transaksi keuangan varian demo: e-KYC wajib disetujui (FR-02) dan PIN 6 digit dengan blokir sementara (FR-04, BR-02).",
  "core/network/ApiClient.kt": "Klien REST: Retrofit + OkHttp, header Bearer, refresh token otomatis saat 401, call { } → AppResult, pesan offline, IsoTime.",
  "core/network/TokenStore.kt": "Penyimpanan token sesi dan biometrik: SecureTokenStore (AES-GCM, kunci Android Keystore) dan InMemoryTokenStore untuk test.",
  "authentication/presentation/state/RecoveryUiState.kt": "Langkah dan state UI alur pemulihan akun (RecoveryStep, RecoveryUiState).",
  "account/domain/AccountModels.kt": "Model Account: aturan kunci kantong (LockRule, FR-09), Pocket, AccountOverview, periode grafik saldo.",
  "topup/domain/TopUpModels.kt": "Model Top-up: jenis layanan, operator, produk, pelanggan PLN/tagihan, ringkasan pesanan sebelum PIN.",
  "transfer/domain/TransferModels.kt": "Model Transfer: Bank, Recipient (hasil validasi rekening, FR-12), FavoriteRecipient, TransferQuote (nominal + biaya).",
  "payment/data/PaymentRepository.kt": "Kontrak modul Payment (QRIS) serta model ScannedQris, DemoQr, dan aturan batas nominal QrisRules.",
  "profile/data/ProfileRepository.kt": "Kontrak modul Profile serta ProfileDraft dan PendingProfileChange (perubahan yang menunggu OTP).",
  "history/data/HistoryRepository.kt": "Kontrak modul History & Dashboard serta model filter, rentang tanggal, item, dan halaman riwayat (PAGE_SIZE).",
  "core/ledger/LedgerModels.kt": "Model buku besar: jenis akun (utama, kantong, EXTERNAL), LedgerAccount, TxCategory (termasuk CARD), TxStatus, JournalLine, LedgerTransaction, PostingRequest, LedgerEvent.",
  "core/notification/NotificationCenter.kt": "Pusat notifikasi varian demo: jenis dan kanal notifikasi, pengaturan per pengguna, kotak masuk, dan outbox email simulasi.",
  "virtualcard/domain/model/VirtualCardModels.kt": "Model kartu virtual: CardStatus, CardScope (DOMESTIC/INTERNATIONAL), VirtualCard, VirtualCardLimits.",
  "virtualcard/domain/repository/VirtualCardRepository.kt": "Kontrak modul Virtual Card (buat, detail, bekukan, limit, otorisasi pembelian) dan AuthGateway untuk verifikasi PIN.",
  "virtualcard/domain/usecase/VirtualCardUseCases.kt": "Use case kartu virtual: daftar, buat dengan cakupan penggunaan, lihat detail dengan PIN, bekukan/aktifkan, atur limit.",
  "virtualcard/presentation/state/VirtualCardUiState.kt": "State UI kartu virtual dan dialog yang sedang terbuka (mengikuti activity diagram).",
  "ui/theme/Theme.kt": "DigitalBankTheme: skema warna Material 3 terang/gelap (tanpa dynamic color), bentuk (sudut 10/14/20 dp), DdbColors (emas, dana masuk, panel, garis, logo) lewat DdbTheme.colors, dan ChartColors per mode.",
  "ui/theme/Brand.kt": "DdbMark (tanda logo Diponegoro dari logo asli, diwarnai navy di mode terang dan emas di mode gelap) dan GoldRule (garis aksen emas).",
  "core/ui/Buttons.kt": "DdbButton dan DdbOutlinedButton: tombol bersudut 10 dp, tinggi minimum 48 dp, tanpa bayangan. Layar memakainya lewat import alias (import …DdbButton as Button).",
  "core/util/AppResult.kt": "Hasil operasi bersama: Success(data) atau Failure(message), dengan helper map dan onFailure.",
  "core/storage/KeyValueStore.kt": "Penyimpanan preferensi kecil (mis. sembunyikan saldo): SharedPrefsStore di aplikasi, InMemoryStore di test.",
  "history/presentation/HistoryViewModels.kt": "HistoryViewModel (filter, pencarian, paginasi), TransactionDetailViewModel, dan ViewModel analitik bulanan.",
  "payment/presentation/PaymentViewModels.kt": "QrisPayViewModel (pindai → konfirmasi → PIN → hasil) dan QrisReceiveViewModel (QRIS pribadi).",
  "profile/presentation/ProfileViewModels.kt": "ProfileViewModel (ubah profil + OTP), SecurityViewModel (password, PIN, biometrik), ViewModel pengaturan dan kotak notifikasi.",
  "topup/presentation/TopUpViewModel.kt": "ViewModel top-up: pilih layanan, inquiry, konfirmasi, PIN, hasil (FR-18 – FR-20).",
  "transfer/presentation/TransferViewModel.kt": "ViewModel transfer bertahap (UC-01): pilih penerima, inquiry, quote, PIN dengan Idempotency-Key, hasil.",
  "DigitalBankApplication.kt": "Kelas Application; memanggil CoreModule.init(context) saat proses aplikasi dimulai.",
  "MainActivity.kt": "Activity tunggal: memasang DigitalBankTheme dan AuthNavGraph, mencatat interaksi untuk timeout sesi, dan meminta izin ACCESS_LOCAL_NETWORK pada varian online di Android 17.",
  "account/presentation/AccountScreens.kt": "Layar BalanceChartScreen (grafik saldo harian/mingguan/bulanan) dan PocketsScreen (daftar, buat, kunci, isi, tarik kantong).",
  "account/presentation/AccountViewModels.kt": "BalanceChartViewModel dan PocketsViewModel: memuat data dari AccountRepository dan memperbarui saat CoreModule.revision berubah.",
  "authentication/domain/model/AuthEnums.kt": "Enum AccountStatus, OtpPurpose, OtpStatus, EkycStatus (sama dengan enum backend).",
  "authentication/domain/model/AuthResult.kt": "Hasil operasi autentikasi: Success(data) atau Error(message).",
  "authentication/domain/model/Credential.kt": "Kredensial login (username/email/telepon dan password).",
  "authentication/domain/model/EkycVerification.kt": "Data verifikasi e-KYC: NIK, referensi swafoto, status.",
  "authentication/domain/model/OtpVerification.kt": "OTP aktif: tujuan, kode demo, masa berlaku, jumlah percobaan.",
  "authentication/domain/model/Session.kt": "Sesi login per perangkat (nama perangkat, waktu login, aktivitas terakhir, status).",
  "authentication/domain/model/TransactionPin.kt": "Status PIN transaksi: hash, jumlah salah, waktu blokir.",
  "authentication/domain/model/User.kt": "Data pengguna: identitas, kontak, status akun, status e-KYC.",
  "authentication/domain/repository/AuthRepository.kt": "Kontrak modul Authentication (registrasi, OTP, e-KYC, login, biometrik, PIN, sesi, pemulihan) yang diimplementasikan Mock- dan RemoteAuthRepository.",
  "authentication/domain/usecase/AuthValidators.kt": "Validasi format input (username, email, telepon, password kuat, PIN 6 digit, NIK 16 digit).",
  "authentication/domain/usecase/LoginUseCases.kt": "Use case login dengan password atau biometrik dan pengambilan data pengguna.",
  "authentication/domain/usecase/PinUseCases.kt": "Use case membuat, mengambil, dan memvalidasi PIN transaksi.",
  "authentication/domain/usecase/RecoveryUseCases.kt": "Use case pemulihan akun: minta OTP, reset password, reset PIN.",
  "authentication/domain/usecase/RegistrationUseCases.kt": "Use case registrasi, kirim dan verifikasi OTP, kirim e-KYC.",
  "authentication/domain/usecase/SessionUseCases.kt": "Use case daftar sesi aktif, logout, logout dari semua perangkat.",
  "authentication/presentation/components/AuthComponents.kt": "Komponen bersama layar autentikasi: AuthScreenContainer (logo, garis emas, judul), MessageBanner, LoadingButton, AuthTextField, PasswordField, PinField, DemoOtpCard, serta PinDots dan PinKeypad untuk layar PIN.",
  "authentication/presentation/navigation/AuthNavGraph.kt": "Navigasi utama aplikasi: alur autentikasi (login, registrasi, OTP, e-KYC, PIN, pemulihan), lalu seluruh rute fitur (AppRoutes) dengan MainBottomBar.",
  "authentication/presentation/navigation/AuthRoutes.kt": "Konstanta rute layar autentikasi.",
  "authentication/presentation/screen/EkyScreen.kt": "EkycScreen: input NIK dan simulasi swafoto untuk verifikasi identitas.",
  "authentication/presentation/screen/HomeScreen.kt": "Diimpor sebagai SessionsScreen: daftar sesi aktif per perangkat, logout satu atau semua perangkat.",
  "authentication/presentation/screen/LoginScreen.kt": "Layar login (tombol Masuk dan Masuk dengan biometrik simulasi) serta tautan registrasi dan pemulihan; teks akun demo hanya pada varian demo.",
  "authentication/presentation/screen/OtpScreen.kt": "Layar verifikasi OTP; menampilkan kode demo (DemoOtpCard).",
  "authentication/presentation/screen/PinScreens.kt": "CreatePinScreen (membuat PIN) dan EnterPinScreen (membuka aplikasi setelah login: enam titik dan keypad angka, PIN dikirim otomatis pada digit keenam).",
  "authentication/presentation/screen/RecoveryScreen.kt": "Layar pemulihan akun bertahap: identitas, OTP, password/PIN baru.",
  "authentication/presentation/screen/RegisterScreen.kt": "Formulir registrasi akun baru.",
  "authentication/presentation/state/AuthEvent.kt": "Event satu kali dari AuthViewModel ke UI (mis. navigasi).",
  "authentication/presentation/state/AuthUiState.kt": "State UI autentikasi: pengguna, loading, pesan, OTP aktif, status PIN dan sesi.",
  "authentication/presentation/viewmodel/AuthViewModel.kt": "ViewModel alur autentikasi; mengamati CoreModule.sessionExpired untuk kembali ke login saat refresh token ditolak.",
  "authentication/presentation/viewmodel/RecoveryViewModel.kt": "ViewModel alur pemulihan akun.",
  "core/navigation/AppRoutes.kt": "Rute fitur (home, history, history/detail/{txId}, history/analytics, account/chart, account/pockets, transfer, qris/pay, qris/receive, topup, profile, …) dan MainBottomBar: lima tab (Beranda, Riwayat, QRIS di tengah, Kantong, Profil).",
  "core/network/ApiModels.kt": "DTO @Serializable yang sesuai dengan request/response backend (AuthTokens, UserDto, AccountDto, TransactionDto, JournalLineDto, dsb.).",
  "core/network/Mappers.kt": "Konversi DTO ke model aplikasi: TransactionDto.toLedger(viewer), JournalLineDto.toLedger, UserDto.toUser, dan pembuat idemKey().",
  "core/ui/Charts.kt": "Grafik Canvas tanpa library: LineChart, GroupedBarChart, Legend, compactRupiah.",
  "core/ui/Components.kt": "Komponen UI bersama: ScreenScaffold (app bar rata), SectionCard (panel bergaris tipis tanpa bayangan), InfoRow, ErrorText, InfoText, StatusChip, SignedAmount (dana masuk biru), PinDialog, MoneyField, DividerLine, EmptyState, HighlightCard, ddbFieldColors.",
  "core/ui/ReceiptView.kt": "ReceiptCard: tampilan bukti transaksi di layar.",
  "core/util/HashUtil.kt": "Hash SHA-256 untuk penyimpanan PIN/password pada repository demo.",
  "core/util/Money.kt": "formatRupiah, parseRupiah, formatDateTime, formatDate, formatAccountNumber (zona WIB).",
  "history/presentation/HistoryScreens.kt": "HistoryScreen (filter, cari, paginasi), TransactionDetailScreen (detail + bukti), AnalyticsScreen (ringkasan bulanan).",
  "home/HomeScreen.kt": "Dashboard utama: header (logo, notifikasi, profil), sapaan, saldo sebagai fokus (bisa disembunyikan), aksi cepat berikon garis, ringkasan bulan ini, transaksi terakhir; meminta izin notifikasi Android 13+.",
  "home/HomeViewModel.kt": "ViewModel Beranda: menggabungkan rekening, ringkasan riwayat, dan notifikasi.",
  "payment/presentation/PaymentScreens.kt": "QrisPayScreen (pindai kamera/galeri, konfirmasi, PIN) dan QrisReceiveScreen (QRIS pribadi statis/dinamis).",
  "profile/presentation/ProfileScreens.kt": "ProfileScreen, SecurityScreen (password, PIN, biometrik), NotificationSettingsScreen, NotificationsScreen.",
  "topup/presentation/TopUpScreen.kt": "Layar top-up pulsa, paket data, token PLN, dan tagihan dengan konfirmasi PIN.",
  "transfer/presentation/TransferScreen.kt": "Layar transfer bertahap: pilih bank/favorit, inquiry, nominal, konfirmasi biaya, PIN, bukti.",
  "ui/theme/Color.kt": "Palet merek: Midnight Navy #0B1D33, Diponegoro Blue #0B3D91, Muted Gold #B69A62, warna latar/garis mode terang dan gelap, warna grafik dan status.",
  "ui/theme/Type.kt": "Tipografi Material 3 dengan Plus Jakarta Sans (bobot 400/500/600; font di res/font, lisensi OFL di app/licenses); angka tabular untuk nominal.",
  "virtualcard/presentation/screen/VirtualCardScreen.kt": "Layar kartu virtual: daftar, buat, lihat detail dengan PIN, bekukan, atur limit, simulasi pembelian.",
  "virtualcard/presentation/viewmodel/VirtualCardViewModel.kt": "ViewModel kartu virtual; dialog mengikuti activity diagram Virtual Card.",
};
const rows = fs.readFileSync(KDOC, "utf8").split(/\r?\n/).filter(Boolean).map((l) => {
  const [file, lines, decls, doc] = l.split(" | ");
  return { file: file.replace(/\\/g, "/").replace(/^\//, ""), lines: +lines, decls: (decls || "").trim(), doc: (doc || "").trim() };
});
const describe = (r) => {
  if (manual[r.file]) return manual[r.file];
  let d = r.doc.replace(/\s+/g, " ").trim();
  d = d.replace(/\s-\s.*$/, "").replace(/Implementasi:.*$/, "").trim();
  const cut = d.search(/(?<=\.)\s/);
  if (cut > 40) d = d.slice(0, cut);
  if (!d) return "";
  return d.length > 230 ? d.slice(0, 227) + "…" : d;
};
const missing = rows.filter((r) => !describe(r));
if (missing.length) throw new Error("no description: " + missing.map((r) => r.file).join(", "));
const groups = {};
rows.forEach((r) => { const g = r.file.includes("/") ? r.file.split("/")[0] : "(root)"; (groups[g] ||= []).push(r); });
const order = ["(root)", "core", "authentication", "account", "transfer", "payment", "topup", "virtualcard", "history", "profile", "home", "ui"];
if (order.filter((g) => groups[g]).length !== Object.keys(groups).length) throw new Error("unknown group " + Object.keys(groups));
const pic = { authentication: "Salomo", virtualcard: "Salomo", account: "Grace", topup: "Grace", transfer: "Cecilia", history: "Cecilia", payment: "Gracia", profile: "Gracia" };
const total = rows.length, totalLines = rows.reduce((a, r) => a + r.lines, 0);
if (total !== 112) throw new Error("expected 112 files, got " + total);
const invRows = [];
order.forEach((g) => {
  const pkg = g === "(root)" ? "com.diponegoro.digitalbank" : `com.diponegoro.digitalbank.${g}`;
  invRows.push([{ t: pkg + (pic[g] ? `  (PIC: ${pic[g]})` : ""), bold: true, fill: S.KEY_FILL, span: 3 }]);
  groups[g].forEach((r) => invRows.push([g === "(root)" ? r.file : r.file.slice(g.length + 1), String(r.lines), describe(r)]));
});

const meta = {
  docLabel: "Dokumen 7 – Volume Android", title: "Developer Manual", subtitle: "Panduan Pengembang dan Keterangan Source Code – Aplikasi Android Diponegoro Digital Bank",
  code: "DVM-DDB-001-AND", version: "0.9.3", statusShort: "Draf Lengkap untuk Review",
  status: "Draf lengkap – menunggu penulisan/penelaahan bagian QRIS dan Profile (Gracia Riyanto), uji keterikutan setup oleh anggota tim, dan persetujuan Project Manager",
  module: "Developer Manual – Aplikasi Android (apps/android, 8 business module)", createdBy: "Salomo Putra Glorious Hokoyoku (Project Manager, Backend & Integrasi Android)",
  updatedBy: "Salomo Putra Glorious Hokoyoku (Project Manager)", team: "Kelompok Jaran Goyang – Mobile: Salomo (Auth, Virtual Card), Cecilia Adelaide Penlaana (Transfer, History), Gracia Riyanto (QRIS, Profile), Grace (Account, Top-up)",
  refs: "SAD-DDB-001 v0.9.1; FSD-DDB-001 v0.9.1; DVM-DDB-001-BE v0.9.1; apps/android/README.md; kode aplikasi per 11 November 2026",
  date: "21 November 2026", logo: LOGO,
  revisions: [
    ["0.1", "11 Nov 2026", "Salomo P.G. Hokoyoku", "Outline Developer Manual Android."],
    ["0.9", "11 Nov 2026", "Salomo P.G. Hokoyoku", `Draf lengkap: setup, varian demo/online, arsitektur, keterangan ${total} berkas source code, pola pengembangan, pengujian, troubleshooting.`],
    ["0.9.1", "12 Nov 2026", "Salomo P.G. Hokoyoku", "Proyek dipindah ke apps/android; perbaikan hasil uji sistem STD-DDB-001: waktu sesi dengan tanggal (DEF-05), back stack OTP (DEF-07), login sebelum e-KYC diarahkan ke e-KYC (DEF-08), status blokir PIN setelah pemulihan (DEF-10), pesan Riwayat saat server tidak terjangkau (DEF-11)."],
    ["0.9.2", "12 Nov 2026", "Salomo P.G. Hokoyoku", "Bagian 3.5 APK rilis bertanda tangan (keystore, build-rilis.ps1, verifikasi apksigner); troubleshooting pemasangan APK rilis; keterbatasan diperbarui (layar Sesi saat server tidak terjangkau, DEF-11)."],
    ["0.9.3", "21 Nov 2026", "Salomo P.G. Hokoyoku", "Tampilan Android baru: tema (ui/theme: Color, Theme, Type, Brand), font Plus Jakarta Sans, ikon dari logo asli, layar Login, PIN dengan keypad, Beranda, bottom navigation lima tab, komponen bersama (Components.kt, Buttons.kt); inventaris berkas diperbarui (112 berkas); jumlah test Android 64 (demo) dan 58 (online)."],
  ],
};

const body = [
  H1("BAB I  PENDAHULUAN"),
  H2("1.1 Tujuan"),
  P("Manual ini membantu pengembang menyiapkan lingkungan, membangun, menjalankan, memahami, menguji, dan mengubah aplikasi Android Diponegoro Digital Bank. Bab VI menjelaskan setiap berkas source code. Bab VII menjelaskan pola yang wajib diikuti saat menambah fitur agar aplikasi tetap dapat berjalan dalam dua varian (demo dan online)."),
  H2("1.2 Pembaca"),
  B("Anggota tim Mobile yang melanjutkan atau meninjau modul Android."),
  B("Penguji (Dokumen 6) yang memasang APK di emulator atau ponsel."),
  B("Tim Web yang ingin melihat cara klien memakai API backend."),
  P("Pembaca diasumsikan memahami dasar Kotlin dan Jetpack Compose. Langkah mencoba aplikasi di ponsel sungguhan dijelaskan lebih rinci di dokumen pendamping Panduan Uji Aplikasi di Ponsel (TUT-DDB-001-AND)."),
  H2("1.3 Ringkasan Teknologi"),
  table(["Komponen", "Versi / Keterangan"], [
    ["Bahasa", "Kotlin 2.2.10"],
    ["UI", "Jetpack Compose + Material 3 (tema DDB, font Plus Jakarta Sans), Navigation Compose"],
    ["Arsitektur", "MVVM (ViewModel + StateFlow), dependency injection manual (tanpa Hilt)"],
    ["Build", "Gradle Wrapper 9.6.0, Android Gradle Plugin 9.4.1, Kotlin DSL"],
    ["SDK", "minSdk 24 (Android 7.0), targetSdk dan compileSdk 37 (Android 17)"],
    ["Jaringan (varian online)", "Retrofit 3.0.0, OkHttp 4.12.0, kotlinx-serialization 1.9.0"],
    ["QR", "ZXing (journeyapps barcodescanner) untuk pindai dan membuat QR"],
    ["Pengujian", "JUnit 4, Robolectric + Compose UI Test, MockWebServer"],
    ["Package", "com.diponegoro.digitalbank (demo: com.diponegoro.digitalbank.demo)"],
  ], [2600, 6470], { boldFirst: true }),
  CAP("Tabel 1.1 Teknologi aplikasi Android"),

  H1("BAB II  PERSIAPAN LINGKUNGAN"),
  H2("2.1 Perangkat yang Dibutuhkan"),
  table(["Perangkat", "Wajib?", "Keterangan"], [
    ["Android Studio (versi terbaru)", "Wajib", "Membawa JDK (folder jbr), SDK Manager, emulator, dan Logcat"],
    ["Android SDK Platform 37", "Wajib", "Pasang dari SDK Manager (Android 17); juga Build-Tools dan Platform-Tools (adb)"],
    ["Git Bash atau PowerShell", "Wajib", "Menjalankan gradlew dari terminal"],
    ["Emulator (AVD) API 37 atau ponsel Android 7.0+", "Wajib salah satu", "Emulator untuk pengembangan harian; ponsel untuk uji kamera dan jaringan"],
    ["Backend ddb-backend", "Hanya varian online", "Lihat DVM-DDB-001-BE Bab III; cara tercepat: .\\run-local.ps1 -H2"],
  ], [2900, 1600, 4570], { boldFirst: true, size: 18 }),
  CAP("Tabel 2.1 Perangkat pengembang"),
  H2("2.2 Membuka Proyek"),
  ...NL([
    "Buka Android Studio → Open → pilih folder apps/android (folder yang berisi settings.gradle.kts).",
    "Tunggu Gradle Sync selesai. Unduhan pertama membutuhkan internet (Gradle 9.6.0 dan dependensi).",
    "Pilih varian di panel Build Variants: demoDebug (tanpa server) atau onlineDebug (perlu backend).",
    "Pilih emulator atau ponsel, lalu tekan Run.",
  ]),
  P("Berkas local.properties (lokasi SDK) dibuat otomatis oleh Android Studio dan tidak di-commit."),
  H2("2.3 Struktur Proyek"),
  code(`
apps/android/
├── settings.gradle.kts, build.gradle.kts, gradle.properties
├── gradle/libs.versions.toml      katalog versi dependensi
├── gradlew, gradlew.bat           Gradle Wrapper
└── app/
    ├── build.gradle.kts           flavor demo/online, BuildConfig.ONLINE dan BASE_URL
    └── src/
        ├── main/                  kode bersama kedua varian (112 berkas Kotlin)
        │   ├── AndroidManifest.xml
        │   ├── java/com/diponegoro/digitalbank/
        │   └── res/xml/network_security_config.xml   (HTTP polos ditolak)
        ├── online/                khusus varian online
        │   ├── AndroidManifest.xml                    (izin ACCESS_LOCAL_NETWORK)
        │   └── res/xml/network_security_config.xml   (HTTP polos diizinkan untuk LAN)
        ├── test/                  unit test kedua varian (11 berkas)
        ├── testDemo/              test UI Robolectric khusus demo (AppTourTest, DarkModeShotsTest)
        └── androidTest/           instrumented test (contoh bawaan)
`),
  CAP("Gambar 2.1 Struktur proyek Android"),

  H1("BAB III  VARIAN BUILD DAN CARA MENJALANKAN"),
  H2("3.1 Dua Product Flavor"),
  table(["Varian", "BuildConfig.ONLINE", "Sumber data", "Application ID", "Kegunaan"], [
    ["demo", "false", "Ledger in-memory + simulator (DemoData)", "…digitalbank.demo", "Uji UI, presentasi tanpa server, cadangan demo"],
    ["online", "true", "Backend REST /api/v1 (BASE_URL)", "…digitalbank", "Integrasi nyata dengan backend dan Web"],
  ], [1100, 1500, 2700, 1770, 2000], { boldFirst: true, size: 18 }),
  CAP("Tabel 3.1 Product flavor"),
  P("Kedua varian memiliki application ID berbeda sehingga dapat terpasang bersamaan di satu perangkat. Nama aplikasi di peluncur sama-sama Digital Bank; bedakan dari ikon info aplikasi atau versionName (1.0-demo untuk demo)."),
  H2("3.2 Perintah Build"),
  code(`
# Git Bash (Windows) – arahkan JAVA_HOME ke JDK bawaan Android Studio
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
cd apps/android

./gradlew :app:assembleDemoDebug          # app/build/outputs/apk/demo/debug/app-demo-debug.apk
./gradlew :app:assembleOnlineDebug        # online, BASE_URL = http://10.0.2.2:8080/ (emulator)
./gradlew :app:assembleOnlineDebug -Pddb.baseUrl=http://192.168.1.10:8080/   # ponsel di Wi-Fi sama
./gradlew :app:installOnlineDebug         # build + pasang ke perangkat yang terhubung adb

# PowerShell: .\\gradlew.bat :app:assembleDemoDebug
`),
  CAP("Gambar 3.1 Build dan pasang APK"),
  NOTE("Alamat 10.0.2.2 adalah alias emulator untuk localhost laptop. Ponsel sungguhan wajib memakai IP laptop di jaringan yang sama atau URL ngrok (lihat TUT-DDB-001-AND)."),
  H2("3.3 Menjalankan Varian Online di Emulator"),
  ...NL([
    "Jalankan backend: cd backend lalu .\\run-local.ps1 -H2. Tunggu sampai http://localhost:8080/actuator/health menampilkan UP.",
    "Build dan pasang varian online: ./gradlew :app:installOnlineDebug.",
    "Buka aplikasi. Pada Android 17 akan muncul dialog izin Nearby devices (ACCESS_LOCAL_NETWORK); pilih Allow.",
    "Login dengan akun demo (Tabel 3.2), masukkan PIN, lalu Beranda menampilkan saldo dari server.",
  ]),
  H2("3.4 Akun dan Data Uji"),
  table(["Data", "Nilai", "Keterangan"], [
    ["Akun demo", "demo / Demo@1234, PIN 123456", "Rekening 1400123456; riwayat 5 bulan"],
    ["Penerima sesama bank", "1400000002 (ANDI), 1400000003 (BUDI), 1400000004 (CITRA)", "Varian online: akun andi/budi/citra dengan password dan PIN yang sama"],
    ["Transfer antarbank ditolak", "BRI 9999999999", "Uji jalur gagal + refund"],
    ["Transfer antarbank timeout", "Mandiri 8888888888", "Uji status tertunda dan cek status"],
    ["e-KYC ditolak", "NIK berakhiran 0000", "Registrasi akun baru"],
    ["PLN/tagihan tidak ditemukan", "Nomor pelanggan berakhiran 0000", "Modul Top-up"],
    ["OTP", "Ditampilkan di kartu Kode OTP demo", "Belum ada SMS/email sungguhan"],
  ], [2600, 3400, 3070], { boldFirst: true, size: 18 }),
  CAP("Tabel 3.2 Akun dan data uji (sama pada varian demo dan online)"),

  H2("3.5 APK Rilis Bertanda Tangan"),
  P("APK rilis ditandatangani dengan kunci di apps/android/keystore/ddb-release.jks. Lokasi berkas dan password dibaca build.gradle.kts dari apps/android/keystore.properties. Kedua berkas tidak boleh dibagikan atau dimasukkan ke repositori (.gitignore). Simpan cadangannya di tempat aman, karena pembaruan aplikasi wajib ditandatangani dengan kunci yang sama. Bila keystore.properties tidak ada, build rilis tetap berjalan tetapi APK tidak ditandatangani."),
  code(`
# PowerShell, dari folder apps/android
.\\build-rilis.ps1                                    # online untuk emulator (http://10.0.2.2:8080/)
.\\build-rilis.ps1 -BaseUrl http://192.168.1.10:8080/  # online untuk ponsel di Wi-Fi yang sama
# Hasil: rilis/DDB-1.0-demo.apk dan rilis/DDB-1.0-online-<host>.apk (tanda tangan diverifikasi apksigner, SHA-256 dicetak)

# Membuat keystore baru (hanya bila belum ada; JDK Android Studio)
keytool -genkeypair -keystore keystore/ddb-release.jks -storetype PKCS12 -alias ddb -keyalg RSA -keysize 2048 -validity 10000
`),
  CAP("Gambar 3.2 Membangun APK rilis"),
  table(["Butir", "Nilai"], [
    ["Sertifikat", "CN=Diponegoro Digital Bank, OU=Kelompok Jaran Goyang, O=Universitas Kristen Satya Wacana, L=Salatiga, C=ID"],
    ["Skema tanda tangan", "APK Signature Scheme v2 (minSdk 24)"],
    ["Optimasi R8/minify", "Nonaktif (Retrofit dan kotlinx-serialization tidak memerlukan aturan ProGuard tambahan)"],
    ["Versi", "versionName 1.0, versionCode 1 (app/build.gradle.kts)"],
  ], [2200, 6870], { boldFirst: true, size: 17 }),
  CAP("Tabel 3.3 APK rilis"),
  NOTE("APK rilis dan debug varian online memakai application ID yang sama tetapi tanda tangan berbeda. Copot versi debug lebih dulu (adb uninstall com.diponegoro.digitalbank) sebelum memasang rilis."),

  H1("BAB IV  ARSITEKTUR APLIKASI"),
  H2("4.1 Lapisan dan Alur Ketergantungan"),
  P("Setiap fitur mengikuti pola MVVM. Layar Compose hanya membaca state dari ViewModel dan meneruskan aksi pengguna. ViewModel memanggil repository melalui interface. Implementasi repository dipilih saat aplikasi berjalan oleh FeatureModule (atau AuthModule/VirtualCardModule) berdasarkan BuildConfig.ONLINE, sehingga layar dan ViewModel tidak berubah di antara kedua varian."),
  img(f5("and_wiring.png"), 600, { alt: "Diagram alur ketergantungan Android" }),
  CAP("Gambar 4.1 Alur ketergantungan dari layar sampai sumber data"),
  table(["Modul", "Interface", "Varian demo", "Varian online", "PIC"], [
    ["Authentication", "AuthRepository", "MockAuthRepository", "RemoteAuthRepository", "Salomo"],
    ["Virtual Card", "VirtualCardRepository", "MockVirtualCardRepository", "RemoteVirtualCardRepository", "Salomo"],
    ["Account", "AccountRepository", "LocalAccountRepository", "RemoteAccountRepository", "Grace"],
    ["Top-up", "TopUpRepository", "LocalTopUpRepository", "RemoteTopUpRepository", "Grace"],
    ["Transfer", "TransferRepository", "LocalTransferRepository", "RemoteTransferRepository", "Cecilia"],
    ["History", "HistoryRepository", "LocalHistoryRepository", "RemoteHistoryRepository", "Cecilia"],
    ["Payment (QRIS)", "PaymentRepository", "LocalPaymentRepository", "RemotePaymentRepository", "Gracia"],
    ["Profile", "ProfileRepository", "LocalProfileRepository", "RemoteProfileRepository", "Gracia"],
    ["Notifikasi", "NotificationSource", "LocalNotificationSource", "RemoteNotificationSource", "Salomo"],
  ], [1500, 2000, 2250, 2420, 900], { boldFirst: true, size: 17 }),
  CAP("Tabel 4.1 Repository per modul"),
  H2("4.2 CoreModule dan Sinyal Perubahan Data"),
  P("core/di/CoreModule.kt menyimpan komponen lintas modul:"),
  table(["Anggota", "Keterangan"], [
    ["online / onlineOverride", "Varian aktif; onlineOverride dipakai test untuk memaksa mode tertentu"],
    ["baseUrl", "Alamat backend dari BuildConfig.BASE_URL"],
    ["tokens", "TokenStore: SecureTokenStore (Android Keystore) di aplikasi, InMemoryTokenStore di test"],
    ["api", "ApiClient (Retrofit) yang dibuat sekali secara lazy"],
    ["sessionExpired", "Flow yang memancar saat refresh token ditolak; AuthViewModel kembali ke layar login"],
    ["revision / dataChanged()", "StateFlow penghitung perubahan data. Varian online: Remote*Repository menerima CoreModule::dataChanged sebagai parameter onDataChanged dan memanggilnya setelah transaksi berhasil. Varian demo: revision diteruskan dari Ledger.revision. ViewModel yang mengamati revision memuat ulang saldo, riwayat, dan kantong"],
    ["notificationSource", "LocalNotificationSource atau RemoteNotificationSource (polling push setiap 15 detik)"],
  ], [2600, 6470], { boldFirst: true, size: 18 }),
  CAP("Tabel 4.2 Anggota CoreModule"),
  P("Karena ViewModel hanya mengamati CoreModule.revision, perilaku pembaruan layar sama pada kedua varian: di demo sinyal berasal dari ledger in-memory setiap kali transaksi diposting, di online dari repository setelah server menerima transaksi."),
  H2("4.3 Lapisan Jaringan"),
  P("Paket core/network berisi DdbApi (kontrak Retrofit untuk endpoint backend), ApiModels (DTO), Mappers (DTO ke model aplikasi), TokenStore, dan ApiClient. ApiClient menyediakan:"),
  B("**BearerInterceptor** – menambahkan header Authorization: Bearer <accessToken> pada setiap permintaan."),
  B("**RefreshAuthenticator** – saat respons 401, menukar refresh token ke POST /auth/refresh, menyimpan token baru (refresh token dirotasi), lalu mengulang permintaan. Bila ditolak, token dihapus dan sessionExpired dipancarkan."),
  B("**call { … }** – membungkus pemanggilan suspend menjadi AppResult.Success atau AppResult.Failure. Pesan kesalahan server (field message) diteruskan apa adanya; kegagalan koneksi menjadi pesan \"Tidak dapat terhubung ke server\". Detail dicatat ke Logcat dengan tag DdbApi."),
  B("**IsoTime** – konversi waktu ISO-8601 UTC dari server ke epoch milidetik."),
  img(f5("and_sequence.png"), 600, { alt: "Sequence diagram panggilan REST dengan refresh token" }),
  CAP("Gambar 4.2 Panggilan transfer dengan pembaruan token otomatis"),
  H2("4.4 Keamanan di Sisi Aplikasi"),
  table(["Aspek", "Implementasi"], [
    ["Penyimpanan token", "SecureTokenStore: access/refresh token dan token biometrik dienkripsi AES-GCM dengan kunci di Android Keystore, disimpan di SharedPreferences"],
    ["Transaksi ganda", "Setiap layar konfirmasi membuat satu Idempotency-Key (idemKey()) dan memakainya ulang saat retry"],
    ["PIN", "Tidak disimpan di aplikasi (varian online); dikirim ke server per transaksi lewat PinDialog"],
    ["HTTP polos", "Ditolak pada varian demo dan pada build produksi; hanya diizinkan varian online untuk backend LAN pengembangan"],
    ["Timeout sesi", "InactivityMonitor mencatat interaksi; server juga menolak sesi tidak aktif > 5 menit"],
    ["Data pada layar", "Saldo dapat disembunyikan; nomor kartu lengkap dan CVV hanya tampil setelah PIN"],
  ], [2300, 6770], { boldFirst: true, size: 18 }),
  CAP("Tabel 4.3 Kontrol keamanan aplikasi"),

  H1("BAB V  NAVIGASI DAN LAYAR"),
  P("AuthNavGraph adalah graf navigasi tunggal. Setelah login dan PIN berhasil, pengguna masuk ke rute fitur pada AppRoutes dengan bilah navigasi bawah (MainBottomBar) berisi lima tab: Beranda, Riwayat, QRIS (tombol bulat di tengah), Kantong, dan Profil. Layar PIN setelah login memakai keypad angka; PIN dikirim otomatis saat digit keenam diketuk. Dialog PIN transaksi (PinDialog) tetap memakai kolom teks."),
  table(["Rute", "Layar", "Modul"], [
    ["home", "HomeScreen (dashboard)", "Home"],
    ["history, history/detail/{txId}, history/analytics", "HistoryScreen, TransactionDetailScreen, AnalyticsScreen", "History"],
    ["account/chart, account/pockets", "BalanceChartScreen, PocketsScreen", "Account"],
    ["transfer", "TransferScreen", "Transfer"],
    ["qris/pay, qris/receive", "QrisPayScreen, QrisReceiveScreen", "Payment"],
    ["topup", "TopUpScreen", "Top-up"],
    ["profile, profile/security, profile/notification-settings", "ProfileScreen, SecurityScreen, NotificationSettingsScreen", "Profile"],
    ["notifications", "NotificationsScreen", "Profile"],
    ["profile/sessions", "SessionsScreen (berkas authentication/…/HomeScreen.kt)", "Authentication"],
    ["virtualcard/home (AuthRoutes.VIRTUAL_CARD)", "VirtualCardScreen", "Virtual Card"],
  ], [3400, 4170, 1500], { size: 18 }),
  CAP("Tabel 5.1 Rute dan layar utama"),
  NOTE("Terdapat dua berkas bernama HomeScreen.kt. Berkas di paket home adalah dashboard; berkas di authentication/presentation/screen berisi layar sesi dan diimpor dengan nama SessionsScreen."),

  H1("BAB VI  KETERANGAN SOURCE CODE"),
  P(`Kode utama terdiri atas ${total} berkas Kotlin (${totalLines.toLocaleString("id-ID")} baris termasuk komentar) dalam paket com.diponegoro.digitalbank. Tabel 6.1 menjelaskan setiap berkas, dikelompokkan per paket beserta PIC modul. Keterangan diambil dari KDoc atau dilengkapi dari isi kode. Path ditulis relatif terhadap paket.`),
  table(["Berkas", "Baris", "Keterangan"], invRows, [3000, 700, 5370], { size: 16, center: [1] }),
  CAP("Tabel 6.1 Keterangan seluruh berkas source code Android"),
  H2("6.1 Berkas Kunci yang Perlu Dipahami Lebih Dulu"),
  ...NL([
    "**core/di/CoreModule.kt** dan **core/di/FeatureModule.kt** – pemilihan varian dan sinyal revision.",
    "**core/network/ApiClient.kt** – token, refresh otomatis, dan konversi kesalahan.",
    "**core/ledger/Ledger.kt** – aturan double-entry varian demo (cerminan LedgerService backend).",
    "**transfer/data/TransferRepository.kt** beserta Local dan Remote – contoh lengkap pola interface + dua implementasi.",
    "**transfer/presentation/TransferViewModel.kt** – contoh ViewModel bertahap dengan PIN dan Idempotency-Key.",
    "**authentication/presentation/navigation/AuthNavGraph.kt** – seluruh navigasi aplikasi.",
  ]),

  H1("BAB VII  POLA PENGEMBANGAN"),
  H2("7.1 Menambah Fitur atau Operasi pada Modul"),
  ...NL([
    "Tambahkan method suspend pada interface repository modul (mis. TransferRepository) dengan tipe kembalian AppResult<T>.",
    "Implementasikan di Local*Repository memakai Ledger/DemoData agar varian demo tetap berjalan.",
    "Tambahkan endpoint di core/network/DdbApi.kt dan DTO di ApiModels.kt (nama field sama dengan JSON backend), lalu implementasikan di Remote*Repository dengan client.call { … } dan mapper.",
    "Bila operasi mengubah saldo, panggil onDataChanged() setelah berhasil (Remote*Repository menerimanya dari FeatureModule sebagai CoreModule::dataChanged). Pada varian demo cukup memposting lewat Ledger.",
    "Panggil dari ViewModel di dalam viewModelScope.launch; ubah state loading, hasil, dan pesan kesalahan.",
    "Tulis test untuk Local (logika) dan, bila perlu, test MockWebServer untuk pemetaan Remote (Bab VIII).",
  ]),
  H2("7.2 Contoh Remote Repository untuk Transaksi"),
  code(`
class RemoteTransferRepository(
    private val client: ApiClient,
    private val onDataChanged: () -> Unit = {}          // FeatureModule: CoreModule::dataChanged
) : TransferRepository {
    override suspend fun execute(userId: String, quote: TransferQuote, pin: String,
                                 idempotencyKey: String): AppResult<LedgerTransaction> {
        val r = quote.recipient
        return client.call {
            transfer(idemKey(idempotencyKey), TransferRequest(r.bank.code, r.accountNumber, quote.amount, quote.note, pin))
        }.map { it.toLedger(userId) }.also { if (it is AppResult.Success) onDataChanged() }
    }
}
`),
  CAP("Gambar 7.1 Cuplikan RemoteTransferRepository.execute (transfer/data)"),
  P("**Aturan:** validasi saldo, batas nominal, biaya, PIN, dan status e-KYC pada varian online selalu diputuskan server. Validasi di aplikasi hanya untuk kenyamanan pengguna (mis. format nominal). Jangan membuat Idempotency-Key baru saat retry."),
  H2("7.3 Menambah Layar"),
  ...NL([
    "Tambahkan konstanta rute di core/navigation/AppRoutes.kt.",
    "Buat composable layar di paket presentation modul, memakai ScreenScaffold dan komponen di core/ui/Components.kt agar tampilan seragam. Untuk tombol, impor import com.diponegoro.digitalbank.core.ui.DdbButton as Button (atau DdbOutlinedButton as OutlinedButton); warna diambil dari MaterialTheme.colorScheme dan DdbTheme.colors, bukan kode warna langsung.",
    "Daftarkan composable(route) di AuthNavGraph.kt, ambil ViewModel dengan viewModel(factory = …).",
    "Tambahkan pemanggilan navigasi dari Beranda atau layar terkait.",
  ]),
  H2("7.4 Catatan Gaya Kode"),
  B("Uang bertipe Long rupiah; tampilkan dengan formatRupiah(). Waktu ditampilkan dalam WIB dengan formatDateTime()."),
  B("Teks antarmuka dan KDoc dalam bahasa Indonesia; rujuk nomor FR/BR SRS pada KDoc kelas."),
  B("Jangan memanggil jaringan dari composable; semua akses data lewat ViewModel."),
  B("ViewModel menerima repository lewat parameter konstruktor dengan nilai default dari FeatureModule agar mudah diganti di test."),

  H1("BAB VIII  PENGUJIAN"),
  H2("8.1 Menjalankan Test"),
  code(`
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :app:testDemoDebugUnitTest      # 64 test (1 test live dilewati), ±2 menit
./gradlew :app:testOnlineDebugUnitTest    # 58 test (tanpa test testDemo; LiveBackendTest dilewati bila -Pddb.liveUrl tidak diisi)
# Uji live ke backend yang sedang berjalan di laptop:
./gradlew :app:testOnlineDebugUnitTest -Pddb.liveUrl=http://localhost:8080/
# Laporan: app/build/reports/tests/testDemoDebugUnitTest/index.html
`),
  CAP("Gambar 8.1 Menjalankan test Android"),
  table(["Kelas test", "Jumlah", "Cakupan"], [
    ["AccountRepositoryTest", "6", "Kantong: buat, kunci tanggal/target, isi, tarik, aturan BR-06"],
    ["LedgerTest", "6", "Double-entry, atomik, saldo tidak negatif"],
    ["TransferRepositoryTest", "9", "Inquiry, biaya, saldo kurang, idempotensi, antarbank ditolak/timeout"],
    ["PaymentRepositoryTest", "6", "Codec QRIS (CRC), statis/dinamis, batas nominal"],
    ["TopUpRepositoryTest", "6", "Pulsa, paket data, PLN, tagihan, pelanggan tidak ditemukan"],
    ["HistoryAndProfileTest", "7", "Filter riwayat, ringkasan, OTP perubahan profil"],
    ["VirtualCardUseCasesTest", "8", "Buat, detail dengan PIN, bekukan, limit, otorisasi pembelian"],
    ["ApiClientTest", "8", "MockWebServer: header Bearer, refresh 401, pesan error, offline, pemetaan DTO"],
    ["LiveBackendTest", "1", "Login → saldo → transfer ke backend sungguhan (hanya bila ddb.liveUrl diisi)"],
    ["AppTourTest (testDemo)", "6", "Robolectric + Compose: AppTourTest (tur layar utama varian demo), DarkModeShotsTest (login, PIN, Beranda mode gelap), ReceiptAndDialogShotsTest (dialog PIN, bukti transaksi); tangkapan layar di app/build/screenshots"],
    ["ExampleUnitTest", "1", "Contoh bawaan proyek"],
  ], [2700, 900, 5470], { boldFirst: true, center: [1], size: 18 }),
  CAP("Tabel 8.1 Kelas test Android (11 berkas test utama + 1 berkas testDemo, ±1.327 baris)"),
  H2("8.2 Menulis Test Baru"),
  code(`
class TransferRepositoryTest {
    private val env = TestEnv()                                   // ledger + guard in-memory, akun demo
    private val repo = LocalTransferRepository(env.ledger, env.guard)

    @Test
    fun \`amount rules min max and insufficient balance\`() = runBlocking<Unit> {
        val r = repo.inquire(env.user, "BCA", "1234567890").ok()
        repo.quote(env.user, r, "4999", "").assertFailure("minimal")
        val q = repo.quote(env.user, r, "100000", "kos").ok()
        assertEquals(TransferRules.INTERBANK_FEE, q.fee)
    }
}
`),
  CAP("Gambar 8.2 Cuplikan TransferRepositoryTest; helper TestEnv, ok(), assertFailure() ada di TestFixtures.kt"),
  P("Test yang memakai Android framework (Compose, SharedPreferences) dijalankan dengan Robolectric (@RunWith(AndroidJUnit4::class)). Test pemetaan Remote memakai MockWebServer dengan InMemoryTokenStore agar tidak bergantung pada Android Keystore."),

  H1("BAB IX  TROUBLESHOOTING"),
  table(["Gejala", "Penyebab", "Solusi"], [
    ["'JAVA_HOME is not set' saat gradlew", "JDK tidak terdeteksi di terminal", "Setel JAVA_HOME ke folder jbr Android Studio"],
    ["\"Tidak dapat terhubung ke server\" (online)", "Backend mati, URL salah, firewall, atau izin LAN belum diberikan", "Periksa health check dari perangkat; izinkan Nearby devices di Android 17; periksa BASE_URL di build"],
    ["CLEARTEXT communication not permitted", "Build demo atau network security config main dipakai untuk URL http", "Gunakan varian online atau URL https (ngrok)"],
    ["Langsung kembali ke layar login", "Refresh token ditolak (sesi berakhir/logout dari perangkat lain)", "Login ulang; periksa jam perangkat"],
    ["Data tidak berubah setelah transaksi", "Repository baru tidak memanggil dataChanged() atau ViewModel tidak mengamati revision", "Lihat Bab 4.2"],
    ["Test Robolectric gagal resolusi layar", "Qualifier perangkat berbeda", "Gunakan @Config bawaan di AppTourTest"],
    ["Dialog PIN tertutup saat uji dengan adb", "keyevent ESC (111) menutup dialog", "Gunakan input text dan keyevent ENTER"],
    ["Path /sdcard rusak di Git Bash", "Konversi path MSYS", "Awali perintah adb dengan MSYS_NO_PATHCONV=1"],
    ["INSTALL_FAILED_UPDATE_INCOMPATIBLE saat memasang APK", "Versi terpasang ditandatangani kunci lain (debug vs rilis)", "Copot aplikasi lama: adb uninstall com.diponegoro.digitalbank"],
    ["build-rilis.ps1: keystore.properties tidak ditemukan", "Kunci rilis tidak ada di komputer ini", "Minta berkas keystore dan keystore.properties ke Project Manager"],
  ], [2700, 3000, 3370], { size: 17 }),
  CAP("Tabel 9.1 Troubleshooting"),
  H2("Keterbatasan yang Diketahui"),
  B("Biometrik masih disimulasikan; BiometricPrompt belum diimplementasikan."),
  B("Foto profil berupa URL; belum ada unggah gambar."),
  B("Notifikasi push memakai polling 15 detik (simulasi FCM)."),
  B("Uji sistem SA-01–SA-28 sudah dijalankan di emulator (STD-DDB-001); uji di ponsel fisik, termasuk pindai QRIS dengan kamera, belum dilakukan."),
  B("Instrumented test (androidTest) masih berupa contoh bawaan; cakupan test belum diukur."),
  H2("Referensi"),
  ...NL([
    "Kelompok Jaran Goyang. (2026). Architectural Design (SAD-DDB-001) v0.9.1.",
    "Kelompok Jaran Goyang. (2026). Developer Manual – Backend (DVM-DDB-001-BE) v0.9.1.",
    "Kelompok Jaran Goyang. (2026). Panduan Uji Aplikasi di Ponsel (TUT-DDB-001-AND) v1.0.",
    "apps/android/README.md, app/build.gradle.kts (11 November 2026).",
    "Android Developers. Guide to app architecture; Network security configuration; Local network permission (Android 17).",
  ]),
];

if (process.env.DUMP) rows.forEach((r) => console.log(r.file, "=>", describe(r)));
S.build(meta, body, OUT);
