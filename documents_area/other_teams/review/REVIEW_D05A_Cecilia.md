# Review D05A User Interface Map Android: untuk Cecilia

Dokumen yang direview: `DDB-001-AND_User_Interface_Map_Android.docx` (v0.1, 16 Nov 2026)
Reviewer: Salomo (PM), 16 Nov 2026
Acuan: template dosen Dokumen 5, DWP-DDB-001 v1.3 (tugas D05A-WP1 s.d. WP4, Definition of Done), D03 v0.9.3, dan kode `code_area/apps/android`

---

## 1. Kesimpulan singkat

**Status: perlu revisi besar sebelum R1.** Strukturnya rapi dan tujuannya sudah tepat. Masalah utamanya: **daftar layar diambil dari "Functional Design v0.1" yang lama** (kode S-01 s.d. S-15), **bukan dari aplikasi Android yang sudah jadi**. Akibatnya jumlah layar dan alurnya berbeda dengan aplikasi sebenarnya.

DWP tugas D05A-WP1-T02 meminta: *"Menginventaris seluruh layar Android **dari build atau prototipe**"*. Aplikasi Android sudah selesai dan bisa dijalankan, jadi peta UI harus mengikuti aplikasi itu.

Tenggat akhir seluruh dokumen: **24 Nov 2026, 23.59 WIB**. Usahakan selesai beberapa hari sebelumnya supaya masih ada waktu review. Pre-review R1 dilakukan **Gracia**, review R2 oleh **Salomo**.

| Bagian wajib template | Ada? | Kondisi |
|---|---|---|
| Blok kontrol dokumen | ⚠️ | "Last updated by" tidak ada; ada baris v1.0 untuk tanggal yang belum terjadi |
| Daftar isi | ❌ | **Tidak ada**, padahal wajib |
| User Interface Map | ⚠️ | Ada, tapi daftar layar tidak sesuai aplikasi; belum ada diagram; belum ada tangkapan layar |

---

## 2. Yang sudah bagus 👍

- Susunan dokumen sudah lengkap: legenda, inventaris, peta global, peta per modul (8 modul), kartu layar, aturan navigasi, dan traceability ke FR.
- Jujur soal tangkapan layar: kamu tidak memalsukan gambar dan menandai tempatnya. Ini sesuai aturan DWP.
- Aturan di Bab 7 (konfirmasi sebelum PIN, maksimal 5 ketukan, pesan kesalahan umum) sudah sesuai SRS.

---

## 3. Temuan utama

### 🔴 A. Daftar layar tidak sama dengan aplikasi

Aplikasi Android sebenarnya punya **23 layar/rute** (dari `AuthRoutes.kt` dan `core/navigation/AppRoutes.kt`), sedangkan dokumen hanya punya 15. Beberapa layar penting tidak ada di dokumen. Usulan daftar baru (kode boleh kamu ubah, asal unik dan konsisten):

| Usulan kode | Layar di aplikasi | Rute di kode | Modul | FR |
|---|---|---|---|---|
| AND-01 | Masuk (Login) | `auth/login` | Authentication | FR-03 |
| AND-02 | Daftar | `auth/register` | Authentication | FR-01 |
| AND-03 | Verifikasi OTP | `auth/otp` | Authentication | FR-01 |
| AND-04 | e-KYC (NIK + selfie simulasi) | `auth/ekyc` | Authentication | FR-02 |
| AND-05 | Buat PIN | `auth/create_pin` | Authentication | FR-04 |
| AND-06 | Masukkan PIN (setiap login) | `auth/enter_pin` | Authentication | FR-04 |
| AND-07 | Pemulihan password/PIN | `auth/recovery` | Authentication | FR-05 |
| AND-08 | Beranda (Dashboard) | `home` | Account | FR-06, FR-25 |
| AND-09 | Grafik saldo | `account/chart` | Account | FR-07 |
| AND-10 | Kantong | `account/pockets` | Account | FR-08, FR-09 |
| AND-11 | Transfer (penerima → nominal → konfirmasi → PIN → bukti, dalam satu layar bertahap) | `transfer` | Transfer | FR-10 – FR-14 |
| AND-12 | Bayar QRIS (pindai kamera/galeri) | `qris/pay` | Payment | FR-15 |
| AND-13 | Terima QRIS | `qris/receive` | Payment | FR-16, FR-17 |
| AND-14 | Top-up & tagihan | `topup` | Top-up | FR-18 – FR-20 |
| AND-15 | Virtual Card | `virtualcard/home` | Virtual Card | FR-21, FR-22 |
| AND-16 | Riwayat | `history` | History | FR-23 |
| AND-17 | Detail transaksi | `history/detail/{txId}` | History | FR-24 |
| AND-18 | Analitik bulanan | `history/analytics` | History | FR-25 |
| AND-19 | Profil | `profile` | Profile | FR-27 |
| AND-20 | Keamanan | `profile/security` | Profile | FR-28 |
| AND-21 | Sesi & perangkat | `profile/sessions` | Authentication | FR-05 |
| AND-22 | Pengaturan notifikasi | `profile/notification-settings` | Profile | FR-28 |
| AND-23 | Kotak notifikasi | `notifications` | Notification | FR-29 |

