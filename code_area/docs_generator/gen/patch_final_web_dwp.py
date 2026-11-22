"""Tahap akhir 22 Nov 2026: D04W v0.8 -> v1.0, D08W v0.3 -> v1.0 (tangkapan layar Web), DWP v1.4 -> v1.5.
Pemakaian: python patch_final_web_dwp.py <documents_area> <folder layar Web> <tanda_tangan.png> <folder keluaran>
"""
import copy, os, sys, docx
from docx.shared import Cm, Pt
from docx.enum.text import WD_ALIGN_PARAGRAPH
DA, LAYAR, SIGN, OUT = sys.argv[1:5]


class Doc:
    def __init__(self, p):
        self.d = docx.Document(p); self.n = 0

    def settext(self, p, text):
        runs = [r for r in p.runs if r.text] or p.runs
        if runs:
            runs[0].text = text
            for r in runs[1:]:
                r.text = ''
        else:
            p.add_run(text)
        self.n += 1

    def setcell(self, ce, text):
        self.settext(ce.paragraphs[0], text)
        for p in ce.paragraphs[1:]:
            p._p.getparent().remove(p._p)

    def cell(self, t, r, c, text):
        self.setcell(self.d.tables[t].rows[r].cells[c], text)

    def row(self, t, first, vals, n=1):
        hit = [r for r in self.d.tables[t].rows if r.cells[0].text.strip() == first]
        assert len(hit) == n, (t, first, len(hit))
        for r in hit:
            for c, v in vals.items():
                self.setcell(r.cells[c], v)

    def para(self, start, text):
        hit = [p for p in self.d.paragraphs if p.text.startswith(start)]
        assert len(hit) == 1, (start, len(hit))
        self.settext(hit[0], text)

    def sub(self, a, b):
        k = 0
        for p in list(self.d.paragraphs) + [p for t in self.d.tables for r in t.rows for c in r.cells for p in c.paragraphs]:
            if a in p.text:
                self.settext(p, p.text.replace(a, b)); k += 1
        assert k, a

    def addrow(self, t, vals):
        tb = self.d.tables[t]; tb.rows[-1]._tr.addnext(copy.deepcopy(tb.rows[-1]._tr))
        for c, v in enumerate(vals):
            self.setcell(tb.rows[-1].cells[c], v)

    def approval_after(self, t):
        """Tabel Persetujuan (hanya PM) setelah tabel ke-t."""
        src = self.d.tables[t]
        tb = self.d.add_table(rows=2, cols=4); tb.style = src.style
        for c, h in enumerate(['Peran', 'Nama', 'Tanda tangan', 'Tanggal']):
            tb.rows[0].cells[c].paragraphs[0].add_run(h).bold = True
        for c, v in enumerate(['Disetujui oleh (Project Manager)', 'Salomo Putra Glorious Hokoyoku', '', '22 Nov 2026']):
            tb.rows[1].cells[c].paragraphs[0].add_run(v)
        p = tb.rows[1].cells[2].paragraphs[0]; p.alignment = WD_ALIGN_PARAGRAPH.CENTER; p.add_run().add_picture(SIGN, width=Cm(2.2))
        # garis tabel mengikuti tabel sumber
        pr = src._tbl.tblPr.find(docx.oxml.ns.qn('w:tblBorders'))
        if pr is not None:
            tb._tbl.tblPr.append(copy.deepcopy(pr))
        else:
            from docx.oxml import parse_xml
            tb._tbl.tblPr.append(parse_xml('<w:tblBorders xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">' + ''.join('<w:%s w:val="single" w:sz="4" w:color="808080"/>' % s for s in ('top', 'left', 'bottom', 'right', 'insideH', 'insideV')) + '</w:tblBorders>'))
        head = self.d.add_paragraph(); head.add_run('Persetujuan').bold = True; head.paragraph_format.space_before = Pt(10)
        src._tbl.addnext(tb._tbl); src._tbl.addnext(head._p)

    def save(self, p):
        for q in self.d.paragraphs:
            act = [r for r in q.runs if r.text.strip()]
            if not q.style.name.startswith('Heading') and len(act) == 1 and act[0].bold and len(q.text) > 110:
                act[0].bold = False
        self.d.save(p); print(os.path.basename(p), self.n)


