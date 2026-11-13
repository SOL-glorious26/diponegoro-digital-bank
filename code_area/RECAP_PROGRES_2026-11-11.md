# Rekap Progres – 11 November 2026 (malam)

> Saat kembali, arahkan Claude ke berkas ini: *"baca RECAP_PROGRES_2026-11-11.md lalu lanjutkan"*.
> Detail teknis per tugas ada di `LANJUTKAN.md` dan `DAFTAR_PEKERJAAN.md` (folder yang sama).

**Tenggat:** pengumpulan dokumentasi **24 Nov 2026 23.59 WIB**. Build baseline **18 Nov**, pengujian **19–22 Nov**, semua dokumen v1.0 **23 Nov**.

---

## 1. Status ringkas

| Area | Status | Catatan |
|---|---|---|
| Backend | 🟢 Selesai | 8 modul, 73 operasi API, 45/45 test lulus, sesuai SRS |
| Android | 🟢 Selesai (Fase 5) | Flavor `demo` + `online`; test demo 63 (1 dilewati), online 58; uji E2E di emulator lulus |
| Web | 🟢 Dibuat sendiri (malam 11 Nov) | Folder `apps/web/`: React + Vite + axios + react-router, terhubung ke backend; 38/38 uji kontrak API + 20/20 langkah uji UI (Chrome headless) lulus |
| Dokumen | 🟡 Berjalan | 6 dokumen Salomo selesai draf + 1 tutorial (lihat §2) |

## 2. Dokumen di `C:\software_engineering\Digital-Banking-Project-01\documents_area`

| Berkas | Kode | Versi | Status |
|---|---|---|---|
| DWP-DDB-001_..._v1.2.docx | DWP-DDB-001 | 1.2 | ✅ Status semua volume dimutakhirkan, tutorial = artefak pendukung, keputusan Web, NIM Grace 622024010 |
| DDB_D01_Project_Document_v0.9.2.docx | PRJ-DDB-001 | 0.9.2 | ✅ Review Steven diterapkan; risiko Web R11 + rencana cadangan (cakupan minimum Web per 17 Nov) |
| DDB_D02_Architectural_Design_v0.9.3.docx | SAD-DDB-001 | 0.9.3 | Web sudah diimplementasikan (8.2, Tabel 8.3); sebelumnya v0.9.2: ✅ Bab VIII: arsitektur Web ditetapkan PM (Vite, axios, router, token, proxy) + ruang lingkup Web v1; ADR-11/12 |
| DDB_D03_Functional_Design_v0.9.1.docx | FSD-DDB-001 | 0.9.1 | — |
| DDB_D04B_Detailed_Design_Backend_Database_v0.9.1.docx | SDD-DDB-001-BE | 0.9.1 | — |
| DDB_D07B_Developer_Manual_Backend_v0.9.1.docx | DVM-DDB-001-BE | 0.9.1 | Menunggu uji setup (Steven) |
| **DDB_D07A_Developer_Manual_Android_v0.9.docx** (baru) | DVM-DDB-001-AND | 0.9 | Menunggu review bagian QRIS/Profile (Gracia) |
| **DDB_D06_System_Testing_v0.9.2.docx** (baru) | STD-DDB-001 | 0.9.2 | Web 10/12, lintas platform 4/4 lulus; 6 defect (5 ditutup); Desain uji lengkap (BE-01–45, SA-01–28, WB-01–12, LP-01–04, NF-01–08), matriks FR, hasil test otomatis aktual, 4 defect; eksekusi manual 2/28; menunggu review Steven |
| **DDB_TUT_Panduan_Uji_Aplikasi_di_Ponsel_v1.0.docx** (baru) | TUT-DDB-001-AND | 1.0 | Belum dicoba di HP sungguhan; 20 skenario U-01–U-20 + lembar hasil uji |

Format semua dokumen: Palatino Linotype, judul/heading 16 bold, isi 12 spasi 1,15, keterangan gambar/tabel 10, teks hitam.
Belum dirender visual (tidak ada Word/LibreOffice di laptop) — buka di Word lalu klik kanan Daftar Isi → *Update Field*.

### Sumber generator dokumen
Disalin ke **`docs_generator/`** (folder proyek ini) karena folder scratchpad Claude bersifat sementara:
- `gen/ddbstyle.js` gaya bersama; `gen/doc1.js`, `doc2.js`, `doc3.js`, `doc4b.js`, `doc7b.js`, `doc7a.js`, `doctut.js` per dokumen.
- `gen/figs*.py` diagram (matplotlib) → `fig*/`; `kdoc.txt` (inventaris 110 berkas Android), `javadoc.txt` (backend).
- Butuh `npm install docx@9.5.1` di `gen/` sebelum dijalankan ulang (node_modules tidak ikut disalin).

