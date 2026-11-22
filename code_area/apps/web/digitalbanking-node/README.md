# diponegoro corp bank — Web Node.js (memakai backend Spring Boot)

Web ini **tidak punya database sendiri**. Semua data dan aturan bisnis ada di backend Spring Boot
(`code_area/backend` di repo GitHub tim, API `/api/v1`). Backend yang sama dipakai aplikasi Android,
sehingga saldo dan riwayat di Web dan Android selalu sama.

```
Peramban ──► Web Node :3000 (Express + EJS) ──REST /api/v1──► Backend Spring Boot :8080 ──► H2 / PostgreSQL
                                                                     ▲
                                                  Aplikasi Android ──┘ (varian online)
```

- Token JWT disimpan di **sesi server Node**, tidak pernah dikirim ke peramban. Karena Node yang memanggil
  backend (server ke server), **tidak perlu CORS**.
- Saat backend membalas 401, Web memperbarui token sekali lewat `/auth/refresh` lalu mengulang permintaan.
  Bila gagal, pengguna diarahkan ke halaman Masuk.
- Setiap transaksi memakai PIN 6 digit dan header `Idempotency-Key` (satu kunci per layar konfirmasi).

## 1. Prasyarat
- Node.js 18 atau lebih baru (`node -v`)
- Backend Spring Boot berjalan di http://localhost:8080 (lihat langkah 2)
- Tidak perlu PostgreSQL/XAMPP untuk Web ini.

## 2. Jalankan (dua terminal)

**Terminal 1 — backend** (dari repo GitHub tim):
```powershell
cd code_area\backend
.\run-local.ps1 -H2
```
Tunggu sampai muncul `Backend jalan di http://localhost:8080`.

**Terminal 2 — Web ini:**
```powershell
cd digitalbanking-node
copy .env.example .env
npm install
npm start
```
Buka http://localhost:3000 lalu masuk dengan akun demo: `demo` / `Demo@1234`, PIN transaksi `123456`.
Akun lain: `andi`, `budi`, `citra` (password dan PIN sama).

Bila backend berjalan di alamat lain, ubah `BACKEND_URL` di file `.env`.

## 3. Yang berubah dibanding Web Node versi lama
| Sebelumnya (database sendiri) | Sekarang (backend Spring Boot) |
|---|---|
| Login → OTP → Dashboard | Login langsung (tanpa OTP, sesuai SRS FR-03). OTP hanya untuk registrasi, pemulihan, dan ubah profil |
| Transfer dikonfirmasi OTP | Transfer dikonfirmasi **PIN 6 digit** dan `Idempotency-Key` |
| Daftar langsung punya rekening | Daftar → OTP → **e-KYC** → **Buat PIN** (halaman `/ekyc` dan `/pin`) |
| Minimal transfer Rp10.000 | Minimal Rp5.000 (BR-01), maksimal Rp25.000.000 per transaksi |
| Tab Tagihan, Top Up, Virtual Account, QRIS | **Top Up** (isi saldo, seperti sebelumnya), Pulsa, Token PLN, Tagihan, E-wallet, QRIS. **Virtual Account dan Paket Data dihapus** |
| Riwayat: filter jenis dan tanggal | Ditambah pencarian, paginasi, jurnal pada detail, tombol Cek status (antarbank), ekspor CSV/PDF |
| Profil: ubah langsung | Ubah profil dikonfirmasi OTP |
| Lupa sandi (password) | Lupa **password atau PIN** |

## 4. Pemetaan halaman ke endpoint backend
| Halaman | Endpoint |
|---|---|
| Masuk | `POST /auth/login` |
| Daftar + OTP | `POST /auth/register`, `/auth/register/verify-otp`, `/auth/register/resend-otp` |
| e-KYC, Buat PIN | `GET/POST /auth/ekyc`, `POST /auth/pin`, `GET /me` |
| Lupa password/PIN | `POST /auth/recovery/request`, `/verify`, `/reset` |
| Dashboard | `GET /accounts/me`, `GET /history?size=5` |
| Transfer | `GET /transfers/banks`, `POST /transfers/inquiry`, `/transfers/quote`, `POST /transfers` |
| Pembayaran | `GET /topup/catalog`, `POST /topup/quote`, `/topup/orders`, `GET /qris/demo`, `POST /qris/scan`, `/qris/pay` |
| Top Up saldo | `POST /qris/simulate-incoming` (lihat catatan di bawah) |
| Riwayat | `GET /history`, `/history/{id}`, `/history/export`, `POST /transfers/{id}/check-status` |
| Profil | `GET /me`, `POST /me/profile-change`, `/me/profile-change/confirm`, `POST /me/password` |
| Keluar | `POST /auth/logout` |

