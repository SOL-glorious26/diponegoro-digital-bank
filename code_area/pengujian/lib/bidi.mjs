// Pengendali Firefox headless lewat WebDriver BiDi (tanpa pustaka tambahan).
// Antarmuka sama dengan cdp.mjs: go, reload, waitFor, text, fill, clickText, shot, login, evalJs, close.
import { spawn, spawnSync } from "node:child_process";
import { mkdirSync, writeFileSync, rmSync } from "node:fs";
import path from "node:path";
import os from "node:os";

const FIREFOX = process.env.FIREFOX || "C:/Program Files/Mozilla Firefox/firefox.exe";
export const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

export async function launch(outDir, { port = 9340, base = "http://localhost:5173" } = {}) {
  const profile = path.join(os.tmpdir(), `ddb-web-test-firefox-${Date.now()}`);
  mkdirSync(profile, { recursive: true });
  mkdirSync(outDir, { recursive: true });
  const proc = spawn(FIREFOX, ["--headless", "--no-remote", "--profile", profile, `--remote-debugging-port=${port}`, "--width=1366", "--height=900"], { stdio: "ignore" });
  let ws;
  for (let i = 0; i < 80 && !ws; i++) {
    try {
      const sock = new WebSocket(`ws://127.0.0.1:${port}/session`);
      await new Promise((res, rej) => { sock.addEventListener("open", res); sock.addEventListener("error", rej); });
      ws = sock;
    } catch { await sleep(300); }
  }
  if (!ws) throw new Error("Firefox BiDi tidak dapat dihubungi");
  let id = 0; const pending = new Map(); const errors = [];
  ws.addEventListener("message", (ev) => {
    const m = JSON.parse(ev.data);
    if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); }
    if (m.method === "log.entryAdded" && m.params.level === "error" && m.params.type === "javascript") errors.push(m.params.text);
  });
  const send = (method, params = {}) => new Promise((r) => { const i = ++id; pending.set(i, r); ws.send(JSON.stringify({ id: i, method, params })); });
  await send("session.new", { capabilities: {} });
  await send("session.subscribe", { events: ["log.entryAdded"] });
  const tree = await send("browsingContext.getTree", {});
  const context = tree.result.contexts[0].context;
  await send("browsingContext.setViewport", { context, viewport: { width: 1366, height: 900 } });

  const evalJs = async (expr) => {
    const r = await send("script.evaluate", { expression: expr, target: { context }, awaitPromise: true, resultOwnership: "none" });
    const v = r.result?.result;
    if (!v) return undefined;
    return v.type === "undefined" || v.type === "null" ? undefined : v.value;
  };
  const b = {
    errors, evalJs, send,
    go: (url) => send("browsingContext.navigate", { context, url: base + url, wait: "complete" }),
    reload: () => send("browsingContext.reload", { context, wait: "complete" }),
    async waitFor(expr, ms = 15000) { const t = Date.now(); while (Date.now() - t < ms) { try { if (await evalJs(`!!(${expr})`)) return true; } catch { /* halaman sedang berpindah */ } await sleep(150); } throw new Error("timeout web: " + expr + " — " + (await b.text())?.slice(0, 300)); },
    text: () => evalJs("document.body ? document.body.innerText : ''"),
    fill: (sel, val) => evalJs(`(() => { const el = document.querySelector(${JSON.stringify(sel)}); const p = el.tagName === 'SELECT' ? HTMLSelectElement.prototype : HTMLInputElement.prototype; Object.getOwnPropertyDescriptor(p, 'value').set.call(el, ${JSON.stringify(val)}); el.dispatchEvent(new Event(el.tagName === 'SELECT' ? 'change' : 'input', { bubbles: true })); return true; })()`),
    clickText: (t, tag = "button") => evalJs(`(() => { const el = [...document.querySelectorAll(${JSON.stringify(tag)})].find(b => b.textContent.trim().includes(${JSON.stringify(t)}) && !b.disabled); if (!el) return false; el.click(); return true; })()`),
    async shot(file) { const r = await send("browsingContext.captureScreenshot", { context }); writeFileSync(path.join(outDir, file), Buffer.from(r.result.data, "base64")); },
    async login(user, pw = "Demo@1234") {
      await b.go("/login");
      await b.waitFor("!!document.querySelector('#id') || !!document.querySelector('.nav-logout')");
      if (await evalJs("!!document.querySelector('.nav-logout')")) { await evalJs("document.querySelector('.nav-logout').click()"); await b.waitFor("!!document.querySelector('#id')"); }
      await b.fill("#id", user); await b.fill("#pw", pw); await b.clickText("Masuk");
      await b.waitFor("location.pathname !== '/login' && !!document.querySelector('.nav-logout')");
      if (await evalJs("location.pathname !== '/'")) await b.go("/");
      await b.waitFor("location.pathname === '/' && document.body.innerText.includes('Saldo rekening utama')");
    },
    async close() {
      try { await send("session.end", {}); } catch { /* */ }
      ws.close(); spawnSync("taskkill", ["/PID", String(proc.pid), "/T", "/F"]); await sleep(800);
      try { rmSync(profile, { recursive: true, force: true }); } catch { /* */ }
    },
  };
  return b;
}
