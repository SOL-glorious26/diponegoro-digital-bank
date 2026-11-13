package com.diponegoro.ddb.topup;

import com.diponegoro.ddb.common.Idempotency;
import com.diponegoro.ddb.ledger.TransactionDto;
import com.diponegoro.ddb.security.AuthUser;
import com.diponegoro.ddb.topup.TopUpCatalog.Biller;
import com.diponegoro.ddb.topup.TopUpCatalog.EWallet;
import com.diponegoro.ddb.topup.TopUpCatalog.Kind;
import com.diponegoro.ddb.topup.TopUpService.BillInquiry;
import com.diponegoro.ddb.topup.TopUpService.DataPackages;
import com.diponegoro.ddb.topup.TopUpService.Order;
import com.diponegoro.ddb.topup.TopUpService.OrderRequest;
import com.diponegoro.ddb.topup.TopUpService.PlnCustomer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/topup")
@Tag(name = "5. Top-up", description = "FR-18 – FR-20: pulsa, paket data, token listrik, tagihan, e-wallet")
public class TopUpController {
    private final TopUpService topups;

    public TopUpController(TopUpService topups) {
        this.topups = topups;
    }

    public record OptionDto(String code, String label) {}

    public record Catalog(List<TopUpCatalog.Operator> operators, List<Long> pulsaNominals, List<Long> plnNominals,
                          long pulsaFee, long plnFee, long billFee, long ewalletFee, long ewalletMin, long ewalletMax,
                          List<OptionDto> billers, List<OptionDto> ewallets) {}

    public record ExecuteRequest(Kind kind, String phone, Long nominal, String productId, String meterNumber,
                                 Biller biller, String customerNumber, EWallet wallet, Long amount,
                                 @NotBlank(message = "PIN wajib diisi") String pin) {
        OrderRequest order() {
            return new OrderRequest(kind, phone, nominal, productId, meterNumber, biller, customerNumber, wallet, amount);
        }
    }

    @GetMapping("/catalog")
    @Operation(summary = "Katalog: operator, nominal, biaya, biller, e-wallet")
    public Catalog catalog() {
        return new Catalog(TopUpCatalog.OPERATORS, TopUpCatalog.PULSA_NOMINALS, TopUpCatalog.PLN_NOMINALS,
                TopUpCatalog.PULSA_FEE, TopUpCatalog.PLN_ADMIN_FEE, TopUpCatalog.BILL_ADMIN_FEE, TopUpCatalog.EWALLET_FEE,
                TopUpCatalog.EWALLET_MIN, TopUpCatalog.EWALLET_MAX,
                Arrays.stream(Biller.values()).map(b -> new OptionDto(b.name(), b.label)).toList(),
                Arrays.stream(EWallet.values()).map(w -> new OptionDto(w.name(), w.label)).toList());
    }

    @GetMapping("/data-packages")
    @Operation(summary = "Deteksi operator dari nomor HP dan daftar paket data (FR-18)")
    public DataPackages dataPackages(@RequestParam String phone) {
        return topups.dataPackages(phone);
    }

    @GetMapping("/pln/inquiry")
    @Operation(summary = "Cek ID pelanggan PLN (FR-19). Berakhiran 0000 = tidak ditemukan.")
    public PlnCustomer pln(@RequestParam String meterNumber) {
        return topups.inquirePln(meterNumber);
    }

    @GetMapping("/bills/inquiry")
    @Operation(summary = "Cek tagihan PDAM/BPJS/Internet (FR-20)")
    public BillInquiry bill(@RequestParam Biller biller, @RequestParam String customerNumber) {
        return topups.inquireBill(biller, customerNumber);
    }

    @PostMapping("/quote")
    @Operation(summary = "Ringkasan pesanan untuk layar konfirmasi")
    public Order quote(@RequestBody OrderRequest r) {
        return topups.quote(r);
    }

    @PostMapping("/orders")
    @Operation(summary = "Bayar pesanan dengan PIN. Wajib header Idempotency-Key.")
    public TransactionDto execute(@AuthenticationPrincipal AuthUser me, @RequestHeader(Idempotency.HEADER) String key,
                                  @Valid @RequestBody ExecuteRequest r) {
        return topups.execute(me.userId(), r.order(), r.pin(), Idempotency.key(me.userId(), key));
    }
}
