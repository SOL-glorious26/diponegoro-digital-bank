"""Generate diagrams for PRJ-DDB-001 and SAD-DDB-001 (PNG, 200 dpi)."""
import sys, os, datetime as dt
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
from matplotlib.patches import FancyBboxPatch, Rectangle
from matplotlib import font_manager

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)
for f in ["pala.ttf", "palab.ttf", "palai.ttf"]:
    p = os.path.join(r"C:\Windows\Fonts", f)
    if os.path.exists(p):
        font_manager.fontManager.addfont(p)
plt.rcParams["font.family"] = ["Palatino Linotype", "DejaVu Sans"]
plt.rcParams["font.size"] = 10

NAVY, BLUE, LBLUE, GREY, LGREY, GOLD = "#1F3864", "#2E75B6", "#DCE6F2", "#7F7F7F", "#F2F2F2", "#BF9000"


def box(ax, x, y, w, h, text, fc=LBLUE, ec=NAVY, fs=9.5, bold=False, ls="-", tc="black"):
    ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.02,rounding_size=0.08",
                                fc=fc, ec=ec, lw=1.2, ls=ls))
    ax.text(x + w / 2, y + h / 2, text, ha="center", va="center", fontsize=fs,
            fontweight="bold" if bold else "normal", color=tc, wrap=True)


def arrow(ax, x1, y1, x2, y2, text="", both=False, fs=8, color="black", ls="-", off=(0, 0)):
    ax.annotate("", xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle="<|-|>" if both else "-|>", color=color, lw=1.1, ls=ls))
    if text:
        ax.text((x1 + x2) / 2 + off[0], (y1 + y2) / 2 + off[1], text, fontsize=fs, ha="center", va="center",
                bbox=dict(fc="white", ec="none", pad=0.6))


def canvas(w, h, xl, yl):
    fig, ax = plt.subplots(figsize=(w, h))
    ax.set_xlim(0, xl); ax.set_ylim(0, yl); ax.axis("off")
    return fig, ax


def save(fig, name):
    fig.savefig(os.path.join(OUT, name), dpi=200, bbox_inches="tight", facecolor="white")
    plt.close(fig)


# ---------------------------------------------------------------- Gantt (Doc 1)
def gantt():
    D = lambda s: dt.date(2026, int(s[3:5]), int(s[:2]))
    rows = [  # label, start, end, kind  (R=realisasi berbukti, P=rencana, M=milestone)
        ("Inisiasi dan riset kebutuhan", "15-09", "02-10", "R"),
        ("Analisis kebutuhan (SRS v0.1)", "22-09", "02-10", "R"),
        ("Perencanaan teknis (MDTA v0.1)", "03-10", "04-10", "R"),
        ("Desain UML per modul + prototipe awal", "03-10", "07-10", "R"),
        ("Pengembangan modul Android per PIC", "03-10", "07-10", "R"),
        ("Integrasi aplikasi Android (8 modul)", "08-10", "10-11", "R"),
        ("Pengembangan aplikasi Web", "03-10", "11-11", "R"),
        ("Pengembangan Backend & Database", "10-11", "11-11", "R"),
        ("Integrasi Android – Backend", "11-11", "11-11", "R"),
        ("API Gateway dan deployment", "12-11", "18-11", "P"),
        ("Pengujian sistem (emulator, peramban)", "11-11", "12-11", "R"),
        ("Uji ponsel fisik + laporan Dok. 6", "17-11", "22-11", "P"),
        ("Dokumentasi 9 kategori", "11-11", "23-11", "P"),
    ]
    ms = [("Project Brief + SRS", "02-10"), ("Pengumpulan progres", "07-10"), ("Backend 8 modul", "11-11"),
          ("Build baseline", "18-11"), ("Proyek selesai", "23-11")]
    fig, ax = plt.subplots(figsize=(11, 5.6))
    for i, (lab, s, e, k) in enumerate(rows):
        y = len(rows) - i
        s, e = D(s), D(e) + dt.timedelta(days=1)
        ax.barh(y, (e - s).days, left=s, height=0.55, color=NAVY if k == "R" else "white",
                edgecolor=NAVY, hatch=None if k == "R" else "////", lw=1)
    ax.set_yticks(range(len(rows), 0, -1)); ax.set_yticklabels([r[0] for r in rows], fontsize=9.5)
    for j, (lab, d) in enumerate(ms):
        ax.axvline(D(d) + dt.timedelta(days=1), color="#C00000", lw=0.9, ls="--")
        ax.text(D(d) + dt.timedelta(days=1), len(rows) + (1.15 if j % 2 == 0 else 0.7), lab, fontsize=8,
                ha="right" if j == len(ms) - 1 else ("left" if j == 0 else "center"), color="#C00000",
                bbox=dict(fc="white", ec="none", pad=0.5))
    ax.axvline(dt.date(2026, 11, 13), color=GREY, lw=1.4)
    ax.text(dt.date(2026, 11, 12), 0.35, "status 12 Nov", fontsize=8, ha="right", color=GREY)
    ax.set_xlim(dt.date(2026, 9, 15), dt.date(2026, 11, 24)); ax.set_ylim(0, len(rows) + 1.6)
    ax.xaxis.set_major_locator(mdates.WeekdayLocator(byweekday=mdates.TU))
    ax.xaxis.set_major_formatter(mdates.DateFormatter("%d/%m"))
    ax.grid(axis="x", color=LGREY); ax.set_axisbelow(True)
    for s in ["top", "right"]: ax.spines[s].set_visible(False)
    ax.set_xlabel("Tanggal (2026); garis kisi = awal minggu proyek (Selasa)")
    from matplotlib.patches import Patch
    ax.legend(handles=[Patch(fc=NAVY, ec=NAVY, label="Realisasi (berbukti)"),
                       Patch(fc="white", ec=NAVY, hatch="////", label="Rencana / berjalan")],
              loc="lower left", fontsize=8.5, frameon=False)
    save(fig, "d1_gantt.png")


