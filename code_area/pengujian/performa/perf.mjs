// Pengukuran performa dasar (STD-DDB-001 NF-01 s.d. NF-03) terhadap backend lokal.
// NF-02: p95 API baca ≤ 500 ms; NF-03: transfer ≤ 3 detik; NF-01: waktu muat layar utama Web ≤ 2 detik.
// Jalankan: node performa/perf.mjs [hasil/perf]   (backend :8080 dan Web tim (Node) :3000 aktif)
import path from "node:path";
import { writeFileSync, mkdirSync } from "node:fs";
import { launch } from "../lib/cdp.mjs";

const OUT = path.resolve(process.argv[2] || "hasil/perf");
mkdirSync(OUT, { recursive: true });
const API = "http://localhost:8080/api/v1";
const lines = [];
const out = (s) => { lines.push(s); console.log(s); };
const pct = (arr, p) => { const a = [...arr].sort((x, y) => x - y); return a[Math.min(a.length - 1, Math.ceil((p / 100) * a.length) - 1)]; };
const stats = (arr) => `n=${arr.length} median=${pct(arr, 50).toFixed(0)} ms p95=${pct(arr, 95).toFixed(0)} ms maks=${Math.max(...arr).toFixed(0)} ms`;

async function token(user) {
  const r = await fetch(API + "/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ identifier: user, password: "Demo@1234", deviceId: "perf-" + user }) });
  return (await r.json()).accessToken;
}
async function timed(fn) { const t = performance.now(); await fn(); return performance.now() - t; }

const tok = await token("budi");
const H = { Authorization: "Bearer " + tok };

// pemanasan JIT
for (let i = 0; i < 20; i++) { await fetch(API + "/accounts/me", { headers: H }); await fetch(API + "/history?size=20", { headers: H }); }

// NF-02: API baca
const reads = { "GET /accounts/me": [], "GET /history?size=20": [], "GET /history/monthly-summary": [] };
for (let i = 0; i < 100; i++) {
  for (const k of Object.keys(reads)) {
    const url = API + k.split(" ")[1];
    reads[k].push(await timed(async () => { const r = await fetch(url, { headers: H }); await r.arrayBuffer(); if (!r.ok) throw new Error(k + " " + r.status); }));
  }
}
const allReads = Object.values(reads).flat();
out("NF-02 API baca (backend lokal, 100 permintaan per endpoint, berurutan)");
for (const [k, v] of Object.entries(reads)) out(`  ${k}: ${stats(v)}`);
out(`  gabungan: ${stats(allReads)} → ${pct(allReads, 95) <= 500 ? "LULUS" : "GAGAL"} (target p95 ≤ 500 ms)`);

// NF-03: transfer sesama bank
const tx = [];
for (let i = 0; i < 10; i++) {
  tx.push(await timed(async () => {
    const r = await fetch(API + "/transfers", { method: "POST", headers: { ...H, "Content-Type": "application/json", "Idempotency-Key": `perf-${Date.now()}-${i}` },
      body: JSON.stringify({ bankCode: "DDB", accountNumber: "1400000004", amount: 5000, note: "uji performa", pin: "123456" }) });
    const d = await r.json(); if (d.status !== "SUCCESS") throw new Error("transfer " + r.status + " " + JSON.stringify(d).slice(0, 100));
  }));
}
out("NF-03 transfer sesama bank budi → citra (10 kali, termasuk verifikasi PIN BCrypt dan posting ledger)");
out(`  ${stats(tx)} → ${Math.max(...tx) <= 3000 ? "LULUS" : "GAGAL"} (target ≤ 3 detik)`);

// NF-01: waktu muat Beranda Web (Chrome headless, cache dingin per pengukuran)
const web = await launch(OUT, { port: 9336, base: process.env.DDB_WEB_NODE || "http://localhost:3000" });
await web.go("/login"); await web.waitFor("!!document.querySelector('[name=identitas]')");
await web.fill("[name=identitas]", "budi"); await web.fill("[name=password]", "Demo@1234"); await web.clickText("Login");
await web.waitFor("location.pathname === '/dashboard'");
const loads = [];
for (let i = 0; i < 5; i++) {
  await web.send("Network.enable"); await web.send("Network.setCacheDisabled", { cacheDisabled: true });
  const t0 = Date.now(), t = performance.now();
  await web.reload();
  await web.waitFor("performance.timeOrigin > " + t0 + " && document.readyState === 'complete' && !!document.querySelector('.balance') && document.querySelectorAll('.tx-row').length > 0", 15000);
  loads.push(performance.now() - t);
}
await web.shot("NF01-dashboard.png");
await web.close();
out("NF-01 muat ulang Dashboard Web tim (Node) sampai saldo dan transaksi terakhir tampil (Chrome headless, cache dimatikan)");
out(`  ${stats(loads)} → ${Math.max(...loads) <= 2000 ? "LULUS" : "GAGAL"} (target ≤ 2 detik)`);
out(`Waktu ukur: ${new Date().toISOString()}; mesin: laptop pengembang (backend profil h2).`);
writeFileSync(path.join(OUT, "hasil.txt"), lines.join("\n") + "\n");