## 3. Yang sudah dikerjakan hari ini (11 Nov)
1. Fase 5 Android: integrasi REST 8 modul + notifikasi, SecureTokenStore, izin `ACCESS_LOCAL_NETWORK` (Android 17), endpoint baru backend `POST /auth/pin/verify`.
2. Uji E2E emulator: login → PIN → Beranda → transfer Rp10.000 ke ANDI, terverifikasi di akun penerima.
3. Ringkasan progres untuk stakeholder.
4. D07A Developer Manual Android v0.9 dan Panduan Uji di Ponsel v1.0.
5. Analisis jawaban tim Web + draf chat untuk tim Web (§4).

## 4. Tim Web – kondisi & keputusan yang ditunggu

**Kondisi (jawaban tim Web):** React + JavaScript + lucide-react; `main.jsx` prototipe (Login, Dashboard, Transfer, Pembayaran, Riwayat, Profil) dengan data hardcoded; `package.json` & `styles.css` belum ada; belum ada fetch/axios, token, routing, cek e-KYC; tema masih biru terang + hijau.
PIC Sprint 1: Surya (Login US-01, OTP US-02), Steven (Transfer A US-04A), Rafi (Dashboard US-03, Transfer B US-04B), Nabila (Riwayat US-05). Modul lain belum ada PIC.

**Backend sudah sesuai SRS** (dicek langsung). Yang harus disesuaikan tim Web:
- Login = username/email + password (FR-03), **bukan OTP**. OTP hanya registrasi (FR-01), pemulihan (FR-05), ubah email/HP (FR-27).
- Transfer pakai **PIN 6 digit + header Idempotency-Key** (FR-04), bukan OTP.
- **Virtual Account tidak ada di SRS** → dihapus.
- **e-KYC wajib di Web** (FR-02 Must); sementara pakai akun `demo / Demo@1234 / PIN 123456`.
- Pindai QRIS khusus Mobile (FR-15); Web cukup QRIS pribadi (FR-16).
- SRS mewajibkan (Must) 14 FR di Web: FR-01–04, 06, 10–12, 16, 18, 19, 23, 24, 27. Sprint 1 baru ±7.

**Usulan keputusan (dikirim lewat chat):** Vite; axios + interceptor; access token di memori + refresh di sessionStorage; react-router-dom + ProtectedRoute + cek e-KYC; CSS sendiri dengan tema biru tua + emas; demo lokal dengan proxy Vite `/api → http://localhost:8080`.

**Diminta dari tim Web paling lambat 12 Nov:** setuju/tidak usulan; kesanggupan Sprint 1 terintegrasi API sebelum 18 Nov; PIC modul di luar Sprint 1; upload `package.json` + `styles.css`; screenshot ClickUp.

> Status: draf chat sudah dibuat, **belum diketahui apakah sudah dikirim** — tanyakan ke Salomo.

Catatan teknis CORS: backend memakai `.cors(withDefaults())` tanpa daftar origin → panggilan browser langsung ke :8080 akan ditolak. Gateway Nginx mengizinkan `localhost` tapi belum pernah diuji (Docker belum terpasang). Solusi: proxy Vite, atau tambahkan `CorsConfigurationSource` untuk localhost di backend.

## 5. Langkah berikutnya (urut prioritas)
1. **Tunggu/proses balasan tim Web.** Bila disetujui: buat *paket awal Web* (`vite.config.js` proxy, `api.js` axios + refresh + Idempotency-Key, `ProtectedRoute`, contoh 5 fitur Sprint 1). Opsional: CORS localhost di backend.
2. **Update dokumen:** ~~DWP~~ ✅ v1.2 · ~~D01~~ ✅ v0.9.2 · ~~D02~~ ✅ v0.9.2 (arsitektur Web ditetapkan PM, tidak menunggu tim Web).
3. **Uji di HP sungguhan** pakai Panduan TUT-DDB-001-AND (19–22 Nov), termasuk uji 2 perangkat (U-16, U-17); revisi tutorial setelahnya.
4. ~~Dokumen 6~~ ✅ v0.9 dibuat. Berikutnya: eksekusi SA-02–SA-28 (emulator + HP), ukur NF-01–03, pasang JaCoCo (NF-08), isi hasil ke `docs_generator/gen/doc6.js` (array SA/NF) + `doc6_results.json`, lalu bangkitkan ulang.
5. Sisa teknis: PostgreSQL profil dev (butuh password postgres dari Salomo), JaCoCo, Docker/gateway, TLS, BiometricPrompt nyata.

