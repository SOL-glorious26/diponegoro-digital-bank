# Review D04W v0.7 dan D08 v0.2: untuk Rafi

Reviewer: Salomo (PM), 22 Nov 2026. **Tenggat: 23 November 2026 pukul 23.59 WIB.**

## 1. Kesimpulan
**Keduanya dipakai sebagai dokumen Web.** D04W akurat terhadap kode (rute, sesi, Idempotency-Key, pseudocode), dan D08 jujur soal apa yang belum ada. Terima kasih, Rafi.

PM hanya mengoreksi fakta, isi tetap punyamu:
- `documents_area/D04W_Detailed_Design_Web/DDB_D04W_Detailed_Design_Web_v0.8.docx`
- `documents_area/D08W_System_Program_Demo_Web/DDB_D08W_System_Program_Demo_Web_v0.3.docx`

D08 sekarang dua berkas: **D08W (punyamu, Web)** dan **D08A (Android, disusun PM)**. **Lanjutkan dari versi di atas, jangan dari v0.7/v0.2.**

## 2. Yang dikoreksi
| Dokumen | Koreksi |
|---|---|
| D04W, D08 | Web Node sudah diuji dengan backend asli: WN 14/14, LPN 3/3 (bukti di `code_area/pengujian/bukti/2026-11-22/`) |
| D04W, D08 | FR-07 dihapus dari Dashboard (grafik saldo belum ada di Web) |
| D04W | Kode halaman WEB-01 s.d. WEB-11 (D05W v0.7 Nabila); jumlah baris disamakan dengan `SE-web.rar`; `pwtoggle.js` dicatat |
| D08 | DEM-06 dan DEM-07 tertukar: ditolak = SA-05, timeout = SA-06 |
| D08 | Ringkasan D06 disamakan dengan v1.0 (14 defect; Android 63/64 dan 57/58) |

## 3. Yang masih perlu kamu kerjakan
1. **Tangkapan layar Web** (D08W 6.1, sembilan gambar) lewat gladi bersama Nabila dan Permata.
2. Uji manual yang belum ada: ubah profil, ubah kata sandi, lupa sandi, Top Up, ID PLN 0000, rekening tidak valid, filter dan jurnal riwayat.
3. D08W: skenario **DEM-12** belum punya naskah. Boleh merujuk ke DA-06 di D08A.
4. Gambar sequence D04W (3.3–3.5) teksnya kecil; perbesar bila sempat.
5. Setelah selesai, naikkan ke v1.0 dan kabari Salomo.

Catatan: jangan unggah `SE-web.rar` ke GitHub karena berisi `.env`, `node_modules`, dan database.
