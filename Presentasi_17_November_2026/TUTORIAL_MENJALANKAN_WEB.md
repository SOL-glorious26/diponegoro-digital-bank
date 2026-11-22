# 🖥️ Tutorial Menjalankan & Menguji Aplikasi Web Diponegoro Digital Bank

Tutorial ini menuntun dari **laptop kosong** sampai aplikasi Web **berjalan dan teruji**.
Ikuti urut dari atas. Setiap langkah diberi tanda **✅ Berhasil jika…** supaya kamu tahu langkahnya sudah benar sebelum lanjut.

> 📄 Versi Word: `documents_area/TUT_Panduan_Menjalankan_Web/DDB_TUT_Panduan_Menjalankan_dan_Menguji_Web_v1.0.docx`
>
> Diuji langsung di Windows 11 pada 14 November 2026 (Node.js 24, Java 21, Chrome).
> Hasil saat tutorial ini dibuat: tes API **38/38 lulus**, tes UI **20/20 lulus**, WB-11 dan WB-13–15 **lulus**.

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
  Chrome (kamu)  ──►  Web (Vite, port 5173)  ──►  Backend (Java, port 8080)  ──►  Database
                       Terminal 2                  Terminal 1
```

Jadi kamu harus menjalankan **dua program sekaligus**, masing-masing di jendela terminal sendiri, dan **keduanya harus tetap terbuka** selama aplikasi dipakai:

| Terminal | Program | Alamat | Jika lupa dijalankan… |
|---|---|---|---|
| **Terminal 1** | Backend | http://localhost:8080 | Web terbuka, tapi login gagal: *"Tidak dapat terhubung ke server. Pastikan backend berjalan."* |
| **Terminal 2** | Web | http://localhost:5173 | Chrome menampilkan *"This site can't be reached"* |

> ⚠️ **Kesalahan paling sering:** hanya menjalankan Web (`npm run dev`) tanpa backend. Pastikan **Terminal 1 menyala dulu**.

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
> │   ├── apps\web\                 ← web ada di code_area\apps\web
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
cd code_area\apps\web
npm install          # HANYA pertama kali (atau setelah package.json berubah), ±1 menit
npm run dev
```

Tunggu sampai muncul:

```
  VITE v5.4.11  ready in 500 ms

  ➜  Local:   http://localhost:5173/
```

✅ **Berhasil jika** membuka http://localhost:5173 menampilkan halaman **Masuk** Diponegoro Digital Bank.

> ⛔ **Jangan tutup jendela ini juga.**

---

## 7. Masuk ke aplikasi

Buka http://localhost:5173 lalu masuk dengan akun demo:

| Username | Password | PIN transaksi | No. rekening |
|---|---|---|---|
| **`demo`** | `Demo@1234` | `123456` | 1400123456 (punya riwayat 5 bulan) |
| `andi` | `Demo@1234` | `123456` | 1400000002 |
| `budi` | `Demo@1234` | `123456` | 1400000003 |
| `citra` | `Demo@1234` | `123456` | 1400000004 |

✅ **Berhasil jika** setelah klik **Masuk** muncul **Beranda** dengan saldo, grafik, dan menu di kiri.

> 💡 Ingin mencoba dua akun bersamaan (misal demo mengirim, andi menerima)? Buka akun kedua di **jendela Incognito** (`Ctrl+Shift+N`).

---

## 8. Tes manual: skenario lengkap

Centang setiap skenario setelah dicoba. Kolom **Hasil yang benar** adalah patokannya. Kalau hasilnya berbeda, catat dan laporkan (lihat bagian bawah).

### A. Akun & keamanan

| # | Skenario | Langkah | Hasil yang benar |
|---|---|---|---|
| A1 | Login benar | Masuk sebagai `demo` / `Demo@1234` | Masuk ke Beranda |
| A2 | Login salah | Password `salah123` | Pesan error, tetap di halaman Masuk. Salah 5× → akun dikunci 15 menit |
| A3 | Sesi bertahan saat refresh | Setelah login, tekan **F5** | Tetap masuk |
| A4 | Sesi hilang saat tab ditutup | Tutup tab, buka lagi http://localhost:5173 | Diminta login lagi (**memang begitu desainnya**, demi keamanan) |
| A5 | Daftar akun baru | Klik **Daftar** → isi data → masukkan **kode OTP yang ditampilkan di layar** | Akun aktif, lanjut ke halaman e-KYC |
| A6 | e-KYC disetujui | Isi NIK 16 digit (mis. `3374012345678901`), nama, foto → kirim | Status *Diproses* ±3 detik → *Disetujui* → lanjut **Buat PIN** |
| A7 | e-KYC ditolak | Akun baru lain, NIK berakhiran **`0000`** | Status *Ditolak* |
| A8 | Buat PIN | Ketik 6 digit dua kali | Masuk ke Beranda (saldo awal akun baru Rp5.000.000) |
| A9 | Lupa password | Halaman Masuk → **Lupa password atau PIN?** → ikuti 3 langkah (OTP tampil di layar) | Password baru bisa dipakai login |
| A10 | Logout semua perangkat | Profil → **Sesi aktif** → Keluar dari semua perangkat | Kembali ke halaman Masuk; tab lain yang login juga ikut keluar saat dipakai |

