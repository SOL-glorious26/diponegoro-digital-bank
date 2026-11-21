"""DWP-DDB-001 v1.3 -> v1.4 (21 Nov 2026): status volume dan angka uji disamakan dengan dokumen terbaru.
Pemakaian: python patch_dwp14.py <folder docx yang sudah diekstrak>
"""
import re, sys, glob, os
sys.stdout.reconfigure(encoding="utf-8")
BASE = sys.argv[1]
P = os.path.join(BASE, 'word', 'document.xml')
x = open(P, encoding='utf-8').read()

def esc(s): return s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
def plain(frag): return ''.join(re.findall(r'<w:t(?: [^>]*)?>([^<]*)</w:t>', frag)).replace('&amp;', '&').replace('&apos;', "'").replace('&quot;', '"')
def set_text(frag, text):
    n = [0]
    def rep(m):
        n[0] += 1
        return '<w:t xml:space="preserve">' + (esc(text) if n[0] == 1 else '') + '</w:t>'
    out = re.sub(r'<w:t(?: [^>]*)?>[^<]*</w:t>', rep, frag)
    assert n[0] >= 1
    return out
log = []
def rows(): return list(re.finditer(r'<w:tr[ >].*?</w:tr>', x, flags=re.S))
def cells(r): return list(re.finditer(r'<w:tc>.*?</w:tc>', r, flags=re.S))
def texts(r): return [plain(c.group(0)).strip() for c in cells(r)]

def set_last(pred, fn, label):
    """Ubah sel terakhir setiap baris yang cocok; fn(teks lama) -> teks baru (None = biarkan)."""
    global x
    n = 0
    for m in reversed(rows()):
        r = m.group(0); t = texts(r)
        if pred(t):
            new = fn(t)
            if new is None or new == t[-1]: continue
            c = cells(r)[-1]
            x = x[:m.start()] + r[:c.start()] + set_text(c.group(0), new) + r[c.end():] + x[m.end():]
            n += 1
    assert n >= 1, 'tidak ada baris: ' + label
    log.append(f'{label}: {n} baris')

def para(prefix, text):
    global x
    for m in re.finditer(r'<w:p[ >].*?</w:p>', x, flags=re.S):
        f = m.group(0)
        if '<w:tbl>' in f: continue
        if plain(f).startswith(prefix):
            x = x[:m.start()] + set_text(f, text) + x[m.end():]; log.append('para ' + prefix[:40]); return
    raise SystemExit('para not found: ' + prefix)

# ---------- versi ----------
assert x.count('1.3 / Baseline Rencana Kerja Dokumentasi (revisi)') == 1
x = x.replace('1.3 / Baseline Rencana Kerja Dokumentasi (revisi)', '1.4 / Baseline Rencana Kerja Dokumentasi (revisi)'); log.append('cover')
for m in rows():
    r = m.group(0); t = texts(r)
    if len(t) == 4 and t[0] == '1.3' and t[2] == 'Project Manager':
        new = r; cs = cells(new)
        vals = ['1.4', '21 Nov 2026', 'Project Manager',
                'Status volume dan angka uji disamakan dengan dokumen terbaru: D01 v0.9.5, D02 v0.9.5, D03 v0.9.3, D06 v0.9.7 '
                '(Android 28/28 termasuk ponsel fisik; test otomatis Android 63/64 dan 57/58 setelah pembaruan tampilan Android 21 Nov; '
                '47 test backend; NFR 8/9), D07A v0.9.3 (tampilan Android baru); D04W dan D07W draf awal v0.5 disusun PM; '
                'D04A, D05A, D05W direview PM; artefak pendukung TUT-DDB-002-WEB v1.0.']
        for i in range(3, -1, -1):
            c = cs[i]; new = new[:c.start()] + set_text(c.group(0), vals[i]) + new[c.end():]
        x = x[:m.end()] + new + x[m.end():]
        log.append('revisi 1.4'); break
else:
    raise SystemExit('rev row 1.3 not found')
for f in glob.glob(os.path.join(BASE, 'word', 'footer*.xml')):
    s = open(f, encoding='utf-8').read()
    s2 = s.replace('Versi 1.3 – Halaman', 'Versi 1.4 – Halaman')
    if s2 != s:
        open(f, 'w', encoding='utf-8', newline='').write(s2); log.append('footer ' + os.path.basename(f))

# ---------- Tabel 3.1 Master Documentation Matrix ----------
def mm(no, name_prefix, status):
    set_last(lambda t: len(t) == 7 and t[0] == no and t[1].startswith(name_prefix), lambda t: status, 'matrix ' + name_prefix)
