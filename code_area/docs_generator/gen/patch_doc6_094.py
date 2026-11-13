import re, sys, os
os.chdir(sys.argv[1])
p = 'doc6.js'
s = open(p, encoding='utf-8').read()

def rep(a, b):
    global s
    assert s.count(a) == 1, (s.count(a), a[:80])
    s = s.replace(a, b)

# WB-13..15 setelah WB-12
m = re.search(r'  \["WB-12", [^\n]*\n', s)
assert m
s = s.replace(m.group(0), m.group(0) +
  '  ["WB-13", "Grafik saldo (Beranda)", "Pilih periode Harian, Mingguan, Bulanan; tampilkan sebagai tabel", "Grafik tampil per periode; titik terakhir sama dengan saldo rekening", "FR-07", ST.L, "Chrome headless; 14/8/6 titik; titik terakhir = saldo"],\n'
  '  ["WB-14", "Foto profil", "Masukkan URL tidak valid, lalu URL gambar valid; muat ulang; hapus foto", "URL tidak valid ditolak; foto tampil di profil dan bilah atas; dapat dihapus", "FR-27", ST.L, "Pesan \\"URL foto tidak valid\\"; foto tetap setelah muat ulang"],\n'
  '  ["WB-15", "Ubah dan kunci kantong", "Buat kantong → Ubah nama/tujuan → Kunci sampai tanggal", "Perubahan tersimpan; status Terkunci dengan tanggal; tombol Kunci hilang", "FR-08, FR-09", ST.L, "Kantong \\"Laptop Kuliah\\" terkunci 7 hari"],\n')

# DEF-11 ditutup
rep('"Riwayat kini menampilkan pesan tidak dapat terhubung ke server; layar Sesi belum diubah", "Terbuka sebagian – Riwayat ditutup (retest SA-20); layar Sesi masih terbuka"]',
    '"Riwayat dan layar Sesi kini menampilkan pesan tidak dapat terhubung/dimuat", "Ditutup – retest lulus (SA-20 dan uji layar Sesi dengan backend dimatikan)"]')
rep('    "Perbaiki sisa DEF-11 (layar Sesi saat server tidak terjangkau) dan DEF-04 (daftar origin CORS) sebelum deployment.",',
    '    "Perbaiki DEF-04 (daftar origin CORS) sebelum deployment.",')
rep('"Kasus uji Web disusun dari User Interface Map (Web) v0.3', '"Kasus uji Web disusun dari User Interface Map (Web) v0.3 dan SRS (WB-13 sampai WB-15 ditambahkan setelah pemeriksaan ulang cakupan FR-07, FR-08/FR-09, dan FR-27 di Web)')
# kalimat asli mungkin berlanjut " dan ruang lingkup Web pada SAD..." — rapikan "dan SRS (...) dan ruang lingkup"
s = s.replace('FR-27 di Web) dan ruang lingkup Web pada SAD-DDB-001 Bab VIII.', 'FR-27 di Web), serta ruang lingkup Web pada SAD-DDB-001 Bab VIII.')

rep('code: "STD-DDB-001", version: "0.9.3",', 'code: "STD-DDB-001", version: "0.9.4",')
rep('skrip uji dipindah ke folder pengujian/."],\n  ],',
    'skrip uji dipindah ke folder pengujian/."],\n'
    '    ["0.9.4", "12 Nov 2026", "Salomo P.G. Hokoyoku", "Kasus uji Web WB-13 (grafik saldo, FR-07), WB-14 (foto profil, FR-27), WB-15 (ubah dan kunci kantong, FR-08/09) ditambahkan dan lulus; DEF-11 ditutup penuh (layar Sesi Android); regresi Web, peramban, dan lintas platform lulus."],\n  ],')
open(p, 'w', encoding='utf-8').write(s)
print('ok')
