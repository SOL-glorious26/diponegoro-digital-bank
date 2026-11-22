"""Koreksi faktual dokumen Rafi untuk volume Web (22 Nov 2026): D04W v0.7 -> v0.8 dan D08 v0.2 -> D08W v0.3.
Isi dan penulis tetap milik Rafi; yang diubah hanya fakta yang sudah berubah setelah Web Node diuji dengan backend asli.
Pemakaian: python patch_rafi_web.py <D04W v0.7.docx> <D04W v0.8.docx> <D08 v0.2.docx> <D08W v0.3.docx>
"""
import copy, sys, docx
A, AO, B, BO = sys.argv[1:5]


class Doc:
    def __init__(self, p):
        self.d = docx.Document(p)
        self.n = 0

    def settext(self, p, text):
        runs = [r for r in p.runs if r.text] or p.runs
        assert runs, p.text
        runs[0].text = text
        for r in runs[1:]:
            r.text = ''
        self.n += 1

    def cell(self, t, r, c, text):
        ce = self.d.tables[t].rows[r].cells[c]
        self.settext(ce.paragraphs[0], text)
        for p in ce.paragraphs[1:]:
            p._p.getparent().remove(p._p)

    def row(self, t, first, vals):
        hit = [i for i, r in enumerate(self.d.tables[t].rows) if r.cells[0].text.strip() == first]
        assert len(hit) == 1, (t, first, hit)
        for c, v in vals.items():
            self.cell(t, hit[0], c, v)

    def para(self, start, text):
        hit = [p for p in self.d.paragraphs if p.text.startswith(start)]
        assert len(hit) == 1, (start, len(hit))
        self.settext(hit[0], text)

    def addrow(self, t, vals, before_last=False):
        tb = self.d.tables[t]
        new = copy.deepcopy(tb.rows[-1]._tr)
        (tb.rows[-1]._tr.addprevious if before_last else tb.rows[-1]._tr.addnext)(new)
        r = len(tb.rows) - (2 if before_last else 1)
        for c, v in enumerate(vals):
            self.cell(t, r, c, v)

    def sub(self, a, b, n=None):
        k = 0
        for p in list(self.d.paragraphs) + [p for t in self.d.tables for r in t.rows for c in r.cells for p in c.paragraphs]:
            if a in p.text:
                self.settext(p, p.text.replace(a, b)); k += 1
        assert k and (n is None or k == n), (a, k)

    def save(self, p):
        # setelah penggantian, paragraf panjang tidak boleh menjadi tebal seluruhnya
        for q in self.d.paragraphs:
            act = [r for r in q.runs if r.text.strip()]
            if not q.style.name.startswith('Heading') and len(act) == 1 and act[0].bold and len(q.text) > 110:
                act[0].bold = False
        self.d.save(p); print(p.split('/')[-1], self.n, 'perubahan')


