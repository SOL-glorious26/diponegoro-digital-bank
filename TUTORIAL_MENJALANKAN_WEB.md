# 🖥️ Tutorial Menjalankan & Menguji Aplikasi Web Diponegoro Digital Bank

Tutorial ini menuntun dari **laptop kosong** sampai aplikasi Web **berjalan dan teruji**.
Ikuti urut dari atas. Setiap langkah diberi tanda **✅ Berhasil jika…** supaya kamu tahu langkahnya sudah benar sebelum lanjut.

> 📄 Versi Word: `documents_area/TUT_Panduan_Menjalankan_Web/DDB_TUT_Panduan_Menjalankan_dan_Menguji_Web_v2.0.docx`
>
> Web yang dipakai adalah aplikasi resmi tim Web (Node.js, Express, EJS) di `code_area/apps/web/digitalbanking-node`.
> Diuji langsung di Windows 11 pada 22 November 2026 (Node.js 24, Java 21, Chrome).
> Hasil: WN **25/25 lulus**, lintas platform LPN **3/3 lulus**, peramban **24/24 pemeriksaan lulus**.

**Daftar isi**
1. [Konsep singkat: kenapa butuh 2 terminal?](#1-konsep-singkat-kenapa-butuh-2-terminal)
2. [Instal perangkat (sekali saja)](#2-instal-perangkat-sekali-saja)
3. [Ambil kode proyek](#3-ambil-kode-proyek)
4. [Izinkan PowerShell menjalankan skrip (sekali saja)](#4-izinkan-powershell-menjalankan-skrip-sekali-saja)
5. [Terminal 1: jalankan Backend](#5-terminal-1-jalankan-backend)
6. [Terminal 2: jalankan Web](#6-terminal-2-jalankan-web)
7. [Masuk ke aplikasi](#7-masuk-ke-aplikasi)
8. [Tes manual: skenario lengkap](#8-tes-manual-skenario-lengkap)
9. [Tes otomatis](#9-tes-otomatis)
10. [Mematikan aplikasi](#10-mematikan-aplikasi)
11. [Kalau ada masalah (troubleshooting)](#11-kalau-ada-masalah-troubleshooting)
12. [Ringkasan perintah (contekan)](#12-ringkasan-perintah-contekan)

---

## 1. Konsep singkat: kenapa butuh 2 terminal?

Aplikasi Web **tidak bisa berdiri sendiri**. Web hanya tampilan; semua data (akun, saldo, transaksi) ada di **backend**.

```
  Chrome (kamu)  ──►  Web (Node, port 3000)  ──►  Backend (Java, port 8080)  ──►  Database
                       Terminal 2                  Terminal 1
```

Jadi kamu harus menjalankan **dua program sekaligus**, masing-masing di jendela terminal sendiri, dan **keduanya harus tetap terbuka** selama aplikasi dipakai:

| Terminal | Program | Alamat | Jika lupa dijalankan… |
|---|---|---|---|
| **Terminal 1** | Backend | http://localhost:8080 | Web terbuka, tapi login gagal: *"Tidak dapat terhubung ke backend…"* |
| **Terminal 2** | Web | http://localhost:3000 | Chrome menampilkan *"This site can't be reached"* |

> ⚠️ **Kesalahan paling sering:** hanya menjalankan Web (`npm start`) tanpa backend. Pastikan **Terminal 1 menyala dulu**.

---

## 2. Instal perangkat (sekali saja)

| Perangkat | Untuk apa | Cara instal | Cek berhasil |
|---|---|---|---|
| **Node.js LTS** (20 atau lebih baru) | Menjalankan Web (`npm`) | Unduh dari https://nodejs.org → tombol **LTS** → Next sampai selesai | `node -v` → muncul `v20…` atau lebih |
| **Java 21** | Menjalankan backend | **Pilihan A:** sudah punya **Android Studio** → tidak perlu apa-apa, Java-nya otomatis dipakai.<br>**Pilihan B:** unduh **Temurin 21** dari https://adoptium.net → centang *Set JAVA_HOME* saat instal | `java -version` → `21` atau lebih |
| **Git** | Mengambil kode dari GitHub | https://git-scm.com → Next sampai selesai | `git --version` |
| **Google Chrome** | Membuka aplikasi & tes otomatis | https://google.com/chrome | – |

Cara mengecek: tekan tombol **Windows**, ketik **PowerShell**, buka, lalu ketik perintah di kolom "Cek berhasil".

> 💡 Setelah menginstal sesuatu, **tutup lalu buka lagi** PowerShell agar perintah barunya dikenali.

---

## 3. Ambil kode proyek

**Pilihan A: pakai Git (disarankan, bisa update dengan `git pull`)**

```powershell
cd $HOME\Documents
git clone https://github.com/SOL-glorious26/diponegoro-digital-bank.git
cd diponegoro-digital-bank
```

Saat pertama kali, Git akan membuka jendela login GitHub. Masuk dengan akun yang **sudah diundang** ke repo.

**Pilihan B: tanpa Git**
Buka https://github.com/SOL-glorious26/diponegoro-digital-bank → tombol hijau **Code** → **Download ZIP** → ekstrak ke `Documents`.

✅ **Berhasil jika** di dalam folder `diponegoro-digital-bank` ada folder `code_area` dan `documents_area`.

> 📍 **Penting soal path.** Struktur foldernya seperti ini:
> ```
> diponegoro-digital-bank\          ← folder UTAMA repo
> ├── code_area\
> │   ├── backend\                  ← backend ada DI DALAM code_area
> │   ├── apps\web\digitalbanking-node\   ← Web resmi tim Web
> │   └── pengujian\                ← tes otomatis
> └── documents_area\
> ```
> Semua perintah `cd code_area\...` di tutorial ini dijalankan **dari folder utama** `diponegoro-digital-bank`.

---

## 4. Izinkan PowerShell menjalankan skrip (sekali saja)

Windows bawaannya **memblokir** skrip PowerShell. Akibatnya `.\run-local.ps1` dan `npm` bisa gagal dengan pesan merah seperti:

```
... cannot be loaded because running scripts is disabled on this system.
```

Perbaiki **sekali saja** dengan perintah ini di PowerShell (tidak perlu Administrator):

```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

Jika ditanya konfirmasi, ketik **Y** lalu Enter.

✅ **Berhasil jika** `Get-ExecutionPolicy` menampilkan `RemoteSigned`.

> Kalau tidak mau mengubah pengaturan ini, ganti `npm` dengan `npm.cmd`, dan jalankan backend dengan
> `powershell -ExecutionPolicy Bypass -File .\run-local.ps1 -H2`.

---

## 5. Terminal 1: jalankan Backend

Buka **PowerShell baru** (ini Terminal 1), lalu:

```powershell
cd $HOME\Documents\diponegoro-digital-bank        # sesuaikan dengan lokasi folder kamu
cd code_area\backend
.\run-local.ps1 -H2
```

Penjelasan:
- `-H2` = memakai database **H2** bawaan, jadi **tidak perlu menginstal PostgreSQL**. Data disimpan di folder `code_area\backend\data`.
- Pertama kali akan lama (**2–5 menit**) karena mengunduh pustaka Java. Berikutnya ±30 detik.

Mula-mula muncul `Membangun backend...`. Tunggu sampai muncul tulisan hijau, lalu banyak baris log, dan akhirnya baris seperti ini:

```
Backend jalan di http://localhost:8080  (HP di Wi-Fi yang sama: http://192.168.x.x:8080)
Akun demo: demo / Demo@1234, PIN 123456. Tekan Ctrl+C untuk berhenti.
...
Started DdbBackendApplication in 15.982 seconds
```

Baris log berwarna kuning `WARN ... SpringDoc` itu **normal**, abaikan saja.

✅ **Berhasil jika** membuka http://localhost:8080/actuator/health di Chrome menampilkan tulisan yang berisi `"status":"UP"`.

🎁 Bonus: http://localhost:8080/swagger-ui.html menampilkan **daftar semua API** dan bisa dicoba langsung.

> ⛔ **Jangan tutup jendela ini.** Kalau ditutup, backend mati dan Web tidak bisa login.

---

## 6. Terminal 2: jalankan Web

Buka **PowerShell baru lagi** (ini Terminal 2; Terminal 1 tetap terbuka), lalu:

```powershell
cd $HOME\Documents\diponegoro-digital-bank        # sesuaikan dengan lokasi folder kamu
cd code_area\apps\web\digitalbanking-node
copy .env.example .env      # HANYA pertama kali
npm install                 # HANYA pertama kali
npm start
```

✅ **Berhasil jika** membuka http://localhost:3000 menampilkan halaman **Masuk**.

Isi `.env`: `PORT` (3000), `BACKEND_URL` (`http://localhost:8080/api/v1`), dan `SESSION_SECRET` (boleh kosong; bila kosong, login hilang setiap Web dimulai ulang).

> ⛔ **Jangan tutup jendela ini juga.** Berkas `.env` tidak boleh diunggah ke GitHub.

---

## 7. Masuk ke aplikasi

Buka http://localhost:3000 lalu masuk dengan akun demo:

| Username | Password | PIN transaksi | No. rekening |
|---|---|---|---|
| **`demo`** | `Demo@1234` | `123456` | 1400123456 (punya riwayat 5 bulan) |
| `andi` | `Demo@1234` | `123456` | 1400000002 |
| `budi` | `Demo@1234` | `123456` | 1400000003 |
| `citra` | `Demo@1234` | `123456` | 1400000004 |

✅ **Berhasil jika** setelah klik **Login** muncul **Dashboard** dengan saldo, nomor rekening, transaksi terakhir, dan menu di kiri.

> 💡 Ingin mencoba dua akun bersamaan (misal demo mengirim, andi menerima)? Buka akun kedua di **jendela Incognito** (`Ctrl+Shift+N`).

---

## 8. Tes manual: skenario lengkap

Centang setiap skenario setelah dicoba. Kolom **Hasil yang benar** adalah patokannya. Kalau hasilnya berbeda, catat dan laporkan (lihat bagian bawah).

### A. Akun & keamanan

| # | Skenario | Langkah | Hasil yang benar |
|---|---|---|---|
| A1 | Login benar | Masuk sebagai `demo` / `Demo@1234` | Masuk ke Dashboard, tanpa OTP |
| A2 | Login salah | Password `Salah@123` | Pesan kesalahan, tetap di halaman Masuk |
| A3 | Halaman terlindungi | Tanpa login, buka http://localhost:3000/transfer | Diarahkan ke halaman Masuk |
| A4 | Daftar akun baru | **Daftar akun** → isi data → masukkan **kode OTP yang tampil di layar** | Lanjut ke halaman e-KYC |
| A5 | e-KYC disetujui | NIK 16 digit (mis. `3374010101010001`) → **Kirim Data e-KYC** | Diproses beberapa detik, lalu lanjut **Buat PIN** |
| A6 | e-KYC ditolak | Akun baru lain, NIK berakhiran **`0000`** | Status ditolak |
| A7 | Buat PIN | Ketik 6 digit dua kali | Masuk ke Dashboard (saldo awal akun baru Rp5.000.000) |
| A8 | Lupa sandi/PIN | Halaman Masuk → **Lupa sandi?** → pilih Password atau PIN → OTP tampil di layar | Nilai baru bisa dipakai, nilai lama ditolak |
| A9 | Logout semua perangkat | Dari Android: Profil → **Sesi & perangkat aktif** → Logout dari semua perangkat | Web kembali ke halaman Masuk dengan pesan sesi berakhir |

### B. Transfer

| # | Skenario | Langkah | Hasil yang benar |
|---|---|---|---|
| B1 | Sesama bank | Transfer → **Sesama Bank (DDB)** → `1400000002` → Rp10.000 → **Lanjut ke Konfirmasi** → PIN `123456` | Nama **ANDI PRATAMA** tampil sebelum PIN; detail transaksi terbuka |
| B2 | Penerima menerima | Jendela Incognito, login `andi` → Riwayat | Ada transaksi masuk Rp10.000 |
| B3 | Ditolak bank tujuan | **Ke Bank Lain** → BRI → `9999999999` | Gagal; **dana dikembalikan** |
| B4 | Timeout antarbank | Mandiri → `8888888888` → buka detail → **Cek status** | Diproses, lalu Berhasil; debit satu kali (biaya Rp2.500) |
| B5 | Rekening tidak ada | DDB `1400999999` | Pesan *"Rekening tujuan tidak valid"*; PIN tidak diminta |
| B6 | Nominal terlalu kecil | Rp1.000 | Pesan nominal minimal Rp5.000 |
| B7 | PIN salah | PIN `000000` | Pesan PIN salah; **salah 3× → PIN diblokir 5 menit** |

> ⚠️ Setelah tes B7, PIN akun itu terblokir 5 menit. Tunggu, atau lanjutkan dengan akun `budi`.

### C. Pembayaran

| # | Skenario | Langkah | Hasil yang benar |
|---|---|---|---|
| C1 | Top Up | Tab **Top Up** → sumber dana → Rp50.000 | Saldo bertambah Rp50.000, tanpa PIN |
| C2 | Pulsa | Tab **Pulsa** → `081234567890` → Rp10.000 → PIN | Berhasil; total Rp11.500 |
| C3 | Token PLN | Tab **Token PLN** → `12345678901` → Rp20.000 → PIN | Berhasil; token ada di detail transaksi |
| C4 | ID PLN salah | ID berakhiran **`0000`** | Pesan ID pelanggan tidak ditemukan |
| C5 | Tagihan dua kali | Bayar tagihan yang sama dua kali | Pembayaran kedua ditolak |
| C6 | E-wallet | Tab **E-wallet** → nomor → Rp20.000 → PIN | Berhasil |
| C7 | QRIS | Tab **QRIS** → pilih contoh → isi nominal bila diminta → PIN | Nama merchant tampil; berhasil |

### D. Riwayat & profil

| # | Skenario | Langkah | Hasil yang benar |
|---|---|---|---|
| D1 | Filter | Riwayat → jenis, kata kunci, atau tanggal → **Terapkan** | Daftar tersaring |
| D2 | Detail | Klik **Detail** | Rincian dan jurnal tampil |
| D3 | Ekspor | Klik **CSV** lalu **PDF** | Berkas terunduh |
| D4 | Ubah profil | Profil → ganti email → **Simpan Perubahan** → OTP yang tampil | Profil diperbarui |
| D5 | Ganti kata sandi | Profil → **Ubah Kata Sandi** | Hanya kata sandi baru yang berlaku |

> Belum ada di Web (tersedia di Android): kantong, kartu virtual, notifikasi, grafik saldo, penerima favorit, QRIS pribadi/dinamis, dan paket data.

### Cara melaporkan bug

Tulis di grup atau buat **Issue** di GitHub (tab **Issues** → **New issue**) dengan format:

```
Skenario : B4 Timeout antarbank
Langkah  : Mandiri 8888888888, Rp50.000, PIN 123456
Harapan  : Diproses, lalu Berhasil setelah Cek status
Kenyataan: muncul pesan "...."
Screenshot: (lampirkan)
```

---

## 9. Tes otomatis

Selain tes manual, proyek punya skrip yang **mengetes otomatis** (Chrome dibuka sendiri di latar belakang).
**Syarat:** Terminal 1 (backend) dan Terminal 2 (Web) **sedang berjalan**.

Buka **PowerShell ketiga** (Terminal 3):

```powershell
cd $HOME\Documents\diponegoro-digital-bank\code_area\pengujian
npm run web:node
```

Tidak perlu `npm install` di folder ini karena skripnya tidak memakai pustaka tambahan.

| Perintah | Yang dites | Lama |
|---|---|---|
| `npm run web:node` | Alur inti (WN-01 – WN-14) | ±20 detik |
| `npm run web:node:ekstra` | Uji lanjutan (WN-15 – WN-25) | ±2 menit |
| `npm run web:node:peramban` | Alur inti di Chrome, Edge, dan Firefox (harus terinstal) | ±1 menit |
| `npm run lintas:node` | Lintas platform dengan emulator Android (LPN-01 – LPN-03) | ±2 menit |
| `npm run perf` | Kinerja API dan Dashboard (NF-01 – NF-03) | ±20 detik |

**Cara membaca hasil:** setiap baris diawali `PASS` (lulus) atau `FAIL` (gagal). Di akhir ada ringkasan seperti:

```
PASS  WN-01  login salah ditolak, login benar masuk Dashboard
...
14/14 PASS
```

Hasil tersimpan di folder `code_area\pengujian\hasil\`.

> ⚠️ Tes otomatis **membuat transaksi** di database lokal. Ini normal.
> WN-21 butuh backend kedua di port 8081 dengan `--ddb.security.access-token-minutes=1` dan Web kedua di port 3001 (`PORT=3001`, `BACKEND_URL=http://localhost:8081/api/v1`). Tanpa itu, WN-21 akan `FAIL`; jalankan kasus lain dengan `node web/node-ekstra.mjs hasil/wnx WN-15,WN-16`.

---

## 10. Mematikan aplikasi

Di **Terminal 2** lalu **Terminal 1**: tekan **`Ctrl + C`**. Jika ditanya `Terminate batch job (Y/N)?`, ketik **Y**.

**Mengulang data dari awal** (semua transaksi tes hilang, kembali ke data demo):
matikan backend → hapus folder `code_area\backend\data` → jalankan lagi `.\run-local.ps1 -H2`.

---

## 11. Kalau ada masalah (troubleshooting)

| Pesan / gejala | Penyebab | Solusi |
|---|---|---|
| **"Tidak dapat terhubung ke backend…"** saat login | Backend (Terminal 1) belum jalan, masih loading, atau sudah ditutup | Jalankan langkah 5. Tunggu sampai `Started DdbBackendApplication`. Cek http://localhost:8080/actuator/health |
| `... cannot be loaded because running scripts is disabled` | Kebijakan PowerShell | Lakukan langkah 4 |
| `cd : Cannot find path '...\backend'` | Salah folder | Pastikan kamu di folder utama repo, lalu `cd code_area\backend` (bukan `cd backend`) |
| `npm : The term 'npm' is not recognized` | Node.js belum terinstal / PowerShell belum dibuka ulang | Instal Node.js, tutup & buka PowerShell |
| `JAVA_HOME is not set` atau `java.exe` tidak ditemukan | Java belum ada | Instal Temurin 21 (langkah 2) **atau** Android Studio, lalu buka ulang PowerShell |
| `Port 8080 was already in use` | Backend sudah jalan di jendela lain | Pakai yang sudah jalan, atau tutup jendela lama |
| `EADDRINUSE :::3000` | Web sudah jalan di jendela lain | Tutup jendela lama, atau ubah `PORT` di `.env` |
| **403** "Token keamanan tidak valid" | Formulir lama setelah Web dimulai ulang | Muat ulang halaman, lalu ulangi |
| "Sesi Anda telah berakhir. Silakan masuk kembali." | Backend direstart, atau logout semua perangkat | Login ulang (normal) |
| PIN tidak bisa dipakai, "PIN diblokir" | Salah PIN 3× | Tunggu 5 menit, atau pakai akun lain |
| Akun terkunci saat login | Salah password 5× | Tunggu 15 menit, atau reset data (bagian 10) |
| Tes otomatis `FAIL` semua / `fetch failed` | Backend atau Web tidak jalan | Pastikan Terminal 1 & 2 menyala |
| `npm install` gagal / error jaringan | Internet putus atau proxy kampus | Coba jaringan lain, lalu ulangi `npm install` |

Masih buntu? Kirim **screenshot seluruh jendela terminal** ke grup.

---

## 12. Ringkasan perintah (contekan)

```powershell
# ===== SEKALI SAJA =====
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
git clone https://github.com/SOL-glorious26/diponegoro-digital-bank.git

# ===== TERMINAL 1: Backend =====
cd <folder>\diponegoro-digital-bank\code_area\backend
.\run-local.ps1 -H2
#   → tunggu "Started DdbBackendApplication"

# ===== TERMINAL 2: Web =====
cd <folder>\diponegoro-digital-bank\code_area\apps\web\digitalbanking-node
copy .env.example .env   # pertama kali saja
npm install              # pertama kali saja
npm start
#   → buka http://localhost:3000  (demo / Demo@1234 / PIN 123456)

# ===== TERMINAL 3: Tes otomatis (opsional) =====
cd <folder>\diponegoro-digital-bank\code_area\pengujian
npm run web:node
npm run web:node:ekstra

# ===== Selesai =====
# Ctrl + C di setiap terminal
```

Ambil versi kode terbaru dari GitHub: `git pull` di folder utama repo.
