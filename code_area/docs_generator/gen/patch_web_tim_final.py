"""22 Nov 2026 malam: seluruh isi Web pada D01, D04W, D08W, dan DWP mengikuti Web resmi tim Web; COCOMO D01 diukur ulang.
Masukan: berkas di documents_area (D01/D04W/D08W v1.0, DWP v1.5). Pemakaian:
python patch_web_tim_final.py <documents_area> <d1_gantt.png> <folder keluaran>
"""
import io, os, re, sys, zipfile, docx
from PIL import Image
DA, GANTT, OUT = sys.argv[1:4]


class Doc:
    def __init__(self, p):
        self.p = p; self.d = docx.Document(p); self.n = 0

    def settext(self, p, text):
        runs = [r for r in p.runs if r.text] or p.runs
        if runs:
            runs[0].text = text
            for r in runs[1:]:
                r.text = ''
        else:
            p.add_run(text)
        self.n += 1

    def setcell(self, c, text):
        self.settext(c.paragraphs[0], text)
        for p in c.paragraphs[1:]:
            p._p.getparent().remove(p._p)

    def all(self):
        return list(self.d.paragraphs) + [p for t in self.d.tables for r in t.rows for c in r.cells for p in c.paragraphs]

    def sub(self, a, b):
        k = 0
        for p in self.all():
            if a in p.text:
                self.settext(p, p.text.replace(a, b)); k += 1
        assert k, a

    def para(self, start, text):
        hit = [p for p in self.d.paragraphs if p.text.startswith(start)]
        assert len(hit) == 1, (start, len(hit)); self.settext(hit[0], text)

    def row(self, t, first, vals):
        hit = [r for r in self.d.tables[t].rows if r.cells[0].text.strip() == first]
        assert len(hit) == 1, (t, first, len(hit))
        for c, v in vals.items():
            self.setcell(hit[0].cells[c], v)

    def save(self, name=None):
        for q in self.d.paragraphs:
            act = [r for r in q.runs if r.text.strip()]
            if not q.style.name.startswith('Heading') and len(act) == 1 and act[0].bold and len(q.text) > 110:
                act[0].bold = False
        o = os.path.join(OUT, name or os.path.basename(self.p)); self.d.save(o); print(os.path.basename(o), self.n); return o


