import sys
root = sys.argv[1]

def rep(path, pairs):
    p = root + '/' + path
    s = open(p, encoding='utf8').read()
    for a, b in pairs:
        assert a in s, (path, a[:70])
        s = s.replace(a, b, 1)
    open(p, 'w', encoding='utf8', newline='\n').write(s)

rep('DAFTAR_PEKERJAAN.md', [
    ('| 1.2 | Rancang daftar endpoint REST per modul + format error + format JWT | `backend/docs/API.md` | 🔄 dibuat otomatis, perlu perbaikan teks (lihat LANJUTKAN.md) |',
     '| 1.2 | Rancang daftar endpoint REST per modul + format error + format JWT | `backend/docs/API.md` | ✅ dibangkitkan ulang dari openapi.json (UTF-8, 11 Nov) |'),
    ('| 1.3 | OpenAPI otomatis dari kode (Swagger UI) untuk tim web | `/swagger-ui.html`, `/v3/api-docs` | ✅ 72 operasi |',
     '| 1.3 | OpenAPI otomatis dari kode (Swagger UI) untuk tim web | `/swagger-ui.html`, `/v3/api-docs`, `backend/docs/openapi.json` | ✅ 73 operasi (berkas diperbarui, encoding diperbaiki) |'),
    ('| 3.11 | Unit & integration test backend, target cakupan logika inti ≥ 70% (NFR-12) | — | bersama | 🔄 44 test lulus; laporan cakupan (JaCoCo) belum |',
     '| 3.11 | Unit & integration test backend, target cakupan logika inti ≥ 70% (NFR-12) | — | bersama | 🔄 45 test lulus; laporan cakupan (JaCoCo) belum |\n'
     '| 3.12 | Endpoint `POST /auth/pin/verify` (layar PIN aplikasi setelah login; aturan blokir 3× sama) | FR-04, BR-02 | Salomo | ✅ 1 test |'),
    ('| 5.1 | Tambah Retrofit + OkHttp + kotlinx-serialization, klien API + interceptor JWT (refresh otomatis) | ⏳ |',
     '| 5.1 | Tambah Retrofit + OkHttp + kotlinx-serialization, klien API + interceptor JWT (refresh otomatis) | ✅ `core/network` (Retrofit 3.0, OkHttp 4.12, kotlinx-serialization 1.9) |'),
    ('| 5.2 | Product flavor `demo` (offline, repository mock seperti sekarang) & `online` (repository REST) | ⏳ |',
     '| 5.2 | Product flavor `demo` (offline, repository mock seperti sekarang) & `online` (repository REST) | ✅ `-Pddb.baseUrl=` untuk ganti URL |'),
    ('| 5.3 | Repository REST per modul (layar & ViewModel tidak diubah) | ⏳ |',
     '| 5.3 | Repository REST per modul (layar & ViewModel tidak diubah) | ✅ 8 modul + notifikasi (interface + Local/Remote); ViewModel hanya dibuat asinkron |'),
    ('| 5.4 | Penyimpanan token aman (EncryptedSharedPreferences / DataStore) | ⏳ |',
     '| 5.4 | Penyimpanan token aman (EncryptedSharedPreferences / DataStore) | ✅ `SecureTokenStore`: AES-GCM dengan kunci Android Keystore |'),
    ('| 5.5 | Penanganan offline/error jaringan + loading state | ⏳ |',
     '| 5.5 | Penanganan offline/error jaringan + loading state | ✅ pesan error server diteruskan; pesan offline; sesi berakhir → login |'),
    ('| 5.6 | Pastikan 54 test lama tetap lulus (flavor `demo`) + test repository REST (MockWebServer) | ⏳ |',
     '| 5.6 | Pastikan 54 test lama tetap lulus (flavor `demo`) + test repository REST (MockWebServer) | ✅ demo 63 lulus (1 dilewati); online 58 lulus termasuk uji live ke backend |\n'
     '| 5.7 | Android 17 (API 37): izin `ACCESS_LOCAL_NETWORK` + network security config untuk backend LAN | ✅ diminta saat aplikasi dibuka (varian online) |'),
    ('| 6.2 | Tes APK `online` di HP lewat Wi-Fi yang sama atau ngrok (HTTPS) | ⏳ |',
     '| 6.2 | Tes APK `online` di HP lewat Wi-Fi yang sama atau ngrok (HTTPS) | 🔄 emulator ✅ (login, PIN, Beranda, transfer); HP sungguhan ⏳ |'),
    ('| 6.3 | Tes end-to-end: 2 HP/emulator saling transfer → saldo konsisten di keduanya | ⏳ |',
     '| 6.3 | Tes end-to-end: 2 HP/emulator saling transfer → saldo konsisten di keduanya | 🔄 uji live (`LiveBackendTest`) + transfer UI DEMO→ANDI terverifikasi di sisi penerima; 2 perangkat ⏳ |'),
    ('- 11 Nov 2026 — Semua 8 modul backend selesai, data demo, gateway Nginx, Docker compose, README backend. 44 test lulus. Bug *lost update* saldo saat transaksi paralel ditemukan & diperbaiki. Server terbukti jalan (profil h2) dengan 72 operasi API.',
     '- 11 Nov 2026 — Semua 8 modul backend selesai, data demo, gateway Nginx, Docker compose, README backend. 44 test lulus. Bug *lost update* saldo saat transaksi paralel ditemukan & diperbaiki. Server terbukti jalan (profil h2) dengan 72 operasi API.\n'
     '- 11 Nov 2026 (sore) — Fase 5 selesai: Android punya flavor `demo` & `online`; 8 modul + notifikasi terhubung REST. Backend: endpoint `POST /auth/pin/verify` (45 test). '
     'openapi.json & API.md dibangkitkan ulang (73 operasi, UTF-8). Ditemukan: Android 17 memblokir akses LAN tanpa `ACCESS_LOCAL_NETWORK` → ditambahkan. '
     'Diuji di emulator: login → PIN → Beranda (data server) → transfer Rp10.000 ke ANDI, terlihat di akun penerima.'),
])