# ------------------------------------------------------------------ D04W
d = Doc(A)
d.row(0, 'Versi / Status', {1: '0.8 / Dipakai sebagai volume Web; menunggu review R1 (Nabila) dan persetujuan'})
d.row(0, 'Tanggal', {1: '22 November 2026'})
d.row(1, 'Status', {1: 'v0.8: koreksi fakta oleh Project Manager atas v0.7 (Rafi); menunggu pre-review (Nabila) dan persetujuan akhir'})
d.row(1, 'Last updated by', {1: 'Salomo Putra Glorious Hokoyoku (Project Manager) – koreksi atas v0.7 Rafi Fadhillah'})
d.row(1, 'Kode dokumen / versi', {1: 'SDD-DDB-001-WEB / 0.8'})
d.row(1, 'Dokumen acuan', {1: 'SRS-DDB-001 v0.1; SAD-DDB-001 v1.0 Bab VIII; FSD-DDB-001 v1.0; SDD-DDB-001-BE v1.0 dan code_area/backend/docs/API.md; UIM-DDB-001-WEB v0.7; STD-DDB-001 v1.0; kode digitalbanking-node v2.0.0'})
d.addrow(2, ['0.8', '22 Nov 2026', 'Salomo P.G. Hokoyoku', 'Koreksi fakta setelah Web Node diuji dengan backend asli: hasil WN-01–WN-14 dan LPN-01–LPN-03 (STD-DDB-001 v1.0), kode halaman WEB-xx (UIM-DDB-001-WEB v0.7), jumlah baris sesuai SE-web.rar, FR-07 dihapus dari Dashboard (grafik saldo belum ada), dan peran tim. Isi rancangan tetap dari v0.7.'])
d.para('Kode sumber berada di folder digitalbanking-node', 'Kode sumber berada di folder digitalbanking-node (sekitar 1.470 baris termasuk tampilan, CSS, dan uji). Struktur dipisah per tanggung jawab agar rute tidak memanggil backend secara langsung tanpa penanganan token. Pembagian kerja: Permata Surya Cahyana (backend Web), Rafi Fadhillah (front end dan backend Web), Steven Wibisana dan Nabila Puspa Annastasya (front end).')
d.row(4, 'lib/helpers.js', {1: '37'})
d.row(4, 'views/*.ejs (11 berkas)', {1: '459'})
d.row(4, 'views/partials (3 berkas)', {1: '30'})
d.row(4, 'public/assets/css/style.css', {1: '167', 2: 'Gaya global dan tata letak responsif; gambar QRIS di public/assets/img/qris.png; public/assets/js/pwtoggle.js (33 baris) untuk ikon mata kata sandi/PIN'})
d.sub('(jumlah baris dihitung dengan wc -l, 21 Nov 2026)', '(jumlah baris dihitung dengan wc -l dari SE-web.rar, 22 Nov 2026)', 1)
d.sub('Tanpa JavaScript sisi klien untuk logika. Seluruh logika berada di server; peramban hanya mengirim form POST bersama token CSRF.', 'Tanpa JavaScript sisi klien untuk logika bisnis. Satu-satunya skrip peramban adalah pwtoggle.js (menampilkan/menyembunyikan kata sandi); selebihnya peramban hanya mengirim form POST bersama token CSRF.', 1)
d.row(3, 'Hasil uji', {2: 'WN-01 s.d. WN-14 dan LPN-01 s.d. LPN-03 lulus (STD-DDB-001 v1.0); kasus WB tidak berlaku'})
d.row(8, 'Dashboard FR-06, FR-07', {0: 'Dashboard FR-06'})
d.para('Kode layar D05W pada UIM-DDB-001-WEB', 'Kode halaman mengikuti UIM-DDB-001-WEB v0.7 (WEB-01 s.d. WEB-11). Kasus WB-01 s.d. WB-15 pada STD-DDB-001 menguji Web React sehingga tidak dirujuk di sini; bukti yang berlaku adalah WN (alur inti Web Node terhadap backend asli) dan LPN (lintas platform dengan Android), 22 November 2026.')
for first, v in {
    'Masuk': 'WEB-01: WN-01, WN-02, WN-14; LPN-03',
    'Daftar, e-KYC, Buat PIN': 'WEB-02 s.d. WEB-05: WN-13',
    'Lupa sandi': 'WEB-11: belum ada kasus Web',
    'Dashboard': 'WEB-06: WN-01; LPN-01',
    'Transfer': 'WEB-07: WN-03 s.d. WN-06',
    'Pembayaran (pulsa, PLN, tagihan, e-wallet)': 'WEB-08: WN-07 s.d. WN-10; LPN-02',
    'Riwayat dan ekspor': 'WEB-09: WN-06 (cek status), WN-12 (riwayat, CSV, PDF)',
    'Profil': 'WEB-10: belum ada kasus Web',
}.items():
    d.row(9, first, {2: v})
d.row(9, 'Dashboard', {1: 'FR-06'})
d.row(9, 'Top Up saldo, QRIS (tampil dan bayar contoh kode)', {1: 'FR-15 sebagian (contoh kode, tanpa kamera); Top Up tidak memiliki FR', 2: 'WEB-08: WN-11 (QRIS); Top Up belum ada kasus'})
d.para('Belum ada: uji alur Web terhadap backend asli', 'Uji alur terhadap backend asli dijalankan pada 22 November 2026 dengan pengujian/web/node-smoke.mjs: WN-01 s.d. WN-14 lulus seluruhnya, dan LPN-01 s.d. LPN-03 (lintas platform dengan Android) lulus. Uji mengirim formulir HTTP seperti peramban lalu mencocokkan saldo ke backend. Belum ada: uji tampilan, uji antar-peramban, ubah profil, ubah kata sandi, pemulihan akun, dan Top Up saldo.')
d.sub('penerapan Perlu Ditentukan oleh Salomo.', 'patch tersebut saat ini tidak diterapkan pada backend bersama.', 1)
d.row(13, 'Tabel 2.1 (inventaris)', {2: 'Diperbarui ke SE-web.rar 22 Nov 2026'})
d.row(13, 'Tabel 4.1 (API per halaman)', {2: 'Sesuai kode; dijalankan terhadap backend asli melalui WN-01 s.d. WN-14 kecuali profil dan pemulihan'})
d.row(13, 'Perilaku terhadap backend asli', {1: 'node-smoke.mjs dan lp-node.mjs, 22 Nov 2026', 2: 'WN 14/14 dan LPN 3/3 lulus'})
d.para('Batasan telaah: diagram dibuat', 'Batasan telaah: diagram dibuat dari pembacaan kode; alur inti diverifikasi dengan menjalankan aplikasi, sedangkan tampilan tidak diperiksa.')
d.save(AO)

