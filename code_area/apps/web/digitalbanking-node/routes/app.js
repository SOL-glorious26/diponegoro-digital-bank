const crypto = require('crypto');
const express = require('express');
const { call, ApiError } = require('../lib/backend');
const { str, digits, fmtRupiah, flash, nextStep } = require('../lib/helpers');

const router = express.Router();
const MIN_TRANSFER = 5000; // BR-01

// ---------- wajib login + e-KYC + PIN; muat ringkasan rekening dari backend ----------
router.use(['/dashboard', '/transfer', '/pembayaran', '/riwayat', '/profil'], async (req, res, next) => {
  if (!req.session.tokens) return res.redirect('/login');
  const step = nextStep(req.session.user);
  if (step !== '/dashboard') return res.redirect(step);
  const ov = await call(req, 'GET', '/accounts/me');
  const u = req.session.user;
  res.locals.user = { nama: ov.ownerName || u.username, username: u.username, email: u.email, no_hp: u.phoneNumber };
  res.locals.account = { saldo: ov.balance, no_rekening: ov.accountNumber, jenis_rekening: 'Tabungan', hidden: !!ov.balanceHidden };
  next();
});

/** Tampilkan kesalahan backend di form; kesalahan lain dilempar ke penangan umum. */
const orRender = (render) => (e) => { if (e instanceof ApiError && e.code !== 'SESSION_ENDED') return render(e.message); throw e; };

/** Pesan hasil transaksi: SUCCESS / PROCESSING / FAILED (mis. antarbank ditolak → dana dikembalikan). */
function txFlash(req, tx, label) {
  if (tx.status === 'SUCCESS') return flash(req, `${label} ${fmtRupiah(tx.total ?? tx.amount)} berhasil.`, 'success');
  if (tx.status === 'PROCESSING') return flash(req, `${label} sedang diproses bank tujuan. Buka detailnya lalu klik "Cek status" untuk memperbarui.`, 'info');
  return flash(req, `${label} gagal${tx.statusReason ? ': ' + tx.statusReason : '.'}`, 'error');
}

// ================= DASHBOARD =================
router.get('/dashboard', async (req, res) => {
  const hist = await call(req, 'GET', '/history', { params: { size: 5 } });
  res.render('dashboard', { page: 'dashboard', title: 'Dashboard', tx: hist?.items || [] });
});

// ================= TRANSFER (FR-10 .. FR-12) =================
const tabOf = (v) => (v === 'bank' ? 'bank' : 'antar');

async function renderTransfer(req, res, { error = '', tab, form } = {}) {
  const banks = await call(req, 'GET', '/transfers/banks');
  res.render('transfer', {
    page: 'transfer', title: 'Transfer', tab, error,
    banks: banks.filter((b) => !b.internal), pending: req.session.pendingTransfer || null, form: form || req.body || {},
  });
}

router.get('/transfer', (req, res) => renderTransfer(req, res, { tab: tabOf(req.query.tab) }));

