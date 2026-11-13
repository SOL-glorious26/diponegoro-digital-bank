"""D01 PRJ-DDB-001 v0.9.2 -> v0.9.3 (patch XML langsung; generator doc1.js sudah tidak sinkron sejak v0.9.2)."""
import re, sys, os, glob, shutil
sys.stdout.reconfigure(encoding="utf-8")
BASE, FIG = sys.argv[1], sys.argv[2]
P = os.path.join(BASE, 'word', 'document.xml')
x = open(P, encoding='utf-8').read()

def esc(s): return s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
def plain(frag): return ''.join(re.findall(r'<w:t(?: [^>]*)?>([^<]*)</w:t>', frag)).replace('&amp;', '&').replace('&apos;', "'").replace('&quot;', '"').replace('&lt;', '<').replace('&gt;', '>')
def set_text(frag, text):
    n = [0]
    def rep(m):
        n[0] += 1
        return '<w:t xml:space="preserve">' + (esc(text) if n[0] == 1 else '') + '</w:t>'
    out = re.sub(r'<w:t(?: [^>]*)?>[^<]*</w:t>', rep, frag)
    assert n[0] >= 1
    return out
done = []
def rows(): return list(re.finditer(r'<w:tr[ >].*?</w:tr>', x, flags=re.S))
def cells(r): return list(re.finditer(r'<w:tc>.*?</w:tc>', r, flags=re.S))
def texts(r): return [plain(c.group(0)).strip() for c in cells(r)]

def set_row(pred, values, label):
    """values: dict col->text"""
    global x
    for m in rows():
        r = m.group(0); t = texts(r)
        if pred(t):
            cs = cells(r)
            for i in sorted(values, reverse=True):
                c = cs[i]; r = r[:c.start()] + set_text(c.group(0), values[i]) + r[c.end():]
            x = x[:m.start()] + r + x[m.end():]
            done.append(label); return
    raise SystemExit('row not found: ' + label)

def insert_row_after(pred, values, label):
    global x
    for m in rows():
        r = m.group(0); t = texts(r)
        if pred(t):
            nr = r; cs = cells(nr)
            for i in range(len(cs) - 1, -1, -1):
                c = cs[i]; nr = nr[:c.start()] + set_text(c.group(0), values[i]) + nr[c.end():]
            x = x[:m.end()] + nr + x[m.end():]
            done.append(label); return
    raise SystemExit('row not found: ' + label)

def delete_row(pred, label):
    global x
    for m in rows():
        if pred(texts(m.group(0))):
            x = x[:m.start()] + x[m.end():]; done.append(label); return
    raise SystemExit('row not found: ' + label)

def para(prefix, text, label=None):
    global x
    for m in re.finditer(r'<w:p[ >].*?</w:p>', x, flags=re.S):
        f = m.group(0)
        if '<w:tbl>' in f: continue
        if plain(f).startswith(prefix):
            x = x[:m.start()] + set_text(f, text) + x[m.end():]
            done.append(label or 'para ' + prefix[:30]); return
    raise SystemExit('para not found: ' + prefix)

def simple(a, b, cnt=1):
    global x
    assert x.count(a) == cnt, (x.count(a), a)
    x = x.replace(a, b); done.append('replace ' + a[:30])

