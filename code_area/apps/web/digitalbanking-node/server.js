require('dotenv').config();
const path = require('path');
const crypto = require('crypto');
const express = require('express');
const session = require('express-session');
const h = require('./lib/helpers');
const { ApiError } = require('./lib/backend');

let secret = process.env.SESSION_SECRET;
if (!secret) {
  secret = crypto.randomBytes(32).toString('hex');
  console.warn('SESSION_SECRET kosong: dibuat acak untuk sesi ini (login hilang saat server dimatikan).');
}

const app = express();
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));
app.disable('x-powered-by');
Object.assign(app.locals, {
  fmtRupiah: h.fmtRupiah, fmtTanggal: h.fmtTanggal,
  txIcon: h.txIcon, txSign: h.txSign, txAmount: h.txAmount, statusClass: h.statusClass, txTitle: h.txTitle, txKategori: h.txKategori,
});

app.use('/assets', express.static(path.join(__dirname, 'public', 'assets')));
app.use(express.urlencoded({ extended: false }));
// Sesi disimpan di memori server: cukup untuk demo; login hilang bila server dimatikan.
app.use(session({
  secret,
  resave: false,
  saveUninitialized: false,
  cookie: { httpOnly: true, sameSite: 'lax', maxAge: 2 * 60 * 60 * 1000 },
}));

// ID perangkat tetap per peramban (dikirim saat login agar daftar sesi backend bisa membedakan perangkat)
app.use((req, res, next) => {
  const m = /(?:^|;\s*)ddb_device=([A-Za-z0-9-]{16,64})/.exec(req.headers.cookie || '');
  req.deviceId = m ? m[1] : crypto.randomUUID();
  if (!m) res.cookie('ddb_device', req.deviceId, { httpOnly: true, sameSite: 'lax', maxAge: 365 * 24 * 3600 * 1000 });
  next();
});

// flash message (sekali tampil) dan proteksi CSRF
app.use((req, res, next) => {
  res.locals.flash = req.session.flash || null;
  delete req.session.flash;
  res.locals.user = null;
  res.locals.account = null;
  if (!req.session.csrf) req.session.csrf = crypto.randomBytes(24).toString('hex');
  res.locals.csrf = req.session.csrf;
  if (req.method === 'POST' && req.body?._csrf !== req.session.csrf) {
    return res.status(403).send('Token keamanan tidak valid. Muat ulang halaman lalu coba lagi.');
  }
  next();
});

app.use(require('./routes/auth'));
app.use(require('./routes/app'));

app.use((req, res) => res.status(404).send('Halaman tidak ditemukan.'));
app.use((err, req, res, next) => {
  if (err instanceof ApiError && err.code === 'SESSION_ENDED') {
    delete req.session.tokens;
    delete req.session.user;
    h.flash(req, err.message, 'error');
    return res.redirect('/login');
  }
  if (err instanceof ApiError) {
    console.error('[backend]', err.status, err.code, err.message);
    return res.status(err.status >= 400 && err.status < 600 ? err.status : 502).send(err.message);
  }
  console.error(err);
  res.status(500).send('Terjadi kesalahan pada server.');
});

const port = Number(process.env.PORT || 3000);
app.listen(port, () => console.log(`Web berjalan di http://localhost:${port}  (backend: ${process.env.BACKEND_URL || 'http://localhost:8080/api/v1'})`));
