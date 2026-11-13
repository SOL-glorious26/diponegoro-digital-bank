# API Diponegoro Digital Bank

Dibangkitkan dari `openapi.json` (OpenAPI 3.1.0). Total **73 operasi**. Dokumentasi interaktif: `/swagger-ui.html` (spesifikasi mentah: `/v3/api-docs`).

## Aturan umum

- Base URL `/api/v1`. Semua endpoint selain registrasi, login, refresh, dan pemulihan memerlukan `Authorization: Bearer <accessToken>` (JWT HS256, 15 menit).
- Saat respons **401**, panggil `POST /api/v1/auth/refresh` dengan refresh token (7 hari, dirotasi); bila gagal → login ulang.
- Setiap transaksi keuangan wajib header **`Idempotency-Key`** (≤ 64 karakter `[A-Za-z0-9._:-]`, mis. UUID per layar konfirmasi; pakai ulang kunci yang sama saat retry → tidak ada debit ganda).
- Uang = bilangan bulat rupiah. Waktu = ISO-8601 UTC (tampilkan dalam WIB). Tanggal = `yyyy-MM-dd`.
- Error: `{"code": "INSUFFICIENT_BALANCE", "message": "Saldo tidak mencukupi", "fields": {...}}` — `message` siap ditampilkan, `code` stabil untuk logika klien.
- Mode simulasi: kode OTP ikut di respons (`demoCode`) bila `EXPOSE_OTP=true`.

## 1. Authentication (18 operasi)

| Metode | Path | Fungsi | Body | Respons | Header / query |
|---|---|---|---|---|---|
| POST | `/api/v1/auth/biometric-login` | Login biometrik (FR-03) dengan token perangkat yang dibuka oleh sidik jari/wajah | `BiometricLoginRequest` | `AuthTokens` | – |
| POST | `/api/v1/auth/biometric/disable` | Nonaktifkan login biometrik di semua perangkat | – | `MessageResponse` | – |
| POST | `/api/v1/auth/biometric/enroll` | Aktifkan login biometrik di perangkat ini (FR-28) | `BiometricEnrollRequest` | `BiometricEnrollment` | – |
| GET | `/api/v1/auth/ekyc` | Status e-KYC (PROCESSING → APPROVED/REJECTED setelah ±3 detik) | – | `EkycDto` | – |
| POST | `/api/v1/auth/ekyc` | Kirim data e-KYC (FR-02, simulasi). NIK berakhiran 0000 akan ditolak. | `EkycRequest` | `EkycDto` | – |
| POST | `/api/v1/auth/login` | Login dengan username/email/nomor telepon + password (FR-03) | `LoginRequest` | `AuthTokens` | – |
| POST | `/api/v1/auth/logout` | Logout dari perangkat ini | – | `MessageResponse` | – |
| POST | `/api/v1/auth/logout-all` | Logout dari semua perangkat (FR-05) | – | `MessageResponse` | – |
| POST | `/api/v1/auth/pin` | Buat PIN transaksi 6 digit (FR-04) | `PinRequest` | `MessageResponse` | – |
| POST | `/api/v1/auth/pin/verify` | Verifikasi PIN transaksi tanpa transaksi keuangan (layar PIN setelah login, FR-04). Salah 3x berturut-turut memblokir PIN 5 menit (BR-02). | `PinRequest` | `MessageResponse` | – |
| POST | `/api/v1/auth/recovery/request` | Mulai pemulihan password/PIN: kirim OTP (FR-05) | `RecoveryRequest` | `RecoveryStarted` | – |
| POST | `/api/v1/auth/recovery/reset` | Setel password/PIN baru dengan recoveryToken | `RecoveryResetRequest` | `MessageResponse` | – |
| POST | `/api/v1/auth/recovery/verify` | Verifikasi OTP pemulihan → recoveryToken | `RecoveryVerifyRequest` | `RecoveryVerified` | – |
| POST | `/api/v1/auth/refresh` | Tukar refresh token dengan access token baru (refresh token dirotasi) | `RefreshRequest` | `AuthTokens` | – |
| POST | `/api/v1/auth/register` | Registrasi akun (FR-01). OTP dikirim ke email/nomor telepon. | `RegisterRequest` | `RegisterResponse` | – |
| POST | `/api/v1/auth/register/resend-otp` | Kirim ulang OTP registrasi | `ResendOtpRequest` | `OtpInfo` | – |
| POST | `/api/v1/auth/register/verify-otp` | Verifikasi OTP registrasi → akun aktif dan langsung login | `VerifyRegistrationRequest` | `AuthTokens` | – |
| GET | `/api/v1/auth/sessions` | Daftar sesi/perangkat aktif | – | list `SessionDto` | – |

## 2. Account (11 operasi)

