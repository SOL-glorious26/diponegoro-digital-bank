"""D02, D03, D04B, D07A, D07B -> v1.0 final (22 Nov 2026). Pemakaian: python patch_v100.py <folder gen>"""
import sys, os
os.chdir(sys.argv[1])
FINAL = 'Final v1.0 – diterbitkan Project Manager 22 November 2026; '


class Doc:
    def __init__(self, f):
        self.f = f
        raw = open(f, encoding='utf-8', newline='').read()
        self.crlf = '\r\n' in raw
        self.s = raw.replace('\r\n', '\n')

    def rep(self, a, b, n=1):
        assert self.s.count(a) == n, (self.f, self.s.count(a), a[:90])
        self.s = self.s.replace(a, b)

    def line(self, start, new, n=1):
        """Ganti satu baris utuh yang (setelah spasi) diawali teks tertentu."""
        L = self.s.split('\n')
        idx = [i for i, l in enumerate(L) if l.strip().startswith(start)]
        assert len(idx) == n, (self.f, len(idx), start)
        for i in idx:
            L[i] = new
        self.s = '\n'.join(L)

    def meta(self, old, pic, status, refs, rev, arg='LOGO'):
        self.rep('version: "%s", statusShort: "Draf Lengkap untuk Review",' % old, 'version: "1.0", statusShort: "Final",')
        self.line('status: "Draf lengkap', '  status: "%s",' % (FINAL + status))
        self.line('refs: "', '  refs: "%s",' % refs)
        i = self.s.index('logo: LOGO,'); j = self.s.rindex('  date: "', 0, i)
        self.s = self.s[:j] + '  date: "22 November 2026", ' + self.s[i:]
        i = self.s.index('\n  ],\n};')
        self.s = self.s[:i] + '\n    ["1.0", "22 Nov 2026", "Salomo P.G. Hokoyoku", "%s Disetujui PM."],\n  ],\n  approval: { sign: "../../../documents_area/_aset/project_manager_sign.png", date: "22 Nov 2026", pic: %s },\n};' % (rev, pic) + self.s[i + len('\n  ],\n};'):]

    def save(self):
        open(self.f, 'w', encoding='utf-8', newline='').write(self.s.replace('\n', '\r\n') if self.crlf else self.s)
        print('ok', self.f)


PEER = '[["Peer reviewer", "Steven Wibisana"]]'
NOPEER = 'peer review (Steven Wibisana) belum diterima sampai tenggat'

# ------------------------------------------------------------------ D02
d = Doc('doc2.js')
d.meta('0.9.5', PEER, NOPEER,
       'SRS-DDB-001 v0.1 (2.1, 2.4, 2.5, Bagian 4); Project Brief v1.0 §9; MDTA-DDB-001 v0.2; STD-DDB-001 v1.0; README aplikasi Web tim (digitalbanking-node v2.0.0); repositori kode per 22 November 2026',
       'Versi final. Web resmi ditetapkan Web tim (Node.js, Express, EJS): 8.2, ADR-13, dan ADR-14 ditambahkan; arsitektur Web React dipertahankan sebagai prototipe pembanding (8.3–8.6); peran tim dan hasil uji disamakan dengan STD-DDB-001 v1.0; navigasi Android lima tab.')
d.rep('sesuai implementasi aktual per 16 November 2026', 'sesuai implementasi aktual per 22 November 2026')
d.line('["Aplikasi Web", "Antarmuka nasabah di peramban',
       '    ["Aplikasi Web (tim)", "Antarmuka nasabah di peramban, termasuk ekspor riwayat (FR-26)", "Node.js 18+, Express 5, EJS, express-session; memanggil backend dari server", "Terhubung ke backend; 14 dari 14 alur inti lulus; sebagian FR belum tersedia (8.2)"],')
