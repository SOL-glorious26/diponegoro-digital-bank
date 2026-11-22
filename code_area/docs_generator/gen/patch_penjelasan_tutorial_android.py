"""22 Nov 2026: PENJELASAN_TEKNOLOGI_ANDROID.docx dan TUTORIAL_MENJALANKAN_ANDROID_EMULATOR.docx disamakan dengan kode 22 Nov (tampilan baru, 112 berkas, uji SA 28/28).
Pemakaian: python patch_penjelasan_tutorial_android.py <penjelasan asli> <tutorial asli> <folder keluaran>
"""
import os, re, sys, docx
from docx_isi import Doc
A, B, OUT = sys.argv[1:4]
d = Doc(A)


def at(start):
    hit = [i for i, p in enumerate(d.P) if p.text.startswith(start)]
    assert len(hit) == 1, (start, len(hit)); return hit[0]


d.rows(d.T[0], [['Proyek', 'Diponegoro Digital Bank (bank digital simulasi)'], ['Tim', 'Kelompok Jaran Goyang – Tim Mobile: Salomo (full stack), Cecilia, Gracia, dan Grace (front end)'], ['Mata kuliah', 'Software Engineering, Universitas Kristen Satya Wacana'],
    ['Sumber data', 'Kode aplikasi (app/build.gradle.kts, gradle/libs.versions.toml, dan kode Kotlin) per 22 November 2026']])
i = at('Aplikasi mobile Diponegoro Digital Bank adalah')
d.set(i, 'Aplikasi mobile Diponegoro Digital Bank adalah aplikasi **Android native**, artinya dibuat khusus untuk sistem operasi Android memakai bahasa dan pustaka resmi Android, bukan aplikasi web yang dibungkus. Aplikasi ini menggabungkan **8 modul bisnis** (Authentication, Account, Transfer, Payment QRIS, Top-up, Virtual Card, Riwayat & Dashboard, Profil & Notifikasi) ke dalam satu aplikasi, dengan **112 berkas kode Kotlin** (sekitar 9.900 baris tanpa komentar dan baris kosong).')
d.set(at('Dipakai untuk apa: komponen seperti tombol, kolom isian'), '**Dipakai untuk apa:** komponen seperti tombol, kolom isian, panel, dialog PIN, dan bottom bar dibangun di atas Material 3. Sejak 21 November 2026 aplikasi memakai **tema sendiri**: Midnight Navy (#0B1D33) sebagai warna utama, Diponegoro Blue (#0B3D91), dan Muted Gold (#B69A62) yang dipakai terbatas sebagai aksen, lengkap dengan **mode gelap** yang mengikuti pengaturan ponsel. Ikon menu diambil dari Material Icons Extended.')
d.after(at('Dipakai untuk apa: komponen seperti tombol, kolom isian'), '**Huruf dan status:** huruf memakai **Plus Jakarta Sans** (disertakan di aplikasi) dengan angka berlebar tetap agar nominal tidak bergeser. Status transaksi selalu ditampilkan dengan ikon dan teks, dan warnanya berbeda untuk mode terang dan gelap agar tetap terbaca.')
d.set(at('Dipakai untuk apa: mengatur 23 rute aplikasi'), '**Dipakai untuk apa:** mengatur **23 rute** aplikasi dan **bottom bar 5 tab** (Beranda, Riwayat, QRIS di tengah, Kantong, Profil). Navigasi juga menjaga urutan masuk: login → e-KYC → buat/masukkan PIN → beranda, dan menolak layar transaksi bila pengguna belum login.')
d.set(at('Dipakai untuk apa: seluruh 23 layar aplikasi dibuat'), '**Dipakai untuk apa:** seluruh **23 layar** aplikasi dibuat dengan Compose: login, PIN dengan keypad, beranda, transfer, QRIS, top-up, kartu virtual, riwayat, profil, dan lainnya. Versi pustaka diatur bersama lewat **Compose BOM 2026.02.01** (Bill of Materials, daftar versi yang dijamin cocok satu sama lain).')
d.after(at('Dipakai untuk apa: seluruh 23 layar aplikasi dibuat'), '**Menyesuaikan layar:** layar PIN berubah menjadi dua kolom saat ponsel dimiringkan, dan PIN dikirim otomatis pada digit keenam.')
d.set(at('Dipakai untuk apa: varian online berkomunikasi dengan backend'), '**Dipakai untuk apa:** varian online memakai **69 dari 73 operasi API** backend di alamat `/api/v1` (login, saldo, transfer, QRIS, top-up, kartu, riwayat, profil). Operasi yang tidak dipakai misalnya ekspor riwayat, yang khusus Web. **Backend yang sama** dipakai aplikasi Web tim (Node.js), sehingga data di HP dan di peramban selalu sama.')
d.set(at('Dipakai untuk apa: seluruh 73 operasi backend'), '**Dipakai untuk apa:** seluruh **69 operasi** backend yang dipakai Android terdaftar di satu tempat (DdbApi). Versi 3.0.0.')
d.set(at('PIN transaksi: setiap transaksi wajib'), '**PIN:** diminta pada setiap login (keypad) dan setiap transaksi (dialog); 3 kali salah → **diblokir 5 menit** (BR-02). Transaksi juga hanya bisa dilakukan bila e-KYC sudah disetujui.')
d.set(at('Dipakai untuk apa: test tur aplikasi yang membuka'), '**Dipakai untuk apa:** test tur aplikasi yang membuka layar-layar utama, serta test yang menggambar layar dan komponen pada mode gelap untuk diperiksa. Versi Robolectric 4.17.')
d.set(at('Test otomatis: varian demo 62 lulus'), '**Test otomatis (22 November 2026):** varian demo **63 lulus + 1 dilewati** dari 64; varian online **57 lulus + 1 dilewati** dari 58. Test yang dilewati hanya berjalan bila alamat backend sungguhan diberikan.')
i = at('Uji sistem: 28/28 kasus lulus')
d.set(i, '**Uji sistem:** **28/28 kasus lulus** pada APK final di emulator **Pixel 8 Android 17** (STD-DDB-001 v1.0), termasuk pindai QRIS dengan kamera virtual emulator. Pada HP fisik **Realme RMX3867 Android 16**, pindai QRIS, notifikasi, dan berbagi bukti lulus pada 14 November 2026 (build sebelum pembaruan tampilan).')
d.after(i, '**Lintas platform:** **3/3 skenario lulus** bersama Web tim (LPN-01 sampai LPN-03): transfer Android tampil di Web, e-wallet di Web tampil di Android, dan logout semua perangkat mengakhiri sesi Web.')
d.set(at('Rujukan teknis lengkap'), 'Rujukan teknis lengkap: **D02 Architectural Design (Bab VII)**, **D04A Detailed Design Android**, **D05A User Interface Map Android**, dan **D07A Developer Manual Android**.')
d.d.save(os.path.join(OUT, 'pena_raw.docx'))

