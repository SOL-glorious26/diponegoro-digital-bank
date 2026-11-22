const S = require("./ddbstyle");
const { P, H1, H2, H3, B, NL, CAP, NOTE, table } = S;
const [, , FIG, OUT, LOGO, SIGN] = process.argv;

// Hasil eksekusi test otomatis (diisi dari laporan Surefire / Gradle pada tanggal eksekusi)
const RUN = require("./doc6_results.json");

const meta = {
  docLabel: "Dokumen 6", title: "Pengujian Sistem", subtitle: "System Testing Android, Web, Backend/API, dan Lintas Platform",
  code: "STD-DDB-001", version: "1.0", statusShort: "Final",
  status: "Final v1.0 – diterbitkan Project Manager 22 November 2026; review PIC Dokumen 6 (Steven Wibisana) belum diterima sampai tenggat",
  module: "Pengujian Sistem Diponegoro Digital Bank (8 modul bisnis, Financial Transaction Core, klien Android dan Web)",
  createdBy: "Salomo Putra Glorious Hokoyoku (Project Manager) – draf awal untuk PIC Dokumen 6",
  updatedBy: "Salomo Putra Glorious Hokoyoku (Project Manager)",
  team: "Kelompok Jaran Goyang – PIC: Steven Wibisana; eksekusi Android: Gracia, Grace; eksekusi Web: Permata; data uji dan backend: Salomo",
  refs: "SRS-DDB-001 v0.1; PRJ-DDB-001 v1.0 Bab III; SAD-DDB-001 v1.0; FSD-DDB-001 v1.0; SDD-DDB-001-BE v1.0; UIM-DDB-001-AND v1.0; TUT-DDB-001-AND v1.1; User Interface Map (Web) v0.3 (WB) dan v0.7 (WN)",
  date: "22 November 2026", logo: LOGO,
  revisions: [
    ["0.9", "11 Nov 2026", "Salomo P.G. Hokoyoku", "Draf awal: strategi, lingkungan, matriks keterlacakan FR-01–FR-29, kasus uji Backend/API, Android, Web, lintas platform, dan non-fungsional; hasil aktual test otomatis; log defect dari pengembangan."],
    ["0.9.1", "11 Nov 2026", "Salomo P.G. Hokoyoku", "Aplikasi Web terhubung ke backend: kasus uji Web dieksekusi dengan uji UI otomatis (Chrome headless) dan uji kontrak API; status WB dan LP diperbarui; DEF-04 diperbarui."],
    ["0.9.2", "11 Nov 2026", "Salomo P.G. Hokoyoku", "Uji lintas platform LP-01 sampai LP-04 lulus (emulator varian online + Chrome headless); WB-09 lulus; DEF-05 dan DEF-06 ditemukan, diperbaiki, dan diuji ulang."],
    ["0.9.3", "12 Nov 2026", "Salomo P.G. Hokoyoku", "Uji sistem Android SA-01–SA-28 dijalankan di emulator (27 lulus, SA-08 menunggu ponsel fisik); WB-11 dan WB-12 lulus; NF-01–NF-03, NF-08 (JaCoCo 92,2%), dan NF-09 (Chrome/Edge/Firefox) lulus; DEF-07 sampai DEF-11 dicatat dan diperbaiki; skrip uji dipindah ke folder pengujian/."],
    ["0.9.4", "12 Nov 2026", "Salomo P.G. Hokoyoku", "Kasus uji Web WB-13 (grafik saldo, FR-07), WB-14 (foto profil, FR-27), WB-15 (ubah dan kunci kantong, FR-08/09) ditambahkan dan lulus; DEF-11 ditutup penuh (layar Sesi Android); regresi Web, peramban, dan lintas platform lulus."],
    ["0.9.5", "12 Nov 2026", "Salomo P.G. Hokoyoku", "DEF-04 ditutup: CORS dikonfigurasi di backend dan gateway; BE-46 dan BE-47 (47 test backend lulus); APK rilis bertanda tangan diuji ulang (SA dan LP lulus); seluruh defect ditutup."],
    ["0.9.6", "14 Nov 2026", "Salomo P.G. Hokoyoku", "Uji di ponsel fisik Realme RMX3867 (Android 16): SA-08 (pindai QRIS dengan kamera) lulus sehingga uji sistem Android 28/28; NF-07 lulus; notifikasi push dan berbagi bukti juga lulus di ponsel fisik; lingkungan ponsel fisik dan kriteria keluar diperbarui."],
    ["0.9.7", "21 Nov 2026", "Salomo P.G. Hokoyoku", "Regresi setelah pembaruan tampilan Android (tema baru, layar Login, PIN dengan keypad, Beranda, bottom navigation, komponen bersama; ViewModel, repository, API, dan backend tidak berubah): test otomatis Android varian demo 63/64 dan varian online 57/58 lulus (masing-masing 1 dilewati); DarkModeShotsTest ditambahkan; skrip uji sistem dan lintas platform disesuaikan; eksekusi ulang SA dan LP pada build baru dicatat sebagai tindak lanjut."],
    ["0.9.8", "22 Nov 2026", "Salomo P.G. Hokoyoku", "Eksekusi ulang pada build Android dengan tampilan baru di emulator: 25 kasus SA otomatis dan LP-01 sampai LP-04 lulus (SA-04 dan SA-19 tercakup LP-01 dan LP-04); SA-08 pada build baru menunggu ponsel fisik. Perbaikan tampilan sebelum eksekusi: layar PIN saat ponsel miring, warna label status mode gelap, dan layar Kartu. Skrip SA dan LP diperbaiki; pemeriksaan LP-04 diperketat."],
    ["1.0", "22 Nov 2026", "Salomo P.G. Hokoyoku", "Versi final. Web resmi ditetapkan Web tim (Node/Express/EJS): kasus WN-01–WN-14 dan LPN-01–LPN-03 ditambahkan dan lulus; WB dan LP dinyatakan sebagai hasil aplikasi Web React. DEF-12 (nomor dan CVV kartu virtual varian online tidak tampil; SA-13 sebelumnya lulus keliru), DEF-13, dan DEF-14 dicatat, diperbaiki, dan diuji ulang; SA dijalankan ulang pada APK final. Disetujui PM."],
  ],
  approval: { sign: SIGN, date: "22 Nov 2026", pic: [["PIC Dokumen 6", "Steven Wibisana"]] },
};

const ST = { L: "Lulus", G: "Gagal", T: "Terblokir", N: "Belum dieksekusi" };

