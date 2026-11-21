"""D01 PRJ-DDB-001 v0.9.4 -> v0.9.5 (patch XML langsung): angka uji Android disamakan dengan STD-DDB-001 v0.9.7 (21 Nov 2026).
Pemakaian: python patch_d01_095.py <folder docx yang sudah diekstrak>
"""
import re, sys, os, glob
sys.stdout.reconfigure(encoding="utf-8")
BASE = sys.argv[1]
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
    x = x.replace(a, b); done.append('replace ' + a[:40])

# ---------- versi dan kontrol dokumen ----------
simple('PRJ-DDB-001 / 0.9.4', 'PRJ-DDB-001 / 0.9.5')
set_row(lambda t: len(t) == 2 and t[0] == 'Status', {1: 'Draf revisi – status per 21 November 2026 (hasil uji STD-DDB-001 v0.9.7: regresi setelah pembaruan tampilan Android; uji sistem termasuk ponsel fisik); menunggu review Steven Wibisana dan persetujuan Project Manager'}, 'status')
set_row(lambda t: len(t) == 2 and t[0] == 'Dokumen acuan', {1: 'SRS-DDB-001 v0.1; Project Brief & Scope Document v1.0; MDTA-DDB-001 v0.1; DWP-DDB-001 v1.4; SAD-DDB-001 v0.9.5; FSD-DDB-001 v0.9.3; STD-DDB-001 v0.9.7; DVM-DDB-001-AND v0.9.3; User Interface Map (Web) v0.3; repositori kode per 21 November 2026'}, 'acuan')
insert_row_after(lambda t: len(t) == 4 and t[0] == '0.9.4', ['0.9.5', '21 Nov 2026', 'Salomo P.G. Hokoyoku',
    'Angka uji Android disamakan dengan STD-DDB-001 v0.9.7: setelah pembaruan tampilan Android (21 Nov), test otomatis 64 demo (63 lulus, 1 dilewati) dan 58 online (57 lulus, 1 dilewati); 28/28 kasus uji sistem berasal dari build sebelum pembaruan tampilan, uji ulang dijadwalkan; Tabel 1.4, 1.6, bagian 3.7, dan Bab VI diperbarui; acuan DWP v1.4 dan DVM-DDB-001-AND v0.9.3.'], 'revisi 0.9.5')
for f in glob.glob(os.path.join(BASE, 'word', 'footer*.xml')):
    s = open(f, encoding='utf-8').read()
    if '0.9.4' in s:
        open(f, 'w', encoding='utf-8', newline='').write(s.replace('0.9.4', '0.9.5')); done.append('footer ' + os.path.basename(f))

# ---------- 1.9 Status Pengembangan (judul + daftar isi) ----------
n19 = x.count('1.9 Status Pengembangan per 16 November 2026')
x = x.replace('1.9 Status Pengembangan per 16 November 2026', '1.9 Status Pengembangan per 21 November 2026'); done.append('judul 1.9 (%d tempat)' % n19)

# ---------- Tabel 1.4 ----------
set_row(lambda t: len(t) == 3 and t[0] == 'Lapisan' and t[2] == 'Status per 16 Nov 2026', {2: 'Status per 21 Nov 2026'}, 'T1.4 header')
set_row(lambda t: len(t) == 3 and t[0] == 'Aplikasi Android',
        {2: '8 modul; varian demo dan online; tampilan diperbarui 21 Nov (tema DDB, Login, PIN dengan keypad, Beranda); 64/58 test otomatis (demo/online) lulus kecuali 1 test live dilewati; 28/28 kasus uji sistem lulus pada build sebelumnya (emulator dan ponsel fisik)'}, 'T1.4 android')

# ---------- Tabel 1.6 (baris Android) ----------
set_row(lambda t: len(t) == 4 and t[0] == 'Android' and t[2].startswith('63 test demo'),
        {1: '8 modul (FR-01–FR-25, FR-27–FR-29); varian demo dan online; integrasi REST ke backend selesai 11 Nov; tampilan diperbarui 21 Nov (tema Midnight Navy, font Plus Jakarta Sans, ikon dari logo asli, Login, PIN dengan keypad, Beranda, bottom navigation lima tab)',
         2: '64 test demo (63 lulus, 1 dilewati) dan 58 test online (57 lulus, 1 dilewati) setelah pembaruan tampilan, 21 Nov 2026; 28 dari 28 kasus uji sistem lulus pada build sebelum pembaruan tampilan: emulator, dan SA-08 (pindai QRIS dengan kamera) di ponsel fisik Realme RMX3867, Android 16, 14 Nov 2026 (STD-DDB-001 v0.9.7); APK rilis bertanda tangan diuji ulang'}, 'T1.6 android')