# ------------------------------------------------------------------ D01
d = Doc(DA + '/D01_Project_Document/DDB_D01_Project_Document_v1.0.docx')
d.sub('User Interface Map (Web) v0.7', 'UIM-DDB-001-WEB v1.0')
d.row(1, '1.0', {3: 'Versi final. Web resmi ditetapkan aplikasi Web tim (Node/Express/EJS) dan peran tim diperbarui (1.6, 1.7); hasil uji menurut STD-DDB-001 v1.0; COCOMO II diukur ulang 22 Nov (15,944 KSLOC, ±Rp683 juta); Gantt dan status dokumen diperbarui. Disetujui PM.'})
d.sub('28/28 kasus uji sistem lulus (27 pada build 22 Nov di emulator, SA-08 di ponsel fisik 14 Nov)', '28/28 kasus uji sistem lulus pada APK final (22 Nov)')
d.row(6, 'Aplikasi Web', {1: 'Aplikasi tim Web: Node.js, Express 5, EJS (digitalbanking-node v2.0.0)', 2: '25/25 kasus lulus terhadap backend asli; Chrome, Edge, Firefox lulus; sebagian FR tidak dimuat (cakupan tim Web)'})
d.para('Tim Web melaporkan PIC Sprint 1', 'Tim Web melaporkan PIC Sprint 1 pada 11 November 2026 (Surya: Login; Steven: Transfer bagian A; Rafi: Dashboard dan Transfer bagian B; Nabila: Riwayat), tetapi integrasi API saat itu belum dimulai. Tim Web kemudian menyelesaikan aplikasinya (Node/Express/EJS) yang memakai backend bersama, dan pada 22 November 2026 aplikasi tersebut ditetapkan sebagai Web resmi.')
d.sub('28 dari 28 kasus uji sistem lulus: 27 dijalankan ulang di emulator pada build 22 Nov dan SA-08 (pindai QRIS dengan kamera) di ponsel fisik Realme RMX3867, Android 16, 14 Nov 2026 (STD-DDB-001 v1.0)', '28 dari 28 kasus uji sistem lulus pada APK final di emulator (22 Nov 2026), termasuk SA-08 dengan kamera emulator; SA-08, notifikasi, dan berbagi bukti juga lulus di ponsel fisik Realme RMX3867 pada 14 Nov 2026 (STD-DDB-001 v1.0)')
d.row(8, 'Web', {1: 'Aplikasi tim Web (Node/Express/EJS, 11 halaman WEB-01–WEB-11) dengan autentikasi, e-KYC, PIN, dashboard, transfer, pembayaran, riwayat dan ekspor, profil', 2: '25/25 kasus (WN), 3/3 skenario lintas platform (LPN), dan Chrome/Edge/Firefox lulus terhadap backend asli, 22 Nov 2026 (STD-DDB-001 v1.0)', 3: 'Tidak ada. Kantong, kartu virtual, notifikasi, grafik saldo, favorit, QRIS pribadi, dan paket data tidak dimuat sesuai cakupan tim Web'})
d.sub('Versi final 1.0: Dokumen 1, 2, 3, 4 (Android, Web, Backend & Database), 5 (Android), 6, 7 (Android, Web, Backend), 8 (Android, Web), dan 9 (Android, Web). Dokumen 5 volume Web pada v0.7; Panduan', 'Versi final 1.0: Dokumen 1, 2, 3, 4 (Android, Web, Backend & Database), 5 (Android, Web), 6, 7 (Android, Web, Backend), 8 (Android, Web), dan 9 (Android, Web); Panduan')
d.sub('Dokumen 5 volume Web ke v1.0', 'Gladi demo (Dokumen 8)')
d.row(9, '7', {3: '22 Nov', 5: 'Aplikasi tim Web (digitalbanking-node v2.0.0) terhubung ke backend; 25/25 kasus lulus (STD-DDB-001 v1.0)'})
d.row(9, '11', {3: '22 Nov', 5: 'STD-DDB-001 v1.0: Android 28/28, Web 25/25, lintas platform 3/3; 14 defect ditutup'})
d.row(9, '13', {4: 'Selesai'})
p62 = [p for p in d.d.paragraphs if p.text.startswith('Catatan baris 7')][0]
d.settext(p62, 'Catatan baris 7: rentang 3 Oktober–22 November mencakup prototipe antarmuka tim Web (7 Oktober 2026) sampai aplikasi tim Web (Node) diterima dan diuji terhadap backend bersama (22 November 2026). ' + p62.text[p62.text.index('Catatan baris 8'):])
d.sub('Chrome 157, Edge 157, Firefox 156 pada laptop Windows (headless untuk uji otomatis) dengan Vite dev server untuk Web React; Node.js 24 untuk Web tim', 'Chrome 157, Edge 158, Firefox 156 pada laptop Windows (headless untuk uji otomatis); server Web Node.js 24')
d.sub('kasus Android dan lintas platform dijalankan ulang pada 22 November 2026.', 'kasus Android, Web, dan lintas platform dijalankan pada kode terbaru tanggal 22 November 2026.')
d.sub('28 dari 28 kasus uji sistem Android lulus, yaitu 27 kasus pada build 22 November 2026 di emulator dan SA-08 (pindai QRIS dengan kamera) di ponsel fisik Realme RMX3867 pada 14 November 2026 dengan build sebelumnya; 14 dari 14 alur inti Web tim dan 3 dari 3 skenario lintas platform Android–Web tim; 15 dari 15 kasus dan 4 dari 4 skenario lintas platform pada prototipe Web React; 8 dari 9', '28 dari 28 kasus uji sistem Android lulus pada APK final di emulator (SA-08 dengan kamera emulator; SA-08 juga lulus di ponsel fisik Realme RMX3867 pada 14 November 2026 dengan build sebelumnya); 25 dari 25 kasus Web dan 3 dari 3 skenario lintas platform Android–Web; 8 dari 9')
d.sub('Tahap yang tersisa adalah pengulangan SA-08 di ponsel fisik pada build terbaru dan uji antar-peramban Web tim.', 'Tahap yang tersisa adalah pengulangan kamera, notifikasi, dan berbagi bukti di ponsel fisik pada APK final.')
d.para('Rencana cadangan aplikasi Web (R11)', 'Rencana cadangan aplikasi Web (R11) tidak diperlukan karena cakupan minimum Web (Login, Dashboard, Transfer, dan Riwayat) tersedia pada aplikasi tim Web. Konsistensi lintas platform (R8) diverifikasi melalui LPN-01 sampai LPN-03.')
d.sub('uji lintas platform LP-01–LP-04 (Web React) dan LPN-01–LPN-03 (Web tim) lulus', 'uji lintas platform LPN-01–LPN-03 lulus')
d.row(13, 'R11', {5: 'Aplikasi tim Web diterima 22 Nov dan lulus 25/25 kasus; fitur yang tidak dimuat dicatat di STD-DDB-001 5.4'})
d.sub('R11 termitigasi setelah aplikasi Web diimplementasikan dan terhubung ke backend pada 11 November 2026, sehingga', 'R11 termitigasi setelah aplikasi tim Web terhubung ke backend dan lulus uji pada 22 November 2026, sehingga')
d.sub('skenario lintas platform LP-01 sampai LP-04 lulus', 'skenario lintas platform LPN-01 sampai LPN-03 lulus')
d.sub('(DWP-DDB-001) v1.3', '(DWP-DDB-001) v1.5')
d.sub('MDTA-DDB-001 v0.2', 'MDTA-DDB-001 v0.3')
# COCOMO (sloc: baris non-kosong, non-komentar; metode sama: Android 14 Nov terhitung 9,235 dengan skrip ini vs 9,227 pada 12 Nov)
d.sub('yang dihitung dari repositori pada 12 November 2026', 'yang dihitung dari repositori pada 22 November 2026')
d.row(14, 'Backend (Java, main)', {2: '4,696'})
d.row(14, 'Skema basis data (SQL migrasi)', {2: '0,194'})
d.row(14, 'Aplikasi Android (Kotlin, main, termasuk lapisan REST Fase 5)', {0: 'Aplikasi Android (Kotlin, main)', 1: '112', 2: '9,935'})
d.row(14, 'Prototipe Web React (JavaScript/JSX; CSS 0,269 KSLOC tidak dihitung)', {0: 'Aplikasi Web tim (JavaScript 0,636 dan templat EJS 0,483; CSS 0,136 KSLOC tidak dihitung)', 1: '20', 2: '1,119'})
d.row(14, 'Total ukuran (seluruh komponen terealisasi dan diukur)', {2: '15,944'})
d.sub('kode uji (1,934 KSLOC) tidak dihitung', 'kode uji (2,070 KSLOC) tidak dihitung')
d.row(15, 'Terealisasi per 11 Nov 2026 (sebelum aplikasi Web)', {0: 'Backend, basis data, dan Android (tanpa Web)', 1: '14,825', 2: '57,03', 3: '13,27', 4: '4,30'})
d.row(15, 'Terealisasi per 12 Nov 2026 (termasuk aplikasi Web)', {0: 'Seluruh komponen per 22 Nov 2026', 1: '15,944', 2: '61,78', 3: '13,61', 4: '4,54'})
d.sub('ukuran terealisasi per 12 November 2026', 'ukuran terealisasi per 22 November 2026')
d.sub('(ukuran terealisasi 12 Nov 2026)', '(ukuran terealisasi 22 Nov 2026)')
d.row(16, '1', {2: '61,78 PM × Rp10.000.000', 3: 'Rp617.793.828'})
d.row(16, '7', {3: 'Rp62.084.383'})
d.sub('Rp689.540.065', 'Rp682.928.211')
d.row(17, 'Rp7.500.000', {1: 'Rp463.345.371', 2: 'Rp513.034.908'})
d.row(17, 'Rp10.000.000', {1: 'Rp617.793.828'})
d.row(17, 'Rp12.500.000', {1: 'Rp772.242.285', 2: 'Rp852.821.514'})
d.sub('ukuran ±16,1 KSLOC memerlukan sekitar 62 person-month dalam ±13,7 bulan', 'ukuran ±15,9 KSLOC memerlukan sekitar 62 person-month dalam ±13,6 bulan')
d.sub('(Spring Boot, Jetpack Compose, React)', '(Spring Boot, Jetpack Compose, Express)')
d.sub('dibulatkan menjadi ±Rp690 juta', 'dibulatkan menjadi ±Rp683 juta')
d.sub('Ukuran Web yang terukur (1,943 KSLOC) lebih kecil daripada asumsi sebelumnya (±4,000 KSLOC) sehingga estimasi turun dari ±Rp785 juta pada versi 0.9.2.', 'Dibanding pengukuran 12 November 2026 (16,085 KSLOC, ±Rp690 juta), Android bertambah karena pembaruan tampilan, sedangkan ukuran Web lebih kecil karena memakai aplikasi tim Web.')
d.sub('sudah dikeluarkan tim sampai 12 November 2026', 'sudah dikeluarkan tim sampai 22 November 2026')
d.para('Seluruh ukuran kode diukur dari repositori', 'Seluruh ukuran kode diukur dari repositori pada 22 November 2026. Aplikasi Web yang dihitung adalah aplikasi tim Web (JavaScript dan templat EJS; CSS dan pustaka pihak ketiga tidak dihitung).')
d.sub('alur intinya lulus uji, tetapi sebagian kebutuhan belum tersedia dan dicatat terbuka pada SAD-DDB-001 dan STD-DDB-001', 'seluruh 25 kasus ujinya lulus; sebagian kebutuhan tidak dimuat sesuai cakupan yang dipilih tim Web dan dicatat pada SAD-DDB-001 dan STD-DDB-001')
d.sub('14 dari 14 alur inti Web tim, dan 3 dari 3', '25 dari 25 kasus Web, dan 3 dari 3')
d.sub('pengulangan uji kamera di ponsel fisik, kelengkapan fitur Web, serta', 'pengulangan uji ponsel fisik pada APK final, serta')
tmp = d.save('d01_tmp.docx')
# ganti gambar Gantt
o = os.path.join(OUT, 'd01_raw.docx')
img = [r.target_ref for r in d.d.part.rels.values() if 'image' in r.reltype]
with zipfile.ZipFile(tmp) as z, zipfile.ZipFile(o, 'w', zipfile.ZIP_DEFLATED) as w:
    names = z.namelist()
    old = [n for n in names if n.startswith('word/media/') and Image.open(io.BytesIO(z.read(n))).size == (2270, 989)]
    assert len(old) == 1, old
    for n in names:
        w.writestr(n, open(GANTT, 'rb').read() if n == old[0] else z.read(n))
