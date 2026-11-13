import re, sys, glob, os
sys.stdout.reconfigure(encoding="utf-8")
BASE = sys.argv[1]  # unzipped dir
P = os.path.join(BASE, 'word', 'document.xml')
x = open(P, encoding='utf-8').read()

def esc(s):
    return s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')

def plain(frag):
    return ''.join(re.findall(r'<w:t(?: [^>]*)?>([^<]*)</w:t>', frag)).replace('&amp;', '&').replace('&apos;', "'").replace('&quot;', '"')

def set_text(frag, text):
    """Put text into first <w:t> of fragment, empty the others."""
    n = [0]
    def rep(m):
        n[0] += 1
        if n[0] == 1:
            return '<w:t xml:space="preserve">' + esc(text) + '</w:t>'
        return '<w:t xml:space="preserve"></w:t>'
    out = re.sub(r'<w:t(?: [^>]*)?>[^<]*</w:t>', rep, frag)
    assert n[0] >= 1, 'no w:t in fragment'
    return out

count = {'n': 0}
def done(label):
    count['n'] += 1
    print('OK', label)

# ---------- paragraphs ----------
def replace_para(prefix, text):
    global x
    for m in re.finditer(r'<w:p[ >].*?</w:p>', x, flags=re.S):
        frag = m.group(0)
        if '<w:tbl>' in frag:
            continue
        if plain(frag).startswith(prefix):
            x = x[:m.start()] + set_text(frag, text) + x[m.end():]
            done('para ' + prefix[:40])
            return
    raise SystemExit('para not found: ' + prefix)

# ---------- table rows ----------
def rows():
    return list(re.finditer(r'<w:tr[ >].*?</w:tr>', x, flags=re.S))

def cells(rowxml):
    return list(re.finditer(r'<w:tc>.*?</w:tc>', rowxml, flags=re.S))

def set_cell(rowmatch_pred, col, text, label, nth=1):
    """col may be negative. nth: which matching row (1-based)."""
    global x
    k = 0
    for m in rows():
        r = m.group(0)
        cs = cells(r)
        texts = [plain(c.group(0)).strip() for c in cs]
        if rowmatch_pred(texts):
            k += 1
            if k != nth:
                continue
            c = cs[col]
            newc = set_text(c.group(0), text)
            r2 = r[:c.start()] + newc + r[c.end():]
            x = x[:m.start()] + r2 + x[m.end():]
            done(label)
            return
    raise SystemExit('row not found: ' + label)

def task(tid, status):
    set_cell(lambda t: t and t[0] == tid and len(t) >= 10, -1, status, tid + ' -> ' + status)

# ===== v1.3 (13 Nov 2026): D03 v0.9.2, D04B v0.9.2, D07B v0.9.2 =====
x = x.replace('1.2 / Baseline Rencana Kerja Dokumentasi (revisi)', '1.3 / Baseline Rencana Kerja Dokumentasi (revisi)', 1); done('cover')

for m in rows():
    r = m.group(0)
    t = [plain(c.group(0)).strip() for c in cells(r)]
    if len(t) == 4 and t[0] == '1.2' and t[2] == 'Project Manager':
        new = r; cs = cells(new)
        vals = ['1.3', '13 Nov 2026', 'Project Manager',
                'Status volume Salomo disinkronkan: D03 v0.9.2 (halaman Web dan status FR per platform dari hasil uji D06), '
                'D04B v0.9.2 (CORS backend, 47 test, JaCoCo 92,3% baris), D07B v0.9.2. Folder documents_area dirapikan: satu folder per volume, '
                'versi lama dipindah ke _temp_versi_lama.']
        for i in range(3, -1, -1):
            c = cs[i]; new = new[:c.start()] + set_text(c.group(0), vals[i]) + new[c.end():]
        x = x[:m.end()] + new + x[m.end():]
        done('revision row 1.3'); break
else:
    raise SystemExit('rev row 1.2 not found')

for f in glob.glob(os.path.join(BASE, 'word', 'footer*.xml')):
    s = open(f, encoding='utf-8').read()
    s2 = s.replace('Versi 1.2 – Halaman', 'Versi 1.3 – Halaman')
    if s2 != s:
        open(f, 'w', encoding='utf-8', newline='').write(s2); done('footer ' + os.path.basename(f))

replace_para('Tabel 3.1 adalah Master Documentation Matrix',
    'Tabel 3.1 adalah Master Documentation Matrix (Artefak A). Status per 13 November 2026: D01 tersedia sebagai draf v0.9.3 '
    'setelah peer review Steven diterapkan; D02 sebagai draf v0.9.4 dengan arsitektur Web yang telah ditetapkan dan diimplementasikan; D03, D04B, D07A, dan D07B sebagai draf v0.9.2; D06 sebagai draf v0.9.5 (hasil uji sistem lengkap kecuali uji di ponsel fisik); dan D05W sebagai '
    'User Interface Map (Web) v0.3 dari Nabila. Volume lain berstatus Belum Dimulai karena belum ada bukti draf. Di luar sembilan kategori, '
    'tersedia artefak pendukung TUT-DDB-001-AND (Panduan Uji Aplikasi di Ponsel v1.1, 20 skenario U-01–U-20) yang menjadi masukan D06 '
    'dan D09A. Status hanya berubah melalui mekanisme pada Bagian 4.4.')

def mm(no, name, status):
    set_cell(lambda t: len(t) == 7 and t[0] == no and t[1] == name, -1, status, 'matrix ' + name)
mm('3', 'Functional Design', 'Draf v0.9.2; halaman Web dan status FR per platform diperbarui dari hasil uji; menunggu R2')
mm('4', 'Detailed Design – Backend & Database', 'Draf v0.9.2 (CORS, 47 test, JaCoCo 92,3%); menunggu R2 (Steven)')

n = 0
for m in reversed(rows()):
    r = m.group(0); cs = cells(r)
    t = [plain(c.group(0)).strip() for c in cs]
    if len(t) >= 10 and t[0].startswith(('D03-', 'D04B-', 'D07B-')) and t[-1] == 'Draf selesai (v0.9.1)':
        c = cs[-1]
        r2 = r[:c.start()] + set_text(c.group(0), 'Draf selesai (v0.9.2)') + r[c.end():]
        x = x[:m.start()] + r2 + x[m.end():]; n += 1
print('task rows updated:', n)

open(P, 'w', encoding='utf-8', newline='').write(x)
print('total changes', count['n'] + n)
