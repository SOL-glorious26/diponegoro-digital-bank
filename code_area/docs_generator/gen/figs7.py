"""Diagram D04A (class ringkas + sequence) dan D05A (peta navigasi Android). Usage: python figs7.py <out_dir>"""
import os, sys
_src = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "figs6.py"), encoding="utf-8").read()
exec(compile(_src[:_src.index("def class_core")], "figs6.py", "exec"))  # helper gambar yang sama dengan D04W


def module(name, screens, vms, repo, impls):
    """Screen -> ViewModel -> Repository <|-- implementasi demo/online (arah panah = memakai)."""
    n, m = len(vms), len(impls)
    W = max(4.4 * n, 4.6 * m) + 0.4
    fig, ax = plt.subplots(figsize=(W * 0.82, 7.4)); ax.axis("off")
    top = 11.0
    vr, ylow = [], 99
    for i, (sc, (vn, ms)) in enumerate(zip(screens, vms)):
        x = W / n * (i + 0.5)
        s = cls(ax, x - 2.0, top, 4.0, sc, [], [], "Composable", LGREY)
        v = cls(ax, x - 2.0, top - 2.4, 4.0, vn, ["state: StateFlow"], ms, None)
        link(ax, s, v, "dep", "memakai"); vr.append(v); ylow = min(ylow, v[1])
    r = cls(ax, W / 2 - 2.5, ylow - 0.9, 5.0, repo[0], [], repo[1], "interface", LGOLD)
    for v in vr:
        link(ax, v, r, "dep")
    low = 0
    for i, (nm, note) in enumerate(impls):
        x = W / m * (i + 0.5)
        c = cls(ax, x - 2.1, r[1] - 1.0, 4.2, nm, [note], [], None, LGREEN)
        link(ax, c, r, "impl", "implementasi"); low = c[1]
    ax.set_xlim(0, W); ax.set_ylim(low - 0.2, top + 0.2)
    save(fig, name)


module("a1_auth.png", ["Login, Register, Otp, Ekyc, Pin", "RecoveryScreen"],
       [("AuthViewModel", ["login()", "register()", "verifyOtp()", "submitEkyc()", "createPin() / validatePin()", "logoutAllDevices()"]),
        ("RecoveryViewModel", ["requestRecovery()", "verifyOtp()", "resetCredential()"])],
       ("AuthRepository (lewat use case)", ["login() / loginWithBiometric()", "generateOtp() / verifyOtp()", "submitEkyc() / checkEkycStatus()", "createPin() / validatePin()", "getActiveSessions() / logout()"]),
       [("MockAuthRepository", "varian demo"), ("RemoteAuthRepository", "varian online")])
module("a2_account.png", ["BalanceChartScreen", "PocketsScreen"],
       [("BalanceChartViewModel", ["selectPeriod()"]), ("PocketsViewModel", ["create() / update()", "deposit() / withdraw()", "earlyWithdraw()", "lock() / delete()"])],
       ("AccountRepository", ["overview() / balanceSeries()", "createPocket() / updatePocket()", "setLock() / deletePocket()", "deposit() / withdraw()"]),
       [("LocalAccountRepository", "varian demo"), ("RemoteAccountRepository", "varian online")])
module("a3_transfer.png", ["TransferScreen"],
       [("TransferViewModel", ["inquire()", "review()", "requestPin() / submit()", "checkStatus()", "toggleFavorite()"])],
       ("TransferRepository", ["banks() / inquire()", "quote() / execute()", "checkStatus()", "favorites() / addFavorite()"]),
       [("LocalTransferRepository", "varian demo"), ("RemoteTransferRepository", "varian online")])
module("a4_topup.png", ["TopUpScreen"],
       [("TopUpViewModel", ["orderPulsa() / orderData()", "inquirePln() / orderPln()", "inquireBill() / orderBill()", "orderEwallet()", "requestPin() / pay()"])],
       ("TopUpRepository", ["orderPulsa() / orderData()", "inquirePln() / orderPln()", "inquireBill() / orderBill()", "orderEwallet() / execute()"]),
       [("LocalTopUpRepository", "varian demo"), ("RemoteTopUpRepository", "varian online")])
