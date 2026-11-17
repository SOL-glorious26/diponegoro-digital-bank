"""MDTA-DDB-001 v0.1 -> v0.2: isi semua butir 'Perlu Ditentukan' / 'Technical Decision Required'
dengan keputusan yang diterapkan pada implementasi (kode backend, Android, Web; D02 v0.9.5, D03 v0.9.3, D04B v0.9.2).

Pemakaian: python patch_mdta_02.py <folder hasil unzip docx>
Teks keputusan ditulis hijau tebal agar terlihat sebagai isian versi 0.2.
"""
import os, re, sys, glob, html
sys.stdout.reconfigure(encoding='utf-8')
BASE = sys.argv[1]
P = os.path.join(BASE, 'word', 'document.xml')
x = open(P, encoding='utf-8').read()

PARA = re.compile(r'<w:p[ >](?:(?!<w:p[ >]).)*?</w:p>', re.S)
RUN = re.compile(r'<w:r[ >].*?</w:r>', re.S)
MARK = ('Perlu Ditentukan', 'Technical Decision Required')
GREEN = '1E6B3A'


def esc(s): return s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
def rtext(r): return html.unescape(''.join(re.findall(r'<w:t[^>]*>([^<]*)</w:t>', r)))
def ptext(p): return ''.join(rtext(r) for r in RUN.findall(p))


def set_rtext(r, text):
    r = re.sub(r'<w:t[^>]*>[^<]*</w:t>', '', r)
    return r.replace('</w:r>', f'<w:t xml:space="preserve">{esc(text)}</w:t></w:r>')


def green(r, text):
    sz = re.search(r'<w:sz w:val="(\d+)"/>', r)
    sz = f'<w:sz w:val="{sz.group(1)}"/><w:szCs w:val="{sz.group(1)}"/>' if sz else ''
    rpr = f'<w:rPr><w:b/><w:bCs/><w:color w:val="{GREEN}"/>{sz}</w:rPr>'
    r = re.sub(r'<w:rPr>.*?</w:rPr>', '', r, flags=re.S)
    r = re.sub(r'^(<w:r[ >])', r'\1' + rpr, r) if False else r.replace('<w:r>', '<w:r>' + rpr, 1) if r.startswith('<w:r>') else re.sub(r'^(<w:r [^>]*>)', r'\1' + rpr, r)
    return set_rtext(r, text)


