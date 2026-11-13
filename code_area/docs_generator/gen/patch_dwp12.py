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

# 1. Cover version
x = x.replace('1.1 / Baseline Rencana Kerja Dokumentasi (revisi)', '1.2 / Baseline Rencana Kerja Dokumentasi (revisi)', 1); done('cover')

# 2. Revision control: duplicate 1.1 row -> 1.2
for m in rows():
    r = m.group(0)
    t = [plain(c.group(0)).strip() for c in cells(r)]
    if len(t) == 4 and t[0] == '1.1' and t[2] == 'Project Manager':
        new = r
        cs = cells(new)
        # rebuild from last cell to first to keep offsets valid
        vals = ['1.2', '11 Nov 2026', 'Project Manager',
                'Pemutakhiran status berbasis bukti: D01 v0.9.3 (review Steven diterapkan; status Web, hasil uji, dan COCOMO diperbarui), D02 v0.9.4 (arsitektur Web ditetapkan PM dan diimplementasikan), D03, D04B, D07B draf v0.9.1, '
                'D07A draf v0.9.2, D06 draf v0.9.5 (hasil uji sistem Android, Web, lintas platform, peramban, performa, dan JaCoCo; 11 defect; disusun PM untuk PIC Steven); aplikasi Web diimplementasikan di folder web/ dan terhubung ke backend, D05W User Interface Map (Web) v0.3 dari Nabila; artefak pendukung TUT-DDB-001-AND '
                '(Panduan Uji Aplikasi di Ponsel v1.0) dicatat sebagai masukan D06 dan D09A; keputusan PM atas ruang lingkup Web '
                '(login tanpa OTP, transfer dengan PIN + Idempotency-Key, Virtual Account dihapus, QRIS Web hanya QRIS pribadi, '
                'e-KYC wajib); NIM Grace dikoreksi menjadi 622024010; Lampiran A diperbarui.']
        for i in range(3, -1, -1):
            c = cs[i]
            new = new[:c.start()] + set_text(c.group(0), vals[i]) + new[c.end():]
        x = x[:m.end()] + new + x[m.end():]
        done('revision row 1.2')
        break
else:
    raise SystemExit('rev row not found')

# 3. Footers
for f in glob.glob(os.path.join(BASE, 'word', 'footer*.xml')):
    s = open(f, encoding='utf-8').read()
    s2 = s.replace('Versi 1.1 – Halaman', 'Versi 1.2 – Halaman')
    if s2 != s:
        open(f, 'w', encoding='utf-8', newline='').write(s2); done('footer ' + os.path.basename(f))

# 4. Paragraph before Tabel 3.1
replace_para('Tabel 3.1 adalah Master Documentation Matrix',
    'Tabel 3.1 adalah Master Documentation Matrix (Artefak A). Status per 11 November 2026 (malam): D01 tersedia sebagai draf v0.9.3 '
    'setelah peer review Steven diterapkan; D02 sebagai draf v0.9.4 dengan arsitektur Web yang telah ditetapkan dan diimplementasikan; D03, D04B, dan D07B sebagai draf v0.9.1; D07A sebagai draf v0.9.2; D06 sebagai draf v0.9.5 (hasil uji sistem lengkap kecuali uji di ponsel fisik); dan D05W sebagai '
    'User Interface Map (Web) v0.3 dari Nabila. Volume lain berstatus Belum Dimulai karena belum ada bukti draf. Di luar sembilan kategori, '
    'tersedia artefak pendukung TUT-DDB-001-AND (Panduan Uji Aplikasi di Ponsel v1.1, 20 skenario U-01–U-20) yang menjadi masukan D06 '
    'dan D09A. Status hanya berubah melalui mekanisme pada Bagian 4.4.')

# 5. Master matrix (Tabel 3.1) status column
def mm(no, name, status):
    set_cell(lambda t: len(t) == 7 and t[0] == no and t[1] == name, -1, status, 'matrix ' + name)