Catatan: **bottom bar** aplikasi berisi 4 tab: **Beranda, Riwayat, QRIS, Profil**. Ini perlu digambar di peta global.

> Format kode di DWP adalah **AND-xx** (misalnya AND-01). Kalau kamu ganti kode, kabari Gracia (D04A), Steven (D06), Rafi (D08), dan Grace (D09A), karena dokumen mereka harus memakai kode yang sama.

### 🔴 B. Alur navigasi ada yang salah

| Di dokumen | Di aplikasi sebenarnya |
|---|---|
| e-KYC → **Login** [KYC disetujui] | Setelah OTP valid pengguna **langsung masuk**, lalu e-KYC → **Buat PIN** → Beranda |
| Login → PIN hanya "akun belum memiliki PIN" | **Setiap login** melewati **Masukkan PIN**. Kalau PIN belum ada → Buat PIN. Kalau e-KYC belum disetujui → kembali ke e-KYC |
| Ada "Splash / App Start" | Tidak ada splash. Aplikasi langsung membuka **Login** |
| "Konfirmasi & PIN" layar terpisah untuk Transfer, QRIS, dan Top-up | Konfirmasi dan PIN adalah **langkah/dialog di dalam layar masing-masing**, bukan satu layar bersama |
| Layar e-KYC: "Unggah KTP dan swafoto" | Isian sebenarnya: **NIK KTP 16 digit + selfie (simulasi)** |

### 🔴 C. FR-26 (ekspor riwayat) tercantum di layar Android

Di Bab 3 dan Bab 8, AND-S14 diberi FR-26. Padahal FR-26 **hanya untuk Web**, dan kamu sendiri menulisnya di Bab 5. Hapus FR-26 dari layar Android.

### 🔴 D. Bagian wajib yang belum ada

1. **Daftar isi** belum ada. Ini wajib menurut template dosen.
2. **"Last updated by"** belum ada di blok kontrol.
3. **Baris versi 1.0 bertanggal 24 Nov** di riwayat revisi harus dihapus. Riwayat revisi hanya mencatat versi yang sudah terjadi (aturan "kejujuran isi" DWP).
4. **Peta navigasi berupa diagram.** Sekarang masih tabel. DWP meminta "diagram navigasi global" dan "8 peta modul". Boleh pakai draw.io.

### 🟡 E. Kartu layar masih isian umum

Semua 15 kartu berisi kalimat yang sama ("Konten/komponen harus mengikuti fungsi layar…"). Ganti dengan **elemen nyata tiap layar**, misalnya untuk Login: kolom username/email/telepon, kolom password, tombol Masuk, tombol biometrik, tautan Daftar, tautan Lupa password.

### 🟡 F. Acuan dokumen usang

| Tertulis | Seharusnya |
|---|---|
| DWP-DDB-001 v1.0 | DWP-DDB-001 v1.3 |
| Functional Design DDB v0.1 | **FSD-DDB-001 v0.9.3 (D03)**. D03 resmi tidak memakai kode S-xx |
| Status "berbasis Functional Design v0.1" | "Draf v0.x, inventaris dari aplikasi Android (build 16 Nov 2026)" |

---

## 4. Bahan yang sudah tersedia (bisa langsung dipakai) 📸

Ada **tangkapan layar asli dari aplikasi** hasil uji sistem 11 Nov di:
`code_area/pengujian/bukti/2026-11-11/android-sa/` (dan beberapa di folder lain di bawah `bukti/2026-11-11/`).

Contoh: `SA01-beranda.png`, `SA03-saldo-tersembunyi.png`, `SA09-qris-statis.png`, `SA10-pulsa.png`, `SA12-1-detail-kantong.png`, `SA13-1-kartu-baru.png`, `SA14-2-detail.png`, `SA14-3-analitik.png`, `SA15-profil.png`, `SA16-notifikasi.png`, `SA21-1-ekyc-disetujui.png`, `SA27-grafik-saldo.png`, `SA28-pengaturan.png`.

Gambar ini boleh dipakai dulu untuk kartu layar. Untuk versi final, ambil ulang dari **build baseline 18 Nov** bersama Grace. Caranya ada di panduan `TUT Panduan Uji Aplikasi di Ponsel v1.1`, atau minta APK ke Salomo.

---

## 5. Checklist sebelum kirim ke Gracia (R1)

- [ ] Inventaris layar diganti sesuai aplikasi (23 layar, tabel di atas)
- [ ] Alur navigasi diperbaiki (OTP → e-KYC → Buat PIN; setiap login lewat Masukkan PIN; tanpa splash)
- [ ] FR-26 dihapus dari layar Android
- [ ] Diagram navigasi global + 8 diagram per modul (draw.io)
- [ ] Kartu layar diisi elemen nyata + tangkapan layar
- [ ] Daftar isi ditambahkan; "Last updated by" diisi; baris v1.0 dihapus
- [ ] Acuan diganti ke DWP v1.3 dan D03 v0.9.3
- [ ] Kode layar final dikabarkan ke Gracia, Steven, Rafi, dan Grace

Semangat, Cecilia! Kalau bingung soal alur aplikasi, coba jalankan aplikasinya atau tanya Salomo. 🙏
