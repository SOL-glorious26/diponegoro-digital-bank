"""D01 PRJ-DDB-001 v0.9.5 -> v1.0 (22 Nov 2026). Pemakaian: python patch_d01_100.py <in.docx> <out.docx> <tanda_tangan.png>"""
import copy, sys, docx
from docx.shared import Cm
SRC, OUT, SIGN = sys.argv[1:4]
d = docx.Document(SRC)
done = []


def settext(p, text):
    runs = p.runs
    assert runs, p.text
    runs[0].text = text
    for r in runs[1:]:
        r.text = ''


def setcell(c, text):
    settext(c.paragraphs[0], text)
    for p in c.paragraphs[1:]:
        p._p.getparent().remove(p._p)


def para(prefix, text):
    hit = [p for p in d.paragraphs if p.text.startswith(prefix)]
    assert len(hit) == 1, (prefix, len(hit))
    settext(hit[0], text); done.append(prefix[:40])


def row(first, values, contains=None):
    hit = [r for t in d.tables for r in t.rows if r.cells[0].text.strip() == first and (contains is None or any(contains in c.text for c in r.cells))]
    assert len(hit) == 1, (first, contains, len(hit))
    for i, v in values.items():
        setcell(hit[0].cells[i], v)
    done.append('row ' + first); return hit[0]


def sub(a, b):
    n = 0
    for p in [p for p in d.paragraphs] + [p for t in d.tables for r in t.rows for c in r.cells for p in c.paragraphs]:
        if a in p.text:
            settext(p, p.text.replace(a, b)); n += 1
    assert n, a
    done.append('sub ' + a[:30])


# ---- kontrol dokumen
row('Status', {1: 'Final v1.0 – diterbitkan Project Manager 22 November 2026; wewenang review dan persetujuan dijalankan Project Manager'})
row('Kode dokumen / versi', {1: 'PRJ-DDB-001 / 1.0'})
row('Dokumen acuan', {1: 'SRS-DDB-001 v0.1; Project Brief & Scope Document v1.0; MDTA-DDB-001 v0.2; DWP-DDB-001 v1.5; SAD-DDB-001 v1.0; FSD-DDB-001 v1.0; STD-DDB-001 v1.0; DVM-DDB-001-AND v1.0; User Interface Map (Web) v0.7; repositori kode per 22 November 2026'})
t = d.tables[1]
new = copy.deepcopy(t.rows[-1]._tr)
t.rows[-1]._tr.addnext(new)
r = t.rows[-1]
for c, v in zip(r.cells, ['1.0', '22 Nov 2026', 'Salomo P.G. Hokoyoku', 'Versi final. Web resmi ditetapkan aplikasi Web tim (Node/Express/EJS) dan peran tim diperbarui (1.6, 1.7); hasil uji disamakan dengan STD-DDB-001 v1.0: Android 28/28 (27 pada build 22 November), Web tim 14/14 alur inti, lintas platform 3/3, 14 defect ditutup; status dokumen 9 kategori; lembar pengesahan ditandatangani Project Manager. Disetujui PM.']):
    setcell(c, v)

# ---- lembar pengesahan: hanya tanda tangan PM
cell = d.tables[2].rows[0].cells[0]
for p in cell.paragraphs:
    if '____ November 2026' in p.text:
        settext(p, p.text.replace('____ November 2026', '22 November 2026'))
blank = [p for p in cell.paragraphs if not p.text.strip()]
assert blank, [p.text for p in cell.paragraphs]
blank[0].alignment = cell.paragraphs[0].alignment
blank[0].add_run().add_picture(SIGN, width=Cm(2.6))
for p in blank[1:]:
    p._p.getparent().remove(p._p)
para('Catatan: lembar ini ditandatangani', 'Catatan: lembar ini ditandatangani Project Manager pada versi final 1.0; kolom Dosen Pengampu diisi saat pengesahan.')
[p for p in d.paragraphs if p.text.startswith('Catatan: lembar ini')][0].runs[0].bold = False

