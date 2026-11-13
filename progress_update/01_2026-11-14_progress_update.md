# Progress Update #01: Sabtu, 14 November 2026

Proyek: **Diponegoro Digital Bank**, Kelompok Jaran Goyang (Software Engineering UKSW)
Disusun oleh: Salomo (PM)
Tenggat dokumentasi: **24 November 2026, 23.59 WIB** (sisa 10 hari)

> Ini catatan progres pertama di folder `progress_update`. Catatan berikutnya diberi nomor urut:
> `02_<tanggal>_progress_update.md`, `03_...`, dan seterusnya.

---

## 1. Ringkasan singkat

- **Aplikasi (backend, Android, Web): SUDAH JADI dan sudah diuji.**
- **Dokumentasi: sebagian besar sudah ada.** Yang belum dibuat adalah volume milik anggota lain (D04A, D05A, D08, D09A, D09W).
- Repo GitHub private untuk tim sudah dibuat dan berisi kode, dokumen, panduan, dan tutorial.

---

## 2. Yang dikerjakan hari ini (14 Nov 2026)

| # | Pekerjaan | Hasil |
|---|---|---|
| 1 | Membuat repositori GitHub private baru | https://github.com/SOL-glorious26/diponegoro-digital-bank (Android, Web, backend, dokumen dipisah per folder) |
| 2 | Mengamankan rahasia sebelum upload | `.env`, keystore rilis Android, `local.properties`, dan tanda tangan PM **tidak** ikut ke repo |
| 3 | Menulis panduan untuk anggota tim | `PANDUAN_PROYEK.md`: teknologi, cara kerja, apa itu API, fitur, progres, dengan bahasa sederhana |
| 4 | Mengundang anggota ke repo | Steven, Grace, Gracia, Nabila, Rafi (akses *write*, undangan menunggu diterima) |
| 5 | Membuat draf **D04W** Detailed Design Web v0.5 | 9 diagram UML/workflow, 7 pseudocode, perancangan software/hardware; diserahkan ke **Rafi** |
| 6 | Membuat draf **D07W** Developer Manual Web v0.5 | Setup, cara menjalankan, penjelasan kode, pengujian, troubleshooting; diserahkan ke **Steven** (bagian komponen UI: **Nabila**) |
| 7 | Membuat tutorial menjalankan & menguji Web | `TUTORIAL_MENJALANKAN_WEB.md` + versi Word **TUT-DDB-002-WEB v1.0** |
| 8 | Menguji ulang aplikasi Web | Tes API **38/38**, tes UI **20/20**, WB-11 dan WB-13–15 **lulus** |
| 9 | Mengganti logo aplikasi Web | Memakai logo resmi Diponegoro (lambang "D" kuda) untuk sidebar, halaman masuk, dan favicon |
| 10 | Merapikan README | Catatan path dipindah keluar dari blok perintah; tautan ke tutorial |
| 11 | Menyesuaikan PIC dokumen dengan DWP | D04W & D08 Rafi, D07W Steven (+Nabila), D04A Gracia, D05A Cecilia, D09A Grace, D09W Permata |

---

## 3. Status keseluruhan proyek

### ✅ Sudah selesai

| Bagian | Status |
|---|---|
| Backend | 8 modul (FR-01 – FR-29), 73 endpoint API, 47 test lulus, cakupan test 92,3% |
| Database | PostgreSQL berjalan (18 tabel + data demo); H2 untuk uji cepat |
| Android | Terhubung ke backend; uji sistem 27/28 lulus |
| Web | Semua halaman selesai, terhubung ke backend; uji Web 15/15 lulus |
| Lintas platform | Android ↔ Web 4/4 lulus |
| Bug | 11 ditemukan, 11 sudah diperbaiki |

### 📄 Dokumen

| Volume | Versi | PIC | Status |
|---|---|---|---|
| D00 Work Plan (DWP) | v1.3 | Salomo | Ada |
| D01 Project Document | v0.9.3 | Salomo | Menunggu review Steven |
| D02 Architectural Design | v0.9.4 | Salomo | Menunggu review Steven |
| D03 Functional Design | v0.9.2 | Salomo | Menunggu review Steven |
| D04B Detailed Design Backend & DB | v0.9.2 | Salomo | Menunggu review Steven |
| **D04W Detailed Design Web** | **v0.5** | **Rafi** | **Baru: draf awal, perlu ditelaah PIC** |
| D05W UI Map Web | v0.3 | Nabila | Ada |
| D06 System Testing | v0.9.5 | Steven | Ada |
| D07A Developer Manual Android | v0.9.2 | Salomo | Ada |
| D07B Developer Manual Backend | v0.9.2 | Salomo | Menunggu review Steven |
| **D07W Developer Manual Web** | **v0.5** | **Steven + Nabila** | **Baru: draf awal, perlu ditelaah PIC** |
| TUT Panduan Uji di Ponsel | v1.1 | Salomo | Ada |
| **TUT Panduan Menjalankan Web** | **v1.0** | Salomo | **Baru** |

---

## 4. Sisa pekerjaan

### Dokumen yang belum ada

| # | Pekerjaan | PIC |
|---|---|---|
| 1 | D04A Detailed Design Android | Gracia |
| 2 | D05A User Interface Map Android | Cecilia |
| 3 | D08 System Program Demo | Rafi |
| 4 | D09A User Manual Android | Grace |
| 5 | D09W User Manual Web | Permata |

### Tindak lanjut

| # | Pekerjaan | PIC | Catatan |
|---|---|---|---|
| 6 | Telaah & lengkapi draf D04W | Rafi | Cocokkan dengan kode `code_area/apps/web`, lalu naikkan versi |
| 7 | Telaah & lengkapi draf D07W | Steven, Nabila | Steven mencoba setup sesuai Bab II–III; Nabila cek Bab 5.3 |
| 8 | Review ke-2 D01, D02, D03, D04B, D07B | Steven | |
| 9 | Undang Permata, Cecilia, Cia ke repo GitHub | Salomo | Butuh username GitHub mereka |
| 10 | Catat D04W, D07W, dan tutorial Web di DWP | Salomo | |
| 11 | Uji di HP fisik: scan QRIS kamera (SA-08) dan NF-07 | Salomo | Satu-satunya uji D06 yang tertunda |
| 12 | Pasang Docker lalu uji gateway + rate limit (NF-04) | Salomo | Butuh hak admin + restart |
| 13 | Deploy Web + backend ke server publik (HTTPS) | Salomo | Opsional |
| 14 | Naikkan semua dokumen ke v1.0 (final + tanda tangan) | Salomo | Setelah semua review selesai |

### Catatan teknis

- PowerShell di laptop PM masih **Restricted**, sehingga `npm` dan `.\run-local.ps1` diblokir. Perbaiki sekali dengan
  `Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned`.
- Database H2 lokal sudah berisi transaksi dan akun hasil tes otomatis. Untuk kembali ke data demo, hapus folder `code_area\backend\data`.

---

## 5. Rencana berikutnya (usulan)

1. Bagikan link repo + `PANDUAN_PROYEK.md` + tutorial Web ke grup.
2. Minta Rafi, Steven, dan Nabila mulai menelaah D04W/D07W.
3. Buat draf awal D04A (paling cepat karena kode Android sudah lengkap), lalu D09A/D09W.
