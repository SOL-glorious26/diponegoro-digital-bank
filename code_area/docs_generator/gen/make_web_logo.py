"""Buat aset logo aplikasi Web dari logo resmi (JPEG 3:2 berlatar navy + teks).

Usage: python make_web_logo.py <logo_sumber.jpeg> <folder_public_web>

Hasil:
  logo-mark.png         lambang "D" kuda, persegi 256 px, sudut membulat, latar transparan di luar sudut
  favicon.ico           16/32/48 px untuk tab peramban
  apple-touch-icon.png  180 px, persegi penuh (iOS membulatkan sendiri)
"""
import sys, os
from PIL import Image, ImageDraw, ImageFilter

SRC, OUT = sys.argv[1], sys.argv[2]
os.makedirs(OUT, exist_ok=True)
im = Image.open(SRC).convert("RGB")
W, H = im.size

# Warna latar = median sudut kiri atas.
bg = im.crop((0, 0, 40, 40)).resize((1, 1), Image.Resampling.BOX).getpixel((0, 0))

# Kotak pembatas lambang: piksel emas di atas baris teks.
px = im.load()
text_top = int(H * 0.77)          # teks "DIPONEGORO CORP" mulai ±78% tinggi
xs, ys = [], []
for y in range(0, text_top, 2):
    for x in range(0, W, 2):
        r, g, b = px[x, y]
        if r > 150 and g > 110 and b < 150 and r - b > 60:
            xs.append(x); ys.append(y)
x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
side = max(x1 - x0, y1 - y0) * 1.22   # ruang napas di sekitar lambang
box = (int(cx - side / 2), int(cy - side / 2), int(cx + side / 2), int(cy + side / 2))
print("latar", bg, "lambang", (x0, y0, x1, y1), "potongan", box)

# Tutup area teks di bawah lambang dengan warna latar agar tidak ikut terpotong.
mark_src = im.copy()
ImageDraw.Draw(mark_src).rectangle((0, y1 + 14, W, H), fill=bg)

# Kanvas persegi berwarna latar (aman bila potongan keluar batas gambar).
sq = Image.new("RGB", (box[2] - box[0], box[3] - box[1]), bg)
sq.paste(mark_src.crop((max(box[0], 0), max(box[1], 0), min(box[2], W), min(box[3], H))),
         (max(-box[0], 0), max(-box[1], 0)))


def rounded(img, size, radius_ratio=0.22):
    img = img.resize((size, size), Image.Resampling.LANCZOS)
    # sedikit pertajam karena garis logo tipis
    img = img.filter(ImageFilter.UnsharpMask(radius=1.2, percent=60, threshold=2))
    mask = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=int(size * 4 * radius_ratio), fill=255)
    mask = mask.resize((size, size), Image.Resampling.LANCZOS)
    out = img.convert("RGBA"); out.putalpha(mask)
    return out


# Ditampilkan ±40 px: garis ditebalkan sedikit agar tetap jelas.
rounded(sq.filter(ImageFilter.MaxFilter(7)), 256).save(os.path.join(OUT, "logo-mark.png"), optimize=True)

# Favicon: ukuran kecil diambil dari potongan lebih rapat agar garis tetap terlihat.
tight = side * 0.92
tbox = (int(cx - tight / 2), int(cy - tight / 2), int(cx + tight / 2), int(cy + tight / 2))
tsq = Image.new("RGB", (tbox[2] - tbox[0], tbox[3] - tbox[1]), bg)
tsq.paste(mark_src.crop((max(tbox[0], 0), max(tbox[1], 0), min(tbox[2], W), min(tbox[3], H))), (max(-tbox[0], 0), max(-tbox[1], 0)))
# Garis ditebalkan (dilasi piksel terang) supaya tidak hilang saat diperkecil ke 16-48 px.
tsq = tsq.filter(ImageFilter.MaxFilter(13))
ico = rounded(tsq, 256, 0.18)
ico.save(os.path.join(OUT, "favicon.ico"), sizes=[(16, 16), (32, 32), (48, 48)])
sq.resize((180, 180), Image.Resampling.LANCZOS).save(os.path.join(OUT, "apple-touch-icon.png"), optimize=True)

print("latar_hex", "#%02x%02x%02x" % bg)
for f in ["logo-mark.png", "favicon.ico", "apple-touch-icon.png"]:
    print(f, os.path.getsize(os.path.join(OUT, f)), "byte")
