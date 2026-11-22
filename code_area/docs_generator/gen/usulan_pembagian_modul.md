# Usulan Pembagian Modul jika Proyek DDB Dimulai dari Awal

## 1. Tujuan Dokumen

Dokumen ini menjawab satu pertanyaan: **jika aplikasi Diponegoro Digital Bank (Android, Web, dan Backend) dibuat ulang dari awal, bagaimana sebaiknya modul dibagi kepada 8 anggota Kelompok Jaran Goyang?**

Dokumen ini adalah usulan perbaikan dan bahan pelajaran (*lessons learned*). Dokumen ini tidak mengubah pembagian kerja resmi pada MDTA-DDB-001 v0.2 maupun DWP-DDB-001 v1.5.

Acuan: SRS-DDB-001 v0.1 (8 modul bisnis, FR-01 s.d. FR-29), MDTA-DDB-001 v0.2, `code_area/submit_mobile/README.md`.

## 2. Apa yang Terjadi pada Pembagian Sebelumnya

| Aspek | Rencana | Realisasi |
|---|---|---|
| Android | 4 anggota, masing-masing PIC 2 modul | Anggota mengirim prototipe terpisah (±440 s.d. ±1.290 baris per orang). Aplikasi final (±10.800 baris) disatukan dan ditulis ulang oleh Salomo |
| Web | 4 anggota, masing-masing PIC 2 modul | Aplikasi tim Web (Node.js, Express, EJS): front end Steven, Nabila, dan Rafi; backend Web Surya dan Rafi. Diterima dan diuji 22 November 2026 |
| Backend dan Database | Slot BE-1 s.d. BE-4, nama belum ditentukan | Backend Spring Boot dan database bersama dikerjakan Salomo; backend Web (server Node) dikerjakan Surya dan Rafi |
| Git | Semua anggota bekerja di repositori | Commit pada repositori atas nama Salomo. Kiriman anggota lewat folder |

Penyebab utama:

1. **Tidak ada kerangka project bersama sejak awal.** Setiap anggota membuat proyek sendiri dengan nama package, teknologi UI (Jetpack Compose atau XML), dan sumber data (memori atau server) yang berbeda. Akibatnya kode tidak dapat langsung digabungkan.
2. **Pembagian per layer.** Tim Android dan Web menunggu Backend, dan Backend tidak punya pemilik. Masalah integrasi baru terlihat di akhir.
3. **Beban menumpuk pada satu orang.** Integrasi, Backend Spring Boot, dan Database jatuh ke PM. Jika PM berhalangan, seluruh proyek berhenti.
4. **Kontribusi anggota tidak terlihat di Git**, sehingga sulit dibuktikan saat penilaian.

## 3. Pilihan Cara Membagi Pekerjaan

| Cara | Artinya | Kelebihan | Kelemahan |
|---|---|---|---|
| Per layer | Tim Android, tim Web, tim Backend | Setiap orang fokus pada satu teknologi | Saling menunggu; integrasi terlambat; ini yang terjadi sebelumnya |
| Full-stack per orang | Satu orang membuat Android + Web + Backend untuk satu modul | Satu modul selesai utuh oleh satu orang | Terlalu berat untuk mahasiswa; login dan ledger akan dibuat berulang kali dengan cara berbeda |
| **Pasangan per modul + Tim Backend & Database (dipilih)** | Layar satu modul dipegang 1 orang Android + 1 orang Web; seluruh backend dan database dipegang tiga orang: Salomo, Rafi, dan Surya | Satu ledger dan satu skema database yang konsisten; pengetahuan backend tidak menumpuk pada satu orang; pemegang layar bisa mulai dengan mock server | Tiga orang memegang dua peran; kontrak API harus terkunci lebih dulu |

**Jawaban singkat:** tidak semua anggota menulis backend. Anggota lain fokus pada layar Android atau halaman Web modulnya. **Backend dan Database dikerjakan Salomo dari tim Android bersama Rafi dan Surya dari tim Web**, sehingga kedua tim klien terwakili saat kontrak API dan tabel dirancang.

## 4. Struktur Tim yang Diusulkan

### 4.1 Tim Backend & Database dan Tim Inti

**Tim Backend & Database: Salomo (Android), Rafi (Web), dan Surya (Web).** Tugasnya merancang skema database, membangun Backend Spring Boot beserta inti transaksi keuangan, menulis kontrak API, dan menguji backend. Salomo menjadi koordinator tim ini.

