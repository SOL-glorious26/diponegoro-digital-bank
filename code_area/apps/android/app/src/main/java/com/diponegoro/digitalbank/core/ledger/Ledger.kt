package com.diponegoro.digitalbank.core.ledger

import com.diponegoro.digitalbank.core.util.AppResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Financial Transaction Core (BR-03, BR-04, NFR-07) — implementasi in-memory.
 *
 * - Double-entry: setiap transaksi adalah kumpulan baris jurnal yang jumlahnya 0.
 * - Atomik: validasi + posting dilakukan di bawah satu Mutex; gagal = tidak ada baris yang ditulis.
 * - Idempotensi: kunci yang sama mengembalikan transaksi yang sudah ada, tidak mendebit dua kali.
 * - Jejak audit: jurnal append-only; pengembalian dana ditulis sebagai baris pembalik, bukan menghapus.
 * - Saldo selalu dihitung dari jurnal, sehingga riwayat & grafik konsisten dengan buku besar.
 */
class Ledger(private val clock: () -> Long = System::currentTimeMillis) {

    private val mutex = Mutex()
    // Koleksi thread-safe: pembacaan (saldo, riwayat) boleh terjadi bersamaan dengan posting.
    private val accounts = ConcurrentHashMap<String, LedgerAccount>()
    private val journal = CopyOnWriteArrayList<JournalLine>()
    private val transactions = ConcurrentHashMap<String, LedgerTransaction>()
    private val idempotency = ConcurrentHashMap<String, String>()
    private var nextAccountSeq = 100_001

    private val _revision = MutableStateFlow(0L)
    /** Bertambah setiap ada perubahan; layar cukup mengamati ini untuk memuat ulang. */
    val revision: StateFlow<Long> = _revision.asStateFlow()

    private val _events = MutableSharedFlow<LedgerEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<LedgerEvent> = _events.asSharedFlow()

    init {
        listOf(
            ExternalAccounts.INTERBANK to "Jaringan antarbank (simulator)",
            ExternalAccounts.MERCHANT to "Merchant QRIS (simulator)",
            ExternalAccounts.BILLER to "Biller top-up & tagihan (simulator)",
            ExternalAccounts.FEE_INCOME to "Pendapatan biaya",
            ExternalAccounts.FUNDING to "Dana masuk eksternal"
        ).forEach { (id, name) ->
            accounts[id] = LedgerAccount(id, null, AccountKind.EXTERNAL, id, name, 0L)
        }
    }

    // ---------------------------------------------------------------- accounts

    fun account(id: String): LedgerAccount? = accounts[id]

    fun findByNumber(number: String): LedgerAccount? =
        accounts.values.firstOrNull { it.kind == AccountKind.MAIN && it.number == number && !it.closed }

    fun mainAccount(userId: String): LedgerAccount? =
        accounts.values.firstOrNull { it.ownerUserId == userId && it.kind == AccountKind.MAIN }

    fun pockets(userId: String): List<LedgerAccount> =
        accounts.values
            .filter { it.ownerUserId == userId && it.kind == AccountKind.POCKET && !it.closed }
            .sortedBy { it.createdAt }

    /** Membuka rekening utama bila belum ada, dengan setoran awal simulasi. */
    suspend fun ensureMainAccount(userId: String, ownerName: String, openingBalance: Long): LedgerAccount =
        mutex.withLock { ensureMainAccountLocked(userId, ownerName, openingBalance, clock()) }

    internal fun ensureMainAccountLocked(
        userId: String, ownerName: String, openingBalance: Long, at: Long, number: String? = null
    ): LedgerAccount {
        mainAccount(userId)?.let { return it }
        val account = LedgerAccount(
            id = "acc-${UUID.randomUUID()}",
            ownerUserId = userId,
            kind = AccountKind.MAIN,
            number = number ?: "1400${nextAccountSeq++}",
            name = ownerName,
            createdAt = at
        )
        accounts[account.id] = account
        if (openingBalance > 0) {
            postLocked(
                PostingRequest(
                    idempotencyKey = "opening:$userId",
                    category = TxCategory.DEPOSIT,
                    title = "Setoran awal (simulasi)",
                    amount = openingBalance,
                    lines = listOf(ExternalAccounts.FUNDING to -openingBalance, account.id to openingBalance),
                    debitUserId = null, creditUserId = userId,
                    fromName = "Setoran awal", fromDetail = "Saldo simulasi",
                    toName = ownerName, toDetail = account.number,
                    at = at
                )
            )
        }
        bump()
        return account
    }

    suspend fun openPocket(userId: String, name: String): LedgerAccount = mutex.withLock {
        val account = LedgerAccount(
            id = "pocket-${UUID.randomUUID()}",
            ownerUserId = userId,
            kind = AccountKind.POCKET,
            number = "P${nextAccountSeq++}",
            name = name,
            createdAt = clock()
        )
        accounts[account.id] = account
        bump()
        account
    }

    suspend fun renamePocket(accountId: String, name: String): Unit = mutex.withLock {
        accounts[accountId]?.let { accounts[accountId] = it.copy(name = name) }
        bump()
    }

    suspend fun closeAccount(accountId: String): AppResult<Unit> = mutex.withLock {
        val account = accounts[accountId] ?: return@withLock AppResult.Failure("Rekening tidak ditemukan")
        if (balanceLocked(accountId) != 0L) return@withLock AppResult.Failure("Saldo harus Rp0 sebelum ditutup")
        accounts[accountId] = account.copy(closed = true)
        bump()
        AppResult.Success(Unit)
    }