# ---------------------------------------------------------------- V-Model (Doc 1)
def vmodel():
    fig, ax = canvas(10, 5, 10, 5)
    L = [("Kebutuhan\n(SRS-DDB-001)", 0.2, 3.9), ("Desain Arsitektur\n(Dokumen 2)", 1.2, 2.9),
         ("Desain Fungsional & Rinci\n(Dokumen 3, 4)", 2.2, 1.9)]
    R = [("Pengujian Penerimaan\n(UAT, demo – Dok. 8)", 7.6, 3.9), ("Pengujian Sistem\n(Dokumen 6)", 6.6, 2.9),
         ("Pengujian Integrasi\n(API, lintas platform)", 5.6, 1.9)]
    for t, x, y in L: box(ax, x, y, 2.2, 0.8, t, fs=9)
    for t, x, y in R: box(ax, x, y, 2.2, 0.8, t, fc="#E2EFDA", ec="#375623", fs=9)
    box(ax, 3.6, 0.5, 2.8, 0.8, "Implementasi + Pengujian Unit\n(63/58 test Android, 45 test backend)", fc=LGREY, ec=GREY, fs=9)
    for (a, b) in [((1.3, 3.9), (2.3, 3.7)), ((2.3, 2.9), (3.3, 2.7)), ((3.3, 1.9), (4.2, 1.3))]:
        arrow(ax, *a, *b)
    for (a, b) in [((5.8, 1.3), (6.6, 1.9)), ((6.9, 2.7), (7.6, 2.9)), ((7.9, 3.7), (8.6, 3.9))]:
        arrow(ax, *a, *b)
    arrow(ax, 2.6, 4.3, 7.4, 4.3, "kriteria penerimaan FR", ls="--", color=GREY)
    arrow(ax, 3.6, 3.3, 6.4, 3.3, "NFR, alur end-to-end", ls="--", color=GREY)
    arrow(ax, 4.5, 2.3, 5.5, 2.3, ls="--", color=GREY)
    ax.text(5.0, 1.55, "kontrak API, sequence", ha="center", fontsize=8, color=GREY)
    ax.text(0.2, 4.85, "Verifikasi (desain)", fontsize=9, fontweight="bold")
    ax.text(7.8, 4.85, "Validasi (pengujian)", fontsize=9, fontweight="bold")
    save(fig, "d1_vmodel.png")


