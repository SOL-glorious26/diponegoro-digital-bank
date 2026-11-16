"""D01 PRJ-DDB-001 v0.9.3 -> v0.9.4 (patch XML langsung): angka disamakan dengan STD-DDB-001 v0.9.6 per 16 Nov 2026."""
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
simple('PRJ-DDB-001 / 0.9.3', 'PRJ-DDB-001 / 0.9.4')
set_row(lambda t: len(t) == 2 and t[0] == 'Status', {1: 'Draf revisi – status per 16 November 2026 (hasil uji sistem STD-DDB-001 v0.9.6, termasuk uji ponsel fisik); menunggu review Steven Wibisana dan persetujuan Project Manager'}, 'status')
set_row(lambda t: len(t) == 2 and t[0] == 'Dokumen acuan', {1: 'SRS-DDB-001 v0.1; Project Brief & Scope Document v1.0; MDTA-DDB-001 v0.1; DWP-DDB-001 v1.3; SAD-DDB-001 v0.9.5; FSD-DDB-001 v0.9.3; STD-DDB-001 v0.9.6; User Interface Map (Web) v0.3; repositori kode per 16 November 2026'}, 'acuan')
insert_row_after(lambda t: len(t) == 4 and t[0] == '0.9.3', ['0.9.4', '16 Nov 2026', 'Salomo P.G. Hokoyoku',
    'Angka disamakan dengan STD-DDB-001 v0.9.6: uji ponsel fisik 14 Nov lulus (Android 28/28, NF-07), Web 15/15, 47 test backend, NFR 8/9, seluruh 11 defect ditutup; PostgreSQL lokal berjalan; Gantt (uji ponsel fisik selesai), Tabel 1.4, 1.6, 2.1, 3.2, bagian 3.7, register risiko (R5 dan R16 termitigasi), dan Bab VI diperbarui; penulisan Android 17 (API 37) diperbaiki; acuan DWP v1.3.'], 'revisi 0.9.4')
for f in glob.glob(os.path.join(BASE, 'word', 'footer*.xml')):
    s = open(f, encoding='utf-8').read()
    if '0.9.3' in s:
        open(f, 'w', encoding='utf-8', newline='').write(s.replace('0.9.3', '0.9.4')); done.append('footer ' + os.path.basename(f))

# ---------- Tabel 1.4 ----------
set_row(lambda t: len(t) == 3 and t[0] == 'Lapisan' and t[2] == 'Status per 12 Nov 2026', {2: 'Status per 16 Nov 2026'}, 'T1.4 header')
set_row(lambda t: len(t) == 3 and t[0] == 'Aplikasi Android', {2: '8 modul; varian demo dan online; 28/28 kasus uji sistem lulus (emulator dan ponsel fisik)'}, 'T1.4 android')
set_row(lambda t: len(t) == 3 and t[0] == 'Aplikasi Web', {2: 'Terhubung ke backend; 15/15 kasus uji Web lulus di Chrome, Edge, Firefox'}, 'T1.4 web')
set_row(lambda t: len(t) == 3 and t[0] == 'API Gateway', {2: 'Konfigurasi tersedia; belum diuji (Docker belum terpasang)'}, 'T1.4 gateway')
set_row(lambda t: len(t) == 3 and t[0] == 'Backend', {2: 'Delapan modul selesai; 73 operasi API; 47 test lulus; cakupan baris 92,2% (JaCoCo)'}, 'T1.4 backend')
set_row(lambda t: len(t) == 3 and t[0] == 'Basis data', {2: 'Skema V1 (17 tabel); profil dev berjalan di PostgreSQL 18 lokal sejak 13 Nov 2026'}, 'T1.4 db')

# ---------- 1.9 dan Tabel 1.6 ----------
n19 = x.count('1.9 Status Pengembangan per 12 November 2026')
x = x.replace('1.9 Status Pengembangan per 12 November 2026', '1.9 Status Pengembangan per 16 November 2026'); done.append('judul 1.9 (%d tempat)' % n19)
set_row(lambda t: len(t) == 4 and t[0] == 'Android', {
    2: '63 test demo dan 58 test online lulus; 28 dari 28 kasus uji sistem lulus: emulator, dan SA-08 (pindai QRIS dengan kamera) di ponsel fisik Realme RMX3867, Android 16, 14 Nov 2026 (STD-DDB-001 v0.9.6); APK rilis bertanda tangan diuji ulang',
    3: 'Integrasi BiometricPrompt (login biometrik masih disimulasikan)'}, 'T1.6 android')
set_row(lambda t: len(t) == 4 and t[0] == 'Backend', {
    2: '47 test integrasi lulus; cakupan baris 92,2% (JaCoCo); openapi.json dan API.md; profil dev berjalan di PostgreSQL lokal; CORS dikonfigurasi (DEF-04 ditutup)',
    3: 'Uji rate limit melalui gateway (NF-04) setelah Docker terpasang'}, 'T1.6 backend')
