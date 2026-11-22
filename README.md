# Diponegoro Digital Bank

Proyek mata kuliah **Software Engineering (UKSW)** dari **Kelompok Jaran Goyang**.
Proyek ini membuat aplikasi bank digital dengan dua tampilan, yaitu **aplikasi Android** dan **aplikasi Web**. Keduanya memakai **satu backend** yang sama.

> 📖 **Anggota tim, baca [`PANDUAN_PROYEK.md`](PANDUAN_PROYEK.md) dulu.**
> Panduan itu menjelaskan teknologi, cara kerja aplikasi, apa itu API, daftar fitur, dan progres proyek dengan bahasa sederhana.

## Isi repositori

| Folder | Isi |
|---|---|
| [`code_area/apps/android/`](code_area/apps/android) | 📱 **Aplikasi Android** (Kotlin + Jetpack Compose) |
| [`code_area/apps/web/digitalbanking-node/`](code_area/apps/web/digitalbanking-node) | 🌐 **Aplikasi Web** buatan tim Web (Node.js + Express + EJS) |
| [`code_area/backend/`](code_area/backend) | ⚙️ **Backend / server** (Java + Spring Boot + PostgreSQL), dipakai Android dan Web |
| [`code_area/gateway/`](code_area/gateway) | Konfigurasi API Gateway (Nginx) |
| [`code_area/pengujian/`](code_area/pengujian) | Skrip uji otomatis dan bukti pengujian |
| [`code_area/uml-design/`](code_area/uml-design), [`code_area/submit_mobile/`](code_area/submit_mobile) | Diagram UML dan kiriman modul asli tiap anggota tim Android |
| [`code_area/docs_generator/`](code_area/docs_generator) | Skrip pembuat dokumen .docx dan gambar diagram |
| [`documents_area/`](documents_area) | 📄 **Semua dokumen proyek** (D00–D09W, SRS, Project Brief). Satu folder per volume, isinya versi terbaru saja. |

## Menjalankan cepat

> 📌 **Bingung cara menjalankan?** Ikuti [`TUTORIAL_MENJALANKAN_WEB.md`](TUTORIAL_MENJALANKAN_WEB.md), isinya langkah demi langkah dari nol sampai tes.
>
> ⚠️ **Perhatikan path-nya, guys!** Semua perintah di bawah dijalankan dari **folder utama repo** (`diponegoro-digital-bank`).
> Folder `backend` ada **di dalam** folder `code_area`, jadi path-nya `code_area\backend`, bukan `backend` saja.

```powershell
# 1. Backend (port 8080), tanpa perlu instal PostgreSQL
cd code_area\backend; powershell -ExecutionPolicy Bypass -File .\run-local.ps1 -H2

# 2. Web (terminal kedua) → buka http://localhost:3000
cd code_area\apps\web\digitalbanking-node; copy .env.example .env; npm.cmd install; npm.cmd start

# 3. Android (emulator, terminal ketiga), varian online
cd code_area\apps\android; .\gradlew :app:installOnlineDebug
```
Akun demo: `demo` / `Demo@1234`, PIN `123456`.

> ⚠️ File rahasia **tidak** ikut di repo ini: `.env`, keystore rilis Android, `local.properties`.
> Salin `code_area/.env.example` menjadi `.env` lalu isi sendiri. Untuk keystore, minta ke PM.
