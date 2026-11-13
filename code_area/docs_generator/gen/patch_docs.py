import sys
G = sys.argv[1]

def rep(name, pairs):
    p = G + '/' + name
    s = open(p, encoding='utf8').read()
    for a, b in pairs:
        if a not in s:
            raise SystemExit(f'{name}: tidak ditemukan -> {a[:90]!r}')
        s = s.replace(a, b)
    open(p, 'w', encoding='utf8', newline='\n').write(s)

REV = 'Pembaruan setelah integrasi Android–backend (Fase 5) 11 Nov sore: '

# ------------------------------------------------------------------ Dokumen 1
rep('doc1.js', [
    ('const sizes = { be: 4.689, sql: 0.218, and: 7.686, web: 0.073, webAdd: 4.0, intAdd: 1.0 };',
     'const sizes = { be: 4.699, sql: 0.218, and: 9.197, web: 0.073, webAdd: 4.0, intAdd: 0 };'),
    ('code: "PRJ-DDB-001", version: "0.9",', 'code: "PRJ-DDB-001", version: "0.9.1",'),
    ('''risiko R1–R15, estimasi biaya COCOMO II."],''',
     '''risiko R1–R15, estimasi biaya COCOMO II."],
    ["0.9.1", "11 Nov 2026", "Salomo P.G. Hokoyoku", "''' + REV + '''status Android online, Gantt (integrasi selesai), jumlah test (63/58 Android, 45 backend), 73 operasi API, risiko R12 termitigasi, ukuran kode COCOMO diukur ulang."],'''),
    ('["Aplikasi Android", "Kotlin 2.2, Jetpack Compose, MVVM, Repository Pattern; minSdk 24, targetSdk 37", "Delapan modul terintegrasi dalam satu aplikasi; data masih simulasi in-memory"]',
     '["Aplikasi Android", "Kotlin 2.2, Jetpack Compose, MVVM, Repository Pattern; Retrofit/OkHttp; minSdk 24, targetSdk 37", "8 modul; varian demo (data simulasi) dan online (terhubung backend lewat REST)"]'),
    ('"Delapan modul selesai; 72 operasi API"', '"Delapan modul selesai; 73 operasi API"'),
    ('''    ["Android", "8 modul (FR-01–FR-25, FR-27–FR-29) terintegrasi; build APK debug berhasil; uji emulator Pixel 8 untuk 5 alur utama lulus, 3 bug ditemukan dan diperbaiki", "54 test lulus (unit + UI Robolectric); NOTES.md 10 Nov 2026", "Integrasi REST ke backend; uji di ponsel fisik (kamera QRIS, push, berbagi berkas)"],''',
     '''    ["Android", "8 modul (FR-01–FR-25, FR-27–FR-29); varian demo dan online; integrasi REST ke backend selesai 11 Nov; di emulator: login, PIN, Beranda dari server, transfer ke nasabah lain terverifikasi di akun penerima", "63 test demo dan 58 test online lulus, termasuk uji end-to-end ke backend (11 Nov 2026)", "Uji di ponsel fisik (kamera QRIS, push, berbagi berkas); uji dua perangkat"],'''),
    ('"8 modul + Financial Transaction Core; 72 operasi API; data demo; bug lost update saldo pada transfer paralel ditemukan dan diperbaiki", "44 test integrasi lulus (laporan surefire 11 Nov 2026); openapi.json", "Laporan cakupan JaCoCo; uji pada PostgreSQL lokal; perbaikan API.md"]',
     '"8 modul + Financial Transaction Core; 73 operasi API (termasuk verifikasi PIN); data demo; bug lost update saldo pada transfer paralel ditemukan dan diperbaiki", "45 test integrasi lulus (11 Nov 2026); openapi.json dan API.md dibangkitkan ulang", "Laporan cakupan JaCoCo; uji pada PostgreSQL lokal"]'),
    ('["9", "Integrasi Android – Backend", "12 Nov", "16 Nov", "Rencana", "DAFTAR_PEKERJAAN Fase 5"]',
     '["9", "Integrasi Android – Backend", "11 Nov", "11 Nov", "Selesai", "Flavor online; 58 test online lulus termasuk uji live (11 Nov 2026)"]'),
    ('["Test otomatis backend", "Profil test, H2 in-memory mode PostgreSQL", "44 test integrasi API (±40 detik)"]',
     '["Test otomatis backend", "Profil test, H2 in-memory mode PostgreSQL", "45 test integrasi API (±40 detik)"]'),
    ('Sampai 11 November 2026 telah tersedia bukti pengujian berikut: **54 test Android lulus**, **44 test integrasi backend lulus** (termasuk uji transfer paralel yang menemukan dan memperbaiki bug lost update saldo), serta uji manual lima alur utama pada emulator. Pengujian sistem lintas platform belum dimulai karena integrasi Android–backend dan aplikasi Web belum selesai.',
     'Sampai 11 November 2026 telah tersedia bukti pengujian berikut: **63 test Android varian demo** dan **58 test varian online lulus** (termasuk uji end-to-end ke backend sungguhan: registrasi sampai transfer, QRIS, top-up, kartu, notifikasi), **45 test integrasi backend lulus** (termasuk uji transfer paralel yang menemukan dan memperbaiki bug lost update saldo), serta uji manual pada emulator untuk varian demo dan online. Pengujian sistem lintas platform Android–Web belum dapat dimulai karena aplikasi Web belum terintegrasi.'),
    ('["R12", "Integrasi Android–backend belum dimulai (Android masih memakai data simulasi in-memory)", "2", "3", "6", "Fase 5: Retrofit, flavor demo/online; flavor demo tetap tersedia sebagai cadangan", "Salomo", "Terbuka"]',
     '["R12", "Integrasi Android–backend (semula belum dimulai)", "1", "3", "3", "Fase 5 selesai 11 Nov: flavor online terhubung REST dan diuji end-to-end; flavor demo tetap cadangan demo", "Salomo", "Termitigasi"]'),
    ('Risiko dengan skor 9 (R10, R11) dan skor 6 (R1, R6, R8, R12) menjadi prioritas.',
     'Risiko dengan skor 9 (R10, R11) dan skor 6 (R1, R6, R8) menjadi prioritas.'),
    (' Untuk R12, integrasi Android–backend dijadwalkan 12–16 November 2026, dan versi demo dengan data simulasi tetap dipertahankan sebagai cadangan demo.',
     ' R12 telah termitigasi karena integrasi Android–backend selesai pada 11 November 2026. Temuan baru dari integrasi: Android 17 memblokir akses aplikasi ke backend di jaringan lokal tanpa izin ACCESS_LOCAL_NETWORK; izin tersebut kini diminta saat aplikasi varian online dibuka.'),
    ('    ["Backend (Java, main)", "72", "4,689", "Diukur"],', '    ["Backend (Java, main)", "72", "4,699", "Diukur"],'),
    ('    ["Aplikasi Android (Kotlin, main)", "90", "7,686", "Diukur"],', '    ["Aplikasi Android (Kotlin, main, termasuk lapisan REST Fase 5)", "110", "9,197", "Diukur"],'),
    ('    ["Integrasi REST pada Android (Fase 5)", "–", "1,000", "Asumsi: klien API, interceptor JWT, repository REST per modul"],\n', ''),
    ('kode uji (1,697 KSLOC) tidak dihitung', 'kode uji (1,934 KSLOC) tidak dihitung'),
    ('["Penyelesaian (termasuk Web dan integrasi)",', '["Penyelesaian (termasuk aplikasi Web)",'),
    ('"Ukuran penyelesaian Web (4,000 KSLOC) dan integrasi Android (1,000 KSLOC) adalah perkiraan dan akan diganti dengan ukuran aktual setelah implementasi."',
     '"Ukuran penyelesaian Web (4,000 KSLOC) adalah perkiraan dan akan diganti dengan ukuran aktual setelah implementasi; ukuran integrasi Android sudah diukur (termasuk dalam 9,197 KSLOC)."'),
])

