# LANJUTKAN — 13 November 2026 (sebelum tidur)

> **Cara melanjutkan di sesi Claude Code baru**, salin prompt ini:
>
> `Baca LANJUTKAN_2026-11-13.md dan RECAP_PROGRES_2026-11-11.md di folder submit_progress_digital_banking_project, lalu lanjutkan.`
>
> Path proyek: `C:\salomo_ADMIN\profesional_life\projects\submit_progress_digital_banking_project`
> Folder dokumen: `C:\software_engineering\Digital-Banking-Project-01\documents_area`

---

## Yang dikerjakan di sesi ini (13 Nov, dini hari)

1. **Rapikan `documents_area`** → sekarang satu folder per volume (D00…D07B, 00_Referensi, _aset), hanya versi terbaru; versi lama di `_temp_versi_lama/`; indeks di `documents_area/README.md`.
2. **Dokumen disinkronkan** ke status "Web sudah jadi":
   - D03 Functional Design → **v0.9.2** (kolom halaman Web diisi, status FR Web = Ada).
   - D04B Detailed Design Backend → **v0.9.2** (CORS, 47 test, JaCoCo 92,3% baris).
   - DWP Work Plan → **v1.3** (status D03/D04B/D07B).
3. **PostgreSQL dev AKTIF** (task 2.2 selesai):
   - Password `postgres` ditemukan (kandidat pertama dari daftar — angka 6 digit).
   - Dibuat role `ddb` + database `ddb`. Backend profil `dev` jalan: Flyway 18 tabel + seed demo + login API `demo/Demo@1234` → HTTP 200. Server uji sudah dimatikan lagi.
4. **`.env` dibuat** di root proyek (JWT_SECRET & DATA_KEY acak, DB_PASSWORD=ddb) → siap untuk `docker compose`.

Detail lengkap: lihat **RECAP_PROGRES_2026-11-11.md bagian §14 dan §15**.

---

## Langkah selanjutnya (urut prioritas)

### 1. Docker (butuh TINDAKAN SALOMO — tidak bisa diotomasi)
Buka **PowerShell sebagai Administrator**:
```powershell
wsl --install
```
→ **restart laptop** → lalu:
```powershell
winget install Docker.DockerDesktop
```
→ buka Docker Desktop sekali, terima lisensi, tunggu status "running". Lalu:
```powershell
cd C:\salomo_ADMIN\profesional_life\projects\submit_progress_digital_banking_project
docker compose up -d --build
```
→ buka http://localhost/swagger-ui.html
**Setelah Docker jalan**, minta Claude: uji gateway + NF-04 (rate limit login) + isi hasilnya ke D06.

### 2. Uji di HP fisik (butuh TINDAKAN SALOMO)
- SA-08 (kamera QRIS) dan NF-07 (perilaku di ponsel nyata) — satu-satunya kasus uji D06 yang masih "menunggu ponsel fisik".
- Build APK dengan IP laptop: `cd apps\android; .\build-rilis.ps1 -BaseUrl http://<IP-laptop>:8080/`
- Pakai panduan `documents_area\TUT_Panduan_Uji_Ponsel\` (TUT v1.1).

### 3. Review Steven (R2)
- D01, D02, D03, D04B, D07B menunggu peer review ke-2 dari Steven.

### 4. Volume anggota lain (belum ada sama sekali)
- D04A (Detailed Design Android – Gracia), D04W (Web – Rafi), D05A (UI Map Android),
  D07W (Developer Manual Web – Rafi), D08 (System Program Demo), D09A/D09W (User Manual).

### 5. Selaraskan angka kecil (opsional)
- D06 v0.9.5 menulis JaCoCo 92,2% (ukuran 12 Nov); D04B v0.9.2 menulis 92,3% (ukuran 13 Nov). Samakan saat D06 direvisi lagi.

---

## Konteks penting / catatan teknis

- **Tenggat dokumentasi: 24 Nov 2026 23.59 WIB.** Timeline "tidak sekaku itu" (kata Salomo); Web = formalitas, boleh selesai lebih cepat.
- **Status aplikasi:** backend, Android, Web semua SELESAI; uji sistem jalan; **11 defect semua ditutup**.
- **psql** ada di `C:\Program Files\PostgreSQL\18\bin`. Password `postgres` = **tanya Salomo** (tidak ditulis di berkas demi keamanan; kandidat pertama dari daftar berhasil).
- **Backend lokal:** `cd backend; .\run-local.ps1` (PostgreSQL, butuh `-DbPassword ddb`) atau `.\run-local.ps1 -H2` (tanpa PostgreSQL).
  JAVA_HOME: `C:\Program Files\Android\Android Studio\jbr`. Test: `.\mvnw verify` (47 lulus + JaCoCo).
- **Generator dokumen:** `docs_generator\gen\` (node + python). Logo: `submit_mobile\diponegoro-corp-logo.jpeg`.
  - D03: `node doc3.js ..\fig ..\fig3 <OUT.docx> <logo> ..\..\backend\docs\openapi.json`
  - D04B: `node doc4b.js ..\fig4 ..\fig3 ..\fig <OUT.docx> <logo>`
  - DWP pakai patch XML: `patch_dwp13.py` (edit raw XML, bukan generator docx).
- **Aturan simpan dokumen:** versi baru → folder volumenya; versi lama → `_temp_versi_lama/`.
- Dokumen `.docx` belum dirender di Word (tak ada Word/LibreOffice). Setelah dibuka: klik kanan Daftar Isi → *Update Field*.
- Akun demo: `demo / Demo@1234 / PIN 123456` (rek. 1400123456); andi/budi/citra = 1400000002/03/04.

---

Selamat tidur. 😴 Semua aman: tidak ada proses yang tertinggal jalan, tidak ada berkas yang terhapus.