# ---------- versi ----------
simple('PRJ-DDB-001 / 0.9.2', 'PRJ-DDB-001 / 0.9.3')
set_row(lambda t: len(t) == 2 and t[0] == 'Status', {1: 'Draf revisi – status per 12 November 2026 (aplikasi Web terhubung, hasil uji sistem STD-DDB-001 v0.9.3); menunggu persetujuan Project Manager'}, 'status')
set_row(lambda t: len(t) == 2 and t[0] == 'Dokumen acuan', {1: 'SRS-DDB-001 v0.1; Project Brief & Scope Document v1.0; MDTA-DDB-001 v0.1; DWP-DDB-001 v1.2; SAD-DDB-001 v0.9.4; STD-DDB-001 v0.9.3; User Interface Map (Web) v0.3; repositori kode per 12 November 2026'}, 'acuan')
insert_row_after(lambda t: len(t) == 4 and t[0] == '0.9.2', ['0.9.3', '12 Nov 2026', 'Salomo P.G. Hokoyoku',
    'Status per 12 Nov 2026: aplikasi Web (apps/web) terhubung ke backend; hasil uji sistem STD-DDB-001 v0.9.3 (Android 27/28 di emulator, Web 12/12, lintas platform 4/4, JaCoCo 92,2%); Gantt dan Tabel 2.1 diperbarui; R8 dan R11 termitigasi, rencana cadangan Web tidak diperlukan; COCOMO II dihitung ulang dengan ukuran Web terukur (16,085 KSLOC, 62,38 PM, ±Rp690 juta).'], 'revisi 0.9.3')
for f in glob.glob(os.path.join(BASE, 'word', 'footer*.xml')):
    s = open(f, encoding='utf-8').read()
    if '0.9.2' in s:
        open(f, 'w', encoding='utf-8', newline='').write(s.replace('0.9.2', '0.9.3')); done.append('footer ' + os.path.basename(f))

# ---------- Tabel 1.4 teknologi ----------
set_row(lambda t: len(t) == 3 and t[0] == 'Aplikasi Web', {1: 'React 18 + JavaScript, Vite 5, react-router-dom 6, axios; folder apps/web', 2: 'Terhubung ke backend; 12/12 kasus uji Web lulus di Chrome, Edge, Firefox'}, 'T1.4 web')
set_row(lambda t: len(t) == 3 and t[0] == 'Aplikasi Android', {2: '8 modul; varian demo dan online; 27/28 kasus uji sistem lulus di emulator'}, 'T1.4 android')
set_row(lambda t: len(t) == 3 and t[0] == 'Backend', {2: 'Delapan modul selesai; 73 operasi API; cakupan test 92,2% (JaCoCo)'}, 'T1.4 backend')
simple('Status per 11 Nov 2026', 'Status per 12 Nov 2026')

para('Pembagian halaman/modul pada tim Web belum dilaporkan',
     'Tim Web melaporkan PIC Sprint 1 pada 11 November 2026 (Surya: Login; Steven: Transfer bagian A; Rafi: Dashboard dan Transfer bagian B; Nabila: Riwayat), tetapi integrasi API belum dimulai. Untuk menjaga jadwal, aplikasi Web kemudian diimplementasikan oleh Project Manager mengikuti SAD-DDB-001 Bab VIII. Anggota tim Web berperan dalam review kode, User Interface Map (Web), pengujian, dan dokumentasi Web (Dokumen 4, 5, 7, 9 Web).')

# ---------- Tabel 1.6 status ----------
set_row(lambda t: len(t) == 4 and t[0] == 'Android', {
    2: '63 test demo dan 58 test online lulus; 27 dari 28 kasus uji sistem lulus di emulator (STD-DDB-001 v0.9.3)',
    3: 'Uji di ponsel fisik (termasuk pindai QRIS dengan kamera, SA-08); APK rilis bertanda tangan'}, 'T1.6 android')
set_row(lambda t: len(t) == 4 and t[0] == 'Backend', {
    2: '45 test integrasi lulus; cakupan baris 92,2% (JaCoCo, 12 Nov 2026); openapi.json dan API.md',
    3: 'Uji pada PostgreSQL lokal; daftar origin CORS (DEF-04)'}, 'T1.6 backend')
