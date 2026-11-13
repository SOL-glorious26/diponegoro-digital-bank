package com.diponegoro.digitalbank.history.data

import com.diponegoro.digitalbank.core.ledger.Direction
import com.diponegoro.digitalbank.core.ledger.JournalLine
import com.diponegoro.digitalbank.core.ledger.Ledger
import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.ledger.TxCategory
import com.diponegoro.digitalbank.core.ledger.TxStatus
import com.diponegoro.digitalbank.core.util.ID_LOCALE
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/** Modul Transaction History & Dashboard (FR-23 – FR-25). Semua angka dihitung dari ledger. */
class LocalHistoryRepository(
    private val ledger: Ledger,
    private val clock: () -> Long = System::currentTimeMillis
) : HistoryRepository {
    companion object {
        const val PAGE_SIZE = HistoryRepository.PAGE_SIZE
    }

    /** FR-23: terbaru lebih dulu, paginasi, filter tanggal/jenis/status + pencarian dapat dikombinasikan. */
    override suspend fun page(userId: String, filter: HistoryFilter, pageCount: Int): HistoryPage {
        val (from, to) = rangeBounds(filter.range)
        val query = filter.query.trim()
        val matches = ledger.transactionsFor(userId).asSequence()
            .map { HistoryItem(it, it.directionFor(userId)) }
            .filter { it.tx.createdAt in from..to }
            .filter { filter.category == null || it.tx.category == filter.category }
            .filter { filter.direction == null || it.direction == filter.direction }
            .filter { filter.status == null || it.tx.status == filter.status }
            .filter { query.isEmpty() || it.matches(query) }
            .toList()
        val limit = (pageCount.coerceAtLeast(1)) * PAGE_SIZE
        return HistoryPage(matches.take(limit), matches.size, matches.size > limit)
    }

    override suspend fun recent(userId: String, count: Int): List<HistoryItem> =
        ledger.transactionsFor(userId).take(count).map { HistoryItem(it, it.directionFor(userId)) }

    /** FR-24: detail selalu dibaca langsung dari ledger, termasuk baris jurnalnya. */
    override suspend fun detail(userId: String, txId: String): TransactionDetail? {
        val tx = ledger.transaction(txId) ?: return null
        if (tx.debitUserId != userId && tx.creditUserId != userId) return null
        return TransactionDetail(HistoryItem(tx, tx.directionFor(userId)), ledger.journalFor(txId))
    }

    /** FR-25: total pemasukan & pengeluaran per bulan. monthsBack = 0 berarti bulan berjalan. */
    override suspend fun monthSummary(userId: String, monthsBack: Int): MonthSummary {
        val start = monthStart(monthsBack)
        val end = monthStart(monthsBack - 1)
        var income = 0L
        var expense = 0L
        ledger.transactionsFor(userId).forEach { tx ->
            if (tx.status != TxStatus.SUCCESS || tx.createdAt < start || tx.createdAt >= end) return@forEach
            when (tx.directionFor(userId)) {
                Direction.IN -> income += tx.amount
                Direction.OUT -> expense += tx.total
                Direction.INTERNAL -> Unit
            }
        }
        return MonthSummary(SimpleDateFormat("MMM yyyy", ID_LOCALE).format(Date(start)), start, income, expense)
    }

    /** Ringkasan beberapa bulan, terlama → terbaru, untuk grafik batang. */
    override suspend fun monthSeries(userId: String, months: Int, endMonthsBack: Int): List<MonthSummary> =
        (endMonthsBack + months - 1 downTo endMonthsBack).map { monthSummary(userId, it) }

    private fun HistoryItem.matches(query: String): Boolean = listOf(
        tx.reference, tx.title, tx.fromName, tx.toName, tx.toDetail, tx.fromDetail, tx.note
    ).any { it.contains(query, ignoreCase = true) }

    private fun monthStart(monthsBack: Int): Long = Calendar.getInstance().apply {
        timeInMillis = clock()
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        add(Calendar.MONTH, -monthsBack)
    }.timeInMillis

    private fun rangeBounds(range: DateRange): Pair<Long, Long> {
        val now = clock()
        val day = 24 * 60 * 60 * 1000L
        return when (range) {
            DateRange.ALL -> Long.MIN_VALUE to Long.MAX_VALUE
            DateRange.LAST_7_DAYS -> now - 7 * day to Long.MAX_VALUE
            DateRange.LAST_30_DAYS -> now - 30 * day to Long.MAX_VALUE
            DateRange.THIS_MONTH -> monthStart(0) to Long.MAX_VALUE
            DateRange.LAST_MONTH -> monthStart(1) to monthStart(0) - 1
        }
    }
}
