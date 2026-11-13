# 📘 Panduan Proyek Diponegoro Digital Bank

**Untuk seluruh anggota Kelompok Jaran Goyang.** Baca dari atas ke bawah, kira-kira 15 menit.
Diperbarui: **14 November 2026**, oleh Salomo (PM).

Daftar isi:
1. [Proyek ini apa?](#1-proyek-ini-apa)
2. [Gambaran besar: 3 bagian aplikasi](#2-gambaran-besar-3-bagian-aplikasi)
3. [Teknologi yang dipakai](#3-teknologi-yang-dipakai)
4. [API itu apa, dan bagaimana cara kerjanya?](#4-api-itu-apa-dan-bagaimana-cara-kerjanya)
5. [Cara kerja aplikasi: contoh alur transfer](#5-cara-kerja-aplikasi-contoh-alur-transfer)
6. [Daftar fitur](#6-daftar-fitur)
7. [Progres proyek: apa yang sudah dan belum](#7-progres-proyek-apa-yang-sudah-dan-belum)
8. [Dokumen proyek](#8-dokumen-proyek)
9. [Cara mencoba aplikasinya sendiri](#9-cara-mencoba-aplikasinya-sendiri)
10. [Kamus istilah](#10-kamus-istilah)

---

## 1. Proyek ini apa?

**Diponegoro Digital Bank (DDB)** adalah aplikasi bank digital, mirip aplikasi mobile banking yang biasa kita pakai.
Nasabah bisa daftar akun, cek saldo, transfer, bayar QRIS, beli pulsa, membuat kantong tabungan, dan sebagainya.

- **Periode proyek:** 15 September – 23 November 2026
- **Tenggat dokumentasi:** **24 November 2026, pukul 23.59 WIB**
- **Acuan utama:** SRS (Software Requirements Specification) v0.1 di `documents_area/00_Referensi/`.
  SRS menjadi "kitab" proyek ini karena semua fitur dan aturan berasal dari sana.

> 💡 Bank, QRIS, dan biller (PLN, operator pulsa) di proyek ini **hanya simulasi**. Tidak ada uang sungguhan.

---

## 2. Gambaran besar: 3 bagian aplikasi

Aplikasi ini terdiri dari **tiga bagian** yang bekerja sama:

```
   📱 Aplikasi Android            🌐 Aplikasi Web
   (di HP nasabah)                (di browser nasabah)
           \                          /
            \     permintaan lewat   /
             \    internet (API)    /
              v                    v
           ┌──────────────────────────┐
           │  🚪 API Gateway (Nginx)  │  ← "satpam" pintu masuk
           └────────────┬─────────────┘
                        v
           ┌──────────────────────────┐
           │  ⚙️ Backend (Spring Boot)│  ← "otak": semua aturan & hitungan
           └────────────┬─────────────┘
                        v
           ┌──────────────────────────┐
           │ 🗄️ Database (PostgreSQL)  │  ← "lemari arsip": semua data disimpan
           └──────────────────────────┘
```

| Bagian | Tugasnya | Analogi |
|---|---|---|
| **Android & Web** (disebut *frontend* atau *klien*) | Menampilkan layar, menerima ketikan dan klik nasabah | **Kasir/teller** yang melayani nasabah |
| **API Gateway** | Pintu masuk tunggal. Membatasi permintaan berlebihan (misalnya tebak password berkali-kali). | **Satpam** di pintu bank |
| **Backend** | Mengecek PIN, menghitung saldo, memindahkan uang, mencatat riwayat | **Ruang belakang bank**, tempat semua keputusan diambil |
| **Database** | Menyimpan akun, saldo, transaksi secara permanen | **Lemari arsip / brankas** |

**Kenapa backend-nya cuma satu?** Supaya data Android dan Web **selalu sama**.
Kalau kamu transfer dari HP, riwayatnya langsung muncul juga di Web karena keduanya bertanya ke "otak" yang sama.

---

## 3. Teknologi yang dipakai

| Bagian | Bahasa / alat | Penjelasan singkat |
|---|---|---|
| 📱 Android | **Kotlin**, **Jetpack Compose**, pola **MVVM** + Repository | Kotlin adalah bahasa resmi Android. Compose dipakai untuk membuat tampilan dengan kode. MVVM memisahkan *tampilan* (View), *logika layar* (ViewModel), dan *data* (Model/Repository) supaya rapi. |
| 📱 Android → server | **Retrofit + OkHttp** | Pustaka untuk "menelepon" API backend dari HP |
| 🌐 Web | **React** (JavaScript), **Vite**, **react-router**, **axios** | React untuk membuat halaman web interaktif. Vite menjalankan dan membangun proyek. axios dipakai untuk memanggil API. |
| ⚙️ Backend | **Java 21**, **Spring Boot 4** | Framework Java populer untuk membuat server |
| 🗄️ Database | **PostgreSQL** (+ **Flyway**). Untuk uji cepat bisa pakai **H2**. | Flyway membuat tabel database otomatis. H2 adalah database "mainan" di memori, jadi tidak perlu instal apa pun. |
| 🔐 Keamanan | **JWT**, **BCrypt**, **AES-GCM**, HTTPS | JWT = "tiket masuk" setelah login. BCrypt = cara menyimpan password/PIN supaya tidak bisa dibaca. AES-GCM = enkripsi data kartu. |
| 🚪 Gateway | **Nginx** | Pintu masuk dan pembatas permintaan |
| 📦 Deployment | **Docker** + docker-compose | "Kotak" berisi backend, database, dan gateway yang bisa dijalankan dengan satu perintah |
| 🧪 Pengujian | JUnit (backend), test Android, skrip Node.js (uji end-to-end) | Uji otomatis supaya kita yakin fiturnya benar |

---

## 4. API itu apa, dan bagaimana cara kerjanya?

### Analogi restoran 🍽️

Bayangkan sebuah restoran:
- **Kamu (pelanggan)** = aplikasi Android/Web
- **Dapur** = backend + database
- **Pelayan + buku menu** = **API**

Kamu tidak masuk ke dapur dan memasak sendiri. Kamu memilih dari **menu** dan menyampaikannya ke **pelayan**.
Pelayan membawa pesananmu ke dapur, lalu kembali membawa makanannya.

**API (Application Programming Interface)** adalah "menu + pelayan" itu: **daftar permintaan yang boleh diajukan aplikasi ke server, beserta format jawabannya.**
Aplikasi Android dan Web tidak pernah menyentuh database langsung. Semua permintaan **selalu lewat API**.

### Satu permintaan API terdiri dari apa?

```
POST  /api/v1/transfers                          ← (1) Metode + alamat (endpoint)
Authorization: Bearer eyJhbGciOi...              ← (2) Tiket login (JWT)
Idempotency-Key: 7f3a-...                        ← (3) Kode unik anti-transfer-ganda
{                                                ← (4) Isi pesan (body), format JSON
  "toAccountNumber": "1400000002",
  "amount": 10000,
  "pin": "123456",
  "note": "bayar makan"
}
```

1. **Metode**, yaitu jenis permintaannya:
   - `GET` = **minta data** (misal: lihat saldo)
   - `POST` = **kirim/buat sesuatu** (misal: transfer, login)
   - `PUT` = **ubah data** (misal: ubah profil)
   - `DELETE` = **hapus** (misal: hapus kantong)
2. **Endpoint** = alamat "menu"-nya, contoh `/api/v1/accounts/me` = "rekening saya".
3. **Header** = info tambahan, misalnya tiket login.
4. **Body** = isi pesan dalam format **JSON** (teks berbentuk `"nama": nilai`).

### Jawaban dari server

Server membalas dengan **kode status** + isi JSON:

| Kode | Artinya |
|---|---|
| `200` / `201` | ✅ Berhasil |
| `400` | ❌ Data yang dikirim salah (misal nominal kosong) |
| `401` | 🔒 Belum login / tiket login kedaluwarsa |
| `403` | ⛔ Tidak boleh (misal PIN sedang diblokir) |
| `404` | ❓ Tidak ditemukan (misal nomor rekening salah) |
| `409` / `422` | ⚠️ Ditolak aturan bisnis (misal saldo tidak cukup) |

Contoh jawaban gagal:
```json
{ "code": "INSUFFICIENT_BALANCE", "message": "Saldo tidak mencukupi" }
```
Kolom `message` langsung ditampilkan ke nasabah, sedangkan `code` dipakai oleh program.

### Aturan penting API proyek ini

1. **Login dulu.** Setelah login, server memberi dua token:
   **access token** (berlaku 15 menit, dipakai di setiap permintaan) dan **refresh token** (berlaku 7 hari, dipakai untuk minta access token baru).
   Aplikasi memperbarui access token secara otomatis, jadi nasabah tidak perlu login ulang setiap 15 menit.
2. **Setiap transaksi uang wajib mengirim `Idempotency-Key`.** Kalau sinyal putus lalu aplikasi mengirim ulang, server tahu itu permintaan yang **sama**, jadi uang **tidak terpotong dua kali**.
3. **Setiap transaksi dikonfirmasi PIN 6 digit.** Salah 3× berturut-turut membuat PIN diblokir 5 menit.
4. Uang selalu **bilangan bulat rupiah** (tanpa koma). Waktu memakai format standar ISO dan ditampilkan dalam WIB.

### Daftar lengkap API

Ada **73 endpoint**. Daftar lengkapnya ada di [`code_area/backend/docs/API.md`](code_area/backend/docs/API.md).
Saat backend berjalan, buka **http://localhost:8080/swagger-ui.html**. Di sana ada halaman interaktif untuk mencoba setiap API langsung dari browser.

---

## 5. Cara kerja aplikasi: contoh alur transfer

Misalkan nasabah **demo** mentransfer Rp10.000 ke **Andi**:

1. 📱 Nasabah membuka aplikasi, lalu **login** → aplikasi memanggil `POST /auth/login` → server mengecek password dan memberi token.
2. 📱 Nasabah membuka menu Transfer dan mengetik nomor rekening Andi → aplikasi memanggil API **cek rekening** → server menjawab "a.n. ANDI".
3. 📱 Nasabah mengisi nominal, menekan *Lanjut*, lalu memasukkan **PIN**.
4. 📱 Aplikasi mengirim `POST /transfers` beserta token, PIN, dan `Idempotency-Key`.
5. ⚙️ Backend melakukan pemeriksaan berurutan:
   - Apakah token masih berlaku? → kalau tidak: 401
   - Apakah PIN benar? → kalau salah: tolak, hitung percobaan salah
   - Apakah saldo cukup dan belum melewati batas harian? → kalau tidak: tolak
   - Apakah `Idempotency-Key` ini pernah dipakai? → kalau ya: kembalikan hasil lama, **jangan transfer lagi**
6. 🗄️ Backend **memindahkan uang secara atomik**, artinya *semua berhasil atau semua batal*:
   saldo demo −10.000 dan saldo Andi +10.000 dicatat dengan sistem **double-entry** (pembukuan berpasangan, seperti akuntansi).
   Tidak mungkin terjadi uang hilang di tengah jalan.
7. ⚙️ Backend mencatat riwayat dan mengirim **notifikasi** ke kedua nasabah.
8. 📱 Aplikasi menampilkan **bukti transfer**. Andi bisa melihat uangnya masuk, **baik di Android maupun di Web.**

Semua fitur lain mengikuti pola yang sama: **layar → panggil API → backend cek aturan → simpan di database → kirim jawaban → layar menampilkan hasil.**

---

## 6. Daftar fitur

Kode **FR** (Functional Requirement) berasal dari SRS. Kolom PIC menunjukkan pemilik modul di tim Android/backend.

| Modul | FR | Fitur | PIC | Android | Web |
|---|---|---|---|:-:|:-:|
| 🔑 **Authentication** | FR-01 – 05 | Daftar akun + OTP, e-KYC (verifikasi KTP, simulasi), login (password / biometrik di Android), buat & verifikasi PIN, logout semua perangkat, lupa password/PIN | Salomo | ✅ | ✅ |
| 💰 **Account** | FR-06 – 09 | Lihat & sembunyikan saldo, grafik saldo, **kantong** tabungan + kunci kantong sampai tanggal/target | Grace | ✅ | ✅ |
| 💸 **Transfer** | FR-10 – 14 | Transfer sesama DDB & antarbank, cek nama penerima, penerima favorit, bukti transfer | Cecil | ✅ | ✅ |
| 📷 **QRIS** | FR-15 – 17 | Scan & bayar QRIS (Android), QRIS pribadi, QRIS dinamis | Cia | ✅ | ✅ (QRIS pribadi saja) |
| 📶 **Top-up** | FR-18 – 20 | Pulsa, paket data, token PLN, tagihan, e-wallet | Grace | ✅ | ✅ |
| 💳 **Virtual Card** | FR-21 – 22 | Buat kartu virtual, lihat detail (pakai PIN), bekukan/aktifkan, atur limit | Salomo | ✅ | ✅ |
| 📜 **History & Dashboard** | FR-23 – 26 | Riwayat + filter + pencarian, detail transaksi, grafik pengeluaran bulanan, ekspor CSV/PDF | Cecil | ✅ | ✅ |
| 👤 **Profile & Notifikasi** | FR-27 – 29 | Ubah profil (pakai OTP), foto profil, keamanan, pengaturan & kotak notifikasi | Cia | ✅ | ✅ |

**Keputusan PM yang sudah ditetapkan:**
- Login **tanpa OTP**. OTP hanya dipakai saat daftar, ubah profil, dan pemulihan akun.
- Semua transaksi memakai **PIN + Idempotency-Key**.
- **Virtual Account tidak dibuat.**
- Di Web, QRIS hanya **menampilkan QRIS pribadi**. Scan QRIS hanya ada di Android karena butuh kamera.

---

## 7. Progres proyek: apa yang sudah dan belum

> **Ringkasnya: aplikasinya SUDAH JADI dan sudah diuji. Yang masih kurang adalah DOKUMEN bagian anggota lain dan beberapa pengujian akhir.**

### ✅ Sudah selesai

| Bagian | Status |
|---|---|
| ⚙️ Backend | Semua 8 modul (FR-01 – FR-29) selesai, 73 endpoint API, **47 test otomatis lulus**, cakupan test **92,3%** (target SRS ≥ 70%) |
| 🗄️ Database | PostgreSQL berjalan: 18 tabel + data demo |
| 📱 Android | 8 modul tim digabung jadi satu aplikasi dan terhubung ke backend. Ada 2 versi: `demo` (offline) dan `online` (pakai server). |
| 🌐 Web | Semua halaman selesai dan terhubung ke backend yang sama |
| 🧪 Uji sistem | Android **27/28** lulus · Web **15/15** · Lintas platform Android↔Web **4/4** · **11 bug ditemukan dan semuanya sudah diperbaiki** |
| 📄 Dokumen | D00 (Work Plan), D01, D02, D03, D04B, D05W (UI Map Web dari Nabila), D06, D07A, D07B, Panduan Uji di Ponsel. Semua masih versi **0.9.x** (draf, menunggu review). Ditambah draf awal **D04W** dan **D07W** v0.5 untuk dilengkapi PIC Web. |

### ⏳ Belum selesai: perlu dikerjakan

| # | Pekerjaan | Siapa | Catatan |
|---|---|---|---|
| 1 | **D04W**: Detailed Design Web | **Rafi** | 🟡 **Draf v0.5 sudah ada** (disiapkan PM dari kode). Tugasmu: baca sambil mencocokkan dengan kode di `code_area/apps/web`, lengkapi, lalu naikkan versi. |
| 2 | **D07W**: Developer Manual Web | **Steven** (bagian komponen UI: **Nabila**) | 🟡 **Draf v0.5 sudah ada.** Steven: ikuti langkah Bab II–III di laptop sendiri dan catat hasilnya. Nabila: periksa Bab 5.3. |
| 3 | **D04A**: Detailed Design Android | **Gracia** | ⏳ Belum ada. Contoh format: D04B dan D04W. |
| 4 | **D05A**: User Interface Map Android | **Cecilia** | ⏳ Belum ada. Contoh format: D05W milik Nabila. |
| 5 | **D08**: System Program Demo | **Rafi** | ⏳ Belum ada |
| 6 | **D09A**: User Manual Android | **Grace** | ⏳ Belum ada |
| 7 | **D09W**: User Manual Web | **Permata** | ⏳ Belum ada |
| 8 | Review ke-2 untuk D01, D02, D03, D04B, D07B | Steven | Menunggu |
| 9 | Uji di HP fisik: scan QRIS pakai kamera (SA-08) dan NF-07 | Salomo | Satu-satunya uji yang masih tertunda |
| 10 | Uji gateway + rate limit (NF-04) | Salomo | Menunggu Docker terpasang |
| 11 | Deploy Web + backend ke server publik (HTTPS) | Salomo | Opsional; saat ini aplikasi dijalankan lokal |
| 12 | Semua dokumen dinaikkan ke **v1.0** (final + tanda tangan) | Salomo | Setelah semua review selesai |

📌 **Pembagian tugas resmi dan format dokumen** ada di Work Plan:
`documents_area/D00_DWP_Work_Plan/`. Template dari dosen ada di `documents_area/00_Referensi/PANDUAN_MODULE.docx`.

**Tips untuk yang menulis dokumen:** lihat dulu dokumen yang sudah jadi sebagai contoh format (misalnya D04B untuk Detailed Design dan D07A untuk Developer Manual).
Semua isi teknisnya sudah ada di kode, jadi kamu tidak perlu mengarang. Kalau bingung, tanya Salomo.

---

## 8. Dokumen proyek

Semua dokumen ada di [`documents_area/`](documents_area). Satu folder per volume, isinya **versi terbaru saja**.

| Folder | Dokumen | Versi |
|---|---|---|
| `00_Referensi/` | SRS v0.1, Project Brief, panduan modul dosen | – |
| `D00_DWP_Work_Plan/` | Rencana kerja dokumentasi (pembagian tugas) | v1.3 |
| `D01_Project_Document/` | Dokumen proyek (jadwal, risiko, biaya COCOMO) | v0.9.3 |
| `D02_Architectural_Design/` | Desain arsitektur | v0.9.4 |
| `D03_Functional_Design/` | Desain fungsional | v0.9.2 |
| `D04B_Detailed_Design_Backend_DB/` | Desain detail backend & database | v0.9.2 |
| `D05W_UI_Map_Web/` | Peta tampilan Web (Nabila) | v0.3 |
| `D06_System_Testing/` | Pengujian sistem | v0.9.5 |
| `D07A_Developer_Manual_Android/` | Manual developer Android | v0.9.2 |
| `D07B_Developer_Manual_Backend/` | Manual developer backend | v0.9.2 |
| `D04W_Detailed_Design_Web/` | Desain detail Web (draf untuk Rafi) | v0.5 |
| `D07W_Developer_Manual_Web/` | Manual developer Web (draf untuk Steven & Nabila) | v0.5 |
| `TUT_Panduan_Uji_Ponsel/` | Cara instal & uji aplikasi di HP | v1.1 |

**Aturan simpan dokumen:** simpan versi baru di folder volumenya. Kalau ada versi lama, beri tahu Salomo supaya dipindahkan dari folder.
Setelah membuka .docx di Word, klik kanan Daftar Isi lalu pilih **Update Field**.

---

## 9. Cara mencoba aplikasinya sendiri

**Yang perlu diinstal:** JDK 21+ (sudah termasuk di Android Studio), Node.js 20+, Android Studio (untuk Android).

```powershell
# 1. Jalankan backend (pakai database H2, tidak perlu instal PostgreSQL)
cd code_area\backend
.\run-local.ps1 -H2
#   → tunggu sampai muncul "Started", lalu buka http://localhost:8080/swagger-ui.html

# 2. Di terminal lain, jalankan Web
cd code_area\apps\web
npm install
npm run dev
#   → buka http://localhost:5173

# 3. Android: buka folder code_area\apps\android di Android Studio,
#    pilih varian "onlineDebug" lalu Run ke emulator.
#    (Varian "demoDebug" bisa jalan tanpa backend.)
```

**Akun demo** (dibuat otomatis):

| Username | Password | PIN | No. rekening |
|---|---|---|---|
| `demo` | `Demo@1234` | `123456` | 1400123456 (punya riwayat 5 bulan) |
| `andi` / `budi` / `citra` | `Demo@1234` | `123456` | 1400000002 / 03 / 04 |

Coba transfer dari `demo` ke `1400000002`, lalu login sebagai `andi` di Web. Uangnya pasti sudah masuk.

Penjelasan lebih teknis:
[backend](code_area/backend/README.md) · [Web](code_area/apps/web/README.md) · [Android](code_area/apps/android/README.md) · [pengujian](code_area/pengujian/README.md)

---

## 10. Kamus istilah

| Istilah | Arti sederhana |
|---|---|
| **Frontend / klien** | Bagian yang dilihat pengguna (aplikasi Android, Web) |
| **Backend / server** | Bagian di belakang layar yang memproses semua aturan dan data |
| **API** | "Menu + pelayan": cara resmi aplikasi meminta sesuatu ke server |
| **Endpoint** | Satu alamat API, misalnya `/api/v1/transfers` |
| **JSON** | Format teks untuk bertukar data: `{"nama": "Andi", "saldo": 50000}` |
| **JWT / token** | "Tiket masuk" digital yang didapat setelah login |
| **OTP** | Kode sekali pakai (di proyek ini ditampilkan langsung karena SMS/email masih simulasi) |
| **e-KYC** | Verifikasi identitas nasabah (KTP + selfie), di sini masih simulasi |
| **Idempotency-Key** | Kode unik per transaksi supaya kalau terkirim dua kali, uang tidak terpotong dua kali |
| **Double-entry** | Setiap perpindahan uang dicatat dua kali (keluar dari A, masuk ke B) supaya totalnya selalu seimbang |
| **Atomik** | Semua langkah berhasil, atau semuanya dibatalkan. Tidak ada setengah jadi. |
| **Modular monolith** | Satu program backend yang di dalamnya dibagi rapi per modul (auth, transfer, dll.) |
| **API Gateway** | Pintu masuk tunggal ke backend. Bisa membatasi permintaan berlebihan. |
| **Docker** | Alat untuk membungkus aplikasi + semua kebutuhannya supaya bisa dijalankan di komputer mana pun |
| **Flavor demo / online** | Dua versi aplikasi Android: `demo` memakai data palsu tanpa internet, `online` terhubung ke backend |
| **FR / NFR / BR** | Functional Requirement (fitur), Non-Functional Requirement (kualitas, misal kecepatan/keamanan), Business Rule (aturan bisnis) dari SRS |

---

*Ada pertanyaan? Hubungi Salomo (PM). Jangan ubah kode di branch `main` tanpa memberi tahu PM. Sebaiknya buat branch sendiri, lalu ajukan Pull Request.*
