# Review D04A revisi 2 (v0.4): untuk Gracia

Dokumen yang direview: `revisi 2/DDB_D04A_Detailed_Design_Android_v0.4_Perbaikan_Review.docx` (v0.4, 16 Nov 2026)
Reviewer: Salomo (PM), 16 Nov 2026
Acuan: review revisi 1 (`REVIEW_D04A_Gracia_revisi1.md`)

---

## 1. Kesimpulan singkat

Terima kasih, Gracia. Ada perbaikan yang bagus: **daftar isi sudah terisi**, tanggal sudah diperbarui, ada **tabel endpoint A.5** yang rapi (sudah aku cek, semua path benar), dan ada **activity diagram notifikasi** baru (A.15).

Tapi **sebagian besar catatan revisi 1 belum dikerjakan**. Penyebab utamanya: kamu masih memakai **ZIP lama**, sehingga dokumen sekarang justru berisi lebih banyak kalimat tentang "ZIP tidak memuat…".

| Catatan revisi 1 | Status v0.4 |
|---|---|
| Ambil repo terbaru; hapus semua kalimat tentang ZIP | ❌ Belum. Kalimat tentang ZIP malah bertambah |
| Hapus bagian "Catatan Revisi" | ❌ Masih ada (judulnya bahkan masih "Catatan Revisi v0.3") |
| Gambar ulang sequence Transfer (A.7) dan Top-up (A.8) | ❌ Gambarnya **sama persis** dengan v0.3. Tabel A.5 bagus, tapi tidak menggantikan diagram |
| Perbaiki arah panah ViewModel/Screen di Lampiran A | ❌ Gambar A.1–A.6 sama persis dengan v0.3 |
| Tanggal judul, daftar isi, Panduan Uji Ponsel v1.1 | ✅ Selesai |
| Pseudocode 5.7 ("Diproses**FUNCTION**") | ❌ Masih menempel |

---

## 2. Cara mendapatkan dokumen terbaru (penting) 📥

Akun GitHub kamu (**graciacantik**) **sudah punya akses** ke repo tim `SOL-glorious26/diponegoro-digital-bank`. Versi terbaru sudah ada di sana sejak 16 Nov sore. ZIP yang kamu pakai diunduh sebelum itu.

Langkahnya: buka repo di GitHub → tombol hijau **Code** → **Download ZIP** (atau `git pull` kalau kamu memakai git). Di ZIP baru kamu akan menemukan:
- `D01_Project_Document/DDB_D01_Project_Document_v0.9.4.docx`
- `D02_Architectural_Design/DDB_D02_Architectural_Design_v0.9.5.docx`
- `D03_Functional_Design/DDB_D03_Functional_Design_v0.9.3.docx`
- `D06_System_Testing/DDB_D06_System_Testing_v0.9.6.docx` (Android 28/28, demo 62 + 1 dilewati, online 58/58)
- folder `code_area/pengujian/bukti/` (tangkapan layar)

**Semua angka di review sudah aku cocokkan sendiri ke dokumen dan kode**, jadi kamu boleh menuliskannya sebagai fakta. Tidak perlu lagi kalimat "menurut review" atau "tidak ada di ZIP".

---

## 3. Yang harus dikerjakan

### 🔴 A. Bersihkan semua kalimat tentang ZIP

Hapus atau ganti di bagian berikut:

| Bagian | Ganti menjadi |
|---|---|
| "Catatan Revisi v0.3" (seluruh bagian) | **Hapus.** Ringkasannya sudah ada di Revision Control |
| Kontrol Dokumen → Dokumen acuan | `SRS-DDB-001 v0.1; SAD-DDB-001 v0.9.5; FSD-DDB-001 v0.9.3; SDD-DDB-001-BE v0.9.2; STD-DDB-001 v0.9.6; UIM-DDB-001-AND v0.2; kode apps/android` |
| Kontrol Dokumen → Status | `Draf 0.5 – menunggu pre-review Cecilia (R1) dan review teknis Salomo (R2)` |
| 2.1 paragraf D01 | `Aplikasi Android terdiri atas 110 berkas Kotlin dengan ukuran 9,227 KSLOC (D01 v0.9.4).` |
| 6.5 | Tulis langsung: *"Berdasarkan D06 v0.9.6: varian demo 62 lulus dan 1 dilewati, varian online 58/58, uji sistem Android 28/28 termasuk pindai QRIS dengan kamera di ponsel fisik."* |
| 6.6 baris terakhir | *"Uji pada ponsel fisik Realme RMX3867 (Android 16) lulus pada 14 Nov 2026 (D06 v0.9.6)."* |
| Referensi | D01 v0.9.4; D02 v0.9.5; D03 v0.9.3; D06 v0.9.6; Panduan Uji Ponsel v1.1. Hapus paragraf "Catatan konsistensi dokumen" |
| A.4 "Status validasi" | Hapus kalimat "D05A v0.1 tidak tersedia dalam ZIP" |

