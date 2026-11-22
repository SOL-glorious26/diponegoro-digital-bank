'use strict';
const str = (v) => (typeof v === 'string' ? v : '');
const digits = (v) => str(v).replace(/\D/g, '');
const fmtRupiah = (n) => 'Rp ' + Math.round(Number(n) || 0).toLocaleString('id-ID');
const fmtTanggal = (d, jam = true) => {
  if (!d) return '-';
  return new Intl.DateTimeFormat('id-ID', {
    timeZone: 'Asia/Jakarta', day: '2-digit', month: 'short', year: 'numeric',
    ...(jam ? { hour: '2-digit', minute: '2-digit', hour12: false } : {}),
  }).format(new Date(d));
};

const flash = (req, msg, type = 'info') => { req.session.flash = { msg, type }; };
const regenerate = (req) => new Promise((ok, no) => req.session.regenerate((e) => (e ? no(e) : ok())));

/** Tahap setelah login: e-KYC disetujui dan PIN dibuat sebelum fitur transaksi (FR-02, FR-04). */
function nextStep(u) {
  if (!u) return '/login';
  if (u.ekycStatus !== 'APPROVED') return '/ekyc';
  if (!u.hasPin) return '/pin';
  return '/dashboard';
}

// ---------- tampilan transaksi dari TransactionDto backend ----------
const ICON = { TRANSFER: ['💸', '💰'], QRIS: ['📱', '📱'], TOPUP: ['🧾', '🧾'], DEPOSIT: ['💰', '💰'], POCKET: ['🐷', '🐷'], CARD: ['💳', '💳'] };
const txIcon = (t) => (ICON[t.category] || ['🔁', '🔁'])[t.direction === 'IN' ? 1 : 0];
const txSign = (t) => (t.direction === 'IN' ? '+' : t.direction === 'OUT' ? '-' : '');
const txAmount = (t) => t.total ?? t.amount;
const statusClass = (s) => (s === 'SUCCESS' ? 'ok' : s === 'FAILED' ? 'gagal' : 'pending');

// Top Up saldo memakai endpoint "dana masuk QRIS" backend; tandanya nama pengirim berawalan "TOP UP ".
const isTopUp = (t) => t && t.direction === 'IN' && /^TOP UP /i.test(t.fromName || '');
const sumberTopUp = (t) => String(t.fromName).slice(7).toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
const txTitle = (t) => (isTopUp(t) ? 'Top Up (' + sumberTopUp(t) + ')' : t.title);
const txKategori = (t) => (isTopUp(t) ? 'Top Up' : t.categoryLabel);

module.exports = { txTitle, txKategori, str, digits, fmtRupiah, fmtTanggal, flash, regenerate, nextStep, txIcon, txSign, txAmount, statusClass };
