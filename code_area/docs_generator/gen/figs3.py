"""Workflow (activity-style) diagrams and state diagram for FSD-DDB-001."""
import sys, os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, Polygon, Circle
from matplotlib import font_manager

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)
for f in ["pala.ttf", "palab.ttf", "palai.ttf"]:
    p = os.path.join(r"C:\Windows\Fonts", f)
    if os.path.exists(p):
        font_manager.fontManager.addfont(p)
plt.rcParams["font.family"] = ["Palatino Linotype", "DejaVu Sans"]
NAVY, LBLUE, LGREY, GREY, RED, LRED, GREEN, LGREEN = "#1F3864", "#DCE6F2", "#F2F2F2", "#7F7F7F", "#C00000", "#FCE4D6", "#375623", "#E2EFDA"


def arrow(ax, x1, y1, x2, y2, label="", color="black", lx=0.12, ly=0.0):
    ax.annotate("", xy=(x2, y2), xytext=(x1, y1), arrowprops=dict(arrowstyle="-|>", lw=1.0, color=color))
    if label:
        ax.text((x1 + x2) / 2 + lx, (y1 + y2) / 2 + ly, label, fontsize=8, color=color, va="center")


def flow(name, steps, title=None):
    """steps: ('s'|'e', text) | ('a', text, [actor]) | ('d', question, no_text) ; 'yes' continues down."""
    H = 0.62
    n = len(steps)
    height = n * 1.0 + 0.4
    fig, ax = plt.subplots(figsize=(8.6, height * 0.62 + 0.3))
    ax.set_xlim(0, 10); ax.set_ylim(0, height); ax.axis("off")
    xc, w = 3.4, 4.2
    y = height - 0.6
    prev = None
    for st in steps:
        k = st[0]
        if k in ("s", "e"):
            ax.add_patch(FancyBboxPatch((xc - 2.1, y - 0.25), 4.2, 0.5, boxstyle="round,pad=0.02,rounding_size=0.25",
                                        fc=NAVY, ec=NAVY))
            ax.text(xc, y, st[1], ha="center", va="center", fontsize=8.8, color="white", fontweight="bold")
            top, bot = y + 0.25, y - 0.25
        elif k == "a":
            fc = GREEN if (len(st) > 2 and st[2] == "sys") else LBLUE
            ax.add_patch(FancyBboxPatch((xc - w / 2, y - H / 2), w, H, boxstyle="round,pad=0.02,rounding_size=0.08",
                                        fc=LGREEN if fc == GREEN else LBLUE, ec=NAVY, lw=1))
            ax.text(xc, y, st[1], ha="center", va="center", fontsize=8.5)
            top, bot = y + H / 2, y - H / 2
        elif k == "d":
            dw, dh = 3.3, 0.86
            ax.add_patch(Polygon([(xc, y + dh / 2), (xc + dw / 2, y), (xc, y - dh / 2), (xc - dw / 2, y)], closed=True,
                                 fc="#FFF2CC", ec="#BF9000", lw=1))
            ax.text(xc, y, st[1], ha="center", va="center", fontsize=7.6)
            top, bot = y + dh / 2, y - dh / 2
            # "no" branch
            ax.add_patch(FancyBboxPatch((6.6, y - 0.3), 3.3, 0.6, boxstyle="round,pad=0.02,rounding_size=0.08", fc=LRED, ec=RED, lw=1))
            ax.text(8.25, y, st[2], ha="center", va="center", fontsize=7.4)
            arrow(ax, xc + dw / 2, y, 6.6, y, "tidak", color=RED, lx=-0.35, ly=0.13)
        if prev is not None:
            arrow(ax, xc, prev, xc, top, "ya" if last_kind == "d" else "", lx=0.1)
        prev, last_kind = bot, k
        y -= 1.0
    fig.savefig(os.path.join(OUT, name), dpi=200, bbox_inches="tight", facecolor="white")
    plt.close(fig)


