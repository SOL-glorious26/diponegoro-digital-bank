package com.diponegoro.ddb.account;

import com.diponegoro.ddb.account.Pocket.LockType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AccountDtos {
    private AccountDtos() {}

    public enum BalancePeriod {
        DAILY("Harian", 14), WEEKLY("Mingguan", 8), MONTHLY("Bulanan", 6);
        public final String label;
        public final int points;
        BalancePeriod(String label, int points) { this.label = label; this.points = points; }
    }

    public record AccountOverview(String ownerName, String accountNumber, long balance, long pocketsTotal,
                                  boolean balanceHidden, boolean financialEnabled) {}

    public record BalanceVisibilityRequest(@NotNull Boolean hidden) {}

    public record BalancePoint(String label, Instant at, long balance) {}

    public record BalanceChart(BalancePeriod period, List<BalancePoint> points) {}

    public record PocketDto(String id, String name, String purpose, Long target, long balance, LockType lockType,
                            LocalDate lockUntil, boolean locked, String lockDescription, float progress,
                            Instant createdAt) {}

    /** lockType DATE butuh lockUntil (tanggal di masa depan); TARGET butuh target. */
    public record CreatePocketRequest(String name, String purpose, Long target, LockType lockType, LocalDate lockUntil) {}

    public record UpdatePocketRequest(String name, String purpose, Long target) {}

    public record LockPocketRequest(@NotNull LockType lockType, LocalDate lockUntil) {}

    public record MoveRequest(@NotNull @Positive(message = "Nominal harus lebih dari 0") Long amount) {}

    /** Penarikan dari kantong terkunci wajib earlyWithdrawal = true dan PIN (BR-06). */
    public record WithdrawRequest(@NotNull @Positive(message = "Nominal harus lebih dari 0") Long amount,
                                  Boolean earlyWithdrawal, String pin) {}
}
