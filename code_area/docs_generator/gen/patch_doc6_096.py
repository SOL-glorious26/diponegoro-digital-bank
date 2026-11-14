import sys, os
os.chdir(sys.argv[1])
p = 'doc6.js'
s = open(p, encoding='utf-8').read()

def rep(a, b):
    global s
    assert s.count(a) == 1, (s.count(a), a[:80])
    s = s.replace(a, b)

# Meta dan riwayat revisi
rep('code: "STD-DDB-001", version: "0.9.5",', 'code: "STD-DDB-001", version: "0.9.6",')
rep('date: "12 November 2026", logo: LOGO,', 'date: "14 November 2026", logo: LOGO,')
rep('seluruh defect ditutup."],\n  ],',
    'seluruh defect ditutup."],\n'
    '    ["0.9.6", "14 Nov 2026", "Salomo P.G. Hokoyoku", "Uji di ponsel fisik Realme RMX3867 (Android 16): SA-08 (pindai QRIS dengan kamera) lulus sehingga uji sistem Android 28/28; NF-07 lulus; notifikasi push dan berbagi bukti juga lulus di ponsel fisik; lingkungan ponsel fisik dan kriteria keluar diperbarui."],\n  ],')

# Kasus uji sistem Android
rep('ST.N, "Memerlukan kamera ponsel fisik; pemindaian QR Web dari gambar galeri sudah lulus (LP-02)"]',
    'ST.L, "Ponsel fisik Realme RMX3867 (Android 16), 14 Nov 2026: QR dari layar Terima QRIS perangkat kedua terpindai kamera; penerima dan nominal tampil; saldo kedua akun berubah"]')
rep('"Notifikasi muncul ±6–10 detik setelah transfer masuk (emulator)"]',
    '"Notifikasi muncul ±6–10 detik setelah transfer masuk (emulator); juga muncul di ponsel fisik Realme RMX3867 (14 Nov 2026)"]')
rep('"Emulator; juga LP-01 (dana terlihat di Web penerima)"]',
    '"Emulator; juga LP-01 (dana terlihat di Web penerima); berbagi bukti ke WhatsApp lulus di ponsel fisik (14 Nov 2026)"]')

# Non-fungsional
rep('ST.N, "Emulator Pixel 8 (API 37) lulus seluruh uji SA; ponsel fisik belum"]',
    'ST.L, "Emulator Pixel 8 (API 37) lulus seluruh uji SA; ponsel fisik Realme RMX3867 (Android 16): login, SA-08, notifikasi, dan berbagi bukti lulus (14 Nov 2026)"]')

# Lingkungan uji
rep('["Android – ponsel fisik", "Varian online dengan -Pddb.baseUrl=http://<IP-laptop>:8080/ (Wi-Fi sama, hotspot, atau ngrok); langkah pada TUT-DDB-001-AND", "Belum dijalankan; merek dan versi Android dicatat saat eksekusi"]',
    '["Android – ponsel fisik", "Realme RMX3867, Android 16; varian online lewat kabel USB (adb reverse tcp:8080 tcp:8080 dan -Pddb.baseUrl=http://127.0.0.1:8080/). Alternatif: -Pddb.baseUrl=http://<IP-laptop>:8080/ (Wi-Fi sama atau hotspot); langkah pada TUT-DDB-001-AND", "Tersedia; dipakai 14 Nov 2026"]')

