const fs = require("fs");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell, WidthType, ShadingType,
  AlignmentType, HeadingLevel, PageOrientation, Header, Footer, PageNumber, TableOfContents,
  LevelFormat, BorderStyle, PageBreak, VerticalAlign,
} = require("docx");
const { team, volumes, T } = require("./data");

const OUT = process.argv[2];
const FONT = "Arial";
const NAVY = "1F3864";
const LIGHT = "D9E2F3";
const PW = 9638;   // portrait content width (A4, 2 cm margins)
const LW = 14570;  // landscape content width

// ---------- helpers ----------
function runs(text, opts = {}) {
  const parts = String(text).split(/(\*\*[^*]+\*\*)/g).filter(Boolean);
  return parts.map((p) => p.startsWith("**")
    ? new TextRun({ text: p.slice(2, -2), bold: true, font: FONT, size: opts.size, color: opts.color })
    : new TextRun({ text: p, font: FONT, size: opts.size, color: opts.color, bold: opts.bold, italics: opts.italics }));
}
const P = (text, o = {}) => new Paragraph({ children: runs(text, o), alignment: o.align ?? AlignmentType.JUSTIFIED, spacing: { after: o.after ?? 120, line: 300 } });
const H1 = (t, pb = true) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun({ text: t, font: FONT })], pageBreakBefore: pb });
const H2 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun({ text: t, font: FONT })] });
const H3 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_3, children: [new TextRun({ text: t, font: FONT })] });
const B = (t, lvl = 0) => new Paragraph({ numbering: { reference: "bul", level: lvl }, children: runs(t), alignment: AlignmentType.JUSTIFIED, spacing: { after: 60, line: 280 } });
let numInst = 0;
const NL = (items) => { numInst++; return items.map((t) => new Paragraph({ numbering: { reference: "num", level: 0, instance: numInst }, children: runs(t), alignment: AlignmentType.JUSTIFIED, spacing: { after: 60, line: 280 } })); };
const CAP = (t) => new Paragraph({ children: [new TextRun({ text: t, font: FONT, size: 19, bold: true, color: NAVY })], spacing: { before: 160, after: 80 }, keepNext: true });
const NOTE = (t) => new Paragraph({ children: runs(t, { size: 17, italics: true }), spacing: { before: 40, after: 160 } });
const SP = () => new Paragraph({ children: [], spacing: { after: 60 } });

const border = { style: BorderStyle.SINGLE, size: 4, color: "8EAADB" };
const borders = { top: border, bottom: border, left: border, right: border };

function cell(text, width, o = {}) {
  const lines = Array.isArray(text) ? text : [text];
  return new TableCell({
    width: { size: width, type: WidthType.DXA },
    borders,
    shading: o.fill ? { fill: o.fill, type: ShadingType.CLEAR, color: "auto" } : undefined,
    margins: { top: 50, bottom: 50, left: 80, right: 80 },
    verticalAlign: o.vAlign ?? VerticalAlign.TOP,
    columnSpan: o.span,
    children: lines.map((l) => new Paragraph({
      alignment: o.align ?? AlignmentType.LEFT,
      children: runs(l, { size: o.size ?? 18, bold: o.bold, color: o.color }),
      spacing: { after: 0 },
    })),
  });
}
function table(headers, rows, widths, o = {}) {
  const size = o.size ?? 18;
  const total = widths.reduce((a, b) => a + b, 0);
  const hdr = new TableRow({
    tableHeader: true, cantSplit: true,
    children: headers.map((h, i) => cell(h, widths[i], { fill: NAVY, color: "FFFFFF", bold: true, size, align: o.hAlign })),
  });
  const body = rows.map((r, ri) => new TableRow({
    cantSplit: true,
    children: r.map((c, i) => {
      const spec = (c && typeof c === "object" && !Array.isArray(c)) ? c : { t: c };
      return cell(spec.t ?? "", widths[i], {
        size, fill: spec.fill ?? (o.zebra && ri % 2 ? "F2F5FB" : undefined),
        bold: spec.bold ?? (o.boldFirst && i === 0), align: spec.align ?? (o.center && o.center.includes(i) ? AlignmentType.CENTER : undefined),
        color: spec.color,
      });
    }),
  }));
  return new Table({ width: { size: total, type: WidthType.DXA }, columnWidths: widths, rows: [hdr, ...body] });
}
function kv(rows, widths = [3000, PW - 3000]) {
  return new Table({
    width: { size: PW, type: WidthType.DXA }, columnWidths: widths,
    rows: rows.map(([k, v]) => new TableRow({ children: [cell(k, widths[0], { fill: LIGHT, bold: true, size: 19 }), cell(v, widths[1], { size: 19 })] })),
  });
}

// ---------- derived numbers ----------
const volIds = volumes.map((v) => v.id);
const countBy = (f) => T.reduce((m, t) => { const k = f(t); m[k] = (m[k] || 0) + 1; return m; }, {});
const tasksPerDoc = countBy((t) => t.doc);
const wpPerDoc = {};
T.forEach((t) => { const k = t.wp.split(" ")[0]; (wpPerDoc[t.doc] ||= new Set()).add(k); });
const tasksPerPic = countBy((t) => t.pic);
const totalTasks = T.length;

// ---------- header / footer ----------
const mkHeader = () => new Header({ children: [new Paragraph({
  border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: NAVY, space: 4 } },
  children: [new TextRun({ text: "DWP-DDB-001  |  Software Engineering Documentation Work Plan  |  PT Diponegoro Digital Bank", font: FONT, size: 16, color: "595959" })],
})] });
const mkFooter = () => new Footer({ children: [new Paragraph({
  alignment: AlignmentType.CENTER,
  children: [
    new TextRun({ text: "Kelompok Jaran Goyang – Versi 1.1 – Halaman ", font: FONT, size: 16, color: "595959" }),
    new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: 16, color: "595959" }),
    new TextRun({ text: " dari ", font: FONT, size: 16, color: "595959" }),
    new TextRun({ children: [PageNumber.TOTAL_PAGES], font: FONT, size: 16, color: "595959" }),
  ],
})] });
const margin = { top: 1134, bottom: 1134, left: 1134, right: 1134, header: 567, footer: 567 };
const portrait = (children, first = false) => ({
  properties: { page: { size: { width: 11906, height: 16838 }, margin }, titlePage: first },
  headers: { default: mkHeader() }, footers: { default: mkFooter() }, children,
});
const landscape = (children) => ({
  properties: { page: { size: { width: 11906, height: 16838, orientation: PageOrientation.LANDSCAPE }, margin } },
  headers: { default: mkHeader() }, footers: { default: mkFooter() }, children,
});

// =====================================================================
// FRONT MATTER
// =====================================================================
const cover = [
  new Paragraph({ spacing: { before: 1200 }, alignment: AlignmentType.CENTER, children: [new TextRun({ text: "REKAYASA PERANGKAT LUNAK", font: FONT, size: 24, bold: true, color: "595959" })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 400 }, children: [new TextRun({ text: "SOFTWARE ENGINEERING", font: FONT, size: 40, bold: true, color: NAVY })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: "DOCUMENTATION WORK PLAN", font: FONT, size: 40, bold: true, color: NAVY })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 200, after: 600 }, border: { bottom: { style: BorderStyle.SINGLE, size: 12, color: NAVY, space: 8 } },
    children: [new TextRun({ text: "Top-Down Analysis, Task Decomposition, PIC Assignment, and Execution Planning", font: FONT, size: 24, italics: true, color: "404040" })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: "End-to-End Development of Greenfield Retail Digital Banking Platform (Web & Mobile Apps)", font: FONT, size: 22, bold: true })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 600 }, children: [new TextRun({ text: "PT Diponegoro Digital Bank – Diponegoro Corporation (Holding Company)", font: FONT, size: 22 })] }),
  kv([
    ["Kode dokumen", "DWP-DDB-001 (Documentation Work Plan)"],
    ["Versi / Status", "1.1 / Baseline Rencana Kerja Dokumentasi (revisi)"],
    ["Dasar dokumen", "SRS-DDB-001 v0.1; Project Brief & Scope Document v1.0; MDTA-DDB-001 v0.1; Template Dokumentasi Project (Software Engineering, UKSW)"],
    ["Mata kuliah", "Software Engineering – Universitas Kristen Satya Wacana"],
    ["Tim proyek", "Kelompok Jaran Goyang"],
    ["Project Manager", "Salomo Putra Glorious Hokoyoku (622024022)"],
    ["Tanggal terbit", "11 November 2026"],
    ["Tenggat pengumpulan", "24 November 2026 pukul 23.59 WIB"],
  ]),
];

