// Shared house style for all Diponegoro Digital Bank documentation volumes.
// Palatino Linotype; headings bold black (H1 16pt, H2 14pt, H3 12pt); body 12pt, spacing 1.15;
// captions 10pt placed BELOW figures and tables.
const fs = require("fs");
const D = require("docx");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell, WidthType, ShadingType, AlignmentType,
  HeadingLevel, Header, Footer, PageNumber, TableOfContents, LevelFormat, BorderStyle, ImageRun, VerticalAlign,
} = D;

const FONT = "Palatino Linotype";
const BLACK = "000000";
const HEAD_FILL = "D9D9D9";
const KEY_FILL = "F2F2F2";
const PW = 9070; // A4, margins 2.5 cm
const SZ = { body: 24, cap: 20, table: 20, h1: 32, h2: 28, h3: 24, small: 18 };

function runs(text, o = {}) {
  return String(text).split(/(\*\*[^*]+\*\*)/g).filter(Boolean).map((p) => p.startsWith("**")
    ? new TextRun({ text: p.slice(2, -2), bold: true, font: FONT, size: o.size, color: BLACK })
    : new TextRun({ text: p, font: FONT, size: o.size, color: BLACK, bold: o.bold, italics: o.italics }));
}
const P = (t, o = {}) => new Paragraph({ children: runs(t, o), alignment: o.align ?? AlignmentType.JUSTIFIED, spacing: { after: o.after ?? 120, line: 276 }, keepNext: o.keepNext });
const H1 = (t, pb = true) => new Paragraph({ heading: HeadingLevel.HEADING_1, pageBreakBefore: pb, children: [new TextRun({ text: t, font: FONT, bold: true, size: SZ.h1, color: BLACK })] });
const H2 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun({ text: t, font: FONT, bold: true, size: SZ.h2, color: BLACK })] });
const H3 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_3, children: [new TextRun({ text: t, font: FONT, bold: true, size: SZ.h3, color: BLACK })] });
const B = (t, lvl = 0) => new Paragraph({ numbering: { reference: "bul", level: lvl }, children: runs(t), alignment: AlignmentType.JUSTIFIED, spacing: { after: 60, line: 276 } });
let inst = 0;
const NL = (items) => { inst++; return items.map((t) => new Paragraph({ numbering: { reference: "num", level: 0, instance: inst }, children: runs(t), alignment: AlignmentType.JUSTIFIED, spacing: { after: 60, line: 276 } })); };

// Captions (below figure/table), auto-numbered per chapter by caller.
const CAP = (t) => new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 80, after: 240 }, children: runs(t, { size: SZ.cap }) });
const NOTE = (t) => new Paragraph({ alignment: AlignmentType.LEFT, spacing: { before: 40, after: 200 }, children: runs(t, { size: SZ.cap, italics: true }) });

const bd = { style: BorderStyle.SINGLE, size: 4, color: "808080" };
const borders = { top: bd, bottom: bd, left: bd, right: bd };
const nb0 = { style: BorderStyle.NONE, size: 0, color: "FFFFFF" };
const noBorders = { top: nb0, bottom: nb0, left: nb0, right: nb0 };
function cell(text, w, o = {}) {
  const lines = Array.isArray(text) ? text : [text];
  return new TableCell({
    width: { size: w, type: WidthType.DXA }, borders: o.noBorder ? noBorders : borders, columnSpan: o.span,
    shading: o.fill ? { fill: o.fill, type: ShadingType.CLEAR, color: "auto" } : undefined,
    margins: { top: 50, bottom: 50, left: 90, right: 90 }, verticalAlign: o.v ?? VerticalAlign.TOP,
    children: o.children ?? lines.map((l) => new Paragraph({ alignment: o.align ?? AlignmentType.LEFT, spacing: { after: 0, line: 252 }, children: runs(l, { size: o.size ?? SZ.table, bold: o.bold }) })),
  });
}
function table(headers, rows, widths, o = {}) {
  const total = widths.reduce((a, b) => a + b, 0);
  if (total !== (o.width ?? PW)) throw new Error(`table width ${total} != ${o.width ?? PW} (${headers[0]})`);
  const size = o.size ?? SZ.table;
  const out = [];
  if (headers) out.push(new TableRow({ tableHeader: true, cantSplit: true, children: headers.map((h, i) => cell(h, widths[i], { fill: HEAD_FILL, bold: true, size, align: AlignmentType.CENTER })) }));
  rows.forEach((r) => out.push(new TableRow({ cantSplit: true, children: r.map((c, i) => {
    const s = (c && typeof c === "object" && !Array.isArray(c)) ? c : { t: c };
    return cell(s.t ?? "", widths[i], { size, fill: s.fill, bold: s.bold ?? (o.boldFirst && i === 0), align: s.align ?? (o.center?.includes(i) ? AlignmentType.CENTER : (o.right?.includes(i) ? AlignmentType.RIGHT : undefined)), span: s.span });
  }) })));
  return new Table({ width: { size: total, type: WidthType.DXA }, columnWidths: widths, rows: out });
}
const kv = (rows, k = 2900) => table(null, rows.map(([a, b]) => [{ t: a, bold: true, fill: KEY_FILL }, b]), [k, PW - k], { size: SZ.table + 2 });

