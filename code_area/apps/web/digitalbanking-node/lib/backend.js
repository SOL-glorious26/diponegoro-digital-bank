'use strict';
/**
 * Klien REST ke backend Spring Boot (API /api/v1). Satu-satunya tempat web ini bicara ke backend.
 * - Token JWT disimpan di sesi SERVER (req.session.tokens), tidak pernah dikirim ke peramban.
 * - Saat backend membalas 401, token diperbarui sekali lewat /auth/refresh lalu permintaan diulang.
 * - Error backend {code, message, fields} diubah menjadi ApiError agar pesannya bisa langsung ditampilkan.
 */
const BASE = () => (process.env.BACKEND_URL || 'http://localhost:8080/api/v1').replace(/\/+$/, '');

class ApiError extends Error {
  constructor(message, { status = 0, code = null, fields = null } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fields = fields;
  }
}

const refreshing = new Map(); // sessionID -> Promise (satu refresh dipakai bersama)

function query(params) {
  const u = new URLSearchParams();
  for (const [k, v] of Object.entries(params || {})) {
    if (v !== undefined && v !== null && v !== '') u.set(k, String(v));
  }
  const s = u.toString();
  return s ? '?' + s : '';
}

async function send(method, path, { body, params, key, token, accept } = {}) {
  const headers = { Accept: accept || 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = 'Bearer ' + token;
  if (key) headers['Idempotency-Key'] = key;
  try {
    return await fetch(BASE() + path + query(params), {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: AbortSignal.timeout(20000),
    });
  } catch (e) {
    throw new ApiError(`Tidak dapat terhubung ke backend (${BASE()}). Pastikan backend sudah berjalan.`, { code: 'BACKEND_UNREACHABLE' });
  }
}

async function toError(res) {
  let b = null;
  try { b = await res.json(); } catch { /* bukan JSON */ }
  return new ApiError(b?.message || `Permintaan gagal (HTTP ${res.status}).`, { status: res.status, code: b?.code || null, fields: b?.fields || null });
}

const sessionEnded = () => new ApiError('Sesi Anda telah berakhir. Silakan masuk kembali.', { status: 401, code: 'SESSION_ENDED' });

async function refreshTokens(req) {
  const sid = req.sessionID;
  if (refreshing.has(sid)) return refreshing.get(sid);
  const p = (async () => {
    const rt = req.session?.tokens?.refreshToken;
    if (!rt) throw sessionEnded();
    const res = await send('POST', '/auth/refresh', { body: { refreshToken: rt } });
    if (!res.ok) throw sessionEnded();
    const t = await res.json();
    return { accessToken: t.accessToken, refreshToken: t.refreshToken };
  })().finally(() => refreshing.delete(sid));
  refreshing.set(sid, p);
  return p;
}

/**
 * call(req, 'POST', '/transfers', { body, params, key, auth:false, raw:true })
 * auth:false  → tanpa token dan tanpa refresh (login, registrasi, pemulihan)
 * raw:true    → kembalikan objek Response (untuk unduhan CSV/PDF)
 */
async function call(req, method, path, opts = {}) {
  const useAuth = opts.auth !== false;
  const go = () => send(method, path, { ...opts, token: useAuth ? req.session?.tokens?.accessToken : undefined });
  let res = await go();
  if (res.status === 401 && useAuth) {
    req.session.tokens = await refreshTokens(req);
    res = await go();
  }
  if (!res.ok) throw await toError(res);
  if (opts.raw) return res;
  if (res.status === 204) return null;
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

module.exports = { call, ApiError, send };
