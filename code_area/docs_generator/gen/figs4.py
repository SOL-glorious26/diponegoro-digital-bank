"""Diagrams for SDD-DDB-001-BE (Detailed Design Backend & Database)."""
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
RH = 0.36  # row height


def save(fig, name):
    fig.savefig(os.path.join(OUT, name), dpi=200, bbox_inches="tight", facecolor="white")
    plt.close(fig)


def edge(rect, p):
    """Point where the segment from rect centre to p leaves rect."""
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


# ------------------------------------------------------------------ ERD
def table_box(ax, name, x, ytop, cols, w=4.4, fill=LBLUE):
    h = RH * (len(cols) + 1) + 0.08
    y = ytop - h
    ax.add_patch(Rectangle((x, y), w, h, fc="white", ec=NAVY, lw=1.2, zorder=3))
    ax.add_patch(Rectangle((x, ytop - RH - 0.04), w, RH + 0.04, fc=fill, ec=NAVY, lw=1.2, zorder=3))
    ax.text(x + w / 2, ytop - RH / 2 - 0.02, name, ha="center", va="center", fontsize=8.6, fontweight="bold", zorder=4)
    for i, c in enumerate(cols):
        key, col = (c.split(" ", 1) + [""])[:2] if c.split(" ", 1)[0] in ("PK", "FK", "PK,FK", "U") else ("", c)
        yy = ytop - RH * (i + 1.5) - 0.06
        ax.text(x + 0.12, yy, key, ha="left", va="center", fontsize=6.8, color=RED if "PK" in key else (NAVY if key else "black"), fontweight="bold", zorder=4)
        ax.text(x + 0.85, yy, col, ha="left", va="center", fontsize=7.2, zorder=4, family="DejaVu Sans Mono")
    return (x, y, w, h)


def rel(ax, child, parent, cc="*", pc="1", label=""):
    c, p = center(child), center(parent)
    a, b = edge(child, p), edge(parent, c)
    ax.plot([a[0], b[0]], [a[1], b[1]], color=GREY, lw=1.0, zorder=1)
    def lab(pt, other, t):
        dx, dy = other[0] - pt[0], other[1] - pt[1]
        n = (dx ** 2 + dy ** 2) ** 0.5 or 1
        ax.text(pt[0] + dx / n * 0.28, pt[1] + dy / n * 0.28 + 0.12, t, fontsize=7.5, ha="center", color=NAVY, zorder=5,
                bbox=dict(fc="white", ec="none", pad=0.3))
    lab(a, b, cc); lab(b, a, pc)
    if label:
        ax.text((a[0] + b[0]) / 2, (a[1] + b[1]) / 2, label, fontsize=7, ha="center", va="center", color=GREY, style="italic", zorder=5,
                bbox=dict(fc="white", ec="none", pad=0.4))


