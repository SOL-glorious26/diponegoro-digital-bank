# documents_area — Diponegoro Digital Bank

Diperbarui: 22 November 2026. Setiap folder hanya berisi **versi terbaru**. Versi lama ada di `_temp_versi_lama/`.

| Folder | Isi terbaru | PIC |
|---|---|---|
| `00_Referensi/` | Template/panduan dosen, Project Brief (docx + pdf), SRS v0.1, Annex-A SRS, draf SRS awal, job desk Mobile Lead | – |
| `D00_DWP_Work_Plan/` | DWP-DDB-001 **v1.5** (tenggat 23 Nov, status volume 22 Nov) | Salomo |
| `D01_Project_Document/` | **v1.0 final** | Salomo |
| `D02_Architectural_Design/` | **v1.0 final** | Salomo |
| `D03_Functional_Design/` | **v1.0 final** | Salomo |
| `D04A_Detailed_Design_Android/` | **v1.0 final** (dari draf Gracia) | Gracia (diselesaikan PM) |
| `D04B_Detailed_Design_Backend_DB/` | **v1.0 final** | Salomo |
| `D05A_UI_Map_Android/` | **v1.0 final** (dari draf Cecilia) | Cecilia (diselesaikan PM) |
| `D05W_UI_Map_Web/` | **v1.0 final** (dari draf v0.7 Nabila, diselesaikan PM) | Nabila |
| `D06_System_Testing/` | **v1.0 final** (Android 28/28, Web Node WN 14/14, LPN 3/3, 14 defect ditutup) | Steven (disusun PM) |
| `D07A_Developer_Manual_Android/` | **v1.0 final** | Salomo |
| `D07B_Developer_Manual_Backend/` | **v1.0 final** | Salomo |
| `D04W_Detailed_Design_Web/` | **v1.0 final** (Rafi + koreksi PM; Web Node) | Rafi |
| `D07W_Developer_Manual_Web/` | **v1.0 final** (ditulis ulang PM; Web Node) | Steven |
| `D08A_System_Program_Demo_Android/` | **v1.0 final** (disusun PM) | Rafi (PIC D08) |
| `D08W_System_Program_Demo_Web/` | **v1.0 final** (Rafi + koreksi PM; gladi belum) | Rafi |
| `D09A_User_Manual_Android/` | **v1.0 final** (disusun PM) | Grace |
| `D09W_User_Manual_Web/` | **v1.0 final** (disusun PM) | Permata |
| `TUT_Panduan_Menjalankan_Web/` | v2.0, tutorial menjalankan & menguji Web tim (artefak pendukung) | Salomo |
| `TUT_Panduan_Uji_Ponsel/` | v1.1 (artefak pendukung) | Salomo |
| `CATATAN_Belajar_Software_Engineering/` | v1.0 (21 Nov), catatan belajar pribadi PM: konsep SE + bedah kode DDB (bukan deliverable) | Salomo |
| `_aset/` | Tanda tangan PM (dipakai saat versi final) | – |
| `_temp_versi_lama/` | Versi lama + `project_brief_digital_banking_dummy.docx`. Boleh dihapus setelah semua dokumen v1.0. | – |

Seluruh volume sudah v1.0. Catatan review anggota: `other_teams/review/`.

Aturan: versi baru disimpan di folder volumenya, lalu versi sebelumnya dipindah ke `_temp_versi_lama/`.
Generator dokumen: `code_area/docs_generator/gen` (D04W: `doc4w.js` + `figs6.py`, D07W: `doc7w.js`;
D01: `patch_d01_095.py` (patch XML atas v0.9.4) + `figs.py`; D02: `doc2.js`; D03: `doc3.js`;
v1.0 (22 Nov): `patch_v100.py` (D02, D03, D04B, D07A, D07B), `patch_d01_100.py`, `patch_doc6_100.py`, `doc4a.js`, `doc5a.js`, `doc8.js`, `doc9a.js`, `figs7.py`, `patch_rafi_web.py`;
D06: `doc6.js`; D07A: `update_kdoc.py` lalu `doc7a.js` (`patch_d07a_093.py`); DWP: `patch_dwp14.py`).
Di laptop tanpa LibreOffice, daftar isi diisi lewat Microsoft Word (perbarui field TOC lalu simpan).
Daftar isi diisi dengan `lo_update_toc.py <in.docx> <out.docx>` (memakai LibreOffice) supaya tidak tampil kosong.