# ---- 1.6 - 1.9
row('Lapisan', {2: 'Status per 22 Nov 2026'}, 'Status per 21 Nov 2026')
row('Aplikasi Android', {2: '8 modul; varian demo dan online; tampilan diperbarui 21–22 Nov; 64/58 test otomatis (demo/online) lulus kecuali 1 test live dilewati; 28/28 kasus uji sistem lulus (27 pada build 22 Nov di emulator, SA-08 di ponsel fisik 14 Nov)'})
row('Aplikasi Web', {1: 'Web resmi (tim Web): Node.js, Express 5, EJS (digitalbanking-node v2.0.0). Prototipe pembanding: React 18, Vite 5 (apps/web_dummy_buatan_sendiri)', 2: 'Web tim: 14/14 alur inti lulus terhadap backend asli, sebagian FR belum tersedia. Prototipe React: 15/15 kasus lulus'})
para('Tim terdiri atas delapan anggota', 'Tim terdiri atas delapan anggota dalam dua kelompok platform. Pembagian modul Android mengikuti README tim Mobile (submit_mobile/README.md). Backend Spring Boot dan basis data yang dipakai bersama dikerjakan oleh Salomo, yang berperan sebagai full stack developer di tim Mobile; anggota Mobile lain mengerjakan front end. Di tim Web, Permata Surya Cahyana mengerjakan backend Web (server Node), Rafi Fadhillah front end dan backend Web, sedangkan Steven Wibisana dan Nabila Puspa Annastasya mengerjakan front end.')
row('622024022', {3: 'Project Manager; full stack developer Mobile: Authentication, Virtual Card, integrasi aplikasi Android; seluruh Backend & Database bersama'})
row('622024019', {3: 'Front end Android: Transfer, Transaction History & Dashboard'})
row('622024006', {3: 'Front end Android: Payment (QRIS), Profile & Notification'})
row('622024010', {3: 'Front end Android: Account, Top-up'})
row('622024008', {3: 'Front end Web'})
row('622024003', {3: 'Front end Web'})
row('622024005', {3: 'Front end dan backend Web (server Node)'})
row('622024016', {3: 'Backend Web (server Node)'})
para('Tim Web melaporkan PIC Sprint 1', 'Tim Web melaporkan PIC Sprint 1 pada 11 November 2026 (Surya: Login; Steven: Transfer bagian A; Rafi: Dashboard dan Transfer bagian B; Nabila: Riwayat), tetapi integrasi API belum dimulai. Untuk menjaga jadwal, Project Manager membuat prototipe Web React yang terhubung ke backend pada 11 November 2026. Tim Web kemudian menyelesaikan aplikasinya sendiri (Node/Express/EJS) yang memakai backend bersama, dan pada 22 November 2026 aplikasi tersebut ditetapkan sebagai Web resmi.')
for p in d.paragraphs:
    if p.text.startswith('1.9 Status Pengembangan per 21 November 2026') and p.style.name.startswith('Heading'):
        settext(p, '1.9 Status Pengembangan per 22 November 2026'); done.append('1.9')
row('Android', {1: '8 modul (FR-01–FR-25, FR-27–FR-29); varian demo dan online; integrasi REST ke backend selesai 11 Nov; tampilan diperbarui 21–22 Nov (tema Midnight Navy, PIN keypad responsif, Beranda, bottom navigation lima tab, layar Kartu)',
               2: '64 test demo (63 lulus, 1 dilewati) dan 58 test online (57 lulus, 1 dilewati), 22 Nov 2026; 28 dari 28 kasus uji sistem lulus: 27 dijalankan ulang di emulator pada build 22 Nov dan SA-08 (pindai QRIS dengan kamera) di ponsel fisik Realme RMX3867, Android 16, 14 Nov 2026 (STD-DDB-001 v1.0)'}, '64 test demo')
row('Web', {1: 'Web resmi: aplikasi tim Web (Node/Express/EJS, 11 halaman WEB-01–WEB-11) dengan autentikasi, e-KYC, PIN, dashboard, transfer, pembayaran, riwayat dan ekspor, profil. Prototipe pembanding: Web React (21 halaman)',
            2: 'Web tim: 14/14 alur inti (WN) dan 3/3 skenario lintas platform (LPN) lulus terhadap backend asli, 22 Nov 2026. Web React: 38 uji kontrak API; 15/15 kasus uji; 4/4 lintas platform (STD-DDB-001 v1.0)',
            3: 'Kantong, kartu virtual, notifikasi, grafik saldo, favorit, QRIS pribadi, dan paket data di Web tim; uji antar-peramban; D05W ke v1.0'}, 'User Interface Map (Web) v0.3')
