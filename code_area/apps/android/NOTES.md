# NOTES — Integrasi Aplikasi Android Diponegoro Digital Bank

Tanggal: 10 November 2026
Lokasi project: `apps/android` (dipindah dari `submit_mobile/Mobile/Modul-salomo` pada 12 Nov 2026)

## Tujuan

Menyelesaikan aplikasi Android sesuai **SRS-DDB-001 v0.1** (FR-01 – FR-29) dengan menggabungkan
keempat modul tim (Salomo, Cecil, Cia, Grace) menjadi **satu aplikasi**. Teknologinya mengikuti
README tim: Kotlin, Jetpack Compose, MVVM, dan Repository Pattern.

Modul tim versi awal tidak bisa langsung digabung. Di bawah ini kondisi tiap modul dan apa yang
diambil darinya:

| Modul (PIC) | Kondisi awal | Yang dipakai ulang |
|---|---|---|
| Authentication, Virtual Card (Salomo) | Auth lengkap; Virtual Card baru ada UML-nya | Seluruh Auth; Virtual Card ditulis dari UML |
| Transfer, History & Dashboard (Cecil) | Login/saldo terpisah dari Auth, belum berupa project Gradle | Daftar rekening uji, alur transfer |
| Payment QRIS, Profile & Notification (Cia) | XML/Activity, PIN plaintext, pembayaran tidak memotong saldo | ZXing, alur profil + OTP |
| Account, Top-up (Grace) | **Tidak bisa dikompilasi** (fungsi & tabel yang dipanggil tidak ada); Account tidak sesuai SRS | Katalog top-up, validasi nomor |

Versi asli semua modul masih tersimpan di `jarang-goyang-mobile-app-fix.zip`.

## Yang sudah berhasil

### Arsitektur
- **`core/ledger`**: Financial Transaction Core in-memory.
  - Ledger double-entry (jumlah tiap jurnal wajib 0), posting atomik di bawah Mutex.
  - Kunci idempotensi supaya tidak ada debit ganda, jurnal append-only, dan refund ditulis
    sebagai baris pembalik (BR-03, BR-04, BR-05, NFR-07).
  - `DemoData` berisi 5 bulan riwayat untuk akun demo, plus nasabah lain:
    ANDI 1400000002, BUDI 1400000003, CITRA 1400000004.
- **`core/security/TransactionGuard`**: semua transaksi wajib e-KYC disetujui + PIN valid,
  memakai aturan blokir 3 kali salah dari modul Auth.
- **`core/notification/NotificationCenter`**: notifikasi transaksi masuk/keluar dikirim
  **asinkron** lewat kanal pilihan pengguna. Push dikirim sebagai notifikasi Android lokal;
  email hanya disimulasikan sebagai outbox.
- **`core/di`**: `CoreModule` dan `FeatureModule` (DI manual); diinisialisasi di `DigitalBankApplication`.

### Modul dan FR

| Modul | FR | Package |
|---|---|---|
| Authentication | FR-01 – FR-05 (timeout sesi dipindah ke `AuthViewModel`) | `authentication/` |
| Account | FR-06 – FR-09: saldo bisa disembunyikan, grafik saldo, kantong + locking & penarikan dini | `account/` |
| Transfer | FR-10 – FR-14 + UC-01 A1–A6, favorit, bukti PNG/PDF | `transfer/` |
| Payment QRIS | FR-15 – FR-17: codec EMVCo + CRC16, QR statis/dinamis, pindai kamera/galeri/QR demo | `payment/` |
| Top-up | FR-18 – FR-20: pulsa, paket data, token PLN, tagihan PDAM/BPJS/Internet, e-wallet | `topup/` |
| Virtual Card | FR-21 – FR-22 | `virtualcard/` |
| History & Dashboard | FR-23 – FR-25: filter gabungan, paginasi, detail + jurnal, grafik batang bulanan | `history/`, `home/` |
| Profile & Notification | FR-27 – FR-29: profil + OTP, foto, password/PIN, biometrik, pengaturan & kotak notifikasi | `profile/` |

FR-26 (ekspor riwayat) tidak dibuat di mobile karena SRS menetapkannya untuk platform **Web**.