# ------------------------------------------------------------------ D08W
d = Doc(B)
d.row(0, 'Versi / Status', {1: '0.3 / Dipakai sebagai volume Web; gladi dan tangkapan layar Web belum ada'})
d.row(0, 'Tanggal', {1: '22 November 2026'})
d.addrow(1, ['0.3', '22 Nov 2026', 'Salomo P.G. Hokoyoku', 'Koreksi fakta atas v0.2 (Rafi): hasil uji otomatis Web Node dengan backend asli diisikan pada Tabel 5.1, pemetaan SA-05/SA-06 pada DEM-06/DEM-07 dibetulkan, FR-07 dihapus dari DEM-03, ringkasan D06 disamakan dengan STD-DDB-001 v1.0. Hasil gladi tetap belum ada.'], before_last=True)
d.para('Dokumen ini adalah draf. Hasil uji', 'Dokumen ini memakai Web Node.js (Express + EJS) sebagai Web resmi. Kasus WB-01 s.d. WB-15 dan LP-01 s.d. LP-04 pada D06 dijalankan terhadap Web React dan tidak dihitung di sini. Pada 22 November 2026 alur inti Web Node.js diuji otomatis terhadap backend asli: WN-01 s.d. WN-14 dan LPN-01 s.d. LPN-03 lulus (STD-DDB-001 v1.0). Uji otomatis itu bukan gladi: tampilan tidak diperiksa dan belum ada tangkapan layar Web. Tidak ada hasil gladi yang diklaim pada versi ini. Skenario Android rinci ada pada DEM-DDB-001-AND.')
d.row(2, 'SDD-DDB-001-WEB', {1: 'D04W Detailed Design Web (v0.8)'})
d.row(2, 'UIM-DDB-001-WEB / -AND', {1: 'D05 UI Map Web v0.7 (WEB-01..11) dan Android v1.0 (AND-01..23)', 2: 'Kode halaman Web Node.js dan kode layar Android'})
d.row(2, 'STD-DDB-001 v0.9.6', {0: 'STD-DDB-001 v1.0', 2: 'Sumber hasil uji backend, Android, Web Node.js (WN), dan lintas platform (LPN)'})
d.row(8, 'DEM-03', {2: 'FR-06'})
d.row(8, 'DEM-06', {4: 'SA-05; WN-05'})
d.row(8, 'DEM-07', {4: 'SA-06; WN-06'})
for k, v in {'DEM-01': 'SA-01; WN-01', 'DEM-02': 'SA-21, SA-22; WN-13', 'DEM-03': 'SA-03; WN-01', 'DEM-04': 'SA-07, LPN-01; WN-03', 'DEM-05': 'WN-04 (nominal); rekening tidak valid: backend', 'DEM-08': 'SA-14; WN-12', 'DEM-09': 'WN-12', 'DEM-10': 'SA-10, SA-11; WN-07, WN-08', 'DEM-11': 'WN-11 (QRIS); Top Up belum ada kasus', 'DEM-13': 'LPN-01', 'DEM-14': 'LPN-02 (e-wallet Web ke riwayat Android)'}.items():
    d.row(8, k, {4: v})
d.sub('Tabel 3.1 Daftar skenario dan pemetaannya ke bukti D06 (backend dan Android). Bukti antarmuka Web Node.js diisi dari gladi (Bab V dan VI).', 'Tabel 3.1 Daftar skenario dan pemetaannya ke bukti STD-DDB-001 v1.0. Tangkapan layar antarmuka Web Node.js diisi dari gladi (Bab VI).', 1)
d.para('Bab ini membandingkan harapan', 'Bab ini membandingkan harapan dengan perilaku nyata. Kolom hasil memuat uji otomatis 22 November 2026 terhadap backend asli; kolom Deviasi diisi dari uji tersebut. Gladi oleh pembawa demo belum dilakukan.')
d.cell(9, 0, 2, 'Bukti D06 v1.0'); d.cell(9, 0, 3, 'Hasil uji otomatis 22 Nov 2026 (gladi belum)')
RES = {'DEM-01': ('SA-01; WN-01', 'Lulus', 'Tidak ada'), 'DEM-02': ('SA-21, SA-22; WN-13', 'Lulus; saldo awal Rp5.000.000', 'Patch saldo Rp0 tidak dipasang'), 'DEM-03': ('SA-03; WN-01', 'Lulus; saldo Web = backend', 'Grafik saldo belum ada di Web'),
       'DEM-04': ('WN-03, LPN-01', 'Lulus; saldo kedua akun tepat', 'Tidak ada'), 'DEM-05': ('WN-04', 'Nominal kecil dan PIN salah ditolak', 'Rekening tidak valid belum diuji lewat Web'), 'DEM-06': ('SA-05; WN-05', 'Lulus; dana kembali', 'Tidak ada'),
       'DEM-07': ('SA-06; WN-06', 'Lulus; final lewat Cek status; debit sekali', 'Tidak ada'), 'DEM-08': ('SA-14; WN-12', 'Riwayat tampil', 'Filter dan jurnal belum diuji lewat Web'), 'DEM-09': ('WN-12', 'CSV dan PDF terunduh', 'Kesesuaian isi dengan filter belum diuji'),
       'DEM-10': ('SA-10, SA-11; WN-07, WN-08', 'Pulsa (Rp11.500) dan PLN lulus', 'ID 0000 belum diuji lewat Web'), 'DEM-11': ('WN-11', 'QRIS contoh lulus', 'Top Up belum diuji'), 'DEM-12': ('SA-12', 'Lulus (Android)', 'Tidak ada'),
       'DEM-13': ('LPN-01', 'Lulus', 'Tidak ada'), 'DEM-14': ('LPN-02', 'E-wallet Web tampil di Android', 'Transfer Web ke Android belum diuji terpisah')}
