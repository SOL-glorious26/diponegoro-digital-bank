# Progress Update #06: Minggu, 22 November 2026

Proyek: **Diponegoro Digital Bank**, Kelompok Jaran Goyang (Software Engineering UKSW)
Disusun oleh: Salomo (PM)
Tenggat dokumentasi: **24 November 2026, 23.59 WIB** (sisa 2 hari)

---

## 1. Perbaikan tampilan Android

| Bagian | Masalah | Perbaikan |
|---|---|---|
| Layar PIN setelah login | Saat HP miring, sebagian keypad dan "Lupa PIN?" tidak tampil dan tidak bisa digulir | Posisi miring: judul di kiri, keypad di kanan; layar dapat digulir bila tidak muat. Posisi tegak tetap |
| Label status transaksi | Di mode gelap kontras hanya 2,6–3,8:1 | Warna khusus mode gelap (hitungan 6,2–8,0:1); "Diproses" mode terang digelapkan |
| Layar Kartu | Judul masih "Virtual Card" dan tombol "← Kembali ke Home" | Judul "Kartu", panah kembali standar, teks "kartu virtual" |

Logika, backend, dan Web tidak diubah (di ViewModel hanya satu teks pesan kartu).

## 2. Pengujian

- Build APK demo dan online berhasil.
- Test otomatis: demo **63 lulus + 1 dilewati** (64), online **57 lulus + 1 dilewati** (58). Angka tidak berubah.
- Uji sistem di emulator Pixel 8 (Android 17), varian online + backend H2: **25 dari 25 kasus di skrip lulus**.
  Dijalankan bertahap pada APK yang sama, bukan dalam satu putaran.
- Rotasi di emulator: 2 digit PIN tetap tersimpan saat HP diputar, dan login selesai dari posisi miring.

Putaran pertama ada 9 kegagalan, semuanya dari skrip `sa-android.mjs`, lalu lulus setelah skrip diperbaiki
(tidak ada pemeriksaan yang dilonggarkan):

| Kasus | Sebab | Perbaikan skrip |
|---|---|---|
| SA-21, SA-22 (dan SA-15, SA-02 yang memakai akunnya) | Tombol "Daftar" sudah bernama "Buka rekening" | Nama tombol disesuaikan |
| SA-14, SA-27, SA-28, SA-16, SA-17 | Sesi masih milik pengguna baru SA-13 | SA-14 login ulang sebagai demo |

SA-23 lulus di putaran pertama, gagal di putaran kedua karena sesi masih milik pengguna yang PIN-nya diblokir SA-02.
Lulus setelah skrip login ulang sebagai demo.

**Lanjutan (sore):** putaran SA penuh dengan skrip final **23/23** (SA-18 dan SA-20 dijalankan terpisah, lulus);
**LP-01–LP-04 lulus** dua putaran berturut-turut. SA-04 tercakup LP-01 dan SA-19 tercakup LP-04, jadi **27 dari 28** kasus
lulus pada build baru. Pemeriksaan LP-04 di `lp-cross.mjs` sebelumnya selalu bernilai lulus, sekarang mewajibkan pesan
"Sesi Anda telah berakhir". Hasil dicatat di **D06 v0.9.8** (`patch_doc6_098.py`); bukti di `pengujian/bukti/2026-11-22/`.

**Belum dijalankan pada build baru:** SA-08 (kamera), berbagi bukti (SA-04), dan uji lain di HP fisik.
Label status mode gelap dicek lewat gambar komponen, belum pada transaksi gagal di HP.

## 3. Sisa pekerjaan

| # | Pekerjaan | PIC |
|---|---|---|
| 1 | Uji HP fisik (Realme) pada build ini: SA-08, berbagi bukti, notifikasi push | Salomo |
| 2 | Samakan D01 dan DWP (masih mengacu D06 v0.9.7); review D06 v0.9.8 | Salomo / Steven |
| 3 | Ambil ulang tangkapan layar D05A, D04A, D09A, presentasi (termasuk layar Kartu yang baru) | Cecilia, Gracia, Grace, Salomo |
| 4 | Semua dokumen ke v1.0 + tanda tangan | Salomo |