# ---------------------------------------------------------------- Risk heat map (Doc 1)
def risk():
    risks = {  # id: (peluang 1-3, dampak 1-3)
        "R1": (2, 3), "R2": (1, 2), "R3": (1, 3), "R4": (1, 3), "R5": (2, 2), "R6": (3, 2), "R7": (2, 2),
        "R8": (1, 2), "R9": (1, 2), "R10": (3, 3), "R11": (1, 3), "R12": (1, 3), "R13": (2, 2), "R14": (2, 1), "R15": (2, 2),
        "R16": (2, 2), "R17": (2, 2)}
    fig, ax = plt.subplots(figsize=(6.2, 4.6))
    col = {2: "#E2EFDA", 3: "#E2EFDA", 4: "#FFF2CC", 5: "#FCE4D6", 6: "#F8CBAD", 7: "#F4B183", 9: "#E06666"}
    for p in (1, 2, 3):
        for d in (1, 2, 3):
            s = p * d
            c = "#E2EFDA" if s <= 2 else ("#FFF2CC" if s <= 4 else ("#F8CBAD" if s <= 6 else "#E06666"))
            ax.add_patch(Rectangle((d - 0.5, p - 0.5), 1, 1, fc=c, ec="white", lw=2))
            ids = [k for k, v in risks.items() if v == (p, d)]
            ax.text(d, p, "\n".join([", ".join(ids[i:i + 3]) for i in range(0, len(ids), 3)]), ha="center", va="center", fontsize=9)
    ax.set_xlim(0.5, 3.5); ax.set_ylim(0.5, 3.5)
    ax.set_xticks([1, 2, 3]); ax.set_xticklabels(["Rendah (1)", "Sedang (2)", "Tinggi (3)"])
    ax.set_yticks([1, 2, 3]); ax.set_yticklabels(["Rendah (1)", "Sedang (2)", "Tinggi (3)"])
    ax.set_xlabel("Dampak"); ax.set_ylabel("Peluang")
    for s in ax.spines.values(): s.set_visible(False)
    ax.tick_params(length=0)
    save(fig, "d1_riskmap.png")


# ---------------------------------------------------------------- Context (Doc 2)
def context():
    fig, ax = canvas(10, 5.6, 10, 5.6)
    box(ax, 3.5, 2.1, 3, 1.4, "Platform\nPT Diponegoro Digital Bank\n(Android, Web, Backend, DB)", fc=NAVY, ec=NAVY, fs=10, bold=True, tc="white")
    users = [("Mahasiswa", 0.2, 4.4), ("Profesional", 0.2, 3.0), ("UMKM", 0.2, 1.6)]
    for t, x, y in users:
        box(ax, x, y, 1.8, 0.7, "Nasabah: " + t, fc="white", fs=9)
        arrow(ax, x + 1.8, y + 0.35, 3.5, 2.8, both=True)
    box(ax, 0.2, 0.2, 1.8, 0.7, "Administrator sistem\n(internal, di luar UI ritel)", fc=LGREY, ec=GREY, fs=8.5, ls="--")
    arrow(ax, 2.0, 0.55, 3.6, 2.1, ls="--", color=GREY)
    ext = [("Jaringan antarbank\n(simulator)", 4.6), ("Jaringan QRIS\n(simulator)", 3.6), ("Penyedia pulsa/PLN/\ntagihan (simulator)", 2.6),
           ("Penerbit kartu\n(simulator)", 1.6), ("Penyedia OTP & e-KYC\n(simulasi)", 0.6)]
    for t, y in ext:
        box(ax, 7.9, y, 1.9, 0.75, t, fc="#FFF2CC", ec=GOLD, fs=8.5)
        arrow(ax, 6.5, 2.8, 7.9, y + 0.37, both=True)
    ax.text(5, 5.35, "Aktor", fontsize=9, ha="center", alpha=0)
    ax.text(1.1, 5.3, "Pengguna", fontsize=9.5, fontweight="bold", ha="center")
    ax.text(8.85, 5.5, "Sistem eksternal (via adapter)", fontsize=9.5, fontweight="bold", ha="center")
    save(fig, "d2_context.png")