| Metode | Path | Fungsi | Body | Respons | Header / query |
|---|---|---|---|---|---|
| GET | `/api/v1/accounts/me` | Ringkasan rekening utama & saldo (FR-06) | – | `AccountOverview` | – |
| GET | `/api/v1/accounts/me/balance-chart` | Grafik saldo harian/mingguan/bulanan (FR-07) | – | `BalanceChart` | period? |
| PUT | `/api/v1/accounts/me/balance-visibility` | Simpan pilihan sembunyikan/tampilkan saldo (FR-06) | `BalanceVisibilityRequest` | `AccountOverview` | – |
| GET | `/api/v1/accounts/pockets` | Daftar kantong (FR-08) | – | list `PocketDto` | – |
| POST | `/api/v1/accounts/pockets` | Buat kantong, opsional langsung dikunci sampai tanggal/target (FR-08, FR-09) | `CreatePocketRequest` | `PocketDto` | – |
| DELETE | `/api/v1/accounts/pockets/{id}` | Hapus kantong; sisa saldo dipindah ke rekening utama | – | – | `Idempotency-Key` |
| GET | `/api/v1/accounts/pockets/{id}` | – | – | `PocketDto` | – |
| PUT | `/api/v1/accounts/pockets/{id}` | Ubah nama, tujuan, target kantong | `UpdatePocketRequest` | `PocketDto` | – |
| POST | `/api/v1/accounts/pockets/{id}/deposit` | Isi kantong dari rekening utama (butuh header Idempotency-Key) | `MoveRequest` | `PocketDto` | `Idempotency-Key` |
| POST | `/api/v1/accounts/pockets/{id}/lock` | Kunci kantong sampai tanggal atau target (FR-09) | `LockPocketRequest` | `PocketDto` | – |
| POST | `/api/v1/accounts/pockets/{id}/withdraw` | Tarik dari kantong; kantong terkunci butuh earlyWithdrawal=true + PIN (BR-06) | `WithdrawRequest` | `PocketDto` | `Idempotency-Key` |

## 3. Transfer (8 operasi)

| Metode | Path | Fungsi | Body | Respons | Header / query |
|---|---|---|---|---|---|
| POST | `/api/v1/transfers` | Eksekusi transfer dengan PIN (FR-10, FR-11). Wajib header Idempotency-Key. | `TransferRequest` | `TransactionDto` | `Idempotency-Key` |
| GET | `/api/v1/transfers/banks` | Daftar bank tujuan (simulasi) beserta biayanya | – | list `BankDto` | – |
| GET | `/api/v1/transfers/favorites` | Penerima favorit (FR-13) | – | list `FavoriteDto` | – |
| POST | `/api/v1/transfers/favorites` | – | `FavoriteRequest` | `FavoriteDto` | – |
| DELETE | `/api/v1/transfers/favorites/{id}` | – | – | – | – |
| POST | `/api/v1/transfers/inquiry` | Validasi rekening tujuan & tampilkan nama penerima (FR-12) | `InquiryRequest` | `RecipientDto` | – |
| POST | `/api/v1/transfers/quote` | Layar konfirmasi: cek nominal, limit, saldo, biaya (UC-01 langkah 3–4) | `QuoteRequest` | `QuoteDto` | – |
| POST | `/api/v1/transfers/{txId}/check-status` | Cek ulang status transfer yang masih diproses (UC-01 A5) | – | `TransactionDto` | – |

## 4. Payment QRIS (5 operasi)

| Metode | Path | Fungsi | Body | Respons | Header / query |
|---|---|---|---|---|---|
| GET | `/api/v1/qris/demo` | [Simulator] Contoh kode QRIS untuk uji tanpa kamera | – | list `DemoQr` | – |
| GET | `/api/v1/qris/my-qr` | QRIS pribadi untuk menerima dana (FR-16); dengan amount = QRIS dinamis (FR-17) | – | `MyQr` | amount? |
| POST | `/api/v1/qris/pay` | Bayar QRIS dengan PIN (FR-15). Wajib header Idempotency-Key. | `PayRequest` | `TransactionDto` | `Idempotency-Key` |
| POST | `/api/v1/qris/scan` | Validasi hasil pindai QRIS → merchant & nominal sebelum PIN (FR-15) | `ScanRequest` | `ScanResult` | – |
| POST | `/api/v1/qris/simulate-incoming` | [Simulator] Pengguna bank lain membayar ke QRIS pribadi Anda | `IncomingRequest` | `TransactionDto` | `Idempotency-Key` |

## 5. Top-up (6 operasi)