**Tim inti: Salomo, Rafi, Surya, dan Steven.** Tiga orang pertama membangun fondasi backend; Salomo juga membangun kerangka Android, dan Steven membangun kerangka Web (lihat Bagian 6). Tim inti me-review Pull Request dan tetap memegang modul.

### 4.2 Empat Pasangan Modul

| Pasangan | Android | Web | Modul 1 | Modul 2 |
|---|---|---|---|---|
| A (tim inti) | Salomo | Steven | Authentication (FR-01 – FR-05) | Profile & Notification (FR-27 – FR-29) |
| B | Cecilia | Nabila | Transfer (FR-10 – FR-14) | Transaction History & Dashboard (FR-23 – FR-26) |
| C | Gracia | Surya | Payment QRIS (FR-15 – FR-17) | Top-up (FR-18 – FR-20) |
| D | Grace | Rafi | Account (FR-06 – FR-09) | Virtual Card (FR-21 – FR-22) |

Alasan pengelompokan:

- **Pasangan A** memegang Authentication karena semua modul lain bergantung pada login, token, dan PIN. Profile & Notification dekat dengan data pengguna.
- **Pasangan B** memegang Transfer, modul paling berat. Pasangannya Transaction History, yang datanya memang berasal dari Transfer.
- **Pasangan C** memegang QRIS dan Top-up karena alurnya sama: pilih tujuan, konfirmasi, masukkan PIN, lihat bukti.
- **Pasangan D** memegang Account dan Virtual Card. Keduanya mengelola "wadah dana" (rekening, kantong, kartu) dan bebannya sedang.

### 4.3 Siapa Menulis Backend dan Database

Backend dan database **hanya ditulis Salomo, Rafi, dan Surya**. Pembagiannya mengikuti modul Web yang mereka pegang dan besarnya beban, dan setiap kode diperiksa anggota lain di tim yang sama.

| Bagian backend | Penulis backend dan tabel | Layar Android | Halaman Web | Reviewer backend |
|---|---|---|---|---|
| Inti transaksi keuangan (ledger, idempotensi, audit) | Salomo | – | – | Rafi, Surya |
| Skema induk database dan aturan migrasi Flyway | Salomo, Rafi, Surya | – | – | Saling memeriksa |
| Authentication (FR-01 – FR-05) | Salomo | Salomo | Steven | Surya |
| Transfer (FR-10 – FR-14) | Salomo | Cecilia | Nabila | Rafi |
| Account (FR-06 – FR-09) | Rafi | Grace | Rafi | Salomo |
| Virtual Card (FR-21 – FR-22) | Rafi | Grace | Rafi | Surya |
| Transaction History & Dashboard (FR-23 – FR-26) | Rafi | Cecilia | Nabila | Salomo |
| Payment QRIS (FR-15 – FR-17) | Surya | Gracia | Surya | Rafi |
| Top-up (FR-18 – FR-20) | Surya | Gracia | Surya | Salomo |
| Profile & Notification (FR-27 – FR-29) | Surya | Salomo | Steven | Rafi |

| Anggota tim backend | Jumlah FR backend | Tambahan |
|---|---|---|
| Salomo | 10 (Authentication, Transfer) | Inti transaksi keuangan, koordinasi tim |
| Rafi | 10 (Account, Virtual Card, History) | Penghubung kebutuhan Web ke kontrak API |
| Surya | 9 (QRIS, Top-up, Profile) | Data uji dan simulator (QRIS, biller) |

Surya = Permata Surya Cahyana. Anggota lain tidak menulis backend, tetapi mengusulkan kebutuhan data layarnya lewat kontrak API.

## 5. Isi Pekerjaan Satu Modul

Satu modul diselesaikan oleh pemegang layar (Android dan Web) bersama penulis backend modul itu (Bagian 4.3):

1. Kontrak API modul (bagian OpenAPI milik modul itu): diusulkan pemegang layar, ditulis dan disetujui Tim Backend & Database.
2. Backend: controller, service, repository, migrasi tabel modul, dan unit test (Salomo, Rafi, atau Surya).
3. Layar Android untuk modul itu (Jetpack Compose, MVVM, memakai komponen bersama).
4. Halaman Web untuk modul itu (Node.js, Express, EJS, memakai komponen bersama).
5. Skenario uji modul untuk D06 System Testing.
6. Bagian modul pada dokumen Detailed Design, UI Map, Developer Manual, dan User Manual.

**Yang tidak boleh dibuat sendiri oleh modul:** login atau token sendiri, verifikasi PIN sendiri, cara memotong saldo sendiri, atau komponen tampilan PIN sendiri. Semuanya memakai milik tim inti.

