# QRISApp — Android Studio Starter

Project ini dibuat berdasarkan diagram Visual Paradigm yang diberikan:
- Use Case Payment (QRIS)
- Use Case Profile & Notification
- Class Diagram Payment
- Class Diagram Profile & Notification
- Activity Diagram Payment
- Activity Diagram Profile & Notification

## Teknologi
- Kotlin
- Android SDK 35
- XML Layout + ViewBinding
- Material Components
- ZXing untuk generate/scan QR
- Demo repository in-memory (belum memakai server/database produksi)

## Cara menjalankan
1. Buka folder `QRISApp` di Android Studio.
2. Tunggu Gradle Sync selesai.
3. Jalankan pada emulator/device.
4. Login demo:
   - Email: `user@gmail.com`
   - Password: `12345678`
   - PIN pembayaran: `123456`
5. Untuk scan QR, aplikasi akan membuka kamera.
6. Untuk pengujian tanpa backend, tombol "Tampilkan QRIS Pribadi" membuat QR demo.

## Catatan penting
Ini adalah prototype akademik/starter berdasarkan diagram. Payment Gateway di sini masih simulasi.
Untuk pembayaran QRIS sungguhan perlu backend, database, autentikasi aman, OTP provider, dan integrasi resmi payment gateway/QRIS.
Jangan menyimpan PIN/password sebagai plaintext pada aplikasi produksi.