router.post('/transfer', async (req, res) => {
  const stage = str(req.body.stage);
  let tab = tabOf(req.query.tab);
  const pending = req.session.pendingTransfer || null;

  if (stage === 'cancel') {
    delete req.session.pendingTransfer;
    return res.redirect('/transfer?tab=' + tab);
  }

  // ---- TAHAP 1: validasi rekening (inquiry) + hitung biaya (quote) ----
  if (stage === 'confirm') {
    tab = req.body.metode === 'bank_lain' ? 'bank' : 'antar';
    const accountNumber = digits(req.body.rekening_tujuan);
    const amount = Number(digits(req.body.nominal)) || 0;
    const note = str(req.body.catatan).trim().slice(0, 255);
    const fail = (error) => renderTransfer(req, res, { error, tab });
    try {
      let bankCode = str(req.body.bank_tujuan).trim();
      if (tab === 'antar') {
        const banks = await call(req, 'GET', '/transfers/banks');
        bankCode = banks.find((b) => b.internal)?.code || '';
      }
      if (!bankCode) return fail('Pilih bank tujuan.');
      if (!accountNumber) return fail('Isi nomor rekening tujuan.');
      if (amount < MIN_TRANSFER) return fail(`Nominal transfer minimal ${fmtRupiah(MIN_TRANSFER)}.`);
      await call(req, 'POST', '/transfers/inquiry', { body: { bankCode, accountNumber } });
      const quote = await call(req, 'POST', '/transfers/quote', { body: { bankCode, accountNumber, amount, note: note || undefined } });
      // satu Idempotency-Key per layar konfirmasi (BR-04): dipakai ulang bila PIN salah
      req.session.pendingTransfer = { bankCode, accountNumber, amount: quote.amount, note, quote, key: crypto.randomUUID(), tab };
      return res.redirect('/transfer?tab=' + tab);
    } catch (e) {
      return orRender(fail)(e);
    }
  }

  // ---- TAHAP 2: PIN -> eksekusi transfer ----
  if (stage === 'verify' && pending) {
    tab = pending.tab;
    const fail = (error) => renderTransfer(req, res, { error, tab });
    const pin = digits(req.body.pin);
    if (pin.length !== 6) return fail('PIN terdiri atas 6 digit.');
    try {
      const tx = await call(req, 'POST', '/transfers', {
        key: pending.key,
        body: { bankCode: pending.bankCode, accountNumber: pending.accountNumber, amount: pending.amount, note: pending.note || undefined, pin },
      });
      delete req.session.pendingTransfer;
      txFlash(req, tx, `Transfer ke ${pending.accountNumber}`);
      return res.redirect('/riwayat?detail=' + encodeURIComponent(tx.id));
    } catch (e) {
      return orRender(fail)(e);
    }
  }
  res.redirect('/transfer?tab=' + tab);
});

// ================= PEMBAYARAN: TOP-UP, TAGIHAN, QRIS (FR-15 .. FR-20) =================
const TABS = { topup: 'Top Up', pulsa: 'Pulsa', pln: 'Token PLN', tagihan: 'Tagihan', ewallet: 'E-wallet', qris: 'QRIS' };
const tabBayar = (v) => (Object.hasOwn(TABS, v) ? v : 'topup');

// Top Up saldo (simulasi): backend belum punya endpoint isi-saldo, jadi dipakai simulator dana masuk
// POST /qris/simulate-incoming (kredit ke rekening utama, nominal Rp1.000 – Rp10.000.000, tanpa PIN).
const SUMBER = ['Kartu Debit', 'Transfer Bank', 'Minimarket'];
const TOPUP_MIN = 1000, TOPUP_MAX = 10000000;
const KEY_RE = /^[A-Za-z0-9._:-]{1,64}$/;

async function renderBayar(req, res, tab, { error = '', form } = {}) {
  const pending = req.session.pendingPay || null;
  const data = { catalog: null, demoQr: [], sumberList: SUMBER, topupKey: crypto.randomUUID() };
  if (!pending) {
    if (tab === 'qris') data.demoQr = (await call(req, 'GET', '/qris/demo').catch(() => [])) || [];
    else if (tab !== 'topup') data.catalog = await call(req, 'GET', '/topup/catalog');
  }
  res.render('pembayaran', { page: 'pembayaran', title: 'Pembayaran', tabs: TABS, tab, error, pending, form: form || req.query || {}, ...data });
}

router.get('/pembayaran', (req, res) => renderBayar(req, res, tabBayar(req.query.tab)));