# ---------- keputusan per paragraf (indeks paragraf pada dokumen v0.1) ----------
# nilai: (daftar teks pengganti untuk tiap run penanda, {teks run lama: teks run baru})
EP = 'Endpoint detail: '
D = {
    50: (['Ditetapkan: Web memakai React 18 + Vite 5 (react-router-dom 6, axios); basis data PostgreSQL 17/18 dengan migrasi Flyway dan H2 untuk pengujian otomatis (SAD-DDB-001 ADR-03, ADR-11)'], {}),
    156: (['React 18 + Vite 5'], {' (SDD).': ' (SAD-DDB-001 Bab VIII).'}),
    161: (['Salomo (seluruh slot BE-1 s.d. BE-4 dan Database)'], {}),
    179: (['realisasi seluruhnya Salomo'], {}),
    345: (['Ditetapkan: unggah berkas gambar (JPG/PNG); verifikasi bersifat simulasi'], {}),
    348: (['Ditetapkan: access token di memori, refresh token di sessionStorage (SAD-DDB-001 ADR-12)'], {}),
    353: (['Ditetapkan: simulator di backend; kode OTP dikembalikan pada respons (demoCode) bila EXPOSE_OTP aktif dan salinan email ditulis ke email_outbox'], {}),
    356: (['Ditetapkan: 5 menit'], {}),
    363: (['Ditetapkan: berkas tidak disimpan di server; tabel ekyc_verifications hanya menyimpan referensi swafoto, NIK tersamar, dan hash NIK',
           'ditetapkan pada D04B (migrasi Flyway V1__init.sql)'], {' pada Database Design.': '.'}),
    371: (['Ditetapkan: OtpService (mode simulasi) dan simulasi e-KYC di AuthService (NIK berakhiran 0000 ditolak)'], {}),
    392: (['Salomo'], {}),
    452: (['Ditetapkan: di server lewat PUT /accounts/me/balance-visibility, sehingga sama di semua perangkat'], {}),
    453: (['Ditetapkan: grafik buatan sendiri dengan Compose Canvas (core/ui/Charts.kt), tanpa pustaka eksternal'], {}),
    466: (['Ditetapkan: kantong adalah akun ledger tersendiri, sehingga setiap pemindahan dicatat sebagai jurnal double-entry'], {}),
    467: (['Ditetapkan: tanpa penjadwal; status kunci dihitung saat data diakses (tanggal kunci sudah lewat atau saldo kantong mencapai target)'], {}),
    473: (['ditetapkan pada D04B'], {' pada Database Design.': ': tabel ledger_accounts (rekening dan kantong) dan pockets.'}),
    481: (['ditetapkan pada SAD-DDB-001'], {' pada Architecture Design.': ': kartu memakai saldo rekening utama sebagai sumber dana.'}),
    500: (['Salomo'], {}),
    579: (['Ditetapkan: dibuat klien (UUID) sekali per layar konfirmasi, dikirim lewat header Idempotency-Key dan dipakai ulang saat retry'], {}),
    588: (['Ditetapkan: biaya antarbank Rp2.500, sesama bank Rp0; maksimum Rp25.000.000 per transaksi (nilai usulan); limit harian belum diterapkan'], {}),
    593: (['Ditetapkan: di klien (Android dan Web) dari data detail transaksi'], {}),
    599: (['ditetapkan pada D04B'], {' pada Database Design.': ': tabel ledger_transactions (kunci idempotensi unik), journal_lines, dan favorite_recipients.'}),
    627: (['Salomo'], {}),
    684: (['Ditetapkan: didukung; pembayar mengisi nominal yang divalidasi Rp1.000–Rp10.000.000 dan terhadap saldo sebelum PIN'], {}),
    700: (['ditetapkan pada D04B'], {' pada Database Design.': ': memakai ledger_transactions dan journal_lines; kode QRIS dibangkitkan dari nomor rekening sehingga tidak perlu tabel tersendiri.'}),
    727: (['Salomo'], {}),
    787: (['Ditetapkan: katalog statis di backend (TopUpCatalog) yang dikirim ke klien lewat GET /topup/catalog'], {}),
    796: (['ditetapkan pada D04B'], {' pada Database Design.': ': ledger_transactions, journal_lines, dan paid_bills (tagihan terbayar).'}),
    823: (['Salomo'], {}),
    892: (['ditetapkan pada D04B'], {' pada Database Design.': ': tabel virtual_cards (nomor kartu dan CVV terenkripsi AES-GCM).'}),
    898: (['ditetapkan'], {' pada Architecture Design.': ': transaksi kartu mendebet rekening utama melalui ledger.'}),
    899: (['Ditetapkan: transaksi kartu disimulasikan lewat POST /cards/{id}/simulate-purchase, diposting ke ledger, dan muncul di riwayat dengan jenis Kartu Virtual'],
          {' (SRS belum mendefinisikan).': '.'}),
    919: (['Salomo'], {}),
    981: (['Ditetapkan: Compose Canvas (core/ui/Charts.kt), tanpa pustaka eksternal'], {}),
    990: (['Ditetapkan: kata kunci dicocokkan dengan ID transaksi, nama pihak, dan keterangan (tanpa membedakan huruf besar/kecil)',
           'paginasi berbasis nomor halaman (page, size bawaan 20)'], {' / ': '; '}),
    992: (['Ditetapkan: pemasukan = transaksi masuk berstatus Berhasil; pengeluaran = transaksi keluar berstatus Berhasil termasuk biaya; pemindahan antarakun milik sendiri tidak dihitung'], {}),
    999: (['ditetapkan pada D04B'], {' pada Database Design.': ': memakai ledger_transactions dan journal_lines tanpa tabel tambahan.'}),
    1006: (['ditetapkan'], {' pada Architecture Design.': ': transaksi kartu muncul dengan jenis Kartu Virtual.'}),
    1026: (['Salomo'], {}),
    1085: (['Ditetapkan: tanpa FCM; aplikasi mengambil antrean push dari server setiap 15 detik (polling) lalu menampilkannya sebagai notifikasi lokal Android'], {}),
    1090: (['Ditetapkan: tidak ada push di Web; Web menampilkan kotak notifikasi dan jumlah belum dibaca'], {}),
    1092: (['Ditetapkan: berupa URL (http/https); unggah berkas belum tersedia'], {}),
    1094: (['Ditetapkan: event Spring diproses secara @Async; push lewat antrean yang diambil klien, dalam aplikasi lewat tabel notifications, email disimulasikan ke email_outbox'], {}),
    1100: (['ditetapkan pada D04B'], {' pada Database Design.': ': tabel notification_settings, notifications, dan email_outbox.'}),
    1126: (['Salomo'], {}),
    1195: (['Salomo'], {}), 1201: (['Salomo'], {}), 1207: (['Salomo'], {}), 1213: (['Salomo'], {}),
    1219: (['diputuskan Salomo mengambil seluruh slot Backend (realisasi 10–11 Nov 2026, lihat D01 bagian 1.7)'], {}),
    1266: (['ditetapkan'], {' pada Architecture Design.': ': kartu memakai rekening utama dan transaksinya tercatat di riwayat.'}),
    1269: (['ditetapkan'], {' pada Architecture Design.': ': transaksi kartu ikut tercatat di riwayat.'}),
    1286: (['sumber dana dari rekening utama'], {}),
    1290: (['Ditetapkan: realisasi ada pada D01 Bab II (Gantt chart); Phase 1–4 berlangsung 3 Okt–11 Nov 2026, Phase 5 (pengujian dan stabilisasi) 11–14 Nov 2026'], {}),
    1305: (['setelah Authentication; sumber dana rekening utama'], {}),
    1324: (['Ditetapkan: OpenAPI otomatis (/v3/api-docs dan Swagger UI), 73 operasi'], {}),
    1327: (['Ditetapkan: JSON tanpa envelope; nama field camelCase'], {' (struktur envelope, field wajib).': '.'}),
    1330: (['Ditetapkan: 200/201 sukses; 400 validasi input; 401 belum login atau token tidak berlaku; 403 tidak berhak; 404 tidak ditemukan; 409 konflik; 422 aturan bisnis ditolak; 423 terkunci'], {}),
    1333: (['Ditetapkan: {code, message, fields}; code stabil untuk logika klien, message siap ditampilkan'], {}),
    1336: (['Ditetapkan: JWT HS256 berumur 15 menit + refresh token 7 hari yang dirotasi; setiap token divalidasi terhadap sesi di basis data'], {}),
    1339: (['Ditetapkan: endpoint huruf kecil di bawah /api/v1; field JSON camelCase; kode error HURUF_BESAR (mis. INSUFFICIENT_BALANCE)'], {' (field, endpoint, kode).': '.'}),
    1342: (['Ditetapkan: nomor referensi DDB + tanggal (yyMMdd) + 8 karakter acak'], {}),
    1345: (['Ditetapkan: ISO-8601 UTC di API; ditampilkan klien dalam WIB (Asia/Jakarta)'], {' (termasuk zona waktu).': '.'}),
    1348: (['Ditetapkan: bilangan bulat rupiah (long), tanpa desimal'], {}),
    1351: (['Ditetapkan: parameter query page, size (bawaan 20), range/from/to, category, direction, status, q'], {}),
    1354: (['Ditetapkan: header HTTP Idempotency-Key berisi UUID dari klien'], {}),
    1357: (['Ditetapkan: versi pada path (/api/v1)'], {}),
}

