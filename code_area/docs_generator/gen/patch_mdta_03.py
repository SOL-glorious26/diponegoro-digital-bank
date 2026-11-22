"""MDTA-DDB-001 v0.2 -> v0.3 (22 Nov 2026): realisasi Web = aplikasi tim Web (Node/Express/EJS) dan backend Web oleh Surya dan Rafi.
Teks keputusan tetap hijau tebal. Pemakaian: python patch_mdta_03.py <v0.2.docx> <v0.3.docx>
"""
import copy, re, sys, docx
SRC, OUT = sys.argv[1:3]
d = docx.Document(SRC)
P = list(d.paragraphs)
GREEN = '1E6B3A'
n = 0


def is_green(r):
    c = r.font.color
    return c is not None and c.type is not None and str(c.rgb) == GREEN


def green(p, must, text):
    """ganti seluruh teks hijau pada paragraf (harus memuat `must`) dengan `text`."""
    global n
    g = [r for r in p.runs if is_green(r)]
    old = ''.join(r.text for r in g)
    assert g and must in re.sub(r'\s+', ' ', old), (must, old[:120])
    lead = ' ' if old.startswith(' ') else ''
    g[0].text = lead + text
    for r in g[1:]:
        r.text = ''
    n += 1


def at(start, k=0):
    hit = [p for p in P if p.text.startswith(start)]
    assert len(hit) > k, start
    return hit[k]


WEB = 'Node.js + Express 5 + EJS'
WEBTIM = 'front end: Steven, Nabila, dan Rafi; backend Web (server Node): Surya (Permata Surya Cahyana) dan Rafi'
green(at('Catatan penggunaan'), 'Versi 0.2', 'Versi 0.2 (17 Nov 2026): seluruh butir bertanda tersebut sudah diisi dengan keputusan yang diterapkan pada implementasi (teks hijau "Ditetapkan"/"Realisasi"), bersumber dari kode dan dokumen D02, D03, D04B. Versi 0.3 (22 Nov 2026): keputusan dan realisasi Web disamakan dengan aplikasi resmi tim Web (D02 v1.0 Bab VIII).')
green(at('Teknologi: Android memakai Kotlin'), 'React 18', f'Ditetapkan: Web memakai {WEB}; basis data PostgreSQL 17/18 dengan migrasi Flyway dan H2 untuk pengujian otomatis (SAD-DDB-001 ADR-03, ADR-13)')
green(at('Slot Backend dipakai'), 'dikerjakan Salomo', 'Realisasi: seluruh slot BE-1 s.d. BE-4 (Backend Spring Boot) beserta Database dikerjakan Salomo. Backend Web (server Node yang menghubungkan halaman Web ke Backend Spring Boot, tanpa basis data sendiri) dikerjakan Surya dan Rafi.')
green(at('Salomo berperan sebagai'), 'aplikasi Web', 'Realisasi: modul Android Salomo menjadi Authentication dan Virtual Card (submit_mobile/README.md), dan Salomo juga mengimplementasikan seluruh Backend Spring Boot dan Database (D01 bagian 1.7). Aplikasi Web dikerjakan tim Web.')
green(at('Implementasi backend dipegang slot'), 'seluruh backend', 'Realisasi: seluruh Backend Spring Boot diimplementasikan Salomo; backend Web (Node) oleh Surya dan Rafi.')
green(at('Usulan awal untuk dibahas tim'), 'akhirnya diimplementasikan Salomo', f'Realisasi pembagian Android (submit_mobile/README.md): Salomo – Authentication dan Virtual Card; Cecilia – Transfer dan Transaction History & Dashboard; Cia (Gracia) – Payment (QRIS) dan Profile & Notification; Grace – Account dan Top-up. Realisasi Web: aplikasi tim Web (digitalbanking-node v2.0.0, {WEB}) ditetapkan sebagai Web resmi pada 22 November 2026; {WEBTIM}. Cakupan yang dipilih tim Web tidak memuat kantong, kartu virtual, notifikasi, grafik saldo, penerima favorit, QRIS pribadi/dinamis, dan paket data; fitur tersebut tersedia di Android.')
green(at('Catatan: (1) Seluruh nama Backend'), 'diputuskan Salomo', 'diputuskan Salomo mengambil seluruh slot Backend Spring Boot (realisasi 10–11 Nov 2026, lihat D01 bagian 1.7); backend Web (Node) oleh Surya dan Rafi')
green(at('Nama anggota Backend dan pengisian'), 'dipegang Salomo', 'Ditetapkan: seluruh slot dipegang Salomo; backend Web (Node) dipegang Surya dan Rafi.')
green(at('Teknologi Web (framework) dan produk'), 'React 18', f'Ditetapkan: Web {WEB}; PostgreSQL 17/18 dengan Flyway (H2 untuk pengujian).')
green(at('Halaman e-KYC: formulir data KTP'), 'unggah berkas', 'Ditetapkan: Web mengisi NIK, nama, dan nama berkas swafoto sebagai teks (simulasi, tanpa unggah); verifikasi bersifat simulasi')
green(at('Penanganan sesi di peramban'), 'sessionStorage', 'Ditetapkan: token disimpan di sesi server Node; peramban hanya memegang cookie sesi httpOnly, SameSite=Lax (SAD-DDB-001 ADR-14)')
for k in (0, 1):
    green(at('Cara pengambilan swafoto di web', k), 'Web memakai unggah', 'Ditetapkan: Web memakai nama berkas sebagai teks referensi (simulasi), Android memakai swafoto simulasi; berkas tidak disimpan di server, hanya referensinya.')
