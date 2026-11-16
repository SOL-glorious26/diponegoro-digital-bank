"""D02 SAD-DDB-001 v0.9.4 -> v0.9.5: ubah doc2.js agar status sama dengan STD-DDB-001 v0.9.6 (16 Nov 2026).
Pemakaian: python patch_doc2_095.py <folder gen>, lalu bangkitkan ulang:
node doc2.js ../fig <OUT.docx> ../../submit_mobile/diponegoro-corp-logo.jpeg
"""
import sys, os
os.chdir(sys.argv[1])
p = 'doc2.js'
s = open(p, encoding='utf-8').read()


def rep(a, b):
    global s
    assert s.count(a) == 1, (s.count(a), a[:80])
    s = s.replace(a, b)


def line(prefix, new):
    """Ganti satu baris utuh yang (setelah spasi awal) diawali prefix."""
    global s
    lines = s.split('\n')
    hit = [i for i, l in enumerate(lines) if l.lstrip().startswith(prefix)]
    assert len(hit) == 1, (len(hit), prefix)
    i = hit[0]
    indent = lines[i][:len(lines[i]) - len(lines[i].lstrip())]
    lines[i] = indent + new
    s = '\n'.join(lines)


# Meta dan riwayat revisi
rep('code: "SAD-DDB-001", version: "0.9.4",', 'code: "SAD-DDB-001", version: "0.9.5",')
rep('repositori kode per 11 November 2026",\n  date: "12 November 2026",',
    'STD-DDB-001 v0.9.6; repositori kode per 16 November 2026",\n  date: "16 November 2026",')
rep('Tabel 8.3 diperbarui."],\n  ],',
    'Tabel 8.3 diperbarui."],\n'
    '    ["0.9.5", "16 Nov 2026", "Salomo P.G. Hokoyoku", "Status disamakan dengan STD-DDB-001 v0.9.6: Web 15/15, 47 test backend, uji lintas platform 4/4, uji ponsel fisik lulus, NFR-01 sampai NFR-03 terukur, Firefox/Edge lulus; CORS backend (DEF-04) ditambahkan pada 9.2; PostgreSQL lokal berjalan; jumlah berkas Kotlin Android dikoreksi; isu terbuka yang sudah selesai ditutup (Tabel 11.1)."],\n  ],')

# Bab I
rep('**sesuai implementasi aktual per 11 November 2026**', '**sesuai implementasi aktual per 16 November 2026**')

# Tabel 3.1 komponen utama
rep('"Terhubung ke backend; 9 dari 12 kasus uji Web lulus"]', '"Terhubung ke backend; 15 dari 15 kasus uji Web lulus"]')
rep('"Konfigurasi siap; belum diuji"]', '"Konfigurasi siap; belum diuji (Docker belum terpasang)"]')
rep('"Implementasi selesai; 45 test lulus"]', '"Implementasi selesai; 47 test lulus; cakupan baris 92,2%"]')

# Bab VII
rep('(paket com.diponegoro.digitalbank, 90 berkas Kotlin)', '(paket com.diponegoro.digitalbank, 110 berkas Kotlin pada source set main)')

# Tabel 9.1: CORS sekarang juga di backend
rep('["CORS", "Origin localhost/127.0.0.1 diizinkan; header Authorization, Content-Type, Idempotency-Key"],',
    '["CORS", "Origin localhost/127.0.0.1 diizinkan; header Authorization, Content-Type, Idempotency-Key. Backend juga memiliki daftar origin sendiri (ddb.cors.allowed-origins, bawaan localhost) agar peramban dapat memanggil port 8080 langsung; header CORS dari backend disembunyikan di gateway agar tidak ganda (DEF-04)"],')

# Tabel 9.2 lingkungan
rep('["Test", "./mvnw test (profil test, H2 in-memory)", "Berjalan; 45 test lulus"],',
    '["Test", "./mvnw verify (profil test, H2 in-memory, laporan JaCoCo)", "Berjalan; 47 test lulus; cakupan baris 92,2%"],')
rep('"Menunggu konfigurasi user/database ddb"]', '"Berjalan sejak 13 Nov 2026 (PostgreSQL 18, role dan database ddb)"]')

# Tabel 10.2 NFR
rep('["NFR-01 Waktu muat ≤ 2 dtk", "Compose/SPA ringan, data dashboard ringkas", "Belum diukur"],',
    '["NFR-01 Waktu muat ≤ 2 dtk", "Compose/SPA ringan, data dashboard ringkas", "Terukur, lulus (NF-01): Web maks. 956 ms; Android buka dingin 1,33–1,37 dtk"],')
