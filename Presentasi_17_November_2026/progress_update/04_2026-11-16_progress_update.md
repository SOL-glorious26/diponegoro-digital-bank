# Progress Update #04: Senin, 16 November 2026 (malam)

Proyek: **Diponegoro Digital Bank**, Kelompok Jaran Goyang (Software Engineering UKSW)
Disusun oleh: Salomo (PM)
Tenggat dokumentasi: **24 November 2026, 23.59 WIB** (sisa 8 hari)

---

## 1. Ringkasan singkat

- Dokumen dari anggota tim (D04A Gracia, D05A Cecilia, D05W Nabila) sudah di-review beberapa putaran.
  Catatan review untuk masing-masing ada di `documents_area/other_teams/review/`.
- Presentasi minggu ini ditambah **5 slide tangkapan layar aplikasi Android**.
- Dokumen pembagian kerja dikumpulkan di folder baru **`pembagian_kerja/`**.

---

## 2. Review dokumen anggota

| Dokumen | PIC | Versi terakhir | Status | Catatan review |
|---|---|---|---|---|
| D04A Detailed Design Android | Gracia | v0.4 | Isi teknis kuat (146 nama kelas/fungsi cocok dengan kode). Sisa: hapus catatan "ZIP", gambar ulang sequence Transfer/Top-up, tambah kode AND-xx | `REVIEW_D04A_Gracia*.md` (3 file) |
| D05A UI Map Android | Cecilia | v0.2 | 23 layar sudah sesuai aplikasi. Sisa: diagram navigasi, beberapa jalur, isi kartu layar, tangkapan layar | `REVIEW_D05A_Cecilia*.md` (2 file) |
| D05W UI Map Web | Nabila | v0.6 (PDF) | Isi sudah sesuai aplikasi. Sisa: sisipkan tangkapan layar, kirim .docx | `REVIEW_D05W_Nabila*.md` (3 file) |

Kode layar Android yang disepakati: **AND-01 s.d. AND-23** (sesuai rute di `AuthRoutes.kt` dan `AppRoutes.kt`).
Kode halaman Web: **UI-01 s.d. UI-22**.

### Temuan penting

- Gracia, Cecilia, dan Nabila bekerja dari **salinan repo lama**, sehingga tidak menemukan D06 v0.9.6, D03 v0.9.3,
  dan folder tangkapan layar `code_area/pengujian/bukti/`. Semua itu sudah ada di repo GitHub tim.
- Akses repo tim: Steven, Nabila, Rafi, Permata, Gracia sudah kolaborator. **Grace**: undangan sejak 13 Nov belum diterima.
  **Cecilia**: belum diundang (butuh username GitHub-nya).

---

## 3. Presentasi 17 November

- File: `Presentasi/17_november_2026/Presentasi_MOBILE_Diponegoro_Digital_Bank_dengan_Screenshot.pptx`
  (file asli Cecilia tidak diubah).
- Tambahan bagian **8. Tampilan Aplikasi Android** (slide 16–20, masing-masing 4 tangkapan layar dari uji sistem):
  masuk & keamanan, beranda & akun, transfer (A1, A5, A6), QRIS & top-up, kartu/riwayat/profil. Agenda ditambah butir 8.

---

## 4. Folder pembagian kerja

`pembagian_kerja/` berisi salinan MDTA-DDB-001 (pembagian modul coding; aslinya bernama `PANDUAN_MODULE.docx`),
pembagian modul Android (`submit_mobile/README.md`), DWP v1.3 (pembagian 9 dokumen), dan job desk Mobile Lead,
ditambah `README.md` berisi ringkasan.

---

## 5. Sisa pekerjaan

| # | Pekerjaan | PIC |
|---|---|---|
| 1 | Revisi D04A, D05A, D05W sesuai catatan review | Gracia, Cecilia, Nabila |
| 2 | D08 System Program Demo, D09A, D09W | Rafi, Grace, Permata |
| 3 | Terima undangan GitHub (Grace); undang Cecilia | Grace, Salomo |
| 4 | Review ke-2 D01 v0.9.4, D02 v0.9.5, D03 v0.9.3 | Steven |
| 5 | Update DWP (angka uji, D04W/D07W/TUT-002, versi baru) | Salomo |
| 6 | Semua dokumen ke v1.0 + tanda tangan | Salomo |