/** Susun permintaan quote sesuai tab; kembalikan {kind, request} atau lempar Error('pesan'). */
function buildOrder(tab, b) {
  const phone = digits(b.phone);
  const nominal = Number(digits(b.nominal)) || 0;
  switch (tab) {
    case 'pulsa':
      if (phone.length < 10) throw new Error('Nomor HP tidak valid.');
      if (!nominal) throw new Error('Pilih nominal pulsa.');
      return { kind: 'PULSA', phone, nominal };
    case 'pln':
      if (digits(b.meterNumber).length < 6) throw new Error('Nomor meter/ID pelanggan minimal 6 digit.');
      if (!nominal) throw new Error('Pilih nominal token.');
      return { kind: 'PLN', meterNumber: digits(b.meterNumber), nominal };
    case 'tagihan':
      if (!str(b.biller)) throw new Error('Pilih jenis tagihan.');
      if (digits(b.customerNumber).length < 6) throw new Error('Nomor pelanggan minimal 6 digit.');
      return { kind: 'BILL', biller: str(b.biller), customerNumber: digits(b.customerNumber) };
    default: // ewallet
      if (!str(b.wallet)) throw new Error('Pilih e-wallet.');
      if (phone.length < 10) throw new Error('Nomor HP e-wallet tidak valid.');
      if (!nominal) throw new Error('Isi nominal.');
      return { kind: 'EWALLET', wallet: str(b.wallet), phone, amount: nominal };
  }
}

router.post('/pembayaran', async (req, res) => {
  const tab = tabBayar(req.query.tab);
  const stage = str(req.body.stage);
  const pending = req.session.pendingPay || null;
  const fail = (error) => renderBayar(req, res, tab, { error, form: req.body });

  if (stage === 'cancel') { delete req.session.pendingPay; return res.redirect('/pembayaran?tab=' + tab); }

  // ---- TOP UP SALDO: satu langkah, tanpa PIN (menambah saldo, bukan mengeluarkan uang) ----
  if (tab === 'topup') {
    const amount = Number(digits(req.body.nominal)) || 0;
    const sumber = str(req.body.sumber);
    if (!SUMBER.includes(sumber)) return fail('Pilih sumber dana.');
    if (amount < TOPUP_MIN || amount > TOPUP_MAX) return fail(`Nominal top up ${fmtRupiah(TOPUP_MIN)} – ${fmtRupiah(TOPUP_MAX)}.`);
    const key = KEY_RE.test(str(req.body.key)) ? str(req.body.key) : crypto.randomUUID();
    try {
      await call(req, 'POST', '/qris/simulate-incoming', { key, body: { amount, payerName: 'TOP UP ' + sumber } });
      flash(req, `Top up ${fmtRupiah(amount)} berhasil, saldo bertambah.`, 'success');
      return res.redirect('/dashboard');
    } catch (e) {
      return orRender(fail)(e);
    }
  }

  // ---- TAHAP 1: ringkasan pesanan (quote) atau hasil pindai QRIS ----
  if (stage === 'confirm') {
    try {
      if (tab === 'qris') {
        const payload = str(req.body.payload_manual).trim() || str(req.body.payload).trim();
        if (!payload) return fail('Pilih contoh QRIS atau tempel isi kode QRIS.');
        const sc = await call(req, 'POST', '/qris/scan', { body: { payload } });
        const amount = sc.dynamic ? sc.amount : Number(digits(req.body.nominal)) || 0;
        if (!amount) return fail('QRIS ini tanpa nominal. Isi nominal pembayaran.');
        req.session.pendingPay = {
          mode: 'qris', tab, key: crypto.randomUUID(), label: `Pembayaran QRIS ke ${sc.merchantName}`,
          request: { payload, amount },
          rows: [['Merchant', sc.merchantName], ['Kota', sc.city || '-'], ['Penerbit', sc.issuer || '-'], ['Nominal', fmtRupiah(amount)]],
        };
      } else {
        const request = buildOrder(tab, req.body);
        const q = await call(req, 'POST', '/topup/quote', { body: request });
        const extra = q.details && typeof q.details === 'object'
          ? Object.entries(q.details).filter(([, v]) => v != null && typeof v !== 'object').map(([k, v]) => [k, String(v)]) : [];
        req.session.pendingPay = {
          mode: 'topup', tab, key: crypto.randomUUID(), label: q.title || 'Pembayaran', request,
          rows: [['Produk', q.title], ['Tujuan', q.target], ...(q.targetName ? [['Nama', q.targetName]] : []), ...extra,
            ['Harga', fmtRupiah(q.amount)], ['Biaya', fmtRupiah(q.fee)], ['Total', fmtRupiah(q.total)]],
        };
      }
      return res.redirect('/pembayaran?tab=' + tab);
    } catch (e) {
      if (e instanceof ApiError) return orRender(fail)(e);
      return fail(e.message);
    }
  }

  // ---- TAHAP 2: PIN -> bayar ----
  if (stage === 'pay' && pending) {
    const failPin = (error) => renderBayar(req, res, pending.tab, { error });
    const pin = digits(req.body.pin);
    if (pin.length !== 6) return failPin('PIN terdiri atas 6 digit.');
    try {
      const tx = await call(req, 'POST', pending.mode === 'qris' ? '/qris/pay' : '/topup/orders', {
        key: pending.key, body: { ...pending.request, pin },
      });
      delete req.session.pendingPay;
      txFlash(req, tx, pending.label);
      return res.redirect('/riwayat?detail=' + encodeURIComponent(tx.id));
    } catch (e) {
      return orRender(failPin)(e);
    }
  }
  res.redirect('/pembayaran?tab=' + tab);
});

