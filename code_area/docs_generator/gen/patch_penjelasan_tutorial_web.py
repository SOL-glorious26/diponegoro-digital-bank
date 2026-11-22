"""22 Nov 2026: PENJELASAN_TEKNOLOGI_WEB.docx dan TUTORIAL_MENJALANKAN_APLIKASI_WEB.docx ditulis ulang untuk Web tim (Node/Express/EJS).
Format (gaya, tabel, daftar) diambil dari berkas asli. Markup: **tebal**, `kode`.
Pemakaian: python patch_penjelasan_tutorial_web.py <penjelasan asli> <tutorial asli> <folder keluaran>
"""
import copy, os, re, sys, docx
from docx.oxml.ns import qn
from docx.text.paragraph import Paragraph
from docx.table import Table
from docx_isi import Doc
A, B, OUT = sys.argv[1:4]


# =============================================================== PENJELASAN
d = Doc(A)
d.rows(d.T[0], [['Proyek', 'Diponegoro Digital Bank (bank digital simulasi)'],
    ['Tim', 'Kelompok Jaran Goyang. Web dikembangkan tim Web: Permata Surya Cahyana (backend Web), Rafi Fadhillah (front end dan backend Web), Steven Wibisana dan Nabila Puspa Annastasya (front end)'],
    ['Mata kuliah', 'Software Engineering, Universitas Kristen Satya Wacana'],
    ['Sumber data', 'Kode aplikasi (apps/web/digitalbanking-node v2.0.0: package.json, server.js, lib/, routes/, views/) dan dokumen D02 Bab VIII, per 22 November 2026']])
H1, H2, BL, NM, NOTE = (copy.deepcopy(d.P[i]._p) for i in (7, 12, 18, 8, 10))
TBL = [copy.deepcopy(t._tbl) for t in d.T]
body = d.d.element.body
kids = list(body); start = kids.index(d.P[7]._p)
for e in kids[start:]:
    if e.tag != qn('w:sectPr'):
        body.remove(e)
end = body.find(qn('w:sectPr'))


def add(kind, text):
    e = copy.deepcopy({'1': H1, '2': H2, '-': BL, 'p': NM, 'n': NOTE}[kind]); end.addprevious(e); Doc.fill(Paragraph(e, d.d._body), text)


def tab(k, data):
    e = copy.deepcopy(TBL[k]); end.addprevious(e); d.rows(Table(e, d.d._body), data)
    sp = copy.deepcopy(NM); end.addprevious(sp); q = Paragraph(sp, d.d._body)
    for r in q.runs:
        r.text = ''