# Paragraf satu run 'Endpoint detail: Perlu Ditentukan pada API Design.'
EPS = {367: '18 operasi di /api/v1/auth/** (D03 Tabel 4.2, openapi.json).',
       476: '11 operasi di /api/v1/accounts/** (D03 Tabel 4.2).',
       602: '8 operasi di /api/v1/transfers/** (D03 Tabel 4.2).',
       703: '5 operasi di /api/v1/qris/** (D03 Tabel 4.2).',
       799: '6 operasi di /api/v1/topup/** (D03 Tabel 4.2).',
       895: '7 operasi di /api/v1/cards/** (D03 Tabel 4.2).',
       1002: '4 operasi di /api/v1/history/** (D03 Tabel 4.2).',
       1103: '14 operasi di /api/v1/me/** (D03 Tabel 4.2).'}

# Butir keputusan (bagian 5 'Keputusan yang Belum Tersedia di SRS' dan bagian 12), dicocokkan dengan teks butirnya
B = {
    'Penyedia/kanal pengiriman OTP.': 'Ditetapkan: simulator di backend; kode OTP dikembalikan pada respons (demoCode) dan email ditulis ke email_outbox; SMS/email sungguhan belum dipakai.',
    'Masa berlaku OTP, jumlah maksimum kirim ulang, dan durasi blokir PIN sementara.': 'Ditetapkan: OTP berlaku 2 menit dan maksimal 5 kali salah; kirim ulang membatalkan OTP lama; PIN diblokir 5 menit setelah 3 kali salah.',
    'Cara pengambilan swafoto di web dan lokasi penyimpanan berkas e-KYC.': 'Ditetapkan: Web memakai unggah berkas gambar, Android memakai swafoto simulasi; berkas tidak disimpan di server, hanya referensinya.',
    'Aturan password (panjang/kompleksitas).': 'Ditetapkan: minimal 8 karakter, mengandung huruf dan angka.',
    'Perilaku penghapusan kantong yang masih berisi dana.': 'Ditetapkan: kantong tidak terkunci dapat dihapus dan sisa saldonya dipindah ke rekening utama; kantong terkunci tidak dapat dihapus.',
    "Definisi 'target' pada locking (nominal atau lainnya) dan kondisi tercapainya.": 'Ditetapkan: target berupa nominal (minimal Rp10.000); kunci terbuka saat saldo kantong mencapai target atau tanggal kunci terlewati.',
    'Apakah pemindahan antar-kantong dicatat sebagai entri ledger.': 'Ditetapkan: ya; kantong adalah akun ledger sehingga pemindahan tercatat sebagai jurnal dan muncul di riwayat.',
    'Cakupan grafik saldo (total rekening atau per kantong).': 'Ditetapkan: saldo rekening utama, dihitung dari jurnal ledger.',
    'Lokasi penyimpanan preferensi sembunyikan saldo.': 'Ditetapkan: di server, sehingga konsisten antarperangkat.',
    'Batas limit harian dan limit per transaksi (BR-07, keputusan terbuka No. 4 pada Project Brief).': 'Ditetapkan: maksimum Rp25.000.000 per transaksi (nilai usulan); limit harian belum diterapkan dan menunggu keputusan dosen.',
    'Konfirmasi nominal minimum Rp5.000 untuk sesama dan antarbank (BR-01).': 'Ditetapkan: minimum Rp5.000 berlaku untuk transfer sesama bank dan antarbank.',
    "Struktur biaya transfer (UC-01 menyebut 'biaya bila ada').": 'Ditetapkan: sesama bank Rp0, antarbank Rp2.500 (nilai usulan).',
    'Daftar bank simulasi dan perilaku simulator (sukses/gagal/timeout).': 'Ditetapkan: antara lain BCA, BRI, BNI, Bank Mandiri, BSI, CIMB Niaga, Bank Permata; BRI 9999999999 ditolak, Mandiri 8888888888 timeout, rekening lain sukses.',
    'Lokasi pembuatan bukti transfer (gambar/PDF).': 'Ditetapkan: di klien (gambar PNG atau PDF), dibagikan melalui share sheet.',
    'Penanganan QRIS statis tanpa nominal (nominal diisi pembayar atau tidak didukung).': 'Ditetapkan: didukung; nominal diisi pembayar. Pada QRIS dinamis nominal terkunci.',
    'Format dan perilaku simulator QRIS serta pembeda bank sama/berbeda.': 'Ditetapkan: QrisCodec (format EMVCo + CRC16); kode berpenerbit Diponegoro Digital Bank diteruskan ke rekening tujuan, kode bank lain dicatat ke akun merchant eksternal; dana masuk disimulasikan lewat POST /qris/simulate-incoming.',
    'Biaya atau limit pembayaran QRIS (BR-07 belum ditetapkan).': 'Ditetapkan: tanpa biaya; nominal Rp1.000–Rp10.000.000 per transaksi (nilai usulan).',
    'Daftar operator, produk, dan nominal yang didukung.': 'Ditetapkan: Telkomsel, Indosat Ooredoo, XL, AXIS, Tri, Smartfren (dideteksi dari prefiks nomor); pulsa Rp5.000–Rp200.000; token listrik Rp20.000–Rp1.000.000; paket data per operator.',
    'Perilaku simulator penyedia (sukses/gagal/tertunda).': "Ditetapkan: data valid selalu sukses; ID pelanggan atau nomor tagihan berakhiran 0000 menghasilkan 'tidak ditemukan'; status tertunda tidak disimulasikan.",
    'Biaya admin, bila ada.': 'Ditetapkan: pulsa Rp1.500, token listrik Rp2.500, tagihan Rp2.500, e-wallet Rp1.000 (nilai usulan).',
    'Cakupan jenis tagihan FR-20 yang dikerjakan pada rilis pertama.': 'Ditetapkan: PDAM, BPJS Kesehatan, Internet Rumah, dan top-up e-wallet (GoPay, OVO, DANA, ShopeePay).',
    'Sumber dana kartu dan mekanisme transaksi kartu simulasi.': 'Ditetapkan: rekening utama; transaksi merchant disimulasikan lewat POST /cards/{id}/simulate-purchase dan diposting ke ledger.',
    'Jumlah maksimum kartu per pengguna.': 'Ditetapkan: maksimal 5 kartu.',
    'Apakah transaksi kartu muncul pada Transaction History.': 'Ditetapkan: ya, dengan jenis Kartu Virtual.',
    'Nilai default dan batas maksimum limit kartu.': 'Ditetapkan: bawaan Rp5.000.000 per transaksi, rentang Rp10.000–Rp50.000.000 (nilai usulan).',
    'Bidang yang dapat dicari pada pencarian riwayat.': 'Ditetapkan: ID transaksi, nama pihak, dan keterangan.',
    'Definisi pemasukan dan pengeluaran pada dashboard (termasuk pemindahan antar-kantong).': 'Ditetapkan: pemasukan = transaksi masuk berhasil; pengeluaran = transaksi keluar berhasil termasuk biaya; pemindahan antar-kantong milik sendiri tidak dihitung.',
    'Format tampilan berkas ekspor PDF/CSV.': 'Ditetapkan: CSV (ID, waktu WIB, jenis, keterangan, dari, ke, arah, nominal, biaya, total, status, catatan) dan PDF tabel ringkas; keduanya mengikuti filter aktif.',
    'Ukuran halaman default paginasi.': 'Ditetapkan: 20 transaksi per halaman.',
    'Penyedia push dan email.': 'Ditetapkan: push lewat polling + notifikasi lokal Android (tanpa FCM); email disimulasikan ke email_outbox (tanpa SMTP).',
    'Lokasi penyimpanan foto profil.': 'Ditetapkan: URL foto disimpan di data profil; unggah berkas belum tersedia.',
    'Jenis notifikasi yang dapat dipilih pengguna.': 'Ditetapkan: Transaksi masuk, Transaksi keluar, Keamanan akun, Info & promo; kanal Push, Dalam aplikasi, Email (bawaan: Push dan Dalam aplikasi).',
    'Dukungan push pada web.': 'Ditetapkan: tidak didukung; Web memakai kotak notifikasi dalam aplikasi.',
    # 12.1 Umum
    'Nama anggota Backend dan pengisian slot BE-1 s.d. BE-4.': 'Ditetapkan: seluruh slot dipegang Salomo.',
    'Teknologi Web (framework) dan produk basis data relasional (SRS 2.5, 2.6).': 'Ditetapkan: React 18 + Vite 5; PostgreSQL 17/18 dengan Flyway (H2 untuk pengujian).',
    'Seluruh butir Integration Contract (bagian 9).': 'Ditetapkan: lihat kolom Status keputusan pada bagian 9.',
    'Batas limit transaksi harian (BR-07; keputusan terbuka No. 4 pada Project Brief).': 'Ditetapkan: maksimum Rp25.000.000 per transaksi (nilai usulan); limit harian belum diterapkan.',
    'Konfirmasi nominal minimum Rp5.000 untuk sesama dan antarbank (BR-01; SRS 2.6).': 'Ditetapkan: Rp5.000 untuk sesama bank dan antarbank.',
    'Hubungan Virtual Card dengan Account dan Transaction History (Architecture Design).': 'Ditetapkan: kartu memakai rekening utama dan transaksinya tercatat di riwayat.',
    'Jadwal pengerjaan per phase.': 'Ditetapkan: realisasi pada D01 Bab II (3 Okt–14 Nov 2026).',
}

