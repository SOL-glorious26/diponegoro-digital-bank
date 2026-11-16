# Review D05W revisi 2 (v0.6): untuk Nabila

Dokumen yang direview: `revisi 2/USER_INTERFACE_MAP_WEB_v0.6.pdf` (v0.6, 16 Nov 2026)
Reviewer: Salomo (PM), 16 Nov 2026
Acuan: review revisi 1 (`REVIEW_D05W_Nabila_revisi1.md`) dan kode `code_area/apps/web`

---

## 1. Kesimpulan singkat

**Semua catatan revisi 1 sudah selesai. 🎉** Isi dokumen sekarang **sesuai aplikasi Web**. Satu-satunya yang tersisa adalah **menyisipkan tangkapan layar**. Setelah itu dokumen boleh langsung dikirim ke Rafi untuk pre-review R1.

| Catatan revisi 1 | Status |
|---|---|
| Panah "OTP valid" → UI-05 (Gambar 1 dan 3) | ✅ Selesai. Gambar 3 malah ditambah jalur "PIN belum dibuat", bagus |
| Teks 3.1 dan kartu UI-02/UI-03 | ✅ Selesai |
| Elemen e-KYC (NIK, nama sesuai KTP, swafoto, NIK 0000 ditolak) | ✅ Selesai |
| Catatan "[Cocokkan Gambar 2…]" dihapus | ✅ Selesai |
| Endpoint tambahan (balance-visibility, sessions, logout-all, notifikasi, simulate-incoming) | ✅ Selesai |
| Kode dokumen UIM-DDB-001-WEB | ✅ Selesai |
| Daftar isi dengan nomor halaman | ✅ Selesai |
| Tangkapan layar | ⏳ Belum disisipkan |

---

## 2. Yang tersisa

### 🔴 A. Sisipkan tangkapan layar

Di Bagian 5 kamu menulis *"Berkas tangkapan layar yang dirujuk dalam review belum tersedia pada berkas kerja ini"*. Kemungkinan kamu masih memakai salinan repo yang lama. **Folder tangkapan layar sudah ada di repo GitHub tim** (`diponegoro-digital-bank`). Silakan `git pull` atau unduh ulang ZIP dari GitHub, lalu buka:

`code_area/pengujian/bukti/2026-11-11/` (gambar Web ada di subfolder `web-ui/` dan folder sejenis)

Semua nama file yang kamu tulis di kartu (`01-login.png`, `02-beranda.png`, `03-transfer-konfirmasi.png`, `04-pin.png`, `05-bukti-transfer.png`, `06-riwayat.png`, `07-detail.png`, `08-topup-pln.png`, `09-qris.png`, `10-profil.png`, `14-ekyc.png`, `WB13-1-grafik-harian.png`, `WB15-kantong-diubah-dikunci.png`) **ada di folder itu**. Tambahan untuk kartu yang masih kosong:

| Kartu | Gambar |
|---|---|
| UI-02/UI-03 | `15-pengguna-baru.png` |
| UI-13 | `13-bukti-pulsa.png` |
| UI-15 | `LP03-1-web-demo-ewallet.png` |
| UI-18 | `WB11-1-otp.png`, `WB14-1-foto-tersimpan.png` |

Sisanya (UI-04, UI-06, UI-08, UI-19, UI-21, UI-22) diambil bersama **Permata** dengan menjalankan Web (`TUTORIAL_MENJALANKAN_WEB.md`; akun `demo` / `Demo@1234`, PIN `123456`).

Setelah gambar disisipkan, hapus kalimat *"Berkas tangkapan layar … belum tersedia pada berkas kerja ini"* dan semua teks merah placeholder.

### 🟢 B. Kecil

- Kirim juga **berkas .docx v0.6**. Kali ini yang dikirim hanya PDF, dan untuk versi final yang ditandatangani kita butuh .docx-nya.
- Bagian 6.3 no. 2 dan Bagian 5 menyebut "paling lambat 21 Nov". Boleh dihapus, karena tenggat yang berlaku hanya pengumpulan **24 Nov 2026**.

---

## 3. Checklist sebelum kirim ke Rafi (R1)

- [ ] Ambil folder `bukti` dari GitHub dan sisipkan tangkapan layar yang sudah ada
- [ ] Ambil gambar sisanya bersama Permata
- [ ] Hapus kalimat "belum tersedia pada berkas kerja ini" dan teks merah placeholder
- [ ] Naikkan versi (misalnya v0.7) dan catat di Revision Control
- [ ] Kirim .docx + PDF

Kerja yang sangat rapi, Nabila. Terima kasih! 🙏