# ------------------------------------------------------------------ Dokumen 2
rep('doc2.js', [
    ('code: "SAD-DDB-001", version: "0.9",', 'code: "SAD-DDB-001", version: "0.9.1",'),
    ('''bagian Web ditandai menunggu konfirmasi tim Web."],''',
     '''bagian Web ditandai menunggu konfirmasi tim Web."],
    ["0.9.1", "11 Nov 2026", "Salomo P.G. Hokoyoku", "''' + REV + '''arsitektur Android online (Bab VII), endpoint verifikasi PIN (73 operasi), 45 test backend, izin jaringan lokal Android 17, isu terbuka diperbarui."],'''),
    ('["Aplikasi Android", "Antarmuka nasabah di ponsel, termasuk pindai QRIS dan biometrik", "Kotlin, Jetpack Compose, MVVM", "Implementasi 8 modul; data in-memory"]',
     '["Aplikasi Android", "Antarmuka nasabah di ponsel, termasuk pindai QRIS dan biometrik", "Kotlin, Jetpack Compose, MVVM, Retrofit", "8 modul; varian demo (in-memory) dan online (REST)"]'),
    ('"Implementasi selesai; 44 test lulus"', '"Implementasi selesai; 45 test lulus"'),
    ('Backend menyediakan **72 operasi REST**', 'Backend menyediakan **73 operasi REST**'),
    ('["/api/v1/auth/**", "17", "Registrasi, OTP, e-KYC, login, biometrik, refresh, PIN, sesi, pemulihan"]',
     '["/api/v1/auth/**", "18", "Registrasi, OTP, e-KYC, login, biometrik, refresh, PIN (buat dan verifikasi), sesi, pemulihan"]'),
    ('Kelompok endpoint API (total 72 operasi, sumber: openapi.json)', 'Kelompok endpoint API (total 73 operasi, sumber: openapi.json)'),
    ('''  H2("7.2 Status dan Arsitektur Target"),
  P("Saat ini repository Android memakai **implementasi in-memory**. Seluruh fungsi berjalan tanpa backend, sehingga aplikasi dapat diuji dan didemokan secara mandiri, tetapi data hilang saat aplikasi ditutup. Arsitektur target (Fase 5) mempertahankan lapisan presentation dan domain tanpa perubahan, lalu menambahkan:"),
  B("klien REST (Retrofit + OkHttp + kotlinx-serialization) dengan interceptor JWT dan refresh otomatis;"),
  B("product flavor **demo** (repository in-memory seperti sekarang) dan **online** (repository REST);"),
  B("penyimpanan token aman (EncryptedSharedPreferences/DataStore) serta penanganan offline dan error jaringan;"),
  B("pembuatan Idempotency-Key per layar konfirmasi transaksi."),
  P("Karena logika ledger kini berada di backend, ledger in-memory Android hanya dipakai flavor demo. Pada flavor online, saldo dan riwayat selalu bersumber dari backend agar konsisten dengan Web (NFR-08)."),''',
     '''    H2("7.2 Varian Demo dan Online"),
  P("Sejak 11 November 2026 (Fase 5) aplikasi memiliki dua **product flavor**. Lapisan presentation dan domain dipakai bersama; yang berbeda hanya implementasi repository:"),
  table(["Aspek", "Varian demo", "Varian online"], [
    ["Repository", "Local*Repository: ledger in-memory, simulator antarbank/biller/kartu", "Remote*Repository: REST /api/v1 lewat Retrofit"],
    ["Sinyal pembaruan data", "Revisi ledger lokal", "CoreModule.revision: dinaikkan setiap mutasi berhasil dan saat ada notifikasi baru"],
    ["Notifikasi", "NotificationCenter lokal", "Kotak notifikasi server + polling antrean push tiap 15 detik"],
    ["Penyimpanan token", "Tidak diperlukan", "SecureTokenStore: AES-GCM dengan kunci Android Keystore"],
    ["Sesi", "Lokal", "Access token disisipkan otomatis; 401 → refresh sekali → bila gagal kembali ke login"],
    ["Kegunaan", "Uji UI, cadangan demo tanpa server", "Pemakaian sebenarnya; data sama dengan Web"],
  ], [2300, 3300, 3470], { boldFirst: true, size: 18 }),
  CAP("Tabel 7.2 Perbandingan varian demo dan online"),
  B("ViewModel dibuat asinkron (memanggil repository di coroutine) dan tidak lagi membaca ledger lokal secara langsung."),
  B("Setiap transaksi keuangan mengirim Idempotency-Key yang dibuat sekali per layar konfirmasi."),
  B("Pesan kesalahan server (field message) diteruskan ke layar; kegagalan jaringan menampilkan pesan offline."),
  B("**Android 17 (API 37):** akses aplikasi ke alamat jaringan lokal (laptop/LAN, emulator 10.0.2.2) memerlukan izin ACCESS_LOCAL_NETWORK. Izin ini diminta saat varian online dibuka. HTTP polos hanya diizinkan pada varian online melalui network security config; produksi wajib HTTPS."),
  B("URL backend ditetapkan saat build: -Pddb.baseUrl=http://<IP-laptop>:8080/ (bawaan http://10.0.2.2:8080/ untuk emulator)."),
  P("Karena logika ledger berada di backend, ledger in-memory Android hanya dipakai varian demo. Pada varian online, saldo dan riwayat selalu bersumber dari backend sehingga konsisten dengan Web (NFR-08)."),'''),
    ('["NFR-05 Keandalan", "Ledger atomik, idempotensi, 54 + 44 test otomatis", "Sebagian; crash-free rate belum diukur"]',
     '["NFR-05 Keandalan", "Ledger atomik, idempotensi, 63/58 test Android + 45 test backend", "Sebagian; crash-free rate belum diukur"]'),
    ('["Test", "./mvnw test (profil test, H2 in-memory)", "Berjalan; 44 test lulus"]', '["Test", "./mvnw test (profil test, H2 in-memory)", "Berjalan; 45 test lulus"]'),
    ('["2", "Android belum terhubung ke backend", "Konsistensi data lintas platform belum teruji", "Fase 5: klien REST, flavor online", "Salomo"]',
     '["2", "Varian online Android belum diuji di ponsel fisik dan dua perangkat sekaligus", "Kamera QRIS, push, dan transfer antarperangkat belum terverifikasi di perangkat nyata", "Uji di HP (Wi-Fi sama) dengan -Pddb.baseUrl", "Salomo"]'),
])