rep('LANJUTKAN.md', [
    ('## Status singkat\n', '''## Status singkat (sore 11 Nov 2026)
- **Fase 5 selesai.** Android: flavor `demo` (data simulasi, seperti sebelumnya) dan `online` (backend REST).
  - Jalankan online di emulator: backend `.\\run-local.ps1 -H2`, lalu `./gradlew :app:assembleOnlineDebug` (URL default `http://10.0.2.2:8080/`).
  - HP di Wi-Fi yang sama: `./gradlew :app:assembleOnlineDebug -Pddb.baseUrl=http://<IP-laptop>:8080/`.
  - Android 17: izinkan dialog "Nearby devices" (izin `ACCESS_LOCAL_NETWORK`), kalau tidak muncul pesan "Tidak dapat terhubung ke server".
  - Test: `./gradlew :app:testDemoDebugUnitTest` (63) dan `:app:testOnlineDebugUnitTest -Pddb.liveUrl=http://localhost:8080/` (58, termasuk uji live).
- Backend: endpoint baru `POST /auth/pin/verify`; **45 test lulus**; `docs/openapi.json` + `docs/API.md` sudah bersih (73 operasi).
- Cadangan Android sebelum Fase 5: `backup_android_sebelum_fase5_2026-11-11.zip` (folder proyek).

### Status sebelumnya
'''),
    ('1. **Perbaiki `backend/docs/API.md`**', '1. ~~Perbaiki `backend/docs/API.md`~~ ✅ (11 Nov sore). **Perbaiki `backend/docs/API.md`**'),
    ('4. **Fase 5 — integrasi Android**', '4. ~~**Fase 5 — integrasi Android**~~ ✅ (11 Nov sore). **Fase 5 — integrasi Android**'),
])

rep('submit_mobile/Mobile/Modul-salomo/README.md', [
    ('''```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"   # Git Bash, Windows
./gradlew :app:assembleDebug        # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # 54 unit/UI test (Robolectric), ±15 detik
```''', '''Dua varian (product flavor), DAFTAR_PEKERJAAN Fase 5:

| Varian | Data | APK |
|---|---|---|
| `demo` | Simulasi in-memory (tanpa server), untuk uji UI & cadangan demo | `app/build/outputs/apk/demo/debug/app-demo-debug.apk` |
| `online` | Backend bersama lewat REST (`/api/v1`) | `app/build/outputs/apk/online/debug/app-online-debug.apk` |

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"   # Git Bash, Windows
./gradlew :app:assembleDemoDebug                       # varian demo
./gradlew :app:assembleOnlineDebug                     # online, backend di http://10.0.2.2:8080/ (emulator)
./gradlew :app:assembleOnlineDebug -Pddb.baseUrl=http://192.168.1.10:8080/   # online, HP di Wi-Fi yang sama
./gradlew :app:testDemoDebugUnitTest                   # 63 test (54 lama + 8 MockWebServer + 1 live dilewati)
./gradlew :app:testOnlineDebugUnitTest -Pddb.liveUrl=http://localhost:8080/  # 58 test termasuk uji live ke backend
```

Varian online di Android 17 (API 37) meminta izin **Nearby devices / `ACCESS_LOCAL_NETWORK`** saat dibuka;
izin ini wajib selama backend berjalan di laptop/LAN. HTTP polos hanya diizinkan pada varian online
(`src/online/res/xml/network_security_config.xml`); produksi wajib HTTPS.'''),
])
print('ok')
