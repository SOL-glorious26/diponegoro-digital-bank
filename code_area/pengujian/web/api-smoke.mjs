// Uji asap kontrak API yang dipakai aplikasi Web, lewat proxy Vite (http://localhost:5173/api/v1).
const B = "http://localhost:5173/api/v1";
let token = null;
const results = [];
async function call(method, path, body, headers = {}) {
  const r = await fetch(B + path, {
    method, headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}), ...headers },
    body: body ? JSON.stringify(body) : undefined,
  });
  const ct = r.headers.get("content-type") || "";
  const data = ct.includes("json") ? await r.json() : await r.text();
  return { status: r.status, data, headers: r.headers };
}
function check(name, cond, extra = "") { results.push([cond ? "PASS" : "FAIL", name, extra]); }
const uuid = () => crypto.randomUUID();

// Login demo (FR-03, tanpa OTP)
let r = await call("POST", "/auth/login", { identifier: "demo", password: "Demo@1234", deviceId: "web-smoke", deviceName: "Chrome (Web)" });
check("login demo", r.status === 200 && r.data.accessToken && r.data.user?.ekycStatus === "APPROVED" && r.data.user?.hasPin, `status ${r.status}`);
token = r.data.accessToken; const refresh = r.data.refreshToken;

r = await call("GET", "/accounts/me");
check("saldo (FR-06)", r.status === 200 && typeof r.data.balance === "number", `${r.data.accountNumber} ${r.data.balance}`);
const before = r.data.balance;

r = await call("GET", "/me/notifications/unread-count");
check("unread-count", r.status === 200 && typeof r.data.unread === "number");

r = await call("GET", "/transfers/banks");
const internal = r.data.find((b) => b.internal);
check("daftar bank", r.status === 200 && internal, internal?.code);

r = await call("POST", "/transfers/inquiry", { bankCode: internal.code, accountNumber: "1400000002" });
check("inquiry (FR-12)", r.status === 200 && /ANDI/i.test(r.data.name), r.data.name);

r = await call("POST", "/transfers/quote", { bankCode: internal.code, accountNumber: "1400000002", amount: 10000, note: "uji web" });
check("quote", r.status === 200 && r.data.total === 10000 && r.data.balanceAfter === before - 10000, JSON.stringify({ fee: r.data.fee, after: r.data.balanceAfter }));

r = await call("POST", "/transfers", { bankCode: internal.code, accountNumber: "1400000002", amount: 10000, note: "uji web", pin: "000000" }, { "Idempotency-Key": uuid() });
check("PIN salah ditolak (FR-04)", r.status >= 400, `${r.status} ${r.data.code} – ${r.data.message}`);

const key = uuid();
r = await call("POST", "/transfers", { bankCode: internal.code, accountNumber: "1400000002", amount: 10000, note: "uji web", pin: "123456" }, { "Idempotency-Key": key });
check("transfer sesama bank (FR-10)", r.status === 200 || r.status === 201, `${r.status} ${r.data.status}`);
const txId = r.data.id;
r = await call("POST", "/transfers", { bankCode: internal.code, accountNumber: "1400000002", amount: 10000, note: "uji web", pin: "123456" }, { "Idempotency-Key": key });
check("kunci idempotensi sama → tidak debit ganda (BR-04)", r.data.id === txId, `${r.status} id sama=${r.data.id === txId}`);
r = await call("GET", "/accounts/me");
check("saldo berkurang tepat sekali", r.data.balance === before - 10000, `${before} → ${r.data.balance}`);

const bri = (await call("GET", "/transfers/banks")).data.find((b) => /BRI/i.test(b.name));
r = await call("POST", "/transfers", { bankCode: bri.code, accountNumber: "9999999999", amount: 10000, pin: "123456" }, { "Idempotency-Key": uuid() });
check("antarbank ditolak A6 (FR-11)", r.data.status === "FAILED" || r.status >= 400, `${r.status} ${r.data.status || r.data.code}`);

r = await call("GET", `/history?size=5&category=TRANSFER`);
check("riwayat + filter (FR-23)", r.status === 200 && Array.isArray(r.data.items) && r.data.items.every((t) => t.category === "TRANSFER"), `${r.data.totalItems} item`);
r = await call("GET", `/history/${txId}`);
check("detail + jurnal (FR-24)", r.status === 200 && r.data.transaction?.id === txId && r.data.journal?.length >= 2, `${r.data.journal?.length} baris`);
let ex = await fetch(B + "/history/export?format=csv&category=TRANSFER", { headers: { Authorization: `Bearer ${token}` } });
check("ekspor CSV (FR-26)", ex.status === 200, `${ex.headers.get("content-type")} ${ex.headers.get("content-disposition")}`);
ex = await fetch(B + "/history/export?format=pdf", { headers: { Authorization: `Bearer ${token}` } });
check("ekspor PDF (FR-26)", ex.status === 200 && /pdf/.test(ex.headers.get("content-type")), `${ex.headers.get("content-type")}`);
r = await call("GET", "/history/monthly-summary?months=6");
check("ringkasan bulanan (FR-25)", r.status === 200 && r.data.length === 6, r.data.map((m) => m.label).join(","));