## 6. Bagian Bersama yang Dipegang Tim Inti

| Komponen | Isi | Pemegang | Dipakai oleh |
|---|---|---|---|
| Kerangka Backend dan Database | Struktur paket per modul, format error standar, konfigurasi keamanan, profil H2/PostgreSQL, skema induk | Salomo, Rafi, Surya | Semua modul |
| Login, token (JWT), dan layanan verifikasi PIN | Satu-satunya tempat memeriksa sesi dan PIN | Salomo | Semua modul |
| Inti transaksi keuangan | Ledger double-entry, transaksi atomik, Idempotency-Key, jejak audit | Salomo (review Rafi, Surya) | Account, Transfer, QRIS, Top-up, Virtual Card, History |
| Kerangka Android | MVVM, navigasi, tema, API client, penyimpanan token, komponen PIN dan bukti transaksi | Salomo | Semua layar Android |
| Kerangka Web | Server Express, sesi, CSRF, klien API, layout EJS, penjaga halaman | Steven (bersama Rafi) | Semua halaman Web |
| Kontrak API dan mock server | OpenAPI induk dan server tiruan agar Android/Web bisa mulai tanpa menunggu backend | Rafi, Surya | Semua pasangan |
| Gateway, CI, aturan migrasi | Nginx, build otomatis, penomoran migrasi Flyway | Salomo | Semua modul |

Modul transaksi (Transfer, QRIS, Top-up) **cukup memanggil** inti transaksi keuangan, misalnya `postTransaction(dari, ke, nominal, idempotencyKey)`, dan tidak menulis logika ledger sendiri.

## 7. Jadwal (15 September – 23 November 2026)

| Minggu | Tanggal | Tim inti dan Tim Backend & Database | Pemegang layar Android dan Web |
|---|---|---|---|
| 1 | 15 – 21 Sep | Finalisasi SRS dan pembagian modul | Mempelajari SRS modul masing-masing |
| 2 – 3 | 22 Sep – 5 Okt | Salomo, Rafi, Surya: skema database, kontrak API, mock server, dan fondasi backend; Steven: kerangka Web | Mengusulkan kebutuhan data layar, UI Map, dan diagram UML; belajar kerangka project |
| 4 – 7 | 6 Okt – 2 Nov | Backend delapan modul sesuai Bagian 4.3; review Pull Request | Mengerjakan layar Android dan halaman Web di repositori yang sama |
| 8 | 3 – 9 Nov | Integrasi dan uji lintas modul | Memperbaiki hasil integrasi |
| 9 | 10 – 16 Nov | Uji sistem (D06) | Menjalankan skenario uji modul, mencatat defect |
| 10 | 17 – 23 Nov | Perbaikan akhir dan demo | Melengkapi dokumen modul |
| – | 23 Nov, 23.59 WIB | Tenggat dokumentasi | Tenggat dokumentasi |

Tonggak penting: **akhir minggu 3 fondasi wajib selesai.** Jika fondasi terlambat, pasangan tetap bisa bekerja dengan mock server.

## 8. Aturan Kerja

1. **Satu repositori untuk semua.** Tidak ada pengiriman kode lewat folder, ZIP, atau chat.
2. **Satu branch per fitur**, misalnya `transfer/konfirmasi-pin`. Penggabungan ke `main` hanya lewat Pull Request.
3. **Setiap Pull Request di-review minimal satu orang**: pasangan modul untuk kode layar, sesama anggota Tim Backend & Database untuk kode backend dan migrasi, tim inti untuk perubahan bagian bersama.
4. **Kontrak API dulu, kode kemudian.** Perubahan kontrak disetujui Tim Backend & Database dan diumumkan ke pemegang layar.
5. **Migrasi database diberi nomor berurutan** dengan nama modul, misalnya `V7__transfer_favorit.sql`, agar tidak bentrok.
6. **Teknologi seragam**: Android memakai Kotlin + Jetpack Compose + MVVM, Web memakai Node.js + Express + EJS, Backend memakai Java 21 + Spring Boot.
7. **Pertemuan singkat dua kali seminggu**: tiap pasangan menyampaikan apa yang sudah selesai, apa yang berikutnya, dan apa hambatannya.

## 9. Definition of Done per Modul

Sebuah modul dianggap selesai jika:

- Semua endpoint modul ada dan sesuai kontrak API.
- Unit test backend modul lulus di CI.
- Layar Android modul berjalan terhubung ke backend sungguhan (bukan data tiruan).
- Halaman Web modul berjalan terhubung ke backend sungguhan.
- Skenario uji modul tertulis dan sudah dijalankan.
- Semua kode sudah masuk `main` lewat Pull Request yang disetujui.
- Bagian modul pada dokumen (Detailed Design, UI Map, manual) sudah terisi.