module("a5_card.png", ["VirtualCardScreen"],
       [("VirtualCardViewModel", ["createCard()", "requestDetails() / submitPin()", "freeze() / activate()", "saveLimit()", "simulate()"])],
       ("VirtualCardRepository (lewat use case)", ["getCards() / createCard()", "updateStatus() / updateLimit()", "authorizeTransaction()", "revealDetails()"]),
       [("MockVirtualCardRepository", "varian demo"), ("RemoteVirtualCardRepository", "varian online")])
module("a6_history.png", ["HomeScreen", "HistoryScreen", "TransactionDetailScreen", "AnalyticsScreen"],
       [("HomeViewModel", ["toggleBalance()"]), ("HistoryViewModel", ["updateFilter()", "loadMore()", "resetFilter()"]),
        ("TransactionDetailViewModel", ["checkStatus()"]), ("AnalyticsViewModel", ["select()", "previousMonth()", "nextMonth()"])],
       ("HistoryRepository", ["page() / recent()", "detail()", "monthSummary() / monthSeries()"]),
       [("LocalHistoryRepository", "varian demo"), ("RemoteHistoryRepository", "varian online")])


def parts(m):
    return ["Pengguna", m + "Screen", m + "ViewModel", "Remote" + m + "\nRepository", "ApiClient /\nDdbApi", "Backend"]


sequence("a7_seq_transfer.png", parts("Transfer"), [
    (2, 3, "banks()"), (3, 4, "banks()"), (4, 5, "GET /transfers/banks"),
    (0, 1, "isi rekening, Cek rekening"), (1, 2, "inquire()"), (2, 3, "inquire()"), (3, 4, "inquiry()"), (4, 5, "POST /transfers/inquiry"), (5, 2, "nama pemilik rekening", True),
    (0, 1, "isi nominal, Lanjut"), (1, 2, "review()"), (2, 3, "quote()"), (3, 4, "quote()"), (4, 5, "POST /transfers/quote"), (5, 2, "biaya, total, saldo setelah", True),
    (0, 1, "Transfer sekarang + PIN"), (1, 2, "requestPin(); submit(pin)"), (2, 3, "execute(quote, pin, kunci)"), (3, 4, "transfer(kunci, body)"),
    (4, 5, "POST /transfers + Idempotency-Key"), (5, 2, "SUCCESS / PROCESSING / FAILED (dana kembali)", True),
    (0, 1, "[alt PROCESSING] Periksa ulang"), (1, 2, "checkStatus()"), (2, 3, "checkStatus(txId)"), (3, 4, "checkStatus(txId)"), (4, 5, "POST /transfers/{txId}/check-status"), (5, 2, "status final", True)], w=15)
sequence("a8_seq_topup.png", parts("TopUp"), [
    (0, 1, "pilih layanan, isi data"), (1, 2, "orderX() / inquireX()"), (2, 3, "orderX() / inquireX()"), (3, 4, "inquiry sesuai layanan"),
    (4, 5, "[alt data] GET /topup/data-packages"), (4, 5, "[alt PLN] GET /topup/pln/inquiry"), (4, 5, "[alt tagihan] GET /topup/bills/inquiry"), (5, 2, "paket / nama pelanggan / tagihan", True),
    (3, 4, "topUpQuote()"), (4, 5, "POST /topup/quote"), (5, 2, "produk, harga, biaya, total", True),
    (0, 1, "Bayar + PIN"), (1, 2, "requestPin(); pay(pin)"), (2, 3, "execute(order, pin, kunci)"), (3, 4, "topUpOrder(kunci, body)"),
    (4, 5, "POST /topup/orders + Idempotency-Key"), (5, 2, "bukti transaksi (token PLN bila ada)", True)], w=15)


