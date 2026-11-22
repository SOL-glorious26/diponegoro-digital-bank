// SA-08 pada emulator: pindai QRIS lewat kamera aplikasi dengan kamera virtual (virtual scene).
// Gambar QR dipasang sebagai poster dinding, lalu kamera virtual "berjalan" ke poster itu (macro bawaan emulator).
// Prasyarat: emulator dijalankan dengan -camera-back virtualscene, APK varian online, backend :8080.
// Jalankan: node android/sa08-kamera-emulator.mjs hasil/sa08 <berkas-qr.png>
import path from "node:path";
import { spawnSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import * as A from "../lib/adbui.mjs";
import * as F from "../lib/android-flows.mjs";
const { sleep } = A;
const OUT = path.resolve(process.argv[2] || "hasil/sa08");
const QR = path.resolve(process.argv[3]);
mkdirSync(OUT, { recursive: true });
const SDK = process.env.ANDROID_HOME || path.join(process.env.LOCALAPPDATA, "Android/Sdk");
const adb = (...a) => { const r = spawnSync(path.join(SDK, "platform-tools/adb.exe"), a, { encoding: "utf8" }); return (r.stdout || "") + (r.stderr || ""); };
const API = "http://localhost:8080/api/v1";
async function saldo(u) {
  const t = (await (await fetch(API + "/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ identifier: u, password: "Demo@1234", deviceId: "sa08-" + u }) })).json()).accessToken;
  return (await (await fetch(API + "/accounts/me", { headers: { Authorization: "Bearer " + t } })).json()).balance;
}
const shot = (n) => A.screenshot(path.join(OUT, n + ".png"));
let hasil;
try {
  adb("shell", "pm", "grant", "com.diponegoro.digitalbank", "android.permission.CAMERA");
  console.log("poster:", adb("emu", "virtualscene-image", "wall", QR.replaceAll("\\", "/")).trim());
  await F.login("demo"); await F.home();
  await A.tap(/^Bayar QRIS$/);
  const d0 = await saldo("demo"), a0 = await saldo("andi");
  await A.tap(/Pindai dengan kamera/); await sleep(3000); shot("SA08-1-kamera-dibuka");
  console.log("macro:", adb("emu", "automation", "play", path.join(SDK, "emulator/resources/macros/Walk_to_image_room").replaceAll("\\", "/")).trim());
  let ok = false;
  for (let i = 0; i < 30 && !ok; i++) {
    await sleep(1000);
    if (i % 4 === 3) shot("SA08-2-kamera-" + String(i).padStart(2, "0"));
    try { ok = /Detail pembayaran/.test(A.screenText()); } catch { /* layar kamera */ }
  }
  if (!ok) throw new Error("kode tidak terbaca kamera virtual");
  shot("SA08-3-terbaca");
  const teks = A.screenText();
  if (!/ANDI/i.test(teks)) throw new Error("penerima bukan andi: " + teks.slice(0, 200));
  await A.typeInto(/^Nominal$/, "13500"); A.back(); await sleep(300);
  await A.tap(/^Bayar$/); await F.pin(); await A.waitText(/berhasil/i, 20000); shot("SA08-4-berhasil");
  const d1 = await saldo("demo"), a1 = await saldo("andi");
  if (d0 - d1 !== 13500 || a1 - a0 !== 13500) throw new Error(`saldo demo ${d0}->${d1}, andi ${a0}->${a1}`);
  hasil = "PASS  SA-08  pindai QRIS pribadi andi dengan kamera virtual emulator, bayar Rp13.500 dengan PIN  — demo −Rp13.500, andi +Rp13.500";
} catch (e) { shot("SA08-gagal"); hasil = "FAIL  SA-08  " + e.message.replace(/\s+/g, " ").slice(0, 300); }
adb("emu", "virtualscene-image", "wall");
writeFileSync(path.join(OUT, "hasil.txt"), hasil + "\n");
console.log(hasil);