# ---------------------------------------------------------------- High-level (Doc 2)
def highlevel():
    fig, ax = canvas(10, 6.6, 10, 6.6)
    layers = [("KLIEN", 5.4), ("GERBANG", 4.2), ("LAYANAN INTI", 2.0), ("DATA", 0.2)]
    for t, y in layers:
        ax.text(0.05, y + 0.5, t, fontsize=8.5, fontweight="bold", rotation=90, va="center", color=GREY)
    box(ax, 0.6, 5.4, 3.9, 0.95, "Aplikasi Android\nKotlin · Jetpack Compose · MVVM", fs=9.5)
    box(ax, 5.5, 5.4, 3.9, 0.95, "Aplikasi Web\n(SPA; framework menunggu konfirmasi)", fs=9.5, ls="--")
    box(ax, 2.5, 4.2, 5, 0.8, "API Gateway (Nginx) — /api/**, rate limit, CORS, TLS", fc=LGREY, ec=GREY, fs=9.5)
    arrow(ax, 2.5, 5.4, 4.0, 5.0, "HTTPS/JSON + JWT", fs=8)
    arrow(ax, 7.4, 5.4, 6.0, 5.0, "HTTPS/JSON + JWT", fs=8)
    ax.add_patch(FancyBboxPatch((0.6, 1.95), 8.8, 1.95, boxstyle="round,pad=0.02", fc="white", ec=NAVY, lw=1.4))
    ax.text(5, 3.68, "Backend — Spring Boot modular monolith (Java 21)", ha="center", fontsize=9.5, fontweight="bold")
    mods = ["auth", "account", "transfer", "payment", "topup", "card", "history", "profile +\nnotification"]
    for i, m in enumerate(mods):
        box(ax, 0.8 + i * 1.08, 2.85, 1.0, 0.62, m, fs=8)
    box(ax, 0.8, 2.08, 5.3, 0.58, "ledger — Financial Transaction Core (double-entry, atomik, idempotensi)", fc="#E2EFDA", ec="#375623", fs=8.2)
    box(ax, 6.25, 2.08, 3.0, 0.58, "security · common · audit", fc=LGREY, ec=GREY, fs=8.2)
    arrow(ax, 5, 4.2, 5, 3.9)
    box(ax, 0.8, 0.2, 3.6, 0.9, "PostgreSQL (Flyway)\n17 tabel; H2 untuk test", fs=9)
    box(ax, 5.6, 0.2, 3.6, 0.9, "Simulator eksternal\nantarbank · QRIS · biller · kartu", fc="#FFF2CC", ec=GOLD, fs=9)
    arrow(ax, 2.6, 1.95, 2.6, 1.1, "JPA/JDBC", fs=8)
    arrow(ax, 7.4, 1.95, 7.4, 1.1, "adapter", fs=8)
    save(fig, "d2_highlevel.png")


