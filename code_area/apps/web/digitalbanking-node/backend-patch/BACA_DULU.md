# Patch backend: saldo awal nasabah baru bisa diatur (mis. Rp0)

Secara bawaan backend memberi nasabah baru saldo awal Rp5.000.000 (`AuthService.NEW_USER_OPENING_BALANCE`).
Patch ini membuat nilainya bisa diganti lewat variabel lingkungan `DDB_OPENING_BALANCE`.
**Tanpa variabel itu perilaku backend tidak berubah (tetap Rp5.000.000)**, jadi Android dan uji backend tidak terpengaruh.

## Pasang (sekali, dari folder utama repo `diponegoro-digital-bank`)
```powershell
git apply backend-patch\opening-balance.patch
```
(atau salin isi perubahan ke `code_area/backend/src/main/java/com/diponegoro/ddb/auth/AuthService.java` secara manual.)
Patch ini mengubah kode backend bersama; beri tahu Salomo dan tim Android sebelum di-commit.

## Pakai
```powershell
cd code_area\backend
$env:DDB_OPENING_BALANCE = "0"
.\run-local.ps1 -H2
```
- Hanya berlaku untuk akun yang **didaftarkan setelah** backend dijalankan dengan nilai ini.
- Akun yang sudah ada (demo, andi, budi, citra, dan akun lama di folder `data`) tidak berubah.
  Untuk mulai bersih: matikan backend, hapus folder `code_area\backend\data`, jalankan ulang.
- Saldo awal Rp0 berarti akun baru harus Top Up dulu (menu Pembayaran → Top Up) sebelum bisa transfer.

Patch diperiksa dengan `patch --dry-run` dan potongan kodenya dikompilasi terpisah dengan `javac`;
backend lengkap belum dikompilasi dan dijalankan (tidak ada Maven/dependensi di lingkungan penyusun).
