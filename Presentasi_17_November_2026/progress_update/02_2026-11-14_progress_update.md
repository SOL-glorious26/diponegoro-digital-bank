# Progress Update #02: Sabtu, 14 November 2026 (malam)

Proyek: **Diponegoro Digital Bank**, Kelompok Jaran Goyang (Software Engineering UKSW)
Disusun oleh: Salomo (PM)
Tenggat dokumentasi: **24 November 2026, 23.59 WIB** (sisa 10 hari)

---

## 1. Ringkasan singkat

- **Uji aplikasi Android di HP sungguhan: LULUS.** Pemindaian QRIS dengan kamera (SA-08),
  notifikasi push, dan berbagi file ke WhatsApp berjalan dengan baik.
- Dengan ini, uji yang sebelumnya tidak bisa dilakukan di emulator sudah tertutup.

---

## 2. Uji di HP fisik

**Perangkat:** Realme RMX3867, Android 16
**Varian aplikasi:** `onlineDebug`, terhubung ke backend di laptop (profil H2)
**Koneksi ke backend:** kabel USB dengan `adb reverse tcp:8080 tcp:8080`, alamat server `http://127.0.0.1:8080/`

| # | Yang diuji | Hasil |
|---|---|---|
| 1 | Login akun demo | ✅ Lulus |
| 2 | Pindai QRIS dengan kamera HP (SA-08) | ✅ Lulus |
| 3 | Notifikasi push setelah transaksi | ✅ Lulus, notifikasi muncul di HP |
| 4 | Berbagi bukti transaksi ke WhatsApp | ✅ Lulus |

### Kendala yang ditemui dan solusinya

| Kendala | Penyebab | Solusi |
|---|---|---|
| "Tidak dapat terhubung ke server" | Aplikasi dipasang dengan alamat default `10.0.2.2`, yang hanya berlaku di emulator | Pasang ulang dengan `ddb.baseUrl=http://127.0.0.1:8080/` dan jalankan `adb reverse tcp:8080 tcp:8080` |
| `INSTALL_FAILED_VERIFICATION_FAILURE` | HP Realme meminta konfirmasi pemasangan lewat USB | Aktifkan *Nonaktifkan pemantauan izin* di Opsi pengembang, lalu setujui dialog di layar HP |

Catatan: `adb reverse` hilang setiap kabel dicabut, jadi perintahnya perlu dijalankan lagi setelah HP dicolok ulang.

---

## 3. Sisa pekerjaan

| # | Pekerjaan | PIC |
|---|---|---|
| 1 | D04A Detailed Design Android | Gracia |
| 2 | D05A User Interface Map Android | Cecilia |
| 3 | D08 System Program Demo | Rafi |
| 4 | D09A User Manual Android | Grace |
| 5 | D09W User Manual Web | Permata |
| 6 | Perbarui D06 System Testing dengan hasil uji HP fisik (SA-08) | Steven / Salomo |
| 7 | Catat D04W, D07W, dan tutorial Web di DWP | Salomo |
| 8 | Naikkan semua dokumen ke v1.0 (final + tanda tangan) | Salomo |