os.remove(tmp)

# ------------------------------------------------------------------ D04W
d = Doc(DA + '/D04W_Detailed_Design_Web/DDB_D04W_Detailed_Design_Web_v1.0.docx')
d.sub('UIM-DDB-001-WEB v0.7; STD', 'UIM-DDB-001-WEB v1.0; STD')
d.row(2, '1.0', {3: 'Versi final; kasus WN-15–WN-25, uji Chrome/Edge/Firefox, dan rujukan UIM-DDB-001-WEB v1.0 ditambahkan; perbandingan dengan rancangan Web lama dihapus. Disetujui PM.'})
d.sub('1.4 Perubahan terhadap Versi 0.6', '1.4 Ringkasan Rancangan')
d.para('Tabel 1.1 Perbandingan Web React', 'Tabel 1.1 Ringkasan rancangan Web')
t = d.d.tables[4]
assert t.rows[0].cells[1].text.startswith('v0.6')
W = docx.oxml.ns.qn
grid = t._tbl.tblGrid.findall(W('w:gridCol')); w1 = int(grid[1].get(W('w:w')))
for r in t.rows:
    tcs = r._tr.findall(W('w:tc')); r._tr.remove(tcs[1])
    e = tcs[2].tcPr.find(W('w:tcW'))
    if e is not None and e.get(W('w:type')) == 'dxa':
        e.set(W('w:w'), str(int(e.get(W('w:w'))) + w1))
