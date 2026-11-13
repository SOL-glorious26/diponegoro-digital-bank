package com.diponegoro.ddb.history;

import com.diponegoro.ddb.common.ApiException;
import com.diponegoro.ddb.history.HistoryService.DateRange;
import com.diponegoro.ddb.history.HistoryService.Filter;
import com.diponegoro.ddb.history.HistoryService.HistoryPage;
import com.diponegoro.ddb.history.HistoryService.MonthSummary;
import com.diponegoro.ddb.history.HistoryService.TransactionDetail;
import com.diponegoro.ddb.ledger.LedgerModels.Direction;
import com.diponegoro.ddb.ledger.LedgerModels.TxCategory;
import com.diponegoro.ddb.ledger.LedgerModels.TxStatus;
import com.diponegoro.ddb.ledger.LedgerService;
import com.diponegoro.ddb.ledger.TransactionDto;
import com.diponegoro.ddb.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.StringJoiner;

@RestController
@RequestMapping("/api/v1/history")
@Tag(name = "7. History & Dashboard", description = "FR-23 – FR-26: riwayat, filter, detail, grafik bulanan, ekspor")
public class HistoryController {
    private final HistoryService history;
    private final HistoryExporter exporter;
    private final LedgerService ledger;
    private final Clock clock;

    public HistoryController(HistoryService history, HistoryExporter exporter, LedgerService ledger, Clock clock) {
        this.history = history;
        this.exporter = exporter;
        this.ledger = ledger;
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "Riwayat terbaru dulu dengan paginasi; filter & pencarian dapat dikombinasikan (FR-23)")
    public HistoryPage list(@AuthenticationPrincipal AuthUser me,
                            @RequestParam(defaultValue = "ALL") DateRange range,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            @RequestParam(required = false) TxCategory category,
                            @RequestParam(required = false) Direction direction,
                            @RequestParam(required = false) TxStatus status,
                            @RequestParam(required = false) String q,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size) {
        return history.page(me.userId(), new Filter(range, from, to, category, direction, status, q), page, size);
    }

    @GetMapping("/{txId}")
    @Operation(summary = "Detail transaksi + baris jurnal ledger (FR-24)")
    public TransactionDetail detail(@AuthenticationPrincipal AuthUser me, @PathVariable String txId) {
        return history.detail(me.userId(), txId);
    }

    @GetMapping("/monthly-summary")
    @Operation(summary = "Pemasukan & pengeluaran per bulan untuk grafik dashboard (FR-25)")
    public List<MonthSummary> monthly(@AuthenticationPrincipal AuthUser me,
                                      @RequestParam(defaultValue = "6") int months,
                                      @RequestParam(defaultValue = "0") int endMonthsBack) {
        return history.monthSeries(me.userId(), months, endMonthsBack);
    }

    @GetMapping("/export")
    @Operation(summary = "Ekspor riwayat sesuai filter aktif ke CSV atau PDF (FR-26, Web)")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal AuthUser me,
                                         @RequestParam(defaultValue = "csv") String format,
                                         @RequestParam(defaultValue = "ALL") DateRange range,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) TxCategory category,
                                         @RequestParam(required = false) Direction direction,
                                         @RequestParam(required = false) TxStatus status,
                                         @RequestParam(required = false) String q) {
        Filter f = new Filter(range, from, to, category, direction, status, q);
        List<TransactionDto> rows = history.all(me.userId(), f);
        String stamp = LocalDate.now(clock.withZone(com.diponegoro.ddb.common.TimeConfig.ZONE)).toString();
        byte[] body;
        MediaType type;
        String name;
        switch (format.toLowerCase()) {
            case "csv" -> { body = exporter.csv(rows); type = new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8); name = "riwayat-" + stamp + ".csv"; }
            case "pdf" -> {
                String owner = ledger.requireMainAccount(me.userId()).name;
                body = exporter.pdf(rows, owner, label(f), clock.instant());
                type = MediaType.APPLICATION_PDF;
                name = "riwayat-" + stamp + ".pdf";
            }
            default -> throw ApiException.badRequest("INVALID_FORMAT", "Format ekspor harus csv atau pdf");
        }
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                .body(body);
    }

    private static String label(Filter f) {
        StringJoiner j = new StringJoiner(", ");
        j.add(f.range() == DateRange.CUSTOM ? f.from() + " s.d. " + f.to() : f.range().name());
        if (f.category() != null) j.add(f.category().label);
        if (f.direction() != null) j.add(f.direction().name());
        if (f.status() != null) j.add(f.status().label);
        if (f.q() != null && !f.q().isBlank()) j.add("\"" + f.q().trim() + "\"");
        return j.toString();
    }
}