set_row(lambda t: len(t) == 4 and t[0] == 'Web', {
    1: 'Aplikasi Web React + Vite (apps/web) terhubung ke backend: registrasi, OTP, e-KYC, PIN, Beranda, Transfer, Top-up, QRIS pribadi, Riwayat dan ekspor, Kantong, Virtual Card, Profil, Notifikasi; peta antarmuka Web 21 halaman (UI-01–UI-21)',
    2: '38 uji kontrak API; 12/12 kasus uji Web; Chrome, Edge, Firefox; 4/4 skenario lintas platform Android–Web (STD-DDB-001 v0.9.3); User Interface Map (Web) v0.3',
    3: 'Review kode oleh tim Web; Developer Manual Web (Dokumen 7) dan User Manual Web (Dokumen 9)'}, 'T1.6 web')
set_row(lambda t: len(t) == 4 and t[0] == 'Dokumentasi', {
    1: 'DWP-DDB-001 v1.2; draf Dokumen 1, 2, 3, 4 (Backend & Database), 5 (UI Map Web), 6, dan 7 (Android, Backend); Panduan Uji Aplikasi di Ponsel (TUT-DDB-001-AND)',
    3: 'Dokumen 4 (Android, Web), 5 (Android), 7 (Web), 8, dan 9 sesuai jadwal DWP'}, 'T1.6 dok')

# ---------- Tabel 2.1 Gantt ----------
set_row(lambda t: len(t) == 6 and t[1] == 'Pengembangan aplikasi Web', {
    3: '11 Nov', 4: 'Selesai', 5: 'apps/web terhubung ke backend (11 Nov 2026); 12/12 kasus uji Web lulus (STD-DDB-001 v0.9.3)'}, 'T2.1 web')
set_row(lambda t: len(t) == 6 and t[1] == 'Pengujian sistem', {
    1: 'Pengujian sistem (emulator, peramban, lintas platform)', 2: '11 Nov', 3: '12 Nov', 4: 'Selesai',
    5: 'STD-DDB-001 v0.9.3: Android 27/28, Web 12/12, lintas platform 4/4, NFR 7/9; 11 defect, 9 ditutup'}, 'T2.1 uji')
insert_row_after(lambda t: len(t) == 6 and t[1].startswith('Pengujian sistem (emulator'), ['12', 'Uji ponsel fisik dan laporan Dokumen 6', '17 Nov', '22 Nov', 'Rencana', 'SA-08 (kamera QRIS), NF-07; review PIC Dokumen 6 (Steven)'], 'T2.1 baru')
set_row(lambda t: len(t) == 6 and t[1] == 'Dokumentasi 9 kategori', {0: '13'}, 'T2.1 renumber')

# ---------- Bab III ----------
set_row(lambda t: len(t) == 4 and t[0] == 'Integrasi', {2: 'Spring Boot Test + H2 (MockMvc); uji klien Android dengan MockWebServer dan uji live ke backend'}, 'T3.1 integrasi')
set_row(lambda t: len(t) == 4 and t[0] == 'Sistem', {2: 'Kasus uji STD-DDB-001 dieksekusi otomatis (folder pengujian/: adb + uiautomator untuk Android, Chrome DevTools/WebDriver BiDi untuk Web) dan manual; emulator, ponsel, peramban'}, 'T3.1 sistem')
set_row(lambda t: len(t) == 3 and t[0] == 'Web', {1: 'Chrome 157, Edge 157, Firefox 156 pada laptop Windows (headless untuk uji otomatis); Vite dev server dengan proxy /api', 2: 'Uji halaman Web, kompatibilitas peramban, dan lintas platform'}, 'T3.2 web')
para('Desain kasus uji dijadwalkan 13–16 November 2026',
     'Desain kasus uji dan sebagian besar eksekusi pengujian sistem selesai lebih awal, pada 11–12 November 2026, dengan bantuan otomasi (folder pengujian/). Hasil pada STD-DDB-001 v0.9.3: 45 test integrasi backend dan 63/58 test Android lulus; cakupan baris test backend 92,2% (JaCoCo); 27 dari 28 kasus uji sistem Android lulus di emulator; 12 dari 12 kasus uji Web; 4 dari 4 skenario lintas platform Android–Web; alur inti Web lulus di Chrome, Edge, dan Firefox; pengukuran performa dasar memenuhi target NFR-01 sampai NFR-03. Pengujian menemukan 11 defect; 9 sudah diperbaiki dan diuji ulang, termasuk dua berkategori Tinggi. Uji di ponsel fisik, termasuk pemindaian QRIS dengan kamera, dijadwalkan 17–21 November 2026 dan laporan final Dokumen 6 pada 22 November 2026.')
