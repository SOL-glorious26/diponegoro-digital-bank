"""D03 FSD-DDB-001 v0.9.2 -> v0.9.3: ubah doc3.js agar sama dengan STD-DDB-001 v0.9.6 (16 Nov 2026)
dan tambahkan uraian singkat pada setiap workflow Bab VI.
Pemakaian: python patch_doc3_093.py <folder gen>, lalu:
node doc3.js ../fig ../fig3 <OUT.docx> ../../submit_mobile/diponegoro-corp-logo.jpeg ../../backend/docs/openapi.json
"""
import sys, os
os.chdir(sys.argv[1])
p = 'doc3.js'
s = open(p, encoding='utf-8').read()


def rep(a, b):
    global s
    assert s.count(a) == 1, (s.count(a), a[:80])
    s = s.replace(a, b)


# Meta dan riwayat revisi
rep('code: "FSD-DDB-001", version: "0.9.2",', 'code: "FSD-DDB-001", version: "0.9.3",')
rep('SAD-DDB-001 v0.9.4; STD-DDB-001 v0.9.5;', 'SAD-DDB-001 v0.9.5; STD-DDB-001 v0.9.6;')
rep('kode backend, Android, dan Web per 13 November 2026",\n  date: "13 November 2026",',
    'kode backend, Android, dan Web per 16 November 2026",\n  date: "16 November 2026",')
rep('isu terbuka tentang verifikasi Web ditutup."],\n  ],',
    'isu terbuka tentang verifikasi Web ditutup."],\n'
    '    ["0.9.3", "16 Nov 2026", "Salomo P.G. Hokoyoku", "Status disamakan dengan STD-DDB-001 v0.9.6: pemindaian QRIS dengan kamera (FR-15) lulus di ponsel fisik, Android 28/28; Tabel 7.1, catatan status, dan isu terbuka diperbarui; uraian singkat ditambahkan pada setiap workflow Bab VI; kalimat komunikasi HTTP/HTTPS pada 4.4 diperjelas."],\n  ],')

# Bab I dan IV
rep('diverifikasi terhadap implementasi backend dan Android per 11 November 2026."',
    'diverifikasi terhadap implementasi backend, Android, dan Web serta hasil uji sistem STD-DDB-001 v0.9.6 per 16 November 2026."')
rep('B("**Komunikasi:** HTTPS ke API Gateway (TLS belum dipasang, lihat SAD-DDB-001 Bab IX);',
    'B("**Komunikasi:** HTTP pada lingkungan pengembangan dan uji; HTTPS wajib saat deployment publik (TLS di API Gateway belum dipasang, lihat SAD-DDB-001 Bab IX);')

