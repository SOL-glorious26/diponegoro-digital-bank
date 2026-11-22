# Diponegoro Digital Bank – Repositori Kerja Proyek

Proyek mata kuliah Software Engineering (UKSW), Kelompok Jaran Goyang.
Satu backend dipakai bersama oleh aplikasi Android dan aplikasi Web.

## Struktur folder

| Folder | Isi |
|---|---|
| `apps/android/` | Aplikasi Android gabungan 8 modul (Kotlin, Jetpack Compose, MVVM). Varian `demo` (offline) dan `online` (REST). Lihat `apps/android/README.md`. |
| `apps/web/` | Aplikasi Web nasabah (React + Vite, axios, react-router). Lihat `apps/web/README.md`. |
| `backend/` | Backend Java 21 + Spring Boot (modular monolith, PostgreSQL/H2, JWT). Lihat `backend/README.md`. |
| `gateway/` | Konfigurasi API Gateway Nginx (dipakai `docker-compose.yml`). |
| `pengujian/` | Uji end-to-end Web, Android, lintas platform, peramban, dan performa, beserta bukti eksekusi. Lihat `pengujian/README.md`. |
| `submit_mobile/` | Kiriman asli tiap anggota tim Android (Modul-cecilia, Modul-grace, Modul-gracia) dan UML per modul. Aplikasi gabungan sudah dipindah ke `apps/android/`. |
| `uml-design/` | Diagram UML (Virtual Card, Authentication). |
| `Document/` | Dokumen sumber: SRS v0.1, Project Brief, panduan modul. |
| `docs_generator/` | Generator dokumen .docx (D02, D03, D04B, D06, D07A, D07B, Panduan Uji) dan gambar diagram. |
| `_arsip/` | Berkas lama yang tidak dipakai lagi (zip lama, cadangan Android sebelum Fase 5, prototipe Web tim). Disimpan, tidak dihapus. |

Catatan kerja: `DAFTAR_PEKERJAAN.md`; progres terbaru di folder `progress_update/`.

## Menjalankan cepat

```powershell
# 1. Backend (port 8080)
cd backend; .\run-local.ps1 -H2

# 2. Web (http://localhost:5173)
cd apps\web; npm install; npm run dev

# 3. Android (emulator) – varian online
cd apps\android; .\gradlew :app:installOnlineDebug
```

Akun demo: `demo` / `Demo@1234`, PIN `123456`. Nasabah lain: `andi`, `budi`, `citra` (password dan PIN sama).
