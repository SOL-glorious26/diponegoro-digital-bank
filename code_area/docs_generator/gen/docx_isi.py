"""Mengisi ulang paragraf dan tabel .docx tanpa mengubah formatnya. Markup: **tebal**, `kode`."""
import copy, re, docx
from docx.oxml.ns import qn
from docx.text.paragraph import Paragraph


class Doc:
    def __init__(self, p):
        self.d = docx.Document(p); self.P = list(self.d.paragraphs); self.T = list(self.d.tables)

    @staticmethod
    def fill(p, text):
        """ganti isi paragraf; format run diambil dari run pertama tiap jenis pada paragraf itu (atau dibuat dari run biasa)."""
        runs = list(p.runs); base = next((r for r in runs if not r.bold and r.font.name != 'Consolas'), runs[0])
        tpl = {'n': base, 'b': next((r for r in runs if r.bold), None), 'c': next((r for r in runs if r.font.name == 'Consolas'), None)}
        tpl = {k: copy.deepcopy(v._r) if v is not None else None for k, v in tpl.items()}
        for ch in list(p._p):
            if ch.tag != qn('w:pPr'):
                p._p.remove(ch)
        for part in re.split(r'(\*\*[^*]+\*\*|`[^`]+`)', text):
            if not part:
                continue
            k = 'b' if part.startswith('**') else 'c' if part.startswith('`') else 'n'
            r = copy.deepcopy(tpl[k] if tpl[k] is not None else tpl['n']); p._p.append(r)
            run = [x for x in p.runs][-1]; run.text = part.strip('*') if k == 'b' else part.strip('`') if k == 'c' else part
            if tpl[k] is None and k == 'b':
                run.bold = True
            if tpl[k] is None and k == 'c':
                run.font.name = 'Consolas'

    def set(self, i, text):
        self.fill(self.P[i], text)

    def rows(self, t, data):
        """isi ulang baris tabel (tanpa kepala); jumlah baris menyesuaikan."""
        while len(t.rows) - 1 > len(data):
            t._tbl.remove(t.rows[-1]._tr)
        while len(t.rows) - 1 < len(data):
            t._tbl.append(copy.deepcopy(t.rows[-1]._tr))
        for r, vals in zip(t.rows[1:], data):
            for c, v in zip(r.cells, vals):
                if c.paragraphs[0].runs:
                    self.fill(c.paragraphs[0], v)
                else:
                    c.paragraphs[0].add_run(v)

    def drop(self, i):
        e = self.P[i]._p; e.getparent().remove(e)

    def after(self, i, text):
        e = copy.deepcopy(self.P[i]._p); self.P[i]._p.addnext(e); self.fill(Paragraph(e, self.P[i]._parent), text)
