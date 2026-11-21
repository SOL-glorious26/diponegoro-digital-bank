"""D07A DVM-DDB-001-AND v0.9.2 -> v0.9.3 (21 Nov 2026): tampilan Android baru, inventaris berkas, angka test.
Jalankan setelah update_kdoc.py. Pemakaian: python patch_d07a_093.py <folder gen>  lalu  node doc7a.js ...
"""
import sys, os
os.chdir(sys.argv[1])
p = 'doc7a.js'
s = open(p, encoding='utf-8').read()


def rep(a, b):
    global s
    assert s.count(a) == 1, (s.count(a), a[:80])
    s = s.replace(a, b)


# Meta dan riwayat revisi
rep('code: "DVM-DDB-001-AND", version: "0.9.2",', 'code: "DVM-DDB-001-AND", version: "0.9.3",')
rep('date: "12 November 2026", logo: LOGO,', 'date: "21 November 2026", logo: LOGO,')
rep('keterbatasan diperbarui (layar Sesi saat server tidak terjangkau, DEF-11)."],\n',
    'keterbatasan diperbarui (layar Sesi saat server tidak terjangkau, DEF-11)."],\n'
    '    ["0.9.3", "21 Nov 2026", "Salomo P.G. Hokoyoku", "Tampilan Android baru: tema (ui/theme: Color, Theme, Type, Brand), font Plus Jakarta Sans, ikon dari logo asli, layar Login, PIN dengan keypad, Beranda, bottom navigation lima tab, komponen bersama (Components.kt, Buttons.kt); inventaris berkas diperbarui (112 berkas); jumlah test Android 64 (demo) dan 58 (online)."],\n')

# Keterangan berkas yang berubah / baru
rep('"ui/theme/Theme.kt": "DigitalBankTheme (Material 3 terang/gelap) dan ChartColors per mode.",',
    '"ui/theme/Theme.kt": "DigitalBankTheme: skema warna Material 3 terang/gelap (tanpa dynamic color), bentuk (sudut 10/14/20 dp), DdbColors (emas, dana masuk, panel, garis, logo) lewat DdbTheme.colors, dan ChartColors per mode.",\n'
    '  "ui/theme/Brand.kt": "DdbMark (tanda logo Diponegoro dari logo asli, diwarnai navy di mode terang dan emas di mode gelap) dan GoldRule (garis aksen emas).",\n'
    '  "core/ui/Buttons.kt": "DdbButton dan DdbOutlinedButton: tombol bersudut 10 dp, tinggi minimum 48 dp, tanpa bayangan. Layar memakainya lewat import alias (import …DdbButton as Button).",')
rep('"authentication/presentation/components/AuthComponents.kt": "Komponen bersama layar autentikasi: AuthScreenContainer, MessageBanner, LoadingButton, AuthTextField, PasswordField, PinField, DemoOtpCard.",',
    '"authentication/presentation/components/AuthComponents.kt": "Komponen bersama layar autentikasi: AuthScreenContainer (logo, garis emas, judul), MessageBanner, LoadingButton, AuthTextField, PasswordField, PinField, DemoOtpCard, serta PinDots dan PinKeypad untuk layar PIN.",')
rep('"authentication/presentation/screen/LoginScreen.kt": "Layar login (password dan biometrik simulasi) serta tautan registrasi dan pemulihan.",',
    '"authentication/presentation/screen/LoginScreen.kt": "Layar login (tombol Masuk dan Masuk dengan biometrik simulasi) serta tautan registrasi dan pemulihan; teks akun demo hanya pada varian demo.",')
rep('"authentication/presentation/screen/PinScreens.kt": "CreatePinScreen (membuat PIN) dan EnterPinScreen (membuka aplikasi setelah login).",',
    '"authentication/presentation/screen/PinScreens.kt": "CreatePinScreen (membuat PIN) dan EnterPinScreen (membuka aplikasi setelah login: enam titik dan keypad angka, PIN dikirim otomatis pada digit keenam).",')
rep('profile, …) dan tab bawah.",',
    'profile, …) dan MainBottomBar: lima tab (Beranda, Riwayat, QRIS di tengah, Kantong, Profil).",')
rep('"core/ui/Components.kt": "Komponen UI bersama: ScreenScaffold, SectionCard, InfoRow, ErrorText, InfoText, StatusChip, SignedAmount, PinDialog, MoneyField, DividerLine, EmptyState, HighlightCard.",',
    '"core/ui/Components.kt": "Komponen UI bersama: ScreenScaffold (app bar rata), SectionCard (panel bergaris tipis tanpa bayangan), InfoRow, ErrorText, InfoText, StatusChip, SignedAmount (dana masuk biru), PinDialog, MoneyField, DividerLine, EmptyState, HighlightCard, ddbFieldColors.",')
