# Daftar Pekerjaan — Diponegoro Digital Bank (Backend + Integrasi Mobile)

Dibuat: 10 November 2026 · Acuan: SRS-DDB-001 v0.1, `submit_mobile/README.md` (Technology Stack)

**Tujuan akhir:** Android (dan nanti Web) memakai **satu backend bersama** lewat API Gateway, data
tersimpan permanen di PostgreSQL, dan aplikasi bisa diinstal serta dites langsung di HP.

**Teknologi (sesuai README tim):**
Mobile: Kotlin, Jetpack Compose, MVVM, Repository Pattern ·
Backend: Java, Spring Boot, REST API, PostgreSQL · Data: JSON ·
Keamanan: JWT, HTTPS · Deployment: Docker, Cloud/VPS.

**Keputusan yang dipakai:** nilai usulan (batas transfer, biaya, limit, dll.) dipakai apa adanya dan
ditandai sebagai asumsi.

Legenda status: ✅ selesai · 🔄 sedang dikerjakan · ⏳ belum · ✋ butuh tindakan pengguna

---

## Fase 0 — Persiapan

| # | Pekerjaan | Status |
|---|---|---|
| 0.1 | Cek kesesuaian aplikasi Android dengan SRS (fitur FR-01–FR-29) | ✅ |
| 0.2 | Uji aplikasi di emulator, perbaiki bug yang ditemukan | ✅ |
| 0.3 | Cek alat di laptop: JDK 25/26 ✅, PostgreSQL 18 (jalan) ✅, ngrok ✅, Docker ❌ | ✅ |
| 0.4 | Buat daftar pekerjaan ini | ✅ |

## Fase 1 — Kontrak bersama (titik temu Mobile, Web, Backend)

| # | Pekerjaan | Hasil | Status |
|---|---|---|---|
| 1.1 | Rancang skema database (tabel, relasi, constraint ledger) | `backend/src/main/resources/db/migration/V1__init.sql` | ✅ |
| 1.2 | Rancang daftar endpoint REST per modul + format error + format JWT | `backend/docs/API.md` | ✅ dibangkitkan ulang dari openapi.json (UTF-8, 11 Nov) |
| 1.3 | OpenAPI otomatis dari kode (Swagger UI) untuk tim web | `/swagger-ui.html`, `/v3/api-docs`, `backend/docs/openapi.json` | ✅ 73 operasi (berkas diperbarui, encoding diperbaiki) |

## Fase 2 — Kerangka backend

| # | Pekerjaan | Status |
|---|---|---|
| 2.1 | Proyek Spring Boot 4.1.2 (Java 21, Maven wrapper), struktur modular monolith per modul SRS | ✅ |
| 2.2 | Konfigurasi PostgreSQL + Flyway; profil `dev` (PostgreSQL lokal) dan `test` (H2) | ✅ (dev aktif di PostgreSQL 18, 13 Nov) |
| 2.3 | Penanganan error seragam (JSON), validasi input, health check | ✅ |
| 2.4 | Keamanan dasar: Spring Security + JWT (access token pendek + refresh token), BCrypt | ✅ |

## Fase 3 — Modul backend (porting logika yang sudah teruji di Android ke Java)

| # | Modul | FR | PIC | Status |
|---|---|---|---|---|
| 3.1 | **Ledger core**: double-entry, transaksi DB atomik, kunci idempotensi, refund, jejak audit | BR-03–05, NFR-07 | bersama | ✅ |
| 3.2 | Authentication: registrasi + OTP, e-KYC (simulasi), login JWT, PIN (hash + blokir 3×), sesi, logout semua perangkat, recovery, biometrik | FR-01–05 | Salomo | ✅ 8 test |
| 3.3 | Account: saldo, sembunyikan saldo, grafik saldo, kantong + locking + tarik dini | FR-06–09 | Grace | ✅ 9 test |
| 3.4 | Transfer: sesama & antarbank, cek rekening, favorit, simulator antarbank (A5/A6) | FR-10–14 | Cecil | ✅ 10 test (termasuk paralel) |
| 3.5 | Payment QRIS: decode/encode EMVCo + CRC16, bayar, QR pribadi, QR dinamis | FR-15–17 | Cia | ✅ |
| 3.6 | Top-up: pulsa, paket data, token PLN, tagihan (simulator biller) | FR-18–20 | Grace | ✅ |
| 3.7 | Virtual Card: buat, detail (PIN), bekukan/aktifkan, limit | FR-21–22 | Salomo | ✅ (9 test QRIS+Top-up+Card) |
| 3.8 | History & Dashboard: filter, pencarian, paginasi, detail, grafik bulanan, **ekspor CSV/PDF (FR-26, untuk Web)** | FR-23–26 | Cecil | ✅ |
| 3.9 | Profile & Notification: profil + OTP, keamanan, pengaturan & kotak notifikasi (asinkron) | FR-27–29 | Cia | ✅ (7 test History+Profile+Notif) |
| 3.10 | Data demo (akun `demo`, nasabah lain, riwayat 5 bulan) di-seed saat profil `dev` | — | bersama | ✅ |
| 3.11 | Unit & integration test backend, target cakupan logika inti ≥ 70% (NFR-12) | — | bersama | 🔄 45 test lulus; laporan cakupan (JaCoCo) belum |
| 3.12 | Endpoint `POST /auth/pin/verify` (layar PIN aplikasi setelah login; aturan blokir 3× sama) | FR-04, BR-02 | Salomo | ✅ 1 test |

