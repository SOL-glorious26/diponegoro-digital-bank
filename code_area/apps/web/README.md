# Diponegoro Digital Bank – Aplikasi Web

Klien Web nasabah (thin client) yang memakai backend dan kontrak API yang sama dengan aplikasi Android.
Arsitektur mengikuti SAD-DDB-001 Bab VIII: React + JavaScript, Vite, react-router-dom, axios.

## Menjalankan

1. Jalankan backend: `cd backend` lalu `.\run-local.ps1 -H2` (port 8080).
2. Jalankan Web:
   ```
   cd apps\web
   npm install
   npm run dev
   ```
3. Buka http://localhost:5173. Akun demo: `demo` / `Demo@1234`, PIN `123456`.

Vite meneruskan `/api` ke `http://localhost:8080`, jadi peramban tidak perlu CORS.
Untuk backend di alamat lain: `$env:DDB_API="http://<ip>:8080"; npm run dev`.
Agar bisa dibuka dari perangkat lain di Wi-Fi yang sama: `npm run dev -- --host`.

Build produksi: `npm run build` → folder `dist/` (berkas statis, dilayani di belakang API Gateway).

## Struktur

| Berkas | Isi |
|---|---|
| `src/api/client.js` | Instance axios (`/api/v1`), token, refresh otomatis saat 401, `Idempotency-Key`, pesan error |
| `src/auth/AuthContext.jsx` | Sesi: login, logout, logout semua perangkat, pemulihan sesi setelah muat ulang |
| `src/auth/ProtectedRoute.jsx` | Route guard: login → e-KYC disetujui → PIN sudah dibuat |
| `src/components/` | Layout (sidebar, notifikasi), dialog PIN, detail/bukti transaksi, elemen UI |
| `src/pages/AuthPages.jsx` | Masuk (FR-03), daftar + OTP (FR-01), e-KYC (FR-02), buat PIN (FR-04), pemulihan (FR-05) |
| `src/pages/Dashboard.jsx` | Saldo + sembunyikan (FR-06), grafik saldo harian/mingguan/bulanan (FR-07, `components/BalanceChart.jsx`), grafik bulanan (FR-25), transaksi terakhir |
| `src/pages/Transfer.jsx` | Sesama bank & antarbank, validasi rekening, favorit, bukti (FR-10–FR-14) |
| `src/pages/History.jsx` | Filter, pencarian, paginasi, detail + jurnal, ekspor CSV/PDF (FR-23, FR-24, FR-26) |
| `src/pages/TopUp.jsx` | Pulsa, paket data, token PLN, tagihan, e-wallet (FR-18–FR-20) |
| `src/pages/Qris.jsx` | QRIS pribadi dan dinamis (FR-16, FR-17); pemindaian QRIS khusus Android (FR-15) |
| `src/pages/Pockets.jsx` | Kantong: buat, isi, tarik, ubah, kunci, hapus (FR-08, FR-09) |
| `src/pages/Cards.jsx` | Virtual card (FR-21, FR-22) |
| `src/pages/Profile.jsx` | Profil dengan OTP, foto profil (URL), ubah password/PIN, pengaturan notifikasi, sesi aktif (FR-27, FR-28, FR-05) |
| `src/pages/Notifications.jsx` | Kotak notifikasi (FR-29) |

## Aturan yang diterapkan (sesuai SRS dan keputusan PM)

- Login memakai username/email/nomor HP + password, tanpa OTP.
- Setiap transaksi dikonfirmasi PIN 6 digit dan mengirim header `Idempotency-Key` (satu kunci per layar konfirmasi; retry memakai kunci yang sama sehingga tidak ada debit ganda).
- Tidak ada Virtual Account. QRIS di Web hanya menampilkan QRIS pribadi.
- Halaman transaksi hanya dapat dibuka setelah e-KYC disetujui dan PIN dibuat.
- Access token disimpan di memori, refresh token di `sessionStorage` (hilang saat tab ditutup).

## Pengujian

Uji end-to-end aplikasi Web (kontrak API, UI di Chrome headless, profil/OTP, token kedaluwarsa, Chrome/Edge/Firefox)
dan uji lintas platform dengan Android ada di folder **`pengujian/`** di root proyek. Lihat `pengujian/README.md`.
