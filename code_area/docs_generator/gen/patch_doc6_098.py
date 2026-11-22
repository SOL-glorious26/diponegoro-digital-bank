"""D06 v0.9.8: eksekusi ulang SA dan LP pada build Android dengan tampilan baru (22 Nov 2026).
Pemakaian: python patch_doc6_098.py <folder gen>  lalu  node doc6.js <fig> <out.docx> <logo>
"""
import sys, os, json
os.chdir(sys.argv[1])


def load(p):
    raw = open(p, encoding='utf-8', newline='').read()
    return raw.replace('\r\n', '\n'), '\r\n' in raw


def save(p, t, crlf):
    open(p, 'w', encoding='utf-8', newline='').write(t.replace('\n', '\r\n') if crlf else t)


s, crlf = load('doc6.js')


def rep(a, b):
    global s
    assert s.count(a) == 1, (s.count(a), a[:80])
    s = s.replace(a, b)


# Meta dan riwayat revisi
rep('code: "STD-DDB-001", version: "0.9.7",', 'code: "STD-DDB-001", version: "0.9.8",')
rep('date: "21 November 2026", logo: LOGO,', 'date: "22 November 2026", logo: LOGO,')
rep('eksekusi ulang SA dan LP pada build baru dicatat sebagai tindak lanjut."],\n',
    'eksekusi ulang SA dan LP pada build baru dicatat sebagai tindak lanjut."],\n'
    '    ["0.9.8", "22 Nov 2026", "Salomo P.G. Hokoyoku", "Eksekusi ulang pada build Android dengan tampilan baru di emulator: 25 kasus SA otomatis dan LP-01 sampai LP-04 lulus (SA-04 dan SA-19 tercakup LP-01 dan LP-04); SA-08 pada build baru menunggu ponsel fisik. Perbaikan tampilan sebelum eksekusi: layar PIN saat ponsel miring, warna label status mode gelap, dan layar Kartu. Skrip SA dan LP diperbaiki; pemeriksaan LP-04 diperketat."],\n')

# Tabel 5.3: istilah di langkah uji mengikuti layar yang sekarang
rep('"Virtual Card → buat → detail dengan PIN → bekukan"', '"Kartu → buat → detail dengan PIN → bekukan"')
rep('"Daftar → OTP → e-KYC dengan NIK valid → buat PIN"', '"Buka rekening → OTP → e-KYC dengan NIK valid → buat PIN"')
rep('"Daftar → OTP → e-KYC dengan NIK berakhiran 0000"', '"Buka rekening → OTP → e-KYC dengan NIK berakhiran 0000"')
rep('Notifikasi muncul ±6–10 detik setelah transfer masuk (emulator);',
    'Notifikasi muncul ±6–13 detik setelah transfer masuk (emulator);')

# Bab 5.3
rep('Seluruh kasus kecuali SA-08 dijalankan pada emulator Pixel 8 (Android 17, varian online) terhadap backend lokal, 11–12 November 2026,',
    'Eksekusi pertama: seluruh kasus kecuali SA-08 dijalankan pada emulator Pixel 8 (Android 17, varian online) terhadap backend lokal, 11–12 November 2026,')
