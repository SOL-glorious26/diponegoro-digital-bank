"""Audit akhir 22 Nov 2026 (malam): D08W diberi blok kontrol template dosen, sisa 'Perlu Ditentukan' dan rencana gladi dirapikan, referensi D01 disamakan.
Masukan: berkas v1.0 di documents_area. Pemakaian: python patch_audit_akhir.py <documents_area> <folder keluaran>
"""
import copy, os, sys, docx
DA, OUT = sys.argv[1:3]


def settext(p, text):
    runs = [r for r in p.runs if r.text] or p.runs
    if runs:
        runs[0].text = text
        for r in runs[1:]:
            r.text = ''
    else:
        p.add_run(text)


def setcell(c, text):
    settext(c.paragraphs[0], text)
    for p in c.paragraphs[1:]:
        p._p.getparent().remove(p._p)


def allp(d):
    return list(d.paragraphs) + [p for t in d.tables for r in t.rows for c in r.cells for p in c.paragraphs]


def sub(d, a, b):
    k = 0
    for p in allp(d):
        if a in p.text:
            settext(p, p.text.replace(a, b)); k += 1
    assert k, a


# ------------------------------------------------------------------ D01
p = DA + '/D01_Project_Document/DDB_D01_Project_Document_v1.0.docx'; d = docx.Document(p)
sub(d, 'Module Decomposition & Task Allocation (MDTA-DDB-001) v0.1.', 'Module Decomposition & Task Allocation (MDTA-DDB-001) v0.3.')
d.save(OUT + '/d01_raw.docx')

# ------------------------------------------------------------------ D08W
p = DA + '/D08W_System_Program_Demo_Web/DDB_D08W_System_Program_Demo_Web_v1.0.docx'; d = docx.Document(p)
assert not any('Nama module' in c.text for t in d.tables for r in t.rows for c in r.cells), 'blok kontrol sudah ada'
src = docx.Document(DA + '/D04W_Detailed_Design_Web/DDB_D04W_Detailed_Design_Web_v1.0.docx').tables[1]
assert src.rows[0].cells[0].text.strip() == 'Nama module'
tb = copy.deepcopy(src._tbl)
rev = d.tables[1]; assert rev.rows[0].cells[0].text.strip() == 'Versi'
hd = [q for q in d.paragraphs if q.text.strip() == 'Riwayat Revisi' and q.style is not None and q.style.name.startswith('Heading')]
assert len(hd) == 1
hd[0]._p.addprevious(tb)
from docx.table import Table
t = Table(tb, d._body)
for r, v in zip(t.rows, ['Demo Program Sistem – Web (Node.js, Express 5, EJS) dan lintas platform',
                         'Final v1.0 – diterbitkan Project Manager 22 November 2026; wewenang review dan persetujuan dijalankan Project Manager',
                         'Rafi Fadhillah (PIC Dokumen 8)',
                         'Salomo Putra Glorious Hokoyoku (Project Manager)',
                         'Kelompok Jaran Goyang – Web Application Team (PIC: Rafi Fadhillah; pendukung: Nabila, Cecilia, Permata)',
                         'DEM-DDB-001 / 1.0',
                         'SRS-DDB-001 v0.1; STD-DDB-001 v1.0; SDD-DDB-001-WEB v1.0; UIM-DDB-001-WEB v1.0; DEM-DDB-001-AND v1.0; kode digitalbanking-node v2.0.0']):
    setcell(r.cells[1], v)
assert [r.cells[0].text.strip() for r in t.rows][:5] == ['Nama module', 'Status', 'Created by', 'Last updated by', 'Responsible team']
sub(d, 'Pemetaan FR final Perlu Ditentukan (Salomo).', 'Top Up saldo bukan kebutuhan pada SRS v0.1, sehingga tidak dipetakan ke FR mana pun (STD-DDB-001, WN-17).')
sub(d, ', dan dimasukkan setelah naskah tersebut diterima (Perlu Ditentukan: Cecilia).', '. Naskah Android rinci memakai DEM-DDB-001-AND v1.0.')
for tt in d.tables:
    for r in tt.rows:
        c = [x.text.strip() for x in r.cells]
        if c[0] == 'DEM-11' and 'Perlu Ditentukan (Salomo)' in c:
            setcell(r.cells[c.index('Perlu Ditentukan (Salomo)')], 'Top Up: tanpa FR; QRIS: FR-15')
        if c[0] == 'DEM-10' and c[-1].startswith('Token PLN di detail'):
            setcell(r.cells[-1], 'Tidak ada; nomor token tampil di detail')
        if c[0] == '21 Nov' and c[1].startswith('Gladi penuh'):
            setcell(r.cells[0], 'Sebelum demo')
            setcell(r.cells[1], 'Gladi penuh pada Web Node.js dan Android (belum dilakukan per 22 Nov); rekam video')
        if c[0] == '22 Nov' and c[1].startswith('Revisi setelah gladi'):
            setcell(r.cells[1], 'Uji otomatis, tangkapan layar Web, dan versi final v1.0 (tanpa gladi)'); setcell(r.cells[2], 'Salomo')
for q in allp(d):
    if 'Ringkasan hasil D06 v0.9.6' in q.text:
        settext(q, q.text.replace('D06 v0.9.6', 'D06 v1.0'))
d.save(OUT + '/d08w_raw.docx')
left = [q.text[:120] for q in allp(d) if 'Perlu Ditentukan' in q.text or 'v0.9.6' in q.text]
print('D08W sisa:', left)

# ------------------------------------------------------------------ D04W: keterangan Tabel 0.2 tepat di bawah tabelnya; Bab I lebih rapat
from docx.shared import Pt
p = DA + '/D04W_Detailed_Design_Web/DDB_D04W_Detailed_Design_Web_v1.0.docx'; d = docx.Document(p)
cap = [q for q in d.paragraphs if q.text.startswith('Tabel 0.2')]; assert len(cap) == 1
d.tables[2]._tbl.addnext(cap[0]._p)
on = False
for q in d.paragraphs:
    if q.text.startswith('BAB I ') and q.style is not None and q.style.name.startswith('Heading'):
        on = True
    if q.text.startswith('BAB II ') and on:
        break
    if on and q.text.strip():
        q.paragraph_format.line_spacing = 1.08
        if q.paragraph_format.space_after is None or q.paragraph_format.space_after > Pt(4):
            q.paragraph_format.space_after = Pt(4)
d.save(OUT + '/d04w_raw.docx')
