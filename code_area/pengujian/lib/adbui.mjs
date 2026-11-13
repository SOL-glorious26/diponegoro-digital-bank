// Penggerak UI Android sederhana lewat adb + uiautomator (tanpa pustaka tambahan).
import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";

const ADB = process.env.ADB || `${process.env.LOCALAPPDATA}\\Android\\Sdk\\platform-tools\\adb.exe`;
export const PKG = "com.diponegoro.digitalbank";
export const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
export const adb = (...args) => execFileSync(ADB, args, { encoding: "utf8", maxBuffer: 32 * 1024 * 1024 });

function decode(s) {
  return s.replace(/&quot;/g, '"').replace(/&apos;/g, "'").replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&#10;/g, "\n").replace(/&amp;/g, "&");
}

export function dump() {
  for (let i = 0; i < 3; i++) {
    try {
      adb("shell", "uiautomator", "dump", "/sdcard/ui.xml");
      const xml = adb("exec-out", "cat", "/sdcard/ui.xml");
      const nodes = [];
      for (const m of xml.matchAll(/<node ([^>]*?)\/?>/g)) {
        const a = Object.fromEntries([...m[1].matchAll(/([\w-]+)="([^"]*)"/g)].map((x) => [x[1], decode(x[2])]));
        const b = a.bounds?.match(/\[(\d+),(\d+)\]\[(\d+),(\d+)\]/);
        if (!b) continue;
        nodes.push({ text: a.text || "", desc: a["content-desc"] || "", cls: a.class, clickable: a.clickable === "true",
          editable: a.class?.includes("EditText"), focused: a.focused === "true",
          checkable: a.checkable === "true", checked: a.checked === "true",
          x: (+b[1] + +b[3]) >> 1, y: (+b[2] + +b[4]) >> 1, x1: +b[1], y1: +b[2], x2: +b[3], y2: +b[4] });
      }
      return nodes;
    } catch { /* uiautomator kadang gagal saat animasi */ }
  }
  return [];
}
export const screenText = () => dump().map((n) => n.text || n.desc).filter(Boolean).join(" | ");
export const find = (re, nodes = dump()) => nodes.find((n) => re.test(n.text) || re.test(n.desc));

export async function waitText(re, ms = 20000) {
  const t = Date.now();
  while (Date.now() - t < ms) {
    const nodes = dump();
    const n = find(re, nodes);
    if (n) return n;
    await sleep(600);
  }
  throw new Error(`timeout menunggu ${re} — layar: ${screenText().slice(0, 400)}`);
}

export const tapXY = (x, y) => adb("shell", "input", "tap", String(x), String(y));
export async function tap(re, ms) { const n = await waitText(re, ms); tapXY(n.x, n.y); await sleep(500); return n; }
export async function tapIfPresent(re) { const n = find(re); if (n) { tapXY(n.x, n.y); await sleep(500); } return !!n; }

// Isi kolom teks: ketuk kolom yang label/teksnya cocok, hapus isi lama, ketik nilai.
export async function typeInto(re, value) {
  const nodes = dump();
  let field = nodes.find((n) => n.editable && (re.test(n.text) || re.test(n.desc)));
  if (!field) {
    const label = find(re, nodes);
    if (label) field = nodes.filter((n) => n.editable).sort((a, b) => Math.hypot(a.x - label.x, a.y - label.y) - Math.hypot(b.x - label.x, b.y - label.y))[0];
  }
  if (!field) throw new Error(`kolom ${re} tidak ditemukan — layar: ${screenText().slice(0, 300)}`);
  tapXY(field.x, field.y); await sleep(300);
  adb("shell", "input", "keyevent", "KEYCODE_MOVE_END");
  for (let i = 0; i < 6; i++) adb("shell", "input", "keyevent", ...Array(12).fill("KEYCODE_DEL"));
  typeText(value); await sleep(300);
}
export function typeText(value) {
  // Setiap potongan dikirim dalam tanda kutip ganda agar spasi dan karakter khusus terketik apa adanya
  // (penggantian spasi dengan %s terbukti menghilangkan karakter di batas potongan).
  // Batas potongan tidak boleh jatuh di spasi: potongan yang diawali/diakhiri spasi kehilangan karakter.
  const s = String(value);
  let i = 0;
  while (i < s.length) {
    let j = Math.min(s.length, i + 80);
    while (j < s.length && j > i + 1 && (s[j] === " " || s[j - 1] === " ")) j--;
    const c = s.slice(i, j);
    adb("shell", `input text "${c.replace(/(["\\$`])/g, "\\$1")}"`);
    i = j;
  }
}
export const key = (k) => adb("shell", "input", "keyevent", k);
export const back = () => key("KEYCODE_BACK");
export function swipeUp() { adb("shell", "input", "swipe", "540", "1800", "540", "700", "300"); }
export async function scrollTo(re, max = 8) {
  for (let i = 0; i < max; i++) { const n = find(re); if (n) return n; swipeUp(); await sleep(700); }
  throw new Error(`tidak menemukan ${re} setelah scroll — layar: ${screenText().slice(0, 300)}`);
}
export function screenshot(file) { writeFileSync(file, execFileSync(ADB, ["exec-out", "screencap", "-p"], { maxBuffer: 64 * 1024 * 1024 })); }
export function restartApp() {
  adb("shell", "am", "force-stop", PKG);
  adb("shell", "monkey", "-p", PKG, "-c", "android.intent.category.LAUNCHER", "1");
}
export function clearApp() { adb("shell", "pm", "clear", PKG); }

// Ketuk kotak centang/sakelar yang sebaris dengan label (Compose memisahkan label dan kontrolnya).
export async function tapControlFor(labelRe) {
  const nodes = dump();
  const label = find(labelRe, nodes);
  if (!label) throw new Error(`label ${labelRe} tidak ditemukan`);
  const ctl = nodes.filter((n) => /CheckBox|Switch|RadioButton/.test(n.cls || "")).sort((a, b) => Math.abs(a.y - label.y) - Math.abs(b.y - label.y))[0];
  if (!ctl || Math.abs(ctl.y - label.y) > 60) throw new Error(`kontrol untuk ${labelRe} tidak ditemukan`);
  tapXY(ctl.x, ctl.y); await sleep(600);
}

