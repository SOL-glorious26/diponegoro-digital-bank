# Review D04A Detailed Design Android: untuk Gracia

Dokumen yang direview: `DDB_D04A_Detailed_Design_Android_v0.1.docx` (v0.1, 14 Nov 2026)
Reviewer: Salomo (PM), 16 Nov 2026
Acuan: template dosen Dokumen 4, DWP-DDB-001 v1.3 (tugas D04A-WP1 s.d. WP6, Definition of Done), D02 v0.9.5, D03 v0.9.3, D04B v0.9.2, D06 v0.9.6, dan kode `code_area/apps/android`

---

## 1. Kesimpulan singkat

**Status: belum bisa masuk review R1, masih kerangka.** Strukturnya sudah benar (5 bagian wajib template sudah ada), tapi **isi utamanya belum ada**: belum ada satu pun diagram UML atau workflow, dan banyak bagian masih bertanda `[ISI DARI KODE]`.

Kabar baiknya, aku sudah mengecek semua nama kelas yang kamu tulis ke kode, dan **hampir semuanya benar-benar ada**. Jadi kamu tinggal membaca kode dan mengisi, bukan mulai dari nol.

Tenggat akhir seluruh dokumen: **24 Nov 2026, 23.59 WIB**. Usahakan selesai beberapa hari sebelumnya supaya masih ada waktu review. Pre-review R1 dilakukan **Cecilia**, review teknis R2 oleh **Salomo**.

| Bagian wajib template | Ada? | Kondisi |
|---|---|---|
| Blok kontrol dokumen | ✅ | Lengkap |
| UML Diagram | ⚠️ | Hanya rencana, **0 gambar** |
| Workflow Diagram | ⚠️ | Hanya tabel ringkas, **0 gambar** |
| Algoritma Pseudocode | ✅ | 6 pseudocode, cukup bagus |
| Perancangan Software/Hardware | ✅ | Hampir lengkap, ada data yang usang |

---

## 2. Yang sudah bagus 👍

- Jujur: bagian yang belum diverifikasi diberi tanda dengan jelas. Ini sesuai aturan "kejujuran isi" di DWP.
- Ketentuan proyek sudah benar: login tanpa OTP, transaksi pakai PIN + kunci idempotensi, Virtual Account tidak dibuat, pindai QRIS hanya di Android.
- Pseudocode 5.1 s.d. 5.6 sudah sesuai aturan SRS (BR-01, BR-02, A5, dan seterusnya).
- Tabel izin aplikasi (kamera, notifikasi, jaringan lokal Android 17) sudah benar.

---

## 3. Hasil cek ke kode (sudah aku verifikasi, tanda `[VERIFIKASI]`-nya boleh dihapus)

| Yang kamu tulis | Hasil cek |
|---|---|
| `QrisRules`, `QrisPayViewModel`, `requestPin` | ✅ Ada. Batas QRIS di kode: `MIN_AMOUNT = 1_000`, `MAX_AMOUNT = 10_000_000` |
| `TransferRules` | ✅ Ada. `MIN_AMOUNT = 5_000`, `MAX_AMOUNT_PER_TRANSACTION = 25_000_000`, `INTERBANK_FEE = 2_500`, `MAX_NOTE = 50`, `MAX_FAVORITES = 20` |
| `TopUpModels` | ✅ Ada sebagai berkas `topup/domain/TopUpModels.kt` |
| 5 use case Virtual Card (`CreateVirtualCardUseCase`, dst.) | ✅ Ada semua |
| `TransactionGuard`, `NotificationCenter`, `CoreModule`, `FeatureModule`, `DigitalBankApplication` | ✅ Ada |
| `ScreenScaffold`, `core/ui/Charts.kt` | ✅ Ada |
| `InactivityMonitor`, `AuthViewModel.startInactivityWatch` | ✅ Ada |
| `AuthNavGraph.kt` | ✅ Ada (309 baris). Rute ada di `AuthRoutes.kt` dan `core/navigation/AppRoutes.kt` |
| Java 11, minSdk 24, targetSdk 37 | ✅ Sesuai `app/build.gradle.kts` |
| **Biometrik** (pertanyaanmu di 6.6) | ✅ Benar: **tidak ada** `androidx.biometric`. Login biometrik **masih simulasi**. Tulis sebagai keterbatasan, jangan sebagai fitur nyata |

**Pertanyaanmu "Isi lapisan REST" (`src/online/`):** folder `src/online` hanya berisi konfigurasi jaringan. Klien REST ada di **`core/network/DdbApi.kt`** (paket `main`), dan endpoint-nya sama dengan daftar 73 operasi di **D03 Tabel 4.2** atau **D04B**.