# Bab VI: uraian per workflow (disisipkan sebelum gambar)
WF = {
    '6.1 Registrasi dan Aktivasi Akun': 'Pengguna mengisi username, password, dan kontak, lalu memasukkan OTP yang berlaku 2 menit. Setelah OTP valid, akun berstatus ACTIVE dan pengguna langsung masuk. Pengguna kemudian mengirim NIK untuk e-KYC; bila disetujui, akun menjadi FINANCIAL_ENABLED dan pengguna membuat PIN transaksi 6 digit. Bila e-KYC ditolak, pengguna dapat mengajukan ulang, dan selama itu seluruh transaksi keuangan ditolak (KYC_REQUIRED).',
    '6.2 Login dan Sesi': 'Pengguna masuk dengan username, email, atau nomor telepon dan password; di Android tersedia juga login biometrik bagi perangkat yang sudah didaftarkan. Lima kali password salah mengunci login selama 15 menit. Setelah login, sistem menerbitkan access token 15 menit dan refresh token 7 hari. Sesi berakhir setelah 5 menit tidak aktif, dan logout semua perangkat langsung membatalkan seluruh sesi aktif.',
    '6.3 Transfer Dana (UC-01)': 'Pengguna memilih jenis transfer, lalu sistem memvalidasi rekening tujuan dan menampilkan nama penerima. Setelah nominal diisi, layar konfirmasi menampilkan nominal, biaya, dan total; pengguna kemudian memasukkan PIN. Jalur alternatif mengikuti SRS UC-01: A1 saldo tidak cukup, A2 nominal di luar batas, A3 rekening tidak valid, A4 PIN salah (blokir setelah 3 kali), A5 waktu habis (status Diproses dan dicek ulang tanpa debit ganda), serta A6 ditolak jaringan antarbank (dana dikembalikan otomatis).',
    '6.4 Pembayaran QRIS': 'Di Android, pengguna memindai kode QRIS dengan kamera atau memilih gambar dari galeri. Sistem memeriksa format EMVCo dan checksum CRC16, lalu menampilkan merchant dan nominal. Pada QRIS statis pengguna mengisi nominal, sedangkan pada QRIS dinamis nominal sudah terkunci. Batas nominal dan saldo diperiksa sebelum dialog PIN, dan pengguna tidak dapat membayar QRIS miliknya sendiri.',
    '6.5 Top-up dan Tagihan': 'Pengguna memilih produk (pulsa, paket data, token listrik, tagihan PDAM/BPJS/Internet, atau e-wallet) dan mengisi nomor HP atau ID pelanggan. Sistem mendeteksi operator atau melakukan inquiry pelanggan, lalu menampilkan ringkasan harga dan biaya admin. Saldo baru didebet setelah PIN valid, dan bukti transaksi tersimpan di riwayat.',
    '6.6 Kantong dan Locking': 'Pengguna membuat kantong (maksimal 10) dan mengisinya dari rekening utama. Kantong dapat dikunci sampai tanggal tertentu atau sampai target tercapai; selama terkunci, dananya tidak dapat dipakai untuk transaksi. Penarikan sebelum kunci terbuka memerlukan penarikan dini dan PIN (BR-06). Saat kantong dihapus, sisa saldonya dipindahkan ke rekening utama.',
    '6.7 Virtual Card': 'Pengguna yang e-KYC-nya disetujui dapat membuat kartu virtual domestik atau internasional (maksimal 5). Nomor kartu dan CVV ditampilkan tersamar dan baru terlihat lengkap setelah PIN valid. Pengguna dapat membekukan dan mengaktifkan kembali kartu serta mengatur limit per transaksi. Kartu beku menolak semua transaksi, dan kartu domestik menolak merchant luar negeri.',
    '6.8 Riwayat dan Ekspor': 'Riwayat ditampilkan dari yang terbaru dengan paginasi. Filter tanggal, jenis, dan status dapat digabung dengan pencarian kata kunci. Memilih satu transaksi membuka detail beserta baris jurnal ledger. Di Web, pengguna dapat mengekspor riwayat sesuai filter aktif ke CSV atau PDF.',
    '6.9 Perubahan Profil': 'Pengguna mengajukan perubahan username, email, atau nomor telepon. Sistem mengirim OTP ke kontak baru, dan perubahan baru berlaku setelah OTP tersebut valid. Bila tidak ada data yang berubah, sistem menolak dengan kode NO_CHANGES.',
}
for h, text in WF.items():
    rep(f'  H2("{h}"),\n', f'  H2("{h}"),\n  P("{text}"),\n')

# Bab VII
rep('hasil uji sistem STD-DDB-001 v0.9.5 per 13 November 2026.', 'hasil uji sistem STD-DDB-001 v0.9.6 per 16 November 2026.')
rep('Uji sistem dijalankan di emulator Android dan peramban Chrome, Edge, serta Firefox; uji di ponsel fisik belum dilakukan.',
    'Uji sistem dijalankan di emulator Android, ponsel fisik Realme RMX3867 (Android 16), dan peramban Chrome, Edge, serta Firefox.')
rep('"Ada (kamera, galeri, QR demo); kamera menunggu uji ponsel fisik"', '"Ada (kamera, galeri, QR demo); kamera lulus di ponsel fisik"')
rep("lulus uji sistem pada STD-DDB-001 v0.9.5: Android SA 27/28 (SA-08 kamera menunggu ponsel fisik),",
    "lulus uji sistem pada STD-DDB-001 v0.9.6: Android SA 28/28 (SA-08 pindai QRIS dengan kamera di ponsel fisik),")
rep('    "Pemindaian QRIS dengan kamera (FR-15) dan perilaku di ponsel fisik belum diuji; dijadwalkan bersama uji ponsel fisik (STD-DDB-001 SA-08, NF-07).",\n', '')
rep('"Kelompok Jaran Goyang. (2026). System Testing (STD-DDB-001) v0.9.5."', '"Kelompok Jaran Goyang. (2026). System Testing (STD-DDB-001) v0.9.6."')
rep('"Kelompok Jaran Goyang. (2026). Architectural Design (SAD-DDB-001) v0.9.4."', '"Kelompok Jaran Goyang. (2026). Architectural Design (SAD-DDB-001) v0.9.5."')
rep('"Kelompok Jaran Goyang. (2026). Project Document (PRJ-DDB-001) v0.9.3."', '"Kelompok Jaran Goyang. (2026). Project Document (PRJ-DDB-001) v0.9.4."')

open(p, 'w', encoding='utf-8').write(s)
print('ok')