// ---------------- Kasus uji Backend/API (otomatis) ----------------
const BE = [
  ["AccountFlowTest", "rekeningDibukaSetelahEkycDenganSaldoAwal", "Rekening dibuka setelah e-KYC disetujui dengan saldo awal", "FR-02, FR-06"],
  ["AccountFlowTest", "rekeningBelumAdaSebelumEkyc", "Rekening belum tersedia sebelum e-KYC", "FR-02"],
  ["AccountFlowTest", "sembunyikanSaldoTersimpan", "Pilihan sembunyikan saldo tersimpan di server", "FR-06"],
  ["AccountFlowTest", "kantongIsiKunciTarikDiniDenganPin", "Kantong: isi, kunci, tarik dini dengan PIN", "FR-08, FR-09, BR-06"],
  ["AccountFlowTest", "kunciTargetTerbukaOtomatis…", "Kunci target terbuka otomatis; hapus kantong mengembalikan saldo", "FR-09"],
  ["AccountFlowTest", "kunciTanggalTerbukaSetelahTanggalLewat", "Kunci tanggal terbuka setelah tanggal lewat", "FR-09"],
  ["AccountFlowTest", "saldoTidakCukupTidakMengubahApaPun", "Saldo tidak cukup tidak mengubah data apa pun", "BR-03"],
  ["AccountFlowTest", "grafikSaldoKonsistenDenganLedger", "Grafik saldo konsisten dengan ledger", "FR-07"],
  ["AccountFlowTest", "kantongOrangLainTidakBisaDiakses", "Kantong milik pengguna lain tidak dapat diakses", "NFR-06"],
  ["AuthFlowTest", "verifikasiPinMandiriMemakaiAturanBlokirYangSama", "Verifikasi PIN setelah login memakai aturan blokir yang sama", "FR-04, BR-02"],
  ["AuthFlowTest", "registrasiOtpEkycPinSampaiFinancialEnabled", "Registrasi → OTP → e-KYC → PIN sampai transaksi aktif", "FR-01, FR-02, FR-04"],
  ["AuthFlowTest", "ekycDenganNikBerakhiran0000Ditolak", "e-KYC dengan NIK berakhiran 0000 ditolak", "FR-02"],
  ["AuthFlowTest", "passwordSalahPesanUmumDanDikunciSetelah5Kali", "Password salah: pesan umum, dikunci setelah 5 kali", "FR-03"],
  ["AuthFlowTest", "logoutSemuaPerangkatMembatalkanSemuaToken", "Logout semua perangkat membatalkan seluruh token", "FR-05"],
  ["AuthFlowTest", "sesiBerakhirSetelah5Menit…", "Sesi berakhir setelah 5 menit tidak aktif; refresh token dirotasi", "FR-05"],
  ["AuthFlowTest", "pemulihanPasswordDenganOtp", "Pemulihan password dengan OTP", "FR-05"],
  ["AuthFlowTest", "loginBiometrikDenganTokenPerangkat", "Login biometrik dengan token perangkat", "FR-03"],
  ["AuthFlowTest", "endpointTerlindungTanpaTokenDitolak…", "Endpoint terlindung tanpa token ditolak dengan format JSON seragam", "NFR-06"],
  ["DemoDataTest", "dataDemoKonsistenDanBisaDipakai", "Data demo konsisten (jurnal seimbang) dan dapat dipakai login", "Data uji"],
  ["HistoryProfileNotificationTest", "riwayatFilterKombinasiPencarianDanPaginasi", "Riwayat: kombinasi filter, pencarian, paginasi", "FR-23"],
  ["HistoryProfileNotificationTest", "detailMemuatJurnalSeimbang…", "Detail memuat jurnal seimbang; tidak dapat dilihat pengguna lain", "FR-24, NFR-06"],
  ["HistoryProfileNotificationTest", "ringkasanBulananDanEksporCsvPdf", "Ringkasan bulanan dan ekspor CSV/PDF", "FR-25, FR-26"],
  ["HistoryProfileNotificationTest", "notifikasiTransaksiMasukDanKeluarAsinkron", "Notifikasi transaksi masuk dan keluar (asinkron)", "FR-29"],
  ["HistoryProfileNotificationTest", "transferDitolakMengirimSatuNotifikasiGagal", "Transfer ditolak mengirim tepat satu notifikasi gagal", "FR-29"],
  ["HistoryProfileNotificationTest", "pengaturanKanalEmail…", "Pengaturan kanal notifikasi; kanal wajib harus dipilih", "FR-28"],
  ["HistoryProfileNotificationTest", "ubahProfilDenganOtpDanUbahPasswordPin", "Ubah profil dengan OTP; ubah password dan PIN", "FR-27, FR-28"],
  ["PaymentTopUpCardFlowTest", "codecRoundTripDanCrcRusakDitolak", "Encode/decode QRIS EMVCo; CRC rusak ditolak", "FR-15"],
  ["PaymentTopUpCardFlowTest", "bayarQrisStatisDanBatasNominal", "Bayar QRIS statis dan batas nominal", "FR-15"],
  ["PaymentTopUpCardFlowTest", "qrisDinamisMemakaiNominalDariKode", "QRIS dinamis memakai nominal dari kode", "FR-17"],
  ["PaymentTopUpCardFlowTest", "qrisPribadiSesamaBank…", "QRIS pribadi sesama bank mengkredit penerima; QR sendiri ditolak", "FR-16"],
  ["PaymentTopUpCardFlowTest", "simulasiDanaMasukQris", "Simulasi dana masuk melalui QRIS", "FR-16"],
  ["PaymentTopUpCardFlowTest", "tokenPlnDanIdTidakDitemukan", "Token PLN dan ID pelanggan tidak ditemukan", "FR-19"],
  ["PaymentTopUpCardFlowTest", "pulsaDanNominalTidakValid", "Pulsa dan nominal tidak valid", "FR-18"],
  ["PaymentTopUpCardFlowTest", "tagihanTidakBisaDibayarDuaKali", "Tagihan tidak dapat dibayar dua kali", "FR-20"],
  ["PaymentTopUpCardFlowTest", "kartuVirtualDetailBerPin…", "Virtual card: detail ber-PIN, bekukan, limit, cakupan", "FR-21, FR-22"],
  ["TransferFlowTest", "transferSesamaBankAtomikDanKeduaSaldoBerubah", "Transfer sesama bank atomik; kedua saldo berubah", "FR-10, BR-03"],
  ["TransferFlowTest", "validasiInputA2A3DanRekeningSendiri", "Validasi input (A2, A3) dan rekening sendiri", "FR-10, FR-12"],
  ["TransferFlowTest", "saldoTidakCukupA1", "Saldo tidak cukup (A1)", "FR-10"],
  ["TransferFlowTest", "pinSalahA4TidakMendebitDanBlokirSetelah3Kali", "PIN salah (A4) tidak mendebit; blokir setelah 3 kali", "FR-04, BR-02"],
  ["TransferFlowTest", "antarbankDenganBiaya", "Transfer antarbank dengan biaya", "FR-11"],
  ["TransferFlowTest", "ditolakJaringanA6DanaDikembalikan", "Ditolak jaringan (A6), dana dikembalikan otomatis", "FR-11, BR-05"],
  ["TransferFlowTest", "timeoutA5DiprosesLaluCekUlangBerhasil", "Timeout (A5) berstatus diproses, lalu cek ulang berhasil", "FR-11"],
  ["TransferFlowTest", "favoritTambahHapusDanTerlihatSaatInquiry", "Favorit: tambah, hapus, terlihat saat inquiry", "FR-12, FR-13"],
  ["TransferFlowTest", "transferParalelTidakMenembusSaldo…", "Transfer paralel tidak menembus saldo; kunci idempotensi sama hanya sekali debit", "BR-04, NFR-07"],
  ["TransferFlowTest", "belumEkycTidakBisaTransfer", "Pengguna belum e-KYC tidak dapat transfer", "FR-02"],
  ["CorsTest", "preflightDariOriginWebDiizinkanDanOriginLainDitolak", "CORS: preflight dari origin Web diizinkan; origin lain ditolak", "NFR-06, DEF-04"],
  ["CorsTest", "responsBiasaMemuatHeaderCors…", "CORS: header pada respons; klien tanpa Origin (Android) tidak terpengaruh", "NFR-06, DEF-04"],
];

// ---------------- Test otomatis Android ----------------
const AND = [
  ["LedgerTest", "6", "Jurnal seimbang, posting tidak seimbang ditolak, atomisitas, idempotensi, refund, saldo awal", "BR-03, BR-04, BR-05"],
  ["AccountRepositoryTest", "6", "Sembunyikan saldo, grafik saldo, siklus kantong, kunci tanggal/target, validasi input", "FR-06–FR-09"],
  ["TransferRepositoryTest", "9", "Validasi rekening, aturan nominal, sesama bank, antarbank, A5, A6, PIN salah, e-KYC, favorit", "FR-04, FR-10–FR-13"],
  ["PaymentRepositoryTest", "6", "Codec QRIS + CRC, QR dirusak, QRIS dinamis, QRIS statis + PIN salah, QRIS pribadi", "FR-15–FR-17"],
  ["TopUpRepositoryTest", "6", "Deteksi operator, pulsa + biaya, paket data, token PLN, tagihan sekali per periode, e-wallet", "FR-18–FR-20"],
  ["VirtualCardUseCasesTest", "8", "Buat kartu, e-KYC wajib, detail dengan PIN, akses pengguna lain, bekukan, limit, cakupan domestik", "FR-21, FR-22"],
  ["HistoryAndProfileTest", "7", "Urutan dan paginasi riwayat, filter + pencarian, ringkasan bulanan, detail jurnal, profil dengan OTP, ubah PIN/password, notifikasi", "FR-23–FR-25, FR-27–FR-29"],
  ["ApiClientTest (MockWebServer)", "8", "Bearer token, refresh sekali saat 401, sesi berakhir, pesan error, offline, Idempotency-Key, format waktu", "FR-04, FR-05, NFR-05"],
  ["AppTourTest + DarkModeShotsTest + ReceiptAndDialogShotsTest (Robolectric, varian demo)", "6", "Tur seluruh layar, mode gelap (login, PIN, Beranda), dialog PIN, bukti transfer berhasil/ditolak, struk PLN", "FR-14, NFR-08"],
  ["LiveBackendTest (varian online)", "1", "Nasabah baru dari registrasi sampai transaksi di backend sungguhan (dilewati pada varian demo)", "FR-01–FR-04, FR-10, FR-15, FR-18, FR-21, FR-29"],
  ["ExampleUnitTest", "1", "Test bawaan templat proyek", "–"],
];

