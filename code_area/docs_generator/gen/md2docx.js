// Ubah tutorial Markdown menjadi .docx bergaya dokumen DDB (sampul, kontrol dokumen, daftar isi).
// Usage: node md2docx.js <input.md> <output.docx> <logo.jpeg>
// Mendukung: # ## ###, paragraf, **tebal**, `kode`, [tautan](url), daftar -, daftar 1., > kutipan,
// tabel |..|, blok ``` kode ```, garis ---. Bagian "**Daftar isi**" dilewati (diganti daftar isi Word).
const fs = require("fs");
const S = require("./ddbstyle");
const { D, FONT, PW, SZ, CAP } = S;
const { Paragraph, TextRun, Table, TableRow, TableCell, WidthType, ShadingType, BorderStyle, AlignmentType, HeadingLevel, ExternalHyperlink } = D;
const [, , IN, OUT, LOGO] = process.argv;

const BLACK = "000000";
// Emoji tidak tersedia di Palatino; hapus agar tidak muncul kotak kosong di Word.
const stripEmoji = (t) => t.replace(/[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{FE0F}\u{2B50}\u{2705}\u{274C}\u{26A0}\u{26D4}]/gu, "").replace(/^\s+/, "");

function inline(text, o = {}) {
  const size = o.size ?? SZ.body;
  const out = [];
  const re = /(\*\*[^*]+\*\*|`[^`]+`|\[[^\]]+\]\([^)]+\)|\*[^*\s][^*]*\*)/g;
  let last = 0, m;
  const plain = (t, extra = {}) => t && out.push(new TextRun({ text: t, font: FONT, size, color: BLACK, bold: o.bold, ...extra }));
  while ((m = re.exec(text))) {
    plain(text.slice(last, m.index));
    const t = m[0];
    if (t.startsWith("**")) plain(t.slice(2, -2), { bold: true });
    else if (t.startsWith("`")) out.push(new TextRun({ text: t.slice(1, -1), font: "Consolas", size: size - 2, color: BLACK, shading: { type: ShadingType.CLEAR, fill: "EFEFEF", color: "auto" } }));
    else if (t.startsWith("[")) {
      const [, label, url] = /\[([^\]]+)\]\(([^)]+)\)/.exec(t);
      if (url.startsWith("http")) out.push(new ExternalHyperlink({ link: url, children: [new TextRun({ text: label.replace(/`/g, ""), font: FONT, size, color: "1F3864", underline: {} })] }));
      else plain(label.replace(/`/g, ""));
    } else plain(t.slice(1, -1), { italics: true });
    last = m.index + t.length;
  }
  plain(text.slice(last));
  return out;
}

const para = (t, o = {}) => new Paragraph({ children: inline(t, o), alignment: o.align ?? AlignmentType.JUSTIFIED, spacing: { after: o.after ?? 120, line: 276 }, indent: o.indent, border: o.border, shading: o.shading });
const bd = { style: BorderStyle.SINGLE, size: 4, color: "808080" };

function codeBlock(lines) {
  return new Table({ width: { size: PW, type: WidthType.DXA }, columnWidths: [PW], rows: [new TableRow({ children: [new TableCell({
    width: { size: PW, type: WidthType.DXA }, borders: { top: bd, bottom: bd, left: bd, right: bd },
    shading: { fill: "F7F7F7", type: ShadingType.CLEAR, color: "auto" }, margins: { top: 80, bottom: 80, left: 140, right: 140 },
    children: lines.map((l) => new Paragraph({ spacing: { after: 0, line: 240 }, children: [new TextRun({ text: l.length ? stripEmoji(l) || " " : " ", font: "Consolas", size: 18, color: BLACK })] })) })] })] });
}

function mdTable(rows) {
  const cells = rows.map((r) => r.replace(/^\||\|$/g, "").split(/(?<!\\)\|/).map((c) => stripEmoji(c.trim().replace(/<br>/g, " — "))));
  const head = cells[0], body = cells.slice(2);
  const n = head.length;
  // Lebar kolom sebanding panjang isi (dibatasi) agar tabel rapi.
  const len = head.map((_, i) => Math.min(60, Math.max(6, ...[head, ...body].map((r) => (r[i] || "").length))));
  const tot = len.reduce((a, b) => a + b, 0);
  // Lebar minimum = kata terpanjang, supaya nama tidak terpotong; sisanya dibagi menurut panjang isi.
  const min = head.map((_, i) => Math.min(2200, 300 + 125 * Math.max(...[head, ...body].flatMap((r) => (r[i] || "").replace(/[*`]/g, "").split(/[\s/]+/)).map((x) => x.length))));
  const sisa = Math.max(0, PW - min.reduce((a, b) => a + b, 0)), kurang = min.reduce((a, b) => a + b, 0) > PW;
  const w = kurang ? len.map((l) => Math.floor(PW * l / tot)) : min.map((m, i) => m + Math.floor(sisa * len[i] / tot)); w[n - 1] += PW - w.reduce((a, b) => a + b, 0);
  const size = n >= 4 ? 17 : 18;
  const mk = (t, i, header) => new TableCell({ width: { size: w[i], type: WidthType.DXA }, borders: { top: bd, bottom: bd, left: bd, right: bd },
    shading: header ? { fill: "D9D9D9", type: ShadingType.CLEAR, color: "auto" } : undefined, margins: { top: 50, bottom: 50, left: 90, right: 90 },
    children: [new Paragraph({ alignment: header ? AlignmentType.CENTER : AlignmentType.LEFT, spacing: { after: 0, line: 252 }, children: inline(t || "", { size, bold: header }) })] });
  return new Table({ width: { size: PW, type: WidthType.DXA }, columnWidths: w, rows: [
    new TableRow({ tableHeader: true, cantSplit: true, children: head.map((t, i) => mk(t, i, true)) }),
    ...body.map((r) => new TableRow({ cantSplit: true, children: head.map((_, i) => mk(r[i], i, false)) })),
  ] });
}

const src = fs.readFileSync(IN, "utf8").replace(/\r/g, "").split("\n");
const body = [];
let i = 0, firstH2 = true, tableNo = 0, numInst = 100, skipping = false, h1Seen = false, intro = [];
while (i < src.length) {
  let l = src[i];
  if (/^\*\*Daftar isi\*\*/.test(l)) { skipping = true; i++; continue; }
  if (skipping) { if (/^---\s*$/.test(l)) skipping = false; i++; continue; }
  if (/^# /.test(l)) { h1Seen = true; i++; continue; }                     // judul → sampul
  if (/^```/.test(l)) { const buf = []; i++; while (i < src.length && !/^```/.test(src[i])) buf.push(src[i++]); i++; body.push(codeBlock(buf)); body.push(new Paragraph({ spacing: { after: 80 }, children: [] })); continue; }
  if (/^\|/.test(l)) { const rows = []; while (i < src.length && /^\|/.test(src[i])) rows.push(src[i++]); tableNo++; body.push(mdTable(rows)); body.push(new Paragraph({ spacing: { after: 160 }, children: [] })); continue; }
  if (/^## /.test(l)) { body.push(new Paragraph({ heading: HeadingLevel.HEADING_1, pageBreakBefore: !firstH2, children: [new TextRun({ text: stripEmoji(l.slice(3)), font: FONT, bold: true, size: SZ.h1, color: BLACK })] })); firstH2 = false; i++; continue; }
  if (/^### /.test(l)) { body.push(new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun({ text: stripEmoji(l.slice(4)), font: FONT, bold: true, size: SZ.h2, color: BLACK })] })); i++; continue; }
  if (/^---\s*$/.test(l) || !l.trim()) { i++; continue; }
  if (/^>/.test(l)) {
    const buf = []; while (i < src.length && /^>/.test(src[i])) buf.push(src[i++].replace(/^>\s?/, ""));
    // kutipan bisa berisi blok kode
    let j = 0; const parts = [];
    while (j < buf.length) {
      if (/^```/.test(buf[j])) { const c = []; j++; while (j < buf.length && !/^```/.test(buf[j])) c.push(buf[j++]); j++; parts.push(codeBlock(c)); continue; }
      if (buf[j].trim()) parts.push(para(stripEmoji(buf[j]), { size: 22, after: 60, indent: { left: 284 }, border: { left: { style: BorderStyle.SINGLE, size: 18, color: "B8860B", space: 8 } }, shading: { type: ShadingType.CLEAR, fill: "FFF8E5", color: "auto" } }));
      j++;
    }
    body.push(...parts, new Paragraph({ spacing: { after: 80 }, children: [] }));
    continue;
  }
  if (/^\s*[-*] /.test(l)) { while (i < src.length && /^\s*[-*] /.test(src[i])) { const lvl = /^\s{2,}/.test(src[i]) ? 1 : 0; body.push(new Paragraph({ numbering: { reference: "bul", level: lvl }, alignment: AlignmentType.LEFT, spacing: { after: 60, line: 276 }, children: inline(stripEmoji(src[i].replace(/^\s*[-*] /, ""))) })); i++; } continue; }
  if (/^\d+\. /.test(l)) { numInst++; while (i < src.length && /^\d+\. /.test(src[i])) { body.push(new Paragraph({ numbering: { reference: "num", level: 0, instance: numInst }, alignment: AlignmentType.LEFT, spacing: { after: 60, line: 276 }, children: inline(stripEmoji(src[i].replace(/^\d+\. /, ""))) })); i++; } continue; }
  // paragraf (gabungkan baris berurutan)
  const buf = [l]; i++;
  while (i < src.length && src[i].trim() && !/^(#|```|\||>|\s*[-*] |\d+\. |---)/.test(src[i])) buf.push(src[i++]);
  body.push(para(stripEmoji(buf.join(" "))));
}

const meta = {
  docLabel: "Artefak Pendukung – Web", title: "Panduan Menjalankan & Menguji Aplikasi Web",
  subtitle: "Tutorial langkah demi langkah untuk anggota tim: instalasi, menjalankan backend dan Web, tes manual, tes otomatis, troubleshooting",
  code: "TUT-DDB-002-WEB", version: "2.0", statusShort: "Final untuk Tim",
  status: "Final – diuji langsung di Windows 11 (22 November 2026); sumber: TUTORIAL_MENJALANKAN_WEB.md di repositori",
  module: "Tutorial Menjalankan & Menguji Aplikasi Web", createdBy: "Salomo Putra Glorious Hokoyoku (Project Manager)",
  updatedBy: "Salomo Putra Glorious Hokoyoku (Project Manager)", team: "Kelompok Jaran Goyang – seluruh anggota",
  refs: "DVM-DDB-001-WEB v1.0; DVM-DDB-001-BE v1.0; STD-DDB-001 v1.0; code_area/pengujian/README.md",
  date: "22 November 2026", logo: LOGO,
  revisions: [["1.0", "14 Nov 2026", "Salomo P.G. Hokoyoku", "Versi pertama."], ["2.0", "22 Nov 2026", "Salomo P.G. Hokoyoku", "Ditulis ulang untuk Web resmi tim Web (Node/Express/EJS). Langkah diverifikasi: WN 25/25, LPN 3/3, Chrome/Edge/Firefox lulus."]],
};
// Argumen ke-4 (opsional): berkas JSON berisi metadata sampul untuk dokumen lain (menimpa nilai di atas).
if (process.argv[5]) Object.assign(meta, JSON.parse(fs.readFileSync(process.argv[5], "utf8")), { logo: LOGO });
S.build(meta, body, OUT);