set_row(lambda t: len(t) == 4 and t[0] == 'Web', {
    2: '38 uji kontrak API; 15/15 kasus uji Web; Chrome, Edge, Firefox; 4/4 skenario lintas platform Android–Web (STD-DDB-001 v0.9.6); User Interface Map (Web) v0.3',
    3: 'Review kode oleh tim Web; telaah draf Detailed Design Web dan Developer Manual Web (v0.5) oleh PIC; User Manual Web (Dokumen 9)'}, 'T1.6 web')
set_row(lambda t: len(t) == 4 and t[0] == 'Dokumentasi', {
    1: 'DWP-DDB-001 v1.3; draf Dokumen 1, 2, 3, 4 (Backend & Database, Web), 5 (UI Map Web), 6, dan 7 (Android, Backend, Web); Panduan Uji Aplikasi di Ponsel (TUT-DDB-001-AND) dan Panduan Menjalankan Web (TUT-DDB-002-WEB)',
    3: 'Dokumen 4 (Android), 5 (Android), 8, dan 9 sesuai jadwal DWP; finalisasi seluruh dokumen ke v1.0'}, 'T1.6 dok')

# ---------- Tabel 2.1 ----------
set_row(lambda t: len(t) == 6 and t[1] == 'Pengembangan aplikasi Web', {
    5: 'apps/web terhubung ke backend (11 Nov 2026); 15/15 kasus uji Web lulus (STD-DDB-001 v0.9.6)'}, 'T2.1 web')
set_row(lambda t: len(t) == 8 and t[0] == 'R11', {
    5: 'Aplikasi Web diimplementasikan PM mengikuti SAD-DDB-001 Bab VIII dan terhubung ke backend pada 11 Nov; 15/15 kasus uji Web lulus; rencana cadangan (3.7) tidak diperlukan'}, 'R11')
set_row(lambda t: len(t) == 6 and t[1].startswith('Pengujian sistem (emulator'), {
    5: 'STD-DDB-001 v0.9.5: Android 27/28 di emulator, Web 15/15, lintas platform 4/4; 11 defect, seluruhnya ditutup'}, 'T2.1 uji')
set_row(lambda t: len(t) == 6 and t[1] == 'Uji ponsel fisik dan laporan Dokumen 6', {
    1: 'Uji ponsel fisik (SA-08, NF-07)', 2: '14 Nov', 3: '14 Nov', 4: 'Selesai',
    5: 'STD-DDB-001 v0.9.6: Realme RMX3867 (Android 16); Android 28/28; NF-07 lulus'}, 'T2.1 ponsel')

# ---------- Bab III ----------
simple('Pixel 8 (Android 37)', 'Pixel 8 (Android 17, API 37)', 2)
set_row(lambda t: len(t) == 3 and t[0] == 'Android', {1: 'Emulator Pixel 8 (Android 17, API 37) dan ponsel fisik Realme RMX3867 (Android 16)'}, 'T3.2 android')
set_row(lambda t: len(t) == 3 and t[0] == 'Test otomatis backend', {2: '47 test integrasi API (±68 detik)'}, 'T3.2 backend')
para('Desain kasus uji dan sebagian besar eksekusi pengujian sistem selesai lebih awal',
     'Desain kasus uji dan eksekusi pengujian sistem selesai lebih awal, pada 11–14 November 2026, dengan bantuan otomasi (folder pengujian/). Hasil pada STD-DDB-001 v0.9.6: 47 test integrasi backend dan 63/58 test Android lulus; cakupan baris test backend 92,2% (JaCoCo); 28 dari 28 kasus uji sistem Android lulus (emulator, dan SA-08 pindai QRIS dengan kamera di ponsel fisik Realme RMX3867, Android 16, pada 14 November 2026); 15 dari 15 kasus uji Web; 4 dari 4 skenario lintas platform Android–Web; alur inti Web lulus di Chrome, Edge, dan Firefox; 8 dari 9 kasus non-fungsional lulus, termasuk performa dasar NFR-01 sampai NFR-03. NF-04 (rate limit di gateway) terblokir sampai Docker terpasang. Pengujian menemukan 11 defect, termasuk satu Kritis dan tiga Tinggi; seluruhnya sudah diperbaiki dan diuji ulang. Tahap yang tersisa adalah review Dokumen 6 oleh PIC (Steven) dan laporan final pada 22 November 2026.')

