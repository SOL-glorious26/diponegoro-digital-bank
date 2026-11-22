# Pengujian End-to-End Diponegoro Digital Bank

Skrip uji sistem untuk STD-DDB-001 (Dokumen 6). Semua skrip memakai Node.js 24 tanpa pustaka tambahan:
Web dikendalikan lewat Chrome DevTools Protocol (Chrome, Edge) atau WebDriver BiDi (Firefox),
Android lewat `adb` + `uiautomator`.

## Prasyarat

1. Backend berjalan: `cd backend; .\run-local.ps1 -H2` (port 8080).
2. Web resmi tim Web berjalan: `cd apps\web\digitalbanking-node; npm install; npm start` (port 3000).
3. Untuk uji Android: emulator berjalan dengan APK varian online (`cd apps\android; .\gradlew :app:installOnlineDebug`).

## Menjalankan

| Perintah (di folder `pengujian/`) | Kasus STD-DDB-001 | Isi |
|---|---|---|
| `npm run web:node` | WN-01 – WN-14 | Alur inti Web resmi (Node) terhadap backend asli |
| `npm run web:node:ekstra` | WN-15 – WN-25 | Uji lanjutan Web resmi (WN-21 butuh backend :8081 token 1 menit dan Web :3001) |
| `npm run web:node:peramban` | NF-09 | Web resmi di Chrome, Edge, Firefox |
| `npm run lintas:node` | LPN-01 – LPN-03 | Lintas platform Android ↔ Web resmi |
| `node android/sa08-kamera-emulator.mjs hasil/sa08 <qr.png>` | SA-08 | Pindai QRIS dengan kamera virtual emulator |
| `npm run web:api` | Kontrak API Web | 38 pemeriksaan API lewat proxy Vite |
| `npm run web:ui` | WB-01 – WB-10 | Uji UI Web di Chrome headless (20 langkah, termasuk tampilan ponsel) |
| `npm run web:extra` | WB-11 | Ubah profil dengan OTP |
| `npm run web:fitur` | WB-13 – WB-15 | Grafik saldo (FR-07), foto profil (FR-27), ubah dan kunci kantong (FR-08, FR-09) |
| `npm run web:token` | WB-12 | Token kedaluwarsa (backend harus dijalankan dengan `$env:DDB_SECURITY_ACCESS_TOKEN_MINUTES="1"`) |
| `npm run web:browsers` | NFR-10 | Alur inti di Chrome, Edge, Firefox |
| `npm run android:sa` | SA-01 – SA-28 | Uji sistem Android di emulator |
| `npm run lintas` | LP-01 – LP-04 | Uji lintas platform Android ↔ Web |
| `npm run perf` | NF-01 – NF-03 | Pengukuran performa dasar (Web resmi) |

Baris `web:api`, `web:ui`, `web:extra`, `web:fitur`, `web:token`, `web:browsers`, dan `lintas` menguji Web React lama
(`apps/web_dummy_buatan_sendiri`) dan tidak dihitung lagi pada STD-DDB-001.

Hasil tiap jalan ditulis ke `hasil/` (tidak disimpan permanen). Bukti yang dilampirkan ke Dokumen 6
disalin ke `bukti/<tanggal>/`.

## Struktur

| Folder | Isi |
|---|---|
| `lib/` | `adbui.mjs` (penggerak UI Android), `android-flows.mjs` (login, PIN, Beranda), `cdp.mjs` (Chrome/Edge), `bidi.mjs` (Firefox) |
| `web/` | Uji Web: `api-smoke.mjs`, `ui-e2e.mjs`, `wb-extra.mjs`, `browsers.mjs` |
| `android/` | `sa-android.mjs` (uji sistem), `explore.mjs` (alat bantu memetakan layar) |
| `lintas-platform/` | `lp-cross.mjs` |
| `performa/` | `perf.mjs` |
| `bukti/` | Tangkapan layar dan `hasil.txt` per tanggal eksekusi |

## Catatan

- `adb input text` di emulator kadang menghilangkan spasi; QRIS diuji lewat gambar galeri, bukan mengetik payload.
- Uji yang mengubah saldo memakai akun berbeda (demo/andi untuk Android, budi/citra untuk performa dan peramban) agar dapat dijalankan bersamaan.
- SA-18 (diam > 5 menit) dan SA-20 (backend dimatikan) hanya berjalan bila disebut: `node android/sa-android.mjs hasil/sa SA-18,SA-20`.