STATUS = 'Final v1.0 – diterbitkan Project Manager 22 November 2026; wewenang review dan persetujuan dijalankan Project Manager'
# ------------------------------------------------------------------ D04W
d = Doc(DA + '/D04W_Detailed_Design_Web/DDB_D04W_Detailed_Design_Web_v0.8.docx')
d.row(0, 'Versi / Status', {1: '1.0 / Final'})
d.row(1, 'Status', {1: STATUS})
d.row(1, 'Kode dokumen / versi', {1: 'SDD-DDB-001-WEB / 1.0'})
d.addrow(2, ['1.0', '22 Nov 2026', 'Salomo P.G. Hokoyoku', 'Versi final; Top Up, ID PLN 0000, rekening tidak valid, dan OTP ubah profil diamati pada aplikasi yang berjalan (tangkapan layar 22 November 2026). Disetujui PM.'])
d.para('Uji alur terhadap backend asli dijalankan', 'Uji alur terhadap backend asli dijalankan pada 22 November 2026 dengan pengujian/web/node-smoke.mjs: WN-01 s.d. WN-14 lulus seluruhnya, dan LPN-01 s.d. LPN-03 (lintas platform dengan Android) lulus. Saat pengambilan tangkapan layar (pengujian/web/node-layar.mjs) juga teramati: Top Up Rp50.000 menambah saldo, ID PLN berakhiran 0000 dan rekening 1400999999 ditolak, filter riwayat bekerja, dan ubah profil meminta OTP; tidak ada error JavaScript. Belum ada: uji antar-peramban, ubah kata sandi, dan pemulihan akun.')
d.row(9, 'Profil', {2: 'WEB-10: permintaan OTP teramati (22 Nov); konfirmasi dan ubah kata sandi belum diuji'})
d.row(9, 'Top Up saldo, QRIS (tampil dan bayar contoh kode)', {2: 'WEB-08: WN-11 (QRIS); Top Up teramati (22 Nov)'})
d.para('Batasan telaah: diagram dibuat', 'Batasan telaah: diagram dibuat dari pembacaan kode; alur inti diverifikasi dengan menjalankan aplikasi dan tampilan 11 halaman diperiksa melalui tangkapan layar di Chrome.')
d.approval_after(2)
d.save(OUT + '/DDB_D04W_Detailed_Design_Web_v1.0.docx')

# ------------------------------------------------------------------ D08W
d = Doc(DA + '/D08W_System_Program_Demo_Web/DDB_D08W_System_Program_Demo_Web_v0.3.docx')
d.row(0, 'Versi / Status', {1: '1.0 / Final'})
d.row(1, '1.0', {1: '22 Nov 2026', 2: 'Salomo P.G. Hokoyoku', 3: 'Versi final; tangkapan layar Web (Bab VI) diambil otomatis pada 22 November 2026 dan Tabel 5.1 diperbarui. Gladi oleh pembawa demo belum dilakukan. Disetujui PM.'})
d.sub('Uji otomatis itu bukan gladi: tampilan tidak diperiksa dan belum ada tangkapan layar Web.', 'Tangkapan layar Web diambil otomatis pada hari yang sama. Uji otomatis itu bukan gladi.')
d.row(2, 'SDD-DDB-001-WEB', {1: 'D04W Detailed Design Web (v1.0)'})
for k, (r, dev) in {'DEM-05': ('Nominal kecil, PIN salah, dan rekening 1400999999 ditolak', 'Tidak ada'), 'DEM-08': ('Riwayat, filter, detail, dan jurnal tampil', 'Tidak ada'), 'DEM-10': ('Pulsa (Rp11.500) dan PLN lulus; ID 0000 ditolak', 'Token PLN di detail belum ditangkap'), 'DEM-11': ('Top Up Rp50.000 menambah saldo; QRIS contoh lulus', 'Tercatat sebagai dana masuk QRIS di backend')}.items():
    d.row(9, k, {3: r, 4: dev})
