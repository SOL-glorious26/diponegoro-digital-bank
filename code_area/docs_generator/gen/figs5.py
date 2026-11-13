"""Diagram untuk DVM-DDB-001-AND dan tutorial uji di ponsel."""
import sys, os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch
from matplotlib import font_manager

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)
for f in ["pala.ttf", "palab.ttf", "palai.ttf"]:
    p = os.path.join(r"C:\Windows\Fonts", f)
    if os.path.exists(p):
        font_manager.fontManager.addfont(p)
plt.rcParams["font.family"] = ["Palatino Linotype", "DejaVu Sans"]
NAVY, LBLUE, LGREY, GREY, GREEN, LGREEN, GOLD, LGOLD = "#1F3864", "#DCE6F2", "#F2F2F2", "#7F7F7F", "#375623", "#E2EFDA", "#BF9000", "#FFF2CC"


def box(ax, x, y, w, h, text, fc=LBLUE, ec=NAVY, fs=9, bold=False, ls="-", tc="black"):
    ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.02,rounding_size=0.08", fc=fc, ec=ec, lw=1.2, ls=ls))
    ax.text(x + w / 2, y + h / 2, text, ha="center", va="center", fontsize=fs, fontweight="bold" if bold else "normal", color=tc)


def arrow(ax, x1, y1, x2, y2, text="", fs=8, color="black", ls="-", dx=0.0, dy=0.12):
    ax.annotate("", xy=(x2, y2), xytext=(x1, y1), arrowprops=dict(arrowstyle="-|>", color=color, lw=1.1, ls=ls))
    if text:
        ax.text((x1 + x2) / 2 + dx, (y1 + y2) / 2 + dy, text, fontsize=fs, ha="center", va="center",
                bbox=dict(fc="white", ec="none", pad=0.5))


def save(fig, name):
    fig.savefig(os.path.join(OUT, name), dpi=200, bbox_inches="tight", facecolor="white")
    plt.close(fig)


# 1) Wiring: layar -> ViewModel -> FeatureModule -> Local/Remote -> Ledger / ApiClient
fig, ax = plt.subplots(figsize=(11, 6.2)); ax.set_xlim(0, 11); ax.set_ylim(0, 6.2); ax.axis("off")
box(ax, 3.6, 5.3, 3.8, 0.7, "Layar Compose (mis. TransferScreen)", fs=9.5)
box(ax, 3.6, 4.2, 3.8, 0.7, "ViewModel (TransferViewModel)\nStateFlow<UiState>", fs=9)
box(ax, 3.6, 3.1, 3.8, 0.7, "FeatureModule / AuthModule / VirtualCardModule\n(DI manual, lazy)", fc=LGREY, ec=GREY, fs=8.6)
box(ax, 0.3, 1.7, 4.4, 0.8, "Local*Repository / Mock*Repository\n(varian demo)", fc=LGOLD, ec=GOLD, fs=9)
box(ax, 6.3, 1.7, 4.4, 0.8, "Remote*Repository\n(varian online)", fc=LGREEN, ec=GREEN, fs=9)
box(ax, 0.3, 0.2, 4.4, 0.9, "core/ledger (Ledger in-memory, DemoData)\nTransactionGuard, NotificationCenter", fc=LGREY, ec=GREY, fs=8.5)
box(ax, 6.3, 0.2, 4.4, 0.9, "core/network: ApiClient (Retrofit + OkHttp)\nTokenStore, Mappers → backend /api/v1", fc=LGREY, ec=GREY, fs=8.5)
arrow(ax, 5.5, 5.3, 5.5, 4.9, "aksi pengguna / state", dx=1.3, dy=0)
arrow(ax, 5.5, 4.2, 5.5, 3.8, "factory(userId)", dx=1.0, dy=0)
arrow(ax, 4.5, 3.1, 2.5, 2.5, "BuildConfig.ONLINE = false", dx=-0.9, dy=0.15)
arrow(ax, 6.5, 3.1, 8.5, 2.5, "BuildConfig.ONLINE = true", dx=0.9, dy=0.15)
arrow(ax, 2.5, 1.7, 2.5, 1.1)
arrow(ax, 8.5, 1.7, 8.5, 1.1)
ax.text(5.5, 2.1, "interface\nTransferRepository\n(dsb.)", ha="center", va="center", fontsize=8, style="italic", color=GREY)
save(fig, "and_wiring.png")

