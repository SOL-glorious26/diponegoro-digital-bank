# Review D05W User Interface Map Web: untuk Nabila

Dokumen yang direview: `USER INTERFACE MAP (WEB) (2).pdf` (v0.3, 11 Nov 2026)
Reviewer: Salomo (PM), 16 Nov 2026
Acuan: template dosen Dokumen 5, DWP-DDB-001 v1.3 (tugas D05W-WP1 s.d. WP4, Definition of Done), D03 v0.9.3, D04W v0.5, dan kode `code_area/apps/web`

---

## 1. Kesimpulan singkat

**Catatan penting:** berkas yang kamu kirim hari ini **sama persis** (identik byte per byte) dengan v0.3 tanggal 11 Nov yang sudah ada di repo. Jadi belum ada perubahan sejak 11 Nov. Review di bawah ini berlaku untuk v0.3.

**Status: dasar sudah bagus, tapi isinya tertinggal dari aplikasi.** Waktu v0.3 ditulis (11 Nov), aplikasi Web belum terhubung ke backend, jadi banyak bagian diberi `[isi]`. Sekarang **aplikasi Web sudah jadi dan lulus uji 15/15**, sehingga semua `[isi]` sudah bisa diisi dari kode.

Tenggat akhir seluruh dokumen: **24 Nov 2026, 23.59 WIB**. Pre-review R1 dilakukan **Rafi**, review R2 oleh **Salomo**.

| Bagian wajib template | Ada? | Kondisi |
|---|---|---|
| Blok kontrol dokumen | ✅ | Lengkap. Ada sisa baris kosong `[isi]` di tabel revisi |
| Daftar isi | ✅ | Ada |
| User Interface Map | ⚠️ | Inventaris dan rincian sudah bagus. Belum ada rute, diagram, dan tangkapan layar |

---

## 2. Yang sudah bagus 👍

- Acuan sudah benar: kembali ke **SRS-DDB-001**. Bagian 6.2 dengan tepat menolak OTP saat login, OTP saat transfer, Virtual Account, dan pindai QRIS di Web.
- Kode halaman **UI-01 s.d. UI-21** rapi dan sudah dipakai dokumen lain (D01, D02, D04W). **Jangan diganti**, supaya dokumen lain tetap konsisten.
- Rincian per halaman (elemen, aksi, kondisi khusus A1–A6) sudah sesuai SRS dan D03.
- Batasan platform jelas: FR-15 dan biometrik tidak ada di Web, FR-26 hanya di Web.

---

## 3. Yang harus diperbarui

### 🔴 A. Isi semua `[isi]` dari aplikasi yang sudah jadi

Rute sebenarnya di `apps/web/src/main.jsx` (lihat juga D03 v0.9.3 Tabel 4.1):

| Halaman | Rute | Endpoint utama (D03 Tabel 4.2) |
|---|---|---|
| UI-01 Login | `/login` | `POST /auth/login`, `/auth/refresh`, `/auth/logout` |
| UI-02, UI-03 Registrasi + OTP | `/daftar` | `POST /auth/register`, `/auth/register/verify-otp`, `/auth/register/resend-otp` |
| UI-04 Lupa Password/PIN | `/lupa` | `POST /auth/recovery/request`, `/verify`, `/reset` |
| UI-05 e-KYC | `/ekyc` | `GET` dan `POST /auth/ekyc` |
| UI-06 Buat PIN | `/pin/buat` | `POST /auth/pin` |
| UI-07 Dashboard | `/` | `GET /accounts/me`, `/accounts/me/balance-chart`, `/history/monthly-summary` |
| UI-08 s.d. UI-11 Transfer | `/transfer` (satu halaman bertahap + dialog PIN) | `GET /transfers/banks`, `POST /transfers/inquiry`, `/transfers/quote`, `/transfers`, `/transfers/{txId}/check-status`, favorit `/transfers/favorites` |
| UI-12 QRIS Pribadi | `/qris` | `GET /qris/my-qr` (dengan `amount` = QRIS dinamis) |
| UI-13 s.d. UI-15 Top-up | `/topup` | `GET /topup/catalog`, `/topup/data-packages`, `/topup/pln/inquiry`, `/topup/bills/inquiry`; `POST /topup/quote`, `/topup/orders` |
| UI-16, UI-17 Riwayat + Detail | `/riwayat` | `GET /history`, `/history/{txId}`, `/history/export` |
| UI-18 Profil | `/profil` | `GET /me`, `POST /me/profile-change`, `/me/profile-change/confirm`, `PUT /me/photo` |
| UI-19 Keamanan dan Notifikasi | `/profil` (bagian keamanan) dan `/notifikasi` | `POST /me/password`, `/me/pin`, `GET/PUT /me/notification-settings`, `GET /me/notifications` |
| UI-20 Kantong | `/kantong` | `/accounts/pockets/**` |
| UI-21 Virtual Card | `/kartu` | `/cards/**` |