## 10. Matriks Tanggung Jawab (RACI) Singkat

R = mengerjakan, A = penanggung jawab akhir, C = dimintai masukan, I = diberi tahu.

| Pekerjaan | Salomo, Rafi, Surya (Backend & DB) | Pemegang layar modul | Pasangan modul | Anggota lain |
|---|---|---|---|---|
| Fondasi backend dan database | R, A | C | I | I |
| Kontrak API modul | R, A | C | C | I |
| Backend dan tabel modul | R, A | C | I | I |
| Kerangka Android / Web | C | C | I | I |
| Layar Android / halaman Web modul | C | R, A | C (review) | I |
| Integrasi lintas modul | R, A | R | C | I |
| Uji sistem | C | R, A | R | C |
| Dokumen modul | C (bagian backend: R) | R, A | C | I |

Kerangka Android dikerjakan Salomo dan kerangka Web dikerjakan Steven bersama Rafi (R, A). Penanggung jawab akhir proyek tetap Project Manager.

## 11. Pembagian Dokumen yang Selaras dengan Modul

Dokumen tetap mengikuti 9 volume pada DWP. Bedanya, setiap pemegang modul **mengisi bagian modulnya sendiri** di setiap volume, sedangkan PIC volume menyatukan dan merapikan:

| Volume | PIC penyatu | Diisi oleh |
|---|---|---|
| D04 Detailed Design | Android: Gracia; Web: Rafi; Backend & Database: Salomo | Pemegang layar; bagian backend dan database oleh Salomo, Rafi, Surya |
| D05 UI Map | Android: Cecilia; Web: Nabila | Pemegang modul masing-masing |
| D06 System Testing | Steven | Skenario uji dari pemegang modul |
| D07 Developer Manual | Android: Salomo; Web: Steven; Backend: Salomo | Bagian modul dari pemegang modul; bagian backend oleh Salomo, Rafi, Surya |
| D09 User Manual | Android: Grace; Web: Surya | Langkah pemakaian modul dari pemegang modul |

Dengan cara ini, dokumen tidak ditulis oleh orang yang tidak membuat fiturnya.

## 12. Risiko dan Cara Mengatasinya

| Risiko | Dampak | Cara mengatasi |
|---|---|---|
| Fondasi terlambat | Semua pasangan tertahan | Mock server sejak minggu 2; fondasi backend dikerjakan tiga orang |
| Rafi atau Surya belum terbiasa dengan Spring Boot | Backend modul lambat atau salah | Modul Authentication dari Salomo dijadikan pola; sesi belajar bertiga di minggu 2 – 3 |
| Salomo, Rafi, dan Surya memegang dua peran (backend dan klien) | Beban tinggi, layar mereka terlambat | Backend didahulukan di minggu 4 – 5; Cecilia atau Grace membantu layar Android Salomo, Nabila dan Steven membantu halaman Web Rafi dan Surya |
| Salah satu anggota tim backend berhalangan | Modulnya macet | Reviewer backend modul itu (Bagian 4.3) mengambil alih |
| Pasangan tidak aktif | Modul terbengkalai | Status dicek dua kali seminggu; modul dipindah ke pasangan lain jika macet dua minggu |
| Perubahan kontrak API mendadak | Android/Web rusak | Kontrak dikunci per minggu; perubahan lewat Pull Request yang di-review |
| Logika saldo dibuat ganda | Saldo tidak konsisten | Saldo hanya boleh diubah lewat inti transaksi keuangan |
| Kode dikirim di luar Git | Kontribusi tidak tercatat dan sulit digabung | Aturan 8.1 ditegakkan sejak minggu pertama |

## 13. Ringkasan

- Layar satu modul dipegang satu **pasangan** (1 Android + 1 Web).
- **Backend dan Database dikerjakan Salomo (tim Android) bersama Rafi dan Surya (tim Web)**, dengan pembagian modul pada Bagian 4.3 dan saling review.
- Bagian bersama (login, PIN, ledger, skema database, kerangka Android/Web/Backend, kontrak API) **dipegang tim inti** dan dibuat satu kali di minggu 2 – 3.
- Semua orang bekerja di **satu repositori** lewat branch dan Pull Request, sehingga kontribusi tercatat dan kode langsung bisa digabung.
- Pengetahuan backend tidak lagi menumpuk pada PM, karena dua anggota tim Web ikut membangun dan memahaminya.