// ================= RIWAYAT (FR-23, FR-24, FR-26) =================
const CATEGORIES = { TRANSFER: 'Transfer', QRIS: 'QRIS', TOPUP: 'Top-up & tagihan' };
const isoDate = (v) => (/^\d{4}-\d{2}-\d{2}$/.test(str(v)) ? str(v) : '');

function historyFilter(q) {
  const category = Object.hasOwn(CATEGORIES, q.jenis) ? q.jenis : '';
  const dari = isoDate(q.dari), sampai = isoDate(q.sampai);
  const params = { category, q: str(q.q).trim().slice(0, 60) };
  if (dari || sampai) Object.assign(params, { range: 'CUSTOM', from: dari || '2000-01-01', to: sampai || new Date().toISOString().slice(0, 10) });
  return { category, dari, sampai, search: params.q, params };
}

router.get('/riwayat', async (req, res) => {
  const f = historyFilter(req.query);
  const page = Math.max(0, parseInt(req.query.page, 10) || 0);
  const hist = await call(req, 'GET', '/history', { params: { ...f.params, page, size: 20 } });
  let detail = null;
  if (req.query.detail) {
    try { detail = await call(req, 'GET', '/history/' + encodeURIComponent(str(req.query.detail))); }
    catch (e) { if (!(e instanceof ApiError) || e.code === 'SESSION_ENDED') throw e; }
  }
  const qp = new URLSearchParams();
  if (f.category) qp.set('jenis', f.category);
  if (f.dari) qp.set('dari', f.dari);
  if (f.sampai) qp.set('sampai', f.sampai);
  if (f.search) qp.set('q', f.search);
  res.render('riwayat', {
    page: 'riwayat', title: 'Riwayat Transaksi', rows: hist?.items || [], hasMore: !!hist?.hasMore, pageNo: page, detail,
    categories: CATEGORIES, filter: f.category, dari: f.dari, sampai: f.sampai, search: f.search, qs: qp.toString(),
  });
});

router.post('/riwayat/cek-status', async (req, res) => {
  const id = str(req.body.id);
  try {
    const tx = await call(req, 'POST', `/transfers/${encodeURIComponent(id)}/check-status`);
    flash(req, `Status transaksi: ${tx.statusLabel || tx.status}.`, tx.status === 'FAILED' ? 'error' : tx.status === 'SUCCESS' ? 'success' : 'info');
  } catch (e) {
    if (!(e instanceof ApiError) || e.code === 'SESSION_ENDED') throw e;
    flash(req, e.message, 'error');
  }
  res.redirect('/riwayat?detail=' + encodeURIComponent(id));
});