flow("w_register.png", [
    ("s", "Mulai: pengguna memilih Daftar"),
    ("a", "Isi username, password, email/telepon"),
    ("d", "Data valid & unik?", "Pesan validasi, mis.\nINVALID_USERNAME, WEAK_PASSWORD"),
    ("a", "Sistem kirim OTP (berlaku 2 menit)", "sys"),
    ("d", "OTP benar & belum\nkedaluwarsa?", "OTP_INVALID / OTP_EXPIRED;\nkirim ulang OTP (maks. 5× salah)"),
    ("a", "Akun ACTIVE; sesi dibuat", "sys"),
    ("a", "Kirim e-KYC: NIK 16 digit + swafoto"),
    ("d", "e-KYC disetujui?\n(diproses ±3 detik)", "KYC_REJECTED: fitur transaksi\ntetap terkunci"),
    ("a", "Buat PIN transaksi 6 digit"),
    ("e", "Selesai: akun FINANCIAL_ENABLED"),
])
flow("w_login.png", [
    ("s", "Mulai: layar Login"),
    ("d", "Biometrik aktif di\nperangkat (mobile)?", "Login dengan username/email/\ntelepon + password"),
    ("a", "Verifikasi sidik jari/wajah → token perangkat"),
    ("d", "Kredensial valid?", "Pesan umum INVALID_CREDENTIALS;\n5× salah → kunci 15 menit"),
    ("a", "Terbitkan access token 15 menit +\nrefresh token 7 hari; catat sesi", "sys"),
    ("a", "Masuk Beranda; pantau tidak aktif"),
    ("d", "Aktif dalam 5 menit\nterakhir?", "Sesi berakhir → kembali ke Login"),
    ("e", "Sesi berjalan (logout / logout semua)"),
])
flow("w_transfer.png", [
    ("s", "Mulai: Beranda → Transfer"),
    ("a", "Pilih sesama bank / antarbank / favorit"),
    ("a", "Isi bank + nomor rekening → inquiry"),
    ("d", "Rekening valid?", "A3: peringatan rekening\ntidak valid"),
    ("a", "Tampilkan nama penerima; isi nominal + catatan"),
    ("d", "Rp5.000 ≤ nominal ≤ limit\n& saldo cukup?", "A1 saldo kurang / A2 di luar\nbatas: ditolak sebelum PIN"),
    ("a", "Layar konfirmasi: penerima, nominal, biaya, total"),
    ("d", "PIN benar?", "A4: sisa percobaan;\n3× salah → PIN diblokir 5 menit"),
    ("a", "Posting ledger atomik + Idempotency-Key", "sys"),
    ("d", "Antarbank berhasil?", "A5 timeout → Diproses (cek ulang)\nA6 ditolak → refund otomatis"),
    ("e", "Bukti transfer + notifikasi"),
])
flow("w_qris.png", [
    ("s", "Mulai: Bayar QRIS (Android)"),
    ("a", "Pindai kode (kamera/galeri)"),
    ("d", "Kode QRIS valid\n(EMVCo, CRC16)?", "INVALID_QRIS: kode\ntidak dapat dibaca"),
    ("a", "Tampilkan merchant + nominal\n(dinamis: nominal terkunci)"),
    ("d", "Rp1.000 ≤ nominal ≤\nRp10 juta & saldo cukup?", "AMOUNT_TOO_SMALL/LARGE,\nINSUFFICIENT_BALANCE"),
    ("d", "PIN benar?", "PIN_INVALID / PIN_BLOCKED"),
    ("a", "Debit atomik + Idempotency-Key", "sys"),
    ("e", "Status berhasil + notifikasi"),
])
flow("w_topup.png", [
    ("s", "Mulai: pilih Pulsa/Data/PLN/Tagihan"),
    ("a", "Isi nomor HP / ID pelanggan"),
    ("d", "Nomor/ID ditemukan?", "UNKNOWN_OPERATOR /\nCUSTOMER_NOT_FOUND"),
    ("a", "Pilih produk/nominal → quote (harga + biaya admin)"),
    ("d", "Saldo cukup &\nPIN benar?", "INSUFFICIENT_BALANCE /\nPIN_INVALID"),
    ("a", "Debit atomik; panggil simulator penyedia", "sys"),
    ("e", "Struk (token listrik bila PLN)"),
])
flow("w_pocket.png", [
    ("s", "Mulai: Kantong"),
    ("a", "Buat kantong: nama, tujuan, target (opsional kunci)"),
    ("a", "Isi kantong dari rekening utama (Idempotency-Key)"),
    ("a", "Pengguna meminta penarikan"),
    ("d", "Kantong terkunci?", "Tarik langsung ke rekening utama"),
    ("d", "Tanggal/target\nsudah tercapai?", "Penarikan dini: konfirmasi\ntambahan + PIN (BR-06)"),
    ("a", "Kunci terbuka otomatis; dana dipindah", "sys"),
    ("e", "Saldo & riwayat diperbarui"),
])
flow("w_card.png", [
    ("s", "Mulai: Virtual Card"),
    ("d", "e-KYC disetujui &\nkartu < 5?", "KYC_REQUIRED / CARD_LIMIT"),
    ("a", "Buat kartu: domestik / internasional", "sys"),
    ("a", "Lihat detail → masukkan PIN"),
    ("d", "PIN benar?", "Detail tidak ditampilkan;\nsisa percobaan"),
    ("a", "Tampilkan nomor, masa berlaku, CVV (didekripsi)", "sys"),
    ("a", "Bekukan/aktifkan; atur limit Rp10 rb–Rp50 jt"),
    ("e", "Aturan berlaku untuk transaksi berikutnya"),
])
flow("w_history.png", [
    ("s", "Mulai: Riwayat"),
    ("a", "Muat transaksi terbaru (paginasi)", "sys"),
    ("a", "Kombinasikan filter tanggal, jenis, status + pencarian"),
    ("a", "Buka detail: ID, waktu, pihak, nominal, biaya, status, jurnal"),
    ("d", "Platform Web?", "Mobile: tanpa ekspor (FR-26)"),
    ("a", "Ekspor CSV/PDF sesuai filter aktif", "sys"),
    ("e", "Berkas diunduh"),
])
flow("w_profile.png", [
    ("s", "Mulai: Profil → Ubah data"),
    ("a", "Ubah username/email/telepon"),
    ("d", "Ada perubahan &\nformat valid?", "NO_CHANGES / pesan validasi"),
    ("a", "Kirim OTP ke kontak baru", "sys"),
    ("d", "OTP benar?", "OTP_INVALID: data lama\ntetap berlaku"),
    ("a", "Simpan perubahan; catat audit", "sys"),
    ("e", "Profil diperbarui"),
])