rep('["NFR-02 API baca p95 ≤ 500 ms", "Modular monolith tanpa panggilan antar-layanan, paginasi, indeks", "Belum diukur"],',
    '["NFR-02 API baca p95 ≤ 500 ms", "Modular monolith tanpa panggilan antar-layanan, paginasi, indeks", "Terukur, lulus (NF-02): p95 13 ms pada backend lokal"],')
rep('["NFR-03 Transfer ≤ 3 dtk", "Posting ledger dalam satu transaksi DB; notifikasi asinkron", "Belum diukur"],',
    '["NFR-03 Transfer ≤ 3 dtk", "Posting ledger dalam satu transaksi DB; notifikasi asinkron", "Terukur, lulus (NF-03): maks. 94 ms pada backend lokal"],')
rep('"Ledger atomik, idempotensi, 63/58 test Android + 45 test backend"', '"Ledger atomik, idempotensi, 63/58 test Android + 47 test backend"')
rep('"Sebagian; TLS belum dipasang"]', '"Sebagian; rate limit gateway (NF-04) dan TLS belum diuji/dipasang"]')
rep('"Sebagian; Web terintegrasi, uji lintas platform belum"]', '"Terverifikasi di lingkungan lokal: uji lintas platform LP-01 sampai LP-04 lulus"]')
rep('"Android diterapkan; Web diuji di Chrome (desktop dan lebar ponsel); Firefox/Edge belum; iOS di luar cakupan"]',
    '"Lulus: emulator Pixel 8 (API 37) dan ponsel fisik Realme RMX3867 (Android 16); Web di Chrome, Edge, Firefox; iOS di luar cakupan"]')
rep('"Sebagian; laporan cakupan dan CI/CD belum"]', '"Sebagian; laporan cakupan JaCoCo ada (92,2% baris); CI/CD belum"]')

# Tabel 11.1 isu terbuka: isu 1, 2, 6 selesai
line('["1", "Uji lintas platform Android–Web belum dijalankan"',
     '["1", "Gateway dan Docker Compose belum pernah dijalankan", "Topologi Bab IX dan rate limit login (NF-04) belum terverifikasi", "Pasang Docker, jalankan compose, uji /health dan rate limit", "Salomo"],')
line('["2", "Varian online Android belum diuji di ponsel fisik',
     '["2", "TLS belum dikonfigurasi", "NFR-06 belum terpenuhi untuk deployment publik", "TLS di gateway atau load balancer cloud", "Salomo"],')
line('["3", "Gateway dan Docker Compose belum pernah dijalankan"',
     '["3", "QRIS, biller, kartu belum berupa adapter", "Penggantian ke layanan nyata memerlukan perubahan service", "Ekstrak interface adapter seperti InterbankGateway", "Salomo"],')
line('["4", "TLS belum dikonfigurasi"',
     '["4", "Nilai usulan (limit transfer Rp25 juta, biaya antarbank Rp2.500, limit kartu, timeout 5 menit) belum dikonfirmasi", "Aturan bisnis dapat berubah", "Konfirmasi tim/dosen; nilai sudah terpusat di konfigurasi kode", "Salomo"],')
line('["5", "QRIS, biller, kartu belum berupa adapter"',
     '["5", "Login biometrik Android masih disimulasikan", "FR-03 belum memakai sensor biometrik perangkat nyata", "Integrasikan BiometricPrompt pada lapisan presentation Android", "Salomo / Gracia"],')
line('["6", "Uji pada PostgreSQL lokal belum dilakukan"', '')
line('["7", "Nilai usulan', '')
s = s.replace('\n\n  ], [450, 2700, 2100, 2620, 1200]', '\n  ], [450, 2700, 2100, 2620, 1200]')
rep('  CAP("Tabel 11.1 Isu terbuka arsitektur"),\n',
    '  CAP("Tabel 11.1 Isu terbuka arsitektur"),\n'
    '  P("Isu yang ditutup sejak versi 0.9.4: uji lintas platform Android–Web (LP-01 sampai LP-04 lulus), uji varian online di ponsel fisik (Realme RMX3867, 14 November 2026), dan uji pada PostgreSQL lokal (profil dev berjalan sejak 13 November 2026). Rinciannya tercatat pada STD-DDB-001 v0.9.6."),\n')

open(p, 'w', encoding='utf-8').write(s)
print('ok')