mm('1', 'Project Document', 'Draf v0.9.5; review Steven (R1) diterapkan; angka disamakan dengan D06 v0.9.7 (21 Nov); menunggu R2')
mm('2', 'Architectural Design', 'Draf v0.9.5; arsitektur Web ditetapkan PM dan diimplementasikan (Bab VIII); menunggu R2')
mm('3', 'Functional Design', 'Draf v0.9.3; uraian workflow dan status FR per platform dari hasil uji; menunggu R2')
mm('4', 'Detailed Design – Android', 'Draf v0.4 (Gracia, documents_area/other_teams); review PM diberikan; revisi berjalan')
mm('4', 'Detailed Design – Web', 'Draf v0.5 (draf awal disusun PM); dilengkapi Rafi; menunggu R1/R2')
mm('5', 'User Interface Map – Android', 'Draf v0.2 (Cecilia, documents_area/other_teams); review PM diberikan; tangkapan layar diambil ulang karena tampilan Android diperbarui 21 Nov')
mm('5', 'User Interface Map – Web', 'Draf v0.3 di documents_area (UI-01–UI-21); v0.6 (PDF) di other_teams sudah direview PM; menunggu versi .docx')
mm('6', 'System Testing', 'Draf v0.9.7 disusun PM: Android 28/28 (emulator dan ponsel fisik, build sebelum pembaruan tampilan), test otomatis Android 63/64 dan 57/58 setelah pembaruan tampilan, Web 15/15, lintas platform 4/4, NFR 8/9; 11 defect, seluruhnya ditutup; uji ulang SA/LP dijadwalkan; menunggu review Steven')
mm('7', 'Developer Manual – Android', 'Draf v0.9.3 (tampilan Android baru, inventaris 112 berkas, APK rilis); menunggu uji keterikutan bagian QRIS/Profile (Gracia)')
mm('7', 'Developer Manual – Web', 'Draf v0.5 (draf awal disusun PM); dilengkapi Steven dan Nabila (komponen UI)')

para('Tabel 3.1 adalah Master Documentation Matrix',
     'Tabel 3.1 adalah Master Documentation Matrix (Artefak A). Status per 21 November 2026: D01 tersedia sebagai draf v0.9.5 '
     'setelah peer review Steven diterapkan; D02 sebagai draf v0.9.5 dengan arsitektur Web yang telah ditetapkan dan diimplementasikan; '
     'D03 sebagai draf v0.9.3; D04B dan D07B sebagai draf v0.9.2; D07A sebagai draf v0.9.3 (tampilan Android diperbarui 21 November 2026); '
     'D06 sebagai draf v0.9.7 (uji sistem Android 28/28 termasuk ponsel fisik, regresi test otomatis setelah pembaruan tampilan, Web 15/15, '
     'lintas platform 4/4, NFR 8/9); D04W dan D07W sebagai draf awal v0.5 yang disusun PM untuk dilengkapi PIC; D04A (v0.4), D05A (v0.2), '
     'dan D05W (v0.3, PDF v0.6) dari anggota tim sudah direview PM. Volume lain berstatus Belum Dimulai karena belum ada bukti draf. '
     'Di luar sembilan kategori, tersedia artefak pendukung TUT-DDB-001-AND (Panduan Uji Aplikasi di Ponsel v1.1, 20 skenario U-01–U-20) '
     'yang menjadi masukan D06 dan D09A, serta TUT-DDB-002-WEB (Panduan Menjalankan dan Menguji Web v1.0). '
     'Status hanya berubah melalui mekanisme pada Bagian 4.4.')

# ---------- Tabel tugas ----------
is_task = lambda vol: (lambda t: len(t) >= 10 and t[0].startswith(vol + '-'))
set_last(is_task('D01'), lambda t: t[-1].replace('(v0.9.3)', '(v0.9.5)').replace('diperbarui v0.9.3', 'diperbarui v0.9.5'), 'D01 tugas')
set_last(is_task('D02'), lambda t: re.sub(r'Draf selesai \(v0\.9(\.\d)?\)', 'Draf selesai (v0.9.5)', t[-1]), 'D02 tugas')
set_last(is_task('D03'), lambda t: t[-1].replace('Draf selesai (v0.9.2)', 'Draf selesai (v0.9.3)'), 'D03 tugas')
set_last(is_task('D07A'), lambda t: t[-1].replace('Draf selesai (v0.9)', 'Draf selesai (v0.9.3)'), 'D07A tugas')
set_last(is_task('D06'), lambda t: t[-1].replace('Draf selesai (v0.9)', 'Draf selesai (v0.9.7)'), 'D06 tugas')
set_last(lambda t: len(t) >= 10 and t[0] == 'D06-WP4-T01', lambda t:
         'Selesai: 28 dari 28 kasus lulus (emulator; SA-08 di ponsel fisik Realme RMX3867, 14 Nov); setelah pembaruan tampilan '
         'Android 21 Nov: test otomatis 63/64 (demo) dan 57/58 (online) lulus, uji ulang SA dijadwalkan', 'D06-WP4-T01')
set_last(lambda t: len(t) >= 10 and t[0] == 'D06-WP4-T03', lambda t:
         'Selesai untuk lingkungan lokal: 47/47 test backend lulus; LP-01 sampai LP-04 lulus (emulator + Chrome); '
         'uji ulang LP pada build Android terbaru dijadwalkan', 'D06-WP4-T03')
review_wp = ('D04W-WP6', 'D07W-WP5')
for vol in ('D04W', 'D07W'):
    set_last(lambda t, v=vol: len(t) >= 10 and t[0].startswith(v + '-') and not t[0].startswith(review_wp) and t[-1] == 'Belum Dimulai',
             lambda t: 'Draf awal v0.5 disusun PM; dilengkapi PIC', vol + ' tugas')

open(P, 'w', encoding='utf-8', newline='').write(x)
print('\n'.join(log)); print(len(log), 'perubahan')