const control = [
  new Paragraph({ pageBreakBefore: true, children: [new TextRun({ text: "Kontrol Dokumen", font: FONT, size: 28, bold: true, color: NAVY })], spacing: { after: 160 } }),
  CAP("Informasi Dokumen"),
  kv([
    ["Nama module", "Software Engineering Documentation Work Plan"],
    ["Status", "Baseline – berlaku sebagai acuan pelaksanaan sampai pengumpulan"],
    ["Created by", "Salomo Putra Glorious Hokoyoku (Project Manager)"],
    ["Last updated by", "Salomo Putra Glorious Hokoyoku (Project Manager)"],
    ["Responsible team", "Kelompok Jaran Goyang (Mobile Application Team dan Web Application Team)"],
    ["Klasifikasi", "Internal tim dan dosen pengampu"],
  ]),
  CAP("Revision Control"),
  table(["Versi", "Tanggal", "Penulis", "Keterangan"], [
    ["0.1", "11 Nov 2026", "Project Manager", "Draf validasi: pemeriksaan sumber, validasi PIC, temuan informasi yang belum tersedia, dan usulan dekomposisi top-down."],
    ["1.0", "11 Nov 2026", "Project Manager", "Baseline setelah validasi Project Owner: seluruh Backend dan Database dipegang Salomo (termasuk volume D04B), tenggat 24 Nov 2026 23.59 WIB, format keluaran .docx."],
    ["1.1", "11 Nov 2026", "Project Manager", "Koreksi pembagian modul Android sesuai submit_mobile/README.md (Salomo: Authentication, Virtual Card; Cecilia: Transfer, History; Gracia: QRIS, Profile; Grace: Account, Top-up) pada D04A-WP1-T02 dan D07A-WP3; pembaruan status D01 dan D02 (draf v0.9); rentang proyek 15 Sep–23 Nov 2026; temuan Lampiran A diperbarui berdasarkan repositori kode."],
  ], [900, 1500, 2000, PW - 4400]),
  CAP("Persetujuan"),
  table(["Peran", "Nama", "Tanda tangan", "Tanggal"], [
    ["Disusun oleh (Project Manager)", "Salomo Putra Glorious Hokoyoku", "", ""],
    ["Diketahui oleh (Mobile Application Team)", "Perwakilan tim", "", ""],
    ["Diketahui oleh (Web Application Team)", "Perwakilan tim", "", ""],
  ], [3000, 2800, 2200, PW - 8000]),
  new Paragraph({ pageBreakBefore: true, children: [new TextRun({ text: "Daftar Isi", font: FONT, size: 28, bold: true, color: NAVY })], spacing: { after: 160 } }),
  new TableOfContents("Daftar Isi", { hyperlink: true, headingStyleRange: "1-2" }),
  NOTE("Catatan: bila daftar isi belum tampil, klik kanan pada area ini lalu pilih Update Field."),
];

// =====================================================================
// BAB I
// =====================================================================
const bab1 = [
  H1("BAB I  PENDAHULUAN"),
  H2("1.1 Latar Belakang"),
  P("PT Diponegoro Digital Bank adalah platform digital banking ritel greenfield yang terdiri atas aplikasi Native Android, aplikasi Web, satu backend bersama, basis data relasional, serta lapisan adapter yang terhubung ke simulator layanan eksternal (antarbank, QRIS, penyedia pulsa/PLN, penerbit kartu, OTP, dan e-KYC). Ruang lingkup fungsionalnya ditetapkan dalam SRS-DDB-001 v0.1 sebagai 29 kebutuhan fungsional (15 Must, 11 Should, 3 Could) yang tersebar pada delapan modul bisnis: Authentication, Account, Transfer, Payment (QRIS), Top-up, Virtual Card, Transaction History & Dashboard, serta Profile & Notification."),
  P("Dosen pengampu mensyaratkan sembilan kategori dokumentasi rekayasa perangkat lunak. Beberapa kategori dipecah per platform sehingga terdapat **15 volume dokumen** yang dikerjakan delapan anggota dalam rentang 11–24 November 2026. Kondisi ini menimbulkan tiga persoalan pengelolaan:"),
  B("**Ketergantungan berantai.** Detailed Design tidak dapat difinalkan sebelum Architectural Design dan Functional Design memiliki baseline. Developer Manual, User Manual, System Testing, dan System Program Demo bergantung pada kode dan build yang benar-benar berjalan."),
  B("**Konsentrasi kompetensi.** Seluruh Backend dan Database hanya dapat dikerjakan oleh Salomo, yang juga Project Manager dan final reviewer. Tanpa perencanaan, Salomo menjadi titik hambatan tunggal."),
  B("**Konsistensi lintas dokumen.** Nama modul, ID kebutuhan, kode layar, endpoint API, dan versi build muncul di banyak dokumen. Tanpa sumber kebenaran yang disepakati, dokumen akan saling bertentangan."),
  P("Rencana kerja ini disusun agar pekerjaan dokumentasi dapat dibagi, diurutkan, dipantau, dan dikendalikan mutunya secara terukur, bukan sekadar daftar tugas."),
  H2("1.2 Tujuan"),
  ...NL([
    "Menguraikan sasaran dokumentasi secara hierarkis (top-down) dari tujuan keseluruhan sampai tugas individual yang dapat dieksekusi.",
    "Menetapkan satu PIC utama untuk setiap volume dan setiap tugas, beserta tanggung jawab eksplisit anggota pendukung.",
    "Menetapkan ketergantungan antardokumen dan urutan eksekusi, termasuk pekerjaan yang dapat berjalan paralel.",
    "Menyusun jadwal usulan yang realistis terhadap tenggat 24 November 2026 pukul 23.59 WIB.",
    "Menetapkan mekanisme pemantauan, review bertahap, konsistensi lintas dokumen, dan Definition of Done.",
    "Menyediakan instrumen operasional bagi Project Manager: matriks dokumentasi, to-do list rinci, RACI, matriks ketergantungan, dan checklist penyelesaian.",
  ]),
  H2("1.3 Ruang Lingkup"),
  P("Ruang lingkup rencana ini adalah **pekerjaan penyusunan sembilan kategori dokumentasi** sesuai template Dokumentasi Project mata kuliah Software Engineering. Ruang lingkup produk yang didokumentasikan mengikuti SRS v0.1. Tabel 1.1 merangkum cakupan tiap kategori."),
  CAP("Tabel 1.1 Ruang lingkup sembilan kategori dokumentasi"),
  table(["No", "Kategori", "Pemecahan volume", "Cakupan isi"], [
    ["1", "Project Document", "1 volume", "Judul, tanda tangan, history, daftar isi, deskripsi project, Gantt chart, model dan rencana testing, manajemen risiko, estimasi biaya"],
    ["2", "Architectural Design", "1 volume", "Arsitektur Android, Web, Backend, Database, dan integrasi"],
    ["3", "Functional Design", "1 volume", "Deskripsi modul, batas sistem, antarmuka eksternal, spesifikasi fungsi, workflow untuk FR-01 s.d. FR-29"],
    ["4", "Detailed Design", "3 volume: Android, Web, Backend & Database", "UML, workflow diagram, pseudocode, perancangan software/hardware"],
    ["5", "User Interface Map", "2 volume: Android, Web", "Peta antarmuka dan navigasi"],
    ["6", "System Testing", "1 volume konsolidasi", "Pengujian Web, Android, Backend/API, dan lintas platform"],
    ["7", "Developer Manual", "3 volume: Android, Web, Backend", "Keterangan source code dan panduan pengembang"],
    ["8", "System Program Demo", "1 volume", "Persiapan, lingkungan, skenario, perilaku sistem, bukti"],
    ["9", "User Manual", "2 volume: Android, Web", "Panduan penggunaan per modul"],
  ], [500, 2000, 2300, PW - 4800], { center: [0] }),
  P("**Di luar ruang lingkup:** (1) implementasi perangkat lunak itu sendiri; rencana ini hanya mengelola dokumentasinya; (2) aplikasi iOS, karena proyek ini mengembangkan Native Android walaupun SRS menyebut Android dan iOS (Lampiran A); (3) fitur diferensiasi (Budgeting Tracker, Smart Split Bill, Round-Up, Quest Keuangan) yang belum dirinci di SRS."),
  H2("1.4 Organisasi Proyek"),
  P("Tim terdiri atas dua kelompok platform. Berdasarkan keputusan Project Owner tanggal 11 November 2026, seluruh Backend dan Database dipegang Salomo. Slot BE-1 s.d. BE-4 pada MDTA-DDB-001 v0.1 dengan demikian terisi oleh Salomo."),
  CAP("Tabel 1.2 Komposisi tim"),
  table(["NIM", "Nama lengkap", "Nama singkat", "Tim", "Peran dalam proyek"], team, [1200, 2600, 1100, 1500, PW - 6400], { zebra: true }),
  P("Pembagian tanggung jawab dokumentasi mengikuti penugasan Project Owner dan karakteristik kerja anggota:"),
  B("**Salomo** memegang dokumen yang menuntut pandangan menyeluruh dan pengetahuan backend: Dokumen 1, 2, 3, Detailed Design Backend & Database (D04B), Developer Manual Android dan Backend. Ia juga final reviewer seluruh dokumentasi dan penanggung jawab konsistensi lintas dokumen."),
  B("**Steven** memegang pekerjaan analitis dan validasi: System Testing dan Developer Manual Web. Ia juga menjadi peer reviewer teknis bagi dokumen Salomo, agar Salomo tidak menyetujui dokumennya sendiri tanpa telaah pihak lain."),
  B("**Rafi** memegang dokumentasi yang menuntut struktur dan presentasi: Detailed Design Web, PIC utama System Program Demo, dan penyusun master template."),
  B("**Cecilia** memegang User Interface Map Android sesuai kecenderungannya pada pekerjaan desain dan visual."),
  B("**Nabila** memegang User Interface Map Web dan bagian komponen UI pada Developer Manual Web."),
  B("**Gracia** memegang Detailed Design Android. Ia juga menulis bagian modul pada Developer Manual Android dan mengeksekusi uji Android, sehingga bebannya setara dengan anggota lain."),
  B("**Grace** memegang User Manual Android. Tugasnya dipecah per modul dengan instruksi dan keluaran eksplisit."),
  B("**Permata** memegang User Manual Web, register istilah, dokumentasi lingkungan dan data demo, serta eksekusi uji Web. Ketiganya berbagi sumber yang sama, yaitu penelusuran fitur Web pada build baseline."),
];