for k, (a, b, c) in RES.items():
    d.row(9, k, {2: a, 3: b, 4: c})
d.sub('Tabel 5.1 Perilaku sistem aktual. Kasus WB D06 v0.9.6 menguji Web React', 'Tabel 5.1 Perilaku sistem aktual. Kasus WB menguji Web React', 1)
for p in d.d.paragraphs:
    if p.text.startswith('5.1 Ringkasan hasil D06 v0.9.6'):
        d.settext(p, '5.1 Ringkasan hasil D06 v1.0')
d.row(10, 'Android otomatis (demo)', {1: '63 / 64 lulus, 1 dilewati'})
d.row(10, 'Android otomatis (online)', {1: '57 / 58 lulus, 1 dilewati'})
d.row(10, 'Android uji sistem', {2: '27 pada build 22 Nov di emulator; SA-08 di ponsel fisik Realme RMX3867 pada 14 Nov (build sebelumnya)'})
d.row(10, 'Lintas platform', {1: 'LP 4 / 4 (Web React); LPN 3 / 3 (Web Node.js)', 2: 'LPN-01 s.d. LPN-03 memakai Web Node.js'})
d.row(10, 'Web (React)', {0: 'Web', 1: 'WN 14 / 14 (Web Node.js); WB 15 / 15 (Web React)', 2: 'WB tidak berlaku untuk Web Node.js'})
d.row(10, 'Cacat', {1: '14 dicatat, 14 ditutup'})
d.row(11, 'T-02', {1: 'PostgreSQL: profil dev pernah berjalan di PostgreSQL lokal sejak 13 Nov; uji memakai H2', 3: 'Demo memakai H2 berkas'})
d.row(11, 'T-04', {1: 'Web Node.js sudah memiliki kasus alur inti (WN), tetapi belum ada uji tampilan dan antar-peramban', 2: 'Tampilan belum terbukti', 3: 'Gladi dan rekam tangkapan layar'})
d.row(11, 'T-06', {1: 'Saldo awal akun baru Rp5.000.000; patch Rp0 tidak diterapkan pada backend bersama', 2: 'Jangan memakai salinan backend dengan DDB_OPENING_BALANCE=0', 3: 'Keputusan mengubahnya ada pada Salomo'})
d.sub('penerapan patch Perlu Ditentukan oleh Salomo sebelum gladi, dan nilai', 'patch saat ini tidak diterapkan, dan nilai', 1)
# gambar LP-01 lama (tampilan 11 Nov) diganti bukti LPN-01 build 22 Nov
import io, os
from PIL import Image
here = os.path.dirname(os.path.abspath(__file__))
im = Image.open(os.path.join(here, '../../pengujian/bukti/2026-11-22/lintas-platform-node/LPN01-android-bukti.png')).convert('RGB')
for rel in d.d.part.rels.values():
    if 'image' in rel.reltype and rel.target_part.partname.endswith('image3.jpg'):
        old = Image.open(io.BytesIO(rel.target_part.blob)); print('gambar lama', old.size, 'baru', im.size)
        buf = io.BytesIO(); im.resize((old.size[0], round(im.size[1] * old.size[0] / im.size[0]))).save(buf, 'JPEG', quality=88); rel.target_part._blob = buf.getvalue()
d.para('Tangkapan layar berikut berasal', 'Tangkapan layar berikut berasal dari uji otomatis LPN-01 (emulator, 22 November 2026) pada folder code_area/pengujian/bukti/2026-11-22/. Gambar ini bukan hasil gladi dan hanya menunjukkan sisi Android.')
d.para('Gambar 6.10 Lintas platform', 'Gambar 6.10 Lintas platform LPN-01: sisi Android (DEM-13).')
d.save(BO)