def erd1():
    fig, ax = plt.subplots(figsize=(12, 9.6)); ax.set_xlim(0, 20); ax.set_ylim(0, 16); ax.axis("off")
    U = table_box(ax, "users", 7.8, 10.6, ["PK id", "U username", "U email", "U phone_number", "password_hash", "status", "biometric_enabled", "balance_hidden", "photo_url", "created_at, updated_at"], w=4.4, fill=LGOLD)
    O = table_box(ax, "otp_codes", 0.2, 15.8, ["PK id", "FK user_id", "purpose", "code_hash", "target, payload", "attempts", "status", "expires_at"])
    E = table_box(ax, "ekyc_verifications", 0.2, 11.6, ["PK id", "FK user_id (unique)", "nik_masked", "nik_hash", "selfie_reference", "status, reason"])
    T = table_box(ax, "transaction_pins", 0.2, 7.9, ["PK,FK user_id", "pin_hash", "failed_attempts", "blocked_until"])
    S = table_box(ax, "sessions", 15.4, 15.8, ["PK id", "FK user_id", "device_id, device_name", "auth_method", "refresh_token_hash", "status", "last_seen_at", "expires_at"])
    B = table_box(ax, "biometric_devices", 15.4, 11.6, ["PK id", "FK user_id", "device_id", "U token_hash", "revoked"])
    NS = table_box(ax, "notification_settings", 15.4, 8.3, ["PK,FK user_id", "types", "channels"])
    N = table_box(ax, "notifications", 4.6, 4.2, ["PK id", "FK user_id", "type, title, message", "FK tx_id", "is_read, pushed"])
    EM = table_box(ax, "email_outbox", 10.6, 4.2, ["PK id", "FK user_id", "to_address", "subject, body"])
    A = table_box(ax, "audit_events", 15.4, 4.6, ["PK id", "user_id (tanpa FK)", "action, detail", "ip_address"], fill=LGREY)
    rel(ax, O, U); rel(ax, E, U, "0..1"); rel(ax, T, U, "0..1"); rel(ax, S, U); rel(ax, B, U); rel(ax, NS, U, "0..1"); rel(ax, N, U); rel(ax, EM, U)
    ax.text(4.6 + 2.2, 0.9, "notifications.tx_id → ledger_transactions (Gambar 3.2)", fontsize=7.5, ha="center", color=GREY, style="italic")
    save(fig, "erd_identity.png")


def erd2():
    fig, ax = plt.subplots(figsize=(12, 9.6)); ax.set_xlim(0, 20); ax.set_ylim(0, 16); ax.axis("off")
    U = table_box(ax, "users", 7.8, 15.8, ["PK id", "…"], w=4.4, fill=LGOLD)
    LA = table_box(ax, "ledger_accounts", 0.4, 12.4, ["PK id", "FK owner_user_id (null = EXTERNAL)", "kind: MAIN|POCKET|EXTERNAL", "U number", "name", "balance ≥ 0 (CHECK)", "closed"], w=5.2, fill=LGREEN)
    LT = table_box(ax, "ledger_transactions", 14.2, 12.4, ["PK id", "U reference", "U idempotency_key", "category, status", "amount > 0, fee ≥ 0", "FK debit_user_id", "FK credit_user_id", "from/to name+detail", "note, extra (JSON)"], w=5.4, fill=LGREEN)
    J = table_box(ax, "journal_lines", 7.6, 8.0, ["PK id (identity)", "FK tx_id", "FK account_id", "amount ≠ 0", "memo, posted_at"], w=4.6, fill=LGREEN)
    P = table_box(ax, "pockets", 0.4, 5.6, ["PK,FK account_id", "FK user_id", "purpose, target", "lock_type: NONE|DATE|TARGET", "lock_until"], w=5.2)
    F = table_box(ax, "favorite_recipients", 7.6, 4.0, ["PK id", "FK user_id", "bank_code, account_number", "name", "U (user, bank, account)"], w=4.6)
    PB = table_box(ax, "paid_bills", 14.2, 5.6, ["PK bill_key", "FK tx_id", "paid_at"], w=5.4)
    V = table_box(ax, "virtual_cards", 14.2, 3.4, ["PK id, FK user_id", "card_number_enc, cvv_enc", "last4, expiry", "status, usage_scope", "transaction_limit > 0"], w=5.4)
    rel(ax, J, LT, "1..*", "1"); rel(ax, J, LA, "*", "1"); rel(ax, P, LA, "0..1", "1"); rel(ax, LA, U, "*", "0..1")
    rel(ax, LT, U, "*", "0..1", "debit/credit"); rel(ax, PB, LT, "0..1", "1");
    ax.text(10, 1.05, "pockets, favorite_recipients, dan virtual_cards juga memiliki FK user_id → users (garis tidak digambar agar terbaca)", fontsize=7.5, ha="center", color=GREY, style="italic")
    ax.text(10, 0.5, "Σ journal_lines.amount per transaksi = 0  ·  ledger_accounts.balance = Σ journal_lines.amount per akun", fontsize=8, ha="center", style="italic")
    save(fig, "erd_ledger.png")


