// Tombol mata untuk semua kolom kata sandi / PIN: klik untuk menampilkan atau menyembunyikan.
(function () {
  var OPEN = '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12z"/><circle cx="12" cy="12" r="3"/></svg>';
  var SHUT = '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M17.94 17.94A10.9 10.9 0 0 1 12 19C5 19 1 12 1 12a18.5 18.5 0 0 1 5.06-5.94"/><path d="M9.9 4.24A9.1 9.1 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/><path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>';
  function init() {
    var inputs = document.querySelectorAll('input[type="password"]');
    Array.prototype.forEach.call(inputs, function (inp) {
      if (inp.dataset.pw) return;
      inp.dataset.pw = '1';
      var wrap = document.createElement('div');
      wrap.className = 'pw-wrap';
      inp.parentNode.insertBefore(wrap, inp);
      wrap.appendChild(inp);
      var btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'pw-eye';
      btn.setAttribute('aria-label', 'Tampilkan kata sandi');
      btn.setAttribute('aria-pressed', 'false');
      btn.tabIndex = -1;
      btn.innerHTML = OPEN;
      btn.addEventListener('click', function () {
        var show = inp.type === 'password';
        inp.type = show ? 'text' : 'password';
        btn.innerHTML = show ? SHUT : OPEN;
        btn.setAttribute('aria-label', show ? 'Sembunyikan kata sandi' : 'Tampilkan kata sandi');
        btn.setAttribute('aria-pressed', show ? 'true' : 'false');
        inp.focus();
      });
      wrap.appendChild(btn);
    });
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();