// =====================================================================
// BAB II
// =====================================================================
const hier = [
  ["Level 0", "Tujuan keseluruhan dokumentasi", "Sembilan kategori dokumentasi selesai, konsisten satu sama lain, disetujui Project Manager, dan terkumpul sebelum 24 Nov 2026 23.59 WIB.", "1"],
  ["Level 1", "Kategori dan volume dokumen", "Dokumen 1–9, dipecah menjadi 15 volume per platform (D01 … D09W), ditambah paket manajemen lintas dokumen D00.", "15 + D00"],
  ["Level 2", "Komponen dokumen", "Bagian wajib template dosen: blok kontrol dokumen (Nama Module, Status, Created by, Last updated by, Responsible team, Revision control), Daftar Isi, dan isi inti per kategori.", "Lihat Tabel 2.2"],
  ["Level 3", "Work package", "Kelompok aktivitas untuk menghasilkan komponen, mis. WP2 UML, WP3 Workflow, WP4 Pseudocode pada Detailed Design.", String(Object.values(wpPerDoc).reduce((a, s) => a + s.size, 0))],
  ["Level 4", "Tugas individual", "Tugas konkret dengan Task ID, PIC, pendukung, ketergantungan, keluaran, kriteria selesai, dan status (Tabel 3.3).", String(totalTasks)],
];
const bab2 = [
  H1("BAB II  ANALISIS DEKOMPOSISI PEKERJAAN DOKUMENTASI"),
  H2("2.1 Metodologi Analisis Top-Down"),
  P("Analisis top-down menguraikan sasaran besar menjadi unsur yang makin kecil sampai setiap unsur dapat ditugaskan kepada satu orang, diperkirakan waktunya, dan diverifikasi penyelesaiannya. Pendekatan ini sejalan dengan prinsip Work Breakdown Structure: setiap tingkat merupakan pemecahan lengkap dari tingkat di atasnya (aturan 100%), sehingga tidak ada pekerjaan yang terlewat dan tidak ada pekerjaan di luar sasaran."),
  P("Penguraian pada rencana ini mengikuti empat aturan:"),
  ...NL([
    "**Diturunkan dari sumber resmi.** Komponen Level 2 diambil dari template dosen. Isi teknis Level 3–4 diturunkan dari SRS v0.1, Project Brief v1.0, dan MDTA v0.1. Tidak ada fitur, tanggal realisasi, atau hasil yang ditambahkan tanpa sumber.",
    "**Satu PIC per unsur.** Setiap volume dan setiap tugas memiliki tepat satu PIC utama. Anggota pendukung dicantumkan dengan tanggung jawab yang disebutkan.",
    "**Granularitas sesuai kebutuhan.** Tugas dipecah sampai dapat diselesaikan dalam 1–2 hari kerja dan memiliki keluaran yang dapat diperiksa. Untuk anggota yang memerlukan panduan lebih terstruktur (Grace, Gracia, Permata), tugas dipecah per modul dengan instruksi langkah.",
    "**Mengikuti pemecahan platform.** Kategori yang dipecah per platform diperlakukan sebagai volume tersendiri karena PIC, sumber, dan ketergantungannya berbeda.",
  ]),
  H2("2.2 Hierarki Dokumentasi"),
  CAP("Tabel 2.1 Tingkat dekomposisi"),
  table(["Level", "Unsur", "Definisi dan contoh", "Jumlah"], hier, [1000, 2000, PW - 4000, 1000], { boldFirst: true, center: [3] }),
  P("Tabel 2.2 menunjukkan penguraian Level 1 ke Level 2, serta jumlah work package (Level 3) dan tugas (Level 4) untuk setiap volume. Rincian Level 4 disajikan pada Tabel 3.3."),
  CAP("Tabel 2.2 Dekomposisi Level 1 → Level 2 dengan jumlah WP dan tugas"),
  table(["ID", "Volume (Level 1)", "Komponen wajib (Level 2)", "WP", "Tugas"],
    [["D00", "Manajemen dan standardisasi lintas dokumen", "Standar, template, register istilah, decision log, status implementasi, build baseline, pengendalian, pengemasan", String(wpPerDoc.D00.size), String(tasksPerDoc.D00)],
      ...volumes.map((v) => [v.id, v.name, v.l2, String(wpPerDoc[v.id].size), String(tasksPerDoc[v.id])]),
      [{ t: "", fill: LIGHT }, { t: "Total", bold: true, fill: LIGHT }, { t: "", fill: LIGHT }, { t: String(Object.values(wpPerDoc).reduce((a, s) => a + s.size, 0)), bold: true, fill: LIGHT }, { t: String(totalTasks), bold: true, fill: LIGHT }]],
    [800, 2400, PW - 4600, 700, 700], { center: [3, 4], boldFirst: true }),
  P("Pola work package dibuat seragam agar mudah dipantau. **WP1** selalu berupa outline dan pengumpulan sumber. WP tengah menghasilkan isi inti sesuai komponen Level 2. **WP terakhir** selalu berupa review dan finalisasi. Dengan pola ini, Project Manager dapat membaca kemajuan setiap volume dari nomor WP yang sedang dikerjakan."),
  H2("2.3 Deliverable Dokumentasi"),
  P("Setiap volume menghasilkan satu berkas .docx dengan kode dokumen tersendiri. Kode mengikuti pola dokumen proyek yang sudah ada (SRS-DDB-001, MDTA-DDB-001). Kode pada Tabel 2.3 merupakan usulan dan berlaku setelah disahkan dalam standar dokumentasi (D00-WP1-T01)."),
  CAP("Tabel 2.3 Deliverable per volume"),
  table(["ID", "Kode dokumen (usulan)", "Volume", "Keluaran yang diharapkan"],
    volumes.map((v) => [v.id, v.code, v.name, v.deliv]), [700, 2000, 2300, PW - 5000], { boldFirst: true, zebra: true }),
  H2("2.4 Ketergantungan Antardokumen"),
  P("Ketergantungan dibedakan menjadi dua jenis. **Mutlak**: dokumen hilir tidak dapat difinalkan sebelum sumbernya tersedia. **Parsial**: penyusunan dapat dimulai dari sumber sementara, misalnya SRS atau prototipe, lalu disesuaikan setelah sumber final tersedia. Tabel 2.4 adalah Dependency Matrix (Artefak D)."),
  CAP("Tabel 2.4 Dependency Matrix (Artefak D)"),
  table(["Volume", "Bergantung pada", "Jenis", "Alasan", "Penanganan bila sumber belum tersedia"], [
    ["D01", "Project Brief v1.0, SRS v0.1; status implementasi (D00-WP2-T02) untuk Gantt", "Parsial", "Deskripsi, risiko, dan fase sudah ada di Project Brief; Gantt memerlukan realisasi.", "Gantt memisahkan rencana dan realisasi; aktivitas tanpa data diberi label 'Belum terkonfirmasi'."],
    ["D02", "SRS 2.1, 2.5; Project Brief §9; decision log (framework Web, produk DB)", "Mutlak (keputusan teknologi)", "Arsitektur tidak dapat difinalkan tanpa keputusan teknologi.", "Keputusan terbuka diselesaikan Salomo paling lambat 13 Nov (D00-WP2-T01)."],
    ["D03", "SRS v0.1 Bagian 3; D02 baseline untuk antarmuka eksternal", "Parsial", "SRS v0.1 belum memuat bagian Antarmuka Eksternal.", "Butir yang belum ada di SRS ditandai 'Perlu Ditentukan' beserta pemiliknya."],
    ["D04A / D04W", "D02 dan D03 baseline yang disetujui; D04B (endpoint API); kode aktual", "Mutlak", "Detailed Design harus konsisten dengan arsitektur dan fungsi yang disetujui.", "Inventaris komponen (WP1) dimulai lebih awal; UML difinalkan setelah baseline 15 Nov."],
    ["D04B", "D02 baseline; skema dan kode backend aktual", "Mutlak", "Diagram dan ERD harus mencerminkan implementasi.", "Bagian yang belum terimplementasi diberi label desain rencana, bukan realisasi."],
    ["D05A / D05W", "Cakupan fungsional D03; struktur antarmuka aktual (build atau prototipe)", "Parsial", "Peta UI mengikuti layar nyata.", "Mulai dari prototipe; tangkapan layar diganti dari build baseline setelah 18 Nov."],
    ["D06", "Kebutuhan (SRS, D03); perilaku yang diharapkan; D04B (API); lingkungan uji dan build baseline", "Mutlak untuk eksekusi; parsial untuk desain kasus uji", "Kasus uji dapat dirancang dari kebutuhan, tetapi eksekusi memerlukan build.", "Kasus yang tidak dapat dieksekusi berstatus 'Terblokir' beserta alasannya; tidak ada hasil rekaan."],
    ["D07A / D07W / D07B", "Source code aktual dan build baseline; D02, D04", "Mutlak", "Developer Manual menjelaskan kode yang benar-benar ada.", "Modul yang belum diimplementasikan dicatat sebagai keterbatasan versi."],
    ["D08", "Build demo yang berjalan; skenario; D05 (kode layar); hasil D06", "Mutlak", "Demo hanya dapat menampilkan fungsi yang berjalan.", "Daftar skenario dibatasi pada fitur terimplementasi (D08-WP1-T02)."],
    ["D09A / D09W", "Fungsi yang terimplementasi dan dapat didemonstrasikan; D05 (kode layar)", "Mutlak", "User Manual menggambarkan perilaku nyata.", "Fitur yang belum tersedia ditandai 'Belum tersedia pada build vX'."],
  ], [1200, 2300, 1300, 2300, PW - 7100], { size: 17, boldFirst: true }),
  H2("2.5 Urutan Eksekusi"),
  P("Berdasarkan Tabel 2.4, pekerjaan disusun dalam lima gelombang. Setiap gelombang dibuka oleh baseline gelombang sebelumnya, tetapi pekerjaan yang hanya bergantung parsial dimulai lebih awal agar waktu 14 hari dimanfaatkan penuh."),
  CAP("Tabel 2.5 Gelombang eksekusi: berurutan dan paralel"),
  table(["Gelombang", "Pekerjaan", "Sifat", "Pembuka gelombang"], [
    ["G0 Inisiasi", "Standar, master template, register istilah, decision log, outline seluruh volume, konfirmasi status implementasi", "Harus pertama; paralel di dalam gelombang", "Terbitnya rencana ini"],
    ["G1 Baseline desain", "D02 dan D03 (baseline); D01 mulai disusun", "Berurutan terhadap G2 (jalur kritis)", "Template dan decision log"],
    ["G2 Desain rinci dan UI", "D04A, D04W, D04B; D05A, D05W; desain kasus uji D06", "Paralel antarvolume", "Baseline D02 dan D03 (15 Nov); D05 dan desain D06 dapat mulai sejak G0"],
    ["G3 Dokumen berbasis implementasi", "D07A, D07W, D07B; eksekusi D06; D09A, D09W", "Paralel antarvolume", "Build baseline dokumentasi (18 Nov)"],
    ["G4 Demo dan kendali mutu akhir", "D08; review konsistensi lintas dokumen; persetujuan PM; pengemasan", "Berurutan", "Hasil D06, kode layar D05, seluruh volume lulus R2"],
  ], [1700, 3600, 2000, PW - 7300], { boldFirst: true }),
  P("**Jalur kritis** rencana ini adalah: keputusan teknologi → D02/D03 baseline → D04 → build baseline → D06 eksekusi → D08 → review konsistensi → pengumpulan. Keterlambatan pada mata rantai ini langsung menggeser tanggal pengumpulan, sehingga mata rantai tersebut dipantau harian (Bagian 4.4)."),
];