d.row(11, 'T-04', {1: 'Web Node.js memiliki kasus alur inti (WN) dan tangkapan layar, tetapi belum ada uji antar-peramban dan gladi', 2: 'Perilaku di peramban lain belum terbukti', 3: 'Pakai Chrome saat demo; lakukan gladi'})
d.para('Penting: tangkapan layar Web React', 'Penting: tangkapan layar Web React pada D08 v0.1 dihapus karena bukan bukti Web final. Tangkapan layar di bawah diambil otomatis di Chrome dari Web Node.js yang terhubung ke backend asli pada 22 November 2026 (pengujian/web/node-layar.mjs), bukan dari gladi.')
PICS = [('6.1', ['WEB-01-masuk', 'WEB-06-dashboard'], 'DEM-01: halaman masuk dan Dashboard'), ('6.2', ['WEB-04-ekyc', 'WEB-05-pin'], 'DEM-02: e-KYC dan pembuatan PIN'), ('6.3', ['WEB-07b-transfer-konfirmasi', 'WEB-07c-transfer-hasil'], 'DEM-04: konfirmasi dan detail transfer'),
        ('6.4', ['WEB-09b-antarbank-gagal'], 'DEM-06: transfer antarbank ditolak dan dana kembali'), ('6.5', ['WEB-09c-antarbank-diproses', 'WEB-09d-cek-status'], 'DEM-07: status Diproses dan hasil Cek status'), ('6.6', ['WEB-09-riwayat'], 'DEM-08: riwayat'),
        ('6.7', ['WEB-08c-pulsa-konfirmasi', 'WEB-08f-pln-ditolak'], 'DEM-10: konfirmasi pulsa dan ID PLN 0000 ditolak'), ('6.8', ['WEB-08-topup', 'WEB-08e-topup-hasil'], 'DEM-11: Top Up dan saldo sesudahnya')]
for k, _, _ in PICS:
    d.row(12, k, {3: 'Ada (otomatis, 22 Nov)'})
d.row(12, '6.9', {3: 'Sisi Android: Gambar 6.10'})
anchor = d.d.tables[12]._tbl
for k, files, cap in reversed(PICS):
    c = d.d.add_paragraph(); c.alignment = WD_ALIGN_PARAGRAPH.CENTER; r = c.add_run('Gambar %s %s.' % (k, cap)); r.italic = True; r.font.size = Pt(9)
    p = d.d.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER; p.paragraph_format.keep_with_next = True; p.paragraph_format.space_before = Pt(8)
    tall = any(f in ('WEB-07c-transfer-hasil', 'WEB-09b-antarbank-gagal', 'WEB-09c-antarbank-diproses', 'WEB-09d-cek-status', 'WEB-09-riwayat') for f in files)
    for i, f in enumerate(files):
        if i:
            p.add_run('  ')
        p.add_run().add_picture('%s/%s.png' % (LAYAR, f), width=Cm(6.9 if len(files) == 2 else (8 if tall else 11)))
    anchor.addnext(c._p); anchor.addnext(p._p)
d.approval_after(1)
d.save(OUT + '/DDB_D08W_System_Program_Demo_Web_v1.0.docx')

# ------------------------------------------------------------------ DWP
d = Doc(DA + '/D00_DWP_Work_Plan/DWP-DDB-001_Software_Engineering_Documentation_Work_Plan_v1.4.docx')
for t in d.d.tables[:2]:
    for r in t.rows:
        if r.cells[0].text.strip() == 'Versi / Status':
            d.setcell(r.cells[1], '1.5 / Baseline Rencana Kerja Dokumentasi (revisi)')
        if r.cells[0].text.strip() == 'Tenggat pengumpulan':
            d.setcell(r.cells[1], '23 November 2026 pukul 23.59 WIB (semula 24 November)')
        if '1.4' in r.cells[-1].text and 'DWP-DDB-001' in r.cells[-1].text:
            d.setcell(r.cells[-1], r.cells[-1].text.replace('1.4', '1.5'))