# ------------------------------------------------------------------ Class diagrams
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


def link(ax, a, b, kind="dep", la="", lb="", label=""):
    """kind: dep (dashed open arrow a->b), assoc (solid), impl (dashed hollow triangle a->b), comp (solid diamond at b)."""
    pa, pb = edge(a, center(b)), edge(b, center(a))
    ls = "--" if kind in ("dep", "impl") else "-"
    if kind == "impl":
        ax.annotate("", xy=pb, xytext=pa, arrowprops=dict(arrowstyle="-|>,head_width=0.45,head_length=0.8", ls=ls, lw=1, color="black", fc="white"), zorder=2)
    elif kind == "dep":
        ax.annotate("", xy=pb, xytext=pa, arrowprops=dict(arrowstyle="->", ls=ls, lw=1, color=GREY), zorder=2)
    else:
        ax.plot([pa[0], pb[0]], [pa[1], pb[1]], color="black", lw=1, zorder=2)
        if kind == "comp":
            ax.plot(*pb, marker="D", ms=6, color="black", zorder=5)
    if label:
        ax.text((pa[0] + pb[0]) / 2, (pa[1] + pb[1]) / 2, label, fontsize=7, ha="center", va="center", style="italic", color=GREY, zorder=5, bbox=dict(fc="white", ec="none", pad=0.3))
    for pt, other, t in ((pa, pb, la), (pb, pa, lb)):
        if t:
            dx, dy = other[0] - pt[0], other[1] - pt[1]; n = (dx ** 2 + dy ** 2) ** 0.5 or 1
            ax.text(pt[0] + dx / n * 0.35 + 0.15, pt[1] + dy / n * 0.35, t, fontsize=7.2, color=NAVY, zorder=5)


def class_ledger():
    fig, ax = plt.subplots(figsize=(12, 8.4)); ax.set_xlim(0, 20); ax.set_ylim(0, 14); ax.axis("off")
    LS = cls(ax, 6.6, 13.8, 7.0, "LedgerService", ["- accounts, transactions, journal", "- events: ApplicationEventPublisher"],
             ["+ post(r: PostingRequest): LedgerTransaction", "+ complete(txId): LedgerTransaction", "+ failWithRefund(txId, reason)", "+ requireMainAccount(userId)", "+ openMainAccount(…), openPocket(…)", "+ findByIdempotencyKey(key)", "- lock(ids): List<LedgerAccount>"], "service")
    PR = cls(ax, 0.2, 13.8, 5.6, "PostingRequest", ["idempotencyKey, category", "amount, fee, status", "lines: List<Line>", "debitUserId, creditUserId", "from/to, note, extra"], ["+ builder(…)"], "record")
    LA = cls(ax, 0.2, 7.0, 5.4, "LedgerAccount", ["id, ownerUserId", "kind: AccountKind", "number, name", "balance: long", "closed: boolean"], [""], "entity", LGREEN)
    LT = cls(ax, 14.0, 7.0, 5.8, "LedgerTransaction", ["id, reference", "idempotencyKey", "category: TxCategory", "status: TxStatus", "amount, fee", "debitUserId, creditUserId"], [""], "entity", LGREEN)
    JL = cls(ax, 7.2, 6.4, 5.4, "JournalLine", ["id: long", "txId, accountId", "amount: long (≠ 0)", "memo, postedAt"], [""], "entity", LGREEN)
    EV = cls(ax, 14.4, 13.8, 5.4, "TransactionPostedEvent", ["txId"], [""], "event", LGREY)
    EN = cls(ax, 7.2, 2.4, 12.6, "Enumerasi", ["AccountKind {MAIN, POCKET, EXTERNAL}   TxStatus {SUCCESS, PROCESSING, FAILED}", "TxCategory {DEPOSIT, TRANSFER, QRIS, TOPUP, POCKET, CARD}"], [""], "enumeration", LGREY)
    link(ax, LS, PR, "dep", label="memakai"); link(ax, LS, EV, "dep", label="menerbitkan")
    link(ax, LS, LA, "dep", label="mengunci"); link(ax, LS, LT, "dep", label="membuat"); link(ax, LS, JL, "dep", label="menulis")
    link(ax, JL, LT, "assoc", "1..*", "1"); link(ax, JL, LA, "assoc", "*", "1")
    save(fig, "cls_ledger.png")