d.rep('  H2("8.2 Kondisi Kode Web"),', r'''  H2("8.2 Web Resmi: Aplikasi Web Tim (Node/Express/EJS)"),
  P("Pada 22 November 2026 Project Manager menetapkan aplikasi yang dibuat tim Web (digitalbanking-node v2.0.0) sebagai Web resmi. Penanggung jawab: Permata Surya Cahyana (backend Web), Rafi Fadhillah (front end dan backend Web), Steven Wibisana dan Nabila Puspa Annastasya (front end). Aplikasi ini memakai pola **backend for frontend (BFF)**: peramban menerima HTML dari EJS dan mengirim formulir ke server Node, lalu server Node memanggil REST /api/v1 pada backend Spring Boot yang sama dengan Android. Web tidak memiliki basis data sendiri."),
  table(["Butir", "Rancangan", "Alasan / dampak"], [
    ["Tampilan", "Express 5 + EJS (server-side rendering)", "Tanpa build front end"],
    ["Sesi dan token", "JWT disimpan di sesi server Node (memori); peramban hanya memegang cookie sesi httpOnly, SameSite=Lax", "Token tidak terbaca JavaScript; sesi hilang bila server dimatikan"],
    ["Refresh token", "Satu kali refresh saat 401, lalu permintaan diulang; gagal → kembali ke login", "FR-05"],
    ["Keamanan formulir", "Token CSRF per sesi; regenerasi sesi setelah login", "Mencegah CSRF dan session fixation"],
    ["Transaksi", "PIN 6 digit dan satu Idempotency-Key per layar konfirmasi", "BR-02, BR-04"],
    ["CORS", "Tidak diperlukan karena pemanggilan server ke server", "Konfigurasi CORS backend tetap untuk klien peramban lain"],
    ["Top up saldo", "Memakai simulator POST /qris/simulate-incoming", "Tercatat sebagai Terima dana QRIS; backend belum memiliki endpoint isi saldo"],
  ], [1700, 4370, 3000], { boldFirst: true, size: 17 }),
  CAP("Tabel 8.0 Rancangan aplikasi Web tim"),
  P("Alur inti Web tim lulus 14 dari 14 kasus terhadap backend asli, dan 3 dari 3 skenario lintas platform dengan Android lulus (STD-DDB-001 v1.0). Menurut README, kantong, kartu virtual, notifikasi, grafik saldo, penerima favorit, QRIS pribadi/dinamis, dan paket data belum tersedia. Patch saldo awal (opening-balance.patch) bersifat opsional dan tidak diterapkan pada backend bersama."),
  H2("8.3 Prototipe Web React"),
  P("Bagian 8.3 sampai 8.6 mendeskripsikan aplikasi Web React yang dibuat Project Manager pada 11 November 2026 dan kini disimpan di apps/web_dummy_buatan_sendiri sebagai pembanding. Penyebutan folder apps/web pada bagian tersebut merujuk pada lokasi saat itu."),''')
d.rep('mengikuti keputusan pada 8.3,', 'mengikuti keputusan pada 8.4,')
d.rep('H2("8.3 Keputusan Arsitektur Web")', 'H2("8.4 Keputusan Arsitektur Web React")')
d.rep('H2("8.4 Ruang Lingkup Web v1")', 'H2("8.5 Ruang Lingkup Web React v1")')
d.rep('H2("8.5 Struktur Kode Aplikasi Web")', 'H2("8.6 Struktur Kode Aplikasi Web React")')
d.rep('FR yang tersedia di Web: FR-01', 'FR yang tersedia di Web React: FR-01')
d.rep('pada apps/web/README.md dan', 'pada README.md aplikasi tersebut dan')
i = d.s.index('    ["ADR-12", "Token Web'); j = d.s.index('\n', i)
d.s = d.s[:j] + '\n    ["ADR-13", "Web resmi = aplikasi Web tim Node/Express/EJS (menggantikan ADR-11 sebagai baseline, 22 Nov 2026)", "Dikembangkan dan dikuasai tim Web; memakai backend bersama", "Sebagian FR belum ada di Web; sebagian dokumen Web perlu diselaraskan"],\n    ["ADR-14", "Token Web tim di sesi server (BFF)", "Token tidak sampai ke peramban", "Sesi di memori: hilang saat restart dan tidak dapat diskalakan tanpa penyimpanan sesi"],' + d.s[j:]
d.rep('"Melanjutkan prototipe; FR-02, FR-03", "Logika bisnis tetap di backend; Web hanya klien tipis"]', '"Melanjutkan prototipe; FR-02, FR-03", "Logika bisnis tetap di backend; kini prototipe pembanding (lihat ADR-13)"]')
d.rep('Terukur, lulus (NF-01): Web maks. 956 ms;', 'Terukur, lulus (NF-01): Web React maks. 956 ms;')
d.rep('uji lintas platform LP-01 sampai LP-04 lulus"],', 'uji lintas platform LPN-01 sampai LPN-03 (Web tim) dan LP-01 sampai LP-04 (Web React) lulus"],')
d.rep('Web di Chrome, Edge, Firefox; iOS', 'Web React di Chrome, Edge, Firefox (Web tim belum diuji antar-peramban); iOS')
d.rep('  ], [450, 2700, 2100, 2620, 1200], { center: [0], size: 17 }),\n  CAP("Tabel 11.1', '    ["6", "Web tim belum mencakup sebagian FR dan menyimpan sesi di memori", "Cakupan Web lebih kecil dari Android", "Lengkapi fitur menurut prioritas MoSCoW; penyimpanan sesi terpisah untuk produksi", "Tim Web"],\n  ], [450, 2700, 2100, 2620, 1200], { center: [0], size: 17 }),\n  CAP("Tabel 11.1')
d.rep('Rinciannya tercatat pada STD-DDB-001 v0.9.6."),', 'Rinciannya tercatat pada STD-DDB-001."),')
d.save()

