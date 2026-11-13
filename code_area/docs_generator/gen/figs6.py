"""Diagrams for SDD-DDB-001-WEB (Detailed Design Web). Usage: python figs6.py <out_dir>"""
import sys, os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, Rectangle, Polygon
from matplotlib import font_manager

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)
for f in ["pala.ttf", "palab.ttf", "palai.ttf"]:
    p = os.path.join(r"C:\Windows\Fonts", f)
    if os.path.exists(p):
        font_manager.fontManager.addfont(p)
plt.rcParams["font.family"] = ["Palatino Linotype", "DejaVu Sans"]
NAVY, LBLUE, LGREY, GREY, RED, LRED, GREEN, LGREEN, GOLD, LGOLD = "#1F3864", "#DCE6F2", "#F2F2F2", "#7F7F7F", "#C00000", "#FCE4D6", "#375623", "#E2EFDA", "#BF9000", "#FFF2CC"
RH = 0.36


def save(fig, name):
    fig.savefig(os.path.join(OUT, name), dpi=200, bbox_inches="tight", facecolor="white")
    plt.close(fig)


def edge(rect, p):
    x, y, w, h = rect
    cx, cy = x + w / 2, y + h / 2
    dx, dy = p[0] - cx, p[1] - cy
    if dx == 0 and dy == 0:
        return cx, cy
    tx = (w / 2) / abs(dx) if dx else 1e9
    ty = (h / 2) / abs(dy) if dy else 1e9
    t = min(tx, ty)
    return cx + dx * t, cy + dy * t


def center(r):
    return r[0] + r[2] / 2, r[1] + r[3] / 2


def cls(ax, x, ytop, w, name, attrs, methods, st=None, fill=LBLUE):
    n_head = 2 if st else 1
    h = RH * (n_head + max(len(attrs), 1) + max(len(methods), 1)) + 0.25
    y = ytop - h
    ax.add_patch(Rectangle((x, y), w, h, fc="white", ec=NAVY, lw=1.2, zorder=3))
    ax.add_patch(Rectangle((x, ytop - RH * n_head - 0.05), w, RH * n_head + 0.05, fc=fill, ec=NAVY, lw=1.2, zorder=3))
    yy = ytop - RH / 2 - 0.03
    if st:
        ax.text(x + w / 2, yy, f"«{st}»", ha="center", va="center", fontsize=7.4, style="italic", zorder=4); yy -= RH
    ax.text(x + w / 2, yy, name, ha="center", va="center", fontsize=8.6, fontweight="bold", zorder=4)
    yy = ytop - RH * n_head - 0.05 - RH / 2 - 0.05
    for a in attrs or [""]:
        ax.text(x + 0.12, yy, a, fontsize=7, va="center", zorder=4, family="DejaVu Sans Mono"); yy -= RH
    ax.plot([x, x + w], [yy + RH / 2 + 0.02, yy + RH / 2 + 0.02], color=NAVY, lw=1, zorder=4)
    yy -= 0.08
    for m in methods or [""]:
        ax.text(x + 0.12, yy, m, fontsize=7, va="center", zorder=4, family="DejaVu Sans Mono"); yy -= RH
    return (x, y, w, h)


def link(ax, a, b, kind="dep", label=""):
    pa, pb = edge(a, center(b)), edge(b, center(a))
    if kind == "dep":
        ax.annotate("", xy=pb, xytext=pa, arrowprops=dict(arrowstyle="->", ls="--", lw=1, color=GREY), zorder=2)
    else:
        ax.plot([pa[0], pb[0]], [pa[1], pb[1]], color="black", lw=1, zorder=2)
        if kind == "comp":
            ax.plot(*pb, marker="D", ms=6, color="black", zorder=5)
    if label:
        ax.text((pa[0] + pb[0]) / 2, (pa[1] + pb[1]) / 2, label, fontsize=7, ha="center", va="center", style="italic", color=GREY, zorder=5, bbox=dict(fc="white", ec="none", pad=0.3))