    // ---------------------------------------------------------------- balances

    fun balance(accountId: String, at: Long = Long.MAX_VALUE): Long =
        journal.sumOf { if (it.accountId == accountId && it.postedAt <= at) it.amount else 0L }

    private fun balanceLocked(accountId: String) = balance(accountId)

    /** Saldo rekening pada akhir setiap titik waktu (untuk grafik FR-07). */
    fun balanceSeries(accountId: String, points: List<Long>): List<Long> = points.map { balance(accountId, it) }

    // ---------------------------------------------------------------- transactions

    fun transaction(id: String): LedgerTransaction? = transactions[id]

    fun findByIdempotencyKey(key: String): LedgerTransaction? = idempotency[key]?.let { transactions[it] }

    /** Semua transaksi yang melibatkan pengguna, terbaru lebih dulu. */
    fun transactionsFor(userId: String): List<LedgerTransaction> =
        transactions.values
            .filter { it.debitUserId == userId || it.creditUserId == userId }
            .sortedByDescending { it.createdAt }

    fun journalFor(txId: String): List<JournalLine> = journal.filter { it.txId == txId }

    suspend fun post(request: PostingRequest): AppResult<LedgerTransaction> =
        mutex.withLock { postLocked(request) }.also { r ->
            if (r is AppResult.Success) _events.tryEmit(LedgerEvent.Posted(r.data))
        }

    /** Menyelesaikan transaksi berstatus PROCESSING (mis. hasil cek ulang status antarbank). */
    suspend fun complete(txId: String): AppResult<LedgerTransaction> = mutex.withLock {
        val tx = transactions[txId] ?: return@withLock AppResult.Failure("Transaksi tidak ditemukan")
        if (tx.status != TxStatus.PROCESSING) return@withLock AppResult.Success(tx)
        val updated = tx.copy(status = TxStatus.SUCCESS, updatedAt = clock(), statusReason = null)
        transactions[txId] = updated
        bump()
        AppResult.Success(updated)
    }.also { r -> if (r is AppResult.Success) _events.tryEmit(LedgerEvent.StatusChanged(r.data)) }

    /** Menandai gagal dan mengembalikan dana dengan baris jurnal pembalik (BR-05). */
    suspend fun failWithRefund(txId: String, reason: String): AppResult<LedgerTransaction> = mutex.withLock {
        val tx = transactions[txId] ?: return@withLock AppResult.Failure("Transaksi tidak ditemukan")
        if (tx.status == TxStatus.FAILED) return@withLock AppResult.Success(tx)
        val now = clock()
        journal.filter { it.txId == txId }.toList().forEach { line ->
            journal.add(JournalLine(txId, line.accountId, -line.amount, now, "refund"))
        }
        val updated = tx.copy(status = TxStatus.FAILED, updatedAt = now, statusReason = reason)
        transactions[txId] = updated
        bump()
        AppResult.Success(updated)
    }.also { r -> if (r is AppResult.Success) _events.tryEmit(LedgerEvent.StatusChanged(r.data)) }

    internal fun postLocked(request: PostingRequest): AppResult<LedgerTransaction> {
        // BR-04: permintaan berulang dengan kunci sama → kembalikan hasil sebelumnya.
        idempotency[request.idempotencyKey]?.let { existing ->
            return AppResult.Success(transactions.getValue(existing))
        }
        if (request.lines.isEmpty()) return AppResult.Failure("Transaksi tidak memiliki baris jurnal")
        if (request.lines.sumOf { it.second } != 0L) return AppResult.Failure("Jurnal tidak seimbang")
        if (request.amount <= 0) return AppResult.Failure("Nominal harus lebih dari 0")

        // Validasi seluruh rekening & saldo sebelum menulis apa pun (atomik).
        val deltas = request.lines.groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }
        for ((accountId, delta) in deltas) {
            val account = accounts[accountId] ?: return AppResult.Failure("Rekening tidak ditemukan")
            if (account.closed) return AppResult.Failure("Rekening ${account.number} sudah ditutup")
            if (account.kind != AccountKind.EXTERNAL && balance(accountId) + delta < 0) {
                return AppResult.Failure(
                    if (account.kind == AccountKind.POCKET) "Saldo kantong tidak mencukupi" else "Saldo tidak mencukupi"
                )
            }
        }

        val now = request.at ?: clock()
        val id = "trx-${UUID.randomUUID()}"
        val tx = LedgerTransaction(
            id = id,
            reference = newReference(now),
            idempotencyKey = request.idempotencyKey,
            category = request.category,
            status = request.status,
            createdAt = now,
            updatedAt = now,
            amount = request.amount,
            fee = request.fee,
            title = request.title,
            debitUserId = request.debitUserId,
            creditUserId = request.creditUserId,
            fromName = request.fromName,
            fromDetail = request.fromDetail,
            toName = request.toName,
            toDetail = request.toDetail,
            note = request.note,
            extra = request.extra
        )
        request.lines.forEach { (accountId, amount) ->
            if (amount != 0L) journal.add(JournalLine(id, accountId, amount, now, request.title))
        }
        transactions[id] = tx
        idempotency[request.idempotencyKey] = id
        bump()
        return AppResult.Success(tx)
    }

    private fun newReference(at: Long): String {
        val date = SimpleDateFormat("yyMMdd", Locale.US).format(Date(at))
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        return "DDB$date$suffix"
    }

    private fun bump() {
        _revision.value = _revision.value + 1
    }
}