# ------------------------------------------------------------------ D03
d = Doc('doc3.js')
d.meta('0.9.3', PEER, NOPEER,
       'SRS-DDB-001 v0.1 (Bagian 3); SAD-DDB-001 v1.0; STD-DDB-001 v1.0; UIM-DDB-001-AND v1.0; User Interface Map (Web) v0.7; backend/docs/openapi.json (73 operasi); kode backend, Android, dan Web per 22 November 2026',
       'Versi final. Navigasi Android lima tab; kolom halaman Web pada Tabel 4.1 dinyatakan sebagai Web React dan Tabel 4.1a (halaman Web tim) ditambahkan; status FR per platform memisahkan Web tim dan Web React; hasil uji disamakan dengan STD-DDB-001 v1.0.')
d.rep('hasil uji sistem STD-DDB-001 v0.9.6 per 16 November 2026."),', 'hasil uji sistem STD-DDB-001 v1.0 per 22 November 2026."),')
d.rep('bottom bar Beranda / Riwayat / QRIS / Profil.', 'bottom bar Beranda / Riwayat / QRIS / Kantong / Profil.')
d.rep('Antarmuka Web dibangun dengan React + Vite + react-router sesuai SAD-DDB-001 Bab VIII;', 'Web resmi adalah aplikasi Web tim (Node/Express/EJS, Tabel 4.1a). Kolom Web pada Tabel 4.1 adalah halaman prototipe Web React (SAD-DDB-001 8.3);')
d.rep('table(["Modul", "Layar Android (berkas)", "Halaman Web (rute: berkas)"], [', 'table(["Modul", "Layar Android (berkas)", "Halaman Web React (rute: berkas)"], [')
i = d.s.index('CAP("Tabel 4.1 '); j = d.s.index('\n', i)
d.s = d.s[:j] + r'''
  table(["Modul", "Halaman Web tim (rute)", "Cakupan"], [
    ["Authentication", "/login, /register, /register/otp, /ekyc, /pin, /lupa-sandi, /logout", "FR-01 – FR-05"],
    ["Account", "/dashboard", "FR-06; grafik saldo dan kantong belum tersedia"],
    ["Transfer", "/transfer", "FR-10 – FR-12; penerima favorit belum tersedia; bukti lewat detail riwayat"],
    ["Payment (QRIS)", "/pembayaran?tab=qris", "FR-15 berupa contoh/payload tanpa kamera; QRIS pribadi/dinamis belum tersedia"],
    ["Top-up", "/pembayaran?tab=pulsa, pln, tagihan, ewallet, topup", "FR-18 (tanpa paket data) – FR-20"],
    ["Virtual Card", "–", "Belum tersedia"],
    ["Transaction History & Dashboard", "/riwayat, /riwayat/export", "FR-23, FR-24, FR-26; ringkasan bulanan belum tersedia"],
    ["Profile & Notification", "/profil, /profil/otp", "FR-27 dan ubah kata sandi; notifikasi belum tersedia"],
  ], [2100, 3570, 3400], { boldFirst: true, size: 17 }),
  CAP("Tabel 4.1a Halaman Web tim (Node/Express/EJS) menurut User Interface Map (Web) v0.7 dan kode"),''' + d.s[j:]
