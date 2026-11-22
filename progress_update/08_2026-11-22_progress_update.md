# Progress Update #08: Minggu malam, 22 November 2026 (penutup)

Disusun oleh: Salomo (PM). Tenggat: **23 November 2026, 23.59 WIB**

## 1. Yang disamakan dengan kode terbaru

| Butir | Hasil |
|---|---|
| Web | Semua dokumen hanya memakai Web tim (`digitalbanking-node` v2.0.0). Bab, tabel, dan hasil uji Web React dikeluarkan |
| Uji Web | **WN 25/25** (14 lama + 11 baru), **Chrome 157/Edge 158/Firefox 156: 24/24 pemeriksaan** |
| Lintas platform | **LPN 3/3** diulang pada APK final |
| SA-08 | Lulus pada APK final dengan kamera virtual emulator; Android **28/28** |
| Backend | 47/47 diulang; JaCoCo 1.787/1.937 baris (92,26%, ditulis 92,2%) |
| Kinerja | NF-01 Dashboard maks. 197 ms, Android buka dingin 1,74–1,99 dtk; NF-02 p95 7 ms; NF-03 maks. 67 ms |
| COCOMO (D01) | 15,944 KSLOC, 61,78 PM, Rp682.928.211 (semula 16,085 / 62,38 / Rp689.540.065); Gantt diperbarui |
| D05W | v1.0 dari draf Nabila, lengkap dengan tangkapan layar |
| TUT-DDB-002 | v2.0 untuk Web tim |

Bukti: `code_area/pengujian/bukti/2026-11-22/` (`web-node*`, `performa-web-node`, `android-sa08-kamera`, `lintas-platform-node`).

## 2. Yang masih terbuka

| # | Butir |
|---|---|
| 1 | Kamera fisik, notifikasi push, dan berbagi bukti di HP pada APK final (terakhir lulus 14 Nov) |
| 2 | NF-04 (gateway) terblokir karena Docker belum terpasang |
| 3 | Gladi demo D08; tampilan ponsel Web belum diuji |

Fitur yang tidak dimuat di Web (kantong, kartu virtual, notifikasi, grafik saldo, favorit, QRIS pribadi, paket data) mengikuti cakupan tim Web dan tidak ditindaklanjuti (keputusan PM).
`PENJELASAN_TEKNOLOGI_WEB.docx` dan `TUTORIAL_MENJALANKAN_APLIKASI_WEB.docx` ditulis ulang untuk Web tim (`gen/patch_penjelasan_tutorial_web.py`).
