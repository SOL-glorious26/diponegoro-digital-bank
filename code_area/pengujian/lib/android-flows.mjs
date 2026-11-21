// Langkah Android yang dipakai berulang oleh uji sistem (SA) dan lintas platform (LP).
import * as A from "./adbui.mjs";
const { sleep } = A;

export function freshStart() {
  A.clearApp();
  for (const p of ["ACCESS_LOCAL_NETWORK", "POST_NOTIFICATIONS", "CAMERA"]) {
    try { A.adb("shell", "pm", "grant", A.PKG, "android.permission." + p); } catch { /* izin tidak ada di versi ini */ }
  }
  A.restartApp();
}

export async function login(user = "demo", password = "Demo@1234", pin = "123456") {
  freshStart();
  await A.waitText(/^Masuk$/, 30000);
  await A.typeInto(/^Username, email/, user);
  await A.typeInto(/^Password$/, password);
  A.back(); await sleep(400);
  await A.tap(/^Masuk$/);
  await keypadPin(pin);
  await A.waitText(/^Saldo rekening utama$/, 20000);
}

// Dialog PIN transaksi: label "PIN 6 digit" (transfer/QRIS/top-up) atau "PIN (6 digit)" (detail kartu).
export async function pin(value = "123456") {
  const label = /^PIN 6 digit$|^PIN \(6 digit\)$/;
  await A.waitText(label);
  // Pengetikan adb di emulator sesekali meleset; pastikan kolom berisi tepat 6 karakter sebelum dikirim
  // agar akun uji tidak terblokir oleh PIN yang salah ketik.
  for (let i = 0; i < 3; i++) {
    await A.typeInto(label, value);
    const field = A.dump().find((n) => n.editable);
    if (field && field.text.length === value.length) break;
  }
  await A.tap(/^Konfirmasi$/);
}
// Layar PIN setelah login dikenali dari judul "Masukkan PIN" (dialog PIN transaksi berjudul "Masukkan PIN Transaksi").
const isLoginPin = (nodes = A.dump()) => !!A.find(/^Masukkan PIN$/, nodes);

// Layar PIN setelah login memakai keypad angka (tanpa kolom teks); PIN terkirim otomatis pada digit keenam.
export async function keypadPin(value) {
  await A.waitText(/^Masukkan PIN$/);
  for (const d of value) await A.tap(new RegExp(`^${d}$`));
}

// Beranda dikenali dari tombol "Terima QRIS" (hanya ada di Beranda; "Saldo rekening utama" juga ada di layar lain).
const isHome = (nodes = A.dump()) => !!A.find(/^Terima QRIS$/, nodes) && !!A.find(/^Saldo rekening utama$/, nodes);
const appInFront = () => A.adb("shell", "dumpsys", "window").match(/mCurrentFocus=.*\}/)?.[0]?.includes(A.PKG);

// Masuk lagi tanpa menghapus data aplikasi (dipakai bila sesi berakhir atau aplikasi dibuka ulang).
export async function relogin(user = "demo", password = "Demo@1234", pinValue = "123456") {
  if (!appInFront()) { A.restartApp(); await sleep(2500); }
  await A.waitText(/^Masuk$|^Masukkan PIN$|^Saldo rekening utama$/, 30000);
  if (A.find(/^Masuk$/) && A.find(/^Username, email/)) {
    await A.typeInto(/^Username, email/, user);
    await A.typeInto(/^Password$/, password);
    A.back(); await sleep(400);
    await A.tap(/^Masuk$/);
    await A.waitText(/^Masukkan PIN$/);
  }
  if (A.find(/^Masukkan PIN$/)) {
    await keypadPin(pinValue);
  }
  await A.waitText(/^Saldo rekening utama$/, 20000);
}

export async function home(user, password, pinValue) {
  for (let i = 0; i < 8 && !isHome(); i++) {
    // Jangan menekan Kembali dari layar login/PIN atau di luar aplikasi: masuk lagi.
    const nodes = A.dump();
    if (!appInFront() || A.find(/^Username, email/, nodes) || isLoginPin(nodes)) { await relogin(user, password, pinValue); continue; }
    if (A.find(/^Batal$/, nodes) && A.find(/PIN/, nodes)) { await A.tapIfPresent(/^Batal$/); await sleep(700); continue; }
    if (await A.tapIfPresent(/^Beranda$/)) { await sleep(900); continue; }
    A.back(); await sleep(800);
  }
  const t = Date.now();
  while (!isHome()) { if (Date.now() - t > 15000) throw new Error("tidak kembali ke Beranda: " + A.screenText().slice(0, 200)); await sleep(500); }
}

export const balanceText = () => {
  const nodes = A.dump();
  const i = nodes.findIndex((n) => n.text === "Saldo rekening utama");
  return nodes.slice(i + 1).find((n) => /^Rp[\d.]+$|•/.test(n.text))?.text;
};