def class_auth():
    fig, ax = plt.subplots(figsize=(12, 9.6)); ax.set_xlim(0, 20); ax.set_ylim(0, 16); ax.axis("off")
    AC = cls(ax, 6.8, 15.8, 6.4, "AuthController", [""], ["/api/v1/auth/** (18 operasi)"], "RestController", LGREY)
    AS = cls(ax, 6.0, 13.6, 8.0, "AuthService", ["- users, sessions, ekyc, devices", "- jwt: JwtService, throttle: LoginThrottle"],
             ["+ register(…), verifyRegistration(…)", "+ login(…), biometricLogin(…), refresh(…)", "+ submitEkyc(…), ekycStatus(…)", "+ createPin(…), logout(…), logoutAll(…)", "+ startRecovery(…), verifyRecovery(…)", "+ resetCredential(…)"], "service")
    OS = cls(ax, 0.2, 10.4, 5.4, "OtpService", ["TTL = 2 menit", "MAX_ATTEMPTS = 5"], ["+ issue(userId, purpose, target)", "+ verify(userId, purpose, code)", "+ consumeVerified(…)"], "service")
    PS = cls(ax, 14.4, 10.4, 5.4, "PinService", ["BLOCK = 5 menit", "MAX_ATTEMPTS = 3"], ["+ create(userId, pin)", "+ verify(userId, pin)", "+ reset(userId, newPin)"], "service")
    TG = cls(ax, 14.4, 6.0, 5.4, "TransactionGuard", [""], ["+ requireFinancial(userId): User", "+ authorize(userId, pin): User"], "service")
    JS = cls(ax, 0.2, 6.0, 5.4, "JwtService", ["HS256; access 15 mnt", "refresh 7 hari"], ["+ issue(user, sessionId)"], "component", LGREY)
    SC = cls(ax, 0.2, 3.0, 5.4, "SessionJwtConverter", ["IDLE_TIMEOUT = 5 menit"], ["+ convert(jwt): AuthUser.Token"], "component", LGREY)
    U = cls(ax, 6.6, 7.6, 6.8, "User", ["id, username, email, phoneNumber", "passwordHash", "status: AccountStatus", "biometricEnabled, balanceHidden"], ["+ financialEnabled(): boolean"], "entity", LGREEN)
    US = cls(ax, 6.6, 3.0, 6.8, "UserSession", ["id, userId, deviceId", "authMethod, refreshTokenHash", "status, lastSeenAt, expiresAt"], [""], "entity", LGREEN)
    link(ax, AC, AS, "dep"); link(ax, AS, OS, "dep"); link(ax, AS, PS, "dep"); link(ax, AS, JS, "dep"); link(ax, AS, U, "dep")
    link(ax, TG, PS, "dep", label="verify"); link(ax, TG, U, "dep"); link(ax, SC, US, "dep", label="validasi sesi"); link(ax, US, U, "assoc", "*", "1")
    save(fig, "cls_auth.png")