// ---------------- Kasus uji sistem Android (manual) ----------------
// [ID, judul, prasyarat/data, langkah, hasil harapan, varian, FR, status, catatan]
const SA = [
  ["SA-01", "Login dan PIN", "Akun demo", "Login demo / Demo@1234 → PIN 123456", "Masuk Beranda; saldo dan rekening 1400123456 tampil", "D/O", "FR-03, FR-04, FR-06", ST.L, "Emulator, varian online (uji otomatis)"],
  ["SA-02", "PIN salah tiga kali", "Akun baru (agar akun demo tidak terblokir)", "Logout → login → PIN salah 3 kali", "PIN diblokir sementara dengan pesan sisa waktu", "D/O", "FR-04, BR-02", ST.L, "Akun baru: Sisa percobaan 2 → 1 → diblokir"],
  ["SA-03", "Sembunyikan saldo", "Login", "Ketuk ikon mata di Beranda; tutup dan buka aplikasi", "Saldo tetap tersembunyi", "D/O", "FR-06", ST.L, "Tersimpan di server; tetap tersembunyi setelah aplikasi ditutup paksa dan masuk lagi"],
  ["SA-04", "Transfer sesama bank", "Login; saldo cukup", "Transfer → Diponegoro Digital Bank → 1400000002 → Rp10.000 → PIN", "Nama ANDI PRATAMA tampil sebelum PIN; berhasil; saldo berkurang; bukti dapat dibagikan", "D/O", "FR-10, FR-12, FR-14", ST.L, "Emulator; juga LP-01 (dana terlihat di Web penerima); berbagi bukti ke WhatsApp lulus di ponsel fisik (14 Nov 2026)"],
  ["SA-05", "Transfer antarbank ditolak", "Login", "Bank BRI, rekening 9999999999", "Gagal; dana dikembalikan; tercatat di riwayat", "D/O", "FR-11, BR-05", ST.L, "Status Gagal; saldo tidak berubah"],
  ["SA-06", "Transfer antarbank timeout", "Login", "Bank Mandiri, rekening 8888888888 → Cek status", "Status diproses, lalu final setelah cek status; tidak ada debit ganda", "D/O", "FR-11, BR-04", ST.L, "Diproses → Berhasil setelah \"Periksa ulang status\"; debit tepat sekali Rp12.500"],
  ["SA-07", "Saldo tidak cukup", "Login", "Transfer nominal melebihi saldo", "Ditolak sebelum PIN dengan pesan saldo tidak mencukupi", "D/O", "FR-10", ST.L, "Pesan saldo tidak mencukupi muncul sebelum PIN"],
  ["SA-08", "Bayar QRIS dengan kamera", "Dua perangkat; varian online", "Perangkat 2: Terima QRIS. Ponsel: Bayar QRIS → pindai", "Penerima dan nominal tampil; setelah PIN, saldo kedua akun berubah", "O", "FR-15, FR-16", ST.L, "Ponsel fisik Realme RMX3867 (Android 16), 14 Nov 2026: QR dari layar Terima QRIS perangkat kedua terpindai kamera; penerima dan nominal tampil; saldo kedua akun berubah"],
  ["SA-09", "Bayar QRIS contoh", "Login", "Bayar QRIS → pilih QR contoh → PIN", "Pembayaran berhasil", "D/O", "FR-15", ST.L, "QR contoh Warung Bu Sri"],
  ["SA-10", "Top-up pulsa", "Login", "Top-up → Pulsa → nomor 0812… → nominal → PIN", "Berhasil; tercatat di riwayat", "D/O", "FR-18", ST.L, "Pulsa Telkomsel Rp10.000, total Rp11.500"],
  ["SA-11", "Token PLN tidak ditemukan", "Login", "Top-up → PLN → ID pelanggan berakhiran 0000", "Pesan pelanggan tidak ditemukan", "D/O", "FR-19", ST.L, "Pesan \"ID pelanggan tidak ditemukan\""],
  ["SA-12", "Kantong terkunci", "Login", "Kantong → buat → isi Rp20.000 → kunci sampai tanggal", "Saldo utama berkurang; penarikan ditolak selama terkunci kecuali penarikan dini dengan PIN", "D/O", "FR-08, FR-09, BR-06", ST.L, "Saldo utama −Rp20.000; penarikan saat terkunci meminta konfirmasi dan PIN"],
  ["SA-13", "Virtual card", "Login; e-KYC disetujui", "Kartu → buat → detail dengan PIN → bekukan", "Nomor lengkap dan CVV hanya tampil setelah PIN; status beku", "D/O", "FR-21, FR-22", ST.L, "Nomor lengkap dan CVV asli tampil setelah PIN (setelah perbaikan DEF-12); status Dibekukan"],
  ["SA-14", "Riwayat dan analitik", "Ada transaksi uji", "Riwayat → filter Keluar → detail; buka Analitik", "Transaksi uji muncul; grafik bulan berjalan berubah", "D/O", "FR-23, FR-24, FR-25", ST.L, "Filter Keluar, detail dengan ID transaksi, analitik"],
  ["SA-15", "Ubah profil dengan OTP", "Login", "Profil → ubah email → masukkan OTP", "Perubahan aktif setelah OTP; notifikasi keamanan muncul", "D/O", "FR-27", ST.L, "Akun baru; email baru aktif setelah OTP"],
  ["SA-16", "Notifikasi push", "Dua perangkat; varian online", "Perangkat 2 (andi) transfer ke 1400123456", "Notifikasi dana masuk muncul ≤ 15 detik", "O", "FR-29", ST.L, "Notifikasi muncul ±6–13 detik setelah transfer masuk (emulator); juga muncul di ponsel fisik Realme RMX3867 (14 Nov 2026)"],
  ["SA-17", "Konsistensi dua perangkat", "Dua perangkat; varian online", "Ponsel (demo) transfer ke andi; cek saldo andi di perangkat 2", "Saldo kedua akun konsisten; total tidak berubah", "O", "BR-03, NFR-08", ST.L, "Perangkat kedua disimulasikan lewat API (akun andi); total saldo tetap"],
  ["SA-18", "Timeout sesi", "Login", "Diamkan aplikasi > 5 menit, lalu lakukan aksi", "Diminta login ulang", "D/O", "FR-05", ST.L, "Diam 6 menit (layar menyala maupun mati): kembali ke login dengan pesan sesi berakhir"],
  ["SA-19", "Logout semua perangkat", "Dua perangkat; varian online", "Profil → Sesi aktif → keluar dari semua perangkat", "Perangkat lain kembali ke login pada aksi berikutnya", "O", "FR-05", ST.L, "LP-04: perangkat lain berupa Web; sesi Web berakhir"],
  ["SA-20", "Server tidak terjangkau", "Varian online", "Hentikan backend, lalu muat ulang Beranda", "Pesan \"Tidak dapat terhubung ke server\"; aplikasi tidak tertutup paksa", "O", "NFR-05", ST.L, "Pesan tidak dapat terhubung di Transfer dan Riwayat (setelah perbaikan DEF-11); aplikasi tidak tertutup"],
  ["SA-21", "Registrasi sampai transaksi aktif", "Varian online; username baru", "Buka rekening → OTP → e-KYC dengan NIK valid → buat PIN", "e-KYC disetujui; rekening dan saldo awal tersedia; menu transaksi aktif", "D/O", "FR-01, FR-02, FR-04", ST.L, "Termasuk tombol Kembali setelah OTP (DEF-07) dan login ulang sebelum e-KYC (DEF-08)"],
  ["SA-22", "e-KYC ditolak", "Username baru", "Buka rekening → OTP → e-KYC dengan NIK berakhiran 0000", "e-KYC ditolak; menu transaksi tetap terkunci", "D/O", "FR-02", ST.L, "Pesan \"e-KYC ditolak. Transaksi finansial tetap dinonaktifkan.\""],
  ["SA-23", "Login biometrik", "Ponsel dengan sidik jari/wajah terdaftar", "Aktifkan biometrik di Profil → logout → login dengan biometrik", "Login berhasil tanpa password", "D/O", "FR-03", ST.L, "Biometrik masih simulasi (dialog \"Autentikasi Biometrik (Simulasi)\"), belum memakai sensor/BiometricPrompt"],
  ["SA-24", "Penerima favorit", "Login", "Simpan penerima sebagai favorit → transfer baru → pilih favorit → hapus", "Favorit dapat dipilih dan dihapus", "D/O", "FR-13", ST.L, "Favorit tersimpan, dipakai, lalu dihapus"],
  ["SA-25", "QRIS dinamis", "Dua perangkat atau QR contoh dinamis", "Buat QRIS dengan nominal → bayar", "Nominal tidak dapat diubah pembayar", "D/O", "FR-17", ST.L, "QR contoh Kopi Senja Rp28.000; nominal terkunci"],
  ["SA-26", "Bayar tagihan", "Login", "Top-up → Tagihan (PDAM/BPJS/Internet) → nomor pelanggan → bayar → ulangi", "Pembayaran pertama berhasil; pembayaran kedua pada periode sama ditolak", "D/O", "FR-20", ST.L, "Pembayaran kedua ditolak: \"sudah lunas\""],
  ["SA-27", "Grafik saldo", "Ada transaksi", "Beranda → grafik saldo → ganti periode", "Titik grafik sesuai perubahan saldo pada riwayat", "D/O", "FR-07", ST.L, "Titik terakhir grafik sama dengan saldo backend"],
  ["SA-28", "Pengaturan keamanan dan notifikasi", "Login", "Profil → Pengaturan → matikan satu jenis notifikasi → lakukan transaksi", "Notifikasi jenis tersebut tidak dikirim", "D/O", "FR-28", ST.L, "Setelan dipulihkan setelah uji"],
];

