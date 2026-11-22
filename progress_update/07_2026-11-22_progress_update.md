# Progress Update #07: Minggu malam, 22 November 2026

Proyek: **Diponegoro Digital Bank**, Kelompok Jaran Goyang (Software Engineering UKSW)
Disusun oleh: Salomo (PM)
Tenggat pengumpulan: **23 November 2026, 23.59 WIB** (DWP semula menulis 24 November)

---

## 1. Dokumen Android, Backend, dan lintas platform: final v1.0

| Volume | Versi | Catatan |
|---|---|---|
| D01, D02, D03 | v1.0 | Web resmi = Web tim (Node); peran tim diperbarui; D01 bertanda tangan PM di lembar pengesahan |
| D04A | v1.0 | Dari draf v0.4 Gracia, diselesaikan PM sesuai review revisi 2 |
| D04B | v1.0 | Isi teknis tetap |
| D05A | v1.0 | Dari draf v0.2 Cecilia, peta navigasi dan 23 kartu layar dengan tangkapan layar build 22 Nov |
| D06 | v1.0 | WN, LPN, DEF-12 s.d. DEF-14; lihat bagian 2 |
| D07A, D07B | v1.0 | Disamakan dengan kode 22 Nov |
| D08A | v1.0 | Baru: demo Android dan lintas platform |
| D09A | v1.0 | Baru: user manual Android |

Review rekan (Steven, R1) **belum diterima** dan ditulis apa adanya pada status tiap dokumen. Hanya tanda tangan PM yang dipasang.

## 2. Temuan penting: DEF-12 (kartu virtual)

Pada varian online, setelah PIN benar layar Kartu menampilkan nomor `0000 0000 0000 xxxx` dan CVV kosong **sejak 11 November**.
SA-13 selama ini lulus keliru karena hanya memeriksa pola 16 digit. Kode diperbaiki (`VirtualCardViewModel.submitPin`),
SA-13 diperketat, dan seluruh SA dijalankan ulang pada APK baru: **25/25 kasus skrip lulus** (23 satu putaran; SA-18 dan SA-20 terpisah).
Dengan LP/LPN berarti 27 dari 28 kasus; SA-08 (kamera) masih dari ponsel fisik 14 Nov.

## 3. Web resmi: aplikasi tim Web (Node/Express/EJS)

- Keputusan PM 22 Nov. Web React disimpan di `apps/web_dummy_buatan_sendiri` sebagai pembanding.
- Peran: Surya (backend Web), Rafi (front end + backend Web), Steven dan Nabila (front end); Salomo full stack Mobile, anggota Mobile lain front end.
- Diuji dengan backend asli: **WN 14/14** (`pengujian/web/node-smoke.mjs`), **LPN 3/3** (`lintas-platform/lp-node.mjs`).
- Belum ada di Web: kantong, kartu virtual, notifikasi, grafik saldo, favorit, QRIS pribadi, paket data.
- Rafi: D04W dan D08 **dipakai** sebagai volume Web (D04W v0.8, D08W v0.3, koreksi fakta oleh PM).
- Nabila: D05W v0.7 direview (`REVIEW_D05W_Nabila_revisi4.md`); tinggal tangkapan layar.

## 4. Sisa pekerjaan

| # | Pekerjaan | PIC |
|---|---|---|
| 1 | Baca ulang volume v1.0, lalu commit dan push ke repo pribadi dan tim | Salomo |
| 2 | Tangkapan layar Web dan gladi; D04W, D05W, D08W ke v1.0 | Rafi, Nabila, Permata |
| 3 | D07W (masih menjelaskan Web React) dan D09W (belum ada) | Steven, Permata |
| 4 | SA-08, berbagi bukti, notifikasi di ponsel fisik pada build terbaru | Salomo |
| 5 | Jangan commit `code_area/apps/web/SE-web.rar` (66 MB, berisi `.env` dan database) | Salomo |
| 6 | DWP v1.4 masih menulis tenggat 24 Nov dan status lama | Salomo |
