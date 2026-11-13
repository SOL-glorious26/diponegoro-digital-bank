import sys, re
root = sys.argv[1]
P = lambda p: root + '/' + p

# 1) pindahkan model ke berkas interface
lp = P('history/data/LocalHistoryRepository.kt')
s = open(lp, encoding='utf8').read()
start = s.index('enum class DateRange')
end = s.index('/** Modul Transaction History & Dashboard')
models = s[start:end]
s = s[:start] + s[end:]
s = s.replace('endMonthsBack: Int = 0): List<MonthSummary>', 'endMonthsBack: Int): List<MonthSummary>')
s = s.replace('    companion object {\n        const val PAGE_SIZE = 20\n    }\n', '    companion object {\n        const val PAGE_SIZE = HistoryRepository.PAGE_SIZE\n    }\n')
open(lp, 'w', encoding='utf8', newline='\n').write(s)

open(P('history/data/HistoryRepository.kt'), 'w', encoding='utf8', newline='\n').write('''package com.diponegoro.digitalbank.history.data

import com.diponegoro.digitalbank.core.ledger.Direction
import com.diponegoro.digitalbank.core.ledger.JournalLine
import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.ledger.TxCategory
import com.diponegoro.digitalbank.core.ledger.TxStatus

''' + models + '''/**
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
''')

open(P('history/data/RemoteHistoryRepository.kt'), 'w', encoding='utf8', newline='\n').write('''package com.diponegoro.digitalbank.history.data

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
            is AppResult.Failure -> HistoryPage(emptyList(), 0, false)
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
''')

vp = P('history/presentation/HistoryViewModels.kt')
s = open(vp, encoding='utf8').read()
pairs = [
    ('        viewModelScope.launch { CoreModule.ledger.revision.collect { reload() } }\n    }\n\n    private fun reload() {\n        val s = _state.value\n        _state.update { it.copy(page = repo.page(userId, s.filter, s.pages)) }\n    }',
     '        viewModelScope.launch { CoreModule.revision.collect { reload() } }\n    }\n\n    private fun reload() {\n        viewModelScope.launch {\n            val s = _state.value\n            val page = repo.page(userId, s.filter, s.pages)\n            _state.update { if (it.filter == s.filter && it.pages == s.pages) it.copy(page = page) else it }\n        }\n    }'),
    ('            CoreModule.ledger.revision.collect {\n                val detail = repo.detail(userId, txId)',
     '            CoreModule.revision.collect {\n                val detail = repo.detail(userId, txId)'),
    ('        viewModelScope.launch { CoreModule.ledger.revision.collect { load(keepSelection = true) } }\n    }\n\n    private fun load(keepSelection: Boolean) {',
     '        viewModelScope.launch { CoreModule.revision.collect { load(keepSelection = true) } }\n    }\n\n    private suspend fun load(keepSelection: Boolean) {'),
    ('''        if (s.selected > 0) select(s.selected - 1)
        else {
            _state.update { it.copy(endMonthsBack = it.endMonthsBack + 1) }
            load(keepSelection = false)
            _state.update { it.copy(selected = 0) }
        }''', '''        if (s.selected > 0) select(s.selected - 1)
        else viewModelScope.launch {
            _state.update { it.copy(endMonthsBack = it.endMonthsBack + 1) }
            load(keepSelection = false)
            _state.update { it.copy(selected = 0) }
        }'''),
    ('''        else if (s.endMonthsBack > 0) {
            _state.update { it.copy(endMonthsBack = it.endMonthsBack - 1) }
            load(keepSelection = false)
        }''', '''        else if (s.endMonthsBack > 0) viewModelScope.launch {
            _state.update { it.copy(endMonthsBack = it.endMonthsBack - 1) }
            load(keepSelection = false)
        }'''),
]
for a, b in pairs:
    assert a in s, a[:80]
    s = s.replace(a, b)
open(vp, 'w', encoding='utf8', newline='\n').write(s)

hp = P('home/HomeViewModel.kt')
s = open(hp, encoding='utf8').read()
pairs = [
    ('    private val notifications: NotificationCenter', '    private val notifications: NotificationSource'),
    ('import com.diponegoro.digitalbank.core.notification.NotificationCenter', 'import com.diponegoro.digitalbank.core.notification.NotificationSource'),
    ('            CoreModule.ensureCustomer(userId, displayName)\n            combine(CoreModule.ledger.revision, notifications.revision)',
     '            if (!CoreModule.online) CoreModule.ensureCustomer(userId, displayName)\n            combine(CoreModule.revision, notifications.revision)'),
    ('''            is AppResult.Success -> _state.update {
                it.copy(
                    loading = false,
                    overview = r.data,
                    recent = history.recent(userId, 5),
                    month = history.monthSummary(userId, 0),
                    unread = notifications.unreadCount(userId),
                    error = null
                )
            }''', '''            is AppResult.Success -> {
                val recent = history.recent(userId, 5)
                val month = history.monthSummary(userId, 0)
                val unread = notifications.unreadCount(userId)
                _state.update {
                    it.copy(loading = false, overview = r.data, recent = recent, month = month, unread = unread, error = null)
                }
            }'''),
    ('        accounts.setBalanceHidden(userId, hidden)\n        _state.update { s -> s.copy(overview = s.overview?.copy(balanceHidden = hidden)) }',
     '        _state.update { s -> s.copy(overview = s.overview?.copy(balanceHidden = hidden)) }\n        viewModelScope.launch { accounts.setBalanceHidden(userId, hidden) }'),
    ('FeatureModule.history, CoreModule.notifications)', 'FeatureModule.history, CoreModule.notificationSource)'),
]
for a, b in pairs:
    assert a in s, a[:80]
    s = s.replace(a, b)
open(hp, 'w', encoding='utf8', newline='\n').write(s)
print('ok')
