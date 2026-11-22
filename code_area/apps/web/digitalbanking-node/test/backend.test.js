// Uji lib/backend.js terhadap backend tiruan (tanpa dependensi): node test/backend.test.js
const http = require('http');
const assert = require('assert');
const { call, ApiError } = require('../lib/backend');

let refreshCalls = 0;
const server = http.createServer((req, res) => {
  let raw = '';
  req.on('data', (c) => (raw += c));
  req.on('end', () => {
    const body = raw ? JSON.parse(raw) : null;
    const json = (code, obj) => { res.writeHead(code, { 'Content-Type': 'application/json' }); res.end(JSON.stringify(obj)); };
    const auth = req.headers.authorization;
    if (req.url === '/api/v1/auth/login') {
      return body.password === 'Demo@1234'
        ? json(200, { accessToken: 'A1', refreshToken: 'R1', user: { username: 'demo' } })
        : json(401, { code: 'BAD_CREDENTIALS', message: 'Username atau password salah' });
    }
    if (req.url === '/api/v1/auth/refresh') { refreshCalls++; return body.refreshToken === 'R1' ? json(200, { accessToken: 'A2', refreshToken: 'R2' }) : json(401, { code: 'X', message: 'x' }); }
    if (req.url === '/api/v1/accounts/me') return auth === 'Bearer A2' ? json(200, { balance: 5000000 }) : json(401, { code: 'UNAUTHORIZED', message: 'x' });
    if (req.url.startsWith('/api/v1/history?')) return json(200, { q: req.url.split('?')[1], key: req.headers['idempotency-key'] || null });
    if (req.url === '/api/v1/transfers') return json(200, { key: req.headers['idempotency-key'], auth, body });
    json(404, { code: 'NOT_FOUND', message: 'tidak ada' });
  });
});

(async () => {
  await new Promise((r) => server.listen(0, r));
  process.env.BACKEND_URL = `http://localhost:${server.address().port}/api/v1`;
  const req = () => ({ sessionID: 's' + Math.random(), session: {} });

  // login berhasil tanpa token
  const t = await call(req(), 'POST', '/auth/login', { auth: false, body: { password: 'Demo@1234' } });
  assert.strictEqual(t.accessToken, 'A1');

  // login salah -> ApiError dengan pesan backend, tanpa mencoba refresh
  await assert.rejects(call(req(), 'POST', '/auth/login', { auth: false, body: { password: 'x' } }),
    (e) => e instanceof ApiError && e.status === 401 && e.code === 'BAD_CREDENTIALS' && e.message === 'Username atau password salah');
  assert.strictEqual(refreshCalls, 0);

  // 401 -> refresh sekali -> ulangi permintaan; token baru tersimpan di sesi
  const r1 = req(); r1.session.tokens = { accessToken: 'A1', refreshToken: 'R1' };
  assert.strictEqual((await call(r1, 'GET', '/accounts/me')).balance, 5000000);
  assert.deepStrictEqual(r1.session.tokens, { accessToken: 'A2', refreshToken: 'R2' });
  assert.strictEqual(refreshCalls, 1);

  // refresh ditolak -> SESSION_ENDED
  const r2 = req(); r2.session.tokens = { accessToken: 'A1', refreshToken: 'BAD' };
  await assert.rejects(call(r2, 'GET', '/accounts/me'), (e) => e.code === 'SESSION_ENDED');

  // Idempotency-Key, parameter query, dan body terkirim
  const r3 = req(); r3.session.tokens = { accessToken: 'A2', refreshToken: 'R2' };
  const h = await call(r3, 'GET', '/history', { params: { size: 5, category: '', q: 'a b' } });
  assert.strictEqual(h.q, 'size=5&q=a+b');
  const tr = await call(r3, 'POST', '/transfers', { key: 'K-1', body: { amount: 10000 } });
  assert.deepStrictEqual([tr.key, tr.auth, tr.body.amount], ['K-1', 'Bearer A2', 10000]);

  // error non-401 -> pesan backend
  await assert.rejects(call(r3, 'GET', '/nope'), (e) => e.status === 404 && e.code === 'NOT_FOUND');

  // backend mati -> pesan jelas
  server.close();
  process.env.BACKEND_URL = 'http://localhost:1/api/v1';
  await assert.rejects(call(req(), 'POST', '/auth/login', { auth: false, body: {} }), (e) => e.code === 'BACKEND_UNREACHABLE');

  console.log('Semua uji lib/backend.js lulus.');
})().catch((e) => { console.error(e); process.exit(1); });