# ---------------------------------------------------------------- Backend modules (Doc 2)
def modules():
    fig, ax = canvas(10, 6, 10, 6)
    mods = [("auth", "FR-01–05", 0.3, 4.6), ("account", "FR-06–09", 2.7, 4.6), ("transfer", "FR-10–14", 5.1, 4.6), ("payment", "FR-15–17", 7.5, 4.6),
            ("topup", "FR-18–20", 0.3, 3.1), ("card", "FR-21–22", 2.7, 3.1), ("history", "FR-23–26", 5.1, 3.1), ("profile · notification", "FR-27–29", 7.5, 3.1)]
    for n, fr, x, y in mods:
        box(ax, x, y, 2.2, 1.0, f"{n}\n{fr}\nController → Service", fs=8.5)
    box(ax, 0.3, 1.45, 4.6, 1.2, "ledger — LedgerService\njurnal double-entry, kunci baris (FOR UPDATE), idempotensi,\nrefund; menerbitkan event transaksi\ndipakai oleh: account, transfer, payment, topup, card, history, auth", fc="#E2EFDA", ec="#375623", fs=8.3)
    box(ax, 5.1, 1.45, 4.6, 1.2, "auth.TransactionGuard\ne-KYC disetujui + verifikasi PIN (blokir 3× salah)\ndipakai oleh: account, transfer, payment, topup, card", fc="#FCE4D6", ec="#C55A11", fs=8.3)
    box(ax, 0.3, 0.2, 3.0, 0.9, "security\nJwtService, SecurityConfig,\nSessionJwtConverter", fc=LGREY, ec=GREY, fs=8.2)
    box(ax, 3.5, 0.2, 3.0, 0.9, "common\nerror seragam, Idempotency,\nMoney, DataCipher, Audit", fc=LGREY, ec=GREY, fs=8.2)
    box(ax, 6.7, 0.2, 3.0, 0.9, "notification (@Async)\nmenerima event ledger;\nin-app · push · email outbox", fc=LGREY, ec=GREY, fs=8.2)
    ax.text(5, 5.85, "Modul bisnis (satu paket per modul SRS)", ha="center", fontsize=9.5, fontweight="bold")
    save(fig, "d2_modules.png")


# ---------------------------------------------------------------- Data groups (Doc 2)
def data():
    fig, ax = canvas(10, 4.8, 10, 4.8)
    groups = [("Identitas & akses (auth)", "users\notp_codes\nekyc_verifications\ntransaction_pins\nsessions\nbiometric_devices", 0.2, "#DCE6F2"),
              ("Keuangan (ledger)", "ledger_accounts\nledger_transactions\njournal_lines", 2.7, "#E2EFDA"),
              ("Fitur nasabah", "pockets\nfavorite_recipients\npaid_bills\nvirtual_cards", 5.2, "#FFF2CC"),
              ("Notifikasi & audit", "notification_settings\nnotifications\nemail_outbox\naudit_events", 7.7, LGREY)]
    for t, body, x, c in groups:
        ax.add_patch(FancyBboxPatch((x, 0.3), 2.1, 4.0, boxstyle="round,pad=0.02", fc=c, ec=NAVY, lw=1.1))
        ax.text(x + 1.05, 4.0, t, ha="center", fontsize=9, fontweight="bold")
        ax.text(x + 1.05, 2.2, body, ha="center", va="center", fontsize=9, family="monospace")
    arrow(ax, 2.3, 2.3, 2.7, 2.3, both=True)
    arrow(ax, 4.8, 2.3, 5.2, 2.3, both=True)
    arrow(ax, 7.3, 2.3, 7.7, 2.3, both=True)
    save(fig, "d2_data.png")


# ---------------------------------------------------------------- Android (Doc 2)
def android():
    fig, ax = canvas(10, 5.4, 10, 5.4)
    rows = [("presentation", "Composable Screen  ↔  ViewModel (StateFlow)  —  navigation: AuthNavGraph, bottom bar", 4.3, LBLUE),
            ("domain", "Model, aturan bisnis, UseCase (mis. CreateVirtualCardUseCase, TransferRules, QrisRules)", 3.2, "#E2EFDA"),
            ("data", "Repository per modul — Local (in-memory, varian demo) / Remote (REST Retrofit, varian online)", 2.1, "#FFF2CC")]
    for n, t, y, c in rows:
        box(ax, 0.2, y, 1.6, 0.85, n, fc=NAVY, ec=NAVY, tc="white", bold=True, fs=9.5)
        box(ax, 2.0, y, 7.8, 0.85, t, fc=c, fs=9)
    box(ax, 0.2, 0.2, 9.6, 1.6, "", fc="white", ec=GREY)
    ax.text(5, 1.55, "core (lintas modul)", ha="center", fontsize=9, fontweight="bold")
    for i, t in enumerate(["ledger\n(in-memory)", "security\nTransactionGuard", "notification\nNotificationCenter", "di\nCore/FeatureModule", "ui · receipt\nstorage · util"]):
        box(ax, 0.4 + i * 1.88, 0.35, 1.7, 0.95, t, fc=LGREY, ec=GREY, fs=8.3)
    arrow(ax, 5.9, 4.3, 5.9, 4.05); arrow(ax, 5.9, 3.2, 5.9, 2.95); arrow(ax, 5.9, 2.1, 5.9, 1.8)
    save(fig, "d2_android.png")