# ---------------- Peta navigasi D05A ----------------
def nav(name, nodes, edges, size):
    """nodes: id -> (x, y, teks, warna). edges: (a, b, label, lengkung, posisi label)."""
    fig, ax = plt.subplots(figsize=size); ax.axis("off")
    BW, BH = 2.35, 0.86
    rc = lambda k: (nodes[k][0] - BW / 2, nodes[k][1] - BH / 2, BW, BH)
    for k, v in nodes.items():
        fc = v[3] if len(v) > 3 else LBLUE
        x, y, w, h = rc(k)
        ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.02,rounding_size=0.1", fc=fc, ec=NAVY, lw=1.1, zorder=3))
        ax.text(v[0], v[1], v[2], ha="center", va="center", fontsize=8, zorder=4)
    for e in edges:
        a, b, t = e[:3]
        rd = e[3] if len(e) > 3 else 0
        pos = e[4] if len(e) > 4 else 0.5
        ca, cb = center(rc(a)), center(rc(b))
        if rd:
            dx, dy = cb[0] - ca[0], cb[1] - ca[1]
            mid = ((ca[0] + cb[0]) / 2 + dy * rd, (ca[1] + cb[1]) / 2 - dx * rd)
            p, q = edge(rc(a), mid), edge(rc(b), mid)
        else:
            p, q = edge(rc(a), cb), edge(rc(b), ca)
        ax.annotate("", xy=q, xytext=p, arrowprops=dict(arrowstyle="-|>", lw=1, color="#404040", connectionstyle="arc3,rad=%s" % rd), zorder=2)
        if t:
            dx, dy = q[0] - p[0], q[1] - p[1]
            ax.text(p[0] + dx * pos + dy * rd * 0.5, p[1] + dy * pos - dx * rd * 0.5, t, ha="center", va="center", fontsize=6.8, zorder=5, bbox=dict(fc="white", ec="none", pad=0.7))
    xs = [v[0] for v in nodes.values()]; ys = [v[1] for v in nodes.values()]
    ax.set_xlim(min(xs) - 1.5, max(xs) + 1.5); ax.set_ylim(min(ys) - 0.9, max(ys) + 0.9)
    save(fig, name)


T, G, S = LGOLD, LGREY, LGREEN  # tab bottom navigation; langkah/dialog di dalam layar; titik masuk
nav("n0_global.png", {
    "L": (0, 8, "AND-01 Masuk\nauth/login", S), "R": (-4.4, 8, "AND-02 Daftar\nauth/register"), "O": (-4.4, 6.4, "AND-03 Verifikasi OTP\nauth/otp"), "E": (-4.4, 4.8, "AND-04 e-KYC\nauth/ekyc"),
    "C": (-1.6, 6.0, "AND-05 Buat PIN\nauth/create_pin"), "P": (2.4, 6.0, "AND-06 Masukkan PIN\nauth/enter_pin"), "V": (4.4, 8, "AND-07 Pemulihan\nauth/recovery"),
    "H": (4.6, 4.4, "AND-08 Beranda\nhome", T), "HI": (-4.4, 2.4, "AND-16 Riwayat\nhistory", T), "Q": (0, 2.4, "AND-12 Bayar QRIS\nqris/pay", T), "K": (4.4, 2.4, "AND-10 Kantong\naccount/pockets", T), "PR": (8.8, 2.4, "AND-19 Profil\nprofile", T),
    "TR": (7.8, 8, "AND-11 Transfer\ntransfer"), "TU": (11.8, 6.6, "AND-14 Top-up\ntopup"), "VC": (11.8, 8, "AND-15 Kartu\nvirtualcard/home"), "CH": (11.8, 5.25, "AND-09 Grafik saldo\naccount/chart"), "N": (11.8, 3.9, "AND-23 Notifikasi\nnotifications"),
    "D": (-6.0, 0.6, "AND-17 Detail\nhistory/detail/{txId}"), "A": (-2.8, 0.6, "AND-18 Analitik\nhistory/analytics"), "QR": (0.6, 0.6, "AND-13 Terima QRIS\nqris/receive"),
    "SK": (5.2, 0.6, "AND-20 Keamanan\nprofile/security"), "SE": (8.8, 0.6, "AND-21 Sesi\nprofile/sessions"), "NS": (12.2, 0.6, "AND-22 Pengaturan\nnotifikasi"),
}, [("L", "R", "Buka rekening"), ("R", "O", "Daftar"), ("O", "E", "OTP valid"), ("E", "L", "Lanjut ke Login", -0.12, 0.25), ("L", "P", "PIN ada"), ("L", "C", "PIN belum ada"), ("C", "P", "disimpan"),
    ("L", "V", "Lupa"), ("P", "V", "Lupa PIN?"), ("P", "H", "PIN valid"), ("H", "TR", "Transfer", 0, 0.6), ("H", "TU", "Top-up", 0, 0.7), ("H", "VC", "Kartu", 0, 0.7), ("H", "CH", "Grafik Saldo", 0, 0.62), ("H", "N", "lonceng"),
    ("H", "K", "tab"), ("H", "Q", "tab"), ("H", "PR", "tab"), ("H", "HI", "tab", 0, 0.6), ("HI", "D", "pilih transaksi"), ("HI", "A", "Analitik"), ("Q", "QR", "Terima dana"),
    ("PR", "SK", "Keamanan"), ("PR", "SE", "Sesi"), ("PR", "NS", "Pengaturan")], (14.4, 8.4))