function img(path, widthPx, opts = {}) {
  const data = fs.readFileSync(path);
  const w = data.readUInt32BE(16), h = data.readUInt32BE(20); // PNG IHDR
  const width = widthPx, height = Math.round(widthPx * h / w);
  return new Paragraph({ alignment: AlignmentType.CENTER, keepNext: true, spacing: { before: 120, after: 0 },
    children: [new ImageRun({ type: opts.type ?? "png", data, transformation: { width, height }, altText: { title: opts.alt ?? "Gambar", description: opts.alt ?? "Gambar", name: opts.alt ?? "Gambar" } })] });
}
function imgJpeg(path, width, height, alt) {
  return new ImageRun({ type: "jpg", data: fs.readFileSync(path), transformation: { width, height }, altText: { title: alt, description: alt, name: alt } });
}

// Persetujuan versi final. m.approval = { sign: <png tanda tangan PM>, date, pic?: [[peran, nama], ...] }.
// Hanya tanda tangan PM yang dipasang; kolom orang lain sengaja dikosongkan.
function approval(a) {
  const w = [3000, 3100, 1670, 1300];
  const row = (peran, nama, sign, date) => new TableRow({ cantSplit: true, children: [
    cell(peran, w[0]), cell(nama, w[1]),
    cell("", w[2], { children: [new Paragraph({ keepNext: true, alignment: AlignmentType.CENTER, spacing: { before: sign ? 0 : 240, after: sign ? 0 : 240 },
      children: sign ? [new ImageRun({ type: "png", data: fs.readFileSync(sign), transformation: { width: 74, height: 75 }, altText: { title: "Tanda tangan", description: "Tanda tangan Project Manager", name: "ttd" } })] : [] })] }),
    cell(date || "", w[3], { align: AlignmentType.CENTER })] });
  return [
    new Paragraph({ pageBreakBefore: !!a.newPage, spacing: { before: 120, after: 120 }, keepNext: true, children: [new TextRun({ text: "Persetujuan", font: FONT, size: SZ.h2, bold: true, color: BLACK })] }),
    new Table({ width: { size: PW, type: WidthType.DXA }, columnWidths: w, rows: [
      new TableRow({ tableHeader: true, children: ["Peran", "Nama", "Tanda tangan", "Tanggal"].map((h, i) => cell(h, w[i], { fill: HEAD_FILL, bold: true, align: AlignmentType.CENTER })) }),
      row("Disetujui oleh (Project Manager)", "Salomo Putra Glorious Hokoyoku", a.sign, a.date),
      ...(a.pic ?? []).map(([p, n]) => row(p, n, null, ""))] }),
    CAP("Tabel 0.3 Persetujuan (kolom kosong: belum ditandatangani)"),
  ];
}

function header(code, title) {
  return new Header({ children: [new Paragraph({ border: { bottom: { style: BorderStyle.SINGLE, size: 4, color: BLACK, space: 4 } },
    children: [new TextRun({ text: `${code}  |  ${title}  |  PT Diponegoro Digital Bank`, font: FONT, size: SZ.small, color: BLACK })] })] });
}
function footer(version) {
  const r = (t) => new TextRun({ text: t, font: FONT, size: SZ.small, color: BLACK });
  return new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [
    r(`Kelompok Jaran Goyang – Versi ${version} – Halaman `), new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: SZ.small }),
    r(" dari "), new TextRun({ children: [PageNumber.TOTAL_PAGES], font: FONT, size: SZ.small })] })] });
}