## Fase 4 — API Gateway & deployment

| # | Pekerjaan | Status |
|---|---|---|
| 4.1 | API Gateway (Nginx): satu pintu `/api/**`, rate limiting, CORS untuk web — `gateway/nginx.conf` | ✅ (belum diuji, butuh Docker) |
| 4.2 | `Dockerfile` backend & gateway + `docker-compose.yml` (gateway + backend + PostgreSQL) | ✅ (belum diuji, butuh Docker) |
| 4.3 | Skrip jalan lokal tanpa Docker: `backend/run-local.ps1` (`-H2` tanpa PostgreSQL) | ✅ |
| 4.4 | ✋ Pasang Docker Desktop (opsional, untuk menjalankan compose) | ⏳ |
| 4.5 | Deploy ke Cloud/VPS dengan HTTPS (Render/Railway/Fly.io atau VPS) | ⏳ |

## Fase 5 — Integrasi Android ke backend

| # | Pekerjaan | Status |
|---|---|---|
| 5.1 | Tambah Retrofit + OkHttp + kotlinx-serialization, klien API + interceptor JWT (refresh otomatis) | ✅ `core/network` (Retrofit 3.0, OkHttp 4.12, kotlinx-serialization 1.9) |
| 5.2 | Product flavor `demo` (offline, repository mock seperti sekarang) & `online` (repository REST) | ✅ `-Pddb.baseUrl=` untuk ganti URL |
| 5.3 | Repository REST per modul (layar & ViewModel tidak diubah) | ✅ 8 modul + notifikasi (interface + Local/Remote); ViewModel hanya dibuat asinkron |
| 5.4 | Penyimpanan token aman (EncryptedSharedPreferences / DataStore) | ✅ `SecureTokenStore`: AES-GCM dengan kunci Android Keystore |
| 5.5 | Penanganan offline/error jaringan + loading state | ✅ pesan error server diteruskan; pesan offline; sesi berakhir → login |
| 5.6 | Pastikan 54 test lama tetap lulus (flavor `demo`) + test repository REST (MockWebServer) | ✅ demo 63 lulus (1 dilewati); online 58 lulus termasuk uji live ke backend |
| 5.7 | Android 17 (API 37): izin `ACCESS_LOCAL_NETWORK` + network security config untuk backend LAN | ✅ diminta saat aplikasi dibuka (varian online) |

## Fase 6 — Tes di HP & rilis

| # | Pekerjaan | Status |
|---|---|---|
| 6.1 | ✋ Instal APK `demo` di HP: tes kamera QRIS, notifikasi, berbagi bukti | ⏳ |
| 6.2 | Tes APK `online` di HP lewat Wi-Fi yang sama atau ngrok (HTTPS) | 🔄 emulator ✅ (login, PIN, Beranda, transfer); HP sungguhan ⏳ |
| 6.3 | Tes end-to-end: 2 HP/emulator saling transfer → saldo konsisten di keduanya | 🔄 uji live (`LiveBackendTest`) + transfer UI DEMO→ANDI terverifikasi di sisi penerima; 2 perangkat ⏳ |
| 6.4 | APK release bertanda tangan + cara distribusi ke tim/dosen | ⏳ |
| 6.5 | Dokumentasi akhir: README backend, cara menjalankan, skenario uji, matriks FR ↔ endpoint | 🔄 `backend/README.md` sudah ada |

---

## Yang butuh dari pengguna

- ✋ **Password user `postgres`** di PostgreSQL 18 lokal (atau izin membuat user/database baru
  `ddb`). Dibutuhkan pada 2.2 untuk menjalankan backend dengan database sungguhan.
  Test otomatis tetap bisa berjalan tanpa ini (memakai H2).
- ✋ Docker Desktop (opsional, Fase 4).
- ✋ Akun cloud untuk deploy (Fase 4.5) — nanti.

## Log kemajuan

- 10 Nov 2026 — Fase 0 selesai. Daftar pekerjaan dibuat.
- 11 Nov 2026 — Kerangka backend, skema DB, keamanan JWT, Ledger, Authentication, Account selesai (17 test lulus).
- 11 Nov 2026 — Semua 8 modul backend selesai, data demo, gateway Nginx, Docker compose, README backend. 44 test lulus. Bug *lost update* saldo saat transaksi paralel ditemukan & diperbaiki. Server terbukti jalan (profil h2) dengan 72 operasi API.
- 11 Nov 2026 (sore) — Fase 5 selesai: Android punya flavor `demo` & `online`; 8 modul + notifikasi terhubung REST. Backend: endpoint `POST /auth/pin/verify` (45 test). openapi.json & API.md dibangkitkan ulang (73 operasi, UTF-8). Ditemukan: Android 17 memblokir akses LAN tanpa `ACCESS_LOCAL_NETWORK` → ditambahkan. Diuji di emulator: login → PIN → Beranda (data server) → transfer Rp10.000 ke ANDI, terlihat di akun penerima.