def class_tx():
    fig, ax = plt.subplots(figsize=(12, 6.6)); ax.set_xlim(0, 20); ax.set_ylim(0, 11); ax.axis("off")
    TS = cls(ax, 0.2, 10.4, 4.6, "TransferService", ["MIN 5.000; MAX 25 jt", "INTERBANK_FEE 2.500"], ["+ inquiry(…), quote(…)", "+ execute(…, pin, idemKey)", "+ checkStatus(userId, txId)", "+ add/removeFavorite(…)"], "service")
    PS = cls(ax, 5.1, 10.4, 4.7, "PaymentService", ["MIN 1.000; MAX 10 jt"], ["+ scan(…), pay(…)", "+ personalQr(…)", "+ simulateIncoming(…)"], "service")
    TU = cls(ax, 10.1, 10.4, 4.7, "TopUpService", [""], ["+ quote(…), order(…)", "+ inquirePln(…)", "+ inquireBill(…)", "+ dataPackages(phone)"], "service")
    VC = cls(ax, 15.1, 10.4, 4.7, "VirtualCardService", ["MAX_CARDS 5"], ["+ create, details", "+ freeze, activate", "+ setLimit", "+ simulatePurchase"], "service")
    gy = min(r[1] for r in (TS, PS, TU, VC)) - 0.25
    ax.add_patch(Rectangle((0.05, gy), 19.9, 10.75 - gy, fc="none", ec=GREY, ls="--", lw=1, zorder=1))
    ax.text(0.2, 10.55, "Service transaksi keuangan", fontsize=8, style="italic", color=GREY)
    TG = cls(ax, 2.6, 3.4, 6.2, "TransactionGuard", [""], ["+ requireFinancial(userId)", "+ authorize(userId, pin)"], "service")
    LS = cls(ax, 11.2, 3.4, 6.2, "LedgerService", [""], ["+ post(r: PostingRequest)", "+ complete / failWithRefund"], "service", LGREEN)
    for tgt in (TG, LS):
        cx = tgt[0] + tgt[2] / 2
        ax.annotate("", xy=(cx, tgt[1] + tgt[3]), xytext=(cx, gy), arrowprops=dict(arrowstyle="->", ls="--", lw=1.1, color=GREY))
    ax.text(5.9, gy - 0.55, "e-KYC + PIN sebelum transaksi", fontsize=7.5, ha="left", style="italic", color=GREY)
    ax.text(14.5, gy - 0.55, "posting atomik + idempotensi", fontsize=7.5, ha="left", style="italic", color=GREY)
    save(fig, "cls_tx.png")


def class_adapters():
    fig, ax = plt.subplots(figsize=(12, 6.0)); ax.set_xlim(0, 20); ax.set_ylim(0, 10); ax.axis("off")
    TS = cls(ax, 0.2, 9.8, 5.6, "TransferService", [""], ["+ execute(…)", "+ checkStatus(…)"], "service")
    IG = cls(ax, 0.2, 6.6, 5.6, "InterbankGateway", [""], ["+ banks(), bank(code)", "+ inquiry(bank, number)", "+ send(…): Outcome", "+ status(reference): Outcome"], "interface", LGOLD)
    IS = cls(ax, 0.2, 2.4, 5.6, "InterbankSimulator", ["9999999999 → REJECTED", "8888888888 → TIMEOUT"], [""], "component", LGOLD)
    PS = cls(ax, 7.2, 9.8, 5.6, "PaymentService", [""], ["+ scan(…), pay(…)"], "service")
    QC = cls(ax, 7.2, 6.6, 5.6, "QrisCodec", ["EMVCo TLV", "CRC16-CCITT 0x1021"], ["+ encode(p), decode(raw)", "+ crc16(data)"], "utility", LGREY)
    VC = cls(ax, 14.2, 9.8, 5.6, "VirtualCardService", [""], ["+ create(…), details(…)"], "service")
    DC = cls(ax, 14.2, 6.6, 5.6, "DataCipher", ["AES-GCM, kunci DATA_KEY"], ["+ encrypt(text)", "+ decrypt(cipher)"], "component", LGREY)
    link(ax, TS, IG, "dep"); link(ax, IS, IG, "impl"); link(ax, PS, QC, "dep"); link(ax, VC, DC, "dep")
    save(fig, "cls_adapters.png")



