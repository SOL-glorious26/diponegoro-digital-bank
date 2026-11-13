package com.diponegoro.ddb.payment;

import com.diponegoro.ddb.common.Idempotency;
import com.diponegoro.ddb.ledger.TransactionDto;
import com.diponegoro.ddb.payment.PaymentService.DemoQr;
import com.diponegoro.ddb.payment.PaymentService.MyQr;
import com.diponegoro.ddb.payment.PaymentService.ScanResult;
import com.diponegoro.ddb.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/qris")
@Tag(name = "4. Payment QRIS", description = "FR-15 – FR-17: pindai & bayar, QRIS pribadi, QRIS dinamis")
public class PaymentController {
    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    public record ScanRequest(@NotBlank(message = "Kode QRIS kosong") String payload) {}

    /** amount diabaikan untuk QRIS dinamis. */
    public record PayRequest(@NotBlank String payload, Long amount, @NotBlank(message = "PIN wajib diisi") String pin) {}

    public record IncomingRequest(@NotNull(message = "Nominal wajib diisi") Long amount, String payerName) {}

    @PostMapping("/scan")
    @Operation(summary = "Validasi hasil pindai QRIS → merchant & nominal sebelum PIN (FR-15)")
    public ScanResult scan(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ScanRequest r) {
        return payments.scan(me.userId(), r.payload());
    }

    @PostMapping("/pay")
    @Operation(summary = "Bayar QRIS dengan PIN (FR-15). Wajib header Idempotency-Key.")
    public TransactionDto pay(@AuthenticationPrincipal AuthUser me, @RequestHeader(Idempotency.HEADER) String key,
                              @Valid @RequestBody PayRequest r) {
        return payments.pay(me.userId(), r.payload(), r.amount(), r.pin(), Idempotency.key(me.userId(), key));
    }

    @GetMapping("/my-qr")
    @Operation(summary = "QRIS pribadi untuk menerima dana (FR-16); dengan amount = QRIS dinamis (FR-17)")
    public MyQr myQr(@AuthenticationPrincipal AuthUser me, @RequestParam(required = false) Long amount) {
        return payments.personalQr(me.userId(), amount);
    }

    @PostMapping("/simulate-incoming")
    @Operation(summary = "[Simulator] Pengguna bank lain membayar ke QRIS pribadi Anda")
    public TransactionDto incoming(@AuthenticationPrincipal AuthUser me, @RequestHeader(Idempotency.HEADER) String key,
                                   @Valid @RequestBody IncomingRequest r) {
        return payments.simulateIncoming(me.userId(), r.amount(), r.payerName(), Idempotency.key(me.userId(), key));
    }

    @GetMapping("/demo")
    @Operation(summary = "[Simulator] Contoh kode QRIS untuk uji tanpa kamera")
    public List<DemoQr> demo() {
        return payments.demoQrs();
    }
}
