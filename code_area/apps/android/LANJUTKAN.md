# LANJUTKAN — Catatan serah-terima sesi

Terakhir diperbarui: 10 November 2026 (sesi ke-2, malam)
Lokasi project: `apps/android` (dipindah dari `submit_mobile/Mobile/Modul-salomo` pada 12 Nov 2026)

> **Cara pakai:** di sesi Claude Code baru, ketik
> `Baca LANJUTKAN.md dan NOTES.md di folder apps/android, lalu lanjutkan.`
> Path lengkapnya:
> `C:\salomo_ADMIN\profesional_life\projects\submit_progress_digital_banking_project\apps\android`
>
> `NOTES.md` = detail teknis lengkap (arsitektur, modul, FR, riwayat masalah).
> File ini = ringkasan status terakhir + apa yang harus dikerjakan berikutnya.

---

## 1. Status saat ini

| Hal | Status |
|---|---|
| Build APK (`gradlew :app:assembleDebug`) | ✅ Berhasil |
| Semua test (`gradlew :app:testDebugUnitTest`) | ✅ **54 lulus, 0 gagal** (±16 detik) |
| `AppTourTest` (tur UI 24 layar) | ✅ Lulus |
| Uji di emulator `Pixel_8` | ✅ **Selesai**: 5 alur utama lulus, 3 bug ditemukan & diperbaiki |
| README diperbarui | ✅ Selesai (akun demo, data uji, batas, skenario semua modul) |
| Uji manual di HP sungguhan | ✅ **Selesai** 14 Nov 2026 (Realme RMX3867, Android 16): kamera QRIS, notifikasi, berbagi ke WhatsApp lulus |
| Nilai usulan dikonfirmasi dosen/tim | ⏳ Belum (sudah ditandai sebagai usulan di README) |

Akun demo: `demo` / `Demo@1234`, PIN `123456`.

## 2. Yang dikerjakan pada sesi ke-2 (10 November 2026)

Emulator berhasil jalan (WHPX aktif: `emulator -accel-check` → "WHPX is installed and usable").
APK dipasang di `Pixel_8` dan diuji lewat `adb` (input + `uiautomator dump` + `screencap`).

**Alur yang diuji dan lulus:**
- Transfer sesama DDB → PIN salah (sisa percobaan tampil) → PIN benar → bukti → bagikan PNG & PDF.
- Transfer antarbank BRI `9999999999` (ditolak, dana kembali) dan Mandiri `8888888888`
  (timeout → Diproses, dana tertahan).
- QRIS statis (bayar Rp25.000), QRIS dinamis (nominal terkunci), tombol galeri (photo picker terbuka),
  Terima QRIS + simulasi dana masuk.
- Top-up PLN: ID `…0000` tidak ditemukan, ID valid → struk token.
- Kantong: buat (kunci 1 bulan), isi, tarik dini (konfirmasi + PIN).
- Profil: ubah nomor telepon → OTP salah → OTP benar.
- Riwayat mencatat semua transaksi; 8 notifikasi push dari aplikasi terdeteksi di `dumpsys notification`.

**Bug yang ditemukan di emulator dan sudah diperbaiki:**
1. **Back di Beranda kembali ke layar "Masukkan PIN Transaksi"** (seharusnya keluar aplikasi).
   Penyebab: `popUpTo(graph.findStartDestination().id)` tidak berbuat apa-apa jika Login sudah tidak
   ada di back stack. Perbaikan: `popUpTo(navController.graph.id) { inclusive = true }` di
   `AuthNavGraph.kt` (2 tempat).
2. **Tab "Beranda" di bottom bar membuka tab terakhir (mis. QRIS), bukan Home.** Penyebab:
   `navigate(HOME) { popUpTo(HOME){saveState}; restoreState = true }` memulihkan state yang disimpan
   atas nama HOME. Perbaikan: `switchTab(HOME)` cukup `popBackStack(HOME, inclusive = false)`.
3. **QRIS: nominal di luar batas (mis. Rp500) tetap membuka dialog PIN.** Perbaikan: validasi batas
   QRIS dan saldo di `QrisPayViewModel.requestPin()` sebelum dialog PIN.

**Catatan minor, belum diubah:**
- Layar Virtual Card belum punya top bar seperti layar lain.
- Tombol "Analitik" di Riwayat terlipat 2 baris di layar 320dp (di Pixel 8 normal).
- Struk top-up menulis "Dari 1400123456", struk transfer "DDB • 1400123456" (tidak konsisten).
- Simulasi dana masuk QRIS tidak menampilkan pesan sukses (saldo tetap bertambah).

## 3. Tips emulator (untuk sesi berikutnya)

- Nyalakan: `& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd Pixel_8 -no-snapshot-load -no-audio -no-boot-anim -gpu swiftshader_indirect`
- adb: `$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe`
- **Keyboard layar emulator sudah dinonaktifkan** (`adb shell ime disable …LatinIME`) supaya tidak
  menutupi layar saat uji otomatis; `adb shell input text` tetap berfungsi. Untuk mengaktifkan lagi:
  `adb shell ime enable com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME`
- Jangan menutup keyboard dengan `keyevent 4` (BACK) atau `111` (ESC): keduanya ikut menavigasi mundur.

## 4. Pekerjaan tersisa

- **Pengguna:** uji di HP sungguhan — pindai QRIS dengan kamera, notifikasi push, berbagi file ke
  WhatsApp/Drive, kesan umum.
- **Claude (opsional):**
  - Rapikan catatan minor di bagian 2.
  - Tulis test instrumentasi `androidTest` untuk alur dialog (transfer, QRIS, PLN, kantong, OTP).
  - Tambah unit test regresi untuk validasi QRIS sebelum PIN dan navigasi tab Beranda.

## 5. Pertanyaan yang masih menunggu jawaban pengguna

1. **Nilai usulan** (batas transfer Rp25 juta, biaya antarbank Rp2.500, batas QRIS, limit Virtual
   Card, saldo awal Rp5 juta, timeout 5 menit): sudah dikonfirmasi dosen/tim?
2. **Cakupan:** apakah wajib penyimpanan permanen (Room), integrasi REST ke backend Spring Boot,
   biometrik sungguhan (`BiometricPrompt`), atau FR-26 (ekspor riwayat, menurut SRS untuk Web)?
3. **Format pengumpulan & tenggat:** cukup APK + source code, atau perlu laporan juga?

## 6. Perintah penting

```bash
# Git Bash
cd "/c/salomo_ADMIN/profesional_life/projects/submit_progress_digital_banking_project/apps/android"
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :app:testDebugUnitTest     # semua test (screenshot ke app/build/screenshots/)
./gradlew :app:assembleDebug         # APK ke app/build/outputs/apk/debug/app-debug.apk
```

Jika `AppTourTest` gagal, lihat `app/build/screenshots/GAGAL_*.png`.
