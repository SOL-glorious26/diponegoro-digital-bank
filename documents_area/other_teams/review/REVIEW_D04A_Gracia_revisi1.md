# Review D04A revisi 1 (v0.3): untuk Gracia

Dokumen yang direview: `revisi 1/DDB_D04A_Detailed_Design_Android_v0.3_Revisi.docx` (v0.3, 16 Nov 2026)
Reviewer: Salomo (PM), 16 Nov 2026
Acuan: review sebelumnya (`REVIEW_D04A_Gracia.md`) dan kode `code_area/apps/android`

---

## 1. Kesimpulan singkat

**Lompatan besar, Gracia! 👏** Dari kerangka tanpa gambar sekarang jadi dokumen dengan **28 diagram** dan pseudocode yang ditulis langsung dari kode. Aku sudah cek otomatis **146 nama kelas dan fungsi** yang kamu sebut: **semuanya ada di kode**. Isinya akurat.

Yang tersisa sebagian besar soal **kerapian dokumen** (menghapus catatan internal, memperbarui versi acuan) dan **2 sequence diagram di lampiran** yang perlu digambar ulang.

| Bagian wajib template | Status |
|---|---|
| Blok kontrol dokumen | ✅ Lengkap |
| UML Diagram | ✅ Arsitektur, 4 class diagram utama, 4 sequence diagram, 2 state diagram. ⚠️ Lampiran A.7 dan A.8 perlu digambar ulang |
| Workflow Diagram | ✅ 3 activity diagram utama + 6 di lampiran |
| Algoritma Pseudocode | ✅ 8 pseudocode dari kode (sangat bagus) |
| Perancangan Software/Hardware | ✅ Lengkap, versi pustaka sudah terisi |

---

## 2. Yang sudah bagus 👍

- Tabel 2.1 sekarang berisi kelas nyata per paket, lengkap dengan PIC.
- Sequence pembayaran QRIS (Gambar 3.6) sangat jelas: lifeline lengkap, ada blok "Validasi sebelum PIN" dan "Pembayaran", dan endpoint-nya benar (`POST /qris/scan`, `POST /qris/pay`).
- Pseudocode 5.1 (dekode QRIS + CRC16) dan 5.7 (transfer antarbank + refund) detail dan sesuai kode.
- Keterbatasan biometrik ditulis jujur: masih simulasi, tanpa `BiometricPrompt`.
- Pengecekan kecil yang sudah aku lakukan, semuanya benar: `allowBackup=false`, optimasi R8 dimatikan, pesan "Pilih minimal satu kanal notifikasi", dialog "Autentikasi Biometrik (Simulasi)".

---

## 3. Yang masih perlu diperbaiki

### 🔴 A. Kamu memakai salinan repo lama (ZIP)

Di beberapa tempat kamu menulis bahwa ZIP hanya berisi D03 v0.9.2 dan D06 v0.9.5. Itu wajar karena ZIP-nya lama. **Versi terbaru sudah ada di repo GitHub tim** (`diponegoro-digital-bank`, branch `main`):
- `documents_area/D03_Functional_Design/DDB_D03_Functional_Design_v0.9.3.docx`
- `documents_area/D06_System_Testing/DDB_D06_System_Testing_v0.9.6.docx` (Android 28/28, ponsel fisik lulus)
- `documents_area/D02_Architectural_Design/DDB_D02_Architectural_Design_v0.9.5.docx`

Silakan `git pull` (atau unduh ulang ZIP dari GitHub). Setelah itu:
1. **Hapus semua kalimat tentang "ZIP"**: di bagian "Catatan Revisi v0.3", blok kontrol (Dokumen acuan), 3.3, 6.4, 6.5, 6.6, dan Referensi. Tulis saja versi terbarunya, misalnya *"Dokumen acuan: SAD-DDB-001 v0.9.5; FSD-DDB-001 v0.9.3; STD-DDB-001 v0.9.6"*.
2. Di 6.5 dan 6.6, tulis hasil uji sebagai fakta dari **D06 v0.9.6**, bukan "menurut review Salomo".

