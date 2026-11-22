// Peramban sederhana untuk Web tim (Node/Express/EJS): cookie sesi, CSRF, redirect. Dipakai node-smoke.mjs dan lp-node.mjs.
const WEB = process.env.DDB_WEB_NODE || "http://localhost:3000";

// Peramban kecil: cookie, token CSRF, dan redirect diikuti manual.
export class Browser {
  jar = new Map();
  async req(url, opt = {}) {
    let u = new URL(url, WEB), o = { ...opt };
    for (let i = 0; i < 8; i++) {
      const r = await fetch(u, { ...o, redirect: "manual", headers: { ...(o.headers || {}), Cookie: [...this.jar].map(([k, v]) => `${k}=${v}`).join("; ") } });
      for (const c of r.headers.getSetCookie()) { const [k, ...v] = c.split(";")[0].split("="); this.jar.set(k, v.join("=")); }
      if (r.status >= 300 && r.status < 400) { u = new URL(r.headers.get("location"), u); o = {}; continue; }
      this.res = r; this.path = u.pathname + u.search; this.html = /text\/html/.test(r.headers.get("content-type") || "") ? await r.text() : "";
      return this;
    }
    throw new Error("terlalu banyak redirect");
  }
  get(u) { return this.req(u); }
  post(u, form) {
    const csrf = this.html.match(/name="_csrf"\s+value="([^"]+)"/)?.[1] || "";
    return this.req(u, { method: "POST", body: new URLSearchParams({ _csrf: csrf, ...form }), headers: { "Content-Type": "application/x-www-form-urlencoded" } });
  }
  get text() { return this.html.replace(/<(script|style)[\s\S]*?<\/\1>/g, "").replace(/<[^>]+>/g, " ").replace(/&amp;/g, "&").replace(/&#39;/g, "'").replace(/&quot;/g, '"').replace(/\s+/g, " "); }
  async login(u, pw = "Demo@1234") { await this.get("/login"); return this.post("/login", { identitas: u, password: pw }); }
}
