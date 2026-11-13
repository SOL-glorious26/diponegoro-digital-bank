package com.diponegoro.ddb.transfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class TransferDtos {
    private TransferDtos() {}

    public record BankDto(String code, String name, boolean internal, long fee) {}

    public record InquiryRequest(@NotBlank(message = "Pilih bank tujuan") String bankCode,
                                 @NotBlank(message = "Nomor rekening wajib diisi") String accountNumber) {}

    public record RecipientDto(String bankCode, String bankName, String accountNumber, String name, boolean favorite) {}

    public record QuoteRequest(@NotBlank String bankCode, @NotBlank String accountNumber,
                               @NotNull(message = "Nominal wajib diisi") Long amount, String note) {}

    /** Layar konfirmasi (UC-01 langkah 4): penerima, nominal, biaya, total, saldo sesudah. */
    public record QuoteDto(RecipientDto recipient, long amount, long fee, long total, String note, long balance,
                           long balanceAfter) {}

    public record TransferRequest(@NotBlank String bankCode, @NotBlank String accountNumber,
                                  @NotNull(message = "Nominal wajib diisi") Long amount, String note,
                                  @NotBlank(message = "PIN wajib diisi") String pin) {}

    public record FavoriteRequest(@NotBlank String bankCode, @NotBlank String accountNumber) {}

    public record FavoriteDto(String id, String bankCode, String bankName, String accountNumber, String name,
                              Instant addedAt) {}
}