// Ekspor CSV/PDF sesuai filter aktif (FR-26): backend membuat berkas, web hanya meneruskan.
router.get('/riwayat/export', async (req, res) => {
  const f = historyFilter(req.query);
  const format = req.query.format === 'pdf' ? 'pdf' : 'csv';
  const r = await call(req, 'GET', '/history/export', { params: { ...f.params, format }, raw: true, accept: '*/*' });
  res.setHeader('Content-Type', r.headers.get('content-type') || (format === 'pdf' ? 'application/pdf' : 'text/csv'));
  res.setHeader('Content-Disposition', r.headers.get('content-disposition') || `attachment; filename="riwayat-ddb.${format}"`);
  res.send(Buffer.from(await r.arrayBuffer()));
});

// ================= PROFIL (FR-27, FR-28) =================
async function renderProfil(req, res, error = '') {
  const me = await call(req, 'GET', '/me');
  req.session.user = me;
  res.locals.user = { ...res.locals.user, username: me.username, email: me.email, no_hp: me.phoneNumber };
  res.render('profil', { page: 'profil', title: 'Profil', error, me });
}

router.get('/profil', (req, res) => renderProfil(req, res));

const otpPage = (req, res, error = '') => res.render('otp', {
  title: 'Konfirmasi Perubahan Profil', action: '/profil/otp', submitLabel: 'Konfirmasi', backHref: '/profil',
  sub: `Masukkan 6 digit kode yang dikirim ke ${req.session.profilChange.sentTo || 'kontak Anda'}.`, demoOtp: req.session.profilChange.demoCode, error,
});

router.post('/profil', async (req, res) => {
  const fail = (error) => renderProfil(req, res, error);
  try {
    if (req.body.aksi === 'profil') {
      const draft = { username: str(req.body.username).trim().toLowerCase(), email: str(req.body.email).trim() || undefined, phoneNumber: str(req.body.no_hp).trim() || undefined };
      if (!draft.username) return fail('Username wajib diisi.');
      const o = await call(req, 'POST', '/me/profile-change', { body: draft });
      req.session.profilChange = { draft, demoCode: o?.demoCode || null, sentTo: o?.sentTo || '' };
      return res.redirect('/profil/otp');
    }
    if (req.body.aksi === 'sandi') {
      const lama = str(req.body.sandi_lama), b1 = str(req.body.sandi_baru), b2 = str(req.body.sandi_baru2);
      if (b1 !== b2) return fail('Konfirmasi kata sandi baru tidak cocok.');
      await call(req, 'POST', '/me/password', { body: { oldPassword: lama, newPassword: b1 } });
      flash(req, 'Kata sandi berhasil diubah.', 'success');
      return res.redirect('/profil');
    }
  } catch (e) {
    return orRender(fail)(e);
  }
  res.redirect('/profil');
});

router.get('/profil/otp', (req, res) => (req.session.profilChange ? otpPage(req, res) : res.redirect('/profil')));

router.post('/profil/otp', async (req, res) => {
  if (!req.session.profilChange) return res.redirect('/profil');
  try {
    if (req.body.resend) {
      const o = await call(req, 'POST', '/me/profile-change', { body: req.session.profilChange.draft });
      req.session.profilChange.demoCode = o?.demoCode || null;
      flash(req, 'Kode OTP baru telah dikirim.', 'info');
      return res.redirect('/profil/otp');
    }
    req.session.user = await call(req, 'POST', '/me/profile-change/confirm', { body: { code: str(req.body.kode).trim() } });
    delete req.session.profilChange;
    flash(req, 'Profil berhasil diperbarui.', 'success');
    res.redirect('/profil');
  } catch (e) {
    orRender((msg) => otpPage(req, res, msg))(e);
  }
});

module.exports = router;