# ---------- acuan STD di tabel lain ----------
simple('4/4 skenario lintas platform Android–Web (STD-DDB-001 v0.9.6)', '4/4 skenario lintas platform Android–Web (STD-DDB-001 v0.9.7)')
simple('15/15 kasus uji Web lulus (STD-DDB-001 v0.9.6)', '15/15 kasus uji Web lulus (STD-DDB-001 v0.9.7)')
simple('NF-01 sampai NF-03 lulus (STD-DDB-001 v0.9.6)', 'NF-01 sampai NF-03 lulus (STD-DDB-001 v0.9.7)')

# ---------- 3.7 hasil pengujian ----------
para('Desain kasus uji dan eksekusi pengujian sistem selesai lebih awal',
     'Desain kasus uji dan eksekusi pengujian sistem selesai lebih awal, pada 11–14 November 2026, dengan bantuan otomasi (folder pengujian/). Hasil pada STD-DDB-001 v0.9.7: 47 test integrasi backend lulus; test otomatis Android dijalankan ulang pada 21 November 2026 setelah pembaruan tampilan Android, dengan 63 dari 64 test varian demo dan 57 dari 58 test varian online lulus (masing-masing 1 test live dilewati); cakupan baris test backend 92,2% (JaCoCo); 28 dari 28 kasus uji sistem Android lulus pada build sebelum pembaruan tampilan (emulator, dan SA-08 pindai QRIS dengan kamera di ponsel fisik Realme RMX3867, Android 16, pada 14 November 2026), dan uji ulang pada build baru dijadwalkan; 15 dari 15 kasus uji Web; 4 dari 4 skenario lintas platform Android–Web; alur inti Web lulus di Chrome, Edge, dan Firefox; 8 dari 9 kasus non-fungsional lulus, termasuk performa dasar NFR-01 sampai NFR-03. NF-04 (rate limit di gateway) terblokir sampai Docker terpasang. Pengujian menemukan 11 defect, termasuk satu Kritis dan tiga Tinggi; seluruhnya sudah diperbaiki dan diuji ulang. Tahap yang tersisa adalah uji ulang SA dan LP pada build Android terbaru, review Dokumen 6 oleh PIC (Steven), dan laporan final pada 22 November 2026.',
     '3.7 hasil')

# ---------- Bab VI ----------
para('Dokumen ini merangkum deskripsi, jadwal, rencana pengujian, risiko, dan estimasi biaya proyek Diponegoro Digital Bank per 16 November 2026.',
     'Dokumen ini merangkum deskripsi, jadwal, rencana pengujian, risiko, dan estimasi biaya proyek Diponegoro Digital Bank per 21 November 2026. Backend, aplikasi Android, dan aplikasi Web telah mencapai cakupan fungsional SRS dan terhubung ke satu backend bersama. Pengujian sistem telah dijalankan pada emulator, ponsel fisik, dan peramban dengan hasil 28 dari 28 kasus Android, 15 dari 15 kasus Web, dan 4 dari 4 skenario lintas platform lulus, serta seluruh 11 defect ditutup. Tampilan aplikasi Android diperbarui pada 21 November 2026 tanpa mengubah logika; test otomatis Android lulus setelah pembaruan tersebut, sedangkan kasus uji sistem Android dan lintas platform dijadwalkan untuk dijalankan ulang pada build terbaru. Pekerjaan yang masih menentukan keberhasilan sampai 23 November 2026 adalah API Gateway dan deployment (termasuk NF-04), serta penyelesaian dokumen yang tersisa sesuai DWP. Dokumen ini diperbarui setiap kali status proyek berubah dan dicatat pada revision control.',
     'Bab VI')

left = [m for m in re.findall(r'STD-DDB-001 v0\.9\.6', plain(x))]
open(P, 'w', encoding='utf-8', newline='').write(x)
print(len(done), 'perubahan'); print('\n'.join(done))
print('sisa "STD-DDB-001 v0.9.6" (seharusnya hanya di riwayat revisi):', len(left))
