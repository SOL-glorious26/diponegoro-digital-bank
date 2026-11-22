"""D06 v1.0 (22 Nov 2026): final, Web tim (Node) WN/LPN, DEF-12 s.d. DEF-14, eksekusi ulang SA pada APK final.
Pemakaian: python patch_doc6_100.py <folder gen>  lalu  node doc6.js <fig> <out.docx> <logo> (tanda tangan dari documents_area/_aset)
"""
import sys, os
os.chdir(sys.argv[1])
raw = open('doc6.js', encoding='utf-8', newline='').read()
crlf = '\r\n' in raw
s = raw.replace('\r\n', '\n')


def rep(a, b, n=1):
    global s
    assert s.count(a) == n, (s.count(a), a[:90])
    s = s.replace(a, b)


rep('const [, , FIG, OUT, LOGO] = process.argv;', 'const [, , FIG, OUT, LOGO, SIGN] = process.argv;')
rep('version: "0.9.8", statusShort: "Draf Desain Uji dan Hasil Awal",', 'version: "1.0", statusShort: "Final",')
i = s.index('  status: "Draf – desain kasus uji lengkap'); j = s.index('\n', i)
s = s[:i] + '  status: "Final v1.0 – diterbitkan Project Manager 22 November 2026; review PIC Dokumen 6 (Steven Wibisana) belum diterima sampai tenggat",' + s[j:]
i = s.index('  refs: "SRS-DDB-001'); j = s.index('\n', i)
s = s[:i] + '  refs: "SRS-DDB-001 v0.1; PRJ-DDB-001 v1.0 Bab III; SAD-DDB-001 v1.0; FSD-DDB-001 v1.0; SDD-DDB-001-BE v1.0; UIM-DDB-001-AND v1.0; TUT-DDB-001-AND v1.1; User Interface Map (Web) v0.3 (WB) dan v0.7 (WN)",' + s[j:]
rep('Skrip SA dan LP diperbaiki; pemeriksaan LP-04 diperketat."],\n  ],',
    'Skrip SA dan LP diperbaiki; pemeriksaan LP-04 diperketat."],\n'
    '    ["1.0", "22 Nov 2026", "Salomo P.G. Hokoyoku", "Versi final. Web resmi ditetapkan Web tim (Node/Express/EJS): kasus WN-01–WN-14 dan LPN-01–LPN-03 ditambahkan dan lulus; WB dan LP dinyatakan sebagai hasil aplikasi Web React. DEF-12 (nomor dan CVV kartu virtual varian online tidak tampil; SA-13 sebelumnya lulus keliru), DEF-13, dan DEF-14 dicatat, diperbaiki, dan diuji ulang; SA dijalankan ulang pada APK final. Disetujui PM."],\n  ],\n'
    '  approval: { sign: SIGN, date: "22 Nov 2026", pic: [["PIC Dokumen 6", "Steven Wibisana"]] },')

# SA-13
rep('ST.L, "Nomor lengkap dan CVV hanya setelah PIN; status Dibekukan"],',
    'ST.L, "Nomor lengkap dan CVV asli tampil setelah PIN (setelah perbaikan DEF-12); status Dibekukan"],')

# Log defect
rep('  ["DEF-06", "Web – Sesi", "Rendah",',
    '  ["DEF-12", "Android – Virtual Card", "Sedang", "Pada varian online, setelah PIN benar layar Kartu menampilkan nomor pengganti 0000 0000 0000 xxxx dan CVV kosong, karena data hasil verifikasi PIN tidak disimpan ke state (FR-22). Ada sejak 11 November; SA-13 sebelumnya lulus keliru karena hanya memeriksa pola 16 digit", "Pengambilan tangkapan layar D05A, 22 Nov 2026", "VirtualCardViewModel.submitPin menyimpan kartu hasil verifikasi; SA-13 kini menolak nomor berawalan 0000 dan CVV kosong", "Ditutup – retest lulus (SA-13)"],\n'
    '  ["DEF-13", "Android – PIN", "Sedang", "Saat ponsel miring, sebagian keypad dan tautan Lupa PIN pada layar PIN setelah login tidak tampil dan layar tidak dapat digulir", "Peninjauan kode dan uji tata letak, 22 Nov 2026", "Tata letak dua kolom saat miring dan layar dapat digulir", "Ditutup – retest lulus (rotasi di emulator)"],\n'
    '  ["DEF-14", "Android – Tampilan", "Rendah", "Kontras label status transaksi pada mode gelap hanya 2,6–3,8:1", "Perhitungan kontras warna, 22 Nov 2026", "Warna status khusus mode gelap (6,2–8,0:1)", "Ditutup – diperiksa melalui perhitungan dan gambar komponen"],\n'
    '  ["DEF-06", "Web – Sesi", "Rendah",')