X = [
('1', '1. Gambaran Umum'),
('p', 'Aplikasi Web adalah **thin client** (klien tipis) dengan pola **backend for frontend (BFF)**: peramban hanya menerima halaman HTML dan mengirim formulir ke server Node, lalu server Node memanggil backend Spring Boot. Saldo, aturan transaksi, dan pencatatan ledger tetap di backend, sehingga aturan bisnis tidak ditulis dua kali dan datanya selalu sama dengan aplikasi Android.'),
('p', 'Aplikasi memiliki **11 halaman** (WEB-01 sampai WEB-11) yang dirender di server (**server-side rendering**), terdiri atas 5 berkas JavaScript server (sekitar 720 baris) dan 14 templat EJS (sekitar 490 baris). Cakupannya: pembukaan akun, dashboard, transfer, pembayaran, riwayat beserta ekspor (FR-26), dan profil. Web tidak memiliki basis data sendiri.'),
('T', 1, [['Bahasa', 'JavaScript (CommonJS) + templat EJS', '–', 'Bahasa kode server dan tampilan'], ['Lingkungan', 'Node.js + npm', '18 ke atas (dicoba 24)', 'Menjalankan server Web dan mengelola pustaka'], ['Kerangka server', 'Express', '5.3.0', 'Rute, middleware, dan formulir'], ['Mesin templat', 'EJS', '3.1.10', 'Menyusun halaman HTML di server'], ['Sesi', 'express-session', '1.19.0', 'Menyimpan login di memori server'], ['Konfigurasi', 'dotenv', '16.6.1', 'Membaca berkas .env'], ['Komunikasi server', 'fetch bawaan Node', '–', 'Memanggil API backend'], ['Gaya tampilan', 'CSS buatan sendiri', '–', 'Tema dan tata letak responsif'], ['Skrip peramban', 'pwtoggle.js', '–', 'Tombol lihat/sembunyikan kata sandi dan PIN'], ['Pengujian', 'Skrip Node + Chrome DevTools Protocol / WebDriver BiDi', '–', 'Tes otomatis Web']]),
('n', 'Aplikasi hanya memakai **4 pustaka runtime** (express, ejs, express-session, dotenv) dan **tanpa alat build**. Kode langsung dijalankan oleh Node.'),
('1', '2. Bahasa dan Lingkungan'),
('2', '2.1 JavaScript'),
('-', '**Apa itu:** bahasa pemrograman yang lazim dipakai di peramban dan, lewat Node.js, juga di server.'),
('-', '**Dipakai untuk apa:** seluruh logika Web, yaitu rute, sesi, dan pemanggilan backend, ditulis dengan JavaScript modul CommonJS (berkas saling memakai lewat `require`).'),
('-', '**Konsekuensi:** tidak ada pemeriksaan tipe data otomatis seperti pada TypeScript.'),
('2', '2.2 Node.js dan npm'),
('-', '**Apa itu:** **Node.js** menjalankan JavaScript di luar peramban. **npm** mengunduh dan memperbarui pustaka.'),
('-', '**Cara kerjanya:** daftar pustaka ada di `package.json`, versi pastinya di `package-lock.json`. Perintah `npm install` mengunduhnya ke folder node_modules (tidak disimpan di GitHub).'),
('-', '**Dipakai untuk apa:** menjalankan server Web dengan `npm start` di port 3000.'),
('1', '3. Kerangka Server dan Tampilan'),
('2', '3.1 Express 5'),
('-', '**Apa itu:** kerangka server web untuk Node.js.'),
('-', '**Cara kerjanya:** setiap permintaan melewati deretan **middleware** (fungsi yang dijalankan berurutan), lalu sampai ke **rute** yang cocok dengan alamatnya. Pada Express 5, kesalahan di fungsi async otomatis diteruskan ke penangan kesalahan.'),
('-', '**Dipakai untuk apa:** middleware di `server.js` menangani sesi, cookie perangkat, pesan sekali tampil, dan token CSRF. Rute dibagi dua: `routes/auth.js` (masuk, daftar, e-KYC, PIN, pemulihan) dan `routes/app.js` (dashboard, transfer, pembayaran, riwayat, profil).'),
('2', '3.2 EJS'),
('-', '**Apa itu:** mesin templat yang menyisipkan data ke dalam HTML di server.'),
('-', '**Cara kerjanya:** server mengisi templat dengan data dari backend lalu mengirim HTML jadi. Teks yang ditampilkan dengan `<%= %>` otomatis di-escape, sehingga membantu mencegah penyisipan skrip (XSS).'),
('-', '**Dipakai untuk apa:** 11 halaman di folder views, ditambah potongan bersama (kepala dan menu samping, penutup, token CSRF).'),
('2', '3.3 Server-Side Rendering'),
('-', '**Apa itu:** halaman disusun di server, bukan di peramban.'),
('-', '**Kelebihan:** tanpa proses build, peramban menerima halaman jadi, dan token login tidak pernah sampai ke peramban. Konsekuensinya, setiap aksi memuat halaman baru.'),
('1', '4. Navigasi dan Pelindung Halaman'),
('2', '4.1 Rute'),
('-', '**Apa itu:** alamat halaman, misalnya /login, /transfer, dan /riwayat.'),
('-', '**Dipakai untuk apa:** area publik (masuk, daftar, OTP, lupa sandi), langkah pembukaan akun (e-KYC, buat PIN), dan area terlindungi (dashboard, transfer, pembayaran, riwayat, profil).'),
('2', '4.2 Pelindung Halaman'),
('-', '**Cara kerjanya:** sebelum membuka area terlindungi, server memeriksa berurutan: **(1) sudah login? (2) e-KYC disetujui? (3) PIN sudah dibuat?** Bila belum, pengguna dialihkan ke langkah yang kurang (fungsi nextStep).'),
('-', '**Dipakai untuk apa:** menjalankan aturan SRS bahwa transaksi baru aktif setelah e-KYC (FR-02) dan memerlukan PIN (FR-04).'),
('1', '5. Pengelolaan Sesi'),
('2', '5.1 express-session'),
('-', '**Apa itu:** pustaka untuk mengingat pengguna antarpermintaan.'),
('-', '**Cara kerjanya:** data sesi (token, data pengguna, transaksi yang sedang dikonfirmasi) disimpan di **memori server Node**. Peramban hanya memegang cookie sesi yang **httpOnly** dan **SameSite=Lax**, berlaku 2 jam.'),
('-', '**Konsekuensi:** login hilang bila server dimulai ulang, dan server belum dapat digandakan tanpa penyimpanan sesi bersama.'),
('2', '5.2 Pesan Sekali Tampil dan Data Konfirmasi'),
('-', '**Dipakai untuk apa:** pesan hasil aksi ditampilkan sekali di halaman berikutnya. Data transfer atau pembayaran pada tahap konfirmasi disimpan di sesi, sehingga tidak dapat diubah dari formulir saat PIN dikirim.'),
('1', '6. Komunikasi dengan Backend'),
('2', '6.1 REST API dan JSON'),
('-', '**Apa itu:** cara meminta data atau menjalankan aksi ke server lewat alamat web dan perintah HTTP. JSON adalah format teks untuk bertukar data.'),
('-', '**Dipakai untuk apa:** Web memakai **backend dan kontrak API yang sama** dengan Android (73 operasi di /api/v1), sehingga saldo dan riwayat selalu sama.'),
('2', '6.2 Klien API (lib/backend.js)'),
('-', '**Apa itu:** satu-satunya berkas yang berbicara dengan backend, memakai `fetch` bawaan Node.'),
('-', '**Yang dikerjakan:** menambahkan token login, menambahkan Idempotency-Key pada transaksi, membatasi waktu tunggu 20 detik, memperbarui token **satu kali** bila backend menjawab 401 lalu mengulang permintaan, dan mengubah kesalahan backend menjadi pesan siap tampil.'),
('-', '**Pembaruan token bersama:** bila beberapa permintaan pada sesi yang sama membutuhkan token baru, hanya satu proses pembaruan yang berjalan.'),
('2', '6.3 Idempotency-Key'),
('-', '**Apa itu:** kode unik agar permintaan yang terkirim dua kali hanya diproses sekali.'),
('-', '**Cara kerjanya:** satu UUID dibuat pada tahap konfirmasi dan dipakai ulang saat PIN dicoba lagi. Backend mengembalikan hasil pertama tanpa memotong saldo lagi (BR-04).'),
('2', '6.4 CORS'),
('-', '**Apa itu:** aturan peramban yang membatasi pemanggilan ke alamat lain.'),
('-', '**Kenapa tidak diperlukan:** yang memanggil backend adalah server Node, bukan peramban. Konfigurasi CORS di backend tetap ada untuk klien peramban lain.'),
('2', '6.5 Penyimpanan Token (ADR-14)'),
('-', '**Keputusan:** access token dan refresh token disimpan di **sesi server** dan tidak pernah dikirim ke peramban, sehingga skrip di halaman tidak dapat membacanya.'),
('-', '**Bila refresh gagal** (misalnya setelah logout semua perangkat), pengguna kembali ke halaman masuk dengan pesan sesi berakhir.'),
('1', '7. Tampilan dan Antarmuka'),
('2', '7.1 CSS Buatan Sendiri'),
('-', '**Dipakai untuk apa:** seluruh tampilan memakai satu berkas CSS tanpa pustaka UI. Warna disimpan sebagai variabel: navy (#0B1F4B), biru (#2459E6), dan aksen emas (#F5B301), serta warna status berhasil, gagal, dan diproses.'),
('2', '7.2 Desain Responsif'),
('-', '**Cara kerjanya:** media query pada 860, 760, dan 420 piksel. Pada layar sempit, menu samping berubah menjadi baris menu yang dapat digeser.'),
('-', '**Status:** aturan CSS sudah ada, tetapi tampilan ponsel belum diuji otomatis. Lebar desktop diuji tanpa scroll horizontal.'),
('2', '7.3 Tombol Lihat PIN dan Kata Sandi'),
('-', '**Dipakai untuk apa:** `pwtoggle.js` menambahkan ikon mata pada semua kolom sandi dan PIN, lengkap dengan label untuk pembaca layar.'),
('2', '7.4 Format Angka dan Tanggal (Intl)'),
('-', '**Dipakai untuk apa:** nominal ditampilkan dalam format Indonesia (contoh Rp 1.500.000) dan waktu dalam WIB (Asia/Jakarta).'),
('1', '8. Fitur Web'),
('2', '8.1 Transaksi Dua Tahap'),
('-', '**Cara kerjanya:** isi formulir, lalu halaman konfirmasi menampilkan penerima, biaya, dan total, kemudian PIN. Rekening atau ID pelanggan yang salah ditolak sebelum PIN diminta. PIN salah tiga kali memblokir PIN.'),
('2', '8.2 Pembayaran'),
('-', '**Isi:** Top Up saldo, pulsa, token PLN, tagihan, e-wallet, dan QRIS (dari contoh atau payload). Top Up memakai simulator dana masuk, tanpa PIN.'),
('2', '8.3 Ekspor Riwayat (CSV dan PDF)'),
('-', '**Cara kerjanya:** backend membuat berkas sesuai filter yang aktif, dan Web meneruskannya ke peramban. Fitur ini hanya ada di Web (FR-26).'),
('2', '8.4 Cetak dan Cek Status'),
('-', '**Cetak:** detail transaksi dapat dicetak atau disimpan sebagai PDF lewat fungsi cetak peramban.'),
('-', '**Cek status:** transfer antarbank yang diproses dapat diperbarui statusnya dari detail riwayat.'),
('2', '8.5 Cakupan yang Dipilih Tim Web'),
('-', '**Tidak dimuat di Web (tersedia di Android):** kantong, kartu virtual, notifikasi, grafik saldo, penerima favorit, QRIS pribadi dan dinamis, serta paket data.'),
('1', '9. Keamanan dan Aksesibilitas'),
('2', '9.1 Keamanan'),
('-', '**CSRF:** setiap formulir membawa token; POST tanpa token yang cocok ditolak dengan HTTP 403.'),
('-', '**Sesi:** ID sesi dibuat ulang setelah login agar tidak dapat dibajak (session fixation).'),
('-', '**Validasi dua lapis:** isian diperiksa di Web, tetapi keputusan akhir selalu di backend.'),
('-', '**Keterbatasan:** masih HTTP lokal, cookie belum memakai atribut secure, dan sesi di memori.'),
('2', '9.2 Aksesibilitas Dasar'),
('-', '**Yang ada:** bahasa halaman (id), label pada kolom isian, label tombol lihat sandi, dan status transaksi berupa teks, bukan hanya warna.'),
('1', '10. Menjalankan Aplikasi'),
('T', 2, [['npm start', 'Server Web di port 3000', 'Membaca .env (PORT, BACKEND_URL, SESSION_SECRET)'], ['npm run dev', 'Server Web yang dimulai ulang otomatis', 'Setiap kode disimpan (node --watch)'], ['npm run test:lib', 'Uji klien API', 'Terhadap server tiruan']]),
('p', 'Aplikasi **tidak memerlukan build**. Saat ini dijalankan secara lokal bersama backend (profil H2). Rencana deployment berada di belakang API Gateway Nginx; Docker dan HTTPS belum dipasang.'),
('1', '11. Pengujian'),
('2', '11.1 Cara Uji'),
('-', '**Skrip HTTP:** mengirim formulir seperti peramban (cookie dan token CSRF), lalu mencocokkan saldo ke backend.'),
('-', '**Peramban nyata:** Chrome dan Edge dikendalikan lewat Chrome DevTools Protocol, Firefox lewat WebDriver BiDi, semuanya tanpa jendela (headless).'),
('2', '11.2 Hasil (22 November 2026, STD-DDB-001 v1.0)'),
('-', '**Uji sistem Web:** WN-01 sampai WN-25, **25/25 lulus** terhadap backend asli.'),
('-', '**Lintas peramban:** Chrome 157, Edge 158, dan Firefox 156, **24/24 pemeriksaan lulus**. Safari belum diuji.'),
('-', '**Lintas platform:** LPN-01 sampai LPN-03, **3/3 lulus**: transfer di Android tampil di Web, e-wallet di Web tampil di Android, dan logout semua perangkat mengakhiri sesi Web.'),
('-', '**Kinerja:** Dashboard termuat maksimal **197 ms** pada server lokal.'),
('1', '12. Perbandingan Web dan Android'),
('T', 3, [['Bahasa dan kerangka', 'JavaScript + Express + EJS', 'Kotlin + Jetpack Compose'], ['Alat build', 'Tidak ada', 'Gradle'], ['Backend', 'Sama (satu backend, 73 operasi API)', 'Sama'], ['Komunikasi server', 'fetch bawaan Node (server ke server)', 'Retrofit + OkHttp'], ['Penyimpanan token', 'Sesi server Node', 'Terenkripsi dengan Android Keystore'], ['Pindai QRIS dengan kamera', 'Tidak ada (FR-15 khusus mobile)', 'Ada'], ['Login biometrik', 'Tidak ada', 'Ada (masih simulasi)'], ['Ekspor riwayat CSV/PDF', 'Ada (FR-26)', 'Tidak ada'], ['Notifikasi', 'Tidak ada', 'Kotak notifikasi + notifikasi Android'], ['Mode tanpa server', 'Tidak ada', 'Ada (varian demo)']]),
('1', '13. Ringkasan Teknologi dan Kelebihannya'),
('T', 4, [['Express + EJS', 'Aplikasi satu halaman (SPA)', 'Tanpa build; halaman disusun di server; token tidak sampai ke peramban'], ['JavaScript', 'TypeScript', 'Sederhana (kekurangan: tanpa cek tipe)'], ['Sesi server', 'Token di peramban', 'Token tidak dapat dibaca skrip halaman'], ['fetch bawaan Node', 'Pustaka HTTP tambahan', 'Tanpa ketergantungan tambahan; satu titik untuk token, refresh, dan Idempotency-Key'], ['CSS sendiri', 'Bootstrap, Tailwind', 'Ringan dan tampilan terkendali penuh'], ['Skrip tes sendiri (CDP/BiDi)', 'Selenium, Cypress, Playwright', 'Tanpa pustaka tambahan dan memakai peramban nyata']]),
('n', 'Rujukan teknis lengkap: **D02 Architectural Design (Bab VIII)**, **D04W Detailed Design Web**, **D05W User Interface Map Web**, **D07W Developer Manual Web**, dan `code_area/apps/web/digitalbanking-node/README.md`.'),
]
for x in X:
    tab(x[1], x[2]) if x[0] == 'T' else add(*x)