## 6. Info cepat
- Backend: `cd backend` → `.\run-local.ps1 -H2` → `http://localhost:8080/swagger-ui.html`.
- Android: `apps/android` (dulu `submit_mobile/Mobile/Modul-salomo`); `./gradlew :app:assembleDemoDebug` / `:app:assembleOnlineDebug -Pddb.baseUrl=http://<IP>:8080/`.
- Akun demo: `demo / Demo@1234 / PIN 123456` (rek. 1400123456); andi/budi/citra = 1400000002/03/04.
- Simulator: BRI 9999999999 ditolak; Mandiri 8888888888 timeout; NIK/ID pelanggan berakhiran 0000 ditolak/tidak ditemukan.
- PIC Android: Salomo (Auth, Virtual Card), Cecilia (Transfer, History), Gracia (QRIS, Profile), Grace (Account, Top-up).
- Cadangan: `_arsip/backup_android_sebelum_fase5_2026-11-11.zip`. Prototipe Web tim: `_arsip/web_team/`.

## 7. Pembaruan malam 11 Nov
- D01 v0.9.2 (review Steven) dan DWP v1.2 selesai. Skrip patch DWP: `docs_generator/gen/patch_dwp12.py` (+ `dumptables.py`, `rezip.py`); D01 v0.9.2 & DWP diedit lewat XML, jadi `doc1.js` sudah tidak sinkron.
- Jawaban tim Web (update 9 butir) = sumber analisis §4. Balasan PM (usulan Vite/axios/router) belum dipastikan terkirim.
- UI Map Web v0.3 (Nabila) sudah mengikuti SRS; pertanyaan terbuka: Kantong (UI-20) & Virtual Card (UI-21) di Web v1, nilai BR-01/BR-07.
- Arahan Salomo (11 Nov malam): timeline tidak ketat; Web hanya formalitas, proyek boleh selesai lebih cepat → jangan menunggu tim Web. D02 v0.9.2 dibangkitkan dari `gen/doc2.js` (cadangan versi lama: `doc2_v091.js.bak`); `node_modules` sudah terpasang di `gen/`.
- D06 v0.9 (STD-DDB-001): generator `gen/doc6.js` + `gen/doc6_results.json`. Test dijalankan ulang 11 Nov 21.03–21.05 WIB: backend 45/45, Android demo 62/63 (1 dilewati). DWP v1.2 sudah mencatat D06.

## 8. Aplikasi Web dibuat sendiri (11 Nov malam)
- Keputusan Salomo: tidak menunggu tim Web; Web dibangun sesuai D02 Bab VIII di folder `web/` (lihat `web/README.md`).
- Jalankan: backend `.
un-local.ps1 -H2`, lalu `cd web; npm install; npm run dev` → http://localhost:5173 (akun demo).
- Fitur: login tanpa OTP, daftar + OTP, e-KYC, buat PIN, pemulihan, Beranda (saldo, sembunyikan, grafik), Transfer (PIN + Idempotency-Key, favorit, bukti), Riwayat (filter, detail + jurnal, ekspor CSV/PDF), Top-up (pulsa, data, PLN, tagihan, e-wallet), QRIS pribadi/dinamis, Kantong, Virtual Card, Profil (OTP, password/PIN, notifikasi, sesi, logout semua), Notifikasi. Responsif (diuji lebar 390px).
- Uji: `npm run test:api` (38/38) dan `npm run test:ui` (20/20, tangkapan layar di `web/test-output/2026-11-11/`).
- Dokumen ikut diperbarui: D02 v0.9.3, D06 v0.9.1, DWP v1.2. **D01 v0.9.2 belum** (risiko R11 & rencana cadangan Web masih menulis Web belum terintegrasi) → perlu revisi v0.9.3 lewat XML.
- Sisa Web: WB-09 (pindai QR Web dari Android), WB-11 (profil OTP via UI), WB-12 (token kedaluwarsa), LP-01–04, uji di Firefox/Edge. Tim Web cukup diberi tahu & diminta review/uji (D07W Developer Manual Web bisa berangkat dari `web/README.md`).

