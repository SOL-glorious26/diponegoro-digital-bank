// Uji kompatibilitas peramban (NFR-10) untuk Web tim (Node/Express/EJS): Chrome, Edge, Firefox (headless).
// Prasyarat: backend :8080 (data demo) dan Web Node :3000. Jalankan: node web/node-peramban.mjs hasil/web-node-peramban [chrome,edge,firefox]
import path from "node:path";
import { mkdirSync, writeFileSync } from "node:fs";

const OUT = path.resolve(process.argv[2] || "hasil/web-node-peramban");
const which = (process.argv[3] || "chrome,edge,firefox").split(",");
const EDGE = "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe";
const base = process.env.DDB_WEB_NODE || "http://localhost:3000";
mkdirSync(OUT, { recursive: true });
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const log = [];
const step = (b, s, ok, extra = "") => { log.push(`${ok ? "PASS" : "FAIL"}  ${b.padEnd(7)}  ${s}${extra ? "  — " + extra : ""}`); console.log(log.at(-1)); };

for (const name of which) {
  let web;
  try {
    if (name === "firefox") web = await (await import("../lib/bidi.mjs")).launch(OUT, { base });
    else {
      if (name === "edge") process.env.CHROME = EDGE; else delete process.env.CHROME;
      const mod = await import(`../lib/cdp.mjs?${name}`); // modul dimuat ulang agar path peramban terbaca
      web = await mod.launch(OUT, { port: name === "edge" ? 9348 : 9349, base });
    }
    const go = async (u, cond) => { await web.go(u); await sleep(400); await web.waitFor(`document.readyState === 'complete' && (${cond})`); };
    const isi = (sel, v) => web.fill(`form input[name="${sel}"]:not([type=hidden])`, v);
    const ada = (t) => `document.body.innerText.includes(${JSON.stringify(t)})`;

    await go("/login", "!!document.querySelector('[name=identitas]')");
    await isi("identitas", "citra"); await isi("password", "Demo@1234"); await web.clickText("Login");
    await web.waitFor(`location.pathname === '/dashboard' && ${ada("Saldo Rekening")}`);
    const ua = (await web.evalJs("navigator.userAgent")) || "";
    const ver = (ua.match(/(Firefox|Edg)\/[\d.]+/) || ua.match(/Chrome\/[\d.]+/) || [""])[0];
    await web.shot(`${name}-1-dashboard.png`);
    step(name, "login dan Dashboard", true, ver);

    await go("/transfer?tab=antar", "!!document.querySelector('[name=rekening_tujuan]')");
    await isi("rekening_tujuan", "1400000003"); await isi("nominal", "6000"); await isi("catatan", "uji " + name);
    await web.clickText("Lanjut ke Konfirmasi");
    await web.waitFor("!!document.querySelector('input[name=pin]')");
    const eye = await web.evalJs("!!document.querySelector('.pw-eye')");
    await isi("pin", "123456"); await web.clickText("Konfirmasi");
    await web.waitFor(`location.pathname === '/riwayat' && ${ada("Detail Transaksi")}`);
    await web.shot(`${name}-2-transfer.png`);
    step(name, "transfer dengan PIN dan detail transaksi", /berhasil/i.test(await web.text()));
    step(name, "tombol lihat/sembunyikan PIN tampil", eye);

    await go("/riwayat?jenis=TRANSFER", "document.querySelectorAll('tbody tr').length > 0");
    step(name, "riwayat dengan filter", true);
    await go("/pembayaran?tab=qris", ada("QRIS"));
    await web.shot(`${name}-3-qris.png`);
    step(name, "halaman pembayaran QRIS tampil", true);
    let lebar = [];
    for (const p of ["/dashboard", "/transfer", "/pembayaran", "/riwayat", "/profil"]) {
      await go(p, "true");
      if (await web.evalJs("document.documentElement.scrollWidth > window.innerWidth + 1")) lebar.push(p);
    }
    step(name, "tanpa scroll horizontal (desktop, 5 halaman)", lebar.length === 0, lebar.join(" "));
    await web.go("/logout"); await web.waitFor("location.pathname === '/login'");
    step(name, "keluar", true);
    step(name, "tanpa error JavaScript", web.errors.length === 0, web.errors.slice(0, 2).join(" | "));
  } catch (e) {
    step(name, "ERROR", false, e.message.replace(/\s+/g, " ").slice(0, 250));
    try { await web?.shot(`${name}-zz-error.png`); } catch { /* */ }
  }
  try { await web?.close(); } catch { /* */ }
}
writeFileSync(path.join(OUT, "hasil.txt"), log.join("\n") + "\n");
console.log(`\n${log.filter((l) => l.startsWith("PASS")).length}/${log.length} PASS`);
