# LANJUTKAN — Serah-terima sesi (tingkat proyek)

Terakhir diperbarui: 11 November 2026

> **Cara melanjutkan:** buka Claude Code baru, ketik:
> `Baca LANJUTKAN.md dan DAFTAR_PEKERJAAN.md di folder submit_progress_digital_banking_project, lalu lanjutkan.`
> Path: `C:\Users\salomo_ADMIN\profesional_life\projects\submit_progress_digital_banking_project`
>
> - `DAFTAR_PEKERJAAN.md` = daftar pekerjaan berfase + status (✅/🔄/⏳/✋).
> - `backend/README.md` = cara menjalankan backend, struktur, aturan API.
> - `apps/android/LANJUTKAN.md` + `NOTES.md` = detail aplikasi Android; `apps/web/README.md` = aplikasi Web; `pengujian/README.md` = uji end-to-end. Struktur folder: `README.md` di root.

## Keputusan yang sudah diambil pengguna
- Teknologi sesuai `submit_mobile/README.md`: Kotlin/Compose/MVVM (mobile); **Java + Spring Boot + REST + PostgreSQL + JWT + Docker** (backend).
- Nilai usulan (batas transfer Rp25 jt, biaya antarbank Rp2.500, batas QRIS, limit kartu, saldo awal Rp5 jt, timeout 5 menit) **dipakai dulu**.
- Arsitektur: Android & Web → API Gateway → satu backend → PostgreSQL.
- Kerja terstruktur: ikuti `DAFTAR_PEKERJAAN.md`, perbarui statusnya setiap selesai.

## Status singkat (sore 11 Nov 2026)
- **Fase 5 selesai.** Android: flavor `demo` (data simulasi, seperti sebelumnya) dan `online` (backend REST).
  - Jalankan online di emulator: backend `.\run-local.ps1 -H2`, lalu `./gradlew :app:assembleOnlineDebug` (URL default `http://10.0.2.2:8080/`).
  - HP di Wi-Fi yang sama: `./gradlew :app:assembleOnlineDebug -Pddb.baseUrl=http://<IP-laptop>:8080/`.
  - Android 17: izinkan dialog "Nearby devices" (izin `ACCESS_LOCAL_NETWORK`), kalau tidak muncul pesan "Tidak dapat terhubung ke server".
  - Test: `./gradlew :app:testDemoDebugUnitTest` (63) dan `:app:testOnlineDebugUnitTest -Pddb.liveUrl=http://localhost:8080/` (58, termasuk uji live).
- Backend: endpoint baru `POST /auth/pin/verify`; **45 test lulus**; `docs/openapi.json` + `docs/API.md` sudah bersih (73 operasi).
- Cadangan Android sebelum Fase 5: `_arsip/backup_android_sebelum_fase5_2026-11-11.zip`.

### Status sebelumnya
- **Backend (`backend/`) selesai untuk 8 modul SRS**: 72 operasi API, **44 test lulus** (`./mvnw test`).
  Termasuk test transfer paralel yang menemukan & memperbaiki bug *lost update* saldo (`LedgerService.lock`).
- Server sudah terbukti jalan dengan profil `h2`: `cd backend; .\run-local.ps1 -H2` → `http://localhost:8080/swagger-ui.html`.
  Akun demo `demo` / `Demo@1234` / PIN `123456`, saldo Rp25.260.000 (sama dengan Android).
- Gateway Nginx (`gateway/nginx.conf`), `docker-compose.yml`, `.env.example` sudah ditulis, **belum diuji** (Docker belum terpasang).
- Aplikasi Android: masih mode offline (mock). Lolos uji emulator; 54 test lulus.

## Langkah berikutnya (urut)
1. ~~Perbaiki `backend/docs/API.md`~~ ✅ (11 Nov sore). **Perbaiki `backend/docs/API.md`** (dibuat otomatis tapi rusak): teks dalam backtick hilang karena shell, dan karakter
   "→"/"±" jadi mojibake. Buat ulang dengan skrip Python yang ditulis ke file (bukan `python -c` di bash) dan baca
   JSON dengan `encoding='utf-8'`. `backend/docs/openapi.json` juga perlu dicek encoding-nya.
2. ✋ **Minta password user `postgres`** (PostgreSQL 18 sudah jalan di laptop) → buat user/database `ddb`
   (SQL ada di `backend/README.md`) → jalankan `.\run-local.ps1` (profil `dev`) dan pastikan Flyway + seed jalan di PostgreSQL.
3. Tambah laporan cakupan test JaCoCo (NFR-12 ≥ 70%).
4. ~~**Fase 5 — integrasi Android**~~ ✅ (11 Nov sore). **Fase 5 — integrasi Android** (lihat DAFTAR_PEKERJAAN.md 5.1–5.6): Retrofit + OkHttp, flavor `demo` & `online`,
   repository REST per modul, token aman, refresh otomatis, header `Idempotency-Key`.
5. Fase 6 — tes di HP: backend di laptop + HP di Wi-Fi yang sama (`http://<IP-laptop>:8080`) atau ngrok (sudah terpasang).

## Catatan teknis penting
- Build/test backend: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` lalu `./mvnw test` (Git Bash).
- Entitas JPA **harus file top-level** (Hibernate tidak memindai kelas bersarang); repository boleh bersarang
  (`considerNestedRepositories = true`).
- Jackson 3 (`tools.jackson.*`): field primitif `boolean` di request record wajib ada di JSON → pakai `Boolean`.
- Validasi PIN & OTP berjalan di transaksi DB terpisah (`REQUIRES_NEW`) agar hitungan salah tidak ikut di-rollback.
- Jangan pakai heredoc panjang berisi kode Java di Bash (sering gagal parse) — pakai tool Write.
- Saat menghentikan server: cari proses `java.exe` dengan CommandLine berisi `ddb-backend` (jangan matikan Gradle daemon).