row('Dokumentasi', {1: 'Versi final 1.0: Dokumen 1, 2, 3, 4 (Android, Web, Backend & Database), 5 (Android), 6, 7 (Android, Web, Backend), 8 (Android, Web), dan 9 (Android, Web). Dokumen 5 volume Web pada v0.7; Panduan Uji Ponsel dan Panduan Menjalankan Web',
                    3: 'Dokumen 5 volume Web ke v1.0'}, 'Folder documents_area')

# ---- Gantt
row('7', {4: 'Selesai', 5: 'Prototipe React terhubung ke backend (11 Nov 2026; 15/15); Web resmi tim Web diterima dan diuji 22 Nov 2026: 14/14 alur inti (STD-DDB-001 v1.0)'}, 'apps/web terhubung')
row('13', {4: 'Selesai (kecuali D05W v0.7)', 5: 'DWP-DDB-001; tenggat pengumpulan 23 Nov 2026 23.59 WIB'}, 'pengumpulan 24 Nov')
sub('dan pengumpulan dokumentasi (24 Nov 2026 pukul 23.59 WIB).', 'dan pengumpulan dokumentasi (23 Nov 2026 pukul 23.59 WIB; DWP-DDB-001 semula merencanakan 24 Nov).')
sub('Catatan baris 7: rentang 3 Oktober–11 November mencakup prototipe antarmuka tim Web (7 Oktober 2026) dan implementasi aplikasi Web yang terhubung ke backend oleh Project Manager (11 November 2026).',
    'Catatan baris 7: rentang 3 Oktober–11 November mencakup prototipe antarmuka tim Web (7 Oktober 2026) dan prototipe Web React yang terhubung ke backend oleh Project Manager (11 November 2026); aplikasi Web tim (Node) diterima pada 22 November 2026.')