# ------------------------------------------------------------------ Dokumen 3
rep('doc3.js', [
    ('code: "FSD-DDB-001", version: "0.9",', 'code: "FSD-DDB-001", version: "0.9.1",'),
    ('''antarmuka eksternal (72 operasi API), spesifikasi FR-01–FR-29, aturan bisnis, workflow."],''',
     '''antarmuka eksternal (72 operasi API), spesifikasi FR-01–FR-29, aturan bisnis, workflow."],
    ["0.9.1", "11 Nov 2026", "Salomo P.G. Hokoyoku", "''' + REV + '''endpoint verifikasi PIN (73 operasi), status implementasi Android online, openapi.json bersih."],'''),
    ('if (eps.length !== 72) throw new Error("expected 72 operations, got " + eps.length);', 'if (eps.length !== 73) throw new Error("expected 73 operations, got " + eps.length);'),
    ('refs: "SRS-DDB-001 v0.1 (Bagian 3); SAD-DDB-001 v0.9; backend/docs/openapi.json (72 operasi);', 'refs: "SRS-DDB-001 v0.1 (Bagian 3); SAD-DDB-001 v0.9.1; backend/docs/openapi.json (73 operasi);'),
    ('Tabel 4.2 memuat seluruh 72 operasi API', 'Tabel 4.2 memuat seluruh 73 operasi API'),
    ('"PIN disimpan sebagai hash BCrypt, terpisah dari password. Setiap transaksi memverifikasi PIN; 3× salah memblokir PIN 5 menit (BR-02)."',
     '"PIN disimpan sebagai hash BCrypt, terpisah dari password. Setiap transaksi memverifikasi PIN; layar PIN setelah login memakai POST /auth/pin/verify dengan aturan yang sama; 3× salah memblokir PIN 5 menit (BR-02)."'),
    ('P("Status berikut disusun berdasarkan kode dan hasil uji per 11 November 2026. Implementasi Android masih memakai data simulasi in-memory; integrasi ke backend dijadwalkan 12–16 November 2026."),',
     'P("Status berikut disusun berdasarkan kode dan hasil uji per 11 November 2026. Aplikasi Android memiliki varian demo (data simulasi) dan varian online yang memanggil backend; varian online lulus uji end-to-end terhadap backend sungguhan."),'),
    ('["FR-01, FR-02, FR-04, FR-05", "Ada", "Ada", "Belum terverifikasi"],', '["FR-01, FR-02, FR-04, FR-05", "Ada", "Ada (demo dan online)", "Belum terverifikasi"],'),
    ('["FR-06 – FR-14", "Ada", "Ada", "Belum terverifikasi"],', '["FR-06 – FR-14", "Ada", "Ada (demo dan online)", "Belum terverifikasi"],'),
    ('["FR-16 – FR-25", "Ada", "Ada", "Belum terverifikasi"],', '["FR-16 – FR-25", "Ada", "Ada (demo dan online)", "Belum terverifikasi"],'),
    ('["FR-27 – FR-29", "Ada (foto berupa URL)", "Ada", "Belum terverifikasi"],', '["FR-27 – FR-29", "Ada (foto berupa URL)", "Ada (demo dan online)", "Belum terverifikasi"],'),
])