# 5.3: ringkas paragraf eksekusi ulang
i = s.index('  P("Eksekusi ulang pada build dengan tampilan baru (22 November 2026)'); j = s.index('\n', s.index('  P("Kegagalan pada putaran awal', i))
s = s[:i] + r'''  P("Eksekusi ulang pada APK final (22 November 2026): varian online di emulator Pixel 8 (Android 17) terhadap backend lokal profil H2. Dalam satu putaran penuh 23 kasus dijalankan dengan skrip final dan seluruhnya lulus. SA-18 dan SA-20, yang hanya berjalan bila disebut eksplisit, dijalankan terpisah pada APK yang sama dan lulus (SA-20 sempat gagal dua kali karena cara menjalankan uji: backend tidak berhasil dimatikan skrip, lalu dialog izin notifikasi menutup layar; lulus setelah prosedur resmi dipakai). SA-04 tercakup oleh LP-01 dan LPN-01 (transfer sesama bank), dan SA-19 oleh LP-04 dan LPN-03 (logout semua perangkat). Dengan demikian 27 dari 28 kasus lulus pada build baru; SA-08 (kamera) serta berbagi bukti pada SA-04 terakhir lulus di ponsel fisik pada 14 November 2026 dengan build sebelumnya dan belum diulang. Bukti: pengujian/bukti/2026-11-22/android-sa/."),
  P("Perbaikan sebelum eksekusi: layar PIN saat ponsel miring (DEF-13; diuji dengan memutar emulator saat dua digit sudah diisi, digit tetap tersimpan dan login berhasil), warna label status mode gelap (DEF-14), layar Kartu (judul dan tombol kembali), dan detail kartu virtual (DEF-12). Saat DEF-12 ditemukan, hasil SA-13 pada 11 dan 22 November dinyatakan tidak sah, pemeriksaannya diperketat, dan kasus diulang sampai lulus."),
  P("Kegagalan lain pada putaran awal 22 November 2026 berasal dari skrip, bukan aplikasi: SA-21 dan SA-22 mencari tombol \"Daftar\" yang sudah bernama \"Buka rekening\" (SA-02 dan SA-15 ikut gagal karena memakai akun dari SA-21); SA-14, SA-16, SA-17, SA-27, dan SA-28 berjalan dengan sesi pengguna baru dari SA-13; SA-23 memakai sesi yang PIN-nya sengaja diblokir SA-02. Skrip diperbaiki tanpa melonggarkan pemeriksaan."),''' + s[j:]

# 5.4 Web: pembeda React dan Node
rep('  H2("5.4 Kasus Uji Web"),\n', r'''  H2("5.4 Kasus Uji Web"),
  P("Dua aplikasi Web diuji. Web React (prototipe yang dibuat Project Manager, kini di apps/web_dummy_buatan_sendiri) diuji dengan WB-01 sampai WB-15. Pada 22 November 2026 Web resmi ditetapkan Web tim (Node.js, Express 5, EJS; digitalbanking-node v2.0.0), yang diuji dengan WN-01 sampai WN-14 pada 5.4.1. Hasil WB tidak berlaku untuk Web tim."),
''')
rep('  H2("5.5 Skenario Lintas Platform"),', r'''  H3("5.4.1 Web Tim (Node/Express/EJS)"),
  P("WN-01 sampai WN-14 dijalankan pada 22 November 2026 dengan pengujian/web/node-smoke.mjs terhadap backend asli (tanpa patch saldo awal) dan data demo bersih; kode sumber Web tim tidak diubah. Skrip mengirim formulir HTTP seperti peramban, termasuk cookie sesi dan token CSRF, lalu mencocokkan saldo ke backend. Tampilan dan kompatibilitas peramban Web tim belum diuji. Bukti: pengujian/bukti/2026-11-22/web-node/."),
  table(["ID", "Kasus", "Hasil yang diharapkan", "Acuan", "Status"], WN, [800, 3000, 3170, 1300, 800], { center: [0], size: 16 }),
  CAP("Tabel 5.4a Kasus uji alur inti Web tim"),
  P("Menurut README Web tim, fitur berikut belum tersedia: kantong (FR-08, FR-09), kartu virtual (FR-21, FR-22), notifikasi (FR-29), grafik saldo (FR-07), penerima favorit (FR-13), QRIS pribadi dan dinamis (FR-16, FR-17), dan paket data (bagian dari FR-18). Fitur tersebut tersedia di Android dan di Web React. QRIS di Web tim berupa pemilihan contoh atau tempel payload, tanpa kamera."),
  H2("5.5 Skenario Lintas Platform"),''')
