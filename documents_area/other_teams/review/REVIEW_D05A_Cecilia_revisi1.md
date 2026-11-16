# Review D05A revisi 1 (v0.2): untuk Cecilia

Dokumen yang direview: `revisi 1/UIM-DDB-001-AND_USER_INTERFACE_MAP_D05A.docx` (v0.2, 16 Nov 2026)
Reviewer: Salomo (PM), 16 Nov 2026
Acuan: review sebelumnya (`REVIEW_D05A_Cecilia.md`) dan kode navigasi Android (`AuthNavGraph.kt`, `AppRoutes.kt`, `home/HomeScreen.kt`, `profile/presentation/ProfileScreens.kt`)

---

## 1. Kesimpulan singkat

**Kemajuan bagus, Cecilia! 👍** Bagian terpenting sudah benar: **23 layar sesuai aplikasi**, alur OTP → e-KYC → Buat PIN sudah benar, FR-26 sudah dihapus, dan daftar isi serta "Last updated by" sudah ada.

Sisa pekerjaan ada **3 hal utama**:
1. **Peta navigasi harus berupa diagram sungguhan**, bukan tabel yang disusun ke bawah.
2. **Beberapa jalur navigasi belum sesuai aplikasi** (daftar ada di bagian 3).
3. **Kartu layar masih ditulis menebak** ("bila tersedia pada build"). Isi dari aplikasi atau tangkapan layar, lalu sisipkan gambarnya.

| Catatan review pertama | Status |
|---|---|
| Inventaris 23 layar dari aplikasi | ✅ Selesai |
| Alur OTP → e-KYC → Buat PIN; login lewat Masukkan PIN; tanpa splash | ✅ Selesai |
| FR-26 dihapus dari Android | ✅ Selesai |
| Daftar isi, Last updated by, baris v1.0 dihapus | ✅ Selesai |
| Acuan DWP v1.3 dan D03 v0.9.3 | ✅ Selesai |
| Diagram navigasi global + 8 modul | ⚠️ Masih tabel bertumpuk, belum diagram |
| Kartu layar berisi elemen nyata + tangkapan layar | ⚠️ Belum |

---

## 2. Peta navigasi harus berupa diagram 🔴

Sekarang Bagian 4 dan 5 berupa kotak tabel yang disusun ke bawah dengan tanda ↓. Masalahnya, tanda ↓ membuat pembaca mengira semua langkah terjadi **berurutan**, padahal tidak. Contohnya: "Buat PIN → Beranda" ↓ "Login berikutnya", atau di Account "Beranda → Grafik" ↓ "Beranda → Kantong".

👉 Gambar ulang sebagai **diagram kotak dan panah** (draw.io, atau alat apa pun yang kamu suka). Contoh yang sudah bagus: **D05W milik Nabila, Gambar 1 dan Gambar 3**. Satu kotak = satu layar (kode + nama + rute), satu panah = satu perpindahan dengan label aksi/kondisi. Buat:
- 1 diagram **navigasi global** (termasuk bottom bar 4 tab)
- 8 diagram **per modul**

---

## 3. Jalur navigasi yang belum sesuai aplikasi 🔴

Aku cocokkan dengan kode. Yang perlu dibetulkan:

| Di dokumen | Di aplikasi sebenarnya | Sumber di kode |
|---|---|---|
| AND-23 Kotak notifikasi dibuka dari **Profil** | Dibuka dari **ikon lonceng di Beranda** (AND-08), ada angka belum dibaca | `home/HomeScreen.kt` |
| Tab QRIS → AND-12 **dan** AND-13 | Tab QRIS membuka **AND-12 Bayar QRIS** saja. AND-13 dibuka dari tombol **Terima** di AND-12, atau dari pintasan di Beranda | `AppRoutes.kt` (Tab), `AuthNavGraph.kt` |
| AND-21 Sesi hanya dari Profil | Bisa dari **Profil** dan dari **Keamanan** (AND-20) | `ProfileScreens.kt`, `AuthNavGraph.kt` |
| "AND-06 Masukkan PIN → bila PIN belum dibuat AND-05; bila e-KYC belum AND-04" | Pemeriksaan ini terjadi **setelah login di AND-01**, sebelum AND-06. Jadi panahnya dari **AND-01**: e-KYC belum → AND-04; PIN belum ada → AND-05; PIN sudah ada → AND-06 | `AuthViewModel.kt` |
| AND-07 Pemulihan hanya dari Login | Juga dari **AND-06** (tautan lupa PIN) | `AuthNavGraph.kt` |

**Jalur yang belum ada di dokumen** (tambahkan):

| Dari | Ke | Aksi |
|---|---|---|
| AND-08 Beranda | AND-18 Analitik | Pintasan "Analitik" atau tombol "Lihat grafik pemasukan & pengeluaran" |
| AND-08 Beranda | AND-16 Riwayat | Tombol "Lihat semua riwayat" |
| AND-08 Beranda | AND-17 Detail transaksi | Ketuk transaksi terakhir |
| AND-11 Transfer | AND-17 Detail transaksi | Buka detail dari bukti transfer |
| AND-23 Kotak notifikasi | AND-17 Detail transaksi | Ketuk notifikasi transaksi |
| AND-12 Bayar QRIS | AND-08 Beranda | Selesai bayar |

