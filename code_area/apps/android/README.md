# Diponegoro Digital Bank — Aplikasi Android

Gabungan keempat modul tim (Salomo, Cecil, Cia, Grace) menjadi satu aplikasi sesuai
**SRS-DDB-001 v0.1** (FR-01 – FR-29, kecuali FR-26 yang ditetapkan SRS untuk Web).
Teknologi: Kotlin, Jetpack Compose, MVVM, Repository Pattern. Data disimulasikan in-memory.

## Menjalankan

Dua varian (product flavor), DAFTAR_PEKERJAAN Fase 5:

| Varian | Data | APK |
|---|---|---|
| `demo` | Simulasi in-memory (tanpa server), untuk uji UI & cadangan demo | `app/build/outputs/apk/demo/debug/app-demo-debug.apk` |
| `online` | Backend bersama lewat REST (`/api/v1`) | `app/build/outputs/apk/online/debug/app-online-debug.apk` |

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"   # Git Bash, Windows
./gradlew :app:assembleDemoDebug                       # varian demo
./gradlew :app:assembleOnlineDebug                     # online, backend di http://10.0.2.2:8080/ (emulator)
./gradlew :app:assembleOnlineDebug -Pddb.baseUrl=http://192.168.1.10:8080/   # online, HP di Wi-Fi yang sama
./gradlew :app:testDemoDebugUnitTest                   # 63 test (54 lama + 8 MockWebServer + 1 live dilewati)
./gradlew :app:testOnlineDebugUnitTest -Pddb.liveUrl=http://localhost:8080/  # 58 test termasuk uji live ke backend
```

Varian online di Android 17 (API 37) meminta izin **Nearby devices / `ACCESS_LOCAL_NETWORK`** saat dibuka;
izin ini wajib selama backend berjalan di laptop/LAN. HTTP polos hanya diizinkan pada varian online
(`src/online/res/xml/network_security_config.xml`); produksi wajib HTTPS.

## Akun demo

| Hal | Nilai |
|---|---|
| Username / password | `demo` / `Demo@1234` |
| PIN transaksi | `123456` |
| Rekening | DDB 1400123456 (saldo awal ±Rp25 juta + riwayat 5 bulan) |
| Nasabah lain (sesama DDB) | ANDI 1400000002, BUDI 1400000003, CITRA 1400000004 |

## Data uji simulator

| Data | Hasil |
|---|---|
| Rekening antarbank normal | BCA 1234567890, BRI 9876543210, BNI 1122334455, Mandiri 5566778899, BSI 7001234567, CIMB 8001234567 |
| BRI `9999999999` | Ditolak jaringan → dana dikembalikan (UC-01 A6) |
| Mandiri `8888888888` | Timeout → status **Diproses**, dana tertahan, tanpa debit ganda (UC-01 A5) |
| ID PLN / nomor tagihan berakhiran `0000` | "ID pelanggan tidak ditemukan" |
| NIK berakhiran `0000` | e-KYC ditolak |
| OTP | Kode simulasi ditampilkan di layar |

## Batas & biaya (usulan, belum ditetapkan SRS — perlu konfirmasi tim/dosen)

| Hal | Nilai | Letak di kode |
|---|---|---|
| Transfer per transaksi | Rp5.000 – Rp25.000.000 | `TransferRules` |
| Biaya transfer antarbank | Rp2.500 (sesama DDB gratis) | `TransferRules` |
| QRIS per transaksi | Rp1.000 – Rp10.000.000 | `QrisRules` |
| Biaya admin pulsa / PLN / tagihan | Rp1.500 / Rp2.500 / Rp2.500 | `TopUpModels` |
| Limit Virtual Card | Rp10.000 – Rp50.000.000 (default Rp5.000.000) | `VirtualCardLimits` |
| Saldo awal pengguna baru | Rp50.000 | `CoreModule` |
| Timeout sesi tidak aktif | 5 menit | `InactivityMonitor` |
| Salah PIN | 3× → PIN diblokir 5 menit | modul Authentication |

## Skenario uji modul transaksi

Diuji di emulator Pixel 8 (Android 37) pada 10 November 2026 — semua lulus.

| Modul | Skenario | Hasil yang diharapkan |
|---|---|---|
| Transfer | Home → Transfer → `1400000002` → Cek rekening → Rp150.000 → Transfer sekarang → PIN | Bukti transfer, biaya Rp0, saldo berkurang |
| Transfer | Dialog PIN → PIN salah | "PIN salah. Sisa percobaan: 2" |
| Transfer | Bukti → Bagikan gambar / Bagikan PDF | Share sheet Android, file `bukti-<ID>.pdf` |
| Transfer | Penerima → ikon bintang | Tersimpan di "Penerima favorit" |
| Transfer | BRI `9999999999` Rp50.000 | "Transfer gagal — dana dikembalikan", saldo kembali |
| Transfer | Mandiri `8888888888` Rp50.000 | Status Diproses, dana Rp52.500 tertahan |
| QRIS bayar | Bayar QRIS → "Warung Bu Sri" (statis) → Rp500 → Bayar | Error "Nominal QRIS Rp1.000 – Rp10.000.000" **sebelum** PIN |
| QRIS bayar | Statis → Rp25.000 → PIN | Pembayaran berhasil |
| QRIS bayar | "Kopi Senja" (dinamis) | Nominal Rp28.000 terkunci, tidak bisa diubah |
| QRIS terima | Terima QRIS → Simulasi dana masuk (nama + nominal) → Kirim dana | Saldo bertambah, notifikasi masuk |
| Top-up | Token Listrik → `123456780000` → Cek | "ID pelanggan tidak ditemukan" |
| Top-up | Token Listrik → `123456789012` → Rp50.000 → Bayar → PIN | Struk dengan nomor token, total Rp52.500 |
| Kantong | + Buat kantong → nama, tujuan, target, kunci 1 bln | Kantong "Terkunci sampai …" |
| Kantong | Isi Rp1.000.000 | Saldo utama berkurang, progres target naik |
| Kantong | Tarik dini Rp200.000 → konfirmasi → PIN | "Penarikan dini … berhasil" |
| Profil | Ubah data → nomor telepon baru → Simpan → OTP salah | "Kode OTP salah." |
| Profil | OTP benar | "Profil berhasil diperbarui" |
| Riwayat | Tab Riwayat → filter periode/jenis/status, Analitik | Semua transaksi di atas tercatat |
| Navigasi | Back di Beranda | Keluar aplikasi (tidak kembali ke layar PIN) |

**Uji manual di HP sungguhan (14 November 2026, Realme RMX3867, Android 16): lulus.** Pindai QRIS dengan kamera,
notifikasi push, dan berbagi bukti transaksi ke WhatsApp berjalan dengan baik.

---

# Skenario Uji Emulator — Authentication

## Skenario

  -----------------------------------------------------------------------
  Skenario                            Langkah Pengujian
  ----------------------------------- -----------------------------------
  **Login biometrik**                 Tombol biometrik muncul karena akun
                                      demo sudah **"diingat"** → pilih
                                      **Autentikasi** → masukkan PIN
                                      `123456` → **Home**

  **Login password**                  Masukkan kredensial `demo` /
                                      `Demo@1234` → masukkan PIN `123456`

  **Registrasi penuh**                Pilih **Daftar** → salin kode OTP
                                      dari kartu → masukkan **NIK 16
                                      digit** (mis. `3374010101010001`) +
                                      centang **selfie** → **approve** →
                                      **Login** → buat PIN → **Enter
                                      PIN**

  **e-KYC ditolak**                   Gunakan NIK berakhiran `0000`

  **OTP salah**                       Masukkan kode OTP acak/salah →
                                      pastikan muncul pesan error → pilih
                                      **"Kirim ulang OTP"**

  **Blokir PIN**                      Masukkan PIN yang salah sebanyak
                                      **3 kali** → pastikan PIN diblokir
                                      selama **5 menit**

  **Logout all**                      Login sebanyak 2× (mis. password
                                      lalu biometrik, dengan melakukan
                                      logout ke login di antaranya agar
                                      sesi tidak menumpuk) → buka
                                      **Home** → pilih **"Logout dari
                                      semua perangkat"**

  **Recovery**                        Pilih **Login** → **"Lupa password
                                      / PIN?"** → pilih tipe recovery →
                                      masukkan **OTP** → buat **set
                                      baru**
  -----------------------------------------------------------------------

## Catatan Pengujian

-   Gunakan akun demo yang telah disediakan oleh aplikasi.
-   Gunakan data uji yang sesuai dengan environment emulator.
-   Untuk skenario e-KYC ditolak, gunakan NIK dengan 4 digit terakhir
    `0000`.
-   Pastikan pesan error, navigasi, dan perubahan status akun sesuai
    dengan hasil yang diharapkan.
-   Setelah pengujian **Blokir PIN**, tunggu hingga masa blokir berakhir
    atau gunakan mekanisme reset yang disediakan oleh environment
    pengujian.
-   Skenario **Logout all** digunakan untuk memastikan sesi pada seluruh
    perangkat/perangkat virtual yang terkait benar-benar terputus.

## Ringkasan Cakupan

  Area             Skenario
  ---------------- ---------------------------------
  Autentikasi      Login biometrik, login password
  Registrasi       Registrasi penuh
  e-KYC            e-KYC ditolak
  OTP              OTP salah & kirim ulang OTP
  Keamanan PIN     Blokir PIN
  Manajemen sesi   Logout dari semua perangkat
  Pemulihan akun   Recovery password/PIN
  Virtual Card     Buat, lihat detail (PIN), bekukan/aktifkan, limit

---

# Modul Virtual Card (FR-21, FR-22)

Package: `com.diponegoro.digitalbank.virtualcard` (pola sama dengan Authentication:
`domain` → `data` → `di` → `presentation`). Diakses dari **Home → Virtual Card**.

| Use case (UML)              | Implementasi                                     |
|-----------------------------|--------------------------------------------------|
| Create Virtual Card + Select Usage Scope | `CreateVirtualCardUseCase` (butuh e-KYC disetujui) |
| View Details + Verify Transaction PIN    | `ViewCardDetailsUseCase` (pakai PIN & aturan blokir modul Authentication) |
| Freeze / Activate Virtual Card           | `FreezeCardUseCase`, `ActivateCardUseCase` |
| Set Transaction Limit                    | `SetTransactionLimitUseCase` (Rp10.000 – Rp50.000.000 per transaksi) |

Tambahan untuk pengujian: tombol **Uji transaksi** mensimulasikan pembayaran merchant
untuk membuktikan bahwa kartu beku menolak transaksi, limit berlaku pada transaksi
berikutnya, dan kartu domestik menolak merchant luar negeri.

## Skenario Uji Emulator — Virtual Card

| Skenario | Langkah | Hasil yang diharapkan |
|---|---|---|
| Buat kartu | Login `demo` → Home → **Virtual Card** → **+ Buat Virtual Card** → pilih Domestik/Internasional | Kartu baru tampil, status Aktif, nomor tersamar |
| Lihat detail | **Lihat detail** → PIN `123456` | Nomor lengkap, masa berlaku, CVV tampil |
| PIN salah | **Lihat detail** → PIN salah | Akses ditolak, sisa percobaan tampil; 3x salah → PIN diblokir 5 menit |
| Bekukan | **Bekukan** → **Uji transaksi** Rp50.000 | Transaksi ditolak: kartu dibekukan |
| Aktifkan | **Aktifkan** → **Uji transaksi** Rp50.000 | Transaksi disetujui |
| Limit tidak valid | **Atur limit** → `5000` | Pesan error limit minimal |
| Limit berlaku | **Atur limit** → `100000` → **Uji transaksi** Rp150.000 | Ditolak: melebihi limit |
| Cakupan domestik | Kartu Domestik → **Uji transaksi** + centang *Merchant luar negeri* | Ditolak: kartu hanya berlaku domestik |
| e-KYC ditolak | Registrasi dengan NIK berakhiran `0000` → login → Virtual Card | Tombol buat kartu nonaktif + peringatan e-KYC |

Unit test: `app/src/test/.../virtualcard/VirtualCardUseCasesTest.kt`
(jalankan `gradlew :app:testDebugUnitTest`).

## APK rilis

APK rilis ditandatangani dengan `keystore/ddb-release.jks`; lokasi dan password dibaca dari `keystore.properties`.
**Kedua berkas tidak boleh dibagikan atau di-commit** (sudah ada di `.gitignore`). Simpan cadangannya di tempat aman:
pembaruan aplikasi harus ditandatangani dengan kunci yang sama.

```powershell
.uild-rilis.ps1                                    # online untuk emulator (http://10.0.2.2:8080/)
.uild-rilis.ps1 -BaseUrl http://192.168.1.10:8080/  # online untuk HP di Wi-Fi yang sama
```

Hasil di `rilis/` (`DDB-1.0-demo.apk`, `DDB-1.0-online-<host>.apk`), tanda tangan diverifikasi `apksigner` dan SHA-256 dicetak.
Varian online rilis dan debug memakai application ID yang sama tetapi tanda tangan berbeda: copot dulu
(`adb uninstall com.diponegoro.digitalbank`) sebelum mengganti.

## Pengujian end-to-end

Uji sistem Android di emulator (SA-01 – SA-28) dan uji lintas platform dengan Web ada di folder
**`pengujian/`** di root proyek (`npm run android:sa`, `npm run lintas`). Lihat `pengujian/README.md`.
