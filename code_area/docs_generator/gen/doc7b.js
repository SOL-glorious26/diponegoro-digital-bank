const fs = require("fs");
const S = require("./ddbstyle");
const { D, P, H1, H2, H3, B, NL, CAP, NOTE, table, PW } = S;
const { Paragraph, TextRun, Table, TableRow, TableCell, WidthType, ShadingType, BorderStyle } = D;
const [, , JAVADOC, OUT, LOGO] = process.argv;

function code(src) {
  const bd = { style: BorderStyle.SINGLE, size: 4, color: "808080" };
  return new Table({ width: { size: PW, type: WidthType.DXA }, columnWidths: [PW], rows: [new TableRow({ children: [new TableCell({
    width: { size: PW, type: WidthType.DXA }, borders: { top: bd, bottom: bd, left: bd, right: bd },
    shading: { fill: "F7F7F7", type: ShadingType.CLEAR, color: "auto" }, margins: { top: 80, bottom: 80, left: 140, right: 140 },
    children: src.replace(/^\n/, "").replace(/\s+$/, "").split("\n").map((l) => new Paragraph({ spacing: { after: 0, line: 240 },
      children: [new TextRun({ text: l.length ? l : " ", font: "Consolas", size: 18, color: "000000" })] })) })] })] });
}