### 🔴 B. Hapus bagian "Catatan Revisi v0.3"

Bagian ini catatan kerja, bukan isi dokumen. Ringkasannya sudah ada di **Revision Control** (baris 0.3), jadi hapus seluruh bagian ini sebelum dikirim.

### 🔴 C. Gambar ulang sequence Transfer (A.7) dan Top-up (A.8)

Dua diagram ini sekarang berupa **graf panah**, bukan sequence diagram: tidak ada **lifeline** (garis putus-putus vertikal), dan urutan waktunya tidak terbaca. DWP meminta 4 sequence diagram: login + PIN, transfer, QRIS, dan top-up.

👉 Gambar ulang **dengan gaya yang sama seperti Gambar 3.6 (QRIS)**, yang sudah bagus. Urutan untuk Transfer:
`Pengguna → TransferScreen → TransferViewModel → RemoteTransferRepository → ApiClient/DdbApi → Backend`
1. `inquire()` → `POST /transfers/inquiry` → nama penerima
2. `quote()` → `POST /transfers/quote` → biaya + total
3. konfirmasi + PIN → `execute(key)` → `POST /transfers` (header `Idempotency-Key`) → status
4. blok *alt*: bila PROCESSING → `checkStatus()` → `POST /transfers/{txId}/check-status`

Kalau sudah dipindah ke Bab III (3.4.5), lebih rapi lagi.

### 🟡 D. Class diagram di Lampiran A.1–A.6

Sekarang masih berupa kotak nama kelas dan panah, tanpa atribut atau fungsi. Untuk ringkasan modul lain ini **boleh**, asalkan judulnya tetap *"ringkasan"* seperti sekarang. Satu koreksi kecil di A.3 (Transfer): panah **TransferViewModel → TransferScreen** terbalik. Yang benar, **Screen yang memakai ViewModel** (`TransferScreen → TransferViewModel`). Cek juga arah panah yang sama di A.1–A.6.

### 🟡 E. Rapikan halaman judul dan referensi

| Bagian | Sekarang | Seharusnya |
|---|---|---|
| Halaman judul | 14 November 2026 | Tanggal versi terbaru |
| Daftar isi | Field kosong | Klik kanan → *Update Field*, lalu simpan |
| Referensi | D01 v0.9.1; Panduan Uji Ponsel v1.0 | D01 v0.9.4; Panduan Uji Ponsel v1.1 |
| Referensi | D02 dan D03 tanpa versi | SAD-DDB-001 v0.9.5; FSD-DDB-001 v0.9.3; tambahkan STD-DDB-001 v0.9.6 |
| Pseudocode 5.7 | "…berhasil; TIMEOUT: Diproses**FUNCTION** checkStatus" | Beri baris baru sebelum `FUNCTION checkStatus` (kata-katanya menempel) |

### 🟢 F. Kode layar AND-xx

Benar bahwa D05A belum final. Cecilia sedang merevisi D05A dan **daftar layarnya akan berubah** (aplikasi punya 23 rute, sama dengan Tabel 3.1 kamu). Setelah D05A Cecilia selesai, tambahkan kolom **kode layar** di Tabel 3.1. Untuk sekarang, Tabel 3.1 berbasis rute sudah cukup.

---

## 4. Checklist sebelum kirim ke Cecilia (R1)

- [ ] Ambil repo terbaru; hapus semua kalimat tentang "ZIP"; versi acuan diperbarui (D02 v0.9.5, D03 v0.9.3, D06 v0.9.6)
- [ ] Hapus bagian "Catatan Revisi v0.3"
- [ ] Gambar ulang sequence Transfer dan Top-up dengan lifeline (gaya Gambar 3.6)
- [ ] Perbaiki arah panah ViewModel/Screen di Lampiran A
- [ ] Tanggal judul, daftar isi, referensi, dan baris pseudocode 5.7
- [ ] (Setelah D05A final) tambahkan kode layar di Tabel 3.1
- [ ] Naikkan versi (misalnya v0.4) dan catat di Revision Control

Dokumenmu sekarang salah satu yang paling teliti di tim. Tinggal dirapikan! 🙏
