// Pengendali Chrome headless lewat Chrome DevTools Protocol (tanpa pustaka tambahan).
import { spawn, spawnSync } from "node:child_process";
import { mkdirSync, writeFileSync, rmSync } from "node:fs";
import path from "node:path";
import os from "node:os";

const CHROME = process.env.CHROME || "C:/Program Files/Google/Chrome/Application/chrome.exe";
export const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// Matikan Chrome uji yang masih memegang port debugging (sisa jalan sebelumnya).
export function killChromeOnPort(port) {
  spawnSync("powershell", ["-NoProfile", "-Command",
    `Get-CimInstance Win32_Process -Filter "Name='chrome.exe'" | Where-Object { $_.CommandLine -like '*remote-debugging-port=${port}*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }`]);
}

export async function launch(outDir, { port = 9334, base = "http://localhost:5173" } = {}) {
  killChromeOnPort(port);
  await sleep(300);
  const profile = path.join(os.tmpdir(), `ddb-web-test-chrome-${port}-${Date.now()}`);
  mkdirSync(outDir, { recursive: true });
  const proc = spawn(CHROME, ["--headless=new", `--remote-debugging-port=${port}`, `--user-data-dir=${profile}`, "--no-first-run", "--window-size=1366,900", "about:blank"], { stdio: "ignore" });
  let wsUrl;
  for (let i = 0; i < 60 && !wsUrl; i++) {
    try { wsUrl = (await (await fetch(`http://127.0.0.1:${port}/json`)).json()).find((x) => x.type === "page")?.webSocketDebuggerUrl; } catch { await sleep(250); }
  }
  const ws = new WebSocket(wsUrl);
  await new Promise((r) => ws.addEventListener("open", r));
  let id = 0; const pending = new Map(); const errors = [];
  ws.addEventListener("message", (ev) => {
    const m = JSON.parse(ev.data);
    if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); }
    if (m.method === "Runtime.exceptionThrown") errors.push(m.params.exceptionDetails?.exception?.description || m.params.exceptionDetails?.text);
  });
  const send = (method, params = {}) => new Promise((r) => { const i = ++id; pending.set(i, r); ws.send(JSON.stringify({ id: i, method, params })); });
  const evalJs = async (expr) => (await send("Runtime.evaluate", { expression: expr, awaitPromise: true, returnByValue: true })).result?.result?.value;
  await send("Page.enable"); await send("Runtime.enable");

  const b = {
    errors, send, evalJs,
    go: (url) => send("Page.navigate", { url: base + url }),
    reload: () => send("Page.reload"),
    async waitFor(expr, ms = 15000) { const t = Date.now(); while (Date.now() - t < ms) { if (await evalJs(expr)) return true; await sleep(150); } throw new Error("timeout web: " + expr + " — " + (await b.text())?.slice(0, 300)); },
    text: () => evalJs("document.body?.innerText || ''"),
    fill: (sel, val) => evalJs(`(() => { const el = document.querySelector(${JSON.stringify(sel)}); const p = el.tagName === 'SELECT' ? HTMLSelectElement.prototype : HTMLInputElement.prototype; Object.getOwnPropertyDescriptor(p, 'value').set.call(el, ${JSON.stringify(val)}); el.dispatchEvent(new Event(el.tagName === 'SELECT' ? 'change' : 'input', { bubbles: true })); return true; })()`),
    clickText: (t, tag = "button") => evalJs(`(() => { const el = [...document.querySelectorAll(${JSON.stringify(tag)})].find(b => b.textContent.trim().includes(${JSON.stringify(t)}) && !b.disabled); if (!el) return false; el.click(); return true; })()`),
    async shot(file) { const r = await send("Page.captureScreenshot", { format: "png" }); writeFileSync(path.join(outDir, file), Buffer.from(r.result.data, "base64")); },
    async login(user, pw = "Demo@1234") {
      await b.go("/login");
      await b.waitFor("!!document.querySelector('#id') || !!document.querySelector('.nav-logout')");
      if (await evalJs("!!document.querySelector('.nav-logout')")) {
        await evalJs("document.querySelector('.nav-logout').click()");
        await b.waitFor("!!document.querySelector('#id')");
      }
      await b.fill("#id", user); await b.fill("#pw", pw); await b.clickText("Masuk");
      await b.waitFor("location.pathname !== '/login' && !!document.querySelector('.nav-logout')");
      if (await evalJs("location.pathname !== '/'")) await b.go("/");
      await b.waitFor("location.pathname === '/' && document.body.innerText.includes('Saldo rekening utama')");
    },
    async close() { ws.close(); spawnSync("taskkill", ["/PID", String(proc.pid), "/T", "/F"]); killChromeOnPort(port); await sleep(500); try { rmSync(profile, { recursive: true, force: true }); } catch { /* */ } },
  };
  return b;
}