d.rep('hasil uji sistem STD-DDB-001 v0.9.6 per 16 November 2026. Aplikasi Android', 'hasil uji sistem STD-DDB-001 v1.0 per 22 November 2026. Aplikasi Android')
d.rep('dan peramban Chrome, Edge, serta Firefox."),\n  table(["FR", "Backend", "Android", "Web"], [', 'dan peramban Chrome, Edge, serta Firefox (Web React). Web tim diuji pada alur inti melalui HTTP."),\n  table(["FR", "Backend", "Android", "Web React"], [')
d.line("NOTE(\"'Ada' berarti fungsi", r'''  NOTE("'Ada' berarti fungsi terimplementasi dan lulus uji pada STD-DDB-001 v1.0: Android SA 28/28 (27 pada build 22 November 2026; SA-08 di ponsel fisik 14 November 2026), Web React WB 15/15 dan LP 4/4, backend 47/47 test otomatis. Seluruh 14 defect yang ditemukan sudah ditutup."),
  table(["FR", "Web tim (Web resmi)"], [["FR-01 – FR-06, FR-10 – FR-12, FR-19, FR-20, FR-23, FR-24, FR-26, FR-27", "Ada; alur inti lulus WN-01 – WN-14 dan LPN-01 – LPN-03 (detail transaksi, ubah profil, dan pemulihan akun tidak diuji terpisah)"], ["FR-14, FR-15, FR-18, FR-28", "Sebagian: bukti tanpa unduhan; QRIS tanpa kamera; tanpa paket data; hanya ubah kata sandi"], ["FR-07 – FR-09, FR-13, FR-16, FR-17, FR-21, FR-22, FR-25, FR-29", "Belum tersedia"]], [4000, 5070], { boldFirst: true, size: 18 }),
  CAP("Tabel 7.1a Status FR pada Web tim"),''')
d.rep('    "Foto profil hanya mendukung URL; unggah berkas belum tersedia.",\n', '    "Foto profil hanya mendukung URL; unggah berkas belum tersedia.",\n    "Web tim belum mencakup seluruh FR (Tabel 7.1a), termasuk paket data pada FR-18 (Must).",\n')
d.rep('(SAD-DDB-001) v0.9.5.",', '(SAD-DDB-001) v1.0.",')
d.rep('(PRJ-DDB-001) v0.9.4.",', '(PRJ-DDB-001) v1.0.",')
d.rep('(STD-DDB-001) v0.9.6.",', '(STD-DDB-001) v1.0.",')
d.rep('"Kelompok Jaran Goyang. (2026). User Interface Map (Web) v0.3.",', '"Kelompok Jaran Goyang. (2026). User Interface Map (Web) v0.3 dan v0.7; User Interface Map Android (UIM-DDB-001-AND) v1.0.",')
d.save()

# ------------------------------------------------------------------ D04B
d = Doc('doc4b.js')
d.meta('0.9.2', PEER, NOPEER,
       'SAD-DDB-001 v1.0; FSD-DDB-001 v1.0; STD-DDB-001 v1.0; SRS-DDB-001 v0.1; kode ddb-backend dan V1__init.sql per 22 November 2026',
       'Versi final. Isi teknis tidak berubah; cakupan JaCoCo disamakan dengan STD-DDB-001 (92,2% baris); keterbatasan PostgreSQL diperbarui; backend bersama dipakai Android dan Web tim tanpa perubahan kode.')