### 🔴 B. Isi kode layar AND-xx di Tabel 3.1

D05A milik Cecilia (v0.2) sudah menetapkan kode layar. Kamu tidak perlu menunggu file-nya, karena kodenya sama persis dengan rute di Tabel 3.1:

| Kode | Rute | Kode | Rute |
|---|---|---|---|
| AND-01 | auth/login | AND-13 | qris/receive |
| AND-02 | auth/register | AND-14 | topup |
| AND-03 | auth/otp | AND-15 | virtualcard/home |
| AND-04 | auth/ekyc | AND-16 | history |
| AND-05 | auth/create_pin | AND-17 | history/detail/{txId} |
| AND-06 | auth/enter_pin | AND-18 | history/analytics |
| AND-07 | auth/recovery | AND-19 | profile |
| AND-08 | home | AND-20 | profile/security |
| AND-09 | account/chart | AND-21 | profile/sessions |
| AND-10 | account/pockets | AND-22 | profile/notification-settings |
| AND-11 | transfer | AND-23 | notifications |
| AND-12 | qris/pay | | |

Ubah Tabel 3.1 menjadi tiga kolom: **Kode | Rute | Kelompok**. Hapus keterangan "kode layar AND-xx tidak dicantumkan".

### 🔴 C. Gambar ulang sequence Transfer (A.7) dan Top-up (A.8)

Ini masih **wajib**, karena DWP meminta 4 sequence diagram, dan A.7/A.8 sekarang bukan sequence diagram (tidak ada lifeline). Tabel A.5 sudah bagus, tapi tetap perlu digambar.

Pakai **gaya yang sama dengan Gambar 3.6 (QRIS)**:
- Kolom/lifeline: `Pengguna | TransferScreen | TransferViewModel | RemoteTransferRepository | ApiClient/DdbApi | Backend`
- Panah dari atas ke bawah sesuai urutan di Tabel A.5 kamu: banks → inquiry → quote → (konfirmasi + PIN) → `POST /transfers` + Idempotency-Key → blok **alt** "bila PROCESSING" → check-status.
- Top-up sama: inquiry (sesuai jenis) → quote → PIN → `POST /topup/orders` + Idempotency-Key.

Setelah digambar ulang, pindahkan keduanya ke **Bab III 3.4.5** supaya 4 sequence wajib ada di bab utama.

### 🟡 D. Arah panah di class diagram Lampiran A

Di A.1–A.6 panah masih **ViewModel → Screen**. Yang benar **Screen → ViewModel** (layar yang memakai ViewModel). Contoh A.3: `TransferScreen → TransferViewModel → TransferRepository`.

### 🟡 E. Kecil

- Pseudocode 5.7: beri baris baru sebelum `FUNCTION checkStatus` (sekarang tertulis "…TIMEOUT: Diproses**FUNCTION** checkStatus").
- Penomoran lampiran: judul bagian "A.9 Activity Diagram Pengaturan…" bertabrakan dengan **Gambar A.9** (Authentication). Ubah judul bagiannya menjadi **A.6**, dan pastikan urutannya A.1, A.2, A.3, A.4, A.5, A.6.

---

## 4. Checklist sebelum kirim ke Cecilia (R1)

- [ ] Unduh ZIP baru dari GitHub
- [ ] Hapus "Catatan Revisi" dan semua kalimat tentang ZIP; versi acuan diperbarui (tabel 3A)
- [ ] Tabel 3.1 diberi kode AND-01 s.d. AND-23
- [ ] Sequence Transfer dan Top-up digambar ulang dengan lifeline, lalu dipindah ke Bab III
- [ ] Arah panah Screen → ViewModel di Lampiran A
- [ ] Pseudocode 5.7 dan penomoran lampiran dirapikan
- [ ] Versi dinaikkan ke v0.5 dan dicatat di Revision Control

Isi teknismu sudah sangat kuat. Yang tersisa tinggal kerapian. 🙏