para('Rencana cadangan aplikasi Web (R11).',
     'Rencana cadangan aplikasi Web (R11) tidak diperlukan. Cakupan minimum Web (UI-01 Login, UI-07 Dashboard, UI-08 sampai UI-11 Transfer, dan UI-16 Daftar Riwayat Transaksi pada User Interface Map (Web) v0.3) telah terhubung ke backend pada 11 November 2026, sebelum batas 17 November 2026. Konsistensi lintas platform (R8) telah diverifikasi melalui skenario LP-01 sampai LP-04.')

# ---------- Bab IV risiko ----------
set_row(lambda t: len(t) == 8 and t[0] == 'R8', {2: '1', 4: '2', 5: 'Satu backend dan kontrak OpenAPI; uji lintas platform LP-01–LP-04 lulus (12 Nov 2026)', 7: 'Termitigasi'}, 'R8')
set_row(lambda t: len(t) == 8 and t[0] == 'R11', {1: 'Aplikasi Web terlambat (semula masih prototipe dan progres belum dilaporkan)', 2: '1', 4: '3',
    5: 'Aplikasi Web diimplementasikan PM mengikuti SAD-DDB-001 Bab VIII dan terhubung ke backend pada 11 Nov; 12/12 kasus uji Web lulus; rencana cadangan (3.7) tidak diperlukan', 7: 'Termitigasi'}, 'R11')
para('Risiko dengan skor 9 (R10, R11)',
     'Risiko dengan skor 9 (R10) dan skor 6 (R1, R6) menjadi prioritas. Untuk R10, ketergantungan pada satu orang dikurangi melalui dokumentasi backend yang dapat diikuti anggota lain dan peer review oleh Steven. R11 termitigasi setelah aplikasi Web diimplementasikan dan terhubung ke backend pada 11 November 2026, sehingga rencana cadangan pada bagian 3.7 tidak diperlukan; R8 termitigasi setelah skenario lintas platform LP-01 sampai LP-04 lulus. R12 telah termitigasi karena integrasi Android–backend selesai pada 11 November 2026. Temuan dari integrasi: Android 17 memblokir akses aplikasi ke backend di jaringan lokal tanpa izin ACCESS_LOCAL_NETWORK; izin tersebut kini diminta saat aplikasi varian online dibuka.')