# Kalimat realisasi yang ditambahkan di akhir paragraf tertentu (dicocokkan dengan awal teksnya)
APPEND = {
    'Catatan penggunaan:': ' Versi 0.2 (17 Nov 2026): seluruh butir bertanda tersebut sudah diisi dengan keputusan yang diterapkan pada implementasi (teks hijau "Ditetapkan"/"Realisasi"), bersumber dari kode dan dokumen D02 v0.9.5, D03 v0.9.3, D04B v0.9.2.',
    'Slot Backend dipakai agar tanggung jawab tetap jelas': ' Realisasi: seluruh slot BE-1 s.d. BE-4 beserta Database dikerjakan Salomo.',
    'Salomo berperan sebagai Mobile Team Lead': ' Realisasi: modul Android Salomo menjadi Authentication dan Virtual Card (submit_mobile/README.md), dan Salomo juga mengimplementasikan seluruh Backend, Database, serta aplikasi Web (D01 bagian 1.7).',
    'Implementasi backend dipegang slot BE-1': ' Realisasi: seluruh backend diimplementasikan Salomo.',
    'Usulan awal untuk dibahas tim.': ' Realisasi pembagian Android (submit_mobile/README.md): Salomo – Authentication dan Virtual Card; Cecilia – Transfer dan Transaction History & Dashboard; Cia (Gracia) – Payment (QRIS) dan Profile & Notification; Grace – Account dan Top-up. Aplikasi Web akhirnya diimplementasikan Salomo; anggota tim Web berperan pada review, UI Map, pengujian, dan dokumentasi.',
    'Seluruh platform (Android, Web, Backend) wajib menyepakati': ' Versi 0.2: seluruh butir telah ditetapkan seperti pada kolom Status keputusan.',
    'Rangkuman informasi yang belum tersedia di SRS.': ' Versi 0.2: setiap butir dilengkapi keputusan yang diterapkan pada implementasi.',
}

