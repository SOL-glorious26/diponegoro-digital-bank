const path = require("path");
const S = require("./ddbstyle");
const { D, P, H1, H2, H3, B, NL, CAP, NOTE, table, img, PW } = S;
const { Paragraph, TextRun, Table, TableRow, TableCell, WidthType, ShadingType, BorderStyle } = D;
const [, , FIG4, FIG3, FIG2, OUT, LOGO] = process.argv;
const f4 = (n) => path.join(FIG4, n), f3 = (n) => path.join(FIG3, n), f2 = (n) => path.join(FIG2, n);

// Pseudocode block: single shaded cell, monospace 9 pt.
function code(src) {
  const bd = { style: BorderStyle.SINGLE, size: 4, color: "808080" };
  return new Table({
    width: { size: PW, type: WidthType.DXA }, columnWidths: [PW],
    rows: [new TableRow({ cantSplit: false, children: [new TableCell({
      width: { size: PW, type: WidthType.DXA }, borders: { top: bd, bottom: bd, left: bd, right: bd },
      shading: { fill: "F7F7F7", type: ShadingType.CLEAR, color: "auto" }, margins: { top: 80, bottom: 80, left: 140, right: 140 },
      children: src.replace(/^\n/, "").replace(/\s+$/, "").split("\n").map((l) => new Paragraph({ spacing: { after: 0, line: 240 },
        children: [new TextRun({ text: l.length ? l : " ", font: "Consolas", size: 18, color: "000000" })] })),
    })] })],
  });
}

const meta = {
  docLabel: "Dokumen 4 – Volume Backend & Database", title: "Detailed Design", subtitle: "UML, Workflow, Algoritma Pseudocode, dan Perancangan Software/Hardware – Backend & Database",
  code: "SDD-DDB-001-BE", version: "1.0", statusShort: "Final",
  status: "Final v1.0 – diterbitkan Project Manager 22 November 2026; peer review (Steven Wibisana) belum diterima sampai tenggat",
  module: "Detailed Design – Backend & Database (ddb-backend)", createdBy: "Salomo Putra Glorious Hokoyoku (Project Manager, Backend)",
  updatedBy: "Salomo Putra Glorious Hokoyoku (Project Manager, Backend)", team: "Kelompok Jaran Goyang – Backend & Database: Salomo P.G. Hokoyoku",
  refs: "SAD-DDB-001 v1.0; FSD-DDB-001 v1.0; STD-DDB-001 v1.0; SRS-DDB-001 v0.1; kode ddb-backend dan V1__init.sql per 22 November 2026",
  date: "22 November 2026", logo: LOGO,
  revisions: [
    ["0.1", "11 Nov 2026", "Salomo P.G. Hokoyoku", "Outline Detailed Design Backend & Database."],
    ["0.9", "11 Nov 2026", "Salomo P.G. Hokoyoku", "Draf lengkap: class diagram, ERD dan kamus data, sequence dan state diagram, activity diagram, 10 pseudocode algoritma inti, perancangan software/hardware."],
    ["0.9.1", "11 Nov 2026", "Salomo P.G. Hokoyoku", "Pembaruan setelah integrasi Android–backend (Fase 5) 11 Nov sore: endpoint POST /auth/pin/verify (18 operasi auth), 45 test, dokumentasi API dibangkitkan ulang."],
    ["0.9.2", "13 Nov 2026", "Salomo P.G. Hokoyoku", "Konfigurasi CORS backend (DEF-04, ddb.cors.allowed-origins) dan pembagian peran CORS dengan gateway; CorsTest (47 test); cakupan JaCoCo 92,3% baris (NFR-12 terpenuhi); hasil uji performa dari STD-DDB-001; tabel keterbatasan diperbarui."],
    ["1.0", "22 Nov 2026", "Salomo P.G. Hokoyoku", "Versi final. Isi teknis tidak berubah; cakupan JaCoCo disamakan dengan STD-DDB-001 (92,2% baris); keterbatasan PostgreSQL diperbarui; backend bersama dipakai Android dan Web tim tanpa perubahan kode. Disetujui PM."],
  ],
  approval: { sign: "../../../documents_area/_aset/project_manager_sign.png", date: "22 Nov 2026", pic: [["Peer reviewer", "Steven Wibisana"]] },
};