mm('1', 'Project Document', 'Draf v0.9.3; review Steven (R1) diterapkan; status 12 Nov; menunggu R2')
mm('2', 'Architectural Design', 'Draf v0.9.4; arsitektur Web ditetapkan PM dan diimplementasikan (Bab VIII); menunggu R2')
mm('3', 'Functional Design', 'Draf v0.9.1; menunggu R2')
mm('4', 'Detailed Design – Backend & Database', 'Draf v0.9.1; menunggu R2 (Steven)')
mm('5', 'User Interface Map – Web', 'Draf v0.3 (PDF, UI-01–UI-21, mengikuti SRS); menunggu R1/R2')
mm('6', 'System Testing (konsolidasi)', 'Draf v0.9.5 disusun PM: Android 27/28, Web 15/15, lintas platform 4/4, NFR 7/9 lulus; 11 defect, seluruhnya ditutup; menunggu review Steven')
mm('7', 'Developer Manual – Android', 'Draf v0.9.2 (termasuk APK rilis); menunggu uji keterikutan bagian QRIS/Profile (Gracia)')
mm('7', 'Developer Manual – Backend', 'Draf v0.9.2; menunggu uji keterikutan setup (Steven)')

# 6. Task statuses (Tabel 3.3)
task('D00-WP2-T01', 'Selesai: PostgreSQL; Web React + JavaScript, Vite, axios, react-router-dom (SAD-DDB-001 v0.9.2 Bab VIII, ADR-11, ADR-12)')
task('D02-WP3-T01', 'Draf selesai (v0.9.2)')
task('D00-WP2-T02', 'Selesai: Android, Backend, dan Web terbukti dengan test (Web: 38 uji kontrak API + uji UI otomatis)')
for t in ['D01-WP1-T01','D01-WP2-T01','D01-WP3-T01','D01-WP4-T01','D01-WP5-T01','D01-WP6-T01','D01-WP7-T01','D01-WP7-T02']:
    task(t, 'Draf selesai (v0.9.3)')
task('D01-WP8-T01', 'R1 Steven selesai dan diterapkan (v0.9.2); diperbarui v0.9.3; menunggu R2')
for t in ['D03-WP1-T01','D03-WP2-T01','D03-WP2-T02','D03-WP3-T01','D03-WP4-T01','D03-WP5-T01']:
    task(t, 'Draf selesai (v0.9.1)')
task('D03-WP6-T01', 'Menunggu review (R2)')
for t in ['D04B-WP1-T01','D04B-WP2-T01','D04B-WP3-T01','D04B-WP4-T01','D04B-WP5-T01']:
    task(t, 'Draf selesai (v0.9.1)')
task('D04B-WP6-T01', 'Menunggu review (R2)')
for t in ['D05W-WP1-T01','D05W-WP1-T02','D05W-WP2-T01','D05W-WP3-T01']:
    task(t, 'Draf v0.3 (Nabila); kode halaman memakai UI-01–UI-21')
task('D05W-WP4-T01', 'Menunggu pre-review (R1)')
for t in ['D07A-WP1-T01','D07A-WP2-T01','D07A-WP3-T02','D07A-WP4-T01']:
    task(t, 'Draf selesai (v0.9)')
task('D07A-WP3-T01', 'Draf awal ditulis Salomo dari kode; menunggu review Gracia')
task('D07A-WP5-T01', 'Menunggu uji keterikutan (Gracia)')
for t in ['D06-WP1-T01','D06-WP1-T02','D06-WP2-T01','D06-WP2-T02','D06-WP2-T03','D06-WP2-T04','D06-WP3-T01']:
    task(t, 'Draf selesai (v0.9)')
task('D06-WP4-T01', 'Selesai di emulator: 27 dari 28 kasus lulus; SA-08 (kamera) menunggu ponsel fisik')
task('D06-WP4-T02', 'Selesai: 15 dari 15 kasus Web lulus; Chrome, Edge, Firefox lulus')
task('D06-WP4-T03', 'Selesai untuk lingkungan lokal: 45/45 test backend lulus; LP-01 sampai LP-04 lulus (emulator + Chrome)')
task('D06-WP5-T01', 'Draf log defect: 11 defect, seluruhnya ditutup dengan retest')
for t in ['D07B-WP1-T01','D07B-WP2-T01','D07B-WP3-T01','D07B-WP4-T01']:
    task(t, 'Draf selesai (v0.9.1)')
task('D07B-WP5-T01', 'Menunggu uji keterikutan setup (Steven)')