# ---------------------------------------------------------------- Deployment (Doc 2)
def deploy():
    fig, ax = canvas(10, 5.2, 10, 5.2)
    box(ax, 0.2, 3.9, 2.2, 0.9, "Perangkat Android\nAPK (minSdk 24)", fc="white", fs=9)
    box(ax, 0.2, 2.5, 2.2, 0.9, "Peramban\n(aplikasi Web)", fc="white", fs=9, ls="--")
    ax.add_patch(FancyBboxPatch((3.0, 0.3), 6.8, 4.6, boxstyle="round,pad=0.02", fc="#FAFAFA", ec=NAVY, lw=1.3, ls="--"))
    ax.text(6.4, 4.65, "Host Docker (lokal / Cloud-VPS) — docker-compose.yml", ha="center", fontsize=9, fontweight="bold")
    box(ax, 3.3, 3.2, 2.4, 1.1, "gateway\nnginx:1.27-alpine\nport 80 (publik)", fc=LGREY, ec=GREY, fs=8.8)
    box(ax, 6.6, 3.2, 2.9, 1.1, "backend\nddb-backend (Spring Boot)\nport 8080 (internal)", fs=8.8)
    box(ax, 6.6, 0.8, 2.9, 1.3, "db\npostgres:17-alpine\nvolume ddb-data", fc="#E2EFDA", ec="#375623", fs=8.8)
    box(ax, 3.3, 0.8, 2.4, 1.3, ".env\nDB_PASSWORD, JWT_SECRET,\nDATA_KEY, EXPOSE_OTP,\nSEED_DEMO_DATA", fc="#FFF2CC", ec=GOLD, fs=8)
    arrow(ax, 2.4, 4.35, 3.3, 3.9, "HTTPS", fs=8)
    arrow(ax, 2.4, 2.95, 3.3, 3.5, "HTTPS", fs=8)
    arrow(ax, 5.7, 3.75, 6.6, 3.75, "HTTP", fs=8)
    arrow(ax, 8.05, 3.2, 8.05, 2.1, "JDBC", fs=8)
    arrow(ax, 5.7, 1.45, 6.6, 3.4, color=GREY, ls=":")
    save(fig, "d2_deploy.png")


# ---------------------------------------------------------------- Transfer runtime (Doc 2)
def sequence():
    parts = ["Klien\n(Android/Web)", "API Gateway", "TransferService", "TransactionGuard", "LedgerService", "InterbankGateway\n(simulator)", "PostgreSQL"]
    fig, ax = canvas(11, 6.4, 11, 6.4)
    xs = [0.8 + i * 1.6 for i in range(len(parts))]
    for x, p in zip(xs, parts):
        box(ax, x - 0.7, 5.6, 1.4, 0.7, p, fs=8.3)
        ax.plot([x, x], [0.2, 5.6], color=GREY, lw=0.8, ls="--")
    msgs = [(0, 1, "POST /transfers + Idempotency-Key + JWT"), (1, 2, "teruskan (rate limit OK)"), (2, 3, "cek e-KYC + verifikasi PIN"),
            (2, 4, "post(jurnal, kunci idempotensi)"), (4, 6, "SELECT … FOR UPDATE; INSERT jurnal"), (2, 5, "kirim (antarbank)"),
            (5, 2, "sukses / ditolak (A6) / timeout (A5)"), (2, 4, "bila ditolak: jurnal pembalik (refund)"), (2, 0, "status + bukti; notifikasi @Async")]
    y = 5.2
    for a, b, t in msgs:
        arrow(ax, xs[a], y, xs[b], y)
        ax.text((xs[a] + xs[b]) / 2, y + 0.12, t, ha="center", fontsize=7.8)
        y -= 0.56
    save(fig, "d2_sequence.png")


for f in (gantt, vmodel, risk, context, highlevel, modules, data, android, deploy, sequence):
    f()
print("ok", sorted(os.listdir(OUT)))