r = await call("GET", "/topup/catalog");
check("katalog top-up", r.status === 200 && r.data.pulsaNominals?.length, `billers=${r.data.billers?.map((b) => b.code)} ewallets=${r.data.ewallets?.map((w) => w.code)}`);
const nominal = r.data.pulsaNominals[0];
r = await call("POST", "/topup/quote", { kind: "PULSA", phone: "081234567890", nominal });
check("quote pulsa (FR-18)", r.status === 200 && r.data.total > 0, JSON.stringify({ title: r.data.title, total: r.data.total }));
r = await call("POST", "/topup/orders", { kind: "PULSA", phone: "081234567890", nominal, pin: "123456" }, { "Idempotency-Key": uuid() });
check("bayar pulsa", r.status < 300 && r.data.status === "SUCCESS", `${r.status} ${r.data.status}`);
r = await call("GET", "/topup/pln/inquiry?meterNumber=123456780000");
check("PLN tidak ditemukan (FR-19)", r.status >= 400, `${r.status} ${r.data.message}`);
r = await call("GET", "/topup/data-packages?phone=081234567890");
check("paket data", r.status === 200 && r.data.packages?.length, r.data.operator?.name);

r = await call("GET", "/qris/my-qr");
check("QRIS pribadi (FR-16)", r.status === 200 && r.data.payload?.startsWith("000201"), r.data.accountName);
r = await call("GET", "/qris/my-qr?amount=25000");
check("QRIS dinamis (FR-17)", r.data.dynamic === true && r.data.amount === 25000);
r = await call("POST", "/qris/simulate-incoming", { amount: 15000, payerName: "Uji Web" }, { "Idempotency-Key": uuid() });
check("simulasi dana masuk", r.status < 300, `${r.status} ${r.data.fromName}`);

r = await call("GET", "/accounts/pockets");
check("kantong (FR-08)", r.status === 200 && Array.isArray(r.data));
r = await call("GET", "/cards");
check("kartu (FR-21)", r.status === 200 && Array.isArray(r.data));
r = await call("GET", "/me/notification-settings");
check("pengaturan notifikasi (FR-28)", r.status === 200 && Array.isArray(r.data.types));
r = await call("GET", "/auth/sessions");
check("daftar sesi (FR-05)", r.status === 200 && r.data.some((s) => s.current));
r = await call("GET", "/me/notifications");
check("kotak notifikasi (FR-29)", r.status === 200 && Array.isArray(r.data.items), `${r.data.items?.length} item`);

// Refresh token (dipakai interceptor 401)
r = await fetch(B + "/auth/refresh", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ refreshToken: refresh }) });
const rd = await r.json();
check("refresh token", r.status === 200 && rd.accessToken && rd.user, `user di respons: ${!!rd.user}`);

// Registrasi pengguna baru → OTP → e-KYC → PIN (FR-01, FR-02, FR-04)
token = null;
const uname = "web" + Date.now().toString(36);
r = await call("POST", "/auth/register", { username: uname, email: `${uname}@mail.test`, password: "Rahasia123" });
check("registrasi (FR-01)", r.status === 201 && r.data.otp?.demoCode, r.data.otp?.sentTo);
r = await call("POST", "/auth/register/verify-otp", { userId: r.data.userId, code: r.data.otp.demoCode, deviceId: "web-smoke2" });
check("verifikasi OTP → login", r.status === 200 && r.data.accessToken && r.data.user?.ekycStatus == null, `ekyc=${r.data.user?.ekycStatus} hasPin=${r.data.user?.hasPin}`);
token = r.data.accessToken;
r = await call("GET", "/accounts/me");
check("sebelum e-KYC tidak ada rekening", r.status >= 400, `${r.status} ${r.data.code}`);
r = await call("GET", "/auth/ekyc");
check("status e-KYC awal", true, `${r.status} ${JSON.stringify(r.data).slice(0, 80)}`);
r = await call("POST", "/auth/ekyc", { nik: "3374010101010001", fullName: "Uji Web", selfieReference: "selfie.jpg" });
check("kirim e-KYC (FR-02)", r.data.status === "PROCESSING", r.data.nikMasked);
await new Promise((s) => setTimeout(s, 4000));
r = await call("GET", "/auth/ekyc");
check("e-KYC disetujui", r.data.status === "APPROVED");
r = await call("POST", "/auth/pin", { pin: "246810" });
check("buat PIN", r.status === 201, `${r.status}`);
r = await call("GET", "/me");
check("user siap transaksi", r.data.ekycStatus === "APPROVED" && r.data.hasPin === true, r.data.status);

for (const [s, n, e] of results) console.log(`${s}  ${n}${e ? "  — " + e : ""}`);
console.log(`\n${results.filter((x) => x[0] === "PASS").length}/${results.length} PASS`);