## 9. Uji lintas platform Android ↔ Web (11 Nov malam) – 4/4 lulus
- `cd web; npm run test:lp` (emulator Pixel_8 + APK online + backend + `npm run dev`). Lulus 2× berturut-turut. Bukti: `web/test-output/2026-11-11-lintas-platform/`.
- LP-01 transfer Android → saldo/riwayat andi di Web; LP-02 QRIS dinamis Web → dipindai Android dari galeri; LP-03 e-wallet di Web → riwayat Android; LP-04 logout semua perangkat dari Android → Web ke login + pesan.
- Defect baru & sudah diperbaiki: DEF-05 (Android: waktu sesi tanpa tanggal → `formatDateTime`, 63 test tetap lulus), DEF-06 (Web: tidak ada pesan sesi berakhir → ditambahkan).
- Catatan alat uji: `adb input text` kadang menghilangkan spasi (IME emulator) → QRIS diuji lewat gambar galeri, bukan ketik payload.
- Sisa uji: WB-11, WB-12, SA di HP fisik, NF-01–03, JaCoCo, Firefox/Edge. D01 masih perlu v0.9.3 (status Web & R11).

## 10. Pengujian lanjutan & struktur folder baru (12 Nov)
**Struktur baru** (lihat `README.md` di root): `apps/android/` (dulu `submit_mobile/Mobile/Modul-salomo`), `apps/web/` (dulu `web/`),
`pengujian/` (semua skrip uji + `bukti/2026-11-11/`), `_arsip/` (zip lama, MyApplication kosong, prototipe `web_team`). Tidak ada yang dihapus permanen
selain hasil build (gradle clean, `dist/`).

**Hasil uji (pengujian/, 11–12 Nov):**
- Android SA-01–SA-28 di emulator: 27/28 lulus (SA-08 kamera menunggu HP fisik; SA-23 biometrik = simulasi). `npm run android:sa`.
- Web WB-01–WB-12: 12/12; Chrome 157 / Edge 157 / Firefox 156 lulus; lintas platform LP-01–04: 4/4.
- NFR: p95 API 13 ms, transfer ≤ 94 ms, muat Web ≤ 956 ms, cold start Android ≤ 1,37 dtk; JaCoCo backend 92,2% baris (plugin di `backend/pom.xml`).
- Defect baru & diperbaiki: DEF-07 (Android back stack OTP), DEF-08 (login sebelum e-KYC → langsung PIN), DEF-09 (Web ubah profil gagal),
  DEF-10 (Android blokir PIN tersimpan setelah pemulihan), DEF-11 (Riwayat "0 transaksi" saat offline; layar Sesi masih terbuka).
- Dokumen: D01 v0.9.3, D02 v0.9.4, D06 v0.9.3, D07A v0.9.1, TUT v1.1, DWP v1.2 (semua sinkron).

**Sisa:** SA-08 + uji di HP fisik (NF-07), DEF-11 layar Sesi, DEF-04 CORS, NF-04 (butuh Docker), review Steven.

## 11. D01 v0.9.3 (12 Nov)
- Patch XML (`docs_generator/gen/patch_d01_093.py`; `doc1.js` tidak sinkron sejak v0.9.2). Gambar Gantt & peta risiko dibuat ulang dari `figs.py`.
- Isi: status Web/Android/backend 12 Nov, Tabel 2.1 + Gantt (Web selesai, pengujian emulator selesai, baris baru "Uji ponsel fisik + laporan Dok. 6"),
  3.7 hasil uji, rencana cadangan Web tidak diperlukan, R8 & R11 termitigasi, COCOMO dengan ukuran Web terukur:
  **16,085 KSLOC → 62,38 PM, 13,66 bulan, total ±Rp689.540.065** (sebelumnya 18,114 / 71,09 / Rp785.303.969).

## 12. Kelengkapan fitur Web & DEF-11 (12 Nov)
- Web ditambah: **grafik saldo harian/mingguan/bulanan (FR-07)** di Beranda, **foto profil (FR-27, URL)** + avatar di bilah atas,
  **ubah & kunci kantong (FR-08/09)**. Uji WB-13–WB-15 lulus (`npm run web:fitur`); regresi Web/peramban/lintas platform lulus.
- Android: layar Sesi kini menampilkan pesan saat server tidak terjangkau → **DEF-11 ditutup**. Defect terbuka tinggal DEF-04 (CORS).
- Dokumen: D06 v0.9.4 (Web 15/15), DWP v1.2 disinkronkan.
- Sisa: uji HP fisik (SA-08, NF-07), APK rilis, DEF-04, Docker/gateway (NF-04), PostgreSQL (butuh password `postgres`), deploy.

