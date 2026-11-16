# documents_area — Diponegoro Digital Bank

Diperbarui: 16 November 2026. Setiap folder hanya berisi **versi terbaru**. Versi lama ada di `_temp_versi_lama/`.

| Folder | Isi terbaru | PIC |
|---|---|---|
| `00_Referensi/` | Template/panduan dosen, Project Brief (docx + pdf), SRS v0.1, Annex-A SRS, draf SRS awal, job desk Mobile Lead | – |
| `D00_DWP_Work_Plan/` | DWP-DDB-001 v1.3 | Salomo |
| `D01_Project_Document/` | v0.9.4 (status 16 Nov, angka = D06 v0.9.6) | Salomo |
| `D02_Architectural_Design/` | v0.9.5 (status 16 Nov) | Salomo |
| `D03_Functional_Design/` | v0.9.3 (status 16 Nov, uraian workflow) | Salomo |
| `D04B_Detailed_Design_Backend_DB/` | v0.9.2 | Salomo |
| `D05W_UI_Map_Web/` | User Interface Map (Web) v0.3 (PDF) | Nabila |
| `D06_System_Testing/` | v0.9.6 (Android 28/28, uji ponsel fisik) | Steven (disusun PM) |
| `D07A_Developer_Manual_Android/` | v0.9.2 | Salomo |
| `D07B_Developer_Manual_Backend/` | v0.9.2 | Salomo |
| `D04W_Detailed_Design_Web/` | v0.5 (draf awal dari PM) | Rafi |
| `D07W_Developer_Manual_Web/` | v0.5 (draf awal dari PM) | Steven (komponen UI: Nabila) |
| `TUT_Panduan_Menjalankan_Web/` | v1.0, tutorial menjalankan & menguji Web (artefak pendukung) | Salomo |
| `TUT_Panduan_Uji_Ponsel/` | v1.1 (artefak pendukung) | Salomo |
| `_aset/` | Tanda tangan PM (dipakai saat versi final) | – |
| `_temp_versi_lama/` | Versi lama + `project_brief_digital_banking_dummy.docx`. Boleh dihapus setelah semua dokumen v1.0. | – |

Belum ada: D04A (Gracia), D05A (Cecilia), D08 (Rafi), D09A (Grace), D09W (Permata).

Aturan: versi baru disimpan di folder volumenya, lalu versi sebelumnya dipindah ke `_temp_versi_lama/`.
Generator dokumen: `code_area/docs_generator/gen` (D04W: `doc4w.js` + `figs6.py`, D07W: `doc7w.js`;
D01: `patch_d01_094.py` + `figs.py`; D02: `doc2.js`; D03: `doc3.js`).
Daftar isi diisi dengan `lo_update_toc.py <in.docx> <out.docx>` (memakai LibreOffice) supaya tidak tampil kosong.
