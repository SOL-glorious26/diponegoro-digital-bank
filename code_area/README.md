# Diponegoro Digital Bank – Repositori Kerja Proyek

Proyek mata kuliah Software Engineering (UKSW), Kelompok Jaran Goyang.
Satu backend dipakai bersama oleh aplikasi Android dan aplikasi Web.

## Struktur folder

| Folder | Isi |
|---|---|
| `apps/android/` | Aplikasi Android gabungan 8 modul (Kotlin, Jetpack Compose, MVVM). Varian `demo` (offline) dan `online` (REST). Lihat `apps/android/README.md`. |
| `apps/web/digitalbanking-node/` | Aplikasi Web nasabah buatan tim Web (Node.js, Express 5, EJS). Lihat `apps/web/digitalbanking-node/README.md`. |
| `apps/web_dummy_buatan_sendiri/` | Web React lama, **tidak dipakai lagi**. |
| `backend/` | Backend Java 21 + Spring Boot (modular monolith, PostgreSQL/H2, JWT). Lihat `backend/README.md`. |
| `gateway/` | Konfigurasi API Gateway Nginx (dipakai `docker-compose.yml`). |
| `pengujian/` | Uji end-to-end Web, Android, lintas platform, peramban, dan performa, beserta bukti eksekusi. Lihat `pengujian/README.md`. |
| `submit_mobile/` | Kiriman asli tiap anggota tim Android (Modul-cecilia, Modul-grace, Modul-gracia) dan UML per modul. Aplikasi gabungan sudah dipindah ke `apps/android/`. |
| `uml-design/` | Diagram UML (Virtual Card, Authentication). |
| `docs_generator/` | Generator dokumen .docx (D01–D09, tutorial) dan gambar diagram. |

Dokumen proyek (SRS, D00–D09) ada di `documents_area/` di folder utama repo.

Catatan kerja: `DAFTAR_PEKERJAAN.md`; progres terbaru di `progress_update/`.

## Menjalankan cepat

```powershell
# 1. Backend (port 8080)
cd backend; powershell -ExecutionPolicy Bypass -File .\run-local.ps1 -H2

# 2. Web (terminal kedua, http://localhost:3000)
cd apps\web\digitalbanking-node; copy .env.example .env; npm.cmd install; npm.cmd start

# 3. Android (emulator, terminal ketiga) – varian online
cd apps\android; .\gradlew :app:installOnlineDebug
```

Akun demo: `demo` / `Demo@1234`, PIN `123456`. Nasabah lain: `andi`, `budi`, `citra` (password dan PIN sama).
