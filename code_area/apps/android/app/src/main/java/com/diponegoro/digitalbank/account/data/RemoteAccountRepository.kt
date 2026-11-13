package com.diponegoro.digitalbank.account.data

import com.diponegoro.digitalbank.account.domain.AccountOverview
import com.diponegoro.digitalbank.account.domain.BalancePeriod
import com.diponegoro.digitalbank.account.domain.BalancePoint
import com.diponegoro.digitalbank.account.domain.LockRule
import com.diponegoro.digitalbank.account.domain.Pocket
import com.diponegoro.digitalbank.core.network.ApiClient
import com.diponegoro.digitalbank.core.network.BalanceVisibilityRequest
import com.diponegoro.digitalbank.core.network.CreatePocketRequest
import com.diponegoro.digitalbank.core.network.IsoTime
import com.diponegoro.digitalbank.core.network.LockPocketRequest
import com.diponegoro.digitalbank.core.network.MoveRequest
import com.diponegoro.digitalbank.core.network.PocketDto
import com.diponegoro.digitalbank.core.network.UpdatePocketRequest
import com.diponegoro.digitalbank.core.network.WithdrawRequest
import com.diponegoro.digitalbank.core.network.idemKey
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.core.util.formatDate
import com.diponegoro.digitalbank.core.util.formatRupiah
import com.diponegoro.digitalbank.core.util.map

/** Account lewat backend: saldo dan kantong adalah akun ledger server; aturan BR-06 dijalankan server. */
class RemoteAccountRepository(
    private val client: ApiClient,
    private val onDataChanged: () -> Unit = {}
) : AccountRepository {

    private fun PocketDto.toModel() = Pocket(
        id = id, name = name, purpose = purpose.orEmpty(), target = target, balance = balance,
        lock = if (!locked) null else when (lockType) {
            "DATE" -> LockRule.UntilDate(IsoTime.parseDate(lockUntil))
            "TARGET" -> LockRule.UntilTarget
            else -> null
        },
        createdAt = IsoTime.parse(createdAt)
    )

    private fun lockFields(lock: LockRule?): Pair<String, String?> = when (lock) {
        null -> "NONE" to null
        is LockRule.UntilDate -> "DATE" to IsoTime.formatDate(lock.until)
        LockRule.UntilTarget -> "TARGET" to null
    }

    private fun <T> AppResult<T>.changed(): AppResult<T> = also { if (it is AppResult.Success) onDataChanged() }

    override suspend fun overview(userId: String): AppResult<AccountOverview> = client.call { account() }.map {
        AccountOverview(it.ownerName, it.accountNumber, it.balance, it.pocketsTotal, it.balanceHidden, it.financialEnabled)
    }

    override suspend fun setBalanceHidden(userId: String, hidden: Boolean) {
        client.call { setBalanceVisibility(BalanceVisibilityRequest(hidden)) }.changed()
    }

    override suspend fun balanceSeries(userId: String, period: BalancePeriod): List<BalancePoint> =
        when (val r = client.call { balanceChart(period.name) }) {
            is AppResult.Success -> r.data.points.map { BalancePoint(it.label, IsoTime.parse(it.at), it.balance) }
            is AppResult.Failure -> emptyList()
        }

    override suspend fun pockets(userId: String): List<Pocket> =
        when (val r = client.call { pockets() }) {
            is AppResult.Success -> r.data.map { it.toModel() }
            is AppResult.Failure -> emptyList()
        }

    override suspend fun pocket(userId: String, pocketId: String): Pocket? = pockets(userId).firstOrNull { it.id == pocketId }

    override suspend fun createPocket(userId: String, name: String, purpose: String, target: Long?, lock: LockRule?): AppResult<Pocket> {
        val (type, until) = lockFields(lock)
        return client.call { createPocket(CreatePocketRequest(name.trim(), purpose.trim(), target, type, until)) }
            .map { it.toModel() }.changed()
    }

    override suspend fun updatePocket(userId: String, pocketId: String, name: String, purpose: String, target: Long?): AppResult<Pocket> =
        client.call { updatePocket(pocketId, UpdatePocketRequest(name.trim(), purpose.trim(), target)) }.map { it.toModel() }.changed()

    override suspend fun setLock(userId: String, pocketId: String, lock: LockRule): AppResult<Pocket> {
        val (type, until) = lockFields(lock)
        return client.call { lockPocket(pocketId, LockPocketRequest(type, until)) }.map { it.toModel() }.changed()
    }

    override suspend fun deletePocket(userId: String, pocketId: String, idempotencyKey: String): AppResult<Unit> =
        client.call {
            val res = deletePocket(pocketId, idemKey(idempotencyKey))
            if (!res.isSuccessful) throw retrofit2.HttpException(res)
        }.changed()

    override suspend fun deposit(userId: String, pocketId: String, rawAmount: Long, idempotencyKey: String): AppResult<Pocket> =
        client.call { deposit(pocketId, idemKey(idempotencyKey), MoveRequest(rawAmount)) }.map { it.toModel() }.changed()

    override suspend fun withdraw(
        userId: String, pocketId: String, rawAmount: Long, idempotencyKey: String,
        earlyWithdrawalConfirmed: Boolean, pin: String?
    ): AppResult<Pocket> =
        client.call { withdraw(pocketId, idemKey(idempotencyKey), WithdrawRequest(rawAmount, earlyWithdrawalConfirmed, pin)) }
            .map { it.toModel() }.changed()

    override fun lockDescription(pocket: Pocket): String = when (val lock = pocket.lock) {
        null -> "Tidak terkunci"
        is LockRule.UntilDate -> "Terkunci sampai ${formatDate(lock.until)}"
        LockRule.UntilTarget -> "Terkunci sampai target ${formatRupiah(pocket.target ?: 0)}"
    }
}