grid[2].set(W('w:w'), str(int(grid[2].get(W('w:w'))) + w1)); t._tbl.tblGrid.remove(grid[1])
d.row(4, 'Aspek', {1: 'Web (Node.js, digitalbanking-node v2.0.0)'})
d.row(4, 'Hasil uji', {1: 'WN-01 s.d. WN-25 dan LPN-01 s.d. LPN-03 lulus; Chrome, Edge, Firefox lulus (STD-DDB-001 v1.0)'})
d.para('Kode halaman mengikuti UIM-DDB-001-WEB', 'Kode halaman mengikuti UIM-DDB-001-WEB v1.0 (WEB-01 s.d. WEB-11). Bukti uji adalah WN (aplikasi Web terhadap backend asli) dan LPN (lintas platform dengan Android), 22 November 2026.')
d.para('Uji alur terhadap backend asli dijalankan', 'Uji terhadap backend asli dijalankan pada 22 November 2026: WN-01 s.d. WN-14 (pengujian/web/node-smoke.mjs), WN-15 s.d. WN-25 (node-ekstra.mjs; termasuk rekening dan ID PLN tidak valid, Top Up, ubah profil dengan OTP, ubah kata sandi, pemulihan, token kedaluwarsa, blokir PIN, dan CSRF), LPN-01 s.d. LPN-03 (lintas platform dengan Android), serta alur inti di Chrome, Edge, dan Firefox (node-peramban.mjs). Seluruhnya lulus tanpa error JavaScript. Tampilan ponsel belum diuji.')
d.row(10, 'Lupa sandi', {2: 'WEB-11: WN-20'})
d.row(10, 'Profil', {2: 'WEB-10: WN-18 (OTP), WN-19 (kata sandi)'})
d.row(10, 'Daftar, e-KYC, Buat PIN', {2: 'WEB-02 s.d. WEB-05: WN-13, WN-23'})
d.row(10, 'Transfer', {2: 'WEB-07: WN-03 s.d. WN-06, WN-15, WN-24'})
d.row(10, 'Top Up saldo, QRIS (tampil dan bayar contoh kode)', {2: 'WEB-08: WN-11 (QRIS), WN-17 (Top Up)'})
d.sub('dijalankan terhadap backend asli melalui WN-01 s.d. WN-14 kecuali profil dan pemulihan', 'dijalankan terhadap backend asli melalui WN-01 s.d. WN-25')
d.sub('WN 14/14 dan LPN 3/3 lulus', 'WN 25/25 dan LPN 3/3 lulus'); d.sub('node-smoke.mjs dan lp-node.mjs, 22 Nov 2026', 'node-smoke.mjs, node-ekstra.mjs, dan lp-node.mjs, 22 Nov 2026')
d.save()

