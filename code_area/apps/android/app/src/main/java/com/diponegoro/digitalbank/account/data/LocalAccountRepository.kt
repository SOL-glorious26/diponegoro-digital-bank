package com.diponegoro.digitalbank.account.data

import com.diponegoro.digitalbank.account.domain.AccountOverview
import com.diponegoro.digitalbank.account.domain.BalancePeriod
import com.diponegoro.digitalbank.account.domain.BalancePoint
import com.diponegoro.digitalbank.account.domain.LockRule
import com.diponegoro.digitalbank.account.domain.Pocket
import com.diponegoro.digitalbank.account.domain.PocketRules
import com.diponegoro.digitalbank.core.ledger.Ledger
import com.diponegoro.digitalbank.core.ledger.PostingRequest
import com.diponegoro.digitalbank.core.ledger.TxCategory
import com.diponegoro.digitalbank.core.security.TransactionGuard
import com.diponegoro.digitalbank.core.storage.KeyValueStore
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.core.util.ID_LOCALE
import com.diponegoro.digitalbank.core.util.formatDate
import com.diponegoro.digitalbank.core.util.formatRupiah
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * Account module (FR-06 – FR-09). Saldo & kantong disimpan di ledger bersama;
 * repository ini menyimpan metadata kantong (tujuan, target, aturan kunci).
 */
