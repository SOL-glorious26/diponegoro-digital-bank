// Uji kompatibilitas peramban (NFR-10): alur inti Web di Chrome, Edge, dan Firefox (headless).
// Jalankan: node web/browsers.mjs hasil/browsers [chrome,edge,firefox]
import path from "node:path";
import { writeFileSync } from "node:fs";

const OUT = path.resolve(process.argv[2] || "hasil/browsers");
const which = (process.argv[3] || "chrome,edge,firefox").split(",");
const EDGE = "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe";
const log = [];
const step = (b, s, ok, extra = "") => { log.push(`${ok ? "PASS" : "FAIL"}  ${b.padEnd(7)}  ${s}${extra ? "  — " + extra : ""}`); console.log(log.at(-1)); };

for (const name of which) {
  let web;
  try {
    if (name === "firefox") web = await (await import("../lib/bidi.mjs")).launch(OUT);
    else {
      if (name === "edge") process.env.CHROME = EDGE;
      else delete process.env.CHROME;
      const mod = await import(`../lib/cdp.mjs?${name}`); // modul dimuat ulang agar path peramban terbaca
      web = await mod.launch(OUT, { port: name === "edge" ? 9338 : 9339 });
    }
    await web.login("citra");
    const ua = (await web.evalJs("navigator.userAgent")) || "";
    const ver = (ua.match(/(Firefox|Edg)\/[\d.]+/) || ua.match(/Chrome\/[\d.]+/) || [""])[0];
    await web.shot(`${name}-1-beranda.png`);
    step(name, "login dan Beranda", true, ver);
    // transfer sesama bank
    await web.go("/transfer");
    await web.waitFor("document.querySelectorAll('#bank option').length > 0 && !!document.querySelector('#acc')");
    await web.fill("#acc", "1400000003"); await web.fill("#amt", "5000"); await web.fill("#note", "uji " + name);
    await web.clickText("Lanjut");
    await web.waitFor("document.body.innerText.includes('Konfirmasi transfer')");
    await web.clickText("Lanjut ke PIN");
    await web.waitFor("!!document.querySelector('#pin')");
    await web.fill("#pin", "123456"); await web.clickText("Konfirmasi");
    await web.waitFor("document.body.innerText.includes('Bukti transfer')");
    const okTx = /Berhasil/.test(await web.text());
    await web.shot(`${name}-2-bukti.png`);
    step(name, "transfer dengan PIN dan bukti", okTx);
    // riwayat + detail
    await web.go("/riwayat");
    await web.waitFor("document.querySelectorAll('.tx-row').length > 0");
    await web.evalJs("document.querySelector('.tx-row').click()");
    await web.waitFor("document.body.innerText.includes('Jurnal ledger')");
    await web.shot(`${name}-3-detail.png`);
    step(name, "riwayat dan detail transaksi", true);
    // QRIS (render SVG)
    await web.go("/qris");
    await web.waitFor("!!document.querySelector('.qr-frame svg')");
    step(name, "QRIS pribadi tampil", true);
    // tata letak ponsel: tidak ada scroll horizontal
    const overflow = await web.evalJs("document.documentElement.scrollWidth > window.innerWidth + 1");
    step(name, "tanpa scroll horizontal (desktop)", !overflow);
    await web.evalJs("document.querySelector('.nav-logout').click()");
    await web.waitFor("location.pathname === '/login'");
    step(name, "keluar", true);
    step(name, "tanpa error JavaScript", web.errors.length === 0, web.errors.slice(0, 2).join(" | "));
  } catch (e) {
    step(name, "ERROR", false, e.message.slice(0, 250));
    try { await web?.shot(`${name}-zz-error.png`); } catch { /* */ }
  }
  try { await web?.close(); } catch { /* */ }
}
writeFileSync(path.join(OUT, "hasil.txt"), log.join("\n") + "\n");