### B. Saldo & Beranda

| # | Skenario | Langkah | Hasil yang benar |
|---|---|---|---|
| B1 | Lihat saldo | Beranda | Saldo + nomor rekening tampil |
| B2 | Sembunyikan saldo | Klik ikon mata | Saldo jadi `••••`; tetap tersembunyi setelah F5 |
| B3 | Grafik saldo | Pilih Harian / Mingguan / Bulanan | Grafik berubah |

### C. Transfer

| # | Skenario | Langkah | Hasil yang benar |
|---|---|---|---|
| C1 | Transfer sesama DDB | Transfer → bank **Diponegoro Digital Bank** → rekening `1400000002` → Rp10.000 → Lanjut → PIN `123456` | Nama penerima **ANDI** tampil sebelum konfirmasi; biaya Rp0; muncul **bukti transfer** |
| C2 | Penerima menerima | Jendela Incognito, login `andi` → Riwayat | Ada transaksi masuk Rp10.000 dari demo |
| C3 | Transfer antarbank | Bank **BCA** → `1234567890` → Rp50.000 | Biaya Rp2.500; berhasil |
| C4 | Ditolak bank tujuan | Bank **BRI** → `9999999999` | Gagal; **dana dikembalikan** (saldo tidak berkurang) |
| C5 | Timeout antarbank | Bank **Mandiri** → `8888888888` | Status **Diproses**; tombol *Cek status*; tidak terpotong dua kali |
| C6 | Rekening tidak ada | Rekening DDB `1400999999` | Pesan *"Rekening tujuan tidak valid. Periksa kembali data penerima."* |
| C7 | Nominal di luar batas | Rp1.000 | Pesan *"Nominal minimal Rp5.000"*. Maksimal Rp25.000.000 per transaksi |
| C8 | PIN salah | Masukkan PIN `000000` | Pesan PIN salah; **salah 3× → PIN diblokir 5 menit** |
| C9 | Favorit | Setelah transfer, klik **Simpan ke favorit** | Penerima muncul di daftar favorit |

> ⚠️ Setelah tes C8, PIN akun `demo` terblokir 5 menit. Tunggu, atau lanjutkan tes dengan akun `budi`.

### D. Top-up & tagihan

| # | Skenario | Langkah | Hasil yang benar |
|---|---|---|---|
| D1 | Pulsa | Top-up → Pulsa → nomor `081234567890` → pilih nominal → PIN | Bukti pembayaran; biaya admin Rp1.500 |
| D2 | Paket data | Pilih paket → PIN | Berhasil |
| D3 | Token PLN | Tab **Token PLN** → ID pelanggan `123456789012` → cek | Nama **SRI WAHYUNI** (R1/2200VA) tampil → pilih nominal → PIN → muncul **nomor token** |
| D4 | ID PLN salah | ID berakhiran **`0000`** | "ID pelanggan tidak ditemukan" |
| D5 | Cetak bukti | Di bukti → **Cetak / PDF** | Dialog cetak Chrome terbuka |

### E. QRIS, Kantong, Virtual Card

| # | Skenario | Langkah | Hasil yang benar |
|---|---|---|---|
| E1 | QRIS pribadi | Menu **QRIS Saya** | Kode QR tampil |
| E2 | QRIS dinamis | Isi nominal → buat | QR baru dengan nominal |
| E3 | Simulasi dana masuk | Isi nominal + nama pembayar → simulasikan | Pesan "Dana masuk … tercatat"; saldo bertambah |
| E4 | Buat kantong | Kantong → Buat → nama "Liburan", target Rp1.000.000 | Kantong baru muncul |
| E5 | Isi kantong | Isi Rp100.000 | Saldo kantong naik, saldo utama turun |
| E6 | Kunci kantong | Kunci sampai tanggal bulan depan | Label *Terkunci* |
| E7 | Tarik dini | Tarik dari kantong terkunci | Diminta **PIN** (penarikan dini) → berhasil |
| E8 | Virtual card | Virtual Card → Buat kartu → **Lihat detail** (PIN) | Nomor kartu, masa berlaku, CVV tampil |
| E9 | Bekukan kartu | Klik Bekukan → lalu Aktifkan | Status berubah |
| E10 | Atur limit | Ubah limit Rp2.000.000 | Limit baru tersimpan |

### F. Riwayat, Profil, Notifikasi, Tampilan