d = Doc(B)
d.set(at('Diponegoro Digital Bank · Android Studio'), 'Diponegoro Digital Bank · Android Studio · 22 November 2026')
d.rows(d.T[0], [['demoDebug', 'Tidak', 'Paling mudah. Data simulasi di dalam aplikasi; hilang saat aplikasi ditutup. Cocok untuk mencoba, tangkapan layar, dan cadangan demo.'], ['onlineDebug', 'Ya (laptop yang sama)', 'Data dari server; sama dengan aplikasi Web tim. Dipakai untuk demo lengkap dan pengujian.']])
i = [k for k, p in enumerate(d.P) if p.text.startswith('Tekan tombol Run')][0]
d.set(i, 'Tekan tombol **Run ▶** (atau **Shift+F10**). Emulator menyala dan aplikasi terpasang otomatis dengan nama **Diponegoro**.')
d.set(at('Login: demo / Demo@1234'), 'Login: **demo / Demo@1234**, lalu ketuk PIN **123456** pada keypad (terkirim otomatis pada digit keenam).')
d.set(at('Login demo / Demo@1234, PIN'), 'Login **demo / Demo@1234**, lalu PIN **123456** pada keypad. Beranda menampilkan saldo dari server.')
rows = [[c.text for c in r.cells] for r in d.T[1].rows[1:]]
rows.append(['Kartu virtual', 'Menu Kartu → buat kartu → Lihat detail → PIN; nomor dan CVV tampil setelah PIN benar'])
d.rows(d.T[1], rows)
rows = [[c.text for c in r.cells] for r in d.T[2].rows[1:]]
rows.append(['Pindai QRIS dengan kamera di emulator', 'Pakai "Pilih gambar QR dari galeri" atau QR contoh. Kamera virtual juga bisa dipakai: pasang gambar QR di Extended Controls → Camera → Wall, lalu arahkan kamera ke dinding itu.'])
d.rows(d.T[2], rows)
d.set(at('Rujukan lebih lengkap'), 'Rujukan lebih lengkap: **D07A Developer Manual Android** dan `code_area/apps/android/README.md`. Untuk memasang di HP fisik, lihat **TUT-DDB-001-AND** di `documents_area/TUT_Panduan_Uji_Ponsel/`.')
d.d.save(os.path.join(OUT, 'TUTORIAL_MENJALANKAN_ANDROID_EMULATOR.docx'))
for f in ('pena_raw.docx', 'TUTORIAL_MENJALANKAN_ANDROID_EMULATOR.docx'):
    x = docx.Document(os.path.join(OUT, f)); tx = '\n'.join(p.text for p in x.paragraphs) + '\n'.join(c.text for t in x.tables for r in t.rows for c in r.cells)
    print(f, re.findall(r'17 November|4 tab|v0\.9\.\d|62 lulus|110 berkas|seluruh 73|biru tua dan emas', tx))