done, out, last = [], [], 0
for i, m in enumerate(PARA.finditer(x)):
    p = m.group(0); new = p
    t = ptext(p).strip()
    runs = RUN.findall(p)
    if i in D:
        texts, fixes = D[i]; k = 0
        for r in runs:
            rt = rtext(r)
            if rt in MARK and k < len(texts):
                new = new.replace(r, green(r, texts[k]), 1); k += 1
            elif rt in fixes:
                new = new.replace(r, set_rtext(r, fixes[rt]), 1)
        assert k == len(texts), (i, k, t[:60])
        done.append(i)
    elif i in EPS:
        r = runs[0]
        new = new.replace(r, set_rtext(r, EP) + green(r, 'Ditetapkan: ' + EPS[i]), 1); done.append(i)
    else:
        key = None
        for kk in B:
            if t == kk or t == kk + ' Perlu Ditentukan.':
                key = kk; break
        if key:
            mk = [r for r in runs if rtext(r).strip() in ('Perlu Ditentukan.', 'Perlu Ditentukan')]
            if mk:
                new = new.replace(mk[0], green(mk[0], B[key]), 1)
            else:
                lr = runs[-1]
                new = new.replace(lr, lr + green(lr, ' ' + B[key]), 1)
            done.append(i)
        for pre, add in APPEND.items():
            if t.startswith(pre):
                lr = RUN.findall(new)[-1]
                new = new.replace(lr, lr + green(lr, add), 1); done.append(('append', pre[:25]))
    out.append(x[last:m.start()]); out.append(new); last = m.end()