d.addrow(2, ['1.5', '22 Nov 2026', 'Project Manager', 'Tenggat pengumpulan menjadi 23 November 2026 pukul 23.59 WIB; status seluruh volume diperbarui: D01, D02, D03, D04A, D04B, D04W, D05A, D06, D07A, D07B, D07W, D08 (Android dan Web), D09A, D09W final v1.0, D05W v0.7; Web resmi = aplikasi tim Web (Node/Express/EJS); wewenang review dan persetujuan dijalankan Project Manager.'])
d.sub('dalam rentang 11–24 November 2026', 'dalam rentang 11–23 November 2026')
d.para('Menyusun jadwal usulan yang realistis', 'Menyusun jadwal usulan yang realistis terhadap tenggat 23 November 2026 pukul 23.59 WIB.')
d.sub('disusun mundur dari tenggat 24 November 2026 pukul 23.59 WIB', 'disusun mundur dari tenggat semula 24 November 2026 pukul 23.59 WIB; sejak v1.5 tenggat menjadi 23 November 2026 pukul 23.59 WIB')
d.row(6, 'Level 0', {2: d.d.tables[6].rows[1].cells[2].text.replace('24 Nov 2026', '23 Nov 2026')})
MM = {('1', ''): 'Final v1.0 (22 Nov)', ('2', ''): 'Final v1.0 (22 Nov)', ('3', ''): 'Final v1.0 (22 Nov)', ('4', 'Android'): 'Final v1.0 (draf Gracia, diselesaikan PM)', ('4', 'Web'): 'Final v1.0 (Rafi, dikoreksi PM; Web Node)', ('4', 'Backend'): 'Final v1.0 (22 Nov)',
      ('5', 'Android'): 'Final v1.0 (draf Cecilia, diselesaikan PM)', ('5', 'Web'): 'v0.7 (Nabila; Web Node); tangkapan layar sudah tersedia, belum disisipkan', ('6', ''): 'Final v1.0: Android 28/28, Web tim WN 14/14, LPN 3/3, backend 47/47, NFR 8/9; 14 defect ditutup',
      ('7', 'Android'): 'Final v1.0 (22 Nov)', ('7', 'Web'): 'Final v1.0 (ditulis ulang untuk Web Node)', ('7', 'Backend'): 'Final v1.0 (22 Nov)', ('8', ''): 'Final v1.0: volume Android (PM) dan Web (Rafi, dikoreksi PM); gladi belum dilakukan', ('9', 'Android'): 'Final v1.0 (disusun PM)', ('9', 'Web'): 'Final v1.0 (disusun PM)'}
used = 0
for r in d.d.tables[11].rows[1:]:
    no, nm = r.cells[0].text.strip(), r.cells[1].text + ' ' + r.cells[2].text
    for (k, w), v in MM.items():
        if k == no and (w == '' or ('– ' + w) in r.cells[1].text or (w == 'Backend' and 'Backend' in r.cells[1].text)):
            d.setcell(r.cells[6], v); used += 1; break
