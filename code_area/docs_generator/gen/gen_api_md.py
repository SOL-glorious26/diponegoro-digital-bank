"""Bangkitkan backend/docs/API.md dari openapi.json (UTF-8)."""
import json, sys
src, dst = sys.argv[1], sys.argv[2]
d = json.load(open(src, encoding='utf-8'))

def ref(s):
    if not s:
        return '–'
    if '$ref' in s:
        return '`' + s['$ref'].split('/')[-1] + '`'
    if s.get('type') == 'array':
        return 'list ' + ref(s['items'])
    return s.get('type', '–')

by_tag = {}
for path, item in d['paths'].items():
    for method, op in item.items():
        tag = (op.get('tags') or ['Lainnya'])[0]
        body = op.get('requestBody', {}).get('content', {}).get('application/json', {}).get('schema')
        resp = next(iter(op.get('responses', {}).get('200', op.get('responses', {}).get('201', {})).get('content', {}).values()), {}).get('schema')
        headers = [p['name'] for p in op.get('parameters', []) if p['in'] == 'header']
        query = [p['name'] + ('' if p.get('required') else '?') for p in op.get('parameters', []) if p['in'] == 'query']
        by_tag.setdefault(tag, []).append((path, method.upper(), op.get('summary', ''), ref(body), ref(resp), headers, query))

ops = sum(len(v) for v in by_tag.values())
out = [
    '# API Diponegoro Digital Bank',
    '',
    f'Dibangkitkan dari `openapi.json` (OpenAPI {d.get("openapi", "")}). Total **{ops} operasi**. '
    'Dokumentasi interaktif: `/swagger-ui.html` (spesifikasi mentah: `/v3/api-docs`).',
    '',
    '## Aturan umum',
    '',
    '- Base URL `/api/v1`. Semua endpoint selain registrasi, login, refresh, dan pemulihan memerlukan '
    '`Authorization: Bearer <accessToken>` (JWT HS256, 15 menit).',
    '- Saat respons **401**, panggil `POST /api/v1/auth/refresh` dengan refresh token (7 hari, dirotasi); bila gagal → login ulang.',
    '- Setiap transaksi keuangan wajib header **`Idempotency-Key`** (≤ 64 karakter `[A-Za-z0-9._:-]`, mis. UUID per layar konfirmasi; '
    'pakai ulang kunci yang sama saat retry → tidak ada debit ganda).',
    '- Uang = bilangan bulat rupiah. Waktu = ISO-8601 UTC (tampilkan dalam WIB). Tanggal = `yyyy-MM-dd`.',
    '- Error: `{"code": "INSUFFICIENT_BALANCE", "message": "Saldo tidak mencukupi", "fields": {...}}` — '
    '`message` siap ditampilkan, `code` stabil untuk logika klien.',
    '- Mode simulasi: kode OTP ikut di respons (`demoCode`) bila `EXPOSE_OTP=true`.',
    '',
]
for tag in sorted(by_tag):
    rows = sorted(by_tag[tag], key=lambda r: (r[0], r[1]))
    out += [f'## {tag} ({len(rows)} operasi)', '', '| Metode | Path | Fungsi | Body | Respons | Header / query |', '|---|---|---|---|---|---|']
    for path, method, summary, body, resp, headers, query in rows:
        extra = ', '.join(['`' + h + '`' for h in headers] + query) or '–'
        out.append(f'| {method} | `{path}` | {summary.replace("|", "/") or "–"} | {body} | {resp} | {extra} |')
    out.append('')
open(dst, 'w', encoding='utf-8', newline='\n').write('\n'.join(out))
print('ok', ops, 'operasi')
