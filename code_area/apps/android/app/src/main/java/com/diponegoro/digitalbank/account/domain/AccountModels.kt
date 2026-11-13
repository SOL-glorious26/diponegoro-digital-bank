package com.diponegoro.digitalbank.account.domain

/** Aturan kunci kantong (FR-09). */
sealed interface LockRule {
    /** Terkunci sampai tanggal tertentu (epoch millis, awal hari). */
    data class UntilDate(val until: Long) : LockRule
    /** Terkunci sampai saldo kantong mencapai target. */
    data object UntilTarget : LockRule
}

data class Pocket(
    val id: String,
    val name: String,
    val purpose: String,
    val target: Long?,
    val balance: Long,
    val lock: LockRule?,
    val createdAt: Long
) {
    val locked: Boolean get() = lock != null
    val progress: Float get() = if (target == null || target <= 0) 0f else (balance.toFloat() / target).coerceIn(0f, 1f)
}

data class AccountOverview(
    val ownerName: String,
    val accountNumber: String,
    val balance: Long,
    val pocketsTotal: Long,
    val balanceHidden: Boolean,
    val financialEnabled: Boolean
)

enum class BalancePeriod(val label: String, val points: Int) {
    DAILY("Harian", 14),
    WEEKLY("Mingguan", 8),
    MONTHLY("Bulanan", 6)
}

data class BalancePoint(val label: String, val at: Long, val balance: Long)

object PocketRules {
    const val MAX_POCKETS = 10
    const val MAX_NAME = 30
    const val MAX_PURPOSE = 60
}