// =====================================================================
// BAB III (landscape)
// =====================================================================
const volStatus = { D01: "Draf v0.9; menunggu R2", D02: "Draf v0.9; menunggu R2" };
const masterRows = volumes.map((v) => [v.no, v.name, v.scope, v.pic, v.sup, v.deliv, volStatus[v.id] || "Belum Dimulai"]);
const toDoRows = T.map((t) => [t.id, t.doc, t.wp, t.task, t.pic, t.sup, t.dep, t.out, t.crit, t.status]);

const people = ["Salomo", "Cecilia", "Gracia", "Grace", "Nabila", "Steven", "Rafi", "Permata"];
const raciSpec = [
  ["Standar dokumentasi dan master template", { Salomo: "A/R", Rafi: "R", Permata: "C" }],
  ["Decision log dan keputusan teknis", { Salomo: "A/R", Steven: "C", Gracia: "C", Nabila: "C" }],
  ["Status implementasi dan build baseline", { Salomo: "A/R", Cecilia: "C", Gracia: "C", Grace: "C", Nabila: "C", Steven: "C", Rafi: "C", Permata: "C" }],
  ["D01 Project Document", { Salomo: "A/R", Steven: "C", Rafi: "R" }],
  ["D02 Architectural Design", { Salomo: "A/R", Gracia: "C", Steven: "C" }],
  ["D03 Functional Design", { Salomo: "A/R", Steven: "C" }],
  ["D04A Detailed Design – Android", { Salomo: "A", Gracia: "R", Cecilia: "C", Grace: "C" }],
  ["D04W Detailed Design – Web", { Salomo: "A", Rafi: "R", Steven: "C", Nabila: "C", Permata: "C" }],
  ["D04B Detailed Design – Backend & DB", { Salomo: "A/R", Steven: "C" }],
  ["D05A UI Map – Android", { Salomo: "A", Cecilia: "R", Grace: "R", Gracia: "C" }],
  ["D05W UI Map – Web", { Salomo: "A", Nabila: "R", Permata: "R", Rafi: "C" }],
  ["D06 System Testing", { Salomo: "A/C", Steven: "R", Gracia: "R", Grace: "R", Permata: "R", Cecilia: "I", Nabila: "I", Rafi: "I" }],
  ["D07A Developer Manual – Android", { Salomo: "A/R", Gracia: "R", Cecilia: "C", Grace: "C" }],
  ["D07W Developer Manual – Web", { Salomo: "A", Steven: "R", Nabila: "R", Rafi: "C", Permata: "C" }],
  ["D07B Developer Manual – Backend", { Salomo: "A/R", Steven: "C" }],
  ["D08 System Program Demo", { Salomo: "A/C", Rafi: "R", Nabila: "R", Cecilia: "R", Permata: "R", Steven: "C" }],
  ["D09A User Manual – Android", { Salomo: "A", Grace: "R", Cecilia: "C" }],
  ["D09W User Manual – Web", { Salomo: "A", Permata: "R", Nabila: "C" }],
  ["Review konsistensi lintas dokumen (R3)", { Salomo: "A/R", Rafi: "R", Permata: "R", Cecilia: "C", Gracia: "C", Grace: "C", Nabila: "C", Steven: "C" }],
  ["Persetujuan akhir dan pengumpulan", { Salomo: "A/R", Rafi: "R" }],
];
const raciRows = raciSpec.map(([act, m]) => [act, ...people.map((p) => {
  const v = m[p] || "I";
  const fill = v.startsWith("A") ? "C9D7EF" : v === "R" ? "E2EFDA" : undefined;
  return { t: v, fill, align: AlignmentType.CENTER, bold: v.includes("A") || v === "R" };
})]);

const load = [
  ["Salomo", "D01, D02, D03, D04B, D07A, D07B", "Seluruh Backend/DB; persetujuan akhir; review R2 untuk 9 volume lain; data uji dan lingkungan demo (C)", "Sangat tinggi. Mitigasi: peer review oleh Steven, template oleh Rafi, register istilah oleh Permata, bagian modul D07A oleh Gracia, pre-review pasangan (R1) sebelum R2."],
  ["Steven", "D06, D07W", "Peer reviewer teknis dokumen Salomo; C untuk D01, D02, D03, D04W", "Tinggi; sesuai kemampuan analitis. Eksekusi uji Android dan Web dilimpahkan ke Gracia, Grace, dan Permata."],
  ["Rafi", "D04W, D08", "Master template; format D01; uji keterikutan D07W; pre-review D05W", "Sedang–tinggi; sesuai kekuatan struktur dokumen."],
  ["Nabila", "D05W", "Bagian komponen UI dan 6 modul D07W; skenario demo Web; pre-review D04W dan verifikasi D09W", "Sedang–tinggi."],
  ["Cecilia", "D05A", "Skenario demo Android; pre-review D04A; verifikasi langkah D09A; catatan modul D07A", "Sedang; berfokus pada visual dan alur layar."],
  ["Gracia", "D04A", "Bagian modul Payment (QRIS) dan Profile & Notification D07A; uji keterikutan D07A; eksekusi uji Android", "Sedang; tugas teknis didefinisikan eksplisit."],
  ["Permata", "D09W", "Register istilah; lingkungan dan data demo; eksekusi uji Web; tangkapan layar D05W", "Sedang; seluruh tugas berbasis penelusuran fitur Web."],
  ["Grace", "D09A", "Tangkapan layar D05A; dukungan eksekusi uji Android; catatan modul D07A", "Sedang; tugas berurutan, repetitif, dan berinstruksi eksplisit per modul."],
];