# ------------------------------------------------------------------ D08W
d = Doc(DA + '/D08W_System_Program_Demo_Web/DDB_D08W_System_Program_Demo_Web_v1.0.docx')
d.para('Dokumen ini memakai Web Node.js', 'Dokumen ini memakai Web Node.js (Express + EJS) sebagai Web resmi. Pada 22 November 2026 Web diuji otomatis terhadap backend asli: WN-01 s.d. WN-25 dan LPN-01 s.d. LPN-03 lulus, dan alur inti lulus di Chrome, Edge, dan Firefox (STD-DDB-001 v1.0). Tangkapan layar Web diambil otomatis pada hari yang sama. Uji otomatis itu bukan gladi. Tidak ada hasil gladi yang diklaim pada versi ini. Skenario Android rinci ada pada DEM-DDB-001-AND.')
d.para('Skenario hanya memuat fungsi yang ada', 'Skenario hanya memuat fungsi yang ada dan dapat dijalankan pada Web Node.js dan Android.')
d.para('Tabel 5.1 Perilaku sistem aktual', 'Tabel 5.1 Perilaku sistem aktual.')
d.para('Kinerja pada D06 (Web termuat', 'Kinerja menurut D06, diukur 22 November 2026 pada Web Node.js dan backend lokal: Dashboard termuat maksimal 197 ms, API baca p95 7 ms, dan transfer terlama 67 ms.')
d.para('Penting: tangkapan layar Web React', 'Tangkapan layar di bawah diambil otomatis di Chrome dari Web Node.js yang terhubung ke backend asli pada 22 November 2026 (pengujian/web/node-layar.mjs), bukan dari gladi.')
d.sub('D05 UI Map Web v0.7 (WEB-01..11)', 'D05 UI Map Web v1.0 (WEB-01..11)')
d.sub('WN-11 (QRIS); Top Up belum ada kasus', 'WN-11 (QRIS), WN-17 (Top Up)')
d.sub('27 pada build 22 Nov di emulator; SA-08 di ponsel fisik Realme RMX3867 pada 14 Nov (build sebelumnya)', 'APK final di emulator, 22 Nov; SA-08 dengan kamera emulator dan di ponsel fisik 14 Nov (build sebelumnya)')
d.row(11, 'Web', {1: 'WN 25 / 25 lulus', 2: 'Chrome, Edge, Firefox lulus'})
d.row(11, 'Lintas platform', {1: 'LPN 3 / 3 lulus', 2: 'Android dan Web'})
d.row(12, 'T-04', {1: 'Gladi belum dilakukan; tampilan ponsel Web belum diuji', 2: 'Waktu demo belum terbukti', 3: 'Lakukan gladi; pakai lebar layar desktop'})
d.save()