# ---------- Bab V COCOMO ----------
para('Estimasi biaya memakai model COCOMO II', 'Estimasi biaya memakai model COCOMO II Post-Architecture (Boehm et al., 2000). Ukuran perangkat lunak diambil dari jumlah baris kode sumber aktual (SLOC: baris non-kosong dan non-komentar, tanpa kode uji dan berkas hasil build) yang dihitung dari repositori pada 12 November 2026. Biaya tenaga kerja diperoleh dari effort (person-month) dikali tarif per person-month. Biaya non-tenaga kerja dan cadangan ditambahkan secara terpisah. Seluruh nilai yang bukan hasil pengukuran ditandai sebagai asumsi.')
set_row(lambda t: len(t) == 4 and t[0] == 'Backend (Java, main)', {2: '4,697'}, 'T5.1 backend')
set_row(lambda t: len(t) == 4 and t[0].startswith('Aplikasi Android (Kotlin'), {2: '9,227'}, 'T5.1 android')
set_row(lambda t: len(t) == 4 and t[0] == 'Aplikasi Web (prototipe JSX)', {0: 'Aplikasi Web (React, JavaScript/JSX; CSS 0,269 KSLOC tidak dihitung)', 1: '19', 2: '1,943'}, 'T5.1 web')
set_row(lambda t: len(t) == 4 and t[0] == 'Subtotal ukuran terealisasi', {0: 'Total ukuran (seluruh komponen terealisasi dan diukur)', 2: '16,085'}, 'T5.1 total')
delete_row(lambda t: len(t) == 4 and t[0].startswith('Sisa penyelesaian aplikasi Web'), 'T5.1 hapus sisa')
delete_row(lambda t: len(t) == 4 and t[0] == 'Total ukuran untuk penyelesaian', 'T5.1 hapus total lama')
set_row(lambda t: len(t) == 5 and t[0] == 'Terealisasi per 11 Nov 2026', {0: 'Terealisasi per 11 Nov 2026 (sebelum aplikasi Web)'}, 'T5.2 lama')
set_row(lambda t: len(t) == 5 and t[0] == 'Penyelesaian (termasuk aplikasi Web)', {0: 'Terealisasi per 12 Nov 2026 (termasuk aplikasi Web)', 1: '16,085', 2: '62,38', 3: '13,66', 4: '4,57'}, 'T5.2 baru')
set_row(lambda t: len(t) == 4 and t[1] == 'Tenaga kerja pengembangan', {2: '62,38 PM × Rp10.000.000', 3: 'Rp623.804.604'}, 'T5.3 tenaga')
set_row(lambda t: len(t) == 4 and t[1] == 'Cadangan risiko (contingency)', {3: 'Rp62.685.460'}, 'T5.3 cadangan')
set_row(lambda t: len(t) == 4 and t[1] == 'Total estimasi biaya proyek', {3: 'Rp689.540.065'}, 'T5.3 total')
set_row(lambda t: len(t) == 3 and t[0] == 'Rp7.500.000', {1: 'Rp467.853.453', 2: 'Rp517.993.798'}, 'T5.4 a')
set_row(lambda t: len(t) == 3 and t[0] == 'Rp10.000.000', {1: 'Rp623.804.604', 2: 'Rp689.540.065'}, 'T5.4 b')
set_row(lambda t: len(t) == 3 and t[0] == 'Rp12.500.000', {1: 'Rp779.755.755', 2: 'Rp861.086.331'}, 'T5.4 c')
para('Tarif tenaga kerja diasumsikan Rp10.000.000 per person-month', 'Tarif tenaga kerja diasumsikan Rp10.000.000 per person-month, setara gaji pengembang tingkat junior di Indonesia. Nilai ini asumsi tim dan belum dirujuk ke survei gaji resmi, sehingga rentang Rp7,5–12,5 juta diuji pada Tabel 5.4. Tabel 5.3 merangkum biaya berdasarkan ukuran terealisasi per 12 November 2026.')
para('Tabel 5.3 Rincian estimasi biaya', 'Tabel 5.3 Rincian estimasi biaya (ukuran terealisasi 12 Nov 2026)')
para('Menurut COCOMO II, proyek dengan ukuran ±18,1 KSLOC',
     'Menurut COCOMO II, proyek dengan ukuran ±16,1 KSLOC memerlukan sekitar 62 person-month dalam ±13,7 bulan. Nilai ini jauh lebih besar daripada kapasitas nyata tim (8 anggota paruh waktu selama ±2,3 bulan). Perbedaan tersebut wajar karena tiga hal. Pertama, COCOMO II dikalibrasi pada proyek industri dengan proses penuh (manajemen konfigurasi, QA formal, dokumentasi produksi). Kedua, sebagian besar fungsionalitas berasal dari framework dan pustaka (Spring Boot, Jetpack Compose, React) serta alat bantu pengembangan modern yang tidak tercermin dalam asumsi driver nominal. Ketiga, integrasi eksternal memakai simulator, bukan sistem produksi. Oleh karena itu, total pada Tabel 5.3, dibulatkan menjadi ±Rp690 juta, dibaca sebagai estimasi biaya bila proyek dikerjakan secara komersial, bukan biaya yang dikeluarkan tim akademik. Ukuran Web yang terukur (1,943 KSLOC) lebih kecil daripada asumsi sebelumnya (±4,000 KSLOC) sehingga estimasi turun dari ±Rp785 juta pada versi 0.9.2. Karena seluruh driver bernilai nominal, presisi sampai satuan rupiah tidak bermakna. Biaya yang benar-benar sudah dikeluarkan tim sampai 12 November 2026 hanya kuota internet anggota (Rp2.400.000). Sewa VPS (Rp450.000) dan domain (Rp200.000), total Rp650.000, masih berupa rencana dan baru terjadi bila deployment cloud dilakukan.')
