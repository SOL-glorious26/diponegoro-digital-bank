# Review D05W revisi 1 (v0.5): untuk Nabila

Dokumen yang direview: `revisi 1/USER_INTERFACE_MAP_WEB_v0.5.docx` (v0.5, 16 Nov 2026)
Reviewer: Salomo (PM), 16 Nov 2026
Acuan: review sebelumnya (`REVIEW_D05W_Nabila.md`) dan kode `code_area/apps/web`

---

## 1. Kesimpulan singkat

**Kerja bagus, Nabila! 👏** Hampir semua catatan review pertama sudah kamu kerjakan. Tinggal **3 perbaikan kecil** dan **menyisipkan tangkapan layar**, setelah itu dokumen siap dikirim ke Rafi untuk pre-review R1.

| Catatan review pertama | Status |
|---|---|
| Semua `[isi]` rute dan endpoint diisi | ✅ Selesai (sudah aku cocokkan ke kode, benar) |
| UI-20 dan UI-21 jadi halaman biasa | ✅ Selesai |
| Halaman `/notifikasi` → UI-22 | ✅ Selesai, ID lama tetap |
| Kalimat usang (1.2, 1.3, 2, 6.1) | ✅ Selesai |
| Peta navigasi global + route guard + 8 peta modul | ✅ Selesai: 10 diagram, rapi dan mudah dibaca |
| Kartu halaman | ✅ Selesai: 21 kartu dengan rute, endpoint, elemen, aksi |
| Baris kosong di tabel revisi | ✅ Dihapus |
| Berkas .docx | ✅ Sudah dikirim |
| Tangkapan layar | ⏳ Belum disisipkan (lihat bagian 3) |

---

## 2. Yang masih perlu diperbaiki

### 🔴 A. Alur setelah OTP salah (Gambar 1, Gambar 3, dan kartu UI-02/UI-03)

Di dokumen: **OTP valid → kembali ke UI-01 Login.**
Di aplikasi (`pages/AuthPages.jsx`): setelah OTP valid, pengguna **langsung masuk** (token diterima) dan diarahkan ke **UI-05 e-KYC**. Pengguna tidak perlu login ulang.

Yang perlu diubah:
- **Gambar 1** dan **Gambar 3**: panah "OTP valid" dari UI-03 diarahkan ke **UI-05 e-KYC**, bukan ke UI-01.
- **Bagian 3.1**: kalimat *"registrasi, verifikasi OTP, kembali ke login, e-KYC…"* menjadi *"registrasi, verifikasi OTP (langsung masuk), e-KYC, pembuatan PIN, lalu dashboard"*.
- **Kartu UI-02/UI-03**, baris "Aksi dan tujuan": *"OTP valid → pengguna langsung masuk dan menuju UI-05"*.

### 🔴 B. Elemen e-KYC tidak sama dengan aplikasi (kartu UI-05)

Di dokumen: *"Formulir data KTP, unggah foto KTP, unggah atau ambil swafoto…"*
Di aplikasi: kolom **NIK (16 digit)**, kolom **Nama lengkap sesuai KTP**, dan **unggah Swafoto (JPG/PNG)**, plus penanda status. **Tidak ada unggah foto KTP.** Tambahkan juga catatan simulasi: *NIK berakhiran 0000 akan ditolak*.

### 🟡 C. Hapus catatan untuk diri sendiri

Di bawah Gambar 2 masih ada teks *"[Cocokkan Gambar 2 dengan diagram route guard pada D04W v0.5 (Rafi)…]"*. Sudah aku cek: **Gambar 2 sudah sesuai** dengan kode `auth/ProtectedRoute.jsx` (login → e-KYC disetujui → PIN sudah dibuat). Jadi catatan itu dan butir 6.3 no. 3 boleh dihapus.

### 🟢 D. Tambahan kecil supaya lengkap (tidak wajib)

| Kartu | Tambahkan |
|---|---|
| UI-01 Login | Kolom di aplikasi bertuliskan **"Username / email / nomor HP"** (bukan hanya username atau email) |
| UI-07 Dashboard | Endpoint `PUT /accounts/me/balance-visibility` (simpan pilihan sembunyikan saldo) dan `GET /history` (transaksi terakhir) |
| UI-11 Bukti Transfer | Endpoint `GET /history/{txId}` (data bukti diambil dari detail transaksi) |
| UI-12 QRIS | `POST /qris/simulate-incoming` [Simulator] untuk mencoba dana masuk |
| UI-18/UI-19 Profil | `GET /auth/sessions` dan tombol **keluar dari semua perangkat** (`POST /auth/logout-all`, FR-05) |
| UI-22 Notifikasi | `POST /me/notifications/{id}/read`, `POST /me/notifications/read-all`; jumlah belum dibaca (`GET /me/notifications/unread-count`) tampil di menu samping |
| Blok kontrol | Kode dokumen sesuai DWP: **UIM-DDB-001-WEB** |
| Daftar isi | Pakai daftar isi otomatis Word (*References → Table of Contents*) supaya ada nomor halaman |

---

## 3. Tangkapan layar yang bisa langsung dipakai 📸

Semua ada di `code_area/pengujian/bukti/2026-11-11/`. Untuk kartu yang masih bertulisan merah:

| Kartu | Usulan gambar |
|---|---|
| UI-02/UI-03 Registrasi + OTP | `15-pengguna-baru.png` (atau ambil baru) |
| UI-13 Pulsa dan Paket Data | `13-bukti-pulsa.png` |
| UI-15 Tagihan Rumah Tangga | `LP03-1-web-demo-ewallet.png` |
| UI-18 Profil (OTP ubah data) | `WB11-1-otp.png`, `WB11-2-profil-diperbarui.png`, `WB14-1-foto-tersimpan.png` |
| UI-01 sesi berakhir | `LP04-2-web-sesi-berakhir.png` |

Untuk UI-04, UI-06, UI-08, UI-19, UI-21, dan UI-22 belum ada gambarnya. Ambil baru bersama Permata dengan menjalankan Web (`TUTORIAL_MENJALANKAN_WEB.md`, akun `demo` / `Demo@1234`, PIN `123456`).

Tips: tangkapan layar yang sudah tersedia **langsung saja disisipkan** (sekarang masih berupa teks "[Sisipkan tangkapan layar: …]").

---

## 4. Checklist sebelum kirim ke Rafi (R1)

- [ ] Panah "OTP valid" di Gambar 1 dan 3 → UI-05; teks 3.1 dan kartu UI-02/03 disesuaikan
- [ ] Elemen kartu UI-05 diganti: NIK, nama sesuai KTP, swafoto
- [ ] Catatan "[Cocokkan Gambar 2…]" dan butir 6.3 no. 3 dihapus
- [ ] Tangkapan layar disisipkan (yang tersedia sekarang, sisanya diambil bersama Permata)
- [ ] (Opsional) tambahan endpoint di bagian 2D, kode dokumen, dan daftar isi otomatis
- [ ] Versi dinaikkan (misalnya v0.6) dan dicatat di Revision Control

Tinggal sedikit lagi! 🙏
