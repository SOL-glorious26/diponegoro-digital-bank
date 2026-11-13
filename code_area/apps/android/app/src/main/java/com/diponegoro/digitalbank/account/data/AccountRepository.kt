package com.diponegoro.digitalbank.account.data

import com.diponegoro.digitalbank.account.domain.AccountOverview
import com.diponegoro.digitalbank.account.domain.BalancePeriod
import com.diponegoro.digitalbank.account.domain.BalancePoint
import com.diponegoro.digitalbank.account.domain.LockRule
import com.diponegoro.digitalbank.account.domain.Pocket
import com.diponegoro.digitalbank.core.util.AppResult

/**
 * Modul Account (FR-06 – FR-09). Implementasi:
 *  - [LocalAccountRepository]  : ledger in-memory (varian demo)
 *  - [RemoteAccountRepository] : backend /api/v1/accounts (varian online)
 */
interface AccountRepository {
    suspend fun overview(userId: String): AppResult<AccountOverview>
    suspend fun setBalanceHidden(userId: String, hidden: Boolean)
    suspend fun balanceSeries(userId: String, period: BalancePeriod): List<BalancePoint>
    suspend fun pockets(userId: String): List<Pocket>
    suspend fun pocket(userId: String, pocketId: String): Pocket?
    suspend fun createPocket(userId: String, name: String, purpose: String, target: Long?, lock: LockRule?): AppResult<Pocket>
    suspend fun updatePocket(userId: String, pocketId: String, name: String, purpose: String, target: Long?): AppResult<Pocket>
    suspend fun setLock(userId: String, pocketId: String, lock: LockRule): AppResult<Pocket>
    suspend fun deletePocket(userId: String, pocketId: String, idempotencyKey: String): AppResult<Unit>
    suspend fun deposit(userId: String, pocketId: String, rawAmount: Long, idempotencyKey: String): AppResult<Pocket>
    suspend fun withdraw(
        userId: String, pocketId: String, rawAmount: Long, idempotencyKey: String,
        earlyWithdrawalConfirmed: Boolean = false, pin: String? = null
    ): AppResult<Pocket>
    fun lockDescription(pocket: Pocket): String
}