out.append(x[last:]); x = ''.join(out)

# ---------- nama anggota, judul bagian 12, metadata ----------
def simple(a, b, cnt=None):
    global x
    n = x.count(a)
    assert n and (cnt is None or n == cnt), (n, a)
    x = x.replace(a, b); done.append(f'{a[:30]} x{n}')
simple('Cia, Cecilia, Grace', 'Cia (Gracia Riyanto), Cecilia, Grace', 1)
simple('Stepen', 'Steven'); simple('Raffi', 'Rafi'); simple('Surya', 'Permata')
simple('>12. Daftar Keputusan Terbuka<', '>12. Daftar Keputusan Terbuka dan Penetapannya<')
simple('0.1 / Draft untuk Review tim', '0.2 / Diperbarui dengan keputusan implementasi', 1)
simple('>4 Oktober 2026<', '>4 Oktober 2026 (v0.1); 17 November 2026 (v0.2)<', 1)

# baris revisi 0.2: gandakan baris 0.1 pada tabel Kontrol Dokumen
rows = list(re.finditer(r'<w:tr[ >].*?</w:tr>', x, re.S))
row01 = [r for r in rows if rtext(r.group(0)).startswith('0.14 Okt 2026')][0]
r2 = row01.group(0)
cells = re.findall(r'<w:tc>.*?</w:tc>', r2, re.S)
vals = ['0.2', '17 Nov 2026', 'Salomo P.G. Hokoyoku',
        'Seluruh butir Perlu Ditentukan dan Technical Decision Required diisi dengan keputusan yang diterapkan pada implementasi (teks hijau); slot Backend BE-1 s.d. BE-4 diisi Salomo; catatan realisasi pembagian Android dan Web; penulisan nama anggota diperbaiki (Steven, Rafi, Permata).']
for c, v in zip(cells, vals):
    rr = RUN.findall(c)
    nc = c.replace(rr[0], set_rtext(rr[0], v), 1)
    for extra in rr[1:]: nc = nc.replace(extra, '', 1)
    r2 = r2.replace(c, nc, 1)
x = x[:row01.end()] + r2 + x[row01.end():]
done.append('revisi 0.2')

open(P, 'w', encoding='utf-8', newline='').write(x)
for f in glob.glob(os.path.join(BASE, 'word', 'footer*.xml')):
    s = open(f, encoding='utf-8').read()
    if 'v0.1' in s: open(f, 'w', encoding='utf-8', newline='').write(s.replace('v0.1', 'v0.2'))

left = [i for i, m in enumerate(PARA.finditer(x)) if any(k in ptext(m.group(0)) for k in MARK)]
print(len(done), 'perubahan')
print('paragraf yang masih memuat kata penanda:', left)