const bab3 = [
  H1("BAB III  PENUGASAN DAN MATRIKS TANGGUNG JAWAB"),
  H2("3.1 Matriks Tanggung Jawab Dokumentasi Keseluruhan"),
  P("Tabel 3.1 adalah Master Documentation Matrix (Artefak A). Status per 11 November 2026: D01 dan D02 telah tersedia sebagai draf v0.9 dan menunggu review teknis (R2); volume lain berstatus **Belum Dimulai** karena belum ada bukti draf. Status hanya berubah melalui mekanisme pada Bagian 4.4."),
  CAP("Tabel 3.1 Master Documentation Matrix (Artefak A)"),
  table(["No", "Nama dokumen", "Platform/cakupan", "PIC", "Anggota pendukung", "Deliverable yang diharapkan", "Status"], masterRows,
    [500, 2400, 2100, 1000, 3570, 3900, 1100], { size: 17, zebra: true, center: [0] }),
  H2("3.2 Matriks Tanggung Jawab Berbasis Platform"),
  P("Tabel 3.2 memetakan tanggung jawab per platform. Volume lintas platform (D01, D02, D03, D06, D08) ditempatkan pada kolom Lintas Platform."),
  CAP("Tabel 3.2 Tanggung jawab per platform"),
  table(["Kategori", "Android", "Web", "Backend & Database", "Lintas platform"], [
    ["1 Project Document", "–", "–", "–", "Salomo"],
    ["2 Architectural Design", "Masukan: Gracia (C)", "Masukan: Steven (C)", "Salomo", "Salomo (PIC)"],
    ["3 Functional Design", "–", "–", "–", "Salomo; review Steven"],
    ["4 Detailed Design", "Gracia (D04A)", "Rafi (D04W)", "Salomo (D04B)", "–"],
    ["5 User Interface Map", "Cecilia (D05A); dukungan Grace", "Nabila (D05W); dukungan Permata", "Tidak berlaku", "–"],
    ["6 System Testing", "Eksekusi: Gracia, Grace", "Eksekusi: Permata", "Kasus uji dan eksekusi API: Steven; data uji: Salomo", "Steven (PIC)"],
    ["7 Developer Manual", "Salomo (D07A); bagian modul Gracia", "Steven (D07W); Nabila", "Salomo (D07B)", "–"],
    ["8 System Program Demo", "Skenario: Cecilia", "Skenario: Nabila", "Lingkungan: Salomo (C), Permata", "Rafi (PIC)"],
    ["9 User Manual", "Grace (D09A)", "Permata (D09W)", "Tidak berlaku", "–"],
  ], [2700, 2900, 2900, 3270, 2800], { boldFirst: true, zebra: true }),
  H2("3.3 Penugasan Tugas Rinci"),
  P(`Tabel 3.3 adalah Detailed To-Do List (Artefak B) yang memuat ${totalTasks} tugas Level 4. Format Task ID adalah **[Volume]-[Work Package]-[Tugas]**, misalnya D04A-WP2-T01 adalah tugas pertama pada work package UML di Detailed Design Android. Kolom PIC selalu berisi satu nama. PIC tugas dapat berbeda dari PIC volume bila tugas tersebut didelegasikan, misalnya eksekusi uji Android pada D06 oleh Gracia, sedangkan Steven tetap PIC volume D06.`),
  CAP("Tabel 3.3 Detailed To-Do List (Artefak B)"),
  table(["Task ID", "Dok.", "Work package", "Tugas spesifik", "PIC", "Pendukung", "Ketergantungan", "Keluaran", "Kriteria selesai", "Status"], toDoRows,
    [1050, 650, 1250, 3200, 950, 1150, 1250, 1850, 2350, 870], { size: 15 }),
  NOTE("Keterangan: (C) = Consulted, yaitu dimintai masukan atau konfirmasi tanpa memegang keluaran. Status per 11 November 2026 berdasarkan bukti berkas; tugas tanpa bukti berstatus 'Belum Dimulai'."),
  H2("3.4 Tanggung Jawab dan Koordinasi"),
  P("Delegasi dibangun di atas tiga peran. **PIC** memiliki keluaran, menyusun isi, dan mengajukan review. **Anggota pendukung** mengerjakan bagian yang disebut eksplisit atau memberi masukan (C), tetapi tidak memiliki keluaran. **Project Manager** menetapkan standar, menyelesaikan konflik, melakukan review teknis, dan memberi persetujuan akhir. Salomo bersifat Accountable (A) pada setiap volume karena ia adalah otoritas persetujuan akhir. Pada volume yang ia tulis sendiri, ia sekaligus Responsible (R). Untuk volume tersebut, telaah independen diberikan oleh Steven sebagai peer reviewer. Tabel 3.4 adalah RACI Matrix (Artefak C)."),
  CAP("Tabel 3.4 RACI Matrix (Artefak C)"),
  table(["Aktivitas / volume", ...people], raciRows, [4170, ...Array(8).fill(1300)], { size: 17, boldFirst: true }),
  NOTE("R = Responsible (mengerjakan); A = Accountable (penanggung jawab akhir, satu per baris); C = Consulted (dimintai masukan); I = Informed (diberi tahu). 'A/R' berarti penanggung jawab akhir sekaligus pengerja; 'A/C' berarti penanggung jawab akhir yang juga menyediakan masukan teknis (backend)."),
  P("Tabel 3.5 merangkum beban kerja per anggota serta alasan penugasannya. Tujuannya memastikan prinsip bahwa pekerjaan tidak dialihkan seluruhnya kepada Salomo hanya karena kemampuan teknisnya lebih tinggi."),
  CAP("Tabel 3.5 Ringkasan beban kerja dan rasional penugasan"),
  table(["Anggota", "Volume sebagai PIC", "Tanggung jawab pendukung", "Jumlah tugas sebagai PIC", "Penilaian beban dan rasional"],
    load.map(([n, a, b, c]) => [n, a, b, String(tasksPerPic[n] || 0), c]), [1300, 2600, 4400, 1500, 4770], { boldFirst: true, center: [3], zebra: true }),
  P("**Aturan koordinasi.** (1) Pertanyaan teknis Android diajukan ke Gracia atau Salomo, Web ke Steven, Backend/DB ke Salomo. (2) Perubahan yang memengaruhi lebih dari satu volume, seperti nama modul, kode layar, endpoint, atau versi build, wajib dicatat di decision log oleh Salomo sebelum diterapkan. (3) Konflik antar-PIC diselesaikan oleh Salomo dalam 24 jam. (4) Bila Salomo menjadi hambatan karena antrean review lebih dari 24 jam, Steven berwenang melakukan R2 sementara untuk dokumen platform Web dan Android, dengan Salomo tetap memberi persetujuan akhir."),
];

// =====================================================================
// BAB IV
// =====================================================================
const days = [11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24];
const dayNames = ["Rab", "Kam", "Jum", "Sab", "Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min", "Sen", "Sel"];
const gantt = [
  ["G0 Standar, template, register istilah, outline", "Salomo, Rafi, Permata", 11, 12, [12]],
  ["G0 Decision log dan keputusan teknologi", "Salomo", 11, 13, [13]],
  ["G0 Konfirmasi status implementasi", "Salomo", 11, 13, []],
  ["G1 D02 dan D03 sampai baseline", "Salomo", 12, 15, [15]],
  ["G1 D01 Project Document", "Salomo", 12, 20, [20]],
  ["G2 D05A / D05W UI Map", "Cecilia, Nabila", 13, 19, [19]],
  ["G2 D06 desain kasus uji", "Steven", 13, 17, [17]],
  ["G2 D04A / D04W / D04B Detailed Design", "Gracia, Rafi, Salomo", 14, 20, [20]],
  ["Build baseline dokumentasi (code freeze)", "Salomo", 16, 18, [18]],
  ["G3 D07A / D07W / D07B Developer Manual", "Salomo, Steven", 17, 21, [21]],
  ["G3 D06 eksekusi uji dan laporan", "Steven, Gracia, Grace, Permata", 19, 22, [22]],
  ["G3 D09A / D09W User Manual", "Grace, Permata", 18, 22, [22]],
  ["G4 D08 persiapan, gladi, bukti", "Rafi, Nabila, Cecilia, Permata", 18, 22, [21, 22]],
  ["G4 Review konsistensi lintas dokumen (R3)", "Salomo, Rafi, Permata", 22, 23, []],
  ["G4 Persetujuan PM dan pengemasan", "Salomo, Rafi", 23, 24, []],
  ["Pengumpulan (target 20.00, batas 23.59 WIB)", "Salomo", 24, 24, [24]],
];
const gW = [3800, 2000, ...Array(14).fill(626)];
gW[2] += LW - gW.reduce((a, b) => a + b, 0);
const ganttTable = new Table({
  width: { size: LW, type: WidthType.DXA }, columnWidths: gW,
  rows: [
    new TableRow({ tableHeader: true, children: [
      cell("Aktivitas", gW[0], { fill: NAVY, color: "FFFFFF", bold: true, size: 16 }),
      cell("PIC", gW[1], { fill: NAVY, color: "FFFFFF", bold: true, size: 16 }),
      ...days.map((d, i) => cell([dayNames[i], String(d)], gW[i + 2], { fill: NAVY, color: "FFFFFF", bold: true, size: 14, align: AlignmentType.CENTER })),
    ] }),
    ...gantt.map(([a, p, s, e, ms]) => new TableRow({ cantSplit: true, children: [
      cell(a, gW[0], { size: 16 }), cell(p, gW[1], { size: 15 }),
      ...days.map((d, i) => {
        const on = d >= s && d <= e;
        const mark = ms.includes(d) ? "◆" : "";
        return cell(mark, gW[i + 2], { fill: on ? (mark ? "C00000" : "2E75B6") : (d === 14 || d === 15 || d === 21 || d === 22 ? "F2F2F2" : undefined), color: "FFFFFF", bold: true, size: 14, align: AlignmentType.CENTER });
      }),
    ] })),
  ],
});

