# Review D05W revisi 4 (v0.7): untuk Nabila

Dokumen yang direview: `revisi 4/DDB_D05W_User_Interface_Map_Web_v0.7.pdf` (v0.7, 22 Nov 2026)
Reviewer: Salomo (PM), 22 Nov 2026
Acuan: kode Web tim `digitalbanking-node` v2.0.0 (dari `SE-web.rar`), SRS-DDB-001 v0.1

---

## 1. Kesimpulan singkat

**Arahnya benar, Nabila.** Dokumen sekarang memetakan Web yang memang dibuat tim, dan jujur soal fitur yang belum ada. Yang tersisa: **tangkapan layar**, menutup butir "Perlu Ditentukan" yang sudah ada jawabannya (bagian 2), dan menaikkan ke v1.0.

**Tenggat: 23 November 2026 pukul 23.59 WIB.**

---

## 2. Jawaban untuk daftar keputusan (Bagian 7.1)

| ID | Keputusan / jawaban |
|---|---|
| D-01 | **Ya.** Web Node/Express/EJS menjadi Web resmi (keputusan PM, 22 Nov 2026; ADR-13 di SAD-DDB-001 v1.0). Web React menjadi prototipe pembanding |
| D-02 | Kode **WEB-01 s.d. WEB-11** dipakai untuk Web Node. Kode UI-xx hanya untuk Web React |
| D-03 | Ikuti kode: form pendaftaran **tidak** memuat nama. Nama diisi (opsional) di e-KYC |
| D-04 | Backend menerima **username, email, atau nomor HP**. Label "Email / No. HP" sebaiknya menjadi "Username / Email / No. HP" |
| D-05 | Simulasi. Backend hanya menerima `selfieReference` (teks), tidak ada unggah berkas |
| D-06 | Minimal Rp5.000: terbukti (WN-04). PIN salah ditolak: terbukti (WN-04). **Maksimal Rp25.000.000 dan kunci akun 15 menit belum diuji lewat Web**; tulis sebagai aturan backend |
| D-07 | Catat sebagai **parsial**: paket data belum ada di Web, tersedia di Android |
| D-08 | Build acuan: `digitalbanking-node` v2.0.0. Tangkapan layar diambil bersama Permata |
| D-09 | **Sudah diuji dengan backend asli:** 14 dari 14 alur inti lulus (WN-01–WN-14) dan 3 dari 3 skenario lintas platform dengan Android (LPN-01–LPN-03). Bukti: `code_area/pengujian/bukti/2026-11-22/web-node/` dan `lintas-platform-node/` |
| D-10 | Pakai `backend/docs/openapi.json` (73 operasi) yang ikut di `SE-web.rar` |

---

## 3. Yang harus dikerjakan

### 🔴 A. Tangkapan layar (wajib)
Semua kartu WEB-01 s.d. WEB-11 masih kosong. Cara menjalankan ada di `log.txt` dan README. Akun: `demo` / `Demo@1234`, PIN `123456`.

### 🔴 B. Ganti teks yang sudah tidak berlaku
| Bagian | Ganti menjadi |
|---|---|
| Status "Basis penyusunan sementara…" | "Final: Web Node/Express/EJS ditetapkan sebagai Web resmi oleh PM pada 22 Nov 2026" |
| 1.3 "uji penuh dengan backend asli masih perlu dilakukan" | "Alur inti diuji dengan backend asli pada 22 Nov 2026: WN 14/14, LPN 3/3 (STD-DDB-001 v1.0)" |
| 5, kolom Status bukti "perlu verifikasi integrasi" | Sebut ID uji yang sesuai (tabel di bawah) |
| Bagian 7.2 dan 7.3 | Hapus setelah gambar disisipkan |

| Halaman | ID uji |
|---|---|
| WEB-01 | WN-01, WN-02, WN-14 |
| WEB-02, 03, 04, 05 | WN-13 |
| WEB-06 | WN-01, LPN-01 |
| WEB-07 | WN-03, WN-04, WN-05, WN-06 |
| WEB-08 | WN-07 – WN-11, LPN-02 |
| WEB-09 | WN-06 (cek status), WN-12 (ekspor) |
| WEB-10, WEB-11 | **Belum diuji otomatis**; tulis "ada pada kode" |

### 🟡 C. Cek ulang
- WEB-01: kolom form bernama `identitas`, bukan `identifier` (`routes/auth.js`). `identifier` adalah nama yang dikirim ke backend.
- WEB-04: field form `nik`, `fullName`, `selfie`.
- FR-25: tulis "belum tersedia" saja, tanpa "hanya jika sesuai definisi SRS".

### 🟢 D. Kirim
- Berkas **.docx dan .pdf**, versi **1.0**, riwayat revisi ditambah satu baris.

---

## 4. Catatan untuk tim Web
- `SE-web.rar` berisi `.env`, `node_modules`, dan database H2. **Jangan unggah ke GitHub.** Kirim kode tanpa tiga hal itu.
- `opening-balance.patch` opsional dan **tidak** dipasang di backend bersama, jadi saldo awal akun baru tetap Rp5.000.000.
- D04W, D07W, dan D09W masih menjelaskan Web React atau belum ada. Rafi, Steven, dan Permata perlu menyesuaikannya.

Terima kasih, Nabila. Tinggal gambar dan penutupan butir terbuka. 🙏
