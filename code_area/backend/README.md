# Backend Diponegoro Digital Bank

Backend bersama untuk aplikasi **Mobile (Android)** dan **Web**, sesuai SRS-DDB-001 v0.1 bagian 2.1:
satu backend *modular monolith*, diakses lewat **API Gateway**, data di **PostgreSQL**.

| Teknologi (README tim) | Dipakai |
|---|---|
| Java, Spring Boot, REST API | Java 21, Spring Boot 4.1.2 (Web MVC, Data JPA, Security, Validation) |
| PostgreSQL | PostgreSQL 17/18 + migrasi skema **Flyway** (`src/main/resources/db/migration`) |
| JSON | Jackson 3 |
| JWT, HTTPS | Access token JWT HS256 15 menit + refresh token 7 hari (dirotasi); HTTPS di gateway/cloud |
| Docker, Cloud/VPS | `Dockerfile`, `../docker-compose.yml` (PostgreSQL + backend + gateway Nginx) |
| Dokumentasi API | OpenAPI otomatis: `/swagger-ui.html`, `/v3/api-docs` |

## Menjalankan

**Cara cepat (tanpa PostgreSQL, untuk mencoba):**
```powershell
cd backend
.\run-local.ps1 -H2
```

**Dengan PostgreSQL lokal:** lihat "Siapkan PostgreSQL" di bawah, lalu `.\run-local.ps1`.

**Dengan Docker (PostgreSQL + backend + gateway sekaligus):**
```bash
cp .env.example .env      # isi nilai rahasia
docker compose up -d --build
# API: http://localhost/api/v1/...   Swagger: http://localhost/swagger-ui.html
```

**Test otomatis** (memakai H2 di memori, tidak butuh PostgreSQL):
```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./mvnw test        # 47 test, ±40 detik
```

### Siapkan PostgreSQL (sekali saja)

Di terminal admin / pgAdmin, sebagai user `postgres`:
```sql
CREATE USER ddb WITH PASSWORD 'ganti-password';
CREATE DATABASE ddb OWNER ddb;
```
Tabel dibuat otomatis oleh Flyway saat backend pertama kali jalan.

## Akun & data demo

Dibuat otomatis saat database masih kosong (`SEED_DEMO_DATA=true`):

| Akun | Password | PIN | Rekening |
|---|---|---|---|
| `demo` | `Demo@1234` | `123456` | 1400123456 (riwayat 5 bulan) |
| `andi`, `budi`, `citra` | `Demo@1234` | `123456` | 1400000002 / 03 / 04 |

Data uji simulator: lihat `../apps/android/README.md` (sama persis).

## Struktur (satu paket per modul SRS)

| Paket | Modul / FR | PIC |
|---|---|---|
| `ledger` | Financial Transaction Core: double-entry, atomik + kunci baris, idempotensi, refund (BR-03–05, NFR-07) | bersama |
| `auth` | FR-01 – FR-05: registrasi + OTP, e-KYC, login, biometrik, PIN, sesi, pemulihan | Salomo |
| `account` | FR-06 – FR-09: saldo, grafik saldo, kantong + locking | Grace |
| `transfer` | FR-10 – FR-14: sesama & antarbank (adapter `InterbankGateway`), favorit | Cecil |
| `payment` | FR-15 – FR-17: QRIS EMVCo + CRC16, QR pribadi/dinamis | Cia |
| `topup` | FR-18 – FR-20: pulsa, data, PLN, tagihan, e-wallet | Grace |
| `card` | FR-21 – FR-22: virtual card (nomor & CVV terenkripsi AES-GCM) | Salomo |
| `history` | FR-23 – FR-26: riwayat + filter + paginasi, detail + jurnal, grafik bulanan, ekspor CSV/PDF | Cecil |
| `profile`, `notification` | FR-27 – FR-29: profil + OTP, keamanan, notifikasi asinkron (in-app, push, email simulasi) | Cia |
| `security`, `common`, `demo` | JWT, error seragam, audit, enkripsi, data demo | bersama |

## Aturan API untuk klien (Mobile & Web)

- Base URL: `/api/v1`. Semua endpoint selain register/login/refresh/recovery butuh `Authorization: Bearer <accessToken>`.
- Saat dapat **401**, panggil `POST /auth/refresh` dengan refresh token; bila gagal → kembali ke layar login.
- Setiap transaksi keuangan wajib header **`Idempotency-Key`** (UUID dibuat sekali per layar konfirmasi, dipakai
  ulang saat retry) → tidak pernah terjadi debit ganda.
- Uang = bilangan bulat rupiah. Waktu = ISO-8601 UTC (tampilkan dalam WIB).
- Error: `{"code": "INSUFFICIENT_BALANCE", "message": "Saldo tidak mencukupi", "fields": {...}}` —
  `message` siap ditampilkan ke pengguna, `code` stabil untuk logika klien.
- Mode simulasi: kode OTP ikut di respons (`demoCode`) karena belum ada SMS/email sungguhan.

## Keamanan (NFR-06)

BCrypt untuk password, PIN, dan OTP · PIN terpisah dari password · blokir PIN 5 menit setelah 3× salah ·
kunci login 15 menit setelah 5× password salah · JWT berumur pendek + sesi dicek di database (logout semua
perangkat langsung berlaku) · sesi berakhir setelah 5 menit tidak aktif · data kartu terenkripsi ·
jejak audit (`audit_events`) · rate limit per IP di gateway · rahasia lewat environment variable.

## Batasan saat ini

- Integrasi antarbank, QRIS, biller, penerbit kartu = **simulator** lewat adapter (sesuai SRS 2.5).
- Push notification: aplikasi mengambil antrean `GET /me/notifications/push` (FCM dapat ditambahkan).
- Foto profil: hanya URL (`PUT /me/photo`); unggah file belum tersedia.
