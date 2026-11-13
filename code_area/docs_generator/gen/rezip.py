import sys, zipfile, os
src, folder, out = sys.argv[1], sys.argv[2], sys.argv[3]
with zipfile.ZipFile(src) as zin, zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as zout:
    for info in zin.infolist():
        p = os.path.join(folder, info.filename)
        with open(p, 'rb') as f:
            zout.writestr(info, f.read(), compress_type=zipfile.ZIP_DEFLATED)
print('wrote', out)
