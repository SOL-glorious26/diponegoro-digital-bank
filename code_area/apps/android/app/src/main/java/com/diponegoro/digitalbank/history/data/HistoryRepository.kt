package com.diponegoro.digitalbank.history.data

import com.diponegoro.digitalbank.core.ledger.Direction
import com.diponegoro.digitalbank.core.ledger.JournalLine
import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.ledger.TxCategory
import com.diponegoro.digitalbank.core.ledger.TxStatus

enum class DateRange(val label: String) {
    ALL("Semua"),
    LAST_7_DAYS("7 hari"),
    LAST_30_DAYS("30 hari"),
    THIS_MONTH("Bulan ini"),
    LAST_MONTH("Bulan lalu")
}

data class HistoryFilter(
    val range: DateRange = DateRange.ALL,
    val category: TxCategory? = null,
    val direction: Direction? = null,
    val status: TxStatus? = null,
    val query: String = ""
)

data class HistoryItem(val tx: LedgerTransaction, val direction: Direction)

/** [error] terisi bila data gagal dimuat (mis. server tidak terjangkau), agar layar tidak menampilkan "0 transaksi". */
data class HistoryPage(val items: List<HistoryItem>, val totalMatches: Int, val hasMore: Boolean, val error: String? = null)

data class TransactionDetail(val item: HistoryItem, val journal: List<JournalLine>)

data class MonthSummary(val label: String, val start: Long, val income: Long, val expense: Long) {
    val net: Long get() = income - expense
}

/**
 * Modul Transaction History & Dashboard (FR-23 – FR-25). Implementasi:
 *  - [LocalHistoryRepository]  : dihitung dari ledger in-memory (varian demo)
 *  - [RemoteHistoryRepository] : backend /api/v1/history (varian online)
 */
interface HistoryRepository {
    suspend fun page(userId: String, filter: HistoryFilter, pageCount: Int): HistoryPage
    suspend fun recent(userId: String, count: Int): List<HistoryItem>
    suspend fun detail(userId: String, txId: String): TransactionDetail?
    suspend fun monthSummary(userId: String, monthsBack: Int): MonthSummary
    suspend fun monthSeries(userId: String, months: Int, endMonthsBack: Int = 0): List<MonthSummary>

    companion object {
        const val PAGE_SIZE = 20
    }
}