class LocalAccountRepository(
    private val ledger: Ledger,
    private val guard: TransactionGuard,
    private val prefs: KeyValueStore,
    private val clock: () -> Long = System::currentTimeMillis
) : AccountRepository {
    private data class PocketMeta(val purpose: String, val target: Long?, val lock: LockRule?)

    private val meta = ConcurrentHashMap<String, PocketMeta>()

    private fun hiddenKey(userId: String) = "balance_hidden:$userId"

    // ---------------------------------------------------------------- FR-06

    override suspend fun overview(userId: String): AppResult<AccountOverview> {
        val main = ledger.mainAccount(userId) ?: return AppResult.Failure("Rekening belum tersedia")
        return AppResult.Success(
            AccountOverview(
                ownerName = main.name,
                accountNumber = main.number,
                balance = ledger.balance(main.id),
                pocketsTotal = ledger.pockets(userId).sumOf { ledger.balance(it.id) },
                balanceHidden = prefs.getBoolean(hiddenKey(userId), false),
                financialEnabled = guard.isFinancialEnabled(userId)
            )
        )
    }

    /** Pilihan sembunyikan/tampilkan saldo tersimpan per pengguna. */
    override suspend fun setBalanceHidden(userId: String, hidden: Boolean) { prefs.putBoolean(hiddenKey(userId), hidden) }

    // ---------------------------------------------------------------- FR-07

    override suspend fun balanceSeries(userId: String, period: BalancePeriod): List<BalancePoint> {
        val main = ledger.mainAccount(userId) ?: return emptyList()
        val labelFormat = SimpleDateFormat(if (period == BalancePeriod.MONTHLY) "MMM yy" else "dd/MM", ID_LOCALE)
        val ends = (period.points - 1 downTo 0).map { back ->
            Calendar.getInstance().apply {
                timeInMillis = clock()
                when (period) {
                    BalancePeriod.DAILY -> add(Calendar.DAY_OF_YEAR, -back)
                    BalancePeriod.WEEKLY -> add(Calendar.WEEK_OF_YEAR, -back)
                    BalancePeriod.MONTHLY -> {
                        add(Calendar.MONTH, -back)
                        if (back > 0) set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    }
                }
                if (back > 0) {
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
                }
            }.timeInMillis.coerceAtMost(clock())
        }
        val balances = ledger.balanceSeries(main.id, ends)
        return ends.mapIndexed { i, at -> BalancePoint(labelFormat.format(Date(at)), at, balances[i]) }
    }

    // ---------------------------------------------------------------- FR-08 / FR-09

    override suspend fun pockets(userId: String): List<Pocket> = ledger.pockets(userId).map { account ->
        val m = meta[account.id] ?: PocketMeta("", null, null)
        val balance = ledger.balance(account.id)
        // Kunci terbuka otomatis saat tanggal/target tercapai.
        val stillLocked = when (val lock = m.lock) {
            null -> null
            is LockRule.UntilDate -> lock.takeIf { clock() < it.until }
            LockRule.UntilTarget -> lock.takeIf { m.target == null || balance < m.target }
        }
        if (stillLocked == null && m.lock != null) meta[account.id] = m.copy(lock = null)
        Pocket(account.id, account.name, m.purpose, m.target, balance, stillLocked, account.createdAt)
    }

    override suspend fun pocket(userId: String, pocketId: String): Pocket? = pockets(userId).firstOrNull { it.id == pocketId }

    override suspend fun createPocket(
        userId: String, name: String, purpose: String, target: Long?, lock: LockRule?
    ): AppResult<Pocket> {
        val financial = guard.requireFinancial(userId)
        if (financial is AppResult.Failure) return financial
        validateFields(name, purpose, target)?.let { return AppResult.Failure(it) }
        validateLock(lock, target)?.let { return AppResult.Failure(it) }
        if (ledger.pockets(userId).size >= PocketRules.MAX_POCKETS)
            return AppResult.Failure("Maksimal ${PocketRules.MAX_POCKETS} kantong")
        val account = ledger.openPocket(userId, name.trim())
        meta[account.id] = PocketMeta(purpose.trim(), target, lock)
        return AppResult.Success(pocket(userId, account.id)!!)
    }

    override suspend fun updatePocket(
        userId: String, pocketId: String, name: String, purpose: String, target: Long?
    ): AppResult<Pocket> {
        val current = pocket(userId, pocketId) ?: return AppResult.Failure("Kantong tidak ditemukan")
        validateFields(name, purpose, target)?.let { return AppResult.Failure(it) }
        if (current.lock == LockRule.UntilTarget && target == null)
            return AppResult.Failure("Kantong terkunci sampai target; target tidak boleh dihapus")
        ledger.renamePocket(pocketId, name.trim())
        meta[pocketId] = PocketMeta(purpose.trim(), target, current.lock)
        return AppResult.Success(pocket(userId, pocketId)!!)
    }

    override suspend fun setLock(userId: String, pocketId: String, lock: LockRule): AppResult<Pocket> {
        val current = pocket(userId, pocketId) ?: return AppResult.Failure("Kantong tidak ditemukan")
        if (current.locked) return AppResult.Failure("Kantong sudah terkunci")
        validateLock(lock, current.target)?.let { return AppResult.Failure(it) }
        if (lock == LockRule.UntilTarget && current.balance >= (current.target ?: 0))
            return AppResult.Failure("Saldo kantong sudah mencapai target")
        meta[pocketId] = (meta[pocketId] ?: PocketMeta("", null, null)).copy(lock = lock)
        return AppResult.Success(pocket(userId, pocketId)!!)
    }

    override suspend fun deletePocket(userId: String, pocketId: String, idempotencyKey: String): AppResult<Unit> {
        val current = pocket(userId, pocketId) ?: return AppResult.Failure("Kantong tidak ditemukan")
        if (current.locked) return AppResult.Failure("Kantong terkunci tidak dapat dihapus")
        if (current.balance > 0) {
            val moved = move(userId, pocketId, current.balance, toPocket = false, idempotencyKey, current.name)
            if (moved is AppResult.Failure) return moved
        }
        val closed = ledger.closeAccount(pocketId)
        if (closed is AppResult.Success) meta.remove(pocketId)
        return closed
    }

    /** Isi kantong dari rekening utama. */
    override suspend fun deposit(userId: String, pocketId: String, rawAmount: Long, idempotencyKey: String): AppResult<Pocket> {
        val financial = guard.requireFinancial(userId)
        if (financial is AppResult.Failure) return financial
        val current = pocket(userId, pocketId) ?: return AppResult.Failure("Kantong tidak ditemukan")
        if (rawAmount <= 0) return AppResult.Failure("Nominal harus lebih dari 0")
        return when (val r = move(userId, pocketId, rawAmount, toPocket = true, idempotencyKey, current.name)) {
            is AppResult.Failure -> r
            is AppResult.Success -> AppResult.Success(pocket(userId, pocketId)!!)
        }
    }

    /**
     * Tarik dana kantong ke rekening utama. Kantong terkunci (BR-06) hanya bisa ditarik
     * lewat penarikan dini: konfirmasi tambahan + PIN valid.
     */
    override suspend fun withdraw(
        userId: String, pocketId: String, rawAmount: Long, idempotencyKey: String,
        earlyWithdrawalConfirmed: Boolean, pin: String?
    ): AppResult<Pocket> {
        val financial = guard.requireFinancial(userId)
        if (financial is AppResult.Failure) return financial
        val current = pocket(userId, pocketId) ?: return AppResult.Failure("Kantong tidak ditemukan")
        if (rawAmount <= 0) return AppResult.Failure("Nominal harus lebih dari 0")
        if (current.locked) {
            if (!earlyWithdrawalConfirmed)
                return AppResult.Failure("Kantong terkunci. Penarikan dini memerlukan konfirmasi dan PIN.")
            val pinCheck = guard.verifyPin(userId, pin.orEmpty())
            if (pinCheck is AppResult.Failure) return pinCheck
        }
        return when (val r = move(userId, pocketId, rawAmount, toPocket = false, idempotencyKey, current.name)) {
            is AppResult.Failure -> r
            is AppResult.Success -> AppResult.Success(pocket(userId, pocketId)!!)
        }
    }

    override fun lockDescription(pocket: Pocket): String = when (val lock = pocket.lock) {
        null -> "Tidak terkunci"
        is LockRule.UntilDate -> "Terkunci sampai ${formatDate(lock.until)}"
        LockRule.UntilTarget -> "Terkunci sampai target ${formatRupiah(pocket.target ?: 0)}"
    }

    private suspend fun move(
        userId: String, pocketId: String, amount: Long, toPocket: Boolean, key: String, pocketName: String
    ): AppResult<Unit> {
        val main = ledger.mainAccount(userId) ?: return AppResult.Failure("Rekening tidak ditemukan")
        val lines = if (toPocket) listOf(main.id to -amount, pocketId to amount)
        else listOf(pocketId to -amount, main.id to amount)
        val result = ledger.post(
            PostingRequest(
                idempotencyKey = key,
                category = TxCategory.POCKET,
                title = if (toPocket) "Isi kantong $pocketName" else "Tarik dari kantong $pocketName",
                amount = amount,
                lines = lines,
                debitUserId = userId,
                creditUserId = userId,
                fromName = if (toPocket) "Rekening utama" else "Kantong $pocketName",
                fromDetail = if (toPocket) main.number else pocketName,
                toName = if (toPocket) "Kantong $pocketName" else "Rekening utama",
                toDetail = if (toPocket) pocketName else main.number
            )
        )
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> AppResult.Success(Unit)
        }
    }

    private fun validateFields(name: String, purpose: String, target: Long?): String? = when {
        name.isBlank() -> "Nama kantong wajib diisi"
        name.trim().length > PocketRules.MAX_NAME -> "Nama kantong maksimal ${PocketRules.MAX_NAME} karakter"
        purpose.trim().length > PocketRules.MAX_PURPOSE -> "Tujuan maksimal ${PocketRules.MAX_PURPOSE} karakter"
        target != null && target < 10_000 -> "Target minimal Rp10.000"
        else -> null
    }

    private fun validateLock(lock: LockRule?, target: Long?): String? = when (lock) {
        null -> null
        is LockRule.UntilDate -> if (lock.until <= clock()) "Tanggal kunci harus di masa depan" else null
        LockRule.UntilTarget -> if (target == null) "Isi target terlebih dahulu untuk mengunci sampai target" else null
    }
}