Pintasan di Beranda ada **8**: Transfer, Bayar QRIS, Terima QRIS, Top-up, Kantong, Virtual Card, Grafik Saldo, Analitik.

---

## 4. Kartu layar masih menebak 🟡

Banyak kartu masih bertulisan *"bila tersedia pada build"*, *"Cocokkan field yang tampil dengan build"*, atau *"yang tersedia pada build"*. Baris **"Kondisi umum"** juga berisi kalimat yang sama di 23 kartu.

👉 Buka aplikasinya (atau lihat tangkapan layar di bagian 5), lalu tulis apa yang **benar-benar tampil**. Contoh untuk Login:
> **Elemen utama:** tombol "Login dengan Biometrik" (membuka dialog "Autentikasi Biometrik (Simulasi)"), kolom "Username / email / nomor telepon", kolom password, tombol masuk, tautan "Lupa password / PIN?", tautan "Belum punya akun? Daftar".

Untuk "Kondisi umum", tulis yang khas tiap layar. Contoh: AND-06 → *"PIN salah 3 kali: PIN diblokir 5 menit"*; AND-16 → *"Server tidak terjangkau: muncul pesan tidak dapat terhubung"*. Kalau tidak ada yang khas, hapus barisnya.

---

## 5. Tangkapan layar sudah tersedia di GitHub 📸

Di Bagian 1 kamu menulis *"file yang tersedia dalam percakapan tidak menyertakan folder screenshot"*. **Hapus kalimat ini.** Folder tangkapan layar ada di **repo GitHub tim** (`diponegoro-digital-bank`), jadi silakan `git pull` atau unduh ulang ZIP terbaru dari GitHub:
`code_area/pengujian/bukti/2026-11-11/android-sa/`

Usulan pemakaian:

| Kartu | Gambar |
|---|---|
| AND-01 Login | `SA23-2-tombol-biometrik.png`, `SA18-timeout-sesi.png` |
| AND-04 e-KYC | `SA21-1-ekyc-disetujui.png`, `SA22-ekyc-ditolak.png` |
| AND-06 Masukkan PIN | `SA02-pin-diblokir.png` |
| AND-08 Beranda | `SA01-beranda.png`, `SA03-saldo-tersembunyi.png` |
| AND-09 Grafik saldo | `SA27-grafik-saldo.png` |
| AND-10 Kantong | `SA12-1-detail-kantong.png`, `SA12-2-tarik-terkunci.png` |
| AND-11 Transfer | `sa20-transfer.png`, `SA24-favorit.png`, `SA06-1-diproses.png` |
| AND-12 Bayar QRIS | `SA09-qris-statis.png` |
| AND-13 Terima QRIS | `SA25-qris-dinamis.png` |
| AND-14 Top-up | `SA10-pulsa.png`, `SA26-1-tagihan.png` |
| AND-15 Virtual Card | `SA13-1-kartu-baru.png`, `SA13-2-detail.png` |
| AND-16 Riwayat | `SA14-1-filter-keluar.png` |
| AND-17 Detail transaksi | `SA14-2-detail.png` |
| AND-18 Analitik | `SA14-3-analitik.png` |
| AND-19 Profil | `SA15-profil.png` |
| AND-21 Sesi | `DEF11-sesi-offline.png` |
| AND-22 Pengaturan notifikasi | `SA28-pengaturan.png` |

Untuk **AND-02, 03, 05, 07, 20, 23** belum ada gambarnya. Ambil baru bersama **Grace** dari aplikasi (panduan: *TUT Panduan Uji Aplikasi di Ponsel v1.1*; akun demo `demo` / `Demo@1234`, PIN `123456`).

---

## 6. Rapikan dokumen 🟢

| Bagian | Perbaikan |
|---|---|
| Daftar isi | Mencantumkan **9. Checklist** dan **10. Catatan Verifikasi**, padahal bagian itu tidak ada. Hapus dari daftar isi, atau pakai daftar isi otomatis Word |
| Bagian 1 | *"…sebagaimana dirangkum dalam review D05A"*. Ganti menjadi *"berdasarkan `AuthRoutes.kt`, `AppRoutes.kt`, dan `AuthNavGraph.kt`"* |
| Daftar Acuan | Hapus "Review D05A … Salomo" (review bukan acuan). Tambahkan berkas kode navigasi di atas |
| Tabel legenda | Ada satu baris kosong di bawah. Hapus |
| Tabel informasi | Tambahkan baris **Versi: 0.2** (atau versi terbaru) |

---

## 7. Checklist sebelum kirim ke Gracia (R1)

- [ ] Diagram navigasi global + 8 diagram modul (kotak dan panah, bukan tabel)
- [ ] Jalur navigasi diperbaiki dan ditambah sesuai bagian 3
- [ ] Elemen kartu layar diisi dari aplikasi; "Kondisi umum" dibuat khas per layar
- [ ] Tangkapan layar disisipkan (ambil folder `bukti` dari GitHub; sisanya bersama Grace)
- [ ] Hapus kalimat "dalam percakapan" dan rujukan ke "review" sebagai acuan
- [ ] Rapikan daftar isi, legenda, dan tabel informasi; naikkan versi (misalnya v0.3)
- [ ] **Kabari Gracia** bahwa kode layar AND-01 s.d. AND-23 sudah final, supaya bisa dipakai di D04A

Tinggal satu langkah lagi. Semangat! 🙏