print('matriks', used, 'dari', len(d.d.tables[11].rows) - 1)
d.para('Tabel 3.1 adalah Master Documentation Matrix', 'Tabel 3.1 adalah Master Documentation Matrix (Artefak A). Status per 22 November 2026: D01, D02, D03, D04A, D04B, D04W, D05A, D06, D07A, D07B, D07W, D08 (volume Android dan Web), D09A, dan D09W berstatus final v1.0; D05W berada pada v0.7 (Nabila). Web resmi adalah aplikasi tim Web (Node/Express/EJS); aplikasi Web React menjadi prototipe pembanding. Wewenang review (R1–R3) dan persetujuan dijalankan Project Manager. Di luar sembilan kategori, tersedia artefak pendukung TUT-DDB-001-AND (Panduan Uji Aplikasi di Ponsel v1.1) dan TUT-DDB-002-WEB (Panduan Menjalankan Web v1.0, masih untuk Web React).')
FINAL = {'D01', 'D02', 'D03', 'D04A', 'D04B', 'D04W', 'D05A', 'D06', 'D07A', 'D07B', 'D07W', 'D09A', 'D09W'}
n = 0
for r in d.d.tables[13].rows[1:]:
    tid, vol, st = r.cells[0].text.strip(), r.cells[1].text.strip(), r.cells[-1].text.strip()
    new = None
    if tid in ('D06-WP4-T01', 'D06-WP4-T02', 'D06-WP4-T03') or st.startswith('Selesai'):
        new = {'D06-WP4-T01': 'Selesai: 28/28 (27 pada build 22 Nov di emulator; SA-08 di ponsel fisik 14 Nov)', 'D06-WP4-T02': 'Selesai: Web tim 14/14 alur inti; Web React 15/15', 'D06-WP4-T03': 'Selesai: backend 47/47; LP 4/4 (Web React) dan LPN 3/3 (Web tim)'}.get(tid)
    elif 'Draf log defect' in st:
        new = 'Selesai: 14 defect, seluruhnya ditutup'
    elif vol in FINAL:
        new = 'Selesai (v1.0); dijalankan PM' if ('eview' in st or 'uji keterikutan' in st.lower() or st == 'Belum Dimulai') else 'Selesai (v1.0)'
    elif vol == 'D08':
        new = 'Sebagian: tangkapan layar dan log ada; gladi dan rekaman belum' if 'ladi' in r.cells[3].text else 'Selesai (v1.0)'
    elif vol == 'D05W':
        new = 'v0.7 (Nabila); kode WEB-01–WEB-11; tangkapan layar belum disisipkan'
    if new:
        d.setcell(r.cells[-1], new); n += 1
print('tugas diperbarui', n, 'dari', len(d.d.tables[13].rows) - 1)
for r in d.d.tables[20].rows[1:]:
    vol = r.cells[0].text.strip()
    if vol in FINAL or vol == 'D08':
        for c in (3, 4, 8):
            d.setcell(r.cells[c], '☑')
        for c in (5, 6, 7):
            if r.cells[c].text.strip() != '☑':
                d.setcell(r.cells[c], 'PM')
for r in d.d.tables[19].rows:
    if r.cells[0].text.startswith('Paket final'):
        for c in r.cells[1:]:
            if '24 Nov' in c.text:
                d.setcell(c, c.text.replace('24 Nov 18.00', '23 Nov 18.00').replace('Kumpul 24 Nov, target 20.00 WIB', 'Kumpul 23 Nov, target 20.00 WIB'))
d.sub('memastikan volume yang ia tulis sendiri telah ditelaah Steven sebelum ia setujui, sehingga tidak terjadi persetujuan tanpa telaah independen.', 'memastikan volume yang ia tulis sendiri diperiksa terhadap kode dan hasil uji sebelum disetujui; sejak v1.5 wewenang review dan persetujuan dijalankan Project Manager karena keterbatasan waktu.')
for s in d.d.sections:
    for p in s.footer.paragraphs:
        for r in p.runs:
            if '1.4' in r.text:
                r.text = r.text.replace('1.4', '1.5')
# tanda tangan PM pada lembar persetujuan
for r in d.d.tables[3].rows:
    if r.cells[0].text.startswith('Disusun oleh'):
        r.cells[2].paragraphs[0].add_run().add_picture(SIGN, width=Cm(2.0)); d.setcell(r.cells[3], '22 Nov 2026')
d.save(OUT + '/DWP-DDB-001_Software_Engineering_Documentation_Work_Plan_v1.5.docx')
