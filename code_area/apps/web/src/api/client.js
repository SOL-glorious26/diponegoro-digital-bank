import axios from "axios";

// Penyimpanan token (SAD-DDB-001 ADR-12): access token di memori, refresh token di sessionStorage.
const REFRESH_KEY = "ddb.refreshToken";
let accessToken = null;
let onSessionEnded = () => {};

const store = {
  getRefresh() {
    try { return sessionStorage.getItem(REFRESH_KEY); } catch { return null; }
  },
  setRefresh(v) {
    try { v ? sessionStorage.setItem(REFRESH_KEY, v) : sessionStorage.removeItem(REFRESH_KEY); } catch { /* abaikan */ }
  },
};

export function setTokens(tokens) {
  accessToken = tokens?.accessToken ?? null;
  store.setRefresh(tokens?.refreshToken ?? null);
}
export function clearTokens() {
  accessToken = null;
  store.setRefresh(null);
}
export const hasRefreshToken = () => !!store.getRefresh();
export function setSessionEndedHandler(fn) { onSessionEnded = fn; }

export const api = axios.create({ baseURL: "/api/v1", timeout: 20000 });

api.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
  return config;
});

// Satu refresh berjalan untuk banyak permintaan 401 sekaligus.
let refreshing = null;
export async function refreshSession() {
  const rt = store.getRefresh();
  if (!rt) throw new Error("no refresh token");
  if (!refreshing) {
    refreshing = axios
      .post("/api/v1/auth/refresh", { refreshToken: rt })
      .then((r) => { setTokens(r.data); return r.data; })
      .finally(() => { refreshing = null; });
  }
  return refreshing;
}

const NO_REFRESH = ["/auth/login", "/auth/refresh", "/auth/register", "/auth/recovery"];

api.interceptors.response.use(
  (r) => r,
  async (error) => {
    const cfg = error.config;
    const status = error.response?.status;
    const skip = NO_REFRESH.some((p) => cfg?.url?.startsWith(p));
    if (status === 401 && cfg && !cfg._retried && !skip) {
      cfg._retried = true;
      try {
        await refreshSession();
        return api(cfg);
      } catch {
        clearTokens();
        onSessionEnded();
      }
    }
    return Promise.reject(error);
  },
);

// Pesan error siap tampil dari backend: {"code","message","fields"}.
export function errorMessage(err, fallback = "Terjadi kesalahan. Coba lagi.") {
  if (!err?.response) return "Tidak dapat terhubung ke server. Pastikan backend berjalan.";
  const d = err.response.data;
  if (d?.fields && Object.keys(d.fields).length) {
    const first = Object.values(d.fields)[0];
    return d.message ? `${d.message} ${first}`.trim() : String(first);
  }
  return d?.message || fallback;
}
export const errorCode = (err) => err?.response?.data?.code;

// Kunci idempotensi per layar konfirmasi (BR-04): dipakai ulang saat retry.
export function newIdempotencyKey() {
  if (crypto?.randomUUID) return crypto.randomUUID();
  return "k" + Date.now().toString(36) + Math.random().toString(36).slice(2, 10);
}
export const withKey = (key) => ({ headers: { "Idempotency-Key": key } });