---

## 4. Yang harus diperbaiki

### 🔴 Wajib (supaya lulus Definition of Done)

1. **Buat diagram UML.** Minimal:
   - Class diagram untuk 2 modulmu (Payment QRIS, Profile & Notification) dan modul core (ledger, security, notification, di).
   - **4 sequence diagram** sesuai DWP D04A-WP2-T02: login + PIN, transfer UC-01, bayar QRIS, top-up. Panggilan API-nya harus sama dengan D04B.
   - State diagram: status sesi dan status transaksi (Berhasil / Diproses / Gagal).
   - Modul lain cukup diagram ringkas, tanyakan ke PIC-nya: Salomo (Authentication, Virtual Card), Cecilia (Transfer, History), Grace (Account, Top-up).
2. **Buat workflow (activity) diagram per modul.** Harus konsisten dengan **workflow D03 v0.9.3 Bab VI** (sekarang sudah ada uraian tiap alurnya) dan dengan kode layar di **D05A** (Cecilia).
3. **Hapus bagian "Catatan untuk PIC"** sebelum dikirim ke review.
4. **Isi semua `[ISI DARI KODE]`.** Kalau memang belum bisa, ganti dengan **"Perlu Ditentukan (pemilik: …)"**. Aturan DWP: tidak boleh ada placeholder tanpa label.
5. **Daftar isi masih kosong.** Di Word: klik kanan → *Update Field* sebelum dikirim.

### 🟡 Data yang sudah usang (ganti dengan angka terbaru)

| Bagian | Tertulis | Seharusnya |
|---|---|---|
| 2.1 | "sekitar 9.197 baris (D01 v0.9.1)" | 9.227 baris / 9,227 KSLOC, 110 berkas Kotlin (D01 v0.9.4) |
| 6.4 | "Emulator Pixel 8 (Android 37)" | "Emulator Pixel 8 (Android 17, API 37)". Angka 37 itu level API, bukan versi Android |
| 6.4 | "ponsel Android fisik" | Realme RMX3867 (Android 16), sudah dipakai uji 14 Nov |
| 6.5 dan 6.6 | "Uji pada ponsel fisik belum dilakukan / masih tertunda" | **Sudah lulus 14 Nov 2026**: pindai QRIS dengan kamera (SA-08), notifikasi push, dan berbagi bukti (D06 v0.9.6). Uji sistem Android **28/28** |
| 6.5 | "demo 63" | 62 lulus + 1 dilewati (D06 v0.9.6 Tabel 6.1) |
| Kontrol dokumen | D02, D03 tanpa versi; D05A "belum ada" | SAD-DDB-001 v0.9.5; FSD-DDB-001 v0.9.3; UIM-DDB-001-AND v0.1 (Cecilia, 16 Nov) |
| Referensi | D01 v0.9.1; Panduan Uji Ponsel v1.0 | D01 v0.9.4; Panduan Uji Ponsel v1.1 |

### 🟢 Saran (tidak wajib)

- Tabel 6.1 kolom versi "via version catalog" bisa diisi dari `gradle/libs.versions.toml`: AGP 9.4.1, Kotlin 2.2.10, Compose BOM 2026.02.01, Retrofit 3.0.0, OkHttp 4.12.0, kotlinx-serialization 1.9.0.
- Isi "RAM minimum" laptop pengembang (misalnya 8 GB, 16 GB disarankan untuk emulator), atau tulis "Perlu Ditentukan".
- Kode dokumen di judul (`SDD-DDB-001-AND`) sudah sesuai DWP. Pertahankan.

---

## 5. Checklist sebelum kirim ke Cecilia (R1)

- [ ] Class diagram (modul QRIS, Profile, core) + ringkasan modul lain
- [ ] 4 sequence diagram (login+PIN, transfer, QRIS, top-up) dengan endpoint dari D04B
- [ ] 2 state diagram
- [ ] Workflow diagram per modul, cocok dengan D03 Bab VI dan D05A
- [ ] Semua `[VERIFIKASI]` / `[ISI DARI KODE]` diisi atau diganti "Perlu Ditentukan (pemilik)"
- [ ] Angka di tabel "data usang" di atas diperbarui
- [ ] Bagian "Catatan untuk PIC" dihapus
- [ ] Daftar isi di-update, versi dinaikkan (misalnya v0.5) dan dicatat di Revision Control

Kalau butuh bantuan membaca kode atau membuat diagram, tanya Salomo. 🙏