def sequence(name, parts, msgs, w=11):
    n = len(parts)
    h = 1.6 + len(msgs) * 0.56
    fig, ax = plt.subplots(figsize=(w, h * 0.62)); ax.set_xlim(0, w); ax.set_ylim(0, h); ax.axis("off")
    step = (w - 0.6) / n
    xs = [0.3 + step * (i + 0.5) for i in range(n)]
    for x, p in zip(xs, parts):
        ax.add_patch(FancyBboxPatch((x - step / 2 + 0.08, h - 0.95), step - 0.16, 0.75, boxstyle="round,pad=0.02", fc=LBLUE, ec=NAVY))
        ax.text(x, h - 0.58, p, ha="center", va="center", fontsize=8)
        ax.plot([x, x], [0.1, h - 0.95], color=GREY, lw=0.8, ls="--")
    y = h - 1.35
    for a, b, t, *ret in msgs:
        if a == b:
            ax.annotate("", xy=(xs[a] + 0.05, y - 0.2), xytext=(xs[a] + 0.05, y), arrowprops=dict(arrowstyle="-|>", connectionstyle="bar,fraction=-0.6", lw=1))
            ax.text(xs[a] + 0.25, y - 0.05, t, fontsize=7.5, va="center")
        else:
            ax.annotate("", xy=(xs[b], y), xytext=(xs[a], y), arrowprops=dict(arrowstyle="-|>", lw=1, ls="--" if ret else "-", color=GREY if ret else "black"))
            ax.text((xs[a] + xs[b]) / 2, y + 0.11, t, ha="center", fontsize=7.5)
        y -= 0.56
    save(fig, name)


def flow(name, steps):
    H = 0.62
    height = len(steps) * 1.2 + 0.4
    fig, ax = plt.subplots(figsize=(8.6, height * 0.62 + 0.3)); ax.set_xlim(0, 10); ax.set_ylim(0, height); ax.axis("off")
    xc, w = 3.4, 4.4
    y = height - 0.6; prev = None; last = None
    for st in steps:
        k = st[0]
        if k in ("s", "e"):
            ax.add_patch(FancyBboxPatch((xc - 2.2, y - 0.25), 4.4, 0.5, boxstyle="round,pad=0.02,rounding_size=0.25", fc=NAVY, ec=NAVY))
            ax.text(xc, y, st[1], ha="center", va="center", fontsize=8.6, color="white", fontweight="bold"); top, bot = y + 0.25, y - 0.25
        elif k == "a":
            ax.add_patch(FancyBboxPatch((xc - w / 2, y - H / 2), w, H, boxstyle="round,pad=0.02,rounding_size=0.08", fc=LGREEN, ec=NAVY, lw=1))
            ax.text(xc, y, st[1], ha="center", va="center", fontsize=8.2); top, bot = y + H / 2, y - H / 2
        else:
            dw, dh = 3.4, 0.86
            ax.add_patch(Polygon([(xc, y + dh / 2), (xc + dw / 2, y), (xc, y - dh / 2), (xc - dw / 2, y)], closed=True, fc=LGOLD, ec=GOLD, lw=1))
            ax.text(xc, y, st[1], ha="center", va="center", fontsize=7.6); top, bot = y + dh / 2, y - dh / 2
            ax.add_patch(FancyBboxPatch((6.6, y - 0.3), 3.3, 0.6, boxstyle="round,pad=0.02,rounding_size=0.08", fc=LRED, ec=RED, lw=1))
            ax.text(8.25, y, st[2], ha="center", va="center", fontsize=7.4)
            ax.annotate("", xy=(6.6, y), xytext=(xc + dw / 2, y), arrowprops=dict(arrowstyle="-|>", color=RED, lw=1))
            ax.text(5.6, y + 0.13, st[3] if len(st) > 3 else "tidak", fontsize=8, color=RED)
        if prev is not None:
            ax.annotate("", xy=(xc, top), xytext=(xc, prev), arrowprops=dict(arrowstyle="-|>", lw=1))
            if last == "d":
                ax.text(xc + 0.1, (prev + top) / 2, "ya", fontsize=8, va="center")
        prev, last = bot, k
        y -= 1.2
    save(fig, name)


