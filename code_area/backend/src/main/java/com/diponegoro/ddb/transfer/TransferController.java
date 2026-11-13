package com.diponegoro.ddb.transfer;

import com.diponegoro.ddb.common.Idempotency;
import com.diponegoro.ddb.ledger.TransactionDto;
import com.diponegoro.ddb.security.AuthUser;
import com.diponegoro.ddb.transfer.TransferDtos.BankDto;
import com.diponegoro.ddb.transfer.TransferDtos.FavoriteDto;
import com.diponegoro.ddb.transfer.TransferDtos.FavoriteRequest;
import com.diponegoro.ddb.transfer.TransferDtos.InquiryRequest;
import com.diponegoro.ddb.transfer.TransferDtos.QuoteDto;
import com.diponegoro.ddb.transfer.TransferDtos.QuoteRequest;
import com.diponegoro.ddb.transfer.TransferDtos.RecipientDto;
import com.diponegoro.ddb.transfer.TransferDtos.TransferRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "3. Transfer", description = "FR-10 – FR-14, UC-01: sesama & antarbank, validasi rekening, favorit, bukti")
public class TransferController {
    private final TransferService transfers;

    public TransferController(TransferService transfers) {
        this.transfers = transfers;
    }

    @GetMapping("/banks")
    @Operation(summary = "Daftar bank tujuan (simulasi) beserta biayanya")
    public List<BankDto> banks() {
        return transfers.banks();
    }

    @PostMapping("/inquiry")
    @Operation(summary = "Validasi rekening tujuan & tampilkan nama penerima (FR-12)")
    public RecipientDto inquiry(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody InquiryRequest r) {
        return transfers.inquiry(me.userId(), r.bankCode(), r.accountNumber());
    }

    @PostMapping("/quote")
    @Operation(summary = "Layar konfirmasi: cek nominal, limit, saldo, biaya (UC-01 langkah 3–4)")
    public QuoteDto quote(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody QuoteRequest r) {
        return transfers.quote(me.userId(), r.bankCode(), r.accountNumber(), r.amount(), r.note());
    }

    @PostMapping
    @Operation(summary = "Eksekusi transfer dengan PIN (FR-10, FR-11). Wajib header Idempotency-Key.")
    public TransactionDto transfer(@AuthenticationPrincipal AuthUser me,
                                   @RequestHeader(Idempotency.HEADER) String key,
                                   @Valid @RequestBody TransferRequest r) {
        return transfers.execute(me.userId(), r.bankCode(), r.accountNumber(), r.amount(), r.note(), r.pin(),
                Idempotency.key(me.userId(), key));
    }

    @PostMapping("/{txId}/check-status")
    @Operation(summary = "Cek ulang status transfer yang masih diproses (UC-01 A5)")
    public TransactionDto checkStatus(@AuthenticationPrincipal AuthUser me, @PathVariable String txId) {
        return transfers.checkStatus(me.userId(), txId);
    }

    @GetMapping("/favorites")
    @Operation(summary = "Penerima favorit (FR-13)")
    public List<FavoriteDto> favorites(@AuthenticationPrincipal AuthUser me) {
        return transfers.favorites(me.userId());
    }

    @PostMapping("/favorites")
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriteDto addFavorite(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody FavoriteRequest r) {
        return transfers.addFavorite(me.userId(), r.bankCode(), r.accountNumber());
    }

    @DeleteMapping("/favorites/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        transfers.removeFavorite(me.userId(), id);
    }
}