### Lain-lain
- Navigasi lengkap ada di `authentication/presentation/navigation/AuthNavGraph.kt`, dengan
  bottom bar Beranda / Riwayat / QRIS / Profil.
- Tema brand tetap (biru tua + emas). Warna grafik memakai palet tervalidasi aman CVD.
- Manifest:
  - `allowBackup=false`.
  - FileProvider untuk berbagi bukti transaksi.
  - Izin `POST_NOTIFICATIONS` dan `CAMERA`.

### Build & test
- `gradlew :app:assembleDebug` → **berhasil** (`app/build/outputs/apk/debug/app-debug.apk`).
- **49 unit test lulus**:
  - `LedgerTest`
  - `TransferRepositoryTest`
  - `PaymentRepositoryTest`
  - `TopUpRepositoryTest`
  - `AccountRepositoryTest`
  - `HistoryAndProfileTest`
  - `VirtualCardUseCasesTest`
  - `ExampleUnitTest`
- **`ReceiptAndDialogShotsTest` lulus (4 test)**: menghasilkan screenshot dialog PIN, bukti
  transfer, bukti transfer yang ditolak (dana dikembalikan), dan bukti token listrik.
- **`AppTourTest > screensTour` lulus** (10 November 2026): tur UI end-to-end menghasilkan 24
  screenshot (`app/build/screenshots/01–24`) dan semuanya sudah diperiksa secara visual.
- **Total: 54 test lulus, 0 gagal.** Seluruh `testDebugUnitTest` selesai dalam ±15 detik.

Akun demo: `demo` / `Demo@1234`, PIN `123456`.

## `AppTourTest`: riwayat perbaikan (sudah selesai)

Test UI end-to-end berbasis Robolectric (`app/src/test/.../ui/AppTourTest.kt`). Masalah terakhir
yang diselesaikan pada 10 November 2026:

1. **"Failed to perform text input" di layar Transfer kedua**: setelah `back()` lalu buka
   Transfer lagi, layar lama masih ada di komposisi karena transisi NavHost (700 ms) belum
   selesai, sehingga ada 2 field "Nomor rekening". Solusi: `click()`/`back()` memompa 1000 ms
   (`transitionMs`), dan `field()` memilih node terakhir (`onAllNodes(...).onLast()`).
   Pengisian teks memakai `SetText`.
2. **Test berjalan >10 menit dan macet di Riwayat ("Filter jenis & status")**: penyebabnya
   `performScrollTo()`. Karena `pump()` mematikan `mainClock.autoAdvance`, animasi scroll tidak
   pernah selesai sehingga `performScrollTo` berputar berulang-ulang (login juga memakan ±4,5
   menit karena tombol Login terpotong). Solusi: `performScrollTo` dihapus, karena klik lewat
   `SemanticsActions.OnClick` tidak butuh node terlihat. Sekarang tur selesai ±12 detik.
3. Tombol "Terima dana dengan QRIS saya" diawali dua spasi (jarak ke ikon), jadi dicocokkan
   dengan `substring = true`.

**Bug UI yang ditemukan dari screenshot dan sudah diperbaiki:**
- `core/ui/Charts.kt` (`drawXLabels`): label sumbu-X grafik saldo bertumpuk ("28/1030/10…",
  "Okt 2Nov 26") dan menabrak label sumbu-Y terbawah. Sekarang jarak label dihitung dari lebar
  teks, digambar dari kanan dengan cek tabrakan, dan diletakkan di bawah label Y
  (`PLOT_BOTTOM_DP` 24 → 30).
- `topup/presentation/TopUpScreen.kt`: tombol nominal pulsa/PLN terpotong per karakter di layar
  sempit ("5.00 / 0"). Sekarang memakai `NominalButton` dengan padding 4dp dan teks satu baris.

**Catatan minor (belum diubah):** layar Virtual Card belum memakai `ScreenScaffold` (tanpa top
bar, tombol teks "← Kembali ke Home"), dan tombol "Analitik" di Riwayat terlipat dua baris pada
layar 320dp.

## Pendekatan yang sudah dicoba (urut)

1. **Emulator**: gagal. Driver hypervisor Android Emulator belum terpasang di laptop
   ("x86_64 emulation currently requires hardware acceleration").