## 5. Top Up saldo, saldo awal, QRIS, dan Paket Data
- **Top Up (isi saldo)**: Sumber Dana (Kartu Debit / Transfer Bank / Minimarket) + nominal Rp1.000 – Rp10.000.000, tanpa PIN.
  Backend belum punya endpoint isi-saldo, jadi Web memakai simulator dana masuk `POST /qris/simulate-incoming`.
  Akibatnya di Riwayat transaksi ini bernama **"Terima dana QRIS"** (kategori QRIS), dengan pengirim "TOP UP <sumber>".
  Cara yang benar adalah menambah endpoint top-up di backend.
- **Saldo awal Rp0** untuk akun baru: lihat `backend-patch/BACA_DULU.md` (perlu mengubah backend; Web tidak bisa mengubah saldo sendiri).
- **Gambar QRIS** di tab QRIS adalah `public/assets/img/qris.png`. Isi QR pada gambar itu adalah tautan
  `https://q.me-qr.com/mfyw2m2j`, **bukan** kode QRIS yang dikenali backend, jadi hanya tampilan.
  Untuk simulasi pembayaran, pilih "Contoh QRIS" dari backend pada form.
- **Paket Data** dihapus dari Web. Backend dan Android masih memilikinya; catatan: FR-18 di SRS (Must) menyebut pulsa **dan** paket data.

## 6. Belum ada di Web Node ini (sudah ada di backend dan Web React)
Kantong simpanan, Virtual Card, Notifikasi, penerima favorit, grafik saldo, dan QRIS pribadi/dinamis milik sendiri.
Menunya tidak ada di Web ini; backend sudah menyediakan endpoint-nya bila ingin ditambah.

## 7. Pengujian
- `npm run test:lib` menguji klien API (`lib/backend.js`) terhadap backend tiruan, tanpa perlu backend asli.
- Alur halaman sudah diuji terhadap **backend tiruan** (bukan backend asli). Uji pertama kali dengan backend asli
  masih perlu dilakukan; catat kolom/respons yang berbeda dari dugaan.

## 8. Masalah umum
| Gejala | Penyebab / solusi |
|---|---|
| "Tidak dapat terhubung ke backend" | Backend belum jalan atau `BACKEND_URL` salah. Jalankan Terminal 1 dulu |
| Login selalu salah | Pastikan memakai username `demo` (bukan email) dan `Demo@1234` |
| Transfer: "PIN salah / diblokir" | 3 kali salah → PIN diblokir 5 menit. Pakai akun `budi` atau tunggu |
| Kembali ke halaman Masuk setelah restart Web | Sesi disimpan di memori; login ulang |
| Data demo berantakan | Matikan backend, hapus folder `code_area\backend\data`, jalankan ulang `.\run-local.ps1 -H2` |

## Folder
`server.js` titik masuk · `lib/backend.js` klien REST ke backend · `lib/helpers.js` format dan bantuan ·
`routes/auth.js` masuk, daftar, e-KYC, PIN, pemulihan · `routes/app.js` dashboard, transfer, pembayaran, riwayat, profil ·
`views/` template EJS · `public/assets/` css, logo, gambar QRIS · `test/` uji klien API · `backend-patch/` patch saldo awal backend

## 6. Tombol mata pada kata sandi / PIN

Semua kolom kata sandi dan PIN (login, daftar, buat PIN, lupa sandi, ubah sandi, konfirmasi PIN transfer/pembayaran) kini punya ikon mata di sisi kanan. Klik untuk menampilkan, klik lagi untuk menyembunyikan. Skrip: `public/assets/js/pwtoggle.js` (tanpa dependensi); gaya di akhir `style.css`.