# 2) Sequence panggilan REST dengan refresh token
parts = ["ViewModel", "Remote*Repository", "ApiClient\n(Interceptor)", "Authenticator", "Backend"]
fig, ax = plt.subplots(figsize=(11, 5.2)); W, H = 11, 8.4; ax.set_xlim(0, W); ax.set_ylim(0, H); ax.axis("off")
xs = [1.1 + i * 2.2 for i in range(len(parts))]
for x, p in zip(xs, parts):
    box(ax, x - 0.95, H - 1.0, 1.9, 0.8, p, fs=8.5)
    ax.plot([x, x], [0.2, H - 1.0], color=GREY, lw=0.8, ls="--")
msgs = [(0, 1, "execute(..., idemKey) di coroutine"), (1, 2, "client.call { transfer(key, body) }"),
        (2, 4, "POST /api/v1/transfers + Bearer access token"), (4, 2, "401 (token kedaluwarsa)", True),
        (2, 3, "authenticate()"), (3, 4, "POST /auth/refresh {refreshToken}"), (4, 3, "AuthTokens baru (refresh dirotasi)", True),
        (3, 2, "simpan token; ulangi dengan token baru"), (2, 4, "POST /api/v1/transfers (ulang, Idempotency-Key sama)"),
        (4, 1, "200 TransactionDto", True), (1, 0, "AppResult.Success(LedgerTransaction); dataChanged()", True)]
y = H - 1.45
for m in msgs:
    a, b, t = m[0], m[1], m[2]
    ret = len(m) > 3
    ax.annotate("", xy=(xs[b], y), xytext=(xs[a], y), arrowprops=dict(arrowstyle="-|>", lw=1, ls="--" if ret else "-", color=GREY if ret else "black"))
    ax.text((xs[a] + xs[b]) / 2, y + 0.16, t, ha="center", fontsize=7.6)
    y -= 0.62
ax.text(W / 2, 0.05, "Bila refresh ditolak: TokenStore dikosongkan, CoreModule.sessionExpired → AuthViewModel kembali ke layar login",
        ha="center", fontsize=7.8, style="italic", color=GREY)
save(fig, "and_sequence.png")

# 3) Topologi uji di ponsel
fig, ax = plt.subplots(figsize=(10, 4.6)); ax.set_xlim(0, 10); ax.set_ylim(0, 4.6); ax.axis("off")
box(ax, 0.2, 2.6, 2.4, 1.4, "Ponsel Android\nAPK varian online\nbaseUrl = http://<IP-laptop>:8080/", fc="white", fs=8.6)
box(ax, 3.7, 2.75, 2.3, 1.1, "Router / hotspot Wi-Fi\n(jaringan yang sama)", fc=LGREY, ec=GREY, fs=8.6)
box(ax, 7.1, 2.4, 2.7, 1.8, "Laptop Windows\nbackend (run-local.ps1 -H2)\nport 8080\nFirewall: izinkan TCP 8080", fc=LGREEN, ec=GREEN, fs=8.6)
arrow(ax, 2.6, 3.3, 3.7, 3.3, "Wi-Fi", dy=0.18)
arrow(ax, 6.0, 3.3, 7.1, 3.3, "Wi-Fi / LAN", dy=0.18)
box(ax, 0.2, 0.3, 2.4, 1.2, "Ponsel (jaringan apa pun)\nbaseUrl = https://xxxx.ngrok-free.app/", fc="white", fs=8.4, ls="--")
box(ax, 3.7, 0.4, 2.3, 1.0, "Internet\n(ngrok, HTTPS)", fc=LGOLD, ec=GOLD, fs=8.6, ls="--")
arrow(ax, 2.6, 0.9, 3.7, 0.9, ls="--", color=GREY)
arrow(ax, 6.0, 0.9, 7.6, 2.4, "ngrok http 8080", ls="--", color=GREY, dx=0.3, dy=0.0)
ax.text(5, 4.45, "Opsi A (disarankan): satu jaringan Wi-Fi", ha="center", fontsize=9.5, fontweight="bold")
ax.text(1.4, 1.75, "Opsi B: lewat ngrok", ha="center", fontsize=9.5, fontweight="bold")
save(fig, "phone_topology.png")
print("ok", sorted(os.listdir(OUT)))