const body = [
  // BAB I
  H1("BAB I  PENDAHULUAN"),
  H2("1.1 Tujuan"),
  P("Dokumen ini adalah volume Backend & Database dari Detailed Design (Dokumen 4). Isinya menjabarkan rancangan rinci backend ddb-backend dan basis datanya: struktur kelas (UML), model data, interaksi runtime, alur algoritma, pseudocode logika inti, serta perancangan perangkat lunak dan perangkat keras. Rancangan diturunkan dari kode aktual per 11 November 2026, sehingga dokumen ini sekaligus menjadi acuan Developer Manual Backend (Dokumen 7) dan kasus uji API (Dokumen 6)."),
  H2("1.2 Ruang Lingkup"),
  P("Volume ini mencakup 12 paket backend (auth, account, transfer, payment, topup, card, history, profile, notification, ledger, security, common) serta skema basis data V1 (17 tabel). Detailed Design untuk Android (D04A) dan Web (D04W) disusun di volume terpisah."),
  H2("1.3 Notasi"),
  B("Class diagram mengikuti notasi UML 2: «stereotype» di atas nama kelas, + publik, - privat, ~ paket. Panah putus-putus menyatakan dependensi, garis utuh asosiasi, dan segitiga kosong putus-putus realisasi interface."),
  B("ERD memakai notasi PK (primary key), FK (foreign key), dan U (unique); kardinalitas ditulis di ujung garis."),
  B("Pseudocode memakai kata kunci bahasa Indonesia (JIKA, MAKA, UNTUK SETIAP, KEMBALIKAN, LEMPAR). Kode error ditulis sesuai field code pada respons API."),

  // BAB II
  H1("BAB II  STRUKTUR PERANGKAT LUNAK BACKEND"),
  H2("2.1 Struktur Paket"),
  P("Backend adalah satu aplikasi Spring Boot (modular monolith) dengan paket dasar com.diponegoro.ddb dan 72 berkas Java. Setiap modul bisnis memiliki satu paket yang memuat controller, DTO, service, entitas, dan repository miliknya."),
  table(["Paket", "Isi utama", "Tanggung jawab"], [
    ["auth", "AuthController, AuthService, OtpService, PinService, TransactionGuard, LoginThrottle, entitas User/OtpCode/EkycVerification/TransactionPin/UserSession/BiometricDevice", "FR-01 – FR-05; gerbang transaksi"],
    ["account", "AccountController, AccountService, Pocket", "FR-06 – FR-09"],
    ["transfer", "TransferController, TransferService, InterbankGateway, InterbankSimulator, FavoriteRecipient", "FR-10 – FR-14"],
    ["payment", "PaymentController, PaymentService, QrisCodec", "FR-15 – FR-17"],
    ["topup", "TopUpController, TopUpService, TopUpCatalog, PaidBill", "FR-18 – FR-20"],
    ["card", "VirtualCardController, VirtualCardService, VirtualCard", "FR-21 – FR-22"],
    ["history", "HistoryController, HistoryService, HistoryExporter", "FR-23 – FR-26"],
    ["profile, notification", "ProfileController, ProfileService, NotificationService, entitas Notification/NotificationSettings/EmailMessage", "FR-27 – FR-29"],
    ["ledger", "LedgerService, LedgerAccount, LedgerTransaction, JournalLine, LedgerModels, TransactionDto", "Financial Transaction Core"],
    ["security", "SecurityConfig, JwtService, SessionJwtConverter, AuthUser", "Autentikasi token dan sesi"],
    ["common", "ApiException, GlobalExceptionHandler, ErrorResponse, Idempotency, Money, Ids, DataCipher, Hashing, AuditService, TimeConfig, OpenApiConfig", "Lintas modul"],
    ["demo", "DemoDataSeeder", "Akun dan riwayat demo"],
  ], [1700, 5170, 2200], { boldFirst: true, size: 17 }),
  CAP("Tabel 2.1 Paket backend dan isinya"),
  H2("2.2 Konvensi Rancangan"),
  B("**Lapisan:** Controller (HTTP, validasi Bean Validation) → Service (aturan bisnis, @Transactional) → Repository Spring Data JPA → entitas. Controller tidak mengakses repository secara langsung."),
  B("**Entitas JPA** ditulis sebagai kelas top-level karena Hibernate tidak memindai kelas bersarang; repository boleh bersarang."),
  B("**Uang** bertipe long dalam rupiah, tanpa pecahan. **Waktu** bertipe Instant (UTC) yang disediakan bean Clock agar dapat dikendalikan dalam pengujian (MutableClock)."),
  B("**ID** dibangkitkan dengan prefiks jenis (mis. trx…, fav…). Nomor referensi transaksi untuk pengguna berformat DDByyMMdd + 8 karakter acak."),
  B("**Kesalahan** dilempar sebagai ApiException(status, code, message) dan diubah GlobalExceptionHandler menjadi JSON {code, message, fields}."),
  B("**Transaksi DB:** operasi keuangan berjalan dalam satu @Transactional. Verifikasi PIN dan OTP memakai REQUIRES_NEW agar hitungan salah tetap tersimpan walaupun transaksi pemanggil dibatalkan."),

  // BAB III UML
  H1("BAB III  UML DIAGRAM"),
  H2("3.1 Class Diagram – Financial Transaction Core"),
  P("LedgerService adalah satu-satunya komponen yang menulis jurnal dan mengubah saldo. Modul lain membangun PostingRequest (baris jurnal bertanda: negatif = debit, positif = kredit) dan menyerahkannya ke post(). Setelah transaksi tercatat, LedgerService menerbitkan TransactionPostedEvent."),
  img(f4("cls_ledger.png"), 600, { alt: "Class diagram ledger" }), CAP("Gambar 3.1 Class diagram Financial Transaction Core"),
  H2("3.2 Class Diagram – Authentication dan Keamanan"),
  P("AuthService mengorkestrasi registrasi, login, e-KYC, sesi, dan pemulihan. OtpService dan PinService menangani kode rahasia dengan hash BCrypt dan penghitung percobaan. TransactionGuard menjadi gerbang bagi semua transaksi keuangan. SessionJwtConverter memvalidasi setiap JWT terhadap catatan sesi di basis data."),
  img(f4("cls_auth.png"), 600, { alt: "Class diagram auth" }), CAP("Gambar 3.2 Class diagram Authentication dan keamanan"),
  H2("3.3 Class Diagram – Service Transaksi Keuangan"),
  P("Empat service transaksi memakai pola yang sama: validasi input dan batas → TransactionGuard.authorize() → LedgerService.post() dengan Idempotency-Key."),
  img(f4("cls_tx.png"), 600, { alt: "Class diagram service transaksi" }), CAP("Gambar 3.3 Class diagram service transaksi keuangan"),
  H2("3.4 Class Diagram – Adapter dan Utilitas"),
  P("InterbankGateway adalah satu-satunya adapter berbentuk interface dengan implementasi simulator. QrisCodec dan DataCipher adalah utilitas yang dipakai service terkait."),
  img(f4("cls_adapters.png"), 600, { alt: "Class diagram adapter" }), CAP("Gambar 3.4 Class diagram adapter dan utilitas"),
  H2("3.5 Class Diagram – Account, History, Profile, dan Notification"),
  img(f4("cls_support.png"), 600, { alt: "Class diagram modul pendukung" }), CAP("Gambar 3.5 Class diagram Account, History, Profile, dan Notification"),
  H2("3.6 Entity Relationship Diagram"),
  P("Skema dibagi menjadi dua diagram agar terbaca: identitas, akses, dan notifikasi (Gambar 3.6), serta keuangan dan fitur nasabah (Gambar 3.7). Tabel users adalah pusat relasi kedua diagram."),
  img(f4("erd_identity.png"), 600, { alt: "ERD identitas" }), CAP("Gambar 3.6 ERD identitas, akses, notifikasi, dan audit"),
  img(f4("erd_ledger.png"), 600, { alt: "ERD keuangan" }), CAP("Gambar 3.7 ERD keuangan dan fitur nasabah"),
  H3("Kamus Data"),
  table(["Tabel", "Fungsi", "Kunci dan constraint penting"], [
    ["users", "Akun nasabah", "PK id; UNIQUE username, email, phone_number; password_hash BCrypt; status PENDING_OTP/ACTIVE/FINANCIAL_ENABLED/KYC_REJECTED"],
    ["otp_codes", "Kode OTP per tujuan", "FK user_id; code_hash; attempts; status PENDING/VERIFIED/USED/EXPIRED; indeks (user_id, purpose, status)"],
    ["ekyc_verifications", "Pengajuan e-KYC", "FK user_id UNIQUE; nik_masked (4 digit terakhir) + nik_hash; status PROCESSING/APPROVED/REJECTED"],
    ["transaction_pins", "PIN transaksi", "PK/FK user_id; pin_hash BCrypt; failed_attempts; blocked_until"],
    ["sessions", "Sesi login per perangkat", "FK user_id; refresh_token_hash; status ACTIVE/TERMINATED/EXPIRED; last_seen_at untuk timeout"],
    ["biometric_devices", "Perangkat biometrik terdaftar", "UNIQUE token_hash; UNIQUE (user_id, device_id); revoked"],
    ["ledger_accounts", "Rekening utama, kantong, akun eksternal", "UNIQUE number; CHECK balance ≥ 0 kecuali EXTERNAL; 6 akun eksternal di-seed (interbank, merchant, biller, card, fee, funding)"],
    ["ledger_transactions", "Transaksi keuangan", "UNIQUE reference; UNIQUE idempotency_key; CHECK amount > 0, fee ≥ 0; indeks per debit/credit user + waktu"],
    ["journal_lines", "Baris jurnal double-entry", "FK tx_id, account_id; CHECK amount ≠ 0; append-only; indeks (account_id, posted_at)"],
    ["pockets", "Metadata kantong", "PK/FK account_id; lock_type NONE/DATE/TARGET; lock_until; target > 0"],
    ["favorite_recipients", "Penerima favorit", "UNIQUE (user_id, bank_code, account_number)"],
    ["paid_bills", "Pencegah tagihan dibayar dua kali", "PK bill_key (biller:nomor:periode); FK tx_id"],
    ["virtual_cards", "Kartu virtual", "card_number_enc, cvv_enc (AES-GCM); last4; status ACTIVE/FROZEN; usage_scope; CHECK transaction_limit > 0"],
    ["notification_settings", "Preferensi notifikasi", "PK/FK user_id; types dan channels (daftar dipisah koma)"],
    ["notifications", "Kotak notifikasi", "FK user_id, tx_id; is_read; pushed (sudah diambil perangkat)"],
    ["email_outbox", "Email simulasi", "FK user_id; to_address, subject, body"],
    ["audit_events", "Jejak audit", "id identity; user_id tanpa FK; action (LOGIN_SUCCESS, LOGIN_FAILED, PIN_BLOCKED, …); ip_address"],
  ], [2100, 2400, 4570], { boldFirst: true, size: 17 }),
  CAP("Tabel 3.1 Kamus data skema V1 (sumber: db/migration/V1__init.sql)"),
  P("**Invarian keuangan:** (1) jumlah journal_lines.amount per transaksi selalu 0; (2) ledger_accounts.balance adalah cache yang diperbarui pada transaksi DB yang sama dengan penulisan jurnal sehingga selalu sama dengan jumlah jurnal per akun; (3) pengembalian dana ditulis sebagai baris pembalik, tidak pernah menghapus atau mengubah jurnal."),
  H2("3.7 Sequence Diagram"),
  P("Sequence transfer dana (UC-01) telah disajikan pada SAD-DDB-001 Gambar 4.2. Volume ini menambahkan tiga interaksi lain."),
  img(f4("seq_login.png"), 600, { alt: "Sequence login" }), CAP("Gambar 3.8 Sequence login dengan password"),
  img(f4("seq_auth_request.png"), 600, { alt: "Sequence permintaan terautentikasi" }), CAP("Gambar 3.9 Sequence permintaan API terautentikasi dan validasi sesi"),
  img(f4("seq_qris.png"), 600, { alt: "Sequence QRIS" }), CAP("Gambar 3.10 Sequence pindai dan bayar QRIS"),
  H2("3.8 State Diagram"),
  P("Status akun dijelaskan pada FSD-DDB-001 Gambar 2.1. Status transaksi ledger mengikuti Gambar 3.11: transaksi internal langsung SUCCESS, sedangkan transfer antarbank dimulai dari PROCESSING."),
  img(f4("state_tx.png"), 520, { alt: "State transaksi" }), CAP("Gambar 3.11 State diagram status transaksi"),

  // BAB IV Workflow
  H1("BAB IV  WORKFLOW DIAGRAM"),
  P("Workflow bisnis per modul (registrasi, login, transfer, QRIS, top-up, kantong, virtual card, riwayat, profil) telah disajikan pada FSD-DDB-001 Bab VI. Bab ini menyajikan workflow internal backend untuk dua algoritma yang paling menentukan integritas sistem."),
  H2("4.1 Posting Ledger"),
  img(f4("act_post.png"), 520, { alt: "Activity posting ledger" }), CAP("Gambar 4.1 Activity diagram LedgerService.post()"),
  P("Kunci rekening diambil sebelum pemeriksaan idempotensi. Dengan urutan ini, dua permintaan paralel yang membawa kunci yang sama dilayani berurutan: permintaan kedua menemukan transaksi pertama dan mengembalikannya tanpa debit ganda. Penguncian dilakukan berurutan menurut id rekening agar tidak terjadi deadlock."),
  H2("4.2 Verifikasi PIN"),
  img(f4("act_pin.png"), 520, { alt: "Activity verifikasi PIN" }), CAP("Gambar 4.2 Activity diagram PinService.verify()"),

  // BAB V Pseudocode
  H1("BAB V  ALGORITMA PSEUDOCODE"),
  P("Pseudocode berikut diturunkan langsung dari implementasi. Nama method di judul setiap algoritma menunjuk lokasi kode sumber."),
  H2("5.1 Posting Transaksi Ledger (LedgerService.post)"),
  code(`
ALGORITMA PostTransaksi(r: PostingRequest) -> LedgerTransaction
  // berjalan di dalam satu transaksi basis data
  JIKA r.lines kosong ATAU jumlah(r.lines.amount) != 0 MAKA LEMPAR "Jurnal tidak seimbang"
  JIKA r.amount <= 0 MAKA LEMPAR INVALID_AMOUNT
  deltas <- kelompokkan r.lines per accountId, jumlahkan amount
  terkunci <- KunciRekening(kunci(deltas))            // SELECT ... FOR UPDATE, urut id
  JIKA |terkunci| != |deltas| MAKA LEMPAR NOT_FOUND
  lama <- cari transaksi dengan idempotency_key = r.idempotencyKey
  JIKA lama ada MAKA KEMBALIKAN lama                  // BR-04: tanpa debit ganda
  UNTUK SETIAP a DALAM terkunci
      JIKA a.closed MAKA LEMPAR ACCOUNT_CLOSED
      JIKA a.kind != EXTERNAL DAN a.balance + deltas[a.id] < 0 MAKA LEMPAR INSUFFICIENT_BALANCE
  tx <- transaksi baru(id, reference = "DDB" + yyMMdd + 8 karakter acak, r.*)
  simpan tx
  UNTUK SETIAP l DALAM r.lines DENGAN l.amount != 0
      simpan JournalLine(tx.id, l.accountId, l.amount, memo = r.title)
  UNTUK SETIAP a DALAM terkunci: a.balance <- a.balance + deltas[a.id]
  terbitkan TransactionPostedEvent(tx.id)             // diproses notifikasi setelah commit
  KEMBALIKAN tx
`),
  CAP("Pseudocode 5.1 Posting transaksi ledger"),
  H2("5.2 Penguncian Rekening (LedgerService.lock)"),
  code(`
ALGORITMA KunciRekening(ids: himpunan id) -> daftar LedgerAccount
  hasil <- []
  UNTUK SETIAP id DALAM urutkan(ids)                   // urutan tetap -> bebas deadlock
      a <- muat rekening id
      JIKA a tidak ada MAKA lanjut
      muat ulang a dari basis data dengan kunci PESSIMISTIC_WRITE
      // muat ulang wajib: entitas di persistence context bisa menyimpan saldo lama (lost update)
      tambahkan a ke hasil
  KEMBALIKAN hasil
`),
  CAP("Pseudocode 5.2 Penguncian rekening"),
  H2("5.3 Pengembalian Dana (LedgerService.failWithRefund)"),
  code(`
ALGORITMA GagalDanKembalikan(txId, alasan) -> LedgerTransaction
  tx <- muat transaksi txId
  JIKA tx.status = FAILED MAKA KEMBALIKAN tx           // idempoten
  asli <- baris jurnal milik txId
  deltas <- untuk setiap akun: jumlah(-l.amount)
  terkunci <- KunciRekening(kunci(deltas))
  UNTUK SETIAP l DALAM asli
      simpan JournalLine(txId, l.accountId, -l.amount, memo = "Pengembalian dana")
  UNTUK SETIAP a DALAM terkunci: a.balance <- a.balance + deltas[a.id]
  tx.status <- FAILED; tx.statusReason <- alasan
  terbitkan TransactionStatusChangedEvent (bila belum diposting di transaksi DB yang sama)
  KEMBALIKAN tx
`),
  CAP("Pseudocode 5.3 Pengembalian dana dengan jurnal pembalik (BR-05)"),
  H2("5.4 Eksekusi Transfer (TransferService.execute)"),
  code(`
ALGORITMA EksekusiTransfer(userId, bank, rekening, nominal, catatan, pin, idemKey) -> TransactionDto
  lama <- cari transaksi dengan idemKey
  JIKA lama ada MAKA KEMBALIKAN lama
  q <- Quote(userId, bank, rekening, nominal, catatan) // validasi ulang di server:
     // rekening valid (INVALID_ACCOUNT), bukan rekening sendiri (OWN_ACCOUNT),
     // 5.000 <= nominal <= 25.000.000, catatan <= 50, nominal + biaya <= saldo
  TransactionGuard.authorize(userId, pin)              // e-KYC + PIN (A4)
  main <- rekening utama userId
  JIKA bank = DDB MAKA
      target <- rekening utama dengan nomor rekening
      tx <- PostTransaksi(baris: main -nominal, target +nominal; status SUCCESS)
      KEMBALIKAN tx
  SELAIN ITU
      tx <- PostTransaksi(baris: main -(nominal+biaya), ext:interbank +nominal, ext:fee +biaya;
                          status PROCESSING)
      hasil <- InterbankGateway.send(bank, rekening, nominal, tx.reference)
      JIKA hasil = ACCEPTED MAKA tx <- complete(tx.id)
      JIKA hasil = REJECTED MAKA tx <- GagalDanKembalikan(tx.id, "Ditolak jaringan antarbank")  // A6
      JIKA hasil = TIMEOUT  MAKA tx tetap PROCESSING; klien memanggil checkStatus()            // A5
      KEMBALIKAN tx
`),
  CAP("Pseudocode 5.4 Eksekusi transfer (FR-10, FR-11, UC-01)"),
  H2("5.5 Verifikasi PIN (PinService.verify)"),
  code(`
ALGORITMA VerifikasiPIN(userId, pin)                   // transaksi DB terpisah (REQUIRES_NEW)
  JIKA pin tidak cocok pola 6 digit MAKA LEMPAR PIN_FORMAT
  p <- muat transaction_pins userId dengan kunci baris; JIKA tidak ada MAKA LEMPAR PIN_NOT_SET
  JIKA p.blockedUntil > sekarang MAKA LEMPAR PIN_BLOCKED("coba lagi dalam n menit")
  JIKA p.blockedUntil sudah lewat MAKA p.blockedUntil <- kosong; p.failedAttempts <- 0
  JIKA BCrypt.cocok(pin, p.pinHash) MAKA p.failedAttempts <- 0; SELESAI
  p.failedAttempts <- p.failedAttempts + 1
  JIKA p.failedAttempts >= 3 MAKA
      p.blockedUntil <- sekarang + 5 menit; catat audit PIN_BLOCKED; LEMPAR PIN_BLOCKED
  LEMPAR PIN_INVALID("Sisa percobaan: " + (3 - p.failedAttempts))
`),
  CAP("Pseudocode 5.5 Verifikasi PIN dan blokir sementara (FR-04, BR-02)"),
  H2("5.6 Verifikasi OTP (OtpService.verify)"),
  code(`
ALGORITMA VerifikasiOTP(userId, tujuan, kode) -> OtpCode // REQUIRES_NEW
  JIKA kode bukan 6 digit MAKA LEMPAR OTP_FORMAT
  o <- OTP terbaru berstatus PENDING untuk (userId, tujuan); JIKA tidak ada MAKA LEMPAR OTP_NOT_FOUND
  JIKA o.expiresAt <= sekarang MAKA o.status <- EXPIRED; LEMPAR OTP_EXPIRED   // berlaku 2 menit
  JIKA BCrypt.cocok(kode, o.codeHash) = salah MAKA
      o.attempts <- o.attempts + 1
      JIKA o.attempts >= 5 MAKA o.status <- EXPIRED; LEMPAR OTP_EXPIRED("terlalu banyak percobaan")
      LEMPAR OTP_INVALID
  o.status <- VERIFIED
  KEMBALIKAN o
`),
  CAP("Pseudocode 5.6 Verifikasi OTP (FR-01, FR-05, FR-27)"),
  H2("5.7 Validasi Sesi pada Setiap Permintaan (SessionJwtConverter.convert)"),
  code(`
ALGORITMA ValidasiSesi(jwt) -> AuthUser                // tanda tangan & exp sudah diverifikasi Spring Security
  sid <- jwt.claim("sid"); userId <- jwt.subject
  s <- muat sesi sid
  JIKA s tidak ada ATAU s.userId != userId ATAU s.status != ACTIVE MAKA LEMPAR 401 "Sesi berakhir"
  JIKA s.expiresAt < sekarang ATAU s.lastSeenAt + 5 menit < sekarang MAKA
      s.status <- EXPIRED; LEMPAR 401 "Sesi berakhir"    // FR-05: timeout tidak aktif
  JIKA s.lastSeenAt + 30 detik < sekarang MAKA s.lastSeenAt <- sekarang
  KEMBALIKAN AuthUser(userId, sid)
`),
  CAP("Pseudocode 5.7 Validasi sesi dan timeout tidak aktif"),
  H2("5.8 Penarikan dari Kantong (AccountService.withdraw)"),
  code(`
ALGORITMA TarikKantong(userId, pocketId, nominal, konfirmasiDini, pin, idemKey)
  TransactionGuard.requireFinancial(userId)            // KYC_REQUIRED
  p <- metadata kantong milik userId; a <- akun ledger kantong
  JIKA Terkunci(p, a.balance) MAKA
      JIKA konfirmasiDini = salah MAKA LEMPAR POCKET_LOCKED
      VerifikasiPIN(userId, pin)                       // BR-06: penarikan dini butuh PIN
  JIKA nominal <= 0 MAKA LEMPAR INVALID_AMOUNT
  PostTransaksi(kategori POCKET, baris: kantong -nominal, rekening utama +nominal, idemKey)

FUNGSI Terkunci(p, saldo) -> boolean                   // terbuka otomatis saat tercapai (FR-09)
  JIKA p.lockType = NONE   MAKA KEMBALIKAN salah
  JIKA p.lockType = DATE   MAKA KEMBALIKAN tanggalHariIni(WIB) < p.lockUntil
  JIKA p.lockType = TARGET MAKA KEMBALIKAN p.target kosong ATAU saldo < p.target
`),
  CAP("Pseudocode 5.8 Penarikan kantong dan aturan locking"),
  H2("5.9 Pembayaran QRIS (PaymentService.pay dan QrisCodec.crc16)"),
  code(`
ALGORITMA BayarQRIS(userId, payload, nominalInput, pin, idemKey) -> TransactionDto
  lama <- cari transaksi dengan idemKey; JIKA ada MAKA KEMBALIKAN lama
  s <- Pindai(userId, payload)       // dekode TLV EMVCo, cocokkan CRC16, tolak QR sendiri (OWN_QRIS)
  nominal <- JIKA s.dinamis MAKA s.nominal SELAIN ITU nominalInput   // FR-17: nominal terkunci
  JIKA nominal kosong MAKA LEMPAR AMOUNT_REQUIRED
  JIKA nominal < 1.000 ATAU nominal > 10.000.000 MAKA LEMPAR AMOUNT_TOO_SMALL/AMOUNT_TOO_LARGE
  JIKA saldo utama < nominal MAKA LEMPAR INSUFFICIENT_BALANCE        // sebelum PIN diminta
  TransactionGuard.authorize(userId, pin)
  kredit <- JIKA s.bankSama MAKA rekening utama pemilik QR SELAIN ITU ext:merchant
  KEMBALIKAN PostTransaksi(kategori QRIS, baris: utama -nominal, kredit +nominal, idemKey)

FUNGSI CRC16(data) -> 4 digit heksadesimal             // CRC16-CCITT, poly 0x1021, init 0xFFFF
  crc <- 0xFFFF
  UNTUK SETIAP byte b DALAM data
      crc <- crc XOR (b << 8)
      ULANGI 8 kali
          JIKA (crc AND 0x8000) != 0 MAKA crc <- (crc << 1) XOR 0x1021 SELAIN ITU crc <- crc << 1
          crc <- crc AND 0xFFFF
  KEMBALIKAN heksadesimal(crc, 4 digit)
`),
  CAP("Pseudocode 5.9 Pembayaran QRIS dan checksum CRC16"),
  H2("5.10 Pembentukan Kunci Idempotensi (Idempotency.key)"),
  code(`
FUNGSI KunciIdempotensi(userId, header) -> string
  JIKA header kosong ATAU panjang > 64 ATAU tidak cocok pola [A-Za-z0-9._:-]+ MAKA
      LEMPAR IDEMPOTENCY_KEY_REQUIRED
  KEMBALIKAN userId + ":" + header        // dicakup per pengguna: kunci sama milik pengguna lain tidak bentrok
`),
  CAP("Pseudocode 5.10 Pembentukan kunci idempotensi"),

  // BAB VI Software/Hardware
  H1("BAB VI  PERANCANGAN SOFTWARE/HARDWARE"),
  H2("6.1 Perangkat Lunak"),
  table(["Komponen", "Versi / pilihan", "Kegunaan"], [
    ["Bahasa dan runtime", "Java 21 (Eclipse Temurin 21 di kontainer)", "Runtime backend"],
    ["Framework", "Spring Boot 4.1.2: Web MVC, Data JPA, Security, OAuth2 Resource Server (JWT), Validation, Actuator", "REST API, persistensi, keamanan, health check"],
    ["Migrasi skema", "Flyway (+ flyway-database-postgresql)", "Skema berversi; Hibernate ddl-auto = validate"],
    ["Basis data", "PostgreSQL 17 (kontainer), 18 (lokal); H2 mode PostgreSQL (test/h2)", "Penyimpanan permanen; uji otomatis"],
    ["Serialisasi", "Jackson 3", "JSON request/response"],
    ["Dokumentasi API", "springdoc-openapi 3.2.0", "/v3/api-docs dan /swagger-ui.html"],
    ["Ekspor PDF", "OpenPDF 3.0.5", "Ekspor riwayat (FR-26)"],
    ["Build", "Maven Wrapper (mvnw); artefak ddb-backend-0.0.1-SNAPSHOT.jar", "Build dan test"],
    ["Kontainer", "Docker multi-stage: maven:3.9-eclipse-temurin-21 → eclipse-temurin:21-jre-alpine", "Image runtime ringan"],
    ["Gateway", "nginx:1.27-alpine", "Rate limit, CORS, satu pintu /api/**"],
    ["Cakupan test", "JaCoCo 0.8.13 (prepare-agent + report)", "Laporan cakupan kode di target/site/jacoco (NFR-12)"],
  ], [2000, 4270, 2800], { boldFirst: true, size: 17 }),
  CAP("Tabel 6.1 Perangkat lunak backend"),
  H2("6.2 Konfigurasi"),
  table(["Variabel lingkungan", "Default pengembangan", "Keterangan"], [
    ["DB_URL, DB_USER, DB_PASSWORD", "jdbc:postgresql://localhost:5432/ddb, ddb, ddb", "Koneksi basis data"],
    ["JWT_SECRET", "nilai dev (wajib diganti)", "Kunci HMAC HS256, minimal 32 karakter"],
    ["DATA_KEY", "nilai dev (wajib diganti)", "Kunci AES-256 (base64, 32 byte) untuk data kartu"],
    ["EXPOSE_OTP", "true", "Kode OTP dikembalikan di respons (mode simulasi)"],
    ["SEED_DEMO_DATA", "true", "Buat akun dan riwayat demo saat basis data kosong"],
    ["PORT", "8080", "Port HTTP backend"],
    ["DDB_CORS_ALLOWED_ORIGINS", "http://localhost:*, http://127.0.0.1:*", "Origin aplikasi Web yang boleh memanggil API langsung dari peramban (dipisah koma; pola port didukung)"],
  ], [2800, 3170, 3100], { boldFirst: true, size: 17 }),
  CAP("Tabel 6.2 Parameter konfigurasi backend"),
  P("Parameter keamanan lain diatur di application.yml: access token 15 menit, refresh token 7 hari, server.error.include-message = never, dan forward-headers-strategy = framework agar backend bekerja benar di belakang gateway."),
  P("CORS ditangani di dua tempat sesuai cara backend diakses. Saat aplikasi Web memanggil backend langsung (pengembangan lokal), SecurityConfig membentuk CorsConfigurationSource dari ddb.cors.allowed-origins; origin lain ditolak dengan HTTP 403 (perbaikan DEF-04, diuji oleh CorsTest). Saat backend berada di belakang gateway, Nginx yang menetapkan header CORS dan menyembunyikan header CORS dari backend (proxy_hide_header) agar peramban tidak menerima header ganda."),
  H2("6.3 Rancangan Kontainer"),
  B("Build multi-stage: dependensi diunduh lebih dulu agar cache layer efektif, lalu artefak dikemas tanpa test (test dijalankan terpisah)."),
  B("Image runtime berbasis JRE Alpine dan berjalan sebagai pengguna non-root ddb."),
  B("JVM memakai -XX:MaxRAMPercentage=75 sehingga heap menyesuaikan batas memori kontainer."),
  B("Port 8080 hanya diekspos ke jaringan internal compose; satu-satunya port publik adalah 80 pada gateway."),
  H2("6.4 Perangkat Keras"),
  P("Uji performa pada STD-DDB-001 (backend lokal di laptop pengembang, profil h2) menghasilkan p95 API baca 13 ms dan transfer maksimum 94 ms, jauh di bawah batas NFR-02 dan NFR-03. Uji beban dengan banyak pengguna bersamaan belum dilakukan, sehingga spesifikasi server berikut tetap **usulan awal** untuk lingkungan demo dan staging."),
  table(["Lingkungan", "Spesifikasi usulan", "Catatan"], [
    ["Laptop pengembang", "JDK 21+, 8 GB RAM, ruang disk ±5 GB", "Menjalankan backend (profil dev/h2), PostgreSQL lokal, IDE"],
    ["Server demo/staging (VPS)", "2 vCPU, 2–4 GB RAM, 20 GB SSD, Linux + Docker", "Menjalankan gateway, backend, dan PostgreSQL dalam satu host"],
    ["Klien", "Ponsel Android 7.0 (API 24) ke atas; peramban modern", "Sesuai NFR-10"],
  ], [2500, 3570, 3000], { boldFirst: true, size: 17 }),
  CAP("Tabel 6.3 Perancangan perangkat keras (usulan)"),
  H2("6.5 Rancangan Pengujian Backend"),
  P("Pengujian otomatis berupa test integrasi API: Spring context penuh, MockMvc, dan H2 in-memory (profil test). Kelas dasar ApiTestBase menyediakan helper untuk membuat nasabah yang sudah lolos OTP, e-KYC, dan memiliki PIN. MutableClock memungkinkan waktu dimajukan, misalnya untuk menguji kedaluwarsa OTP, timeout sesi 5 menit, atau berakhirnya blokir PIN."),
  table(["Kelas test", "Jumlah test", "Cakupan"], [
    ["AuthFlowTest", "9", "Registrasi + OTP, e-KYC, login, PIN dan blokir, verifikasi PIN mandiri, sesi, pemulihan"],
    ["AccountFlowTest", "9", "Saldo, grafik, kantong, locking, penarikan dini"],
    ["TransferFlowTest", "10", "Sesama bank, antarbank, A1–A6, idempotensi, transfer paralel"],
    ["PaymentTopUpCardFlowTest", "9", "QRIS, top-up/tagihan, virtual card"],
    ["HistoryProfileNotificationTest", "7", "Riwayat, ekspor, profil + OTP, notifikasi"],
    ["DemoDataTest", "1", "Data demo konsisten"],
    ["CorsTest", "2", "Origin localhost diizinkan, origin lain ditolak 403 (DEF-04)"],
    [{ t: "Total", bold: true }, { t: "47", bold: true }, "Seluruhnya lulus pada 13 November 2026 (laporan surefire, ./mvnw verify)"],
  ], [3000, 1300, 4770], { center: [1], size: 17 }),
  CAP("Tabel 6.4 Test otomatis backend"),
  P("Cakupan kode diukur dengan JaCoCo saat ./mvnw verify: 1.787 dari 1.937 baris (92,26%; ditulis 92,2% pada STD-DDB-001) dan 532 dari 821 cabang (64,8%). Target NFR-12 (cakupan logika inti ≥ 70%) terpenuhi untuk cakupan baris. Cakupan cabang lebih rendah karena sebagian jalur error (validasi input dan kegagalan simulator) belum diuji satu per satu."),
  H2("6.6 Keterbatasan dan Rencana Perbaikan"),
  table(["No", "Keterbatasan", "Rencana"], [
    ["1", "QRIS, biller, dan penerbit kartu belum dipisah sebagai interface adapter", "Ekstrak interface seperti InterbankGateway"],
    ["2", "Pemanggilan simulator antarbank berada di dalam transaksi DB yang sama dengan posting", "Untuk integrasi nyata: posting PROCESSING, commit, lalu kirim di luar transaksi"],
    ["3", "Test otomatis dan uji sistem memakai H2; profil dev berjalan di PostgreSQL 18 lokal sejak 13 November 2026", "Jalankan test integrasi pada PostgreSQL (misalnya Testcontainers)"],
    ["4", "Cakupan cabang (branch) baru 64,8% walaupun cakupan baris 92,2%", "Tambah test jalur error: validasi input, simulator gagal/timeout, sesi kedaluwarsa"],
    ["5", "Foto profil hanya berupa URL; push memakai polling", "Unggah berkas; integrasi FCM bila diperlukan"],
    ["6", "openapi.json dan API.md disalin dari server berjalan (sudah diperbaiki 11 Nov)", "Bangkitkan ulang dari /v3/api-docs setiap endpoint berubah"],
  ], [500, 4570, 4000], { center: [0], size: 17 }),
  CAP("Tabel 6.5 Keterbatasan rancangan backend"),
  H2("Referensi"),
  ...NL([
    "Kelompok Jaran Goyang. (2026). Architectural Design (SAD-DDB-001) v1.0.",
    "Kelompok Jaran Goyang. (2026). Functional Design (FSD-DDB-001) v1.0.",
    "Kelompok Jaran Goyang. (2026). System Testing (STD-DDB-001) v1.0.",
    "Kelompok Jaran Goyang. (2026). Software Requirements Specification (SRS-DDB-001) v0.1.",
    "Kode sumber ddb-backend dan db/migration/V1__init.sql (11 November 2026).",
    "Object Management Group. (2017). Unified Modeling Language (UML) Version 2.5.1.",
    "EMVCo. EMV QR Code Specification for Payment Systems – Merchant-Presented Mode.",
  ]),
];

S.build(meta, body, OUT);