# ------------------------------------------------------------ class diagram: inti aplikasi
def class_core():
    fig, ax = plt.subplots(figsize=(11, 7.4)); ax.set_xlim(0, 15); ax.set_ylim(0, 10); ax.axis("off")
    app = cls(ax, 0.2, 9.8, 3.6, "App (main.jsx)", ["BrowserRouter", "Routes / Route"], ["render()"], st="component")
    prov = cls(ax, 5.2, 9.8, 4.6, "AuthProvider", ["user: User | null", "state: loading|in|out", "notice: string"],
               ["login(identifier, password)", "logout(all)", "acceptTokens(tokens)", "reloadUser()"], st="context")
    guard = cls(ax, 0.2, 6.6, 3.6, "RequireAuth / GuestOnly", ["onboarding: boolean"], ["onboardingStep(user)", "→ Navigate | Outlet"], st="route guard")
    client = cls(ax, 10.6, 9.8, 4.2, "api (client.js)", ["accessToken (memori)", "refreshToken (sessionStorage)", "baseURL = /api/v1"],
                 ["setTokens() / clearTokens()", "refreshSession()", "errorMessage(err)", "newIdempotencyKey()", "withKey(key)"], st="axios instance")
    layout = cls(ax, 5.2, 5.7, 4.6, "Layout", ["NAV[8 menu]", "unread: number"], ["poll unread-count 30 dtk", "<Outlet/>"], st="component")
    pages = cls(ax, 0.2, 3.6, 4.9, "Halaman (pages/*.jsx)", ["Dashboard, Transfer, TopUp, Qris", "History, Pockets, Cards", "Profile, Notifications, AuthPages"],
                ["useState / useEffect", "api.get/post/put/delete"], st="page")
    comps = cls(ax, 5.9, 3.0, 4.2, "Komponen bersama", ["PinDialog, TxDetailModal", "BalanceChart, Avatar", "ui.jsx: Button, Modal, Card…"], [""], st="component")
    fmt = cls(ax, 10.9, 3.0, 3.9, "lib/format.js", [""], ["rupiah(), dateTime() WIB", "parseAmount()", "deviceId(), deviceName()"], st="utility")
    be = cls(ax, 10.9, 5.0, 3.9, "Backend /api/v1", ["73 operasi REST"], ["JSON {code,message,fields}"], st="external", fill=LGREY)
    link(ax, app, prov, "comp", "membungkus")
    link(ax, app, guard, "comp")
    link(ax, guard, prov, "dep", "useAuth()")
    link(ax, guard, layout, "dep")
    link(ax, layout, pages, "dep", "Outlet")
    link(ax, prov, client, "dep")
    link(ax, pages, comps, "dep")
    # pages -> api: jalur siku di bawah Layout agar tidak menembus kotak lain
    ax.plot([5.1, 10.35, 10.35], [3.12, 3.12, 6.9], color=GREY, lw=1, ls="--", zorder=2)
    ax.annotate("", xy=(10.6, 6.9), xytext=(10.35, 6.9), arrowprops=dict(arrowstyle="->", lw=1, color=GREY), zorder=2)
    ax.text(10.15, 4.6, "panggil API", fontsize=7, ha="center", va="center", rotation=90, style="italic", color=GREY)
    link(ax, comps, fmt, "dep")
    link(ax, client, be, "dep", "HTTP via proxy/gateway")
    save(fig, "cls_web_core.png")


# ------------------------------------------------------------ peta route
def route_map():
    fig, ax = plt.subplots(figsize=(11, 5.4)); ax.set_xlim(0, 15); ax.set_ylim(0, 7.4); ax.axis("off")

    def box(x, y, w, t, fc=LBLUE, ec=NAVY, bold=False):
        ax.add_patch(FancyBboxPatch((x, y), w, 0.62, boxstyle="round,pad=0.02,rounding_size=0.08", fc=fc, ec=ec, lw=1))
        ax.text(x + w / 2, y + 0.31, t, ha="center", va="center", fontsize=7.8, fontweight="bold" if bold else "normal")
        return (x, y, w, 0.62)

    def grp(x, y, w, h, t):
        ax.add_patch(Rectangle((x, y), w, h, fc="none", ec=GREY, lw=1, ls="--"))
        ax.text(x + 0.12, y + h - 0.22, t, fontsize=8, style="italic", color=GREY)

    grp(0.2, 4.2, 4.2, 3.0, "GuestOnly (belum login)")
    l1 = box(0.5, 5.9, 3.6, "/login  – Masuk (FR-03)", bold=True)
    box(0.5, 5.1, 3.6, "/daftar – Daftar + OTP (FR-01)")
    box(0.5, 4.3, 3.6, "/lupa – Pemulihan (FR-05)")
    grp(5.0, 4.2, 4.2, 3.0, "RequireAuth onboarding")
    e1 = box(5.3, 5.9, 3.6, "/ekyc – e-KYC (FR-02)", bold=True)
    p1 = box(5.3, 4.7, 3.6, "/pin/buat – Buat PIN (FR-04)", bold=True)
    grp(0.2, 0.2, 14.6, 3.6, "RequireAuth + Layout (e-KYC APPROVED dan PIN sudah dibuat)")
    items = [("/ – Beranda (FR-06, 07, 25)", 0.5, 2.6), ("/transfer (FR-10–14)", 4.1, 2.6), ("/topup (FR-18–20)", 7.7, 2.6), ("/qris (FR-16, 17)", 11.3, 2.6),
             ("/riwayat (FR-23, 24, 26)", 0.5, 1.6), ("/kantong (FR-08, 09)", 4.1, 1.6), ("/kartu (FR-21, 22)", 7.7, 1.6), ("/profil (FR-05, 27, 28)", 11.3, 1.6),
             ("/notifikasi (FR-29)", 0.5, 0.6)]
    boxes = [box(x, y, 3.3, t) for t, x, y in items]
    ax.text(4.3, 0.9, "*  (lainnya) → Navigate ke /", fontsize=8, style="italic", color=GREY)
    ax.annotate("", xy=(5.3, 6.2), xytext=(4.1, 6.2), arrowprops=dict(arrowstyle="-|>", lw=1)); ax.text(4.7, 6.35, "login OK", fontsize=7, ha="center")
    ax.annotate("", xy=(7.1, 5.32), xytext=(7.1, 5.9), arrowprops=dict(arrowstyle="-|>", lw=1)); ax.text(7.2, 5.55, "APPROVED", fontsize=7)
    ax.annotate("", xy=(2.15, 3.22), xytext=(7.1, 4.7), arrowprops=dict(arrowstyle="-|>", lw=1)); ax.text(5.2, 3.85, "PIN dibuat", fontsize=7)
    ax.add_patch(FancyBboxPatch((10.0, 5.3), 4.6, 1.3, boxstyle="round,pad=0.02,rounding_size=0.08", fc=LRED, ec=RED, lw=1))
    ax.text(12.3, 5.95, "Dari halaman mana pun:\nlogout atau sesi berakhir (refresh gagal)\n→ kembali ke /login", fontsize=7.4, ha="center", va="center")
    save(fig, "route_map.png")