# ---------------- state diagram: account status
fig, ax = plt.subplots(figsize=(8.6, 2.6)); ax.set_xlim(0, 10); ax.set_ylim(0, 3); ax.axis("off")
def st(x, y, t, fc=LBLUE):
    ax.add_patch(FancyBboxPatch((x - 1.2, y - 0.32), 2.4, 0.64, boxstyle="round,pad=0.02,rounding_size=0.2", fc=fc, ec=NAVY))
    ax.text(x, y, t, ha="center", va="center", fontsize=9, fontweight="bold")
ax.add_patch(Circle((0.35, 1.8), 0.12, color="black"))
st(1.9, 1.8, "PENDING_OTP"); st(5.0, 1.8, "ACTIVE"); st(8.4, 2.4, "FINANCIAL_ENABLED", LGREEN); st(8.4, 0.8, "KYC_REJECTED", LRED)
arrow(ax, 0.47, 1.8, 0.7, 1.8)
arrow(ax, 3.1, 1.8, 3.8, 1.8); ax.text(3.45, 2.05, "OTP valid", ha="center", fontsize=8)
arrow(ax, 6.2, 1.95, 7.2, 2.35); ax.text(6.4, 2.45, "e-KYC disetujui", ha="center", fontsize=8)
arrow(ax, 6.2, 1.65, 7.2, 0.9); ax.text(6.3, 1.0, "e-KYC ditolak", ha="center", fontsize=8)
arrow(ax, 8.4, 1.12, 8.4, 2.08); ax.text(9.2, 1.6, "kirim ulang\ne-KYC", ha="center", fontsize=7.5)
fig.savefig(os.path.join(OUT, "w_state.png"), dpi=200, bbox_inches="tight", facecolor="white"); plt.close(fig)
print("ok", sorted(os.listdir(OUT)))
