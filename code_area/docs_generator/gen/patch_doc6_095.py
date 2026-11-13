import sys, os
os.chdir(sys.argv[1])
p = 'doc6.js'
s = open(p, encoding='utf-8').read()

def rep(a, b):
    global s
    assert s.count(a) == 1, (s.count(a), a[:80])
    s = s.replace(a, b)

rep('''  ["TransferFlowTest", "belumEkycTidakBisaTransfer", "Pengguna belum e-KYC tidak dapat transfer", "FR-02"],
];''', '''  ["TransferFlowTest", "belumEkycTidakBisaTransfer", "Pengguna belum e-KYC tidak dapat transfer", "FR-02"],
  ["CorsTest", "preflightDariOriginWebDiizinkanDanOriginLainDitolak", "CORS: preflight dari origin Web diizinkan; origin lain ditolak", "NFR-06, DEF-04"],
  ["CorsTest", "responsBiasaMemuatHeaderCors…", "CORS: header pada respons; klien tanpa Origin (Android) tidak terpengaruh", "NFR-06, DEF-04"],
];''')
rep('"Web memakai proxy Vite (SAD Bab VIII), terverifikasi pada uji Web 11 Nov 2026; alternatif: CorsConfigurationSource untuk localhost", "Terbuka – solusi sementara terverifikasi"]',
    '"CORS dikonfigurasi di backend (ddb.cors.allowed-origins, default localhost) dan header CORS backend disembunyikan di gateway", "Ditutup – retest lulus (BE-46, BE-47; peramban memanggil backend langsung tanpa proxy)"]')
rep('    "Perbaiki DEF-04 (daftar origin CORS) sebelum deployment.",\n', '')
rep('["Tidak ada defect Kritis/Tinggi terbuka", "Terpenuhi: DEF-01, DEF-02, DEF-08, DEF-09 sudah ditutup dengan retest"],',
    '["Tidak ada defect Kritis/Tinggi terbuka", "Terpenuhi: seluruh 11 defect sudah ditutup dengan retest"],')
anchor = '  P("Keterbatasan: pengujian memakai emulator, bukan ponsel fisik;'
assert anchor in s
s = s.replace(anchor, '  P("APK rilis bertanda tangan (build-rilis.ps1, 12 November 2026) diuji ulang di emulator dengan SA-01, SA-07, SA-09, SA-10, SA-13, dan skenario lintas platform LP-01 sampai LP-04; seluruhnya lulus."),\n' + anchor, 1)
rep('code: "STD-DDB-001", version: "0.9.4",', 'code: "STD-DDB-001", version: "0.9.5",')
rep('regresi Web, peramban, dan lintas platform lulus."],\n  ],',
    'regresi Web, peramban, dan lintas platform lulus."],\n'
    '    ["0.9.5", "12 Nov 2026", "Salomo P.G. Hokoyoku", "DEF-04 ditutup: CORS dikonfigurasi di backend dan gateway; BE-46 dan BE-47 (47 test backend lulus); APK rilis bertanda tangan diuji ulang (SA dan LP lulus); seluruh defect ditutup."],\n  ],')
open(p, 'w', encoding='utf-8').write(s)

p = 'doc6_results.json'
s = open(p, encoding='utf-8').read()
s = s.replace('"backend": { "date": "11 November 2026 pukul 21.03 WIB", "total": 45, "passed": 45,', '"backend": { "date": "12 November 2026 (setelah perbaikan DEF-04)", "total": 47, "passed": 47,')
assert '"total": 47' in s
open(p, 'w', encoding='utf-8').write(s)
print('ok')