def class_support():
    fig, ax = plt.subplots(figsize=(12, 6.0)); ax.set_xlim(0, 20); ax.set_ylim(3.0, 12.4); ax.axis("off")
    AS = cls(ax, 0.2, 12.2, 6.2, "AccountService", ["MAX_POCKETS 10; MIN_TARGET 10.000", "- pockets: repo entitas Pocket"], ["+ overview, balanceChart, setBalanceHidden", "+ create, update, delete, lock", "+ deposit, withdraw(…, early, pin)", "~ isLocked(pocket, balance)"], "service")
    HS = cls(ax, 7.0, 12.2, 6.0, "HistoryService", [""], ["+ page(userId, filter)", "+ detail(userId, txId)", "+ monthSeries(userId)"], "service")
    HE = cls(ax, 7.8, 8.4, 5.2, "HistoryExporter", ["CSV; PDF (OpenPDF)"], ["+ export(filter, format)"], "component", LGREY)
    PF = cls(ax, 13.6, 12.2, 6.2, "ProfileService", [""], ["+ me, requestChange, confirmChange", "+ changePassword, changePin", "+ updatePhoto"], "service")
    NS = cls(ax, 13.6, 5.6, 6.2, "NotificationService", ["@Async listener"], ["+ onPosted(event), onStatusChanged(event)", "+ list, unreadCount, markRead", "+ takePendingPush, settings"], "service")
    LS = cls(ax, 0.2, 5.6, 6.2, "LedgerService", [""], ["+ post(r)", "+ balanceAt(accountId, t)"], "service", LGREEN)
    OS = cls(ax, 13.6, 8.6, 6.2, "OtpService", [""], ["+ issue / verify"], "service")
    link(ax, AS, LS, "dep"); link(ax, HS, LS, "dep"); link(ax, HS, HE, "dep"); link(ax, PF, OS, "dep")
    link(ax, LS, NS, "dep", label="TransactionPostedEvent")
    save(fig, "cls_support.png")


# ------------------------------------------------------------------ Sequence diagrams
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


erd1(); erd2(); class_ledger(); class_auth(); class_tx(); class_adapters(); class_support()

sequence("seq_login.png", ["Klien", "AuthController", "AuthService", "LoginThrottle", "JwtService", "PostgreSQL"], [
    (0, 1, "POST /auth/login {identifier, password, deviceId}"), (1, 2, "login(…)"), (2, 3, "cek kunci (5× gagal → 15 mnt)"),
    (2, 5, "SELECT users; BCrypt.matches"), (2, 5, "INSERT sessions (refresh_token_hash)"), (2, 4, "issue(user, sid)"),
    (4, 2, "accessToken (15 mnt) + refreshToken (7 hari)", True), (1, 0, "200 {accessToken, refreshToken, user}", True)])
sequence("seq_auth_request.png", ["Klien", "Gateway", "Spring Security", "SessionJwtConverter", "Controller", "PostgreSQL"], [
    (0, 1, "GET /api/v1/… + Bearer JWT"), (1, 2, "rate limit OK → teruskan"), (2, 2, "verifikasi tanda tangan HS256 + exp"),
    (2, 3, "convert(jwt)"), (3, 5, "SELECT sessions WHERE id = sid"), (3, 3, "ACTIVE? tidak idle > 5 mnt? update last_seen"),
    (3, 2, "AuthUser / 401 Sesi berakhir", True), (2, 4, "panggil handler (AuthUser)"), (4, 0, "200 JSON / error {code, message}", True)])
sequence("seq_qris.png", ["Klien Android", "PaymentController", "PaymentService", "QrisCodec", "TransactionGuard", "LedgerService"], [
    (0, 1, "POST /qris/scan {payload}"), (1, 2, "scan(userId, payload)"), (2, 3, "decode + cek CRC16"), (2, 0, "merchant + nominal", True),
    (0, 1, "POST /qris/pay {payload, amount, pin} + Idempotency-Key"), (2, 2, "requireRange 1.000–10.000.000; cek saldo"), (2, 4, "authorize(userId, pin)"),
    (2, 5, "post(MAIN −x, merchant atau rekening DDB +x)"), (5, 5, "event → notifikasi @Async"), (1, 0, "200 TransactionDto (SUCCESS)", True)])

