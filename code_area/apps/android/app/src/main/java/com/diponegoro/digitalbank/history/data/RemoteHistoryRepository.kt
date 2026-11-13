package com.diponegoro.digitalbank.history.data

import com.diponegoro.digitalbank.core.ledger.Direction
import com.diponegoro.digitalbank.core.network.ApiClient
import com.diponegoro.digitalbank.core.network.IsoTime
import com.diponegoro.digitalbank.core.network.MonthSummaryDto
import com.diponegoro.digitalbank.core.network.TransactionDto
import com.diponegoro.digitalbank.core.network.toLedger
import com.diponegoro.digitalbank.core.util.AppResult

/** Riwayat, detail (dengan jurnal), dan ringkasan bulanan dari backend; semua angka dihitung server dari ledger. */
class RemoteHistoryRepository(private val client: ApiClient) : HistoryRepository {

    private fun TransactionDto.item(userId: String) =
        HistoryItem(toLedger(userId), runCatching { Direction.valueOf(direction) }.getOrDefault(Direction.OUT))

    private fun MonthSummaryDto.toModel() = MonthSummary(label, IsoTime.parseDate(month), income, expense)

    override suspend fun page(userId: String, filter: HistoryFilter, pageCount: Int): HistoryPage {
        val size = pageCount.coerceAtLeast(1) * HistoryRepository.PAGE_SIZE
        val r = client.call {
            history(
                range = filter.range.name, category = filter.category?.name, direction = filter.direction?.name,
                status = filter.status?.name, q = filter.query.trim().ifEmpty { null }, page = 0, size = size
            )
        }
        return when (r) {
            is AppResult.Success -> HistoryPage(r.data.items.map { it.item(userId) }, r.data.totalItems.toInt(), r.data.hasMore)
            is AppResult.Failure -> HistoryPage(emptyList(), 0, false, error = r.message) // DEF-11
        }
    }

    override suspend fun recent(userId: String, count: Int): List<HistoryItem> =
        when (val r = client.call { history(page = 0, size = count) }) {
            is AppResult.Success -> r.data.items.map { it.item(userId) }
            is AppResult.Failure -> emptyList()
        }

    override suspend fun detail(userId: String, txId: String): TransactionDetail? =
        when (val r = client.call { transactionDetail(txId) }) {
            is AppResult.Success -> TransactionDetail(r.data.transaction.item(userId), r.data.journal.map { it.toLedger(txId) })
            is AppResult.Failure -> null
        }

    override suspend fun monthSummary(userId: String, monthsBack: Int): MonthSummary =
        monthSeries(userId, 1, monthsBack).lastOrNull() ?: MonthSummary("", 0, 0, 0)

    override suspend fun monthSeries(userId: String, months: Int, endMonthsBack: Int): List<MonthSummary> =
        when (val r = client.call { monthlySummary(months, endMonthsBack) }) {
            is AppResult.Success -> r.data.map { it.toModel() }
            is AppResult.Failure -> emptyList()
        }
}