# ------------------------------------------------------------ state: sesi & halaman transfer
def states():
    fig, ax = plt.subplots(figsize=(10, 3.6)); ax.set_xlim(0, 13); ax.set_ylim(0, 4.6); ax.axis("off")

    def stb(x, y, t, fc):
        ax.add_patch(FancyBboxPatch((x - 1.2, y - 0.33), 2.4, 0.66, boxstyle="round,pad=0.02,rounding_size=0.25", fc=fc, ec=NAVY, zorder=3))
        ax.text(x, y, t, ha="center", va="center", fontsize=9.5, fontweight="bold", zorder=4)

    def ar(a, b, t=None, tp=None, rad=0):
        ax.annotate("", xy=b, xytext=a, arrowprops=dict(arrowstyle="-|>", lw=1.1, connectionstyle=f"arc3,rad={rad}"))
        if t: ax.text(tp[0], tp[1], t, fontsize=7.8, ha="center", va="center")
    ax.plot(0.5, 2.3, "o", ms=10, color="black")
    stb(3.0, 2.3, "loading", LGOLD); stb(8.0, 3.7, "in", LGREEN); stb(8.0, 0.9, "out", LRED)
    ar((0.65, 2.3), (1.8, 2.3), "muat aplikasi", (1.2, 2.55))
    ar((4.2, 2.5), (6.8, 3.6), "refresh token valid", (5.2, 3.35))
    ar((4.2, 2.1), (6.8, 1.0), "tidak ada / ditolak", (5.2, 1.3))
    ar((8.3, 1.25), (8.3, 3.35), "login / verify-otp", (9.4, 2.3))
    ar((7.7, 3.35), (7.7, 1.25), "logout / 401 gagal refresh", (6.4, 2.3))
    ax.text(11.2, 3.7, "Halaman terproteksi\ndapat diakses", fontsize=8, ha="center", style="italic", color=GREY)
    ax.text(11.2, 0.9, "Diarahkan ke /login\n(+ pesan notice)", fontsize=8, ha="center", style="italic", color=GREY)
    save(fig, "state_session.png")

    fig, ax = plt.subplots(figsize=(10, 3.6)); ax.set_xlim(0, 13.5); ax.set_ylim(0, 4.6); ax.axis("off")
    Y = 2.4
    ax.plot(0.4, Y, "o", ms=10, color="black")
    for x, t, fc in [(2.2, "FORM", LBLUE), (5.4, "KONFIRMASI", LGOLD), (8.6, "PIN", LGOLD), (11.8, "HASIL", LGREEN)]:
        stb(x, Y, t, fc)
    ar((0.55, Y), (1.0, Y))
    for x0, x1, t in [(3.4, 4.2, "inquiry +\nquote OK"), (6.6, 7.4, "Lanjut"), (9.8, 10.6, "POST\n/transfers")]:
        ar((x0, Y), (x1, Y)); ax.text((x0 + x1) / 2, Y - 0.62, t, fontsize=7.2, ha="center", va="center")
    ax.annotate("", xy=(2.2, Y + 0.35), xytext=(5.0, Y + 0.35), arrowprops=dict(arrowstyle="-|>", lw=0.9, ls="--", connectionstyle="arc3,rad=0.5"))
    ax.text(3.6, Y + 1.45, "Ubah / Batal", fontsize=7.4, ha="center")
    ax.annotate("", xy=(9.0, Y + 0.35), xytext=(8.2, Y + 0.35), arrowprops=dict(arrowstyle="-|>", lw=1, connectionstyle="arc3,rad=-1.4"))
    ax.text(8.6, Y + 1.5, "PIN salah → pesan, PIN dikosongkan\n(Idempotency-Key tetap sama)", fontsize=7.2, ha="center")
    ax.annotate("", xy=(2.2, Y - 0.35), xytext=(11.8, Y - 0.35), arrowprops=dict(arrowstyle="-|>", lw=0.9, ls="--", connectionstyle="arc3,rad=-0.22"))
    ax.text(7.0, 0.15, "Transfer lagi → reset() (kunci idempotensi baru)", fontsize=7.4, ha="center")
    save(fig, "state_transfer.png")