// Cover + document control block (identical structure for every volume).
function frontMatter(m) {
  const c = (t, size, bold, before = 0, after = 0) => new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before, after }, children: [new TextRun({ text: t, font: FONT, size, bold, color: BLACK })] });
  const cover = [
    c("SOFTWARE ENGINEERING", 28, true, 200, 120),
    c("Universitas Kristen Satya Wacana", 24, false, 0, 600),
    new Paragraph({ alignment: AlignmentType.CENTER, children: [imgJpeg(m.logo, 220, 147, "Logo Diponegoro Corporation")] }),
    c(m.docLabel.toUpperCase(), 32, true, 500, 120),
    c(m.title, 40, true, 0, 120),
    c(m.subtitle, 24, false, 0, 600),
    c("End-to-End Development of Greenfield Retail Digital Banking Platform (Web & Mobile Apps)", 24, true, 0, 60),
    c("PT Diponegoro Digital Bank – Diponegoro Corporation (Holding Company)", 24, false, 0, 600),
    kv([["Kode dokumen", m.code], ["Versi / Status", `${m.version} / ${m.statusShort}`], ["Tim proyek", "Kelompok Jaran Goyang"], ["Project Manager", "Salomo Putra Glorious Hokoyoku (622024022)"], ["Tanggal", m.date]]),
  ];
  const ctl = [
    new Paragraph({ pageBreakBefore: true, spacing: { after: 160 }, children: [new TextRun({ text: "Kontrol Dokumen", font: FONT, size: SZ.h1, bold: true, color: BLACK })] }),
    kv([["Nama module", m.module], ["Status", m.status], ["Created by", m.createdBy], ["Last updated by", m.updatedBy], ["Responsible team", m.team], ["Kode dokumen / versi", `${m.code} / ${m.version}`], ["Dokumen acuan", m.refs]]),
    CAP("Tabel 0.1 Informasi dokumen"),
    new Paragraph({ spacing: { before: 120, after: 120 }, keepNext: true, children: [new TextRun({ text: "Revision Control", font: FONT, size: SZ.h2, bold: true, color: BLACK })] }),
    table(["Versi", "Tanggal", "Penulis", "Keterangan"], m.revisions, [900, 1500, 2100, PW - 4500]),
    CAP("Tabel 0.2 Riwayat revisi dokumen"),
    ...(m.approval ? approval(m.approval) : []),
    ...(m.extraControl ?? []),
    new Paragraph({ pageBreakBefore: true, spacing: { after: 160 }, children: [new TextRun({ text: "Daftar Isi", font: FONT, size: SZ.h1, bold: true, color: BLACK })] }),
    new TableOfContents("Daftar Isi", { hyperlink: true, headingStyleRange: "1-3" }),
    NOTE("Bila daftar isi belum tampil, klik kanan pada area ini lalu pilih Update Field."),
  ];
  return [...cover, ...ctl];
}

function build(m, body, out) {
  const margin = { top: 1418, bottom: 1418, left: 1418, right: 1418, header: 708, footer: 708 };
  const doc = new Document({
    creator: m.createdBy, title: `${m.code} ${m.title}`, description: m.subtitle,
    features: { updateFields: true },
    styles: {
      default: { document: { run: { font: FONT, size: SZ.body, color: BLACK }, paragraph: { spacing: { line: 276 } } } },
      paragraphStyles: [
        { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true, run: { font: FONT, size: SZ.h1, bold: true, color: BLACK }, paragraph: { spacing: { before: 240, after: 240 }, keepNext: true, outlineLevel: 0 } },
        { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true, run: { font: FONT, size: SZ.h2, bold: true, color: BLACK }, paragraph: { spacing: { before: 240, after: 120 }, keepNext: true, outlineLevel: 1 } },
        { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true, run: { font: FONT, size: SZ.h3, bold: true, color: BLACK }, paragraph: { spacing: { before: 180, after: 80 }, keepNext: true, outlineLevel: 2 } },
      ],
    },
    numbering: { config: [
      { reference: "bul", levels: [
        { level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 567, hanging: 283 } } } },
        { level: 1, format: LevelFormat.BULLET, text: "–", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 1000, hanging: 283 } } } }] },
      { reference: "num", levels: [{ level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 567, hanging: 360 } } } }] },
    ] },
    sections: [{
      properties: { page: { size: { width: 11906, height: 16838 }, margin }, titlePage: true },
      headers: { default: header(m.code, m.title) }, footers: { default: footer(m.version) },
      children: [...frontMatter(m), ...body],
    }],
  });
  return Packer.toBuffer(doc).then((b) => { fs.writeFileSync(out, b); console.log("written", out, b.length); });
}

module.exports = { approval, D, FONT, PW, SZ, runs, P, H1, H2, H3, B, NL, CAP, NOTE, cell, table, kv, img, build, KEY_FILL, HEAD_FILL };