d.d.save(os.path.join(OUT, 'pen_raw.docx'))

# =============================================================== TUTORIAL
d = Doc(B)
d.set(1, 'Diponegoro Digital Bank · dari laptop kosong sampai aplikasi berjalan · 22 November 2026')
d.rows(d.T[0], [['Terminal 1', 'Backend (Java)', 'http://localhost:8080', 'Web terbuka, tetapi login gagal: "Tidak dapat terhubung ke backend"'], ['Terminal 2', 'Web (Node.js)', 'http://localhost:3000', 'Chrome menampilkan "This site can\'t be reached"']])
d.set(4, 'Alur datanya: **Chrome → Web (port 3000) → Backend (port 8080) → Database**. Kesalahan paling sering adalah hanya menjalankan Web tanpa backend. Nyalakan **Terminal 1 lebih dulu**.')
d.set(27, 'cd code_area\\apps\\web\\digitalbanking-node')
d.set(28, 'copy .env.example .env   # hanya pertama kali')
d.set(29, 'npm install              # hanya pertama kali')
d.after(29, 'npm start')
d.set(30, 'Tunggu sampai server Web berjalan di alamat `http://localhost:3000`.')
d.set(31, 'Buka `http://localhost:3000` di Chrome. Berhasil bila tampil halaman **Masuk** dengan logo Diponegoro.')
d.set(32, '**Jangan tutup jendela ini juga.** Server Web memanggil backend di port 8080 secara otomatis. Bila backend ada di alamat lain, ubah `BACKEND_URL` di berkas `.env`. Jangan unggah `.env` ke GitHub.')
d.set(34, 'Berhasil bila setelah klik **Login** muncul **Dashboard** dengan saldo, transaksi terakhir, dan menu di kiri. Untuk mencoba dua akun bersamaan (misalnya demo mengirim dan andi menerima), buka akun kedua di **jendela Incognito** (Ctrl+Shift+N).')
d.rows(d.T[3], [
    ['Transfer sesama bank', 'Transfer → Sesama Bank (DDB) → rekening 1400000002 → Rp10.000 → Lanjut ke Konfirmasi → PIN 123456', 'Nama ANDI PRATAMA tampil sebelum PIN; biaya Rp0; detail transaksi terbuka'],
    ['Rekening salah', 'Rekening DDB 1400999999', '"Rekening tujuan tidak valid"; PIN tidak diminta'],
    ['Ditolak bank tujuan', 'Ke Bank Lain → BRI → 9999999999', 'Gagal; dana dikembalikan, saldo tidak berkurang'],
    ['Timeout antarbank', 'Ke Bank Lain → Mandiri → 8888888888', 'Status Diproses; klik Cek status; tidak terpotong dua kali (biaya Rp2.500)'],
    ['Nominal terlalu kecil', 'Rp1.000', 'Pesan nominal minimal Rp5.000'],
    ['PIN salah', 'Masukkan 000000', 'Pesan PIN salah; 3 kali salah → PIN diblokir 5 menit'],
    ['Top Up saldo', 'Pembayaran → Top Up → Rp50.000', 'Saldo bertambah Rp50.000 tanpa PIN'],
    ['Pulsa', 'Pembayaran → Pulsa → 081234567890 → Rp10.000 → PIN', 'Berhasil; total Rp11.500'],
    ['Token listrik', 'Pembayaran → Token PLN → ID 12345678901 → Rp20.000 → PIN', 'Berhasil; nomor token tampil di detail. ID berakhiran 0000 ditolak'],
    ['e-KYC ditolak', 'Akun baru dengan NIK berakhiran 0000', 'Status ditolak'],
    ['Ekspor riwayat', 'Riwayat → CSV lalu PDF', 'Berkas terunduh sesuai filter'],
    ['Ubah profil', 'Profil → ganti email → Simpan Perubahan → OTP yang tampil', 'Profil diperbarui'],
])
d.set(36, 'Setelah tes PIN salah, PIN akun demo **terblokir 5 menit**. Tunggu, atau lanjutkan dengan akun budi. OTP **ditampilkan di layar** karena belum ada SMS atau email sungguhan. Kantong, kartu virtual, notifikasi, QRIS pribadi, dan paket data hanya ada di Android.')
d.set(40, 'npm run web:node')
d.rows(d.T[4], [['npm run web:node', 'Alur inti Web (WN-01 sampai WN-14)', 'sekitar 20 detik'], ['npm run web:node:ekstra', 'Uji lanjutan (WN-15 sampai WN-25)', 'sekitar 2 menit'], ['npm run web:node:peramban', 'Alur inti di Chrome, Edge, dan Firefox (butuh ketiganya terpasang)', 'sekitar 1 menit'], ['npm run lintas:node', 'Lintas platform dengan emulator Android (LPN-01 sampai LPN-03)', 'sekitar 2 menit'], ['npm run perf', 'Kinerja API dan Dashboard', 'sekitar 20 detik']])
d.set(41, 'Setiap baris diawali **PASS** atau **FAIL**; di akhir ada ringkasan seperti **14/14 PASS**. Tes otomatis membuat transaksi di basis data lokal, dan itu normal. WN-21 butuh backend kedua (port 8081, token 1 menit) dan Web kedua (port 3001); tanpa itu WN-21 akan FAIL.')
T5 = [[c.text for c in r.cells] for r in d.T[5].rows[1:]]
T5[0][0] = '"Tidak dapat terhubung ke backend" saat login'
assert T5[6][0].startswith('Port 5173'); T5[6] = ['EADDRINUSE port 3000', 'Web sudah berjalan di jendela lain', 'Tutup jendela lama, atau ubah PORT di .env']
T5.insert(7, ['403 "Token keamanan tidak valid"', 'Formulir lama setelah Web dimulai ulang', 'Muat ulang halaman, lalu ulangi'])
T5[8][1] = 'Backend dimulai ulang atau logout semua perangkat'
for i, r in enumerate(T5):
    pass