i = s.index('  P("Status pada Tabel 5.3 berasal dari build sebelum pembaruan tampilan Android')
j = s.index('\n', i)
s = s[:i] + r'''  P("Eksekusi ulang pada build dengan tampilan baru (22 November 2026): varian online di emulator Pixel 8 (Android 17) terhadap backend lokal profil H2. Dengan skrip final, 23 kasus dijalankan dalam satu putaran dan seluruhnya lulus; SA-18 dan SA-20, yang hanya berjalan bila disebut eksplisit, dijalankan terpisah pada hari dan build yang sama dan lulus. SA-04 tercakup oleh LP-01 (transfer sesama bank) dan SA-19 oleh LP-04 (logout semua perangkat). Dengan demikian 27 dari 28 kasus lulus pada build baru; SA-08 (kamera) serta berbagi bukti pada SA-04 terakhir lulus di ponsel fisik pada 14 November 2026 dengan build sebelumnya dan belum diulang. Bukti: pengujian/bukti/2026-11-22/android-sa/."),
  P("Sebelum eksekusi ulang, tiga masalah tampilan dari peninjauan kode diperbaiki: (1) layar PIN setelah login tidak menampilkan seluruh keypad saat ponsel miring, kini disusun dua kolom dan dapat digulir; diuji dengan memutar emulator saat dua digit sudah diisi, digit tetap tersimpan dan login berhasil; (2) kontras label status transaksi pada mode gelap 2,6–3,8:1, kini 6,2–8,0:1 menurut perhitungan warna; (3) layar Kartu masih memakai judul dan tombol kembali lama. Ketiganya hanya mengubah lapisan tampilan (ditambah satu teks pesan) dan tidak ditambahkan ke log defect Bab VII."),
  P("Kegagalan pada putaran awal 22 November 2026 berasal dari skrip, bukan aplikasi: SA-21 dan SA-22 mencari tombol \"Daftar\" yang sudah bernama \"Buka rekening\" (SA-02 dan SA-15 ikut gagal karena memakai akun dari SA-21); SA-14, SA-16, SA-17, SA-27, dan SA-28 berjalan dengan sesi pengguna baru dari SA-13; pada putaran berikutnya SA-23 memakai sesi yang PIN-nya sengaja diblokir SA-02. Skrip diperbaiki (nama tombol dan login ulang sebagai akun demo) tanpa melonggarkan pemeriksaan, lalu seluruh kasus tersebut lulus."),''' + s[j:]

# Bab 5.5
rep('dijalankan dua kali berturut-turut dengan hasil sama."),',
    'dijalankan dua kali berturut-turut dengan hasil sama."),\n'
    r'  P("Eksekusi ulang pada build Android dengan tampilan baru (22 November 2026): LP-01 sampai LP-04 lulus dua putaran berturut-turut dengan skrip final, tanpa error JavaScript di Web. Pada putaran percobaan sebelumnya pesan \"Sesi Anda telah berakhir\" (DEF-06) tidak tertangkap pada satu jalan. Penelusuran menunjukkan Web sudah lebih dulu kembali ke halaman masuk dan menampilkan pesan melalui polling notifikasi (30 detik), lalu skrip memuat ulang halaman sehingga pesan hilang; DEF-06 tidak terulang. Pemeriksaan LP-04 yang sebelumnya selalu bernilai lulus kini mewajibkan sesi Web terlihat di Android dan pesan sesi berakhir tampil di Web. Bukti: pengujian/bukti/2026-11-22/lintas-platform/."),')

# Bab VI, 8.1, 8.2, 8.3
rep('setelah pembaruan tampilan Android. Eksekusi kasus uji sistem manual',
    'setelah perbaikan tampilan Android. Baris Sistem Android menghitung hasil terakhir setiap kasus: 27 kasus pada build 22 November 2026 dan SA-08 pada build 14 November 2026. Baris Lintas platform berasal dari eksekusi 22 November 2026. Eksekusi kasus uji sistem manual')
rep('kasus lulus (SA-08 di ponsel fisik Realme RMX3867, Android 16; kasus lain di emulator).`),',
    'kasus lulus; 27 di antaranya dijalankan ulang di emulator pada build dengan tampilan baru (22 November 2026), sedangkan SA-08 lulus di ponsel fisik Realme RMX3867 (Android 16) pada build sebelumnya (14 November 2026).`),')
rep('pemindaian QRIS dengan kamera (SA-08) lulus di ponsel fisik"],',
    'pemindaian QRIS dengan kamera (SA-08) lulus di ponsel fisik pada build 14 November 2026"],')
rep('"Jalankan ulang SA-01 sampai SA-28 dan LP-01 sampai LP-04 pada build Android dengan tampilan baru (emulator dan ponsel fisik), lalu perbarui status Tabel 5.3 dan 5.5.",',
    '"Ulangi SA-08 (kamera), berbagi bukti (SA-04), dan notifikasi push (SA-16) di ponsel fisik pada build Android dengan tampilan baru.",')

r, rcrlf = load('doc6_results.json')
r = json.loads(r)
r['androidDemo']['date'] = '22 November 2026 (setelah perbaikan tampilan)'
r['androidOnline']['date'] = '22 November 2026'
save('doc6.js', s, crlf)
save('doc6_results.json', json.dumps(r, ensure_ascii=False, indent=2), rcrlf)
print('ok')