# 7. Checklist (Tabel 4.4): cols ID,Volume,PIC,R0,Draf,R1,R2,R3,PM,Kumpul
def tick(vid, upto_cols):
    global x
    for m in rows():
        r = m.group(0)
        cs = cells(r)
        t = [plain(c.group(0)).strip() for c in cs]
        if len(t) == 10 and t[0] == vid and '☐' in t[3]:
            for i in sorted(upto_cols, reverse=True):
                c = cs[i]
                r = r[:c.start()] + c.group(0).replace('☐', '☑') + r[c.end():]
            x = x[:m.start()] + r + x[m.end():]
            done('tick ' + vid)
            return
    raise SystemExit('checklist row not found ' + vid)
tick('D01', [3, 4, 5])
for v in ['D02', 'D03', 'D04B', 'D05W', 'D06', 'D07A', 'D07B']:
    tick(v, [3, 4])

# 8. NIM Grace
assert x.count('6220204010') == 2
x = x.replace('6220204010', '622024010'); done('NIM table')

# 9. Lampiran A updates
def lamp(no, col, text):
    set_cell(lambda t: len(t) == 4 and t[0] == no and ('Daftar anggota' in t[2] or 'Folder proyek' in t[2] or 'MDTA 1.2' in t[2]), col, text, 'lampiran %s col %d' % (no, col))
lamp('6', 1, 'NIM Grace tertulis 6220204010 (10 digit) pada daftar anggota, sedangkan NIM anggota lain 9 digit.')
lamp('6', 3, 'Terverifikasi (11 Nov 2026): NIM Grace adalah 622024010. Tabel 1.2 dan blok kontrol volume memakai nilai ini.')
lamp('11', 1, 'Folder code_area kosong, tetapi kode berada di repositori kerja PM (submit_progress_digital_banking_project): backend 8 modul (45 test lulus, 73 operasi API), aplikasi Android 8 modul dengan varian demo dan online (63 dan 58 test lulus), aplikasi Web (React + Vite) yang terhubung ke backend.')
lamp('11', 3, 'Status implementasi dicatat berbasis bukti pada PRJ-DDB-001 Tabel 1.6. Integrasi Android–backend selesai 11 Nov 2026 (uji end-to-end di emulator); aplikasi Web diimplementasikan di folder web/ dan terhubung ke backend pada 11 Nov 2026 malam.')
lamp('13', 3, 'Basis data ditetapkan PostgreSQL (H2 untuk test). Web ditetapkan PM (11 Nov 2026): React + JavaScript, Vite, axios dengan interceptor token dan Idempotency-Key, react-router-dom dengan route guard login dan e-KYC (SAD-DDB-001 v0.9.2 Bab VIII). Selesai pada D00-WP2-T01.')

# add rows 16, 17 after row 15 of Tabel A.1
for m in rows():
    r = m.group(0)
    t = [plain(c.group(0)).strip() for c in cells(r)]
    if len(t) == 4 and t[0] == '15' and t[2].startswith('Project Brief'):
        newrows = ''
        for vals in [
            ['16', 'Prototipe dan UML Web memuat fitur yang tidak sesuai SRS: login dengan OTP, transfer dengan OTP, dan Virtual Account; e-KYC belum ada.',
             'Jawaban tim Web (11 Nov 2026); User Interface Map (Web) v0.3',
             'Keputusan PM: login tanpa OTP (FR-03), transfer memakai PIN dan header Idempotency-Key (FR-04), Virtual Account dihapus, QRIS Web hanya QRIS pribadi (FR-16), e-KYC wajib di Web (FR-02). D02, D04W, dan D05W mengikuti keputusan ini.'],
            ['17', 'Tersedia artefak di luar sembilan kategori: Panduan Uji Aplikasi di Ponsel (TUT-DDB-001-AND v1.0) dengan 20 skenario U-01–U-20 dan lembar hasil uji.',
             'documents_area (11 Nov 2026)',
             'Dicatat sebagai artefak pendukung, bukan volume baru. Skenario dipakai sebagai masukan D06 (kasus uji Android) dan D09A (langkah pengguna). Belum dicoba di ponsel sungguhan; direvisi setelah eksekusi uji.'],
        ]:
            nr = r
            cs = cells(nr)
            for i in range(3, -1, -1):
                c = cs[i]
                nr = nr[:c.start()] + set_text(c.group(0), vals[i]) + nr[c.end():]
            newrows += nr
        x = x[:m.end()] + newrows + x[m.end():]
        done('lampiran rows 16-17')
        break
else:
    raise SystemExit('row 15 not found')

open(P, 'w', encoding='utf-8', newline='').write(x)
print('total changes', count['n'])