# ------------------------------------------------------------------ D04B
rep('doc4b.js', [
    ('code: "SDD-DDB-001-BE", version: "0.9",', 'code: "SDD-DDB-001-BE", version: "0.9.1",'),
    ('''10 pseudocode algoritma inti, perancangan software/hardware."],''',
     '''10 pseudocode algoritma inti, perancangan software/hardware."],
    ["0.9.1", "11 Nov 2026", "Salomo P.G. Hokoyoku", "''' + REV + '''endpoint POST /auth/pin/verify (18 operasi auth), 45 test, dokumentasi API dibangkitkan ulang."],'''),
    ('["AuthFlowTest", "8", "Registrasi + OTP, e-KYC, login, PIN dan blokir, sesi, pemulihan"]', '["AuthFlowTest", "9", "Registrasi + OTP, e-KYC, login, PIN dan blokir, verifikasi PIN mandiri, sesi, pemulihan"]'),
    ('[{ t: "Total", bold: true }, { t: "44", bold: true }, "Seluruhnya lulus pada 11 November 2026 (laporan surefire)"]', '[{ t: "Total", bold: true }, { t: "45", bold: true }, "Seluruhnya lulus pada 11 November 2026 (laporan surefire)"]'),
    ('["6", "openapi.json berisi karakter rusak (encoding)", "Bangkitkan ulang dari /v3/api-docs dengan encoding UTF-8"]',
     '["6", "openapi.json dan API.md disalin dari server berjalan (sudah diperbaiki 11 Nov)", "Bangkitkan ulang dari /v3/api-docs setiap endpoint berubah"]'),
])