d.rows(d.T[5], T5)
d.set(57, 'cd <folder>\\diponegoro-digital-bank\\code_area\\apps\\web\\digitalbanking-node')
d.set(58, 'copy .env.example .env   # pertama kali saja')
d.set(59, 'npm install              # pertama kali saja')
d.after(59, 'npm start                # buka http://localhost:3000')
d.set(63, 'npm run web:node ; npm run web:node:ekstra')
d.set(64, 'Rujukan lengkap dengan seluruh skenario tes manual: `TUTORIAL_MENJALANKAN_WEB.md` di folder utama repo dan **TUT-DDB-002-WEB v2.0** di `documents_area/TUT_Panduan_Menjalankan_Web/`.')
d.d.save(os.path.join(OUT, 'TUTORIAL_MENJALANKAN_APLIKASI_WEB.docx'))
for f in ('pen_raw.docx', 'TUTORIAL_MENJALANKAN_APLIKASI_WEB.docx'):
    x = docx.Document(os.path.join(OUT, f)); tx = '\n'.join(p.text for p in x.paragraphs) + '\n'.join(c.text for t in x.tables for r in t.rows for c in r.cells)
    print(f, [m for m in re.findall(r'React|Vite|5173|axios|SPA\b|JSX|web:api|web:ui|npm run dev', tx)])
