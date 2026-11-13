import sys; sys.stdout.reconfigure(encoding="utf-8")
import re, sys
xml = open(sys.argv[1], encoding='utf-8').read()
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
import xml.etree.ElementTree as ET
root = ET.fromstring(xml)
body = root.find(W + 'body')
ti = 0
want = set(int(x) for x in sys.argv[2].split(',')) if len(sys.argv) > 2 and sys.argv[2] else None
for el in body:
    if el.tag == W + 'tbl':
        ti += 1
        if want and ti not in want:
            continue
        print(f'=== TABLE {ti}')
        for tr in el.findall(W + 'tr'):
            cells = []
            for tc in tr.findall(W + 'tc'):
                cells.append(' '.join(''.join(t.text or '' for t in p.iter(W + 't')) for p in tc.findall(W + 'p')).strip())
            print(' | '.join(cells))
    elif el.tag == W + 'p' and not want:
        txt = ''.join(t.text or '' for t in el.iter(W + 't'))
        if txt.startswith('Tabel'):
            print(f'--- (before table {ti+1}) {txt}')