para('Ukuran total Web (±4,000 KSLOC',
     'Seluruh ukuran kode diukur dari repositori pada 12 November 2026, termasuk aplikasi Web (1,943 KSLOC JavaScript/JSX; stylesheet CSS tidak dihitung) dan perbaikan hasil uji sistem pada aplikasi Android.')

# ---------- judul 1.9 dan catatan Gantt ----------
x = x.replace('1.9 Status Pengembangan per 11 November 2026', '1.9 Status Pengembangan per 12 November 2026'); done.append('judul 1.9 (judul + entri daftar isi)')
para('1.9 Status Pengembangan per 11 November 2026', '1.9 Status Pengembangan per 12 November 2026', 'judul 1.9 (paragraf judul)')
para('Catatan baris 8: rentang 10–11 November', 'Catatan baris 7: rentang 3 Oktober–11 November mencakup prototipe antarmuka tim Web (7 Oktober 2026) dan implementasi aplikasi Web yang terhubung ke backend oleh Project Manager (11 November 2026). Catatan baris 8: rentang 10–11 November adalah periode implementasi dan pengujian backend sampai seluruh test lulus, bukan seluruh proses perancangannya. Pembagian modul, alur bisnis, dan kebutuhan data sudah ditetapkan sejak SRS-DDB-001 dan MDTA-DDB-001 (2–4 Oktober 2026). Aturan bisnis inti (ledger, aturan transfer, simulator eksternal) juga sudah diimplementasikan pada varian demo Android sehingga menjadi acuan langsung. Pengerjaan dipercepat dengan alat bantu pengembangan modern dan kode yang dibangkitkan framework (Spring Boot, Spring Data JPA, OpenAPI).')

# ---------- Penutup ----------
para('Dokumen ini merangkum deskripsi, jadwal, rencana pengujian, risiko, dan estimasi biaya proyek Diponegoro Digital Bank per 11 November 2026.',
     'Dokumen ini merangkum deskripsi, jadwal, rencana pengujian, risiko, dan estimasi biaya proyek Diponegoro Digital Bank per 12 November 2026. Backend, aplikasi Android, dan aplikasi Web telah mencapai cakupan fungsional SRS dan terhubung ke satu backend bersama. Pengujian sistem pada emulator dan peramban telah dijalankan dengan hasil 27 dari 28 kasus Android, 12 dari 12 kasus Web, dan 4 dari 4 skenario lintas platform lulus. Pekerjaan yang masih menentukan keberhasilan sampai 23 November 2026 adalah uji di ponsel fisik, API Gateway dan deployment, serta penyelesaian dokumen yang tersisa sesuai DWP. Dokumen ini diperbarui setiap kali status proyek berubah dan dicatat pada revision control.')

open(P, 'w', encoding='utf-8', newline='').write(x)

# ---------- gambar ----------
shutil.copy(os.path.join(FIG, 'd1_gantt.png'), os.path.join(BASE, 'word', 'media', 'image2.png'))
shutil.copy(os.path.join(FIG, 'd1_riskmap.png'), os.path.join(BASE, 'word', 'media', 'image4.png'))
done.append('gambar gantt + riskmap')
print(len(done), 'perubahan'); print('\n'.join(done))