d.rep('walaupun cakupan baris 92,3%"', 'walaupun cakupan baris 92,2%"')
d.rep('["3", "Belum diuji pada PostgreSQL (hanya H2)", "Jalankan profil dev pada PostgreSQL lokal sebelum build baseline"],',
      '["3", "Test otomatis dan uji sistem memakai H2; profil dev berjalan di PostgreSQL 18 lokal sejak 13 November 2026", "Jalankan test integrasi pada PostgreSQL (misalnya Testcontainers)"],')
d.rep('(SAD-DDB-001) v0.9.4.",', '(SAD-DDB-001) v1.0.",')
d.rep('(FSD-DDB-001) v0.9.2.",', '(FSD-DDB-001) v1.0.",')
d.rep('(STD-DDB-001) v0.9.5.",', '(STD-DDB-001) v1.0.",')
d.save()

# ------------------------------------------------------------------ D07A
d = Doc('doc7a.js')
d.meta('0.9.3', '[["Pendukung (QRIS, Profile)", "Gracia Riyanto"]]', 'telaah bagian QRIS dan Profile serta uji keterikutan setup oleh anggota tim belum diterima sampai tenggat',
       'SAD-DDB-001 v1.0; FSD-DDB-001 v1.0; SDD-DDB-001-AND v1.0; UIM-DDB-001-AND v1.0; DVM-DDB-001-BE v1.0; STD-DDB-001 v1.0; apps/android/README.md; kode aplikasi per 22 November 2026',
       'Versi final. Layar PIN responsif (DEF-13), warna status per mode (DdbTheme.colors.success/warning, DEF-14), layar Kartu memakai ScreenScaffold, detail kartu virtual varian online (DEF-12), skrip tangkap-layar.mjs; hasil uji dan acuan disamakan dengan STD-DDB-001 v1.0.')
d.rep('uji di ponsel fisik, termasuk pindai QRIS dengan kamera, belum dilakukan."),', 'pindai QRIS dengan kamera, notifikasi, dan berbagi bukti lulus di ponsel fisik pada 14 November 2026 (build sebelum pembaruan tampilan)."),')
d.rep('  B("Instrumented test (androidTest)', '  B("Test otomatis varian demo memakai repository tiruan sehingga tidak mendeteksi DEF-12; pemeriksaannya ada pada skrip SA-13."),\n  B("Instrumented test (androidTest)')
d.rep('(SAD-DDB-001) v0.9.1.",', '(SAD-DDB-001) v1.0.",')
d.rep('(DVM-DDB-001-BE) v0.9.1.",', '(DVM-DDB-001-BE) v1.0.",')
d.rep('(TUT-DDB-001-AND) v1.0.",', '(TUT-DDB-001-AND) v1.1.",')
d.save()

# ------------------------------------------------------------------ D07B
d = Doc('doc7b.js')
d.meta('0.9.2', PEER, 'uji keterikutan setup (Steven Wibisana) belum diterima sampai tenggat',
       'SDD-DDB-001-BE v1.0; SAD-DDB-001 v1.0; FSD-DDB-001 v1.0; STD-DDB-001 v1.0; backend/README.md; kode ddb-backend per 22 November 2026',
       'Versi final. Kode backend tidak berubah; keterbatasan PostgreSQL, catatan Web tim (BFF, tanpa CORS), dan acuan disamakan.')
d.rep('B("Docker Compose dan gateway belum diuji; profil dev belum dijalankan pada PostgreSQL."),', 'B("Docker Compose dan gateway belum diuji. Profil dev berjalan pada PostgreSQL 18 lokal sejak 13 November 2026; test otomatis memakai H2."),\n  B("Web tim (Node) memanggil backend dari server sehingga tidak memerlukan CORS. Patch saldo awal milik tim Web (DDB_OPENING_BALANCE) bersifat opsional dan belum diterapkan pada backend bersama."),')
d.rep('atau jalankan Web lewat proxy Vite"]', 'atau panggil backend dari server (seperti Web tim) atau lewat proxy"]')
d.rep('(SDD-DDB-001-BE) v0.9.",', '(SDD-DDB-001-BE) v1.0.",')
d.rep('(SAD-DDB-001) v0.9.",', '(SAD-DDB-001) v1.0.",')
d.save()