2. **`captureToImage()` Compose**: timeout di Robolectric. Diganti dengan menggambar semua
   window (`WindowManagerGlobal.mViews`) ke bitmap, dan cara ini berhasil.
3. **Robolectric 4.14.1**: gagal karena JDK 25 ("Unsupported class file major version 69").
   Dinaikkan ke **4.17** dan berhasil.
4. **Dialog membuat Compose tidak pernah idle**: hasil bisection dengan probe test:
   - Tidak berpengaruh: tema, Scaffold, NavHost, navigasi, fokus text field, `PinDialog`,
     timer sesi, izin notifikasi (SDK 32 vs 34), `mainClock.autoAdvance = false`.
   - **Penyebab:** qualifier **ukuran layar** di `@Config` (`w411dp-h891dp`). Densitas
     (`xxhdpi`) aman. Bahkan `MaterialTheme` biasa ikut macet → ini keterbatasan Robolectric,
     **bukan bug aplikasi**.
   - Solusi: `qualifiers = "xxhdpi"` saja.
5. **Layar default kecil (320×470dp)**:
   - Tombol Login terpotong sehingga klik sentuh meleset. Diganti klik lewat semantics dan berhasil.
   - Lalu `performTextInput` gagal pada field kedua (error di atas).

Catatan sampingan: timer timeout sesi (FR-05) sudah dipindah dari `LaunchedEffect` di UI ke
`AuthViewModel` (`startInactivityWatch`). Perubahan ini tetap dipertahankan karena desainnya
lebih baik, walaupun bukan penyebab masalah dialog.

## Langkah berikutnya

1. ~~Perbaiki `AppTourTest`~~ ✅ dan ~~periksa screenshot 08–24~~ ✅ (selesai 10 November 2026).
   Menjalankan semua test:

   ```
   export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
   ./gradlew :app:testDebugUnitTest
   ```

   Jika gagal, lihat `app/build/screenshots/GAGAL_*.png` yang dibuat otomatis oleh `waitForText`.
2. ~~Uji di emulator~~ ✅ (10 November 2026, sesi ke-2). Emulator `Pixel_8` jalan dengan WHPX.
   Alur transfer (+ ditolak/timeout, bagikan PNG/PDF), QRIS statis/dinamis/terima, top-up PLN,
   kantong (buat/isi/tarik dini), dan ubah profil + OTP semuanya lulus. Bug yang ditemukan dan
   diperbaiki:
   - Back di Beranda kembali ke layar PIN → `popUpTo(navController.graph.id)` di `AuthNavGraph`.
   - Tab Beranda memulihkan tab terakhir → `switchTab(HOME)` = `popBackStack(HOME, false)`.
   - QRIS di luar batas tetap meminta PIN → validasi di `QrisPayViewModel.requestPin()`.
   Masih perlu dicek pengguna di HP sungguhan: kamera QRIS, notifikasi push, berbagi file.
3. ~~Perbarui `README.md`~~ ✅ (akun demo, data uji simulator, batas & biaya, skenario semua modul).
4. Opsional / di luar cakupan saat ini:
   - Persistensi data (sekarang in-memory, data hilang saat aplikasi ditutup).
   - Integrasi REST API ke backend Spring Boot.
   - Biometrik sungguhan (`BiometricPrompt`).

## Nilai yang belum ditetapkan SRS dan diisi sebagai usulan

Semua nilai ini perlu dikonfirmasi tim/dosen:

| Hal | Nilai usulan | Letak di kode |
|---|---|---|
| Maksimal transfer per transaksi (BR-07) | Rp25.000.000 | `TransferRules` |
| Biaya transfer antarbank | Rp2.500 | `TransferRules` |
| Batas QRIS per transaksi | Rp1.000 – Rp10.000.000 | `QrisRules` |
| Limit Virtual Card | Rp10.000 – Rp50.000.000 (default Rp5.000.000) | `VirtualCardLimits` |
| Saldo awal pengguna baru (simulasi) | Rp50.000 | `CoreModule` |
| Timeout sesi karena tidak aktif | 5 menit | `InactivityMonitor` |