# ------------------------------------------------------------------ DWP
d = Doc(DA + '/D00_DWP_Work_Plan/DWP-DDB-001_Software_Engineering_Documentation_Work_Plan_v1.5.docx')
d.sub('D09A, D09W final v1.0, D05W v0.7; Web resmi', 'D09A, D09W, D05W final v1.0; Web resmi')
d.para('Tabel 3.1 adalah Master Documentation Matrix', 'Tabel 3.1 adalah Master Documentation Matrix (Artefak A). Status per 22 November 2026: seluruh volume (D01, D02, D03, D04A, D04B, D04W, D05A, D05W, D06, D07A, D07B, D07W, D08 Android dan Web, D09A, D09W) berstatus final v1.0. Web resmi adalah aplikasi tim Web (Node/Express/EJS). Wewenang review (R1–R3) dan persetujuan dijalankan Project Manager. Di luar sembilan kategori, tersedia artefak pendukung TUT-DDB-001-AND (Panduan Uji Aplikasi di Ponsel v1.1) dan TUT-DDB-002-WEB (Panduan Menjalankan dan Menguji Web v2.0).')
for r in d.d.tables[11].rows:
    if 'tangkapan layar sudah tersedia, belum disisipkan' in r.cells[-1].text:
        d.setcell(r.cells[-1], 'Final v1.0 (draf Nabila, diselesaikan PM; Web Node)')
    if 'Web tim WN 14/14' in r.cells[-1].text:
        d.setcell(r.cells[-1], 'Final v1.0: Android 28/28, Web 25/25, lintas platform 3/3, backend 47/47, NFR 8/9; 14 defect ditutup')
for r in d.d.tables[13].rows:
    k = r.cells[0].text.strip()
    if k.startswith('D05W'):
        d.setcell(r.cells[-1], 'Selesai (v1.0)' + ('; dijalankan PM' if 'WP4' in k else ''))
    new = {'D06-WP4-T01': 'Selesai: 28/28 pada APK final di emulator (22 Nov); SA-08 juga di ponsel fisik 14 Nov', 'D06-WP4-T02': 'Selesai: 25/25; Chrome, Edge, Firefox lulus', 'D06-WP4-T03': 'Selesai: backend 47/47; lintas platform 3/3',
           'D00-WP2-T01': 'Selesai: PostgreSQL; Web Node.js + Express + EJS (SAD-DDB-001 v1.0 Bab VIII, ADR-13, ADR-14)'}.get(k)
    if new:
        d.setcell(r.cells[-1], new)
for r in d.d.tables[20].rows:
    if r.cells[0].text.strip() == 'D05W':
        for c in (3, 4, 8):
            d.setcell(r.cells[c], '☑')
        for c in (5, 6, 7):
            d.setcell(r.cells[c], 'PM')
d.sub('Web ditetapkan PM (11 Nov 2026): React + JavaScript, Vite, axios dengan interceptor token dan Idempotency-Key, react-router-dom dengan route guard login dan e-KYC (SAD-DDB-001 v0.9.2 Bab VIII).', 'Web ditetapkan PM (22 Nov 2026): aplikasi tim Web Node.js + Express + EJS (SAD-DDB-001 v1.0 Bab VIII).')
d.sub('aplikasi Web (React + Vite) yang terhubung ke backend', 'aplikasi Web yang terhubung ke backend')
d.sub('QRIS Web hanya QRIS pribadi (FR-16)', 'tanpa pindai QRIS di Web')
d.save()
for k in ('D04W', 'D08W', 'DWP'):
    for f in os.listdir(OUT):
        if f.endswith('.docx') and k in f and 'raw' not in f:
            for p in Doc(os.path.join(OUT, f)).all():
                if re.search(r'React|Vite|\bWB-|LP-0|14/14|v0\.7|belum disisipkan', p.text):
                    print(k, '|', p.text[:170])
for p in Doc(o).all():
    if re.search(r'React|Vite|\bWB-|LP-0|14/14|14 dari 14|v0\.7|12 Nov|689|16,085|623', p.text):
        print('D01 |', p.text[:200])