const sched = [
  ["Standar dan master template", "–", "11 Nov", "12 Nov 12.00", "12 Nov"],
  ["D01 Project Document", "12 Nov 21.00", "17 Nov", "20 Nov", "21 Nov"],
  ["D02 Architectural Design", "12 Nov 21.00", "14 Nov", "15 Nov (baseline); 20 Nov (v1.0)", "21 Nov"],
  ["D03 Functional Design", "12 Nov 21.00", "14 Nov", "15 Nov (baseline); 20 Nov (v1.0)", "21 Nov"],
  ["D04A / D04W / D04B", "12 Nov 21.00", "17 Nov", "20 Nov", "21 Nov"],
  ["D05A / D05W", "12 Nov 21.00", "16 Nov", "19 Nov (tangkapan layar diperbarui maks. 21 Nov)", "21 Nov"],
  ["D06 System Testing", "12 Nov 21.00", "17 Nov (kasus uji)", "22 Nov (hasil eksekusi)", "23 Nov"],
  ["D07A / D07W / D07B", "12 Nov 21.00", "19 Nov", "21 Nov", "22 Nov"],
  ["D08 System Program Demo", "12 Nov 21.00", "20 Nov (naskah)", "22 Nov (setelah gladi 21 Nov)", "23 Nov"],
  ["D09A / D09W", "12 Nov 21.00", "20 Nov", "22 Nov", "23 Nov"],
  ["Paket final seluruh dokumen", "–", "–", "24 Nov 18.00", "Kumpul 24 Nov, target 20.00 WIB"],
];

const checkCols = ["Outline (R0)", "Draf lengkap", "Pre-review (R1)", "Review teknis (R2)", "Konsistensi (R3)", "Disetujui PM", "Terkumpul"];
const checklistRows = volumes.map((v) => [v.id, v.name, v.pic, ...checkCols.map(() => ({ t: "☐", align: AlignmentType.CENTER }))]);

const bab4 = [
  H1("BAB IV  RENCANA EKSEKUSI DAN JADWAL"),
  H2("4.1 Fase Eksekusi Dokumentasi"),
  P("Fase eksekusi mengikuti gelombang pada Bagian 2.5. Setiap fase memiliki gerbang keluar (exit gate), yaitu kondisi yang harus terpenuhi sebelum fase berikutnya dianggap terbuka. Tanggal pada tabel adalah **usulan Project Manager** yang disusun mundur dari tenggat 24 November 2026 pukul 23.59 WIB."),
  CAP("Tabel 4.1 Fase eksekusi dan gerbang keluar"),
  table(["Fase", "Rentang (usulan)", "Keluaran utama", "Gerbang keluar"], [
    ["G0 Inisiasi dan standardisasi", "11–13 Nov", "Standar, template, register istilah, decision log, outline 15 volume, matriks status implementasi", "Semua outline lulus R0; keputusan framework Web dan produk DB tercatat"],
    ["G1 Baseline desain", "12–15 Nov", "D02 dan D03 baseline", "Baseline D02 dan D03 disetujui (15 Nov)"],
    ["G2 Desain rinci dan UI", "13–20 Nov", "D04A/W/B, D05A/W, kasus uji D06", "Draf lengkap lulus R1; diagram konsisten dengan baseline"],
    ["G3 Dokumen berbasis implementasi", "17–22 Nov", "D07A/W/B, hasil eksekusi D06, D09A/W", "Build baseline tersedia (18 Nov); setiap klaim memiliki bukti"],
    ["G4 Demo dan kendali mutu akhir", "18–24 Nov", "D08, laporan konsistensi, paket final", "Seluruh volume Disetujui PM; paket terkumpul"],
  ], [2600, 1700, 2900, PW - 7200], { boldFirst: true }),
  H2("4.2 Ketergantungan Tugas"),
  P("Ketergantungan tingkat tugas tercantum pada kolom Ketergantungan di Tabel 3.3. Prasyarat yang menjadi titik kendali utama adalah sebagai berikut:"),
  CAP("Tabel 4.2 Prasyarat kunci dan titik kendali"),
  table(["Prasyarat", "Pemilik", "Batas (usulan)", "Tugas yang terblokir bila terlambat"], [
    ["Master template (D00-WP1-T02)", "Rafi", "12 Nov 12.00", "Seluruh WP1 di 15 volume"],
    ["Keputusan framework Web dan produk DB (D00-WP2-T01)", "Salomo", "13 Nov", "D02-WP2/WP3, D04W-WP5, D07W-WP2, D04B-WP3"],
    ["Matriks status implementasi (D00-WP2-T02)", "Salomo", "13 Nov", "D01-WP4, D04A/W-WP1-T02, D05-WP1-T02, D08-WP1-T02"],
    ["Baseline D02 dan D03 (D02-WP5, D03-WP6)", "Salomo", "15 Nov", "D04A/W/B WP2–WP4, D05-WP2-T02, D06-WP1-T02"],
    ["Ringkasan endpoint API (D04B-WP5-T01)", "Salomo", "17 Nov", "D04A/W-WP2-T02, D06-WP2-T03, D07B-WP4"],
    ["Build baseline dokumentasi (D00-WP2-T03)", "Salomo", "18 Nov 21.00", "D06-WP3/WP4, D07 WP2, D08 WP2, D09 WP2–WP3, tangkapan layar final D05"],
    ["Hasil eksekusi D06 (D06-WP5-T01)", "Steven", "22 Nov", "D08-WP4-T02 (perbandingan perilaku), D01 konsistensi rencana uji"],
  ], [3500, 1200, 1500, PW - 6200], { boldFirst: true }),
  P("**Ketentuan bila implementasi belum siap.** Per 11 November 2026, backend dan aplikasi Android sudah berjalan secara terpisah, tetapi integrasi keduanya serta aplikasi Web belum selesai (Lampiran A). Oleh karena itu, volume berbasis implementasi wajib mendokumentasikan kondisi build baseline apa adanya. Fitur yang belum tersedia ditandai jelas dan tidak boleh digambarkan seolah sudah berjalan."),
];
const bab4L = [
  H2("4.3 Jadwal Usulan"),
  P("Gambar 4.1 menyajikan jadwal dalam bentuk Gantt. Seluruh tanggal adalah usulan dan dapat disesuaikan Project Manager melalui revision control dokumen ini. Kolom abu-abu menandai akhir pekan; tugas tetap dapat berjalan, tetapi review dijadwalkan pada hari kerja bila memungkinkan. Pengumpulan ditargetkan pukul 20.00 WIB sehingga tersisa penyangga 4 jam sebelum batas 23.59 WIB."),
  CAP("Gambar 4.1 Gantt chart rencana dokumentasi, 11–24 November 2026 (usulan)"),
  ganttTable,
  NOTE("Biru = rentang aktivitas; merah dengan ◆ = milestone (gerbang, baseline, atau tenggat volume); abu-abu = akhir pekan."),
  CAP("Tabel 4.3 Tenggat per volume (usulan)"),
  table(["Volume", "Outline (R0)", "Draf lengkap untuk R1", "Final untuk R2", "Disetujui PM"], sched, [4170, 2000, 2600, 3500, 2300], { boldFirst: true, zebra: true }),
  H2("4.4 Pemantauan Kemajuan"),
  P("Kemajuan dipantau melalui tiga mekanisme:"),
  B("**Laporan harian (pukul 21.00 WIB).** Setiap anggota melaporkan di kanal koordinasi tim dengan format tetap: Task ID selesai, Task ID berikutnya, blocker atau informasi yang belum tersedia. Blocker yang tidak terselesaikan dalam 24 jam dieskalasi ke Salomo."),
  B("**Status volume.** Siklus status: Belum Dimulai → Outline Diajukan → Outline Disetujui → Draf → Dalam Review → Revisi → Disetujui PM → Terkumpul, ditambah status Terblokir. Status hanya naik bila ada bukti berupa berkas pada versi yang sesuai."),
  B("**Bobot kemajuan.** Outline disetujui = 10%, draf lengkap = 50%, lulus R1 = 70%, lulus R2 = 85%, lulus R3 dan Disetujui PM = 100%. Bobot dipakai untuk melihat deviasi terhadap jadwal, bukan untuk menyatakan selesai."),
  P("Tabel 4.4 adalah Documentation Completion Checklist (Artefak E) yang diperbarui Salomo setiap hari. Checklist mutu per dokumen tercantum pada Bagian 5.5."),
  CAP("Tabel 4.4 Documentation Completion Checklist (Artefak E)"),
  table(["ID", "Volume", "PIC", ...checkCols], checklistRows, [902, 3300, 1100, ...Array(7).fill(1324)], { size: 17, boldFirst: true, zebra: true }),
];
// fix last widths sum
{
  const w = [902, 3300, 1100, ...Array(7).fill(1324)];
  const diff = LW - w.reduce((a, b) => a + b, 0);
  if (diff !== 0) console.error("checklist width diff", diff);
}