rep('// ---------------- Non-fungsional ----------------\n', r'''// ---------------- Web tim (Node) ----------------
const WN = [
  ["WN-01", "Login salah dan benar", "Login salah ditolak; login benar masuk Dashboard; saldo sama dengan backend", "FR-03, FR-06", ST.L],
  ["WN-02", "Halaman terlindungi tanpa login", "Diarahkan ke halaman masuk", "FR-05", ST.L],
  ["WN-03", "Transfer sesama bank", "PIN diminta; saldo kedua akun berubah tepat", "FR-10, FR-12", ST.L],
  ["WN-04", "Nominal kecil dan PIN salah", "Ditolak; saldo tidak berubah", "BR-01, BR-02", ST.L],
  ["WN-05", "Transfer antarbank ditolak", "Status Gagal; dana kembali", "FR-11, BR-05", ST.L],
  ["WN-06", "Transfer antarbank diproses", "Final lewat Cek status; debit tepat sekali", "FR-11, BR-04", ST.L],
  ["WN-07", "Pulsa", "Berhasil dengan PIN; debit Rp11.500", "FR-18", ST.L],
  ["WN-08", "Token PLN", "Berhasil dengan PIN", "FR-19", ST.L],
  ["WN-09", "E-wallet", "Berhasil dengan PIN", "FR-18", ST.L],
  ["WN-10", "Tagihan dua kali", "Pembayaran kedua pada periode sama ditolak", "FR-20", ST.L],
  ["WN-11", "QRIS contoh", "Merchant tampil; berhasil dengan PIN", "FR-15", ST.L],
  ["WN-12", "Riwayat dan ekspor", "Riwayat tampil; CSV dan PDF terunduh", "FR-23, FR-26", ST.L],
  ["WN-13", "Registrasi sampai Dashboard", "OTP, e-KYC, PIN, lalu Dashboard", "FR-01, FR-02, FR-04", ST.L],
  ["WN-14", "Logout semua perangkat", "Sesi Web berakhir dengan pesan", "FR-05", ST.L],
];
const LPN = [
  ["LPN-01", "Transfer Android tampil di Web tim", "Saldo dan riwayat andi di Web bertambah sesuai nominal", "FR-10, FR-23, NFR-08", ST.L],
  ["LPN-02", "E-wallet di Web tim tampil di Android", "Transaksi dengan total sama muncul di riwayat Android", "FR-18, FR-23", ST.L],
  ["LPN-03", "Logout semua perangkat dari Android", "Sesi Web terlihat di Android; Web kembali ke login dengan pesan sesi berakhir", "FR-05", ST.L],
];

// ---------------- Non-fungsional ----------------
''')
# 5.5
rep('  P("Eksekusi ulang pada build Android dengan tampilan baru (22 November 2026): LP-01 sampai LP-04 lulus',
    '  P("Eksekusi ulang dengan Web React pada build Android dengan tampilan baru (22 November 2026, sebelum perbaikan DEF-12): LP-01 sampai LP-04 lulus')
