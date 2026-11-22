const express = require('express');
const { call, ApiError } = require('../lib/backend');
const { str, digits, flash, regenerate, nextStep } = require('../lib/helpers');

const router = express.Router();
const DEVICE_NAME = 'Web diponegoro corp bank';
const guestOnly = (req, res, next) => (req.session.tokens ? res.redirect(nextStep(req.session.user)) : next());
const needLogin = (req, res, next) => (req.session.tokens ? next() : res.redirect('/login'));
const handle = (render) => (e, res) => { if (e instanceof ApiError) return render(e.message); throw e; };

async function startSession(req, t) {
  await regenerate(req); // cegah session fixation
  req.session.tokens = { accessToken: t.accessToken, refreshToken: t.refreshToken };
  req.session.user = t.user;
}

router.get('/', (req, res) => res.redirect(req.session.tokens ? nextStep(req.session.user) : '/login'));

// ---------------- LOGIN (tanpa OTP, sesuai SRS FR-03) ----------------
router.get('/login', guestOnly, (req, res) => res.render('login', { error: '', identitas: '' }));

router.post('/login', guestOnly, async (req, res) => {
  const identitas = str(req.body.identitas).trim();
  try {
    const t = await call(req, 'POST', '/auth/login', {
      auth: false,
      body: { identifier: identitas, password: str(req.body.password), deviceId: req.deviceId, deviceName: DEVICE_NAME },
    });
    await startSession(req, t);
    res.redirect(nextStep(t.user));
  } catch (e) {
    handle((msg) => res.render('login', { error: msg, identitas }))(e, res);
  }
});

// ---------------- REGISTRASI + OTP ----------------
const otpView = (res, o) => res.render('otp', { error: '', demoOtp: null, ...o });

router.get('/register', guestOnly, (req, res) => res.render('register', { error: '', f: {} }));

router.post('/register', guestOnly, async (req, res) => {
  const f = { username: str(req.body.username).trim(), email: str(req.body.email).trim(), no_hp: str(req.body.no_hp).trim() };
  const p1 = str(req.body.password), p2 = str(req.body.password2);
  const fail = (error) => res.render('register', { error, f });
  if (!f.username || !p1) return fail('Username dan kata sandi wajib diisi.');
  if (f.username.length < 3) return fail('Username minimal 3 karakter.');
  if (!f.email && !f.no_hp) return fail('Isi email atau No. HP untuk menerima kode OTP.');
  if (p1 !== p2) return fail('Konfirmasi kata sandi tidak cocok.');
  try {
    const r = await call(req, 'POST', '/auth/register', {
      auth: false,
      body: { username: f.username.toLowerCase(), email: f.email || undefined, phoneNumber: f.no_hp || undefined, password: p1 },
    });
    req.session.reg = { userId: r.userId, demoCode: r.otp?.demoCode || null, sentTo: r.otp?.sentTo || '' };
    res.redirect('/register/otp');
  } catch (e) {
    handle(fail)(e, res);
  }
});

const needReg = (req, res, next) => (req.session.reg ? next() : res.redirect('/register'));
const regView = (req, res, error = '') => otpView(res, {
  title: 'Verifikasi Pendaftaran', action: '/register/otp', submitLabel: 'Verifikasi & Lanjut', backHref: '/register',
  sub: `Masukkan 6 digit kode yang dikirim ke ${req.session.reg.sentTo || 'kontak Anda'}.`, demoOtp: req.session.reg.demoCode, error,
});

router.get('/register/otp', guestOnly, needReg, (req, res) => regView(req, res));

router.post('/register/otp', guestOnly, needReg, async (req, res) => {
  try {
    if (req.body.resend) {
      const o = await call(req, 'POST', '/auth/register/resend-otp', { auth: false, body: { userId: req.session.reg.userId } });
      req.session.reg.demoCode = o?.demoCode || null;
      flash(req, 'Kode OTP baru telah dikirim.', 'info');
      return res.redirect('/register/otp');
    }
    const t = await call(req, 'POST', '/auth/register/verify-otp', {
      auth: false,
      body: { userId: req.session.reg.userId, code: str(req.body.kode).trim(), deviceId: req.deviceId, deviceName: DEVICE_NAME },
    });
    await startSession(req, t);
    flash(req, 'Pendaftaran berhasil! Lengkapi verifikasi e-KYC untuk mulai bertransaksi.', 'success');
    res.redirect('/ekyc');
  } catch (e) {
    handle((msg) => regView(req, res, msg))(e, res);
  }
});

