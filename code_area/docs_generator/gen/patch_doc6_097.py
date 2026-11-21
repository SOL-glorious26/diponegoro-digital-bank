"""D06 v0.9.7: regresi setelah pembaruan tampilan Android (21 Nov 2026).
Pemakaian: python patch_doc6_097.py <folder gen>  lalu  node doc6.js <fig> <out.docx> <logo>
"""
import sys, os, json
os.chdir(sys.argv[1])
p = 'doc6.js'
s = open(p, encoding='utf-8').read()


def rep(a, b):
    global s
    assert s.count(a) == 1, (s.count(a), a[:80])
    s = s.replace(a, b)


# Meta dan riwayat revisi
rep('code: "STD-DDB-001", version: "0.9.6",', 'code: "STD-DDB-001", version: "0.9.7",')
rep('date: "14 November 2026", logo: LOGO,', 'date: "21 November 2026", logo: LOGO,')
rep('berbagi bukti juga lulus di ponsel fisik; lingkungan ponsel fisik dan kriteria keluar diperbarui."],\n  ],',
    'berbagi bukti juga lulus di ponsel fisik; lingkungan ponsel fisik dan kriteria keluar diperbarui."],\n'
    '    ["0.9.7", "21 Nov 2026", "Salomo P.G. Hokoyoku", "Regresi setelah pembaruan tampilan Android (tema baru, layar Login, PIN dengan keypad, Beranda, bottom navigation, komponen bersama; ViewModel, repository, API, dan backend tidak berubah): test otomatis Android varian demo 63/64 dan varian online 57/58 lulus (masing-masing 1 dilewati); DarkModeShotsTest ditambahkan; skrip uji sistem dan lintas platform disesuaikan; eksekusi ulang SA dan LP pada build baru dicatat sebagai tindak lanjut."],\n  ],')

# Tabel 5.2: test Robolectric bertambah satu (mode gelap)
rep('["AppTourTest + ReceiptAndDialogShotsTest (Robolectric, varian demo)", "5", "Tur seluruh layar, dialog PIN, bukti transfer berhasil/ditolak, struk PLN", "FR-14, NFR-08"],',
    '["AppTourTest + DarkModeShotsTest + ReceiptAndDialogShotsTest (Robolectric, varian demo)", "6", "Tur seluruh layar, mode gelap (login, PIN, Beranda), dialog PIN, bukti transfer berhasil/ditolak, struk PLN", "FR-14, NFR-08"],')

# Bab 5.2: varian online kali ini tanpa backend live
rep('Varian online (${ao.date}): ${ao.passed} dari ${ao.total} lulus, termasuk LiveBackendTest terhadap backend sungguhan.`),',
    'Varian online (${ao.date}): ${ao.passed} dari ${ao.total} lulus, ${ao.skipped} dilewati (LiveBackendTest hanya berjalan bila backend aktif dan parameter -Pddb.liveUrl diberikan; eksekusi terakhirnya terhadap backend sungguhan lulus pada 11 November 2026).`),\n'
    '  P("Pembaruan tampilan Android (21 November 2026) mengganti tema, font, ikon aplikasi, layar Login, layar PIN (keypad angka; PIN dikirim otomatis pada digit keenam), Beranda, bottom navigation (QRIS di tengah, tab Kantong), serta komponen bersama (panel, kolom input, tombol). ViewModel, repository, integrasi API, dan backend tidak diubah. Seluruh test di atas dijalankan ulang setelah pembaruan tersebut; AppTourTest disesuaikan dengan label dan alur PIN yang baru, dan DarkModeShotsTest ditambahkan untuk memeriksa mode gelap."),')

# Bab 5.3: hasil SA berasal dari build sebelum pembaruan tampilan
anchor = '  P("Keterbatasan: sebagian besar kasus dijalankan di emulator;'
assert s.count(anchor) == 1
s = s.replace(anchor,
    '  P("Status pada Tabel 5.3 berasal dari build sebelum pembaruan tampilan Android (21 November 2026). Skrip pengujian/android/sa-android.mjs, pengujian/lib/android-flows.mjs, dan pengujian/lintas-platform/lp-cross.mjs sudah disesuaikan dengan layar baru (tombol Masuk, label kolom username, PIN melalui keypad, menu Kartu) dan lolos pemeriksaan sintaks; eksekusi ulang SA dan LP pada build baru belum dilakukan saat revisi ini dan dicatat pada Bab 8.3."),\n'
    + anchor, 1)

# Tabel 6.1: varian online kini punya test yang dilewati
rep('["Android otomatis (online)", String(ao.total), String(ao.passed), String(ao.failed), "0", "0"],',
    '["Android otomatis (online)", String(ao.total), String(ao.passed), String(ao.failed), "0", ao.skipped ? String(ao.skipped) + " (dilewati)" : "0"],')
rep('Test otomatis backend dan Android varian demo dijalankan ulang pada ${be.date} untuk dokumen ini.',
    'Test otomatis backend dijalankan ulang pada ${be.date}.')
rep('Hasil varian online berasal dari eksekusi ${ao.date} setelah integrasi Android–backend.',
    'Test otomatis Android varian demo dan online dijalankan ulang pada ${ao.date} setelah pembaruan tampilan Android.')

# Bab 8.1 dan 8.3
rep('dan ${ao.passed}/${ao.total} test varian online lulus. Uji sistem Android:',
    'dan ${ao.passed}/${ao.total} test varian online lulus (${ao.skipped} dilewati), dijalankan ulang setelah pembaruan tampilan. Uji sistem Android:')
rep('    "Jalankan NF-04 (rate limit gateway) setelah Docker terpasang.",\n',
    '    "Jalankan ulang SA-01 sampai SA-28 dan LP-01 sampai LP-04 pada build Android dengan tampilan baru (emulator dan ponsel fisik), lalu perbarui status Tabel 5.3 dan 5.5.",\n'
    '    "Jalankan NF-04 (rate limit gateway) setelah Docker terpasang.",\n')

open(p, 'w', encoding='utf-8').write(s)

# Hasil test otomatis Android 21 Nov 2026 (Gradle, laporan app/build/test-results)
r = json.load(open('doc6_results.json', encoding='utf-8'))
r['androidDemo'] = {"date": "21 November 2026 (regresi setelah pembaruan tampilan)", "total": 64, "passed": 63, "failed": 0, "skipped": 1}
r['androidOnline'] = {"date": "21 November 2026", "total": 58, "passed": 57, "failed": 0, "skipped": 1}
json.dump(r, open('doc6_results.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
print('ok')