nav("n1_auth.png", {"L": (0, 4, "AND-01\nMasuk", S), "R": (3.8, 4, "AND-02\nDaftar"), "O": (7.6, 4, "AND-03\nVerifikasi OTP"), "E": (11.4, 4, "AND-04\ne-KYC"), "C": (7.6, 2, "AND-05\nBuat PIN"),
                    "P": (3.8, 2, "AND-06\nMasukkan PIN"), "H": (3.8, 0, "AND-08\nBeranda", T), "V": (0, 2, "AND-07\nPemulihan"), "SE": (7.6, 0, "AND-21\nSesi & perangkat")},
    [("L", "R", "Buka rekening"), ("R", "O", "Daftar"), ("O", "E", "OTP valid"), ("E", "L", "e-KYC disetujui: Lanjut ke Login", 0.22), ("L", "C", "e-KYC ok, PIN belum ada", 0, 0.68), ("L", "P", "PIN ada", 0, 0.4),
     ("C", "P", "disimpan"), ("P", "H", "PIN valid"), ("L", "V", "Lupa password/PIN"), ("P", "V", "Lupa PIN?"), ("H", "SE", "Profil")], (11, 5.2))
nav("n2_account.png", {"H": (0, 2, "AND-08\nBeranda", T), "GS": (4, 3.2, "AND-09\nGrafik saldo"), "K": (4, 0.8, "AND-10\nKantong", T), "F": (8.2, 0.8, "Dialog buat, isi, tarik,\nkunci, hapus", G), "P": (12.4, 0.8, "Dialog PIN", G)},
    [("H", "GS", "Grafik Saldo"), ("H", "K", "aksi cepat / tab"), ("K", "F", "pilih kantong"), ("F", "P", "tarik dini\n(terkunci)")], (11, 3.2))
nav("n3_transfer.png", {"H": (0, 2, "AND-08\nBeranda", T), "A": (3.8, 2, "AND-11\nPenerima"), "B": (7.6, 2, "AND-11\nNominal", G), "C": (11.4, 2, "AND-11\nKonfirmasi", G), "P": (11.4, 0, "Dialog PIN", G), "R": (7.6, 0, "AND-11\nBukti", G), "D": (3.8, 0, "AND-17\nDetail transaksi")},
    [("H", "A", "Transfer"), ("A", "B", "Cek rekening"), ("B", "C", "Lanjut"), ("C", "P", "Transfer sekarang"), ("P", "R", "PIN valid"), ("R", "D", "Lihat detail"), ("R", "H", "Kembali ke beranda", 0, 0.45)], (11, 3.3))