// ---------------- E-KYC (FR-02) dan BUAT PIN (FR-04) ----------------
router.get('/ekyc', needLogin, async (req, res) => {
  let ekyc = null;
  try { ekyc = await call(req, 'GET', '/auth/ekyc'); } catch (e) { if (!(e instanceof ApiError) || e.status !== 404) throw e; }
  if (ekyc?.status === 'APPROVED') {
    req.session.user = await call(req, 'GET', '/me');
    return res.redirect(nextStep(req.session.user));
  }
  res.render('ekyc', { error: '', ekyc, f: {} });
});

router.post('/ekyc', needLogin, async (req, res) => {
  const f = { nik: digits(req.body.nik), fullName: str(req.body.fullName).trim(), selfie: str(req.body.selfie).trim() || 'selfie.jpg' };
  const fail = (error) => res.render('ekyc', { error, ekyc: null, f });
  if (f.nik.length !== 16) return fail('NIK terdiri atas 16 digit.');
  try {
    await call(req, 'POST', '/auth/ekyc', { body: { nik: f.nik, fullName: f.fullName || undefined, selfieReference: f.selfie } });
    res.redirect('/ekyc');
  } catch (e) {
    handle(fail)(e, res);
  }
});

router.get('/pin', needLogin, (req, res) => {
  const step = nextStep(req.session.user);
  if (step !== '/pin') return res.redirect(step);
  res.render('pin', { error: '' });
});

router.post('/pin', needLogin, async (req, res) => {
  const p1 = digits(req.body.pin), p2 = digits(req.body.pin2);
  const fail = (error) => res.render('pin', { error });
  if (p1.length !== 6) return fail('PIN terdiri atas 6 digit angka.');
  if (p1 !== p2) return fail('Konfirmasi PIN tidak cocok.');
  try {
    await call(req, 'POST', '/auth/pin', { body: { pin: p1 } });
    req.session.user = await call(req, 'GET', '/me');
    flash(req, 'PIN berhasil dibuat. Akun siap dipakai bertransaksi.', 'success');
    res.redirect('/dashboard');
  } catch (e) {
    handle(fail)(e, res);
  }
});

// ---------------- LUPA PASSWORD / PIN (FR-05) ----------------
const lupaView = (req, res, error = '') => res.render('lupa_sandi', {
  error, step: req.session.rec ? 'reset' : 'request', demoOtp: req.session.rec?.demoCode || null, type: req.session.rec?.type || 'PASSWORD',
});

router.get('/lupa-sandi', (req, res) => {
  if (req.query.ulang) delete req.session.rec;
  lupaView(req, res);
});

router.post('/lupa-sandi', async (req, res) => {
  try {
    if (typeof req.body.identitas === 'string') {
      const type = req.body.type === 'PIN' ? 'PIN' : 'PASSWORD';
      const identifier = req.body.identitas.trim();
      const r = await call(req, 'POST', '/auth/recovery/request', { auth: false, body: { identifier, type } });
      req.session.rec = { identifier, type, demoCode: r?.otp?.demoCode || null };
      return lupaView(req, res);
    }
    const rec = req.session.rec;
    if (typeof req.body.kode === 'string' && rec) {
      const s1 = str(req.body.password), s2 = str(req.body.password2);
      if (!s1 || s1 !== s2) return lupaView(req, res, 'Isian baru dan konfirmasinya harus sama.');
      if (rec.type === 'PIN' && !/^\d{6}$/.test(s1)) return lupaView(req, res, 'PIN baru harus 6 digit angka.');
      const v = await call(req, 'POST', '/auth/recovery/verify', { auth: false, body: { identifier: rec.identifier, type: rec.type, code: req.body.kode.trim() } });
      await call(req, 'POST', '/auth/recovery/reset', { auth: false, body: { identifier: rec.identifier, type: rec.type, recoveryToken: v.recoveryToken, newSecret: s1 } });
      delete req.session.rec;
      flash(req, rec.type === 'PIN' ? 'PIN berhasil diubah.' : 'Kata sandi berhasil diubah, silakan login.', 'success');
      return res.redirect('/login');
    }
    res.redirect('/lupa-sandi');
  } catch (e) {
    handle((msg) => lupaView(req, res, msg))(e, res);
  }
});

// ---------------- LOGOUT ----------------
router.get('/logout', async (req, res) => {
  if (req.session.tokens) { try { await call(req, 'POST', '/auth/logout'); } catch { /* sesi lokal tetap diakhiri */ } }
  req.session.destroy(() => res.redirect('/login'));
});

module.exports = router;
