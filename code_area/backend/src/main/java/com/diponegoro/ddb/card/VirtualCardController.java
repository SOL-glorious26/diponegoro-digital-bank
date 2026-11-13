package com.diponegoro.ddb.card;

import com.diponegoro.ddb.card.VirtualCard.Scope;
import com.diponegoro.ddb.card.VirtualCardService.CardDetails;
import com.diponegoro.ddb.card.VirtualCardService.CardDto;
import com.diponegoro.ddb.common.Idempotency;
import com.diponegoro.ddb.ledger.TransactionDto;
import com.diponegoro.ddb.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
@Tag(name = "6. Virtual Card", description = "FR-21 – FR-22: buat kartu, detail ber-PIN, bekukan/aktifkan, limit")
public class VirtualCardController {
    private final VirtualCardService cards;

    public VirtualCardController(VirtualCardService cards) {
        this.cards = cards;
    }

    public record CreateRequest(@NotNull(message = "Pilih cakupan penggunaan") Scope usageScope) {}

    public record PinRequest(@NotBlank(message = "PIN wajib diisi") String pin) {}

    public record LimitRequest(@NotNull(message = "Limit wajib diisi") Long limit) {}

    public record PurchaseRequest(@NotNull Long amount, Boolean international, String merchant) {}

    @GetMapping
    public List<CardDto> list(@AuthenticationPrincipal AuthUser me) {
        return cards.list(me.userId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Buat virtual card domestik/internasional (FR-21)")
    public CardDto create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateRequest r) {
        return cards.create(me.userId(), r.usageScope());
    }

    @PostMapping("/{id}/details")
    @Operation(summary = "Lihat nomor lengkap, masa berlaku, CVV — wajib PIN (FR-22)")
    public CardDetails details(@AuthenticationPrincipal AuthUser me, @PathVariable String id, @Valid @RequestBody PinRequest r) {
        return cards.details(me.userId(), id, r.pin());
    }

    @PostMapping("/{id}/freeze")
    public CardDto freeze(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return cards.freeze(me.userId(), id);
    }

    @PostMapping("/{id}/activate")
    public CardDto activate(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return cards.activate(me.userId(), id);
    }

    @PutMapping("/{id}/limit")
    @Operation(summary = "Atur limit per transaksi (berlaku untuk transaksi berikutnya)")
    public CardDto limit(@AuthenticationPrincipal AuthUser me, @PathVariable String id, @Valid @RequestBody LimitRequest r) {
        return cards.setLimit(me.userId(), id, r.limit());
    }

    @PostMapping("/{id}/simulate-purchase")
    @Operation(summary = "[Simulator] Transaksi merchant memakai kartu. Wajib header Idempotency-Key.")
    public TransactionDto purchase(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                   @RequestHeader(Idempotency.HEADER) String key, @Valid @RequestBody PurchaseRequest r) {
        return cards.simulatePurchase(me.userId(), id, r.amount(), Boolean.TRUE.equals(r.international()), r.merchant(),
                Idempotency.key(me.userId(), key));
    }
}