// ---------------- Kasus uji Web ----------------
const WB = [
  ["WB-01", "Login (UI-01)", "Login dengan username/email + password, tanpa OTP", "Masuk ke Dashboard; token tersimpan sesuai SAD Bab VIII", "FR-03", ST.L, "Chrome headless, uji UI otomatis 11 Nov 2026"],
  ["WB-02", "Registrasi, OTP, e-KYC", "Daftar → OTP → e-KYC → PIN", "e-KYC disetujui; transaksi aktif", "FR-01, FR-02, FR-04", ST.L, "Chrome headless, uji UI otomatis 11 Nov 2026; pengguna baru"],
  ["WB-03", "Route guard e-KYC", "Akun belum e-KYC membuka halaman transfer lewat URL", "Diarahkan ke halaman e-KYC", "FR-02", ST.L, "Chrome headless, uji UI otomatis 11 Nov 2026"],
  ["WB-04", "Dashboard saldo (UI-07)", "Buka Dashboard; sembunyikan saldo; muat ulang", "Saldo sama dengan Android; pilihan sembunyikan tersimpan", "FR-06, NFR-08", ST.L, "Chrome headless, uji UI otomatis 11 Nov 2026"],
  ["WB-05", "Transfer sesama bank (UI-08–UI-11)", "Rekening 1400000002 → Rp10.000 → PIN", "Nama penerima tampil; berhasil; header Idempotency-Key terkirim", "FR-04, FR-10, FR-12", ST.L, "Chrome headless, uji UI otomatis 11 Nov 2026; nama ANDI PRATAMA tampil sebelum PIN"],
  ["WB-06", "Transfer antarbank ditolak", "BRI 9999999999", "Gagal; dana dikembalikan; tercatat di riwayat", "FR-11, BR-05", ST.L, "Chrome headless, uji UI otomatis 11 Nov 2026"],
  ["WB-07", "Riwayat dan detail (UI-16)", "Filter jenis/tanggal, cari kata kunci, buka detail", "Hasil sesuai filter; detail sesuai ledger", "FR-23, FR-24", ST.L, "Chrome headless, uji UI otomatis 11 Nov 2026"],
  ["WB-08", "Ekspor riwayat", "Ekspor CSV dan PDF dengan filter aktif", "Berkas terunduh dan isinya sesuai filter", "FR-26", ST.L, "Chrome headless, uji UI otomatis 11 Nov 2026; berkas .csv dan .pdf terunduh"],
  ["WB-09", "QRIS pribadi", "Buka halaman QRIS pribadi", "Kode QRIS pribadi tampil dan dapat dipindai Android", "FR-16", ST.L, "QR Web dipindai Android dari gambar galeri (LP-02); pemindaian kamera tetap diuji di SA-08"],
  ["WB-10", "Top-up pulsa dan PLN", "Pulsa 0812…; PLN ID berakhiran 0000", "Pulsa berhasil; PLN menampilkan pelanggan tidak ditemukan", "FR-18, FR-19", ST.L, "Chrome headless, uji UI otomatis 11 Nov 2026; pulsa berhasil, PLN 0000 ditolak"],
  ["WB-11", "Profil dengan OTP", "Ubah email → OTP", "Perubahan aktif setelah OTP", "FR-27", ST.L, "Chrome headless; menemukan dan memverifikasi perbaikan DEF-09"],
  ["WB-12", "Sesi dan token", "Biarkan access token kedaluwarsa, lalu lakukan aksi", "Token diperbarui sekali; bila gagal diarahkan ke login", "FR-05", ST.L, "Access token 1 menit: diperbarui otomatis; refresh dicabut → halaman masuk dengan pesan"],
  ["WB-13", "Grafik saldo (Beranda)", "Pilih periode Harian, Mingguan, Bulanan; tampilkan sebagai tabel", "Grafik tampil per periode; titik terakhir sama dengan saldo rekening", "FR-07", ST.L, "Chrome headless; 14/8/6 titik; titik terakhir = saldo"],
  ["WB-14", "Foto profil", "Masukkan URL tidak valid, lalu URL gambar valid; muat ulang; hapus foto", "URL tidak valid ditolak; foto tampil di profil dan bilah atas; dapat dihapus", "FR-27", ST.L, "Pesan \"URL foto tidak valid\"; foto tetap setelah muat ulang"],
  ["WB-15", "Ubah dan kunci kantong", "Buat kantong → Ubah nama/tujuan → Kunci sampai tanggal", "Perubahan tersimpan; status Terkunci dengan tanggal; tombol Kunci hilang", "FR-08, FR-09", ST.L, "Kantong \"Laptop Kuliah\" terkunci 7 hari"],
];

// ---------------- Lintas platform ----------------
const LP = [
  ["LP-01", "Transfer di Android tampil di Web", "Transfer dari Android (akun demo) ke andi; buka Web dengan akun andi", "Saldo dan riwayat andi di Web sesuai ledger", "FR-10, FR-23, NFR-08", ST.L, "Emulator (varian online) + Chrome headless, 11 Nov 2026; saldo andi di Web tepat bertambah sebesar nominal; ID transaksi sama"],
  ["LP-02", "QRIS Web dibayar Android", "Web (andi) menampilkan QRIS dinamis; Android (demo) memindai gambarnya dan membayar dengan PIN", "Saldo penerima di Web bertambah sesuai nominal", "FR-15, FR-16, FR-17", ST.L, "QR dinamis dirender Web, gambar dipindai Android dari galeri; nominal terkunci; saldo andi di Web sesuai"],
  ["LP-03", "Top-up di Web tampil di Android", "Top-up (e-wallet) di Web; buka riwayat Android akun yang sama", "Transaksi muncul di Android dengan nominal dan status sama", "FR-18, FR-23", ST.L, "E-wallet dibayar di Web; transaksi dan total sama muncul teratas di riwayat Android"],
  ["LP-04", "Logout semua perangkat lintas platform", "Login di Web dan Android; logout semua perangkat dari Android", "Sesi Web berakhir pada aksi berikutnya", "FR-05", ST.L, "Sesi Web terlihat di daftar sesi Android; setelah logout semua perangkat, Web kembali ke login dengan pesan sesi berakhir"],
];

// ---------------- Web tim (Node) ----------------
const WN = [
  ["WN-01", "Login salah dan benar", "Login salah ditolak; login benar masuk Dashboard; saldo sama dengan backend", "FR-03, FR-06", ST.L],
  ["WN-02", "Halaman terlindungi tanpa login", "Diarahkan ke halaman masuk", "FR-05", ST.L],
  ["WN-03", "Transfer sesama bank", "PIN diminta; saldo kedua akun berubah tepat", "FR-10, FR-12", ST.L],
  ["WN-04", "Nominal kecil dan PIN salah", "Ditolak; saldo tidak berubah", "BR-01, BR-02", ST.L],
  ["WN-05", "Transfer antarbank ditolak", "Status Gagal; dana kembali", "FR-11, BR-05", ST.L],
  ["WN-06", "Transfer antarbank diproses", "Final lewat Cek status; debit tepat sekali", "FR-11, BR-04", ST.L],
  ["WN-07", "Pulsa", "Berhasil dengan PIN; debit Rp11.500", "FR-18", ST.L],
  ["WN-08", "Token PLN", "Berhasil dengan PIN", "FR-19", ST.L],
  ["WN-09", "E-wallet", "Berhasil dengan PIN", "FR-18", ST.L],
  ["WN-10", "Tagihan dua kali", "Pembayaran kedua pada periode sama ditolak", "FR-20", ST.L],
  ["WN-11", "QRIS contoh", "Merchant tampil; berhasil dengan PIN", "FR-15", ST.L],
  ["WN-12", "Riwayat dan ekspor", "Riwayat tampil; CSV dan PDF terunduh", "FR-23, FR-26", ST.L],
  ["WN-13", "Registrasi sampai Dashboard", "OTP, e-KYC, PIN, lalu Dashboard", "FR-01, FR-02, FR-04", ST.L],
  ["WN-14", "Logout semua perangkat", "Sesi Web berakhir dengan pesan", "FR-05", ST.L],
];
const LPN = [
  ["LPN-01", "Transfer Android tampil di Web tim", "Saldo dan riwayat andi di Web bertambah sesuai nominal", "FR-10, FR-23, NFR-08", ST.L],
  ["LPN-02", "E-wallet di Web tim tampil di Android", "Transaksi dengan total sama muncul di riwayat Android", "FR-18, FR-23", ST.L],
  ["LPN-03", "Logout semua perangkat dari Android", "Sesi Web terlihat di Android; Web kembali ke login dengan pesan sesi berakhir", "FR-05", ST.L],
];

// ---------------- Non-fungsional ----------------
const NF = [
  ["NF-01", "NFR-01", "Waktu muat layar utama ≤ 2 detik", "Stopwatch/Logcat pada emulator dan ponsel; DevTools pada Web", ST.L, "Web: muat Beranda maks. 956 ms (Chrome headless, cache mati); Android: buka dingin 1,33–1,37 detik (emulator)"],
  ["NF-02", "NFR-02", "Respons API baca p95 ≤ 500 ms", "100 permintaan GET saldo/riwayat ke backend lokal, hitung p95", ST.L, "Gabungan 300 permintaan: p95 13 ms, maks. 36 ms (backend lokal)"],
  ["NF-03", "NFR-03", "Transfer selesai ≤ 3 detik", "Ukur waktu POST transfer sesama bank (10 kali)", ST.L, "10 transfer: maks. 94 ms (backend lokal)"],
  ["NF-04", "NFR-06", "Rate limit login di gateway", "Kirim > 10 login/menit melalui gateway", ST.T, "Gateway belum dapat dijalankan (Docker belum terpasang)"],
  ["NF-05", "NFR-06", "Akses tanpa token ditolak", "Panggil endpoint terlindung tanpa Authorization", ST.L, "Dicakup test otomatis BE-18"],
  ["NF-06", "NFR-07", "Tidak ada debit ganda dan saldo tidak tembus pada transaksi paralel", "Transfer paralel dengan kunci idempotensi sama", ST.L, "Dicakup test otomatis BE-44"],
  ["NF-07", "NFR-10", "Kompatibilitas Android", "Emulator Pixel 8 (API 37) dan minimal satu ponsel fisik", ST.L, "Emulator Pixel 8 (API 37) lulus seluruh uji SA; ponsel fisik Realme RMX3867 (Android 16): login, SA-08, notifikasi, dan berbagi bukti lulus (14 Nov 2026)"],
  ["NF-08", "NFR-12", "Cakupan unit test logika inti backend ≥ 70%", "Laporan JaCoCo", ST.L, "JaCoCo 0.8.13: cakupan baris 92,2%, cabang 64,8% (45 test)"],
  ["NF-09", "NFR-10", "Kompatibilitas peramban Web", "Alur inti (login, transfer, riwayat, QRIS, keluar) di Chrome, Edge, Firefox terbaru", ST.L, "Chrome 157, Edge 157, Firefox 156 (headless); tanpa error JavaScript"],
];