green(at('Daftar bank (simulasi), CRUD'), 'di klien', 'Ditetapkan: di klien dari data detail transaksi (Android: gambar/PDF; Web: cetak dari detail riwayat)')
for k in (0, 1):
    green(at('Lokasi pembuatan bukti transfer', k), 'di klien', 'Ditetapkan: di klien; Android membuat gambar PNG atau PDF dan membagikannya melalui share sheet, Web mencetak dari detail transaksi.')
green(at('Tampilan notifikasi dalam aplikasi. Dukungan push'), 'kotak notifikasi', 'Ditetapkan: notifikasi tidak dimuat di Web (cakupan tim Web); tersedia di Android')
for k in (0, 1):
    green(at('Dukungan push pada web', k), 'tidak didukung', 'Ditetapkan: tidak didukung; notifikasi hanya tersedia di Android.')
green(d.tables[4].rows[1].cells[2].paragraphs[0], 'React 18', f'{WEB}. {WEBTIM[0].upper() + WEBTIM[1:]}')
green(d.tables[4].rows[3].cells[1].paragraphs[0], 'Salomo (seluruh slot', 'Salomo (seluruh slot BE-1 s.d. BE-4 dan Database); backend Web (Node): Surya dan Rafi')


def cell(c, text):
    r = [x for x in c.paragraphs[0].runs if x.text]; r[0].text = text
    for x in r[1:]:
        x.text = ''


t = d.tables[0]
cell(t.rows[2].cells[1], '0.3 / Realisasi Web disamakan dengan aplikasi tim Web')
cell(t.rows[7].cells[1], '4 Oktober 2026 (v0.1); 17 November 2026 (v0.2); 22 November 2026 (v0.3)')
t = d.tables[1]
t.rows[-1]._tr.addnext(copy.deepcopy(t.rows[-1]._tr))
for c, v in zip(t.rows[-1].cells, ['0.3', '22 Nov 2026', 'Salomo P.G. Hokoyoku', 'Teknologi dan realisasi Web disamakan dengan aplikasi resmi tim Web (Node.js, Express, EJS): sesi di server, e-KYC teks referensi, tanpa notifikasi; peran tim Web dan backend Web (Surya, Rafi) dicatat. Backend Spring Boot dan Database tetap Salomo. Rencana awal tidak diubah.']):
    cell(c, v)
for s in d.sections:
    for p in s.footer.paragraphs:
        for r in p.runs:
            if 'v0.2' in r.text:
                r.text = r.text.replace('v0.2', 'v0.3')
d.save(OUT)
x = docx.Document(OUT); tx = '\n'.join(p.text for p in x.paragraphs) + '\n'.join(c.text for t in x.tables for r in t.rows for c in r.cells)
print(n, 'keputusan diubah; sisa:', re.findall(r'.{30}(?:React|Vite|axios|sessionStorage|diimplementasikan Salomo; anggota).{20}', tx))