# ---- pengujian
sub('headless untuk uji otomatis); Vite dev server dengan proxy /api', 'headless untuk uji otomatis) dengan Vite dev server untuk Web React; Node.js 24 untuk Web tim')
para('Desain kasus uji dan eksekusi pengujian sistem selesai lebih awal', 'Desain kasus uji dan eksekusi pertama pengujian sistem selesai pada 11–14 November 2026 dengan bantuan otomasi (folder pengujian/); kasus Android dan lintas platform dijalankan ulang pada 22 November 2026. Hasil pada STD-DDB-001 v1.0: 47 test integrasi backend lulus; 63 dari 64 test Android varian demo dan 57 dari 58 test varian online lulus (masing-masing 1 test live dilewati); cakupan baris test backend 92,2% (JaCoCo); 28 dari 28 kasus uji sistem Android lulus, yaitu 27 kasus pada build 22 November 2026 di emulator dan SA-08 (pindai QRIS dengan kamera) di ponsel fisik Realme RMX3867 pada 14 November 2026 dengan build sebelumnya; 14 dari 14 alur inti Web tim dan 3 dari 3 skenario lintas platform Android–Web tim; 15 dari 15 kasus dan 4 dari 4 skenario lintas platform pada prototipe Web React; 8 dari 9 kasus non-fungsional lulus, termasuk performa dasar NFR-01 sampai NFR-03. NF-04 (rate limit di gateway) terblokir sampai Docker terpasang. Pengujian menemukan 14 defect, termasuk satu Kritis dan tiga Tinggi; seluruhnya sudah diperbaiki dan ditutup. DEF-12 (detail kartu virtual) menunjukkan bahwa SA-13 sebelumnya lulus keliru; pemeriksaannya telah diperketat. Tahap yang tersisa adalah pengulangan SA-08 di ponsel fisik pada build terbaru dan uji antar-peramban Web tim.')
para('Rencana cadangan aplikasi Web (R11)', 'Rencana cadangan aplikasi Web (R11) tidak diperlukan. Cakupan minimum Web (Login, Dashboard, Transfer, dan Riwayat) tersedia pada prototipe Web React sejak 11 November 2026 dan pada Web tim. Konsistensi lintas platform (R8) diverifikasi melalui LP-01 sampai LP-04 (Web React) dan LPN-01 sampai LPN-03 (Web tim).')
sub('Nilai P dan D pada Tabel 4.1 adalah penilaian per 16 November 2026', 'Nilai P dan D pada Tabel 4.1 adalah penilaian per 22 November 2026')
sub('NF-01 sampai NF-03 lulus (STD-DDB-001 v0.9.7)', 'NF-01 sampai NF-03 lulus (STD-DDB-001 v1.0)')
row('R8', {5: 'Satu backend dan kontrak OpenAPI; uji lintas platform LP-01–LP-04 (Web React) dan LPN-01–LPN-03 (Web tim) lulus'})
row('R11', {5: 'Prototipe Web React dibuat PM (11 Nov; 15/15 lulus); Web tim diterima 22 Nov dan alur intinya lulus 14/14; fitur yang belum ada dicatat di STD-DDB-001 5.4.1'})
sub('termasuk aplikasi Web (1,943 KSLOC JavaScript/JSX; stylesheet CSS tidak dihitung)', 'termasuk prototipe Web React (1,943 KSLOC JavaScript/JSX; stylesheet CSS tidak dihitung)')
sub('Aplikasi Web (React, JavaScript/JSX; CSS 0,269 KSLOC tidak dihitung)', 'Prototipe Web React (JavaScript/JSX; CSS 0,269 KSLOC tidak dihitung)')
d.paragraphs  # noqa
hit = [p for p in d.paragraphs if p.text.startswith('Seluruh ukuran kode diukur dari repositori pada 12 November 2026')]
settext(hit[0], hit[0].text + ' Aplikasi Web tim (Node) diterima setelah pengukuran dan tidak termasuk dalam estimasi.')
para('Dokumen ini merangkum deskripsi', 'Dokumen ini merangkum deskripsi, jadwal, rencana pengujian, risiko, dan estimasi biaya proyek Diponegoro Digital Bank per 22 November 2026. Backend dan aplikasi Android telah mencapai cakupan fungsional SRS. Aplikasi Web resmi adalah aplikasi tim Web (Node/Express/EJS) yang memakai backend yang sama; alur intinya lulus uji, tetapi sebagian kebutuhan belum tersedia dan dicatat terbuka pada SAD-DDB-001 dan STD-DDB-001. Pengujian menghasilkan 28 dari 28 kasus Android, 14 dari 14 alur inti Web tim, dan 3 dari 3 skenario lintas platform lulus, serta seluruh 14 defect ditutup. Pekerjaan yang tersisa adalah API Gateway dan deployment (termasuk NF-04), pengulangan uji kamera di ponsel fisik, kelengkapan fitur Web, serta pengesahan dosen pengampu. Dokumen ini diperbarui setiap kali status proyek berubah dan dicatat pada revision control.')

# ---- paragraf isi yang seluruhnya tebal (sisa patch lama: semua teks masuk ke run pertama yang tebal)
for p in d.paragraphs:
    act = [r_ for r_ in p.runs if r_.text.strip()]
    if not p.style.name.startswith('Heading') and len(act) == 1 and act[0].bold and len(p.text) > 120:
        act[0].bold = False; done.append('tebal')

# ---- footer dan sampul
for s in d.sections:
    for p in s.footer.paragraphs:
        for r_ in p.runs:
            if '0.9.5' in r_.text:  # jangan menyentuh run lain: isinya field nomor halaman
                r_.text = r_.text.replace('0.9.5', '1.0')
d.save(OUT)
import zipfile, re
x = zipfile.ZipFile(OUT).read('word/document.xml').decode()
tx = re.sub(r'<[^>]+>', '', x)
print(len(done), 'perubahan; sisa v0.9.7:', tx.count('v0.9.7'), '| 24 Nov:', tx.count('24 Nov'), '| 11 defect:', tx.count('11 defect'), '| 0.9.5:', tx.count('0.9.5'))