// =====================================================================
// BAB V
// =====================================================================
const bab5 = [
  H1("BAB V  KENDALI MUTU DAN REVIEW"),
  H2("5.1 Standar Mutu Dokumentasi"),
  ...NL([
    "**Struktur wajib.** Setiap volume memuat seluruh field template dosen, yaitu Nama Module, Status, Created by, Last updated by, Responsible team, Revision control, Daftar Isi, dan isi inti. Daftar Isi juga dipakai pada Detailed Design sebagai standar internal, meskipun template Dokumen 4 tidak mencantumkannya.",
    "**Bahasa.** Bahasa Indonesia akademik formal. Istilah teknis baku (UML, API, endpoint, sequence diagram) dipertahankan dan ditulis konsisten sesuai register istilah.",
    "**Keterlacakan.** Rujukan kebutuhan memakai ID SRS (FR-xx, BR-xx, NFR-xx). Rujukan layar memakai kode D05 (AND-xx, WEB-xx). Rujukan API memakai daftar endpoint D04B.",
    "**Kejujuran isi.** Tidak ada hasil uji, tangkapan layar, kode, atau tanggal realisasi yang direkayasa. Informasi yang belum tersedia ditulis 'Perlu Ditentukan' beserta pemilik keputusannya.",
    "**Format.** Memakai master template. Gambar dan tabel diberi nomor dan keterangan. Diagram diekspor dalam resolusi yang terbaca.",
    "**Konvensi nama berkas.** DDB_[ID]_[Nama_Dokumen]_v[versi].docx, misalnya DDB_D04W_Detailed_Design_Web_v1.0.docx. Versi 0.x untuk draf dan 1.0 untuk versi yang disetujui.",
  ]),
  H2("5.2 Proses Review dan Revisi"),
  P("Review dilakukan bertahap agar kesalahan ditemukan sedini mungkin dan antrean Salomo tidak menumpuk di akhir."),
  CAP("Tabel 5.1 Tahapan review"),
  table(["Tahap", "Pelaksana", "Fokus", "Waktu respons"], [
    ["R0 Review outline", "Salomo", "Kelengkapan struktur terhadap template dan cakupan", "≤ 12 jam"],
    ["R1 Pre-review pasangan", "Pasangan review: Gracia ↔ Cecilia, Rafi ↔ Nabila, Grace ↔ Cecilia, Permata ↔ Nabila, dokumen Salomo ↔ Steven/Rafi", "Kelengkapan checklist, format, keterbacaan, langkah dapat diikuti", "≤ 12 jam"],
    ["R2 Review teknis", "Salomo (untuk volume Salomo: Steven)", "Kebenaran teknis, konsistensi dengan baseline D02/D03/D04B, kejujuran isi", "≤ 24 jam"],
    ["R3 Review konsistensi", "Salomo, dibantu Rafi dan Permata", "Matriks konsistensi Bagian 5.3 terhadap seluruh volume", "22–23 Nov"],
    ["Persetujuan", "Salomo", "Keputusan Disetujui PM; versi naik ke 1.0", "23 Nov"],
  ], [2200, 3400, 2700, PW - 8300], { boldFirst: true }),
  P("Temuan review dicatat pada tabel revisi di dalam volume, berisi nomor temuan, lokasi, uraian, tindakan, dan status. PIC menindaklanjuti temuan paling lambat 24 jam. Setiap siklus revisi menaikkan versi minor (0.2, 0.3, …) dan dicatat di revision control."),
  H2("5.3 Konsistensi Lintas Dokumen"),
  P("Setiap unsur yang muncul di lebih dari satu volume memiliki satu sumber kebenaran. Bila terjadi perbedaan, sumber kebenaran yang berlaku dan volume hilir yang diperbaiki."),
  CAP("Tabel 5.2 Matriks konsistensi"),
  table(["Unsur", "Sumber kebenaran", "Volume yang wajib selaras"], [
    ["Nama modul, FR, BR, NFR", "SRS-DDB-001 v0.1 melalui register istilah", "Semua volume"],
    ["Komponen arsitektur dan teknologi", "D02 dan decision log", "D04A/W/B, D07A/W/B, D08"],
    ["Spesifikasi fungsi dan workflow", "D03", "D04A/W/B, D05A/W, D06, D09A/W"],
    ["Kode dan nama layar/halaman", "D05A, D05W", "D04A/W (workflow), D06, D08, D09A/W"],
    ["Endpoint API dan model data", "D04B", "D04A/W, D06, D07A/W/B"],
    ["Hasil pengujian", "D06", "D08 (bukti), D01 (kesesuaian rencana uji)"],
    ["Versi build", "Build baseline (D00-WP2-T03)", "D05 (tangkapan layar), D06, D07, D08, D09"],
    ["Nama, NIM, dan peran anggota", "Tabel 1.2 dokumen ini", "Blok kontrol seluruh volume"],
  ], [3000, 3200, PW - 6200], { boldFirst: true, zebra: true }),
  H2("5.4 Persetujuan Project Manager"),
  P("Salomo adalah final reviewer dan otoritas persetujuan untuk sembilan kategori dokumentasi. Tanggung jawabnya:"),
  B("menetapkan standar dan decision log, serta memastikan keputusan teknis tetap konsisten dengan arsitektur yang disetujui;"),
  B("melaksanakan R2 untuk volume yang tidak ia tulis dan R3 untuk seluruh volume;"),
  B("menolak volume yang belum memenuhi Definition of Done, dengan alasan tertulis dan tindakan perbaikan yang jelas;"),
  B("mencatat persetujuan pada revision control setiap volume (versi 1.0, tanggal, 'Disetujui PM');"),
  B("memastikan volume yang ia tulis sendiri telah ditelaah Steven sebelum ia setujui, sehingga tidak terjadi persetujuan tanpa telaah independen."),
  P("Persetujuan PM bukan formalitas. Dokumen tidak dianggap selesai hanya karena drafnya ada."),
  H2("5.5 Definition of Done"),
  P("Sebuah volume dinyatakan **Done** bila seluruh butir berikut terpenuhi:"),
  ...[
    "Seluruh komponen wajib template dosen untuk kategori tersebut tersedia dan terisi.",
    "Blok kontrol dokumen lengkap; revision control mencatat seluruh versi sampai 1.0.",
    "Daftar isi, nomor gambar, dan nomor tabel mutakhir.",
    "Seluruh ID kebutuhan, nama modul, kode layar, dan endpoint sesuai sumber kebenaran (Tabel 5.2).",
    "Isi berbasis bukti: tangkapan layar dan penjelasan kode berasal dari build baseline; hasil uji berasal dari eksekusi nyata.",
    "Tidak ada placeholder tanpa label; butir yang belum tersedia ditandai 'Perlu Ditentukan' beserta pemiliknya.",
    "Lulus R1 dan R2; seluruh temuan ditutup atau dicatat sebagai keterbatasan.",
    "Lulus R3 (konsistensi lintas dokumen).",
    "Persetujuan PM tercatat; berkas bernama sesuai konvensi dan tersimpan di folder final.",
  ].map((t) => new Paragraph({ children: runs("☐  " + t), spacing: { after: 60 }, indent: { left: 360 } })),
];