class_core(); route_map(); states()

sequence("seq_web_login.png", ["Pengguna", "LoginPage", "AuthProvider", "api (axios)", "Backend"], [
    (0, 1, "isi identifier + password, klik Masuk"), (1, 2, "login(identifier, password)"),
    (2, 3, "POST /auth/login {identifier, password, deviceId, deviceName}"), (3, 4, "HTTP via proxy /api"),
    (4, 3, "200 {accessToken, refreshToken, user}", True), (2, 2, "setTokens(): access → memori, refresh → sessionStorage"),
    (2, 1, "state = in, user", True), (1, 1, "onboardingStep(user): ekyc / pin / done"), (1, 0, "navigate ke /ekyc, /pin/buat, atau /", True)])

sequence("seq_web_refresh.png", ["Halaman", "api (axios)", "Interceptor 401", "Backend", "AuthProvider"], [
    (0, 1, "api.get('/accounts/me')"), (1, 3, "GET + Bearer accessToken (kedaluwarsa)"), (3, 1, "401", True),
    (1, 2, "interceptor respons"), (2, 3, "POST /auth/refresh {refreshToken} (satu kali untuk semua 401)"),
    (3, 2, "200 token baru (refresh dirotasi)", True), (2, 1, "ulangi permintaan asli (_retried = true)"),
    (1, 0, "200 data", True), (2, 4, "jika refresh gagal: clearTokens() + onSessionEnded()"), (4, 0, "state = out → /login + pesan", True)])

sequence("seq_web_transfer.png", ["Pengguna", "Transfer.jsx", "PinDialog", "api (axios)", "Backend"], [
    (0, 1, "pilih bank, isi rekening & nominal"), (1, 3, "POST /transfers/inquiry"), (3, 1, "nama pemilik rekening", True),
    (1, 3, "POST /transfers/quote"), (3, 1, "biaya + total + saldo cukup?", True), (1, 1, "idemKey = newIdempotencyKey()"),
    (0, 2, "klik Lanjut, ketik PIN 6 digit"), (2, 3, "POST /transfers {.., pin} + Idempotency-Key"),
    (3, 2, "200 TransactionDto / 4xx {message}", True), (2, 1, "sukses: tampilkan bukti (Receipt)", True),
    (1, 0, "bukti transfer, simpan favorit, cek status", True)])

flow("act_guard.png", [
    ("s", "RequireAuth dirender untuk sebuah route"),
    ("d", "state ≠ loading?", "Tampilkan Spinner\n'Memulihkan sesi…'", "loading"),
    ("d", "state = in?", "Navigate /login\n(simpan from)"),
    ("a", "step = onboardingStep(user)"),
    ("d", "Route biasa:\nstep = done?", "Navigate /ekyc atau /pin/buat"),
    ("d", "Route onboarding:\nstep ≠ done?", "Navigate / (onboarding\nsudah selesai)"),
    ("e", "Render <Outlet/> (halaman diminta)"),
])
flow("act_pin_tx.png", [
    ("s", "Pengguna menekan Konfirmasi di PinDialog"),
    ("d", "PIN tepat 6 digit?", "'PIN terdiri atas 6 digit.'"),
    ("a", "busy = true; onSubmit(pin)\n(POST transaksi + Idempotency-Key)"),
    ("d", "Respons 2xx?", "errorMessage(ex) ditampilkan;\nPIN dikosongkan; kunci tetap"),
    ("a", "Tutup dialog; set result"),
    ("e", "Tampilkan bukti transaksi"),
])
print("ok", sorted(os.listdir(OUT)))