Catatan: halaman **`/notifikasi`** (kotak notifikasi, FR-29) belum punya kode sendiri. Usul: masukkan ke UI-19 atau tambahkan **UI-22 Kotak Notifikasi**. Kode lama jangan diubah.

### 🔴 B. UI-20 dan UI-21 bukan lagi "menunggu konfirmasi"

**Kantong dan Virtual Card sudah dikerjakan di Web v1** (D02 v0.9.5 Tabel 8.3) dan sudah ada di `/kantong` dan `/kartu`. Kantong juga lulus uji WB-15. Pindahkan dari bagian 5.8 ke bagian biasa, dan hapus pertanyaan 6.3 no. 2.

### 🔴 C. Kalimat yang sudah tidak benar

- 1.3: *"Dokumen ini tidak menyatakan bahwa suatu halaman sudah diimplementasikan"*. Sekarang semua halaman **sudah** diimplementasikan dan diuji (D06 v0.9.6: Web 15/15).
- 2: *"Endpoint API diambil dari draf koordinasi tim Web untuk Sprint 1"*. Ganti menjadi: endpoint mengikuti **D04B / openapi.json (73 operasi)**.
- 6.1 no. 4: *"Sprint 1 memakai akun demo… UI-05 dijadwalkan sprint berikutnya"*. UI-05 e-KYC **sudah ada** dan diuji.
- 1.2: Safari disebut, padahal yang diuji **Chrome, Edge, dan Firefox** (D06 NF-09). Tulis Safari sebagai "belum diuji".

### 🔴 D. Bagian yang diminta DWP tapi belum ada

1. **Diagram navigasi.** Sekarang peta navigasi masih berupa daftar. DWP D05W-WP2-T01 meminta **peta global + 8 peta per modul**, termasuk **route guard** (login → e-KYC disetujui → PIN sudah dibuat). Lihat D04W v0.5 (Rafi): ada diagram route guard yang bisa dijadikan acuan.
2. **Kartu halaman dengan tangkapan layar** (D05W-WP3-T01, dibantu **Permata**).
3. **Baris kosong `[isi] [isi] [isi] [isi]`** di tabel revisi harus dihapus.

---

## 4. Bahan yang sudah tersedia (bisa langsung dipakai) 📸

Tangkapan layar asli Web hasil uji ada di `code_area/pengujian/bukti/2026-11-11/`, misalnya:
`01-login.png`, `02-beranda.png`, `03-transfer-konfirmasi.png`, `04-pin.png`, `05-bukti-transfer.png`, `06-riwayat.png`, `07-detail.png`, `08-topup-pln.png`, `09-qris.png`, `10-profil.png`, `14-ekyc.png`, `WB13-1-grafik-harian.png`, `WB15-kantong-diubah-dikunci.png`.

Cara menjalankan Web sendiri untuk mengambil gambar baru: `TUTORIAL_MENJALANKAN_WEB.md` di folder utama repo (akun demo: `demo` / `Demo@1234`, PIN `123456`). Untuk versi final, ambil dari **build baseline 18 Nov**.

---

## 5. Checklist sebelum kirim ke Rafi (R1)

- [ ] Semua `[isi]` rute dan endpoint diisi (tabel di atas)
- [ ] UI-20 dan UI-21 dipindah ke halaman biasa; pertanyaan 6.3 no. 2 dihapus
- [ ] Halaman `/notifikasi` dimasukkan (UI-19 atau UI-22)
- [ ] Kalimat usang di 1.2, 1.3, 2, dan 6.1 diperbaiki
- [ ] Diagram navigasi global + per modul, termasuk route guard
- [ ] Kartu halaman dengan tangkapan layar (bersama Permata)
- [ ] Baris kosong di tabel revisi dihapus; versi dinaikkan (misalnya v0.5) dan dicatat
- [ ] Kirim juga **berkas sumber (.docx)**, bukan hanya PDF, supaya mudah direvisi dan diberi tanda tangan saat final

Terima kasih, Nabila! Dokumenmu sudah paling dekat ke selesai di antara dokumen UI. 🙏
