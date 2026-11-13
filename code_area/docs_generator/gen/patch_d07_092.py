import sys, os, re
os.chdir(sys.argv[1])

def patch(p, pairs):
    s = open(p, encoding='utf-8').read()
    for a, b in pairs:
        assert s.count(a) == 1, (p, s.count(a), a[:70])
        s = s.replace(a, b)
    open(p, 'w', encoding='utf-8').write(s)

def add_revision(p, row):
    s = open(p, encoding='utf-8').read()
    i = s.index('  revisions: [')
    j = s.index('  ],\n};', i)
    s = s[:j] + row + '\n' + s[j:]
    open(p, 'w', encoding='utf-8').write(s)

# ---------------- D07A v0.9.2 ----------------
patch('doc7a.js', [
    ('code: "DVM-DDB-001-AND", version: "0.9.1",', 'code: "DVM-DDB-001-AND", version: "0.9.2",'),
    ('  H1("BAB IV  ARSITEKTUR APLIKASI"),', '''  H2("3.5 APK Rilis Bertanda Tangan"),
  P("APK rilis ditandatangani dengan kunci di apps/android/keystore/ddb-release.jks. Lokasi berkas dan password dibaca build.gradle.kts dari apps/android/keystore.properties. Kedua berkas tidak boleh dibagikan atau dimasukkan ke repositori (.gitignore). Simpan cadangannya di tempat aman, karena pembaruan aplikasi wajib ditandatangani dengan kunci yang sama. Bila keystore.properties tidak ada, build rilis tetap berjalan tetapi APK tidak ditandatangani."),
  code(`
# PowerShell, dari folder apps/android
.\\\\build-rilis.ps1                                    # online untuk emulator (http://10.0.2.2:8080/)
.\\\\build-rilis.ps1 -BaseUrl http://192.168.1.10:8080/  # online untuk ponsel di Wi-Fi yang sama
# Hasil: rilis/DDB-1.0-demo.apk dan rilis/DDB-1.0-online-<host>.apk (tanda tangan diverifikasi apksigner, SHA-256 dicetak)

# Membuat keystore baru (hanya bila belum ada; JDK Android Studio)
keytool -genkeypair -keystore keystore/ddb-release.jks -storetype PKCS12 -alias ddb -keyalg RSA -keysize 2048 -validity 10000
`),
  CAP("Gambar 3.2 Membangun APK rilis"),
  table(["Butir", "Nilai"], [
    ["Sertifikat", "CN=Diponegoro Digital Bank, OU=Kelompok Jaran Goyang, O=Universitas Kristen Satya Wacana, L=Salatiga, C=ID"],
    ["Skema tanda tangan", "APK Signature Scheme v2 (minSdk 24)"],
    ["Optimasi R8/minify", "Nonaktif (Retrofit dan kotlinx-serialization tidak memerlukan aturan ProGuard tambahan)"],
    ["Versi", "versionName 1.0, versionCode 1 (app/build.gradle.kts)"],
  ], [2200, 6870], { boldFirst: true, size: 17 }),
  CAP("Tabel 3.3 APK rilis"),
  NOTE("APK rilis dan debug varian online memakai application ID yang sama tetapi tanda tangan berbeda. Copot versi debug lebih dulu (adb uninstall com.diponegoro.digitalbank) sebelum memasang rilis."),

  H1("BAB IV  ARSITEKTUR APLIKASI"),'''),
    ('    ["Path /sdcard rusak di Git Bash", "Konversi path MSYS", "Awali perintah adb dengan MSYS_NO_PATHCONV=1"],',
     '''    ["Path /sdcard rusak di Git Bash", "Konversi path MSYS", "Awali perintah adb dengan MSYS_NO_PATHCONV=1"],
    ["INSTALL_FAILED_UPDATE_INCOMPATIBLE saat memasang APK", "Versi terpasang ditandatangani kunci lain (debug vs rilis)", "Copot aplikasi lama: adb uninstall com.diponegoro.digitalbank"],
    ["build-rilis.ps1: keystore.properties tidak ditemukan", "Kunci rilis tidak ada di komputer ini", "Minta berkas keystore dan keystore.properties ke Project Manager"],'''),
    ('  B("Uji di ponsel sungguhan dan uji dua perangkat saling transfer belum dilakukan (dijadwalkan 19–22 November 2026)."),',
     '  B("Uji sistem SA-01–SA-28 sudah dijalankan di emulator (STD-DDB-001); uji di ponsel fisik, termasuk pindai QRIS dengan kamera, belum dilakukan."),'),
])
add_revision('doc7a.js', '    ["0.9.2", "12 Nov 2026", "Salomo P.G. Hokoyoku", "Bagian 3.5 APK rilis bertanda tangan (keystore, build-rilis.ps1, verifikasi apksigner); troubleshooting pemasangan APK rilis; keterbatasan diperbarui (layar Sesi saat server tidak terjangkau, DEF-11)."],')