| # | Skenario | Langkah | Hasil yang benar |
|---|---|---|---|
| F1 | Filter riwayat | Riwayat → pilih jenis/arah/rentang tanggal | Daftar tersaring |
| F2 | Cari | Ketik "andi" → Enter | Transaksi yang cocok |
| F3 | Detail | Klik satu transaksi | Detail + jurnal pembukuan |
| F4 | Ekspor | Klik **CSV** lalu **PDF** | File terunduh |
| F5 | Ubah profil | Profil → Ubah → ganti email → kirim → masukkan OTP yang tampil | "Profil diperbarui" |
| F6 | Ganti PIN / password | Profil → Keamanan | Berhasil; PIN/password baru berlaku |
| F7 | Notifikasi | Lonceng di kanan atas | Notifikasi transaksi; bisa ditandai dibaca |
| F8 | Tampilan HP | Perkecil jendela Chrome, atau `F12` → ikon HP (`Ctrl+Shift+M`) | Menu kiri berubah jadi tombol ☰ |

### Cara melaporkan bug

Tulis di grup atau buat **Issue** di GitHub (tab **Issues** → **New issue**) dengan format:

```
Skenario : C3 Transfer antarbank
Langkah  : BCA 1234567890, Rp50.000, PIN 123456
Harapan  : berhasil, biaya Rp2.500
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
npm run web:api
```

Tidak perlu `npm install` di folder ini karena skripnya tidak memakai pustaka tambahan.

| Perintah | Yang dites | Lama |
|---|---|---|
| `npm run web:api` | 38 pemeriksaan API yang dipakai Web (login, saldo, transfer, top-up, kantong, dll.) | ±10 detik |
| `npm run web:ui` | 20 langkah klik-klik di Chrome (WB-01 – WB-10), termasuk tampilan HP | ±1 menit |
| `npm run web:extra` | Ubah profil dengan OTP (WB-11) | ±20 detik |
| `npm run web:fitur` | Grafik saldo, foto profil, ubah & kunci kantong (WB-13 – WB-15) | ±40 detik |
| `npm run web:browsers` | Alur inti di Chrome, Edge, dan Firefox (butuh Edge & Firefox terinstal) | ±2 menit |

**Cara membaca hasil:** setiap baris diawali `PASS` (lulus) atau `FAIL` (gagal). Di akhir ada ringkasan seperti:

```
PASS  login demo
PASS  saldo (FR-06)  1400123456 2347500
...
38/38 PASS
```

Screenshot dan log tersimpan di folder `code_area\pengujian\hasil\`.

> ⚠️ Tes otomatis **membuat transaksi sungguhan** di database lokal (misal transfer kecil dari akun demo). Ini normal.
> Tes `npm run web:token` butuh backend dijalankan khusus (token 1 menit). Lewati saja kecuali diminta.

---

## 10. Mematikan aplikasi

Di **Terminal 2** lalu **Terminal 1**: tekan **`Ctrl + C`**. Jika ditanya `Terminate batch job (Y/N)?`, ketik **Y**.

**Mengulang data dari awal** (semua transaksi tes hilang, kembali ke data demo):
matikan backend → hapus folder `code_area\backend\data` → jalankan lagi `.\run-local.ps1 -H2`.

---

## 11. Kalau ada masalah (troubleshooting)

| Pesan / gejala | Penyebab | Solusi |
|---|---|---|
| **"Tidak dapat terhubung ke server. Pastikan backend berjalan."** saat login | Backend (Terminal 1) belum jalan, masih loading, atau sudah ditutup | Jalankan langkah 5. Tunggu sampai `Started DdbBackendApplication`. Cek http://localhost:8080/actuator/health |
| `... cannot be loaded because running scripts is disabled` | Kebijakan PowerShell | Lakukan langkah 4 |
| `cd : Cannot find path '...\backend'` | Salah folder | Pastikan kamu di folder utama repo, lalu `cd code_area\backend` (bukan `cd backend`) |
| `npm : The term 'npm' is not recognized` | Node.js belum terinstal / PowerShell belum dibuka ulang | Instal Node.js, tutup & buka PowerShell |
| `JAVA_HOME is not set` atau `java.exe` tidak ditemukan | Java belum ada | Instal Temurin 21 (langkah 2) **atau** Android Studio, lalu buka ulang PowerShell |
| `Port 8080 was already in use` | Backend sudah jalan di jendela lain | Pakai yang sudah jalan, atau tutup jendela lama |
| `Port 5173 is in use, trying another one...` | Web sudah jalan di jendela lain | Buka alamat yang ditampilkan (mis. 5174), atau tutup jendela lama |
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
cd <folder>\diponegoro-digital-bank\code_area\apps\web
npm install        # pertama kali saja
npm run dev
#   → buka http://localhost:5173  (demo / Demo@1234 / PIN 123456)

# ===== TERMINAL 3: Tes otomatis (opsional) =====
cd <folder>\diponegoro-digital-bank\code_area\pengujian
npm run web:api
npm run web:ui

# ===== Selesai =====
# Ctrl + C di setiap terminal
```

Ambil versi kode terbaru dari GitHub: `git pull` di folder utama repo.
