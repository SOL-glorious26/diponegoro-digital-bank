"""Ubah repository konkret menjadi implementasi Local dari sebuah interface.
python localize.py <file.kt> <NamaInterface> <metode,metode,...>
 - file lama di-rename menjadi Local<Nama>.kt
 - class <Nama>( -> class Local<Nama>(... ) : <Nama> {
 - metode publik yang disebut -> override suspend fun
"""
import os, re, sys

path, name, methods = sys.argv[1], sys.argv[2], sys.argv[3].split(',')
s = open(path, encoding='utf8').read()
start = s.index(f'class {name}(')
s = s[:start] + f'class Local{name}(' + s[start + len(f'class {name}('):]
# akhir parameter konstruktor: ") {" pertama setelah deklarasi kelas
i = s.index(') {', start)
s = s[:i] + f') : {name} {{' + s[i + 3:]
for m in methods:
    pat = re.compile(r'^(    )(?:suspend )?fun ' + re.escape(m) + r'\(', re.M)
    s, n = pat.subn(r'\1override suspend fun ' + m + '(', s)
    if n != 1:
        raise SystemExit(f'{m}: {n} kecocokan')
# KDoc default param tidak boleh pada override: hapus nilai default di signature override
new = os.path.join(os.path.dirname(path), f'Local{name}.kt')
open(new, 'w', encoding='utf8', newline='\n').write(s)
os.remove(path)
print('ok', new)