| Metode | Path | Fungsi | Body | Respons | Header / query |
|---|---|---|---|---|---|
| GET | `/api/v1/topup/bills/inquiry` | Cek tagihan PDAM/BPJS/Internet (FR-20) | – | `BillInquiry` | biller, customerNumber |
| GET | `/api/v1/topup/catalog` | Katalog: operator, nominal, biaya, biller, e-wallet | – | `Catalog` | – |
| GET | `/api/v1/topup/data-packages` | Deteksi operator dari nomor HP dan daftar paket data (FR-18) | – | `DataPackages` | phone |
| POST | `/api/v1/topup/orders` | Bayar pesanan dengan PIN. Wajib header Idempotency-Key. | `ExecuteRequest` | `TransactionDto` | `Idempotency-Key` |
| GET | `/api/v1/topup/pln/inquiry` | Cek ID pelanggan PLN (FR-19). Berakhiran 0000 = tidak ditemukan. | – | `PlnCustomer` | meterNumber |
| POST | `/api/v1/topup/quote` | Ringkasan pesanan untuk layar konfirmasi | `OrderRequest` | `Order` | – |

## 6. Virtual Card (7 operasi)

| Metode | Path | Fungsi | Body | Respons | Header / query |
|---|---|---|---|---|---|
| GET | `/api/v1/cards` | – | – | list `CardDto` | – |
| POST | `/api/v1/cards` | Buat virtual card domestik/internasional (FR-21) | `CreateRequest` | `CardDto` | – |
| POST | `/api/v1/cards/{id}/activate` | – | – | `CardDto` | – |
| POST | `/api/v1/cards/{id}/details` | Lihat nomor lengkap, masa berlaku, CVV — wajib PIN (FR-22) | `PinRequest` | `CardDetails` | – |
| POST | `/api/v1/cards/{id}/freeze` | – | – | `CardDto` | – |
| PUT | `/api/v1/cards/{id}/limit` | Atur limit per transaksi (berlaku untuk transaksi berikutnya) | `LimitRequest` | `CardDto` | – |
| POST | `/api/v1/cards/{id}/simulate-purchase` | [Simulator] Transaksi merchant memakai kartu. Wajib header Idempotency-Key. | `PurchaseRequest` | `TransactionDto` | `Idempotency-Key` |

## 7. History & Dashboard (4 operasi)

| Metode | Path | Fungsi | Body | Respons | Header / query |
|---|---|---|---|---|---|
| GET | `/api/v1/history` | Riwayat terbaru dulu dengan paginasi; filter & pencarian dapat dikombinasikan (FR-23) | – | `HistoryPage` | range?, from?, to?, category?, direction?, status?, q?, page?, size? |
| GET | `/api/v1/history/export` | Ekspor riwayat sesuai filter aktif ke CSV atau PDF (FR-26, Web) | – | string | format?, range?, from?, to?, category?, direction?, status?, q? |
| GET | `/api/v1/history/monthly-summary` | Pemasukan & pengeluaran per bulan untuk grafik dashboard (FR-25) | – | list `MonthSummary` | months?, endMonthsBack? |
| GET | `/api/v1/history/{txId}` | Detail transaksi + baris jurnal ledger (FR-24) | – | `TransactionDetail` | – |

## 8. Profile & Notification (14 operasi)

| Metode | Path | Fungsi | Body | Respons | Header / query |
|---|---|---|---|---|---|
| GET | `/api/v1/me` | Profil pengguna yang login (FR-27) | – | `UserDto` | – |
| GET | `/api/v1/me/emails` | [Simulator] Kotak keluar email notifikasi | – | list `EmailDto` | – |
| GET | `/api/v1/me/notification-settings` | – | – | `SettingsDto` | – |
| PUT | `/api/v1/me/notification-settings` | Pilih jenis & kanal notifikasi (FR-28) | `SettingsRequest` | `SettingsDto` | – |
| GET | `/api/v1/me/notifications` | Kotak notifikasi (FR-29) | – | `NotificationPage` | page? |
| GET | `/api/v1/me/notifications/push` | Ambil notifikasi push yang belum ditampilkan perangkat (polling; sekali ambil) | – | list `NotificationDto` | – |
| POST | `/api/v1/me/notifications/read-all` | – | – | `MessageResponse` | – |
| GET | `/api/v1/me/notifications/unread-count` | – | – | object | – |
| POST | `/api/v1/me/notifications/{id}/read` | – | – | `MessageResponse` | – |
| POST | `/api/v1/me/password` | Ubah password (FR-28) | `PasswordChange` | `MessageResponse` | – |
| PUT | `/api/v1/me/photo` | – | `PhotoRequest` | `UserDto` | – |
| POST | `/api/v1/me/pin` | Ubah PIN transaksi (FR-28) | `PinChange` | `MessageResponse` | – |
| POST | `/api/v1/me/profile-change` | Ajukan perubahan username/email/telepon → OTP dikirim (FR-27) | `ProfileDraft` | `OtpInfo` | – |
| POST | `/api/v1/me/profile-change/confirm` | Konfirmasi perubahan profil dengan OTP | `OtpCodeRequest` | `UserDto` | – |