nav("n4_qris.png", {"H": (0, 2, "AND-08\nBeranda", T), "Q": (3.8, 2, "AND-12\nBayar QRIS", T), "K": (7.6, 2, "Detail pembayaran", G), "P": (11.4, 2, "Dialog PIN", G), "BK": (11.4, 0, "Bukti pembayaran", G), "TQ": (3.8, 0, "AND-13\nTerima QRIS")},
    [("H", "Q", "tab / aksi cepat"), ("Q", "K", "pindai / galeri"), ("K", "P", "Bayar"), ("P", "BK", "PIN valid"), ("BK", "H", "Kembali ke beranda", 0, 0.3), ("Q", "TQ", "Terima dana"), ("H", "TQ", "Terima QRIS")], (11, 3.3))
nav("n5_topup.png", {"H": (0, 2, "AND-08\nBeranda", T), "U": (3.8, 2, "AND-14\nTop-up & tagihan"), "K": (7.6, 2, "Konfirmasi", G), "P": (11.4, 2, "Dialog PIN", G), "BK": (7.6, 0, "Bukti / token listrik", G)},
    [("H", "U", "Top-up"), ("U", "K", "pilih produk"), ("K", "P", "Bayar"), ("P", "BK", "PIN valid"), ("BK", "H", "Kembali ke beranda", 0, 0.4)], (11, 3.3))
nav("n6_card.png", {"H": (0, 1.8, "AND-08\nBeranda", T), "V": (3.8, 1.8, "AND-15\nKartu"), "C": (8, 3.4, "Dialog buat kartu", G), "P": (8, 1.8, "Dialog PIN", G), "LM": (8, 0.2, "Dialog limit /\nuji transaksi", G), "D": (12, 1.8, "Nomor lengkap + CVV", G)},
    [("H", "V", "Kartu"), ("V", "C", "Buat kartu virtual"), ("V", "P", "Lihat detail"), ("V", "LM", "Atur limit / Uji"), ("P", "D", "PIN valid")], (11, 3.8))
nav("n7_history.png", {"H": (0, 2, "AND-08\nBeranda", T), "R": (4.2, 2, "AND-16\nRiwayat", T), "D": (8.6, 3.6, "AND-17\nDetail transaksi"), "A": (8.6, 0.4, "AND-18\nAnalitik bulanan"), "N": (12.6, 3.6, "AND-23\nKotak notifikasi")},
    [("H", "R", "tab / Lihat semua"), ("R", "D", "pilih transaksi"), ("R", "A", "Analitik"), ("H", "D", "transaksi terakhir"), ("H", "A", "Analitik"), ("N", "D", "buka transaksi")], (11, 3.9))
nav("n8_profile.png", {"H": (0, 2, "AND-08\nBeranda", T), "P": (4, 2, "AND-19\nProfil", T), "SK": (8.4, 4, "AND-20\nKeamanan"), "E": (8.4, 2, "AND-21\nSesi & perangkat"), "N": (8.4, 0, "AND-22 Pengaturan\nnotifikasi"), "O": (4, 4, "Dialog OTP\nubah data", G), "L": (4, 0, "AND-01\nMasuk", S), "K": (0, 0, "AND-23\nKotak notifikasi")},
    [("H", "P", "tab / avatar"), ("P", "SK", "Keamanan"), ("P", "E", "Sesi aktif"), ("SK", "E", "Kelola sesi"), ("P", "N", "Pengaturan"), ("P", "O", "Ubah data"), ("P", "L", "Keluar"), ("H", "K", "lonceng")], (9.6, 4.6))
print("ok", len(os.listdir(OUT)))