## 13. Persiapan rilis (12 Nov)
- **DEF-04 ditutup:** CORS dikonfigurasi di backend (`ddb.cors.allowed-origins` / env `DDB_CORS_ALLOWED_ORIGINS`, default localhost),
  header CORS backend disembunyikan di `gateway/nginx.conf`; `.env.example` & `docker-compose.yml` diperbarui. 47 test backend lulus (CorsTest).
  Terbukti: peramban di :5173 bisa login langsung ke :8080; origin lain ditolak 403. **Semua 11 defect kini ditutup.**
- **APK rilis bertanda tangan:** `apps/android/keystore/ddb-release.jks` + `keystore.properties` (**rahasia — simpan cadangan, jangan dibagikan**).
  `cd apps/android; .uild-rilis.ps1 [-BaseUrl http://<IP-laptop>:8080/]` → `rilis/DDB-1.0-demo.apk`, `rilis/DDB-1.0-online-<host>.apk`.
  APK rilis diuji di emulator: SA-01/07/09/10/13 + LP-01–04 lulus. (Emulator saat ini terpasang versi **rilis**; copot dulu bila ingin pasang debug.)
- Dokumen: D06 v0.9.5, D07A v0.9.2 (bagian 3.5 APK rilis), D07B v0.9.2 (CORS, JaCoCo), DWP v1.2.
- Sisa: uji HP fisik (SA-08, NF-07, butuh build dengan IP laptop), Docker/gateway (NF-04), PostgreSQL (password `postgres`), deploy + HTTPS.

## 14. Rapikan documents_area + D03/D04B v0.9.2 (13 Nov)
- `documents_area` kini **satu folder per volume** (hanya versi terbaru); versi lama + brief dummy di `_temp_versi_lama/`; indeks di `documents_area/README.md`.
  Simpan versi baru di folder volumenya (argumen OUT generator), lalu pindahkan versi lama ke `_temp_versi_lama/`.
- **D03 v0.9.2** (`gen/doc3.js`): Tabel 4.1 kolom Web diisi rute/berkas `apps/web`, Tabel 7.1 Web = Ada (dari D06 v0.9.5), isu "Web belum terverifikasi" diganti isu uji ponsel fisik.
- **D04B v0.9.2** (`gen/doc4b.js`): CORS backend + peran gateway, env `DDB_CORS_ALLOWED_ORIGINS`, CorsTest (47 test lulus 13 Nov), JaCoCo **92,3% baris / 64,8% cabang**, hasil performa dari D06.
  Catatan: D06 v0.9.5 menulis JaCoCo 92,2% (ukuran 12 Nov); bisa diselaraskan saat D06 berikutnya.
- **DWP v1.3** (`gen/patch_dwp13.py`): status D03/D04B/D07B v0.9.2, revisi 1.3.
- Perintah generator: `node doc3.js ../fig ../fig3 <OUT> <logo> ../../backend/docs/openapi.json`; `node doc4b.js ../fig4 ../fig3 ../fig <OUT> <logo>`; logo = `submit_mobile/diponegoro-corp-logo.jpeg`.
- Sisa (butuh Salomo): uji HP fisik (SA-08, NF-07), password `postgres`, Docker (NF-04), deploy; review Steven (R2).

## 15. PostgreSQL dev aktif + siap Docker (13 Nov)
- Password superuser `postgres` dipulihkan (kandidat pertama dari daftar Salomo berhasil) — tanyakan Salomo bila lupa lagi.
- Dibuat: role `ddb` (password `ddb`) + database `ddb` OWNER ddb. Backend profil `dev` dijalankan:
  Flyway menerapkan V1 (18 tabel), seeder membuat akun demo/andi/budi/citra, login API `demo/Demo@1234` -> HTTP 200 (token).
  Jadi 2.2 DAFTAR_PEKERJAAN (profil dev PostgreSQL) **selesai**. Jalankan ulang: `cd backend; .\run-local.ps1` (atau `-DbPassword ddb`).
- Docker belum terpasang — tidak bisa diotomasi penuh (butuh hak admin + reboot). Prasyarat: winget ada (v1.29), WSL belum ada, virtualisasi aktif.
  Langkah manual Salomo (PowerShell admin): `wsl --install` -> reboot -> `winget install Docker.DockerDesktop` -> buka Docker Desktop, terima lisensi.
  Sesudah itu: `.env` sudah dibuat otomatis (JWT_SECRET & DATA_KEY acak) -> `docker compose up -d --build` -> http://localhost/swagger-ui.html.
  Itu membuka NF-04 (uji rate limit gateway) dan NF menyeluruh via gateway.