// =====================================================================
// BAB VI + LAMPIRAN
// =====================================================================
const bab6 = [
  H1("BAB VI  PENUTUP"),
  P(`Rencana kerja ini menguraikan sasaran dokumentasi proyek PT Diponegoro Digital Bank secara top-down: dari satu tujuan keseluruhan menjadi 9 kategori dan 15 volume, lalu menjadi komponen wajib template, ${Object.values(wpPerDoc).reduce((a, s) => a + s.size, 0)} work package, dan ${totalTasks} tugas individual. Setiap tugas memiliki satu PIC, ketergantungan, keluaran, dan kriteria selesai yang dapat diperiksa.`),
  P("Kontribusi utama rencana ini terhadap pelaksanaan proyek ada tiga. Pertama, jalur kritis menjadi terlihat: keputusan teknologi, baseline D02/D03, dan build baseline dapat dikendalikan sebagai titik kendali harian. Kedua, pekerjaan didelegasikan secara seimbang tanpa mengorbankan kendali mutu. Salomo tetap memegang backend dan persetujuan akhir, sedangkan penyusunan template, register istilah, pre-review, eksekusi uji, dan sebagian Developer Manual dilimpahkan kepada anggota lain dengan tanggung jawab eksplisit. Ketiga, konsistensi dan kejujuran isi dijaga melalui matriks sumber kebenaran, review bertahap, dan Definition of Done yang menolak klaim tanpa bukti."),
  P("Rencana ini adalah dokumen hidup. Perubahan jadwal, penugasan, atau ruang lingkup dicatat pada revision control dan diumumkan kepada seluruh anggota sebelum berlaku."),
  H1("LAMPIRAN A  TEMUAN SUMBER, ASUMSI, DAN KEPUTUSAN PROJECT OWNER"),
  P("Lampiran ini mencatat temuan dari pemeriksaan sumber. Temuan dicatat terbuka dan tidak diselesaikan secara diam-diam."),
  CAP("Tabel A.1 Temuan dan penanganan"),
  table(["No", "Temuan", "Sumber", "Penanganan dalam rencana ini"], [
    ["1", "Dokumen WBS tidak tersedia di folder proyek. Yang tersedia adalah MDTA-DDB-001 v0.1 (Module Decomposition & Task Allocation).", "Folder documents_area", "MDTA dipakai sebagai acuan dekomposisi teknis. Bila WBS diserahkan, rencana ini direvisi."],
    ["2", "SRS masih v0.1 dan berstatus draf parsial (Bagian 1–4). Antarmuka eksternal, kebutuhan data, dan matriks keterlacakan dijadwalkan pada v0.2.", "SRS 1.5, penutup Bagian 4", "D03 menandai butir yang belum tersedia 'Perlu Ditentukan'. D06 menyusun matriks keterlacakan sendiri dari FR v0.1."],
    ["3", "SRS menyebut aplikasi Android dan iOS, sedangkan proyek ini mengembangkan Native Android.", "SRS 2.1, 2.4, NFR-10", "iOS dicatat di luar ruang lingkup dokumentasi."],
    ["4", "MDTA menulis Backend sebagai slot BE-1 s.d. BE-4 tanpa nama.", "MDTA 3.1, 6", "Keputusan Project Owner (11 Nov 2026): seluruh Backend dan Database dipegang Salomo, termasuk volume D04B dan D07B."],
    ["5", "MDTA memakai nama panggilan (Cia, Stepen, Surya, Raffi).", "MDTA 3.1, 4, 6", "Cia = Gracia Riyanto terkonfirmasi dari repositori (folder Modul-gracia dan uml-modul-cia). Stepen = Steven Wibisana, Surya = Permata Surya Cahyana, Raffi = Rafi Fadhillah masih asumsi. Catatan: pembagian modul Android aktual (submit_mobile/README.md) berbeda dari MDTA; v1.1 mengikuti README."],
    ["6", "NIM Grace (6220204010) terdiri atas 10 digit, sedangkan NIM anggota lain 9 digit.", "Daftar anggota kelompok", "Dicantumkan sesuai daftar resmi kelompok; perlu verifikasi sebelum blok kontrol volume difinalkan."],
    ["7", "Penulisan nama PIC Dokumen 8 tertulis 'Permata Surya Cahy ana'.", "Brief penugasan", "Ditulis 'Permata Surya Cahyana' sesuai daftar anggota."],
    ["8", "Brief penugasan menyebut empat PIC Dokumen 8 dan dua PIC Developer Manual Web tanpa PIC utama.", "Brief penugasan", "Sesuai aturan satu PIC utama: Rafi sebagai PIC utama D08 dan Steven sebagai PIC utama D07W; anggota lain menjadi pendukung dengan tugas eksplisit."],
    ["9", "Template dosen Dokumen 4 tidak mencantumkan Daftar Isi, sedangkan template Dokumen 8 mencantumkan blok kontrol dokumen lengkap.", "Template Dokumentasi Project", "Rencana ini memakai gabungan keduanya: Daftar Isi dipakai di semua volume; D08 memuat blok kontrol lengkap."],
    ["10", "Dokumen 1 (rencana testing) dan Dokumen 6 (pengujian sistem) beririsan.", "Template; brief penugasan", "D01 memuat model dan rencana pengujian tingkat tinggi; D06 memuat kasus uji, eksekusi, dan hasil. Steven menjadi Consulted pada D01-WP5."],
    ["11", "Folder code_area kosong, tetapi kode berada di repositori kerja PM (submit_progress_digital_banking_project): backend 8 modul (44 test lulus), aplikasi Android 8 modul (54 test lulus), prototipe Web awal.", "Folder proyek; repositori kerja PM", "Status implementasi dicatat berbasis bukti pada PRJ-DDB-001 Tabel 1.6. Integrasi Android–backend dan Web belum selesai."],
    ["12", "Jadwal indikatif Project Brief memakai Minggu 1–16, sedangkan Project Owner menetapkan rentang proyek 15 Sep–23 Nov 2026 (10 minggu).", "Project Brief §10.2; keputusan Project Owner 11 Nov 2026", "Rentang 15 Sep–23 Nov 2026 dipakai pada Gantt chart Dokumen 1. Integrasi Android–backend, Web, dan pengujian sistem masih berlangsung sehingga D06–D09 tetap mendokumentasikan kondisi build baseline 18 Nov."],
    ["13", "Framework Web dan produk basis data masih 'Perlu Ditentukan'.", "MDTA 1.2, 12.1", "Diselesaikan pada D00-WP2-T01 paling lambat 13 Nov."],
    ["14", "Estimasi biaya belum memiliki data dasar (tarif, metode).", "Template Dokumen 1", "Metode dan asumsi ditetapkan pada D01-WP7-T01 dan ditulis eksplisit."],
    ["15", "Project Brief menyebut deliverable SDD, sedangkan template dosen memakai Architectural, Functional, dan Detailed Design.", "Project Brief §10.3", "Isi SDD dipetakan ke D02, D03, D04A/W/B."],
  ], [500, 3500, 1900, PW - 5900], { size: 17, center: [0] }),
  CAP("Tabel A.2 Daftar sumber acuan"),
  table(["Dokumen", "Versi / tanggal", "Pemakaian"], [
    ["Software Requirements Specification PT Diponegoro Digital Bank (SRS-DDB-001)", "0.1, 2 Okt 2026", "Cakupan fungsional, FR/BR/NFR, UC-01"],
    ["Project Brief & Scope Document", "1.0, 2 Okt 2026", "Deskripsi, fase indikatif, risiko, organisasi"],
    ["Module Decomposition & Task Allocation (MDTA-DDB-001)", "0.1, 4 Okt 2026", "Modul, PIC teknis per layer, ketergantungan, fase pengembangan"],
    ["Software Engineering – Dokumentasi Project (UKSW)", "Materi kuliah", "Komponen wajib sembilan dokumen"],
    ["Daftar anggota Kelompok Jaran Goyang", "Diserahkan 11 Nov 2026", "Nama dan NIM"],
  ], [4600, 2000, PW - 6600], { zebra: true }),
];

// =====================================================================
const doc = new Document({
  creator: "Salomo Putra Glorious Hokoyoku",
  title: "DWP-DDB-001 Software Engineering Documentation Work Plan",
  description: "Rencana kerja dokumentasi sembilan kategori Software Engineering – PT Diponegoro Digital Bank",
  features: { updateFields: true },
  styles: {
    default: { document: { run: { font: FONT, size: 21 } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { font: FONT, size: 30, bold: true, color: NAVY }, paragraph: { spacing: { before: 240, after: 200 }, keepNext: true, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { font: FONT, size: 25, bold: true, color: NAVY }, paragraph: { spacing: { before: 240, after: 120 }, keepNext: true, outlineLevel: 1 } },
      { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { font: FONT, size: 22, bold: true, color: "2E5597" }, paragraph: { spacing: { before: 180, after: 80 }, keepNext: true, outlineLevel: 2 } },
    ],
  },
  numbering: { config: [
    { reference: "bul", levels: [
      { level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 540, hanging: 270 } } } },
      { level: 1, format: LevelFormat.BULLET, text: "–", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 1000, hanging: 270 } } } },
    ] },
    { reference: "num", levels: [
      { level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 540, hanging: 360 } } } },
    ] },
  ] },
  sections: [
    portrait([...cover, ...control, ...bab1, ...bab2], true),
    landscape(bab3),
    portrait(bab4),
    landscape(bab4L),
    portrait([...bab5, ...bab6]),
  ],
});

Packer.toBuffer(doc).then((buf) => { fs.writeFileSync(OUT, buf); console.log("written", OUT, buf.length, "bytes; tasks", totalTasks); });