# Bab 5.3: narasi eksekusi
rep('P(`Eksekusi: ${count(SA, 7, ST.L)} dari ${SA.length} kasus lulus pada emulator Pixel 8 (Android 17, varian online) terhadap backend lokal, 11–12 November 2026. Kasus dijalankan dengan skrip pengujian/android/sa-android.mjs yang menggerakkan aplikasi lewat adb dan uiautomator; tangkapan layar dan log tersimpan di pengujian/bukti/2026-11-11/android-sa/. Belum dieksekusi: ${SA.filter((r) => r[7] === ST.N).map((r) => r[0] + " (" + r[8] + ")").join("; ")}.`),',
    'P(`Eksekusi: ${count(SA, 7, ST.L)} dari ${SA.length} kasus lulus. Seluruh kasus kecuali SA-08 dijalankan pada emulator Pixel 8 (Android 17, varian online) terhadap backend lokal, 11–12 November 2026, dengan skrip pengujian/android/sa-android.mjs yang menggerakkan aplikasi lewat adb dan uiautomator; tangkapan layar dan log tersimpan di pengujian/bukti/2026-11-11/android-sa/. SA-08 dijalankan manual di ponsel fisik pada 14 November 2026. ${(() => { const n = SA.filter((r) => r[7] === ST.N); return n.length ? "Belum dieksekusi: " + n.map((r) => r[0] + " (" + r[8] + ")").join("; ") + "." : "Seluruh kasus sudah dieksekusi."; })()}`),')
anchor = '  P("Keterbatasan: pengujian memakai emulator, bukan ponsel fisik;'
assert s.count(anchor) == 1
s = s.replace(anchor,
    '  P("Uji di ponsel fisik (14 November 2026) memakai Realme RMX3867 (Android 16) dengan varian online yang terhubung ke backend lokal (profil H2) lewat kabel USB (adb reverse). SA-08 dijalankan manual: QR dari layar Terima QRIS pada perangkat kedua dipindai dengan kamera, penerima dan nominal tampil, pembayaran dengan PIN berhasil, dan saldo kedua akun berubah. Pada perangkat yang sama, login akun demo, notifikasi push (SA-16), dan berbagi bukti transaksi ke WhatsApp juga lulus."),\n'
    '  P("Dua kendala lingkungan ditemui dan diatasi tanpa perubahan kode, sehingga tidak dicatat sebagai defect: (1) APK yang dibangun dengan alamat bawaan emulator (http://10.0.2.2:8080/) menampilkan \\"Tidak dapat terhubung ke server\\" di ponsel fisik, diatasi dengan membangun ulang memakai -Pddb.baseUrl dan adb reverse; (2) ponsel Realme menolak pemasangan lewat USB (INSTALL_FAILED_VERIFICATION_FAILURE) sampai opsi pengembang \\"Nonaktifkan pemantauan izin\\" diaktifkan dan dialog pemasangan disetujui."),\n'
    + anchor, 1)
rep('P("Keterbatasan: pengujian memakai emulator, bukan ponsel fisik;',
    'P("Keterbatasan: sebagian besar kasus dijalankan di emulator; di ponsel fisik baru SA-08, notifikasi push, login, dan berbagi bukti yang dijalankan;')

# Bab 5.4: hilangkan "Belum dieksekusi: ." bila daftar kosong
rep('''" lulus. Belum dieksekusi: " + WB.filter((r) => r[5] === ST.N).map((r) => `${r[0]}${r[6] ? " (" + r[6] + ")" : ""}`).join("; ") + "."),''',
    '''" lulus." + (() => { const n = WB.filter((r) => r[5] === ST.N); return n.length ? " Belum dieksekusi: " + n.map((r) => `${r[0]}${r[6] ? " (" + r[6] + ")" : ""}`).join("; ") + "." : ""; })()),''')

# Bab 8: ringkasan, kriteria keluar, tindak lanjut
rep('Uji sistem di emulator: ${saL} dari ${SA.length} kasus lulus, sisanya belum dieksekusi.`),',
    'Uji sistem Android: ${saL} dari ${SA.length} kasus lulus (SA-08 di ponsel fisik Realme RMX3867, Android 16; kasus lain di emulator).`),')
rep('"Hampir: seluruh kasus Must lulus di emulator dan peramban; pemindaian kamera di ponsel fisik (SA-08) belum"',
    '"Terpenuhi: seluruh kasus Must lulus di emulator dan peramban; pemindaian QRIS dengan kamera (SA-08) lulus di ponsel fisik"')
rep('    "Jalankan SA-08 (pindai QRIS dengan kamera) dan ulangi sebagian SA di minimal satu ponsel fisik memakai TUT-DDB-001-AND; catat merek dan versi Android (NF-07).",\n', '')

open(p, 'w', encoding='utf-8').write(s)
print('ok')