// ---------------- Defect ----------------
const DEF = [
  ["DEF-01", "Backend – Ledger", "Kritis", "Dua transfer paralel dari rekening yang sama dapat menimpa saldo (lost update) sehingga saldo tidak sesuai jurnal", "Test transfer paralel (BE-44)", "Penguncian baris rekening sebelum posting (LedgerService.lock)", "Ditutup – retest lulus"],
  ["DEF-02", "Android – Jaringan", "Tinggi", "Android 17 (API 37) memblokir koneksi ke backend di jaringan lokal; aplikasi varian online menampilkan \"Tidak dapat terhubung ke server\"", "Uji emulator varian online, 11 Nov 2026", "Izin ACCESS_LOCAL_NETWORK diminta saat aplikasi dibuka", "Ditutup – retest lulus (SA-01)"],
  ["DEF-03", "Dokumentasi API", "Rendah", "backend/docs/API.md rusak: teks dalam backtick hilang dan karakter → / ± menjadi mojibake", "Pemeriksaan dokumen", "API.md dibangkitkan ulang dari openapi.json dengan encoding UTF-8", "Ditutup"],
  ["DEF-04", "Backend – CORS", "Sedang", "Konfigurasi CORS backend tidak memuat daftar origin, sehingga panggilan peramban langsung ke port 8080 akan ditolak", "Pemeriksaan konfigurasi", "CORS dikonfigurasi di backend (ddb.cors.allowed-origins, default localhost) dan header CORS backend disembunyikan di gateway", "Ditutup – retest lulus (BE-46, BE-47; peramban memanggil backend langsung tanpa proxy)"],
  ["DEF-05", "Android – Sesi", "Rendah", "Layar Sesi & Perangkat hanya menampilkan jam, sehingga waktu Berakhir (7 hari kemudian) tampak sama dengan waktu Mulai", "Uji lintas platform LP-04", "Waktu ditampilkan dengan tanggal (formatDateTime); 63 test Android tetap lulus", "Ditutup – retest lulus (LP-04)"],
  ["DEF-07", "Android – Registrasi", "Sedang", "Setelah OTP berhasil, tombol Kembali di layar e-KYC membuka lagi layar OTP; pengguna tertahan dengan pesan \"Akun sudah diverifikasi\"", "Uji SA-21 (emulator)", "Layar OTP dikeluarkan dari back stack setelah verifikasi", "Ditutup – retest lulus (SA-21)"],
  ["DEF-08", "Android – Login", "Tinggi", "Pengguna yang belum menyelesaikan e-KYC lalu login ulang diarahkan ke pembuatan PIN dan Beranda tanpa e-KYC (transaksi keuangan tetap ditolak backend)", "Uji SA-21 (emulator)", "Setelah login, status e-KYC diperiksa; bila belum disetujui pengguna diarahkan ke layar e-KYC", "Ditutup – retest lulus (SA-21)"],
  ["DEF-09", "Web – Profil", "Tinggi", "Ubah profil di Web gagal dengan pesan validasi username karena Web hanya mengirim kolom yang berubah, sedangkan backend menerima draf profil lengkap", "Uji WB-11", "Web mengirim draf profil lengkap (username, email, nomor HP)", "Ditutup – retest lulus (WB-11)"],
  ["DEF-10", "Android – PIN", "Sedang", "Setelah pemulihan PIN berhasil, varian online tetap menganggap PIN diblokir hingga 5 menit karena status blokir disimpan di memori aplikasi", "Uji SA (pemulihan PIN akun demo)", "Status blokir lokal dihapus saat reset PIN berhasil", "Ditutup – retest lulus"],
  ["DEF-11", "Android – Riwayat/Sesi", "Sedang", "Saat server tidak terjangkau, Riwayat menampilkan \"0 transaksi ditemukan\" dan layar Sesi \"Sesi aktif (0)\" tanpa pesan kesalahan", "Uji SA-20 (backend dimatikan)", "Riwayat dan layar Sesi kini menampilkan pesan tidak dapat terhubung/dimuat", "Ditutup – retest lulus (SA-20 dan uji layar Sesi dengan backend dimatikan)"],
  ["DEF-12", "Android – Virtual Card", "Sedang", "Pada varian online, setelah PIN benar layar Kartu menampilkan nomor pengganti 0000 0000 0000 xxxx dan CVV kosong, karena data hasil verifikasi PIN tidak disimpan ke state (FR-22). Ada sejak 11 November; SA-13 sebelumnya lulus keliru karena hanya memeriksa pola 16 digit", "Pengambilan tangkapan layar D05A, 22 Nov 2026", "VirtualCardViewModel.submitPin menyimpan kartu hasil verifikasi; SA-13 kini menolak nomor berawalan 0000 dan CVV kosong", "Ditutup – retest lulus (SA-13)"],
  ["DEF-13", "Android – PIN", "Sedang", "Saat ponsel miring, sebagian keypad dan tautan Lupa PIN pada layar PIN setelah login tidak tampil dan layar tidak dapat digulir", "Peninjauan kode dan uji tata letak, 22 Nov 2026", "Tata letak dua kolom saat miring dan layar dapat digulir", "Ditutup – retest lulus (rotasi di emulator)"],
  ["DEF-14", "Android – Tampilan", "Rendah", "Kontras label status transaksi pada mode gelap hanya 2,6–3,8:1", "Perhitungan kontras warna, 22 Nov 2026", "Warna status khusus mode gelap (6,2–8,0:1)", "Ditutup – diperiksa melalui perhitungan dan gambar komponen"],
  ["DEF-06", "Web – Sesi", "Rendah", "Setelah logout semua perangkat dari Android, Web kembali ke halaman masuk tanpa pesan bahwa sesi telah berakhir", "Uji lintas platform LP-04", "Pesan \"Sesi Anda telah berakhir\" ditampilkan bila refresh token ditolak saat memulihkan sesi", "Ditutup – retest lulus (LP-04)"],
];

// ---------------- Matriks keterlacakan ----------------
const FR = [
  ["FR-01", "Registrasi akun + OTP", "Must"], ["FR-02", "Verifikasi e-KYC", "Must"], ["FR-03", "Login password dan biometrik", "Must"],
  ["FR-04", "PIN transaksi", "Must"], ["FR-05", "Sesi dan pemulihan", "Should"], ["FR-06", "Tampil/sembunyikan saldo", "Must"],
  ["FR-07", "Grafik saldo", "Should"], ["FR-08", "Kantong simpanan", "Should"], ["FR-09", "Locking kantong", "Should"],
  ["FR-10", "Transfer sesama bank", "Must"], ["FR-11", "Transfer antarbank", "Must"], ["FR-12", "Validasi rekening tujuan", "Must"],
  ["FR-13", "Penerima favorit", "Should"], ["FR-14", "Bukti transfer", "Should"], ["FR-15", "Pindai QRIS (Mobile)", "Must"],
  ["FR-16", "QRIS pribadi", "Must"], ["FR-17", "QRIS dinamis", "Could"], ["FR-18", "Pulsa dan paket data", "Must"],
  ["FR-19", "Token listrik", "Must"], ["FR-20", "Tagihan lain", "Could"], ["FR-21", "Buat virtual card", "Should"],
  ["FR-22", "Kelola kartu", "Should"], ["FR-23", "Riwayat + filter", "Must"], ["FR-24", "Detail transaksi", "Must"],
  ["FR-25", "Dashboard bulanan", "Should"], ["FR-26", "Ekspor riwayat (Web)", "Could"], ["FR-27", "Profil pengguna", "Must"],
  ["FR-28", "Pengaturan keamanan dan notifikasi", "Should"], ["FR-29", "Notifikasi transaksi", "Should"],
];
const hasFR = (s, fr) => {
  // s: "FR-01, FR-02" or "FR-06–FR-09"
  const n = +fr.slice(3);
  return s.split(/,\s*/).some((part) => {
    const m = part.trim().match(/^FR-(\d+)(?:–FR-(\d+))?/);
    if (!m) return false;
    const a = +m[1], b = m[2] ? +m[2] : a;
    return n >= a && n <= b;
  });
};
const ids = (list, idIdx, frIdx, fr) => list.filter((r) => hasFR(r[frIdx], fr)).map((r) => r[idIdx]);
const beIds = BE.map((r, i) => ["BE-" + String(i + 1).padStart(2, "0"), r[3]]);
const trace = FR.map(([fr, name, pr]) => {
  const be = ids(beIds, 0, 1, fr), sa = ids(SA, 0, 6, fr), wb = ids(WB, 0, 4, fr), lp = ids(LP, 0, 4, fr);
  return [fr, name, pr, be.join(", ") || "–", sa.join(", ") || "–", [...wb, ...lp].join(", ") || "–"];
});
const mustUncovered = trace.filter((r) => r[2] === "Must" && r[3] === "–" && r[4] === "–");

// ---------------- Ringkasan ----------------
const count = (list, idx, st) => list.filter((r) => r[idx] === st).length;
const saL = count(SA, 7, ST.L), saN = count(SA, 7, ST.N);
const nfL = count(NF, 4, ST.L), nfT = count(NF, 4, ST.T), nfN = count(NF, 4, ST.N);
const be = RUN.backend, an = RUN.androidDemo, ao = RUN.androidOnline;

