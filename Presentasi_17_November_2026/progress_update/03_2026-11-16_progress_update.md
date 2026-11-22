# Progress Update #03: Senin, 16 November 2026

Proyek: **Diponegoro Digital Bank**, Kelompok Jaran Goyang (Software Engineering UKSW)
Disusun oleh: Salomo (PM)
Tenggat dokumentasi: **24 November 2026, 23.59 WIB** (sisa 8 hari)

---

## 1. Ringkasan singkat

- Dokumen 1, 2, dan 3 sudah di-review dan diperbarui. Angka di dalamnya sekarang **sama dengan D06 v0.9.6**.
- Daftar isi ketiga dokumen sekarang **sudah terisi** dan tidak kosong lagi saat dibuka di luar Word.

---

## 2. Hasil review dan perbaikan

| Dokumen | Versi baru | Yang diperbaiki |
|---|---|---|
| D01 Project Document | **v0.9.4** | Hasil uji terbaru (Android 28/28, Web 15/15, 47 test backend, 11/11 defect ditutup); Gantt (uji ponsel fisik selesai 14 Nov); risiko R5 dan R16 termitigasi; "Android 17 (API 37)"; acuan DWP v1.3 |
| D02 Architectural Design | **v0.9.5** | Status komponen dan NFR (NFR-01 s.d. 03 terukur, lintas platform lulus, Firefox/Edge lulus); CORS backend (DEF-04); PostgreSQL lokal berjalan; jumlah berkas Kotlin 110; isu terbuka yang sudah selesai ditutup |
| D03 Functional Design | **v0.9.3** | Kamera QRIS (FR-15) lulus di ponsel fisik; uraian singkat untuk 9 workflow Bab VI; kalimat HTTP/HTTPS diperjelas |

Versi lama dipindah ke `documents_area/_temp_versi_lama/`.

Cara membuat ulang (folder `code_area/docs_generator/gen`):
- D01: `patch_d01_094.py` (patch XML) + gambar dari `figs.py`
- D02: `patch_doc2_095.py` lalu `node doc2.js ../fig <OUT> ../../submit_mobile/diponegoro-corp-logo.jpeg`
- D03: `patch_doc3_093.py` lalu `node doc3.js ../fig ../fig3 <OUT> <logo> ../../backend/docs/openapi.json`
- Daftar isi: `python lo_update_toc.py <in.docx> <out.docx>` (butuh LibreOffice)

---

## 3. Sisa pekerjaan

| # | Pekerjaan | PIC |
|---|---|---|
| 1 | Review ke-2 D01 v0.9.4, D02 v0.9.5, D03 v0.9.3 | Steven |
| 2 | Perbarui DWP: catat D04W, D07W, TUT-002, serta versi baru D01–D03 dan D06 | Salomo |
| 3 | Dokumen yang belum ada: D04A, D05A, D08, D09A, D09W | Gracia, Cecilia, Rafi, Grace, Permata |
| 4 | Login biometrik Android masih simulasi (BiometricPrompt belum dipakai, FR-03 Must) | Salomo / Gracia |
| 5 | Docker + uji rate limit gateway (NF-04) | Salomo |
| 6 | Naikkan semua dokumen ke v1.0 + tanda tangan | Salomo |
