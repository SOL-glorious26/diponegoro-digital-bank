"""Perbarui docs_generator/kdoc.txt (inventaris berkas Kotlin Android untuk D07A) dari source sebenarnya.

Format baris: <dir\\sub/Berkas.kt> | <jumlah baris> | <deklarasi> | <KDoc pertama>
Baris yang sudah ada: hanya jumlah baris yang diperbarui. Berkas baru: deklarasi dan KDoc diambil dari kode.
Pemakaian: python update_kdoc.py <kdoc.txt> <folder com/diponegoro/digitalbank> [--check]
"""
import os, re, sys

kdoc_path, src = sys.argv[1], sys.argv[2]
check = "--check" in sys.argv


def key(rel):  # "account/data/X.kt" -> "account\data/X.kt", "X.kt" -> "/X.kt"
    d, f = os.path.split(rel)
    return (d.replace("/", "\\") if d else "") + "/" + f


def count_lines(p):
    with open(p, encoding="utf-8") as fh:
        return len(fh.read().splitlines())


rows = {}
order = []
for line in open(kdoc_path, encoding="utf-8").read().splitlines():
    if not line.strip():
        continue
    k = line.split(" | ")[0]
    rows[k] = line.split(" | ")
    order.append(k)

found = {}
for root, _, files in os.walk(src):
    for f in files:
        if f.endswith(".kt"):
            rel = os.path.relpath(os.path.join(root, f), src).replace("\\", "/")
            found[key(rel)] = os.path.join(root, f)

changed, added = [], []
for k, p in sorted(found.items()):
    n = count_lines(p)
    if k in rows:
        if int(rows[k][1]) != n:
            changed.append((k, rows[k][1], n))
            rows[k][1] = str(n)
    else:
        text = open(p, encoding="utf-8").read()
        decls = ",".join(re.findall(r"^(?:data |sealed |private |internal )*(?:class|object|interface) (\w+)", text, flags=re.M))
        doc = re.search(r"/\*\*\s*(.*?)\*/", text, flags=re.S)
        doc = re.sub(r"\s*\*\s*", " ", doc.group(1)).strip() if doc else ""
        rows[k] = [k, str(n), decls, doc]
        added.append(k)
removed = [k for k in order if k not in found]

for k, a, b in changed:
    print(f"baris  {k}: {a} -> {b}")
for k in added:
    print(f"baru   {k}")
for k in removed:
    print(f"hilang {k}")
if not check:
    # Pertahankan urutan lama; berkas baru disisipkan berurutan di dalam folder yang sama.
    keys = [k for k in order if k in found]
    for k in sorted(added):
        d = k.rsplit("/", 1)[0]
        same = [i for i, x in enumerate(keys) if x.rsplit("/", 1)[0] == d]
        pos = next((i for i in same if keys[i].lower() > k.lower()), (same[-1] + 1) if same else len(keys))
        keys.insert(pos, k)
    with open(kdoc_path, "w", encoding="utf-8", newline="\n") as fh:
        for k in keys:
            fh.write(" | ".join(rows[k]) + "\n")
    print("ditulis", len(keys), "berkas")