# ---------------- D07B v0.9.2 ----------------
patch('doc7b.js', [
    ('code: "DVM-DDB-001-BE", version: "0.9.1",', 'code: "DVM-DDB-001-BE", version: "0.9.2",'),
    ('date: "11 November 2026", logo: LOGO,', 'date: "12 November 2026", logo: LOGO,'),
    ('    ["SEED_DEMO_DATA (ddb.seed-demo-data)", "true", "Data demo saat basis data kosong; false pada profil test"],',
     '''    ["SEED_DEMO_DATA (ddb.seed-demo-data)", "true", "Data demo saat basis data kosong; false pada profil test"],
    ["DDB_CORS_ALLOWED_ORIGINS (ddb.cors.allowed-origins)", "http://localhost:*, http://127.0.0.1:*", "Origin Web yang boleh memanggil API langsung dari peramban (pisahkan koma); tambahkan domain Web saat deploy"],'''),
    ('./mvnw test                                    # 45 test integrasi API, ±40 detik',
     './mvnw test                                    # 47 test integrasi API + laporan JaCoCo, ±70 detik'),
    ('# Laporan: backend/target/surefire-reports/', '# Laporan: backend/target/surefire-reports/ dan backend/target/site/jacoco/index.html (cakupan)'),
    ('    "Tambahkan domain Web ke peta CORS di gateway/nginx.conf.",',
     '    "Tambahkan domain Web ke DDB_CORS_ALLOWED_ORIGINS (.env) dan ke peta $cors_origin di gateway/nginx.conf. Gateway menyembunyikan header CORS dari backend agar tidak ganda.",'),
    ('    ["Ponsel tidak dapat mengakses backend", "Firewall Windows atau beda jaringan", "Gunakan Wi-Fi yang sama, izinkan port 8080, atau gunakan ngrok"],',
     '''    ["Ponsel tidak dapat mengakses backend", "Firewall Windows atau beda jaringan", "Gunakan Wi-Fi yang sama, izinkan port 8080, atau gunakan ngrok"],
    ["Peramban: CORS error / 403 Invalid CORS request", "Origin aplikasi Web tidak terdaftar", "Tambahkan origin ke DDB_CORS_ALLOWED_ORIGINS, atau jalankan Web lewat proxy Vite"],'''),
    ('  B("Laporan cakupan test (JaCoCo) belum tersedia."),', '  B("Cakupan test: 92,2% baris, 64,8% cabang (JaCoCo, 12 November 2026)."),'),
])
s = open('doc7b.js', encoding='utf-8').read()
s = s.replace('kode ddb-backend per 11 November 2026', 'kode ddb-backend per 12 November 2026')
open('doc7b.js', 'w', encoding='utf-8').write(s)
add_revision('doc7b.js', '    ["0.9.2", "12 Nov 2026", "Salomo P.G. Hokoyoku", "CORS dapat dikonfigurasi (ddb.cors.allowed-origins, DEF-04) dan header CORS backend disembunyikan di gateway; 47 test (CorsTest); laporan JaCoCo; troubleshooting CORS."],')
print('ok')
