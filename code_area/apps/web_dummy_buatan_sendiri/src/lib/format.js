const rp = new Intl.NumberFormat("id-ID");
export const rupiah = (n) => (n == null ? "–" : `Rp${rp.format(n)}`);
export const signedRupiah = (n, direction) =>
  (direction === "IN" ? "+" : direction === "OUT" ? "−" : "") + rupiah(Math.abs(n ?? 0));

const dt = new Intl.DateTimeFormat("id-ID", {
  timeZone: "Asia/Jakarta", day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit",
});
const d = new Intl.DateTimeFormat("id-ID", { timeZone: "Asia/Jakarta", day: "2-digit", month: "long", year: "numeric" });
export const dateTime = (iso) => (iso ? `${dt.format(new Date(iso))} WIB` : "–");
export const dateOnly = (iso) => (iso ? d.format(new Date(iso)) : "–");

export const onlyDigits = (s) => (s || "").replace(/\D/g, "");
export const parseAmount = (s) => {
  const v = parseInt(onlyDigits(s), 10);
  return Number.isFinite(v) ? v : 0;
};
export const formatAmountInput = (s) => {
  const v = onlyDigits(s);
  return v ? rp.format(parseInt(v, 10)) : "";
};

export function deviceId() {
  const KEY = "ddb.deviceId";
  try {
    let id = localStorage.getItem(KEY);
    if (!id) {
      id = "web-" + (crypto?.randomUUID ? crypto.randomUUID() : Date.now().toString(36));
      localStorage.setItem(KEY, id);
    }
    return id;
  } catch {
    return "web-anon";
  }
}
export function deviceName() {
  const ua = navigator.userAgent;
  const b = /Edg\//.test(ua) ? "Edge" : /Firefox\//.test(ua) ? "Firefox" : /Chrome\//.test(ua) ? "Chrome" : /Safari\//.test(ua) ? "Safari" : "Peramban";
  return `${b} (Web)`;
}