# ------------------------------------------------------------------ D07B
rep('doc7b.js', [
    ('code: "DVM-DDB-001-BE", version: "0.9",', 'code: "DVM-DDB-001-BE", version: "0.9.1",'),
    ('''pola pengembangan, API, basis data, pengujian, deployment, troubleshooting.`],''',
     '''pola pengembangan, API, basis data, pengujian, deployment, troubleshooting.`],
    ["0.9.1", "11 Nov 2026", "Salomo P.G. Hokoyoku", "''' + REV + '''endpoint verifikasi PIN (73 operasi), 45 test, cara menghubungkan aplikasi Android varian online."],'''),
    ('"Swagger UI menampilkan 8 grup tag dan 72 operasi"', '"Swagger UI menampilkan 8 grup tag dan 73 operasi"'),
    ('./mvnw test                                    # 44 test integrasi API, ±40 detik', './mvnw test                                    # 45 test integrasi API, ±40 detik'),
    ('P("Daftar lengkap 72 operasi tersedia di Swagger UI dan di FSD-DDB-001 Tabel 4.2. Aturan bagi klien Android dan Web:"),',
     'P("Daftar lengkap 73 operasi tersedia di Swagger UI, backend/docs/API.md, dan FSD-DDB-001 Tabel 4.2. Aturan bagi klien Android dan Web:"),'),
    ('  H2("7.1 Simulator untuk Pengujian"),',
     '''  H2("7.1 Menghubungkan Aplikasi Android (Varian Online)"),
  code(`
# Emulator (backend di laptop yang sama)
./gradlew :app:assembleOnlineDebug                       # URL bawaan http://10.0.2.2:8080/
# HP di Wi-Fi yang sama
./gradlew :app:assembleOnlineDebug -Pddb.baseUrl=http://<IP-laptop>:8080/
# Uji end-to-end aplikasi ke backend yang sedang berjalan
./gradlew :app:testOnlineDebugUnitTest -Pddb.liveUrl=http://localhost:8080/
`),
  CAP("Gambar 7.1 Build dan uji varian online"),
  P("Di Android 17 (API 37), izinkan dialog Nearby devices (izin ACCESS_LOCAL_NETWORK) saat aplikasi dibuka; tanpa izin ini sistem memblokir koneksi ke alamat LAN dan aplikasi menampilkan pesan \\"Tidak dapat terhubung ke server\\". Pastikan juga Windows Firewall mengizinkan port 8080 untuk akses dari HP."),
  H2("7.2 Simulator untuk Pengujian"),'''),
])
print('ok')