const body = [
  H1("BAB I  PENDAHULUAN"),
  H2("1.1 Tujuan Dokumen"),
  P("Dokumen ini adalah laporan pengujian sistem Diponegoro Digital Bank. Isinya mencakup strategi pengujian, lingkungan dan data uji, keterlacakan kebutuhan ke kasus uji, kasus uji per platform (Backend/API, Android, Web, lintas platform, dan non-fungsional), hasil eksekusi aktual, log defect, serta ringkasan. Rencana pengujian tingkat tinggi ditetapkan pada Dokumen 1 (PRJ-DDB-001) Bab III. Dokumen ini merinci dan melaporkan pelaksanaannya."),
  H2("1.2 Ruang Lingkup"),
  P("Pengujian mencakup delapan modul bisnis SRS (FR-01 sampai FR-29), aturan bisnis BR-01 sampai BR-07 yang terkait transaksi, Financial Transaction Core (ledger), klien Android varian demo dan online, klien Web, serta NFR terpilih. Di luar ruang lingkup: integrasi produksi dengan jaringan antarbank, QRIS, dan biller (digantikan simulator), aplikasi iOS, uji beban skala produksi, dan uji penetrasi formal."),
  H2("1.3 Status Hasil Uji"),
  table(["Status", "Arti"], [
    ["Lulus", "Hasil aktual sama dengan hasil yang diharapkan; bukti tercatat"],
    ["Gagal", "Hasil aktual berbeda; defect dicatat di Bab VII"],
    ["Terblokir", "Kasus tidak dapat dieksekusi karena prasyarat belum tersedia; alasan dicatat"],
    ["Belum dieksekusi", "Kasus sudah dirancang dan prasyarat tersedia, tetapi belum dijalankan"],
  ], [2200, 6870], { boldFirst: true, size: 18 }),
  CAP("Tabel 1.1 Status hasil uji"),
  P("Seluruh hasil pada dokumen ini berasal dari eksekusi nyata. Kasus yang belum dijalankan tidak diberi hasil."),

  H1("BAB II  STRATEGI PENGUJIAN"),
  H2("2.1 Model dan Tingkat Pengujian"),
  P("Pengujian mengikuti V-Model yang diterapkan secara iteratif (PRJ-DDB-001 3.1). Tabel 2.1 merangkum tingkat pengujian dan perwujudannya pada dokumen ini."),
  table(["Tingkat", "Perwujudan", "Alat", "Bab"], [
    ["Unit", "Test logika per kelas di Android (ledger, repository, use case)", "JUnit, Robolectric", "5.2"],
    ["Integrasi", "Alur API end-to-end di backend; klien Android terhadap server tiruan dan backend sungguhan", "Spring Boot Test + H2 (MockMvc), MockWebServer", "5.1, 5.2"],
    ["Sistem", "Kasus uji manual per platform dan lintas platform; NFR terpilih", "Emulator, ponsel, peramban, Swagger UI", "5.3–5.6"],
    ["Penerimaan", "Skenario demo bersama dosen", "Dokumen 8", "–"],
  ], [1500, 3970, 2600, 1000], { boldFirst: true, size: 18 }),
  CAP("Tabel 2.1 Tingkat pengujian"),
  H2("2.2 Prioritas"),
  P("Kasus uji untuk FR berprioritas Must dijalankan lebih dahulu, termasuk alur alternatif transfer UC-01 A1–A6. Setiap FR Must wajib memiliki minimal satu kasus uji (Bab IV). Kasus dengan risiko kehilangan dana (atomisitas, idempotensi, refund, blokir PIN) diuji pada tingkat backend dan sistem."),
  H2("2.3 Kriteria Masuk, Keluar, dan Keparahan Defect"),
  P("Kriteria masuk dan keluar mengikuti PRJ-DDB-001 Tabel 3.3. Ringkasnya: pengujian sistem dimulai setelah build uji tersedia dan seluruh test otomatis lulus; pengujian dinyatakan selesai bila seluruh kasus FR Must telah dieksekusi, tidak ada defect Kritis atau Tinggi yang terbuka, dan hasil pengukuran NFR tercatat."),
  table(["Keparahan", "Kriteria"], [
    ["Kritis", "Kehilangan dana, data rusak, atau aplikasi tidak dapat dipakai"],
    ["Tinggi", "Fungsi Must gagal tanpa solusi sementara"],
    ["Sedang", "Fungsi Should/Could gagal, atau ada solusi sementara"],
    ["Rendah", "Kosmetik atau dokumentasi"],
  ], [2000, 7070], { boldFirst: true, size: 18 }),
  CAP("Tabel 2.2 Tingkat keparahan defect"),

  H1("BAB III  LINGKUNGAN DAN DATA UJI"),
  H2("3.1 Lingkungan"),
  table(["Lingkungan", "Konfigurasi", "Status"], [
    ["Test otomatis backend", "Java 21, Spring Boot 4.1.2, profil test (H2 in-memory mode PostgreSQL); ./mvnw test", "Tersedia"],
    ["Backend lokal untuk uji manual", "run-local.ps1 -H2 → http://localhost:8080 (Swagger UI /swagger-ui.html)", "Tersedia"],
    ["Backend dengan PostgreSQL", "run-local.ps1 (profil dev)", "Belum dikonfigurasi"],
    ["Gateway + kontainer", "docker compose (Nginx, backend, PostgreSQL 17)", "Belum tersedia (Docker belum terpasang)"],
    ["Android – emulator", "Pixel 8, Android 17 (API 37); varian demo dan online (http://10.0.2.2:8080/)", "Tersedia"],
    ["Android – ponsel fisik", "Realme RMX3867, Android 16; varian online lewat kabel USB (adb reverse tcp:8080 tcp:8080 dan -Pddb.baseUrl=http://127.0.0.1:8080/). Alternatif: -Pddb.baseUrl=http://<IP-laptop>:8080/ (Wi-Fi sama atau hotspot); langkah pada TUT-DDB-001-AND", "Tersedia; dipakai 14 Nov 2026"],
    ["Web", "Vite dev server (http://localhost:5173) dengan proxy /api → backend; Chrome (headless untuk uji otomatis), Firefox, Edge", "Tersedia; diuji di Chrome"],
  ], [2300, 4570, 2200], { boldFirst: true, size: 18 }),
  CAP("Tabel 3.1 Lingkungan pengujian"),
  H2("3.2 Data Uji"),
  table(["Data", "Nilai", "Kegunaan"], [
    ["Akun demo", "demo / Demo@1234 / PIN 123456; rekening 1400123456", "Akun utama penguji"],
    ["Nasabah lain", "andi, budi, citra; rekening 1400000002, 1400000003, 1400000004", "Penerima transfer, perangkat kedua"],
    ["Antarbank ditolak", "BRI 9999999999", "Alur A6 dan refund (BR-05)"],
    ["Antarbank timeout", "Mandiri 8888888888", "Alur A5 (status diproses)"],
    ["e-KYC ditolak", "NIK berakhiran 0000", "FR-02 negatif"],
    ["Pelanggan tidak ditemukan", "ID pelanggan PLN/tagihan berakhiran 0000", "FR-19, FR-20 negatif"],
  ], [2300, 4170, 2600], { boldFirst: true, size: 18 }),
  CAP("Tabel 3.2 Data uji simulasi"),
  P("Akun dan kredensial demo hanya untuk lingkungan lokal dan demo. Seluruh data bersifat simulasi."),

  H1("BAB IV  MATRIKS KETERLACAKAN"),
  P("Tabel 4.1 memetakan setiap FR ke kasus uji. Kolom Backend berisi test otomatis (Tabel 5.1), kolom Android berisi kasus uji sistem Android (Tabel 5.3), dan kolom Web/Lintas berisi kasus uji Web (Tabel 5.4) dan lintas platform (Tabel 5.5)."),
  table(["FR", "Kebutuhan", "Prioritas", "Backend (otomatis)", "Android (sistem)", "Web / Lintas"], trace, [800, 2000, 900, 2100, 1700, 1570], { size: 16, center: [2] }),
  CAP("Tabel 4.1 Matriks keterlacakan FR ke kasus uji"),
  P(mustUncovered.length === 0
    ? "Seluruh FR berprioritas Must memiliki minimal satu kasus uji Backend dan satu kasus uji sistem. FR-15 (pindai QRIS) hanya berlaku di Android, dan FR-26 (ekspor riwayat) diuji di backend dan Web."
    : "FR Must tanpa kasus uji: " + mustUncovered.map((r) => r[0]).join(", ") + "."),

  H1("BAB V  KASUS UJI"),
  H2("5.1 Backend/API (Test Integrasi Otomatis)"),
  P(`Seluruh test berjalan terhadap API sungguhan (MockMvc) dengan basis data H2 dan jam yang dapat diatur (MutableClock) untuk menguji batas waktu. Hasil eksekusi ${be.date}: **${be.passed} dari ${be.total} lulus**, ${be.failed} gagal, ${be.skipped} dilewati, waktu ${be.time}.`),
  table(["ID", "Kelas test", "Yang diuji", "Acuan", "Hasil"],
    BE.map((r, i) => ["BE-" + String(i + 1).padStart(2, "0"), r[0].replace("HistoryProfileNotificationTest", "HistoryProfileNotif…").replace("PaymentTopUpCardFlowTest", "PaymentTopUpCard…"), r[2], r[3], be.failedNames.includes(r[1]) ? ST.G : ST.L]),
    [750, 2100, 4020, 1400, 800], { center: [0, 4], size: 16 }),
  CAP("Tabel 5.1 Kasus uji Backend/API otomatis"),
  H2("5.2 Android (Test Unit dan Integrasi Otomatis)"),
  P(`Varian demo dijalankan dengan ./gradlew :app:testDemoDebugUnitTest. Hasil ${an.date}: **${an.passed} lulus dari ${an.total}**, ${an.failed} gagal, ${an.skipped} dilewati (LiveBackendTest hanya berjalan pada varian online). Varian online (${ao.date}): ${ao.passed} dari ${ao.total} lulus, ${ao.skipped} dilewati (LiveBackendTest hanya berjalan bila backend aktif dan parameter -Pddb.liveUrl diberikan; eksekusi terakhirnya terhadap backend sungguhan lulus pada 11 November 2026).`),
  P("Pembaruan tampilan Android (21 November 2026) mengganti tema, font, ikon aplikasi, layar Login, layar PIN (keypad angka; PIN dikirim otomatis pada digit keenam), Beranda, bottom navigation (QRIS di tengah, tab Kantong), serta komponen bersama (panel, kolom input, tombol). ViewModel, repository, integrasi API, dan backend tidak diubah. Seluruh test di atas dijalankan ulang setelah pembaruan tersebut; AppTourTest disesuaikan dengan label dan alur PIN yang baru, dan DarkModeShotsTest ditambahkan untuk memeriksa mode gelap."),
  table(["Kelas test", "Jumlah", "Cakupan", "Acuan"], AND, [2500, 800, 4070, 1700], { center: [1], size: 16 }),
  CAP("Tabel 5.2 Test otomatis Android per kelas"),
  H2("5.3 Kasus Uji Sistem Android"),
  P("Kasus SA-01 sampai SA-20 sama dengan skenario U-01 sampai U-20 pada Panduan Uji Aplikasi di Ponsel (TUT-DDB-001-AND), sehingga penguji dapat memakai panduan tersebut langkah demi langkah. SA-21 sampai SA-28 ditambahkan untuk menutup FR yang belum tercakup. Kolom varian: D = demo, O = online."),
  table(["ID", "Judul", "Prasyarat", "Langkah", "Hasil yang diharapkan", "Var.", "Acuan", "Status"],
    SA.map((r) => r.slice(0, 8)), [650, 1250, 1100, 1900, 2000, 500, 900, 770], { center: [0, 5], size: 15 }),
  CAP("Tabel 5.3 Kasus uji sistem Android"),
  P(`Eksekusi: ${count(SA, 7, ST.L)} dari ${SA.length} kasus lulus. Eksekusi pertama: seluruh kasus kecuali SA-08 dijalankan pada emulator Pixel 8 (Android 17, varian online) terhadap backend lokal, 11–12 November 2026, dengan skrip pengujian/android/sa-android.mjs yang menggerakkan aplikasi lewat adb dan uiautomator; tangkapan layar dan log tersimpan di pengujian/bukti/2026-11-11/android-sa/. SA-08 dijalankan manual di ponsel fisik pada 14 November 2026. ${(() => { const n = SA.filter((r) => r[7] === ST.N); return n.length ? "Belum dieksekusi: " + n.map((r) => r[0] + " (" + r[8] + ")").join("; ") + "." : "Seluruh kasus sudah dieksekusi."; })()}`),
  P("APK rilis bertanda tangan (build-rilis.ps1, 12 November 2026) diuji ulang di emulator dengan SA-01, SA-07, SA-09, SA-10, SA-13, dan skenario lintas platform LP-01 sampai LP-04; seluruhnya lulus."),
  P("Uji di ponsel fisik (14 November 2026) memakai Realme RMX3867 (Android 16) dengan varian online yang terhubung ke backend lokal (profil H2) lewat kabel USB (adb reverse). SA-08 dijalankan manual: QR dari layar Terima QRIS pada perangkat kedua dipindai dengan kamera, penerima dan nominal tampil, pembayaran dengan PIN berhasil, dan saldo kedua akun berubah. Pada perangkat yang sama, login akun demo, notifikasi push (SA-16), dan berbagi bukti transaksi ke WhatsApp juga lulus."),
  P("Dua kendala lingkungan ditemui dan diatasi tanpa perubahan kode, sehingga tidak dicatat sebagai defect: (1) APK yang dibangun dengan alamat bawaan emulator (http://10.0.2.2:8080/) menampilkan \"Tidak dapat terhubung ke server\" di ponsel fisik, diatasi dengan membangun ulang memakai -Pddb.baseUrl dan adb reverse; (2) ponsel Realme menolak pemasangan lewat USB (INSTALL_FAILED_VERIFICATION_FAILURE) sampai opsi pengembang \"Nonaktifkan pemantauan izin\" diaktifkan dan dialog pemasangan disetujui."),
  P("Eksekusi ulang pada APK final (22 November 2026): varian online di emulator Pixel 8 (Android 17) terhadap backend lokal profil H2. Dalam satu putaran penuh 23 kasus dijalankan dengan skrip final dan seluruhnya lulus. SA-18 dan SA-20, yang hanya berjalan bila disebut eksplisit, dijalankan terpisah pada APK yang sama dan lulus (SA-20 sempat gagal dua kali karena cara menjalankan uji: backend tidak berhasil dimatikan skrip, lalu dialog izin notifikasi menutup layar; lulus setelah prosedur resmi dipakai). SA-04 tercakup oleh LP-01 dan LPN-01 (transfer sesama bank), dan SA-19 oleh LP-04 dan LPN-03 (logout semua perangkat). Dengan demikian 27 dari 28 kasus lulus pada build baru; SA-08 (kamera) serta berbagi bukti pada SA-04 terakhir lulus di ponsel fisik pada 14 November 2026 dengan build sebelumnya dan belum diulang. Bukti: pengujian/bukti/2026-11-22/android-sa/."),
  P("Perbaikan sebelum eksekusi: layar PIN saat ponsel miring (DEF-13; diuji dengan memutar emulator saat dua digit sudah diisi, digit tetap tersimpan dan login berhasil), warna label status mode gelap (DEF-14), layar Kartu (judul dan tombol kembali), dan detail kartu virtual (DEF-12). Saat DEF-12 ditemukan, hasil SA-13 pada 11 dan 22 November dinyatakan tidak sah, pemeriksaannya diperketat, dan kasus diulang sampai lulus."),
  P("Kegagalan lain pada putaran awal 22 November 2026 berasal dari skrip, bukan aplikasi: SA-21 dan SA-22 mencari tombol \"Daftar\" yang sudah bernama \"Buka rekening\" (SA-02 dan SA-15 ikut gagal karena memakai akun dari SA-21); SA-14, SA-16, SA-17, SA-27, dan SA-28 berjalan dengan sesi pengguna baru dari SA-13; SA-23 memakai sesi yang PIN-nya sengaja diblokir SA-02. Skrip diperbaiki tanpa melonggarkan pemeriksaan."),
  P("Keterbatasan: sebagian besar kasus dijalankan di emulator; di ponsel fisik baru SA-08, notifikasi push, login, dan berbagi bukti yang dijalankan; pengetikan lewat adb di emulator sesekali meleset sehingga skrip memeriksa panjang isian PIN sebelum dikirim; login biometrik diuji pada mode simulasi aplikasi."),
  H2("5.4 Kasus Uji Web"),
  P("Dua aplikasi Web diuji. Web React (prototipe yang dibuat Project Manager, kini di apps/web_dummy_buatan_sendiri) diuji dengan WB-01 sampai WB-15. Pada 22 November 2026 Web resmi ditetapkan Web tim (Node.js, Express 5, EJS; digitalbanking-node v2.0.0), yang diuji dengan WN-01 sampai WN-14 pada 5.4.1. Hasil WB tidak berlaku untuk Web tim."),
  P("Kasus uji Web disusun dari User Interface Map (Web) v0.3 dan SRS (WB-13 sampai WB-15 ditambahkan setelah pemeriksaan ulang cakupan FR-07, FR-08/FR-09, dan FR-27 di Web), serta ruang lingkup Web pada SAD-DDB-001 Bab VIII. Aplikasi Web (folder apps/web/, React + Vite) terhubung ke backend sejak 11 November 2026. Kasus dieksekusi dengan uji UI otomatis di Chrome headless (pengujian/web/ui-e2e.mjs) terhadap backend lokal; tangkapan layar disimpan sebagai bukti. Kontrak API yang dipakai Web juga diperiksa dengan pengujian/web/api-smoke.mjs (38 pemeriksaan, seluruhnya lulus). Cakupan minimum Web (WB-01, WB-04, WB-05, WB-07) sudah tercapai."),
  P("Bukti: " + WB.filter((r) => r[5] === ST.L).map((r) => r[0]).join(", ") + " lulus." + (() => { const n = WB.filter((r) => r[5] === ST.N); return n.length ? " Belum dieksekusi: " + n.map((r) => `${r[0]}${r[6] ? " (" + r[6] + ")" : ""}`).join("; ") + "." : ""; })()),
  table(["ID", "Judul", "Langkah", "Hasil yang diharapkan", "Acuan", "Status"],
    WB.map((r) => r.slice(0, 6)), [700, 1700, 2300, 2470, 1100, 800], { center: [0], size: 16 }),
  CAP("Tabel 5.4 Kasus uji Web"),
  H3("5.4.1 Web Tim (Node/Express/EJS)"),
  P("WN-01 sampai WN-14 dijalankan pada 22 November 2026 dengan pengujian/web/node-smoke.mjs terhadap backend asli (tanpa patch saldo awal) dan data demo bersih; kode sumber Web tim tidak diubah. Skrip mengirim formulir HTTP seperti peramban, termasuk cookie sesi dan token CSRF, lalu mencocokkan saldo ke backend. Tampilan dan kompatibilitas peramban Web tim belum diuji. Bukti: pengujian/bukti/2026-11-22/web-node/."),
  table(["ID", "Kasus", "Hasil yang diharapkan", "Acuan", "Status"], WN, [800, 3000, 3170, 1300, 800], { center: [0], size: 16 }),
  CAP("Tabel 5.4a Kasus uji alur inti Web tim"),
  P("Menurut README Web tim, fitur berikut belum tersedia: kantong (FR-08, FR-09), kartu virtual (FR-21, FR-22), notifikasi (FR-29), grafik saldo (FR-07), penerima favorit (FR-13), QRIS pribadi dan dinamis (FR-16, FR-17), dan paket data (bagian dari FR-18). Fitur tersebut tersedia di Android dan di Web React. QRIS di Web tim berupa pemilihan contoh atau tempel payload, tanpa kamera."),
  H2("5.5 Skenario Lintas Platform"),
  P("Skenario lintas platform memverifikasi bahwa Android dan Web membaca ledger yang sama (NFR-08). Skenario dijalankan pada emulator Pixel 8 (Android 17, varian online) dan Chrome headless terhadap satu backend lokal, dengan akun demo dan andi. Konsistensi antarperangkat Android tetap diuji melalui SA-17."),
  table(["ID", "Judul", "Langkah", "Hasil yang diharapkan", "Acuan", "Status"],
    LP.map((r) => r.slice(0, 6)), [700, 1700, 2500, 2270, 1100, 800], { center: [0], size: 16 }),
  P("Pelaksanaan: " + LP.map((r) => `${r[0]} – ${r[6]}`).join("; ") + ". Skrip: pengujian/lintas-platform/lp-cross.mjs (Android digerakkan lewat adb/uiautomator, Web lewat Chrome DevTools Protocol); dijalankan dua kali berturut-turut dengan hasil sama."),
  P("Eksekusi ulang dengan Web React pada build Android dengan tampilan baru (22 November 2026, sebelum perbaikan DEF-12): LP-01 sampai LP-04 lulus dua putaran berturut-turut dengan skrip final, tanpa error JavaScript di Web. Pada putaran percobaan sebelumnya pesan \"Sesi Anda telah berakhir\" (DEF-06) tidak tertangkap pada satu jalan. Penelusuran menunjukkan Web sudah lebih dulu kembali ke halaman masuk dan menampilkan pesan melalui polling notifikasi (30 detik), lalu skrip memuat ulang halaman sehingga pesan hilang; DEF-06 tidak terulang. Pemeriksaan LP-04 yang sebelumnya selalu bernilai lulus kini mewajibkan sesi Web terlihat di Android dan pesan sesi berakhir tampil di Web. Bukti: pengujian/bukti/2026-11-22/lintas-platform/."),
  CAP("Tabel 5.5 Skenario lintas platform"),
  P("Skenario lintas platform dengan Web tim (22 November 2026, pengujian/lintas-platform/lp-node.mjs): LPN-01 sampai LPN-03 adalah padanan LP-01, LP-03, dan LP-04. LP-02 tidak memiliki padanan karena Web tim belum menampilkan QRIS pribadi atau dinamis. Bukti: pengujian/bukti/2026-11-22/lintas-platform-node/."),
  table(["ID", "Skenario", "Hasil yang diharapkan", "Acuan", "Status"], LPN, [900, 2700, 3370, 1300, 800], { center: [0], size: 16 }),
  CAP("Tabel 5.5a Skenario lintas platform Android dan Web tim"),
  H2("5.6 Pengujian Non-Fungsional"),
  table(["ID", "NFR", "Target", "Cara uji", "Status", "Catatan"], NF, [700, 800, 2000, 2470, 1000, 2100], { center: [0, 1], size: 16 }),
  CAP("Tabel 5.6 Kasus uji non-fungsional"),

  H1("BAB VI  HASIL EKSEKUSI"),
  table(["Kelompok", "Jumlah", "Lulus", "Gagal", "Terblokir", "Belum dieksekusi"], [
    ["Backend/API otomatis", String(be.total), String(be.passed), String(be.failed), "0", "0"],
    ["Android otomatis (demo)", String(an.total), String(an.passed), String(an.failed), "0", String(an.skipped) + " (dilewati)"],
    ["Android otomatis (online)", String(ao.total), String(ao.passed), String(ao.failed), "0", ao.skipped ? String(ao.skipped) + " (dilewati)" : "0"],
    ["Sistem Android", String(SA.length), String(saL), "0", "0", String(saN)],
    ["Sistem Web React", String(WB.length), String(count(WB, 5, ST.L)), "0", "0", String(count(WB, 5, ST.N))],
    ["Alur inti Web tim (Node)", String(WN.length), String(count(WN, 4, ST.L)), "0", "0", String(count(WN, 4, ST.N))],
    ["Lintas platform (Web React)", String(LP.length), String(count(LP, 5, ST.L)), "0", "0", String(count(LP, 5, ST.N))],
    ["Lintas platform (Web tim)", String(LPN.length), String(count(LPN, 4, ST.L)), "0", "0", String(count(LPN, 4, ST.N))],
    ["Non-fungsional", String(NF.length), String(nfL), "0", String(nfT), String(nfN)],
  ], [2770, 1100, 1100, 1100, 1300, 1700], { boldFirst: true, center: [1, 2, 3, 4, 5], size: 18 }),
  CAP("Tabel 6.1 Ringkasan hasil eksekusi"),
  P(`Test otomatis backend dijalankan ulang pada ${be.date}. Test otomatis Android varian demo dan online dijalankan ulang pada ${ao.date} setelah perbaikan tampilan Android. Baris Sistem Android menghitung hasil terakhir setiap kasus: 27 kasus pada build 22 November 2026 dan SA-08 pada build 14 November 2026. Baris Lintas platform dan Web tim berasal dari eksekusi 22 November 2026; baris Sistem Web React berasal dari eksekusi 11–12 November 2026. Eksekusi kasus uji sistem manual mengikuti jadwal pengujian pada PRJ-DDB-001 3.7 dan hasilnya diisikan pada revisi dokumen ini beserta bukti (tangkapan layar atau rekaman).`),

  H1("BAB VII  LOG DEFECT"),
  P("Defect berikut ditemukan selama pengembangan dan pengujian awal. Defect baru dari eksekusi uji sistem ditambahkan dengan format yang sama."),
  table(["ID", "Komponen", "Keparahan", "Deskripsi", "Ditemukan melalui", "Perbaikan", "Status"], DEF, [750, 1200, 900, 2300, 1400, 1520, 1000], { center: [0], size: 15 }),
  CAP("Tabel 7.1 Log defect"),

  H1("BAB VIII  RINGKASAN DAN TINDAK LANJUT"),
  H2("8.1 Ringkasan"),
  B(`Backend: ${be.passed}/${be.total} test integrasi lulus, mencakup seluruh FR Must yang relevan dengan backend serta BR transaksi (atomisitas, idempotensi, refund, blokir PIN).`),
  B(`Android: ${an.passed}/${an.total} test varian demo lulus (${an.skipped} dilewati sesuai desain) dan ${ao.passed}/${ao.total} test varian online lulus (${ao.skipped} dilewati), dijalankan ulang setelah pembaruan tampilan. Uji sistem Android: ${saL} dari ${SA.length} kasus lulus; 27 di antaranya dijalankan ulang di emulator pada build dengan tampilan baru (22 November 2026), sedangkan SA-08 lulus di ponsel fisik Realme RMX3867 (Android 16) pada build sebelumnya (14 November 2026).`),
  B(`Web tim (Node, Web resmi): ${count(WN, 4, ST.L)} dari ${WN.length} alur inti lulus terhadap backend asli; tujuh kelompok fitur belum tersedia (5.4.1). Web React (prototipe): ${count(WB, 5, ST.L)} dari ${WB.length} kasus lulus melalui uji UI otomatis.`),
  B(`Lintas platform: ${count(LPN, 4, ST.L)} dari ${LPN.length} skenario dengan Web tim dan ${count(LP, 5, ST.L)} dari ${LP.length} skenario dengan Web React lulus; saldo, riwayat, QRIS, dan sesi konsisten antara Android dan Web (NFR-08 terverifikasi pada lingkungan lokal).`),
  B((() => { const open = DEF.filter((d) => d[6].startsWith("Terbuka")).length; return `Defect: ${DEF.length} tercatat; ${DEF.length - open} ditutup dengan retest` + (open ? `; ${open} terbuka dengan solusi sementara terverifikasi.` : "; tidak ada defect terbuka."); })()),
  H2("8.2 Status Kriteria Keluar"),
  table(["Kriteria keluar", "Status"], [
    ["Seluruh kasus FR Must dieksekusi", "Terpenuhi: seluruh kasus Must lulus di emulator dan peramban; pemindaian QRIS dengan kamera (SA-08) lulus di ponsel fisik pada build 14 November 2026. Pengecualian Web tim: paket data (bagian FR-18) belum ada"],
    ["Tidak ada defect Kritis/Tinggi terbuka", "Terpenuhi: seluruh 14 defect sudah ditutup; tidak ada defect Kritis atau Tinggi terbuka"],
    ["Cakupan unit test logika inti backend ≥ 70% (JaCoCo)", "Terpenuhi: 92,2% baris (64,8% cabang)"],
    ["Pengukuran NFR performa tercatat", "Terpenuhi: NF-01 sampai NF-03 lulus (Tabel 5.6)"],
    ["Laporan disetujui Project Manager", "Terpenuhi: 22 November 2026"],
  ], [4500, 4570], { boldFirst: true, size: 18 }),
  CAP("Tabel 8.1 Status kriteria keluar"),
  H2("8.3 Tindak Lanjut"),
  ...NL([
    "Ulangi SA-08 (kamera), berbagi bukti (SA-04), dan notifikasi push (SA-16) di ponsel fisik pada build Android dengan tampilan baru.",
    "Jalankan NF-04 (rate limit gateway) setelah Docker terpasang.",
    "Uji tampilan dan kompatibilitas peramban untuk Web tim.",
    "Review oleh PIC Dokumen 6 (Steven Wibisana) setelah penerbitan; temuan dicatat pada versi berikutnya.",
  ]),
  H2("Referensi"),
  ...NL([
    "Kelompok Jaran Goyang. (2026). Software Requirements Specification PT Diponegoro Digital Bank (SRS-DDB-001) v0.1.",
    "Kelompok Jaran Goyang. (2026). Project Document (PRJ-DDB-001) v1.0, Bab III Rencana Testing.",
    "Kelompok Jaran Goyang. (2026). Architectural Design (SAD-DDB-001) v1.0; Functional Design (FSD-DDB-001) v1.0.",
    "Kelompok Jaran Goyang. (2026). Panduan Uji Aplikasi di Ponsel (TUT-DDB-001-AND) v1.1.",
    "ISO/IEC/IEEE 29119-3:2021. Software and systems engineering – Software testing – Test documentation.",
  ]),
];

S.build(meta, body, OUT);
