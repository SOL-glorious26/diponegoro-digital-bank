package com.diponegoro.ddb.history;

import com.diponegoro.ddb.common.Money;
import com.diponegoro.ddb.common.TimeConfig;
import com.diponegoro.ddb.ledger.LedgerModels.Direction;
import com.diponegoro.ddb.ledger.TransactionDto;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Ekspor riwayat transaksi ke CSV/PDF sesuai filter aktif (FR-26, platform Web). */
@Component
public class HistoryExporter {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.of("id", "ID"))
            .withZone(TimeConfig.ZONE);

    public byte[] csv(List<TransactionDto> rows) {
        StringBuilder sb = new StringBuilder("﻿"); // BOM agar Excel membaca UTF-8
        sb.append("ID Transaksi,Waktu (WIB),Jenis,Keterangan,Dari,Ke,Arah,Nominal,Biaya,Total,Status,Catatan\n");
        for (TransactionDto t : rows) {
            sb.append(String.join(",",
                    esc(t.reference()), esc(TIME.format(t.createdAt())), esc(t.categoryLabel()), esc(t.title()),
                    esc(t.fromName()), esc(t.toName()), esc(arah(t.direction())),
                    String.valueOf(signed(t)), String.valueOf(t.fee()), String.valueOf(t.total()),
                    esc(t.statusLabel()), esc(t.note()))).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] pdf(List<TransactionDto> rows, String owner, String filterLabel, Instant generatedAt) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        PdfWriter.getInstance(doc, out);
        doc.open();
        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font head = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        doc.add(new Paragraph("Diponegoro Digital Bank — Riwayat Transaksi", title));
        doc.add(new Paragraph("Pemilik: " + owner + "   |   Filter: " + filterLabel + "   |   Dibuat: "
                + TIME.format(generatedAt) + " WIB   |   " + rows.size() + " transaksi", small));
        doc.add(new Paragraph(" ", small));

        float[] widths = {14, 12, 9, 18, 16, 7, 10, 8};
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        for (String h : List.of("ID Transaksi", "Waktu (WIB)", "Jenis", "Keterangan", "Pihak lawan", "Arah", "Nominal", "Status")) {
            PdfPCell c = new PdfPCell(new Phrase(h, head));
            c.setGrayFill(0.9f);
            table.addCell(c);
        }
        for (TransactionDto t : rows) {
            table.addCell(new Phrase(t.reference(), small));
            table.addCell(new Phrase(TIME.format(t.createdAt()), small));
            table.addCell(new Phrase(t.categoryLabel(), small));
            table.addCell(new Phrase(t.title(), small));
            table.addCell(new Phrase(t.direction() == Direction.IN ? t.fromName() : t.toName(), small));
            table.addCell(new Phrase(arah(t.direction()), small));
            PdfPCell amount = new PdfPCell(new Phrase(Money.rupiah(signed(t)), small));
            amount.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(amount);
            table.addCell(new Phrase(t.statusLabel(), small));
        }
        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    private static long signed(TransactionDto t) {
        return switch (t.direction()) {
            case IN -> t.amount();
            case OUT -> -t.total();
            case INTERNAL -> t.amount();
        };
    }

    private static String arah(Direction d) {
        return switch (d) { case IN -> "Masuk"; case OUT -> "Keluar"; case INTERNAL -> "Antar-kantong"; };
    }

    /** CSV aman: kutip ganda + cegah CSV/formula injection di Excel. */
    private static String esc(String v) {
        String s = v == null ? "" : v;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