// ---------- file inventory from Javadoc
const manual = {
  "account/AccountController.java": "Endpoint /api/v1/accounts: ringkasan rekening, grafik saldo, preferensi sembunyikan saldo, CRUD kantong, kunci, isi, dan tarik.",
  "account/AccountDtos.java": "Record request/response modul Account; aturan lockType DATE wajib lockUntil, TARGET wajib target.",
  "auth/AuthController.java": "Endpoint /api/v1/auth (18 operasi): registrasi, OTP, e-KYC, login, biometrik, refresh, PIN (buat dan verifikasi), sesi, logout, pemulihan.",
  "auth/AuthEntities.java": "Enum AccountStatus, OtpPurpose, OtpStatus, EkycStatus, SessionStatus, AuthMethod, RecoveryType.",
  "auth/BiometricDevice.java": "Entitas biometric_devices: hash token perangkat biometrik per pengguna dan perangkat.",
  "auth/EkycVerification.java": "Entitas ekyc_verifications: NIK tersamar + hash, referensi swafoto, status keputusan.",
  "auth/OtpCode.java": "Entitas otp_codes: hash kode, tujuan, payload perubahan, jumlah percobaan, status, masa berlaku.",
  "auth/TransactionPin.java": "Entitas transaction_pins: hash PIN, penghitung salah, waktu blokir; isBlocked(now).",
  "auth/User.java": "Entitas users: identitas, hash password, status akun, preferensi; financialEnabled().",
  "auth/UserSession.java": "Entitas sessions: sesi per perangkat dengan hash refresh token dan waktu aktivitas terakhir.",
  "card/VirtualCardController.java": "Endpoint /api/v1/cards: daftar, buat, detail dengan PIN, bekukan/aktifkan, limit, simulasi pembelian.",
  "common/DdbProperties.java": "Binding konfigurasi ddb.* (security.jwt-secret, token minutes/days, data-key, expose-otp, seed-demo-data).",
  "common/GlobalExceptionHandler.java": "Mengubah ApiException, kesalahan validasi, JSON rusak, pelanggaran constraint, 403, 404, 405 menjadi ErrorResponse seragam.",
  "common/Ids.java": "ID berformat prefiks-UUID (mis. trx-…, fav-…) dan deret digit acak SecureRandom untuk OTP, nomor kartu, dan token.",
  "common/Money.java": "Format rupiah untuk pesan kesalahan (mis. Rp5.000).",
  "common/RequestContext.java": "Akses informasi permintaan saat ini (alamat IP untuk audit).",
  "common/TimeConfig.java": "Bean Clock UTC dan zona tampilan Asia/Jakarta (WIB).",
  "history/HistoryController.java": "Endpoint /api/v1/history: daftar berhalaman, detail, ringkasan bulanan, ekspor CSV/PDF.",
  "ledger/JournalLine.java": "Entitas journal_lines: satu baris debit/kredit sebuah transaksi (append-only).",
  "ledger/LedgerAccount.java": "Entitas ledger_accounts: rekening utama, kantong, atau akun eksternal beserta cache saldo.",
  "ledger/LedgerRepositories.java": "Repository Spring Data untuk akun, transaksi, dan jurnal ledger.",
  "ledger/LedgerTransaction.java": "Entitas ledger_transactions: referensi, kunci idempotensi, kategori, status, nominal, biaya, pihak.",
  "notification/Notification.java": "Entitas notifications: isi notifikasi, status dibaca, status sudah diambil sebagai push.",
  "payment/PaymentController.java": "Endpoint /api/v1/qris: pindai, bayar, QRIS pribadi/dinamis, contoh QR, simulasi dana masuk.",
  "profile/ProfileController.java": "Endpoint /api/v1/me: profil, perubahan profil + OTP, password, PIN, foto, pengaturan dan kotak notifikasi.",
  "security/SecurityConfig.java": "Rantai filter Spring Security: daftar endpoint publik, resource server JWT, respons 401/403 berformat seragam.",
  "topup/TopUpController.java": "Endpoint /api/v1/topup: katalog, paket data, inquiry PLN dan tagihan, quote, pesanan.",
  "transfer/TransferController.java": "Endpoint /api/v1/transfers: daftar bank, inquiry, quote, transfer, cek status, favorit.",
  "transfer/TransferDtos.java": "Record request/response modul Transfer (inquiry, quote, transfer, favorit, bank).",
  "payment/PaymentController.java#": "",
};
const rows = fs.readFileSync(JAVADOC, "utf8").split(/\r?\n/).filter(Boolean).map((l) => {
  const [file, lines, doc] = l.split(" | ");
  return { file: file.replace(/^\//, ""), lines: +lines, doc: (doc || "").trim() };
});
const describe = (r) => {
  if (manual[r.file]) return manual[r.file];
  let d = r.doc.replace(/\s+/g, " ").replace(/[A-Z]\w*#(\w+)/g, "$1").replace(/#(\w+)/g, "$1").trim();
  const cut = d.search(/(?<=\.)\s/); // first sentence
  if (cut > 40) d = d.slice(0, cut);
  return d.length > 230 ? d.slice(0, 227) + "…" : d;
};
const groups = {};
rows.forEach((r) => { const g = r.file.includes("/") ? r.file.split("/")[0] : "(root)"; (groups[g] ||= []).push(r); });
const missing = rows.filter((r) => !describe(r));
if (missing.length) throw new Error("no description: " + missing.map((r) => r.file).join(", "));
const order = ["(root)", "auth", "account", "transfer", "payment", "topup", "card", "history", "profile", "notification", "ledger", "security", "common", "demo"];
if (order.filter((g) => groups[g]).length !== Object.keys(groups).length) throw new Error("unknown group " + Object.keys(groups));
const total = rows.length, totalLines = rows.reduce((a, r) => a + r.lines, 0);
const invRows = [];
order.forEach((g) => {
  if (!groups[g]) return;
  invRows.push([{ t: g === "(root)" ? "com.diponegoro.ddb" : `com.diponegoro.ddb.${g}`, bold: true, fill: S.KEY_FILL, span: 3 }]);
  groups[g].forEach((r) => invRows.push([r.file.split("/").pop(), String(r.lines), describe(r)]));
});

const meta = {
  docLabel: "Dokumen 7 – Volume Backend", title: "Developer Manual", subtitle: "Panduan Pengembang dan Keterangan Source Code – Backend ddb-backend",
  code: "DVM-DDB-001-BE", version: "0.9.2", statusShort: "Draf Lengkap untuk Review",
  status: "Draf lengkap – menunggu uji keterikutan setup (Steven Wibisana) dan persetujuan Project Manager",
  module: "Developer Manual – Backend & Database (ddb-backend)", createdBy: "Salomo Putra Glorious Hokoyoku (Project Manager, Backend)",
  updatedBy: "Salomo Putra Glorious Hokoyoku (Project Manager, Backend)", team: "Kelompok Jaran Goyang – Backend & Database: Salomo P.G. Hokoyoku",
  refs: "SDD-DDB-001-BE v0.9; SAD-DDB-001 v0.9; backend/README.md; kode ddb-backend per 12 November 2026",
  date: "12 November 2026", logo: LOGO,
  revisions: [
    ["0.1", "11 Nov 2026", "Salomo P.G. Hokoyoku", "Outline Developer Manual Backend."],
    ["0.9", "11 Nov 2026", "Salomo P.G. Hokoyoku", `Draf lengkap: setup, cara menjalankan, struktur dan keterangan ${total} berkas source code, pola pengembangan, API, basis data, pengujian, deployment, troubleshooting.`],
    ["0.9.1", "11 Nov 2026", "Salomo P.G. Hokoyoku", "Pembaruan setelah integrasi Android–backend (Fase 5) 11 Nov sore: endpoint verifikasi PIN (73 operasi), 45 test, cara menghubungkan aplikasi Android varian online."],
    ["0.9.2", "12 Nov 2026", "Salomo P.G. Hokoyoku", "CORS dapat dikonfigurasi (ddb.cors.allowed-origins, DEF-04) dan header CORS backend disembunyikan di gateway; 47 test (CorsTest); laporan JaCoCo; troubleshooting CORS."],
  ],
};

const body = [
  H1("BAB I  PENDAHULUAN"),
  H2("1.1 Tujuan"),
  P("Manual ini membantu pengembang menyiapkan lingkungan, menjalankan, memahami, menguji, dan mengubah backend Diponegoro Digital Bank (ddb-backend). Bab V menjelaskan setiap berkas source code. Bab VI menjelaskan pola yang wajib diikuti saat menambah fitur agar aturan integritas transaksi tidak dilanggar."),
  H2("1.2 Pembaca"),
  B("Anggota tim yang melanjutkan atau meninjau backend."),
  B("Tim Android dan Web yang perlu menjalankan backend secara lokal untuk integrasi."),
  B("Penguji (Dokumen 6) yang menyiapkan lingkungan dan data uji."),
  P("Pembaca diasumsikan memahami dasar Java, HTTP/REST, dan SQL. Rancangan rinci (UML, ERD, pseudocode) ada di SDD-DDB-001-BE dan tidak diulang di sini."),
  H2("1.3 Ringkasan Teknologi"),
  table(["Komponen", "Versi"], [
    ["Java", "21 (bisa memakai JDK bawaan Android Studio: jbr)"],
    ["Spring Boot", "4.1.2 (Web MVC, Data JPA, Security + OAuth2 Resource Server, Validation, Actuator, Flyway)"],
    ["Basis data", "PostgreSQL 17/18; H2 untuk test dan percobaan cepat"],
    ["Build", "Maven Wrapper (mvnw / mvnw.cmd) – tidak perlu memasang Maven"],
    ["Dokumentasi API", "springdoc-openapi 3.2.0 (Swagger UI)"],
    ["Kontainer", "Docker + docker-compose (opsional)"],
  ], [2500, 6570], { boldFirst: true }),
  CAP("Tabel 1.1 Teknologi backend"),

  H1("BAB II  PERSIAPAN LINGKUNGAN"),
  H2("2.1 Perangkat yang Dibutuhkan"),
  table(["Perangkat", "Wajib?", "Keterangan"], [
    ["JDK 21 atau lebih baru", "Wajib", "Dapat memakai C:\\Program Files\\Android\\Android Studio\\jbr; run-local.ps1 memakainya otomatis bila JAVA_HOME kosong"],
    ["PowerShell atau Git Bash", "Wajib", "Menjalankan skrip dan perintah build"],
    ["PostgreSQL 17/18", "Opsional", "Untuk profil dev (data permanen); tanpa PostgreSQL gunakan profil h2"],
    ["Docker Desktop", "Opsional", "Untuk menjalankan gateway + backend + PostgreSQL sekaligus"],
    ["IDE (IntelliJ IDEA/VS Code)", "Disarankan", "Membuka folder backend sebagai proyek Maven"],
  ], [2700, 1200, 5170], { boldFirst: true, size: 18 }),
  CAP("Tabel 2.1 Perangkat pengembang"),
  H2("2.2 Struktur Repositori"),
  code(`
submit_progress_digital_banking_project/
├── backend/                     proyek Spring Boot (ddb-backend)
│   ├── pom.xml, mvnw, mvnw.cmd  build Maven
│   ├── run-local.ps1            jalankan lokal (profil dev / -H2)
│   ├── Dockerfile               image backend
│   ├── docs/                    API.md, openapi.json
│   └── src/
│       ├── main/java/com/diponegoro/ddb/   kode sumber (12 paket)
│       ├── main/resources/application.yml  konfigurasi + profil test/h2
│       ├── main/resources/db/migration/    skema Flyway (V1__init.sql)
│       └── test/java/com/diponegoro/ddb/   test integrasi API
├── gateway/nginx.conf           API Gateway
├── docker-compose.yml           gateway + backend + PostgreSQL
└── .env.example                 contoh variabel rahasia
`),
  CAP("Gambar 2.1 Struktur repositori backend"),
  H2("2.3 Menyiapkan PostgreSQL (sekali saja)"),
  P("Langkah ini hanya diperlukan untuk profil dev. Jalankan sebagai user postgres (psql atau pgAdmin). Flyway akan membuat seluruh tabel saat backend pertama kali dijalankan."),
  code(`
CREATE USER ddb WITH PASSWORD 'ganti-password';
CREATE DATABASE ddb OWNER ddb;
`),
  CAP("Gambar 2.2 Membuat user dan database ddb"),

  H1("BAB III  MENJALANKAN BACKEND"),
  H2("3.1 Cara Cepat tanpa PostgreSQL (Profil h2)"),
  code(`
cd backend
.\\run-local.ps1 -H2
`),
  CAP("Gambar 3.1 Menjalankan dengan basis data berkas H2"),
  P("Skrip membangun JAR (tanpa test), lalu menjalankan backend di http://localhost:8080. Data disimpan di folder backend/data dan akun demo dibuat otomatis."),
  H2("3.2 Dengan PostgreSQL Lokal (Profil dev)"),
  code(`
cd backend
.\\run-local.ps1                       # meminta password user database ddb
# atau: $env:DB_PASSWORD = "..."; .\\run-local.ps1
`),
  CAP("Gambar 3.2 Menjalankan dengan PostgreSQL lokal"),
  H2("3.3 Dengan Docker Compose"),
  code(`
cp .env.example .env                   # isi DB_PASSWORD, JWT_SECRET, DATA_KEY
docker compose up -d --build
# API     : http://localhost/api/v1/...
# Swagger : http://localhost/swagger-ui.html
# Health  : http://localhost/health
`),
  CAP("Gambar 3.3 Menjalankan gateway, backend, dan PostgreSQL"),
  NOTE("Konfigurasi Docker Compose dan gateway belum pernah dijalankan karena Docker belum terpasang di laptop pengembang (lihat Bab X)."),
  H2("3.4 Verifikasi"),
  table(["Pemeriksaan", "Alamat / perintah", "Hasil yang diharapkan"], [
    ["Kesehatan", "GET http://localhost:8080/actuator/health", "{\"status\":\"UP\"}"],
    ["Dokumentasi API", "http://localhost:8080/swagger-ui.html", "Swagger UI menampilkan 8 grup tag dan 73 operasi"],
    ["Login demo", "POST /api/v1/auth/login (Gambar 3.4)", "Respons berisi accessToken dan refreshToken"],
    ["Akses dari ponsel", "http://<IP-laptop>:8080 (Wi-Fi yang sama)", "Health check dapat diakses dari ponsel"],
  ], [2000, 3870, 3200], { boldFirst: true, size: 18 }),
  CAP("Tabel 3.1 Verifikasi setelah backend berjalan"),
  H2("3.5 Akun dan Data Demo"),
  table(["Akun", "Password", "PIN", "Rekening"], [
    ["demo", "Demo@1234", "123456", "1400123456 (riwayat 5 bulan)"],
    ["andi, budi, citra", "Demo@1234", "123456", "1400000002 / 1400000003 / 1400000004"],
  ], [2300, 1900, 1400, 3470], { boldFirst: true }),
  CAP("Tabel 3.2 Akun demo (dibuat bila SEED_DEMO_DATA = true dan akun demo belum ada)"),
  H2("3.6 Contoh Alur API"),
  code(`
# 1) Login
curl -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" \\
  -d '{"identifier":"demo","password":"Demo@1234","deviceId":"dev-1","deviceName":"Laptop"}'
# -> {"accessToken":"...","refreshToken":"...","sessionId":"...","user":{...}}

# 2) Cek saldo
curl http://localhost:8080/api/v1/accounts/me -H "Authorization: Bearer <accessToken>"

# 3) Transfer sesama bank (wajib Idempotency-Key; pakai ulang kunci yang sama saat retry)
curl -X POST http://localhost:8080/api/v1/transfers \\
  -H "Authorization: Bearer <accessToken>" -H "Content-Type: application/json" \\
  -H "Idempotency-Key: 6f1c2a9e-0b7d-4c1e-9a33-1f2e3d4c5b6a" \\
  -d '{"bankCode":"DDB","accountNumber":"1400000002","amount":150000,"note":"makan","pin":"123456"}'
`),
  CAP("Gambar 3.4 Contoh login, cek saldo, dan transfer"),

  H1("BAB IV  KONFIGURASI"),
  table(["Kunci application.yml / env", "Default", "Keterangan"], [
    ["spring.profiles.default", "dev", "Profil lain: test (H2 in-memory), h2 (berkas H2)"],
    ["DB_URL / DB_USER / DB_PASSWORD", "localhost:5432/ddb, ddb, ddb", "Koneksi PostgreSQL"],
    ["spring.jpa.hibernate.ddl-auto", "validate", "Skema hanya diubah lewat Flyway"],
    ["JWT_SECRET (ddb.security.jwt-secret)", "nilai dev", "Wajib diganti di server; ≥ 32 karakter"],
    ["ddb.security.access-token-minutes / refresh-token-days", "15 / 7", "Umur token"],
    ["DATA_KEY (ddb.security.data-key)", "nilai dev", "AES-256 base64 32 byte; wajib diganti di server"],
    ["EXPOSE_OTP (ddb.expose-otp)", "true", "Kode OTP ikut di respons (demoCode)"],
    ["SEED_DEMO_DATA (ddb.seed-demo-data)", "true", "Data demo saat basis data kosong; false pada profil test"],
    ["DDB_CORS_ALLOWED_ORIGINS (ddb.cors.allowed-origins)", "http://localhost:*, http://127.0.0.1:*", "Origin Web yang boleh memanggil API langsung dari peramban (pisahkan koma); tambahkan domain Web saat deploy"],
    ["PORT", "8080", "Port HTTP"],
    ["server.error.include-message", "never", "Detail internal tidak dibocorkan"],
  ], [3400, 2270, 3400], { boldFirst: true, size: 17 }),
  CAP("Tabel 4.1 Konfigurasi backend"),
  P("Membuat nilai rahasia acak:"),
  code(`
python -c "import secrets;print(secrets.token_urlsafe(48))"                                  # JWT_SECRET
python -c "import secrets,base64;print(base64.b64encode(secrets.token_bytes(32)).decode())"   # DATA_KEY
`),
  CAP("Gambar 4.1 Membangkitkan rahasia"),

  H1("BAB V  KETERANGAN SOURCE CODE"),
  P(`Kode sumber terdiri atas ${total} berkas Java (${totalLines.toLocaleString("id-ID")} baris termasuk komentar) dalam paket com.diponegoro.ddb. Tabel 5.1 menjelaskan setiap berkas. Keterangan diambil dari Javadoc kelas atau dilengkapi dari isi kode.`),
  table(["Berkas", "Baris", "Keterangan"], invRows, [2700, 700, 5670], { size: 16, center: [1] }),
  CAP("Tabel 5.1 Keterangan seluruh berkas source code backend"),
  H2("5.1 Berkas Kunci yang Perlu Dipahami Lebih Dulu"),
  ...NL([
    "**ledger/LedgerService.java** – satu-satunya tempat saldo berubah. Pahami post(), complete(), failWithRefund(), dan lock().",
    "**auth/TransactionGuard.java** dan **auth/PinService.java** – gerbang e-KYC dan PIN untuk semua transaksi.",
    "**security/SessionJwtConverter.java** – validasi sesi dan timeout tidak aktif pada setiap permintaan.",
    "**transfer/TransferService.java** – contoh lengkap pola transaksi (inquiry → quote → execute, idempotensi, adapter, refund).",
    "**common/ApiException.java** dan **common/GlobalExceptionHandler.java** – format error untuk klien.",
    "**db/migration/V1__init.sql** – skema dan invarian data (lihat SDD-DDB-001-BE Tabel 3.1).",
  ]),

  H1("BAB VI  POLA PENGEMBANGAN"),
  H2("6.1 Menambah Endpoint Baru"),
  ...NL([
    "Tambahkan record request/response di berkas *Dtos.java modul terkait. Gunakan anotasi Bean Validation (@NotBlank, @NotNull) dengan pesan berbahasa Indonesia.",
    "Tambahkan method di service modul. Lempar kesalahan bisnis dengan ApiException.badRequest/unprocessable/conflict/forbidden/notFound/locked(code, message) dengan code berupa HURUF_BESAR yang stabil.",
    "Tambahkan method di controller dengan @Operation(summary = …) agar tampil di Swagger. Ambil pengguna dengan parameter @AuthenticationPrincipal AuthUser me.",
    "Bila endpoint harus dapat diakses tanpa login, tambahkan path ke SecurityConfig.PUBLIC. Lakukan ini hanya untuk alur registrasi, login, atau pemulihan.",
    "Tulis test di kelas *FlowTest yang sesuai (lihat Bab VIII).",
  ]),
  H2("6.2 Menambah Transaksi Keuangan Baru (Wajib Diikuti)"),
  code(`
@Transactional
public TransactionDto bayarSesuatu(String userId, long amount, String pin, String idemKey) {
    var existing = ledger.findByIdempotencyKey(idemKey);           // 1. idempotensi lebih dulu
    if (existing.isPresent()) return ledger.dto(existing.get(), userId);
    if (amount < MIN || amount > MAX) throw ApiException.badRequest("AMOUNT_TOO_SMALL", "...");  // 2. validasi
    guard.authorize(userId, pin);                                  // 3. e-KYC + PIN
    LedgerAccount main = ledger.requireMainAccount(userId);
    LedgerTransaction tx = ledger.post(PostingRequest.builder(idemKey, TxCategory.TOPUP, "Judul", amount)
            .lines(new Line(main.id, -amount), new Line(External.BILLER, amount))   // 4. jumlah baris = 0
            .debitUser(userId).from(main.name, main.number).to("Penerima", "detail")
            .build());
    return ledger.dto(tx, userId);
}
// Controller: @RequestHeader(Idempotency.HEADER) String key  ->  Idempotency.key(me.userId(), key)
`),
  CAP("Gambar 6.1 Kerangka transaksi keuangan baru"),
  P("**Aturan:** jangan pernah mengubah ledger_accounts.balance atau menulis journal_lines di luar LedgerService. Jangan menghapus atau mengubah baris jurnal; koreksi selalu berupa jurnal pembalik. Selalu teruskan Idempotency-Key dari klien."),
  H2("6.3 Mengubah Skema Basis Data"),
  ...NL([
    "Jangan mengubah V1__init.sql yang sudah dijalankan; Flyway akan menolak karena checksum berbeda.",
    "Buat berkas baru src/main/resources/db/migration/V2__deskripsi.sql (nomor versi naik), memakai SQL yang kompatibel dengan PostgreSQL dan H2 mode PostgreSQL.",
    "Sesuaikan entitas JPA. Hibernate berjalan dengan validate, sehingga ketidaksesuaian kolom akan menggagalkan startup.",
    "Entitas JPA wajib ditulis sebagai kelas top-level (bukan kelas bersarang). Repository boleh bersarang.",
  ]),
  H2("6.4 Catatan Gaya Kode"),
  B("Entitas memakai field publik dan relasi sebagai id String (tanpa relasi lazy) untuk menghindari masalah proxy Hibernate."),
  B("Uang bertipe long rupiah. Waktu diambil dari bean Clock (bukan Instant.now()) agar dapat diuji dengan MutableClock."),
  B("Jackson 3: field boolean pada request record wajib ada di JSON. Gunakan Boolean (objek) untuk field opsional."),
  B("Operasi yang menghitung percobaan gagal (PIN, OTP, audit) memakai @Transactional(propagation = REQUIRES_NEW, noRollbackFor = ApiException.class)."),
  B("Komentar dan pesan pengguna ditulis dalam bahasa Indonesia; code error dalam bahasa Inggris HURUF_BESAR."),

  H1("BAB VII  API DAN INTEGRASI KLIEN"),
  P("Daftar lengkap 73 operasi tersedia di Swagger UI, backend/docs/API.md, dan FSD-DDB-001 Tabel 4.2. Aturan bagi klien Android dan Web:"),
  table(["Aturan", "Keterangan"], [
    ["Base URL", "/api/v1 (melalui gateway: http://<host>/api/v1)"],
    ["Autentikasi", "Header Authorization: Bearer <accessToken> untuk semua endpoint kecuali SecurityConfig.PUBLIC"],
    ["Token kedaluwarsa", "Respons 401 → panggil POST /auth/refresh {refreshToken}; refresh token dirotasi, simpan yang baru; bila gagal → layar login"],
    ["Transaksi keuangan", "Header Idempotency-Key (≤ 64 karakter [A-Za-z0-9._:-]); buat sekali per layar konfirmasi dan pakai ulang saat retry"],
    ["Uang dan waktu", "Bilangan bulat rupiah; ISO-8601 UTC, tampilkan dalam WIB"],
    ["Kesalahan", "{\"code\", \"message\", \"fields\"}; tampilkan message ke pengguna, gunakan code untuk logika"],
    ["Kode bank", "DDB untuk sesama Diponegoro Digital Bank; daftar bank lain dari GET /transfers/banks"],
    ["OTP simulasi", "Field otp.demoCode pada respons registrasi/pemulihan/perubahan profil (bila EXPOSE_OTP = true)"],
  ], [2400, 6670], { boldFirst: true, size: 18 }),
  CAP("Tabel 7.1 Aturan integrasi klien"),
  table(["Status HTTP", "Contoh code", "Arti"], [
    ["400", "VALIDATION_ERROR, AMOUNT_TOO_SMALL, PIN_FORMAT", "Input tidak valid"],
    ["401", "UNAUTHORIZED, INVALID_CREDENTIALS", "Belum login, token/sesi tidak valid, atau kredensial salah"],
    ["403", "KYC_REQUIRED, FORBIDDEN", "Belum berhak (mis. e-KYC belum disetujui)"],
    ["404", "NOT_FOUND", "Data atau endpoint tidak ditemukan"],
    ["409", "PIN_EXISTS, ALREADY_FAVORITE", "Konflik dengan data yang ada"],
    ["422", "INSUFFICIENT_BALANCE, PIN_INVALID, INVALID_ACCOUNT", "Aturan bisnis menolak permintaan"],
    ["423", "PIN_BLOCKED", "PIN diblokir sementara"],
  ], [1400, 4170, 3500], { center: [0], size: 18 }),
  CAP("Tabel 7.2 Status HTTP dan contoh kode kesalahan"),
  NOTE("Status untuk tiap kode mengikuti factory ApiException yang dipakai di service; periksa Swagger atau test untuk kasus tertentu."),
  H2("7.1 Menghubungkan Aplikasi Android (Varian Online)"),
  code(`
# Emulator (backend di laptop yang sama)
./gradlew :app:assembleOnlineDebug                       # URL bawaan http://10.0.2.2:8080/
# HP di Wi-Fi yang sama
./gradlew :app:assembleOnlineDebug -Pddb.baseUrl=http://<IP-laptop>:8080/
# Uji end-to-end aplikasi ke backend yang sedang berjalan
./gradlew :app:testOnlineDebugUnitTest -Pddb.liveUrl=http://localhost:8080/
`),
  CAP("Gambar 7.1 Build dan uji varian online"),
  P("Di Android 17 (API 37), izinkan dialog Nearby devices (izin ACCESS_LOCAL_NETWORK) saat aplikasi dibuka; tanpa izin ini sistem memblokir koneksi ke alamat LAN dan aplikasi menampilkan pesan \"Tidak dapat terhubung ke server\". Pastikan juga Windows Firewall mengizinkan port 8080 untuk akses dari HP."),
  H2("7.2 Simulator untuk Pengujian"),
  table(["Kebutuhan uji", "Data / endpoint"], [
    ["Transfer antarbank ditolak (A6)", "bankCode BRI, rekening 9999999999"],
    ["Transfer antarbank timeout (A5)", "bankCode MANDIRI, rekening 8888888888, lalu POST /transfers/{txId}/check-status"],
    ["e-KYC ditolak", "NIK 16 digit berakhiran 0000"],
    ["PLN/tagihan tidak ditemukan", "ID pelanggan berakhiran 0000"],
    ["QRIS tanpa kamera", "GET /qris/demo (contoh kode statis/dinamis)"],
    ["Dana masuk QRIS dari bank lain", "POST /qris/simulate-incoming"],
    ["Transaksi kartu", "POST /cards/{id}/simulate-purchase (+ Idempotency-Key)"],
    ["Kotak email", "GET /me/emails"],
  ], [3500, 5570], { boldFirst: true, size: 18 }),
  CAP("Tabel 7.3 Data dan endpoint simulator"),

  H1("BAB VIII  PENGUJIAN"),
  H2("8.1 Menjalankan Test"),
  code(`
# Git Bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
cd backend
./mvnw test                                    # 47 test integrasi API + laporan JaCoCo, ±70 detik
./mvnw test -Dtest=TransferFlowTest            # satu kelas
# PowerShell: .\\mvnw.cmd test
# Laporan: backend/target/surefire-reports/ dan backend/target/site/jacoco/index.html (cakupan)
`),
  CAP("Gambar 8.1 Menjalankan test"),
  H2("8.2 Menulis Test Baru"),
  P("Turunkan kelas test dari support/ApiTestBase. Kelas dasar ini menjalankan Spring context penuh dengan MockMvc dan H2 (profil test), serta menyediakan helper berikut."),
  table(["Helper", "Kegunaan"], [
    ["newCustomer()", "Membuat nasabah yang sudah lolos OTP, e-KYC, dan memiliki PIN; mengembalikan Customer(userId, token, refreshToken, accountNumber, username)"],
    ["registerAndVerify(username)", "Registrasi + verifikasi OTP saja (belum e-KYC/PIN)"],
    ["post / get / put / delete(url, token, body)", "Memanggil API; hasil Res(status, body JSON) dengan helper text(field) dan num(field)"],
    ["pay(url, token, idemKey, body)", "POST dengan header Idempotency-Key"],
    ["balance(customer)", "Saldo rekening utama"],
    ["uniq()", "String unik untuk username/kunci"],
    ["MutableClock (bean)", "Majukan waktu, mis. untuk menguji OTP kedaluwarsa, timeout sesi, berakhirnya blokir PIN"],
  ], [3300, 5770], { boldFirst: true, size: 18 }),
  CAP("Tabel 8.1 Helper pengujian"),
  code(`
class ContohFlowTest extends ApiTestBase {
    @Test
    void transferKeRekeningSendiriDitolak() throws Exception {
        Customer c = newCustomer();
        Res r = post("/api/v1/transfers/inquiry", c.token(),
                Map.of("bankCode", "DDB", "accountNumber", c.accountNumber()));
        assertThat(r.status()).isEqualTo(422);
        assertThat(r.text("code")).isEqualTo("OWN_ACCOUNT");
    }
}
`),
  CAP("Gambar 8.2 Contoh test baru"),

  H1("BAB IX  DEPLOYMENT"),
  ...NL([
    "Salin .env.example menjadi .env dan isi seluruh rahasia. Jangan commit berkas .env.",
    "Untuk server publik: setel EXPOSE_OTP=false bila sudah ada pengirim OTP sungguhan, dan SEED_DEMO_DATA=false bila data demo tidak diperlukan.",
    "Jalankan docker compose up -d --build. Image backend dibangun multi-stage dan berjalan sebagai pengguna non-root.",
    "Tambahkan domain Web ke DDB_CORS_ALLOWED_ORIGINS (.env) dan ke peta $cors_origin di gateway/nginx.conf. Gateway menyembunyikan header CORS dari backend agar tidak ganda.",
    "Pasang TLS di gateway atau load balancer cloud (NFR-06). Konfigurasi saat ini hanya HTTP port 80.",
    "Pantau GET /health dan log kontainer: docker compose logs -f backend.",
  ]),

  H1("BAB X  TROUBLESHOOTING"),
  table(["Gejala", "Penyebab", "Solusi"], [
    ["'JAVA_HOME is not set' / versi Java salah", "JDK tidak terdeteksi", "Setel JAVA_HOME ke JDK 21+, mis. jbr Android Studio"],
    ["Startup gagal: Schema-validation missing column", "Entitas tidak sesuai skema", "Tambahkan migrasi Flyway baru; jangan ubah V1"],
    ["Flyway: checksum mismatch", "Berkas migrasi lama diubah", "Kembalikan berkas lama; buat V berikutnya. Untuk basis data dev boleh di-drop dan dibuat ulang"],
    ["Entitas tidak dikenali Hibernate", "Entitas ditulis sebagai kelas bersarang", "Pindahkan menjadi kelas top-level"],
    ["Field boolean request selalu error", "Jackson 3 mewajibkan boolean primitif", "Ganti tipe menjadi Boolean"],
    ["Port 8080 sudah dipakai", "Backend lama masih berjalan", "Hentikan proses java.exe yang CommandLine-nya berisi ddb-backend (jangan mematikan Gradle daemon Android)"],
    ["Profil h2 gagal membuka basis data", "Berkas data/ddb terkunci proses lain", "Hentikan instance lain atau hapus folder backend/data (data demo dibuat ulang)"],
    ["401 terus-menerus", "Sesi berakhir (tidak aktif > 5 menit) atau sudah logout", "Panggil /auth/refresh atau login ulang"],
    ["Teks rusak (mis. â†’) di openapi.json/API.md", "Berkas dibangkitkan dengan encoding salah", "Bangkitkan ulang dari /v3/api-docs dan simpan sebagai UTF-8"],
    ["Ponsel tidak dapat mengakses backend", "Firewall Windows atau beda jaringan", "Gunakan Wi-Fi yang sama, izinkan port 8080, atau gunakan ngrok"],
    ["Peramban: CORS error / 403 Invalid CORS request", "Origin aplikasi Web tidak terdaftar", "Tambahkan origin ke DDB_CORS_ALLOWED_ORIGINS, atau jalankan Web lewat proxy Vite"],
  ], [2700, 2770, 3600], { size: 17 }),
  CAP("Tabel 10.1 Troubleshooting"),
  H2("Keterbatasan yang Diketahui"),
  B("Docker Compose dan gateway belum diuji; profil dev belum dijalankan pada PostgreSQL."),
  B("Cakupan test: 92,2% baris, 64,8% cabang (JaCoCo, 12 November 2026)."),
  B("QRIS, biller, dan penerbit kartu masih disimulasikan di dalam service (belum berupa interface adapter)."),
  B("Foto profil berupa URL; push notification memakai polling."),
  H2("Referensi"),
  ...NL([
    "Kelompok Jaran Goyang. (2026). Detailed Design – Backend & Database (SDD-DDB-001-BE) v0.9.",
    "Kelompok Jaran Goyang. (2026). Architectural Design (SAD-DDB-001) v0.9.",
    "backend/README.md, run-local.ps1, docker-compose.yml, .env.example (11 November 2026).",
    "Spring Boot Reference Documentation, versi 4.1.",
  ]),
];

S.build(meta, body, OUT);