rep('"home/HomeScreen.kt": "Dashboard utama: saldo (bisa disembunyikan), aksi cepat, ringkasan bulan ini, notifikasi terbaru; meminta izin notifikasi Android 13+.",',
    '"home/HomeScreen.kt": "Dashboard utama: header (logo, notifikasi, profil), sapaan, saldo sebagai fokus (bisa disembunyikan), aksi cepat berikon garis, ringkasan bulan ini, transaksi terakhir; meminta izin notifikasi Android 13+.",')
rep('"ui/theme/Color.kt": "Palet warna aplikasi (terang dan gelap).",',
    '"ui/theme/Color.kt": "Palet merek: Midnight Navy #0B1D33, Diponegoro Blue #0B3D91, Muted Gold #B69A62, warna latar/garis mode terang dan gelap, warna grafik dan status.",')
rep('"ui/theme/Type.kt": "Tipografi Material 3.",',
    '"ui/theme/Type.kt": "Tipografi Material 3 dengan Plus Jakarta Sans (bobot 400/500/600; font di res/font, lisensi OFL di app/licenses); angka tabular untuk nominal.",')

# Jumlah berkas
rep('if (total !== 110) throw new Error("expected 110 files, got " + total);',
    'if (total !== 112) throw new Error("expected 112 files, got " + total);')
rep('kode bersama kedua varian (110 berkas Kotlin)', 'kode bersama kedua varian (112 berkas Kotlin)')
rep('["UI", "Jetpack Compose + Material 3, Navigation Compose"],',
    '["UI", "Jetpack Compose + Material 3 (tema DDB, font Plus Jakarta Sans), Navigation Compose"],')

# Bab V navigasi
rep('P("AuthNavGraph adalah graf navigasi tunggal. Setelah login dan PIN berhasil, pengguna masuk ke rute fitur pada AppRoutes dengan bilah navigasi bawah (MainBottomBar)."),',
    'P("AuthNavGraph adalah graf navigasi tunggal. Setelah login dan PIN berhasil, pengguna masuk ke rute fitur pada AppRoutes dengan bilah navigasi bawah (MainBottomBar) berisi lima tab: Beranda, Riwayat, QRIS (tombol bulat di tengah), Kantong, dan Profil. Layar PIN setelah login memakai keypad angka; PIN dikirim otomatis saat digit keenam diketuk. Dialog PIN transaksi (PinDialog) tetap memakai kolom teks."),')

# Pola pengembangan: tombol dan tema
rep('"Buat composable layar di paket presentation modul, memakai ScreenScaffold dan komponen di core/ui/Components.kt agar tampilan seragam.",',
    '"Buat composable layar di paket presentation modul, memakai ScreenScaffold dan komponen di core/ui/Components.kt agar tampilan seragam. Untuk tombol, impor import com.diponegoro.digitalbank.core.ui.DdbButton as Button (atau DdbOutlinedButton as OutlinedButton); warna diambil dari MaterialTheme.colorScheme dan DdbTheme.colors, bukan kode warna langsung.",')

# Bab VIII pengujian
rep('# 63 test (1 test live dilewati), ±1 menit', '# 64 test (1 test live dilewati), ±2 menit')
rep('# 58 test (tanpa AppTourTest)', '# 58 test (tanpa test testDemo; LiveBackendTest dilewati bila -Pddb.liveUrl tidak diisi)')
rep('["AppTourTest (testDemo)", "5", "Robolectric + Compose: menelusuri layar utama varian demo"],',
    '["AppTourTest (testDemo)", "6", "Robolectric + Compose: AppTourTest (tur layar utama varian demo), DarkModeShotsTest (login, PIN, Beranda mode gelap), ReceiptAndDialogShotsTest (dialog PIN, bukti transaksi); tangkapan layar di app/build/screenshots"],')
rep('(11 berkas test utama + 1 berkas testDemo, ±1.091 baris)', '(11 berkas test utama + 1 berkas testDemo, ±1.327 baris)')
rep('test UI Robolectric khusus demo (AppTourTest)', 'test UI Robolectric khusus demo (AppTourTest, DarkModeShotsTest)')

open(p, 'w', encoding='utf-8').write(s)
print('ok')
