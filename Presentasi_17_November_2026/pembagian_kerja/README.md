# Pembagian Kerja: Kelompok Jaran Goyang

Folder ini mengumpulkan semua dokumen tentang pembagian kerja proyek Diponegoro Digital Bank.
Isinya **salinan**; dokumen aslinya tetap ada di lokasi semula (lihat kolom "Asal").
Dikumpulkan: 16 November 2026. Diperbarui: 17 November 2026 (MDTA v0.2).

| No | Berkas | Isi | Asal |
|---|---|---|---|
| 01 | `01_Pembagian_Modul_Coding_MDTA-DDB-001_v0.2.docx` | **Module Decomposition & Task Allocation**: 8 modul bisnis, pembagian ke Android/Web/Backend/Database, PIC, dependency, urutan pengerjaan, RACI. **v0.2 (17 Nov 2026)**: semua butir "Perlu Ditentukan" sudah diisi keputusan dari kode (teks hijau), slot Backend diisi Salomo | v0.1 asli (4 Okt 2026): `documents_area/00_Referensi/PANDUAN_MODULE.docx`; skrip: `code_area/docs_generator/gen/patch_mdta_02.py` |
| 02 | `02_Pembagian_Modul_Android_submit_mobile_README.md` | **Pembagian coding modul Android** per anggota tim Mobile | `code_area/submit_mobile/README.md` |
| 03 | `03_Pembagian_9_Dokumen_DWP-DDB-001_v1.3.docx` | **Pembagian 9 dokumen**: PIC tiap volume, 125 tugas (Bab III), alur review, Definition of Done | `documents_area/D00_DWP_Work_Plan/` |
| 04 | `04_Job_Desk_Mobile_Lead_Salomo.md` | Job desk Mobile Team Lead / Mobile Architect (Salomo) | `documents_area/00_Referensi/` |

---

## Ringkasan 1: Pembagian coding modul Android (dari berkas 02)

| PIC | Modul 1 | Modul 2 |
|---|---|---|
| Salomo | Authentication | Virtual Card |
| Cecilia | Transfer | Transaction History & Dashboard |
| Gracia | Payment (QRIS) | Profile & Notification |
| Grace | Account | Top-up |

Backend dan Database dikerjakan seluruhnya oleh Salomo. Aplikasi Web akhirnya diimplementasikan oleh Salomo
(lihat D01 bagian 1.7); anggota tim Web berperan pada review, UI Map, pengujian, dan dokumentasi Web.

## Ringkasan 2: Pembagian 9 dokumen (dari berkas 03, Tabel 3.2)

| Kategori | Android | Web | Backend & Database | Lintas platform |
|---|---|---|---|---|
| 1 Project Document | – | – | – | Salomo |
| 2 Architectural Design | Masukan: Gracia | Masukan: Steven | Salomo | Salomo (PIC) |
| 3 Functional Design | – | – | – | Salomo; review Steven |
| 4 Detailed Design | Gracia (D04A) | Rafi (D04W) | Salomo (D04B) | – |
| 5 User Interface Map | Cecilia (D05A); dukungan Grace | Nabila (D05W); dukungan Permata | – | – |
| 6 System Testing | Eksekusi: Gracia, Grace | Eksekusi: Permata | Kasus uji API: Steven; data uji: Salomo | Steven (PIC) |
| 7 Developer Manual | Salomo (D07A); bagian modul Gracia | Steven (D07W); Nabila | Salomo (D07B) | – |
| 8 System Program Demo | Skenario: Cecilia | Skenario: Nabila | Lingkungan: Salomo, Permata | Rafi (PIC) |
| 9 User Manual | Grace (D09A) | Permata (D09W) | – | – |

Pasangan review R1 (pre-review): Gracia ↔ Cecilia, Rafi ↔ Nabila, Grace ↔ Cecilia, Permata ↔ Nabila, dokumen Salomo ↔ Steven/Rafi.
Review teknis R2: Salomo (untuk dokumen Salomo: Steven).

## Anggota tim

| NIM | Nama | Tim |
|---|---|---|
| 622024022 | Salomo Putra Glorious Hokoyoku | Mobile (PM) |
| 622024019 | Cecilia Adelaide Penlaana | Mobile |
| 622024006 | Gracia Riyanto | Mobile |
| 622024010 | Grace | Mobile |
| 622024008 | Nabila Puspa Annastasya | Web |
| 622024003 | Steven Wibisana | Web |
| 622024005 | Rafi Fadhillah | Web |
| 622024016 | Permata Surya Cahyana | Web |
