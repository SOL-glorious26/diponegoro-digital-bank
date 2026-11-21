# Progress Update #05: Sabtu, 21 November 2026

Proyek: **Diponegoro Digital Bank**, Kelompok Jaran Goyang (Software Engineering UKSW)
Disusun oleh: Salomo (PM)
Tenggat dokumentasi: **24 November 2026, 23.59 WIB** (sisa 3 hari)

---

## 1. Ringkasan singkat

- **Tampilan aplikasi Android diperbarui** (arah desain "quiet institutional"). Hanya lapisan UI yang berubah;
  ViewModel, repository, integrasi API, backend, dan Web **tidak** diubah.
- **D06 System Testing naik ke v0.9.7** untuk mencatat regresi setelah pembaruan tampilan.
- Catatan belajar pribadi PM ditambahkan: `documents_area/CATATAN_Belajar_Software_Engineering/` (bukan deliverable).

---

## 2. Pembaruan tampilan Android

| Bagian | Perubahan |
|---|---|
| Tema | Midnight Navy `#0B1D33`, Diponegoro Blue `#0B3D91`, Muted Gold `#B69A62`; mode terang dan gelap mengikuti pengaturan HP |
| Font | Plus Jakarta Sans 400/500/600 (lisensi OFL di `app/licenses/`) |
| Ikon & nama | Ikon launcher dari logo asli Diponegoro (bukan robot Android lagi); nama di launcher "Diponegoro" |
| Login | Logo, garis emas, tombol **Masuk** dan **Masuk dengan biometrik**; teks akun demo hanya di varian demo |
| PIN setelah login | Enam titik + keypad angka; PIN terkirim otomatis pada digit keenam (tidak ada tombol "Konfirmasi PIN") |
| Beranda | Saldo sebagai fokus, aksi cepat ikon bergaris (menu "Virtual Card" kini bernama **Kartu**), transaksi berupa daftar |
| Bottom navigation | Beranda, Riwayat, **QRIS** (tombol bulat di tengah), **Kantong** (tab baru), Profil |
| Layar lain | Panel bergaris tipis tanpa bayangan, kolom input dan tombol seragam (sudut 10 dp) |

Mockup dan design system: kanvas desain "DDB Quiet Luxury" (tautan dari PM).

### Pengujian

- Test otomatis Android: varian demo **63/64 lulus**, varian online **57/58 lulus** (masing-masing 1 dilewati:
  LiveBackendTest hanya berjalan dengan backend aktif). `DarkModeShotsTest` baru untuk mode gelap.
- Skrip uji adb (`pengujian/lib/android-flows.mjs`, `android/sa-android.mjs`, `lintas-platform/lp-cross.mjs`)
  disesuaikan: tombol Masuk, label "Username, email, atau nomor telepon", PIN lewat keypad (`keypadPin()`), menu Kartu.
- **Belum**: eksekusi ulang SA-01–SA-28 dan LP-01–LP-04 pada build baru (emulator + HP fisik).

---

## 3. Dampak ke dokumen lain

| Dokumen | Yang perlu disesuaikan | PIC |
|---|---|---|
| D06 v0.9.7 | Sudah diperbarui. Setelah SA/LP dijalankan ulang, perbarui status Tabel 5.3 dan 5.5 | Salomo / Steven |
| D01 v0.9.5, D07A v0.9.3, DWP v1.4 | **Sudah** disamakan (21 Nov sore): angka test Android 63/64 dan 57/58; D07A juga memuat tema, layar, dan inventaris 112 berkas yang baru; DWP memuat status semua volume per 21 Nov | Salomo |
| D05A UI Map Android | Tangkapan layar Login, PIN, Beranda, bottom navigation harus diambil ulang; nama menu "Kartu" | Cecilia |
| D04A Detailed Design Android | Bila memuat tangkapan layar atau deskripsi layar PIN (kolom teks + tombol), sesuaikan dengan keypad | Gracia |
| D09A User Manual Android | Pakai tampilan baru sejak awal | Grace |
| Presentasi | Slide 16–20 memakai tangkapan layar lama | Salomo |

Anggota harus **unduh ulang repo** sebelum mengambil tangkapan layar baru.

---

## 4. Sisa pekerjaan

| # | Pekerjaan | PIC |
|---|---|---|
| 1 | Uji tampilan baru di HP fisik (Realme), lalu jalankan ulang SA dan LP | Salomo |
| 2 | ~~Samakan angka uji Android di D01, D07A, DWP~~ (selesai 21 Nov) | Salomo |
| 3 | Ambil ulang tangkapan layar untuk D05A, D04A, D09A, presentasi | Cecilia, Gracia, Grace, Salomo |
| 4 | Semua dokumen ke v1.0 + tanda tangan | Salomo |