rep('  H2("5.6 Pengujian Non-Fungsional"),', r'''  P("Skenario lintas platform dengan Web tim (22 November 2026, pengujian/lintas-platform/lp-node.mjs): LPN-01 sampai LPN-03 adalah padanan LP-01, LP-03, dan LP-04. LP-02 tidak memiliki padanan karena Web tim belum menampilkan QRIS pribadi atau dinamis. Bukti: pengujian/bukti/2026-11-22/lintas-platform-node/."),
  table(["ID", "Skenario", "Hasil yang diharapkan", "Acuan", "Status"], LPN, [900, 2700, 3370, 1300, 800], { center: [0], size: 16 }),
  CAP("Tabel 5.5a Skenario lintas platform Android dan Web tim"),
  H2("5.6 Pengujian Non-Fungsional"),''')
# Tabel 6.1
rep('    ["Sistem Web", String(WB.length),', '    ["Sistem Web React", String(WB.length),')
rep('    ["Lintas platform", String(LP.length),', '    ["Alur inti Web tim (Node)", String(WN.length), String(count(WN, 4, ST.L)), "0", "0", String(count(WN, 4, ST.N))],\n    ["Lintas platform (Web React)", String(LP.length),')
rep('    ["Non-fungsional", String(NF.length),', '    ["Lintas platform (Web tim)", String(LPN.length), String(count(LPN, 4, ST.L)), "0", "0", String(count(LPN, 4, ST.N))],\n    ["Non-fungsional", String(NF.length),')
rep('Baris Lintas platform berasal dari eksekusi 22 November 2026.', 'Baris Lintas platform dan Web tim berasal dari eksekusi 22 November 2026; baris Sistem Web React berasal dari eksekusi 11–12 November 2026.')
# 8.x
rep('  B(`Web: ${count(WB, 5, ST.L)} dari ${WB.length} kasus lulus melalui uji UI otomatis; cakupan minimum Web tercapai.`),',
    '  B(`Web tim (Node, Web resmi): ${count(WN, 4, ST.L)} dari ${WN.length} alur inti lulus terhadap backend asli; tujuh kelompok fitur belum tersedia (5.4.1). Web React (prototipe): ${count(WB, 5, ST.L)} dari ${WB.length} kasus lulus melalui uji UI otomatis.`),')
rep('  B(`Lintas platform: ${count(LP, 5, ST.L)} dari ${LP.length} skenario lulus;', '  B(`Lintas platform: ${count(LPN, 4, ST.L)} dari ${LPN.length} skenario dengan Web tim dan ${count(LP, 5, ST.L)} dari ${LP.length} skenario dengan Web React lulus;')
rep('Terpenuhi: seluruh 11 defect sudah ditutup dengan retest', 'Terpenuhi: seluruh 14 defect sudah ditutup; tidak ada defect Kritis atau Tinggi terbuka')
rep('pemindaian QRIS dengan kamera (SA-08) lulus di ponsel fisik pada build 14 November 2026"],', 'pemindaian QRIS dengan kamera (SA-08) lulus di ponsel fisik pada build 14 November 2026. Pengecualian Web tim: paket data (bagian FR-18) belum ada"],')
rep('["Laporan disetujui Project Manager", "Belum"],', '["Laporan disetujui Project Manager", "Terpenuhi: 22 November 2026"],')
rep('"Review oleh PIC Dokumen 6 (Steven Wibisana), lalu persetujuan Project Manager.",', '"Uji tampilan dan kompatibilitas peramban untuk Web tim.",\n    "Review oleh PIC Dokumen 6 (Steven Wibisana) setelah penerbitan; temuan dicatat pada versi berikutnya.",')
rep('Project Document (PRJ-DDB-001) v0.9.2, Bab III', 'Project Document (PRJ-DDB-001) v1.0, Bab III')
rep('Architectural Design (SAD-DDB-001) v0.9.2; Functional Design (FSD-DDB-001) v0.9.1.', 'Architectural Design (SAD-DDB-001) v1.0; Functional Design (FSD-DDB-001) v1.0.')
rep('Panduan Uji Aplikasi di Ponsel (TUT-DDB-001-AND) v1.0.', 'Panduan Uji Aplikasi di Ponsel (TUT-DDB-001-AND) v1.1.')
open('doc6.js', 'w', encoding='utf-8', newline='').write(s.replace('\n', '\r\n') if crlf else s)
print('ok')