flow("act_post.png", [
    ("s", "LedgerService.post(PostingRequest)"),
    ("d", "Σ amount baris = 0\n& amount > 0?", "IllegalArgument /\nINVALID_AMOUNT"),
    ("a", "Kunci rekening terlibat urut id\n(SELECT … FOR UPDATE + refresh)"),
    ("d", "Idempotency-Key\nbelum pernah dipakai?", "Kembalikan transaksi lama\n(tanpa debit ganda)"),
    ("d", "Semua rekening terbuka\n& saldo cukup?", "ACCOUNT_CLOSED /\nINSUFFICIENT_BALANCE → rollback"),
    ("a", "INSERT ledger_transactions + journal_lines"),
    ("a", "Perbarui cache balance; flush"),
    ("a", "Terbitkan TransactionPostedEvent"),
    ("e", "Kembalikan LedgerTransaction"),
])
flow("act_pin.png", [
    ("s", "PinService.verify(userId, pin)"),
    ("d", "Format 6 digit?", "PIN_FORMAT (400)"),
    ("a", "Kunci baris transaction_pins (REQUIRES_NEW)"),
    ("d", "Tidak sedang diblokir?", "PIN_BLOCKED (423):\nsisa menit"),
    ("d", "BCrypt cocok?", "failed_attempts++; ≥ 3 →\nblokir 5 mnt + audit; PIN_INVALID"),
    ("a", "Reset failed_attempts = 0"),
    ("e", "Verifikasi lulus"),
])
# State diagram: status transaksi ledger
fig, ax = plt.subplots(figsize=(9, 3.8)); ax.set_xlim(0, 12); ax.set_ylim(0, 5); ax.axis("off")
def stb(x, y, t, fc):
    ax.add_patch(FancyBboxPatch((x - 1.3, y - 0.35), 2.6, 0.7, boxstyle="round,pad=0.02,rounding_size=0.25", fc=fc, ec=NAVY, zorder=3))
    ax.text(x, y, t, ha="center", va="center", fontsize=9.5, fontweight="bold", zorder=4)
def ar(a, b, t=None, tp=None, **kw):
    ax.annotate("", xy=b, xytext=a, arrowprops=dict(arrowstyle="-|>", lw=1.1, **kw))
    if t: ax.text(tp[0], tp[1], t, fontsize=8, ha="center", va="center")
ax.plot(0.6, 2.6, "o", ms=10, color="black")
stb(4.6, 1.3, "PROCESSING", LGOLD); stb(10.2, 4.0, "SUCCESS", LGREEN); stb(10.2, 1.3, "FAILED", LRED)
ar((0.75, 2.75), (8.9, 4.0), "post: sesama bank, QRIS, top-up, kantong, kartu", (4.4, 3.75))
ar((0.75, 2.45), (3.3, 1.45), "post: antarbank", (1.6, 1.75))
ar((5.9, 1.55), (8.9, 3.8), "ACCEPTED → complete()", (7.0, 2.95))
ar((5.9, 1.3), (8.9, 1.3), "REJECTED → failWithRefund()", (7.4, 1.05))
ax.annotate("", xy=(4.2, 0.95), xytext=(5.0, 0.95), arrowprops=dict(arrowstyle="-|>", connectionstyle="arc3,rad=-1.2", lw=1))
ax.text(4.6, 0.05, "TIMEOUT → tetap diproses; checkStatus() ulang", fontsize=8, ha="center")
ax.text(10.2, 0.6, "dana dikembalikan dengan jurnal pembalik", fontsize=7.5, ha="center", style="italic", color=GREY)
save(fig, "state_tx.png")
print("ok", sorted(os.listdir(OUT)))
