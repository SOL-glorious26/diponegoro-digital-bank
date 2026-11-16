"""Isi daftar isi (field TOC) dengan LibreOffice headless, lalu tempelkan hasilnya ke docx asli.

LibreOffice dipakai hanya untuk menghitung entri dan nomor halaman. Isi field TOC hasil LibreOffice
disalin ke document.xml docx asli supaya format dokumen lain tidak ikut berubah.

Pemakaian (python biasa):
  python lo_update_toc.py <in.docx> <out.docx>
"""
import os, re, shutil, subprocess, sys, tempfile, time, zipfile

SOFFICE_DIR = r"C:\Program Files\LibreOffice\program"
LO_PY = os.path.join(SOFFICE_DIR, "python.exe")

UNO_SCRIPT = r'''
import sys, time, uno
from com.sun.star.beans import PropertyValue
def pv(n, v):
    p = PropertyValue(); p.Name = n; p.Value = v; return p
src, dst, port = sys.argv[1], sys.argv[2], sys.argv[3]
local = uno.getComponentContext()
resolver = local.ServiceManager.createInstanceWithContext("com.sun.star.bridge.UnoUrlResolver", local)
for _ in range(60):
    try:
        ctx = resolver.resolve("uno:socket,host=127.0.0.1,port=%s;urp;StarOffice.ComponentContext" % port); break
    except Exception:
        time.sleep(1)
desktop = ctx.ServiceManager.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
doc = desktop.loadComponentFromURL(uno.systemPathToFileUrl(src), "_blank", 0, (pv("Hidden", True),))
idx = doc.getDocumentIndexes()
for i in range(idx.getCount()):
    idx.getByIndex(i).update()
doc.refresh()
for i in range(idx.getCount()):
    idx.getByIndex(i).update()
doc.storeToURL(uno.systemPathToFileUrl(dst), (pv("FilterName", "MS Word 2007 XML"),))
n = idx.getCount()
doc.close(True)
print("indexes", n)
try:
    desktop.terminate()
except Exception:
    pass
'''


def toc_block(xml):
    """Kembalikan (start, end) dari paragraf yang memuat field TOC: dari fldChar begin sampai fldChar end."""
    m = re.search(r'<w:instrText[^>]*>\s*TOC\b', xml)
    assert m, 'TOC field tidak ditemukan'
    pstart = xml.rfind('<w:p>', 0, m.start())
    p2 = xml.rfind('<w:p ', 0, m.start())
    pstart = max(pstart, p2)
    # cari fldChar end yang menutup TOC (hitung bersarang: hyperlink PAGEREF dsb.)
    depth = 0
    for f in re.finditer(r'<w:fldChar [^>]*w:fldCharType="(begin|end)"', xml[pstart:]):
        depth += 1 if f.group(1) == 'begin' else -1
        if depth == 0:
            endpos = pstart + f.end()
            pend = xml.find('</w:p>', endpos) + len('</w:p>')
            return pstart, pend
    raise SystemExit('akhir field TOC tidak ditemukan')


def main(src, out):
    tmp = tempfile.mkdtemp(prefix='lotoc_')
    lo_out = os.path.join(tmp, 'lo.docx')
    port = '2002'
    prof = 'file:///' + os.path.join(tmp, 'profile').replace('\\', '/')
    office = subprocess.Popen([os.path.join(SOFFICE_DIR, 'soffice.exe'), '--headless', '--invisible', '--norestore',
                               '-env:UserInstallation=' + prof,
                               '--accept=socket,host=127.0.0.1,port=%s;urp;' % port])
    try:
        script = os.path.join(tmp, 'u.py')
        open(script, 'w', encoding='utf-8').write(UNO_SCRIPT)
        r = subprocess.run([LO_PY, script, os.path.abspath(src), lo_out, port], capture_output=True, text=True, timeout=300)
        print(r.stdout.strip(), r.stderr.strip()[-500:])
    finally:
        time.sleep(1)
        office.kill()
    lo_xml = zipfile.ZipFile(lo_out).read('word/document.xml').decode('utf-8')
    a, b = toc_block(lo_xml)
    lo_toc = lo_xml[a:b]
    entries = re.findall(r'<w:t[^>]*>([^<]*)</w:t>', lo_toc)
    with zipfile.ZipFile(src) as zin:
        x = zin.read('word/document.xml').decode('utf-8')
        sa, sb = toc_block(x)
        # gaya entri: LibreOffice memakai nama gaya "Contents1".. ; Word memakai "TOC1"..
        styles = zin.read('word/styles.xml').decode('utf-8')
        if 'w:styleId="TOC1"' in styles:
            lo_toc = re.sub(r'w:val="Contents(\d)"', r'w:val="TOC\1"', lo_toc)
        # Bookmark _Toc di judul harus ada di dokumen asli; tanpa itu, hyperlink dan PAGEREF tidak cocok.
        # Agar aman, entri disalin tanpa hyperlink/PAGEREF: teks + tab + nomor halaman statis di dalam field.
        lo_toc = re.sub(r'<w:hyperlink[^>]*>', '', lo_toc).replace('</w:hyperlink>', '')
        x2 = x[:sa] + lo_toc + x[sb:]
        with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as zout:
            for info in zin.infolist():
                data = x2.encode('utf-8') if info.filename == 'word/document.xml' else zin.read(info.filename)
                zout.writestr(info, data)
    shutil.rmtree(tmp, ignore_errors=True)
    print('entri TOC:', ' | '.join(e for e in entries if e.strip())[:600])
    print('wrote', out)


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