# ---------- Bab IV ----------
para('Manajemen risiko dilakukan dalam empat langkah',
     'Manajemen risiko dilakukan dalam empat langkah: identifikasi dari Project Brief §12 dan kondisi proyek terkini; analisis dengan skala peluang dan dampak 1–3 (skor = peluang × dampak); mitigasi dengan tindakan dan pemilik yang jelas; serta pemantauan yang ditinjau Project Manager setiap minggu sampai proyek selesai. Risiko R1–R9 berasal dari Project Brief. R10–R17 adalah risiko baru yang teridentifikasi per 11 November 2026. Nilai P dan D pada Tabel 4.1 adalah penilaian per 16 November 2026: untuk risiko berstatus Termitigasi atau Terkendali, nilai itu adalah skor residual setelah mitigasi; untuk risiko berstatus Terbuka, nilai itu adalah skor saat ini sebelum mitigasi selesai.')
set_row(lambda t: len(t) == 8 and t[0] == 'R5', {2: '1', 4: '2', 5: 'Paginasi, notifikasi asinkron; NF-01 sampai NF-03 lulus (STD-DDB-001 v0.9.6)', 7: 'Termitigasi'}, 'R5')
set_row(lambda t: len(t) == 8 and t[0] == 'R13', {5: 'Alternatif run-local.ps1 (H2 atau PostgreSQL lokal); compose dan rate limit (NF-04) diuji setelah Docker terpasang'}, 'R13')
set_row(lambda t: len(t) == 8 and t[0] == 'R16', {1: 'Uji di ponsel fisik (kamera QRIS, push notification, berbagi berkas)', 2: '1', 4: '2',
    5: 'Diuji pada ponsel fisik Realme RMX3867 (Android 16), 14 Nov 2026: pindai QRIS dengan kamera, notifikasi push, dan berbagi bukti lulus; varian demo tetap cadangan demo', 7: 'Termitigasi'}, 'R16')
para('Risiko dengan skor 9 (R10) dan skor 6 (R1, R6) menjadi prioritas.',
     'Risiko dengan skor 9 (R10) dan skor 6 (R1, R6) menjadi prioritas. Untuk R10, ketergantungan pada satu orang dikurangi melalui dokumentasi backend yang dapat diikuti anggota lain dan peer review oleh Steven. R11 termitigasi setelah aplikasi Web diimplementasikan dan terhubung ke backend pada 11 November 2026, sehingga rencana cadangan pada bagian 3.7 tidak diperlukan; R8 termitigasi setelah skenario lintas platform LP-01 sampai LP-04 lulus. R12 telah termitigasi karena integrasi Android–backend selesai pada 11 November 2026. R5 termitigasi karena pengukuran performa dasar NF-01 sampai NF-03 memenuhi target, dan R16 termitigasi setelah uji di ponsel fisik pada 14 November 2026 lulus. Temuan dari integrasi: Android 17 memblokir akses aplikasi ke backend di jaringan lokal tanpa izin ACCESS_LOCAL_NETWORK; izin tersebut kini diminta saat aplikasi varian online dibuka.')

# ---------- Penutup dan referensi ----------
para('Dokumen ini merangkum deskripsi, jadwal, rencana pengujian, risiko, dan estimasi biaya proyek Diponegoro Digital Bank per 12 November 2026.',
     'Dokumen ini merangkum deskripsi, jadwal, rencana pengujian, risiko, dan estimasi biaya proyek Diponegoro Digital Bank per 16 November 2026. Backend, aplikasi Android, dan aplikasi Web telah mencapai cakupan fungsional SRS dan terhubung ke satu backend bersama. Pengujian sistem telah dijalankan pada emulator, ponsel fisik, dan peramban dengan hasil 28 dari 28 kasus Android, 15 dari 15 kasus Web, dan 4 dari 4 skenario lintas platform lulus, serta seluruh 11 defect ditutup. Pekerjaan yang masih menentukan keberhasilan sampai 23 November 2026 adalah API Gateway dan deployment (termasuk NF-04), serta penyelesaian dokumen yang tersisa sesuai DWP. Dokumen ini diperbarui setiap kali status proyek berubah dan dicatat pada revision control.')
para('Kelompok Jaran Goyang. (2026). Software Engineering Documentation Work Plan (DWP-DDB-001) v1.0.',
     'Kelompok Jaran Goyang. (2026). Software Engineering Documentation Work Plan (DWP-DDB-001) v1.3.')

open(P, 'w', encoding='utf-8', newline='').write(x)

shutil.copy(os.path.join(FIG, 'd1_gantt.png'), os.path.join(BASE, 'word', 'media', 'image2.png'))
shutil.copy(os.path.join(FIG, 'd1_vmodel.png'), os.path.join(BASE, 'word', 'media', 'image3.png'))
shutil.copy(os.path.join(FIG, 'd1_riskmap.png'), os.path.join(BASE, 'word', 'media', 'image4.png'))
done.append('gambar gantt + vmodel + riskmap')
print(len(done), 'perubahan'); print('\n'.join(done))
