package com.diponegoro.digitalbank.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diponegoro.digitalbank.core.di.CoreModule
import com.diponegoro.digitalbank.core.di.FeatureModule
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.history.data.HistoryFilter
import com.diponegoro.digitalbank.history.data.HistoryPage
import com.diponegoro.digitalbank.history.data.HistoryRepository
import com.diponegoro.digitalbank.history.data.MonthSummary
import com.diponegoro.digitalbank.history.data.TransactionDetail
import com.diponegoro.digitalbank.transfer.data.TransferRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val filter: HistoryFilter = HistoryFilter(),
    val pages: Int = 1,
    val page: HistoryPage = HistoryPage(emptyList(), 0, false)
)

/** FR-23: riwayat + filter + pencarian + paginasi. */
class HistoryViewModel(private val userId: String, private val repo: HistoryRepository) : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { CoreModule.revision.collect { reload() } }
    }

    private fun reload() {
        viewModelScope.launch {
            val s = _state.value
            val page = repo.page(userId, s.filter, s.pages)
            _state.update { if (it.filter == s.filter && it.pages == s.pages) it.copy(page = page) else it }
        }
    }

    fun updateFilter(transform: (HistoryFilter) -> HistoryFilter) {
        _state.update { it.copy(filter = transform(it.filter), pages = 1) }
        reload()
    }

    fun loadMore() {
        _state.update { it.copy(pages = it.pages + 1) }
        reload()
    }

    fun resetFilter() {
        _state.update { HistoryUiState() }
        reload()
    }

    companion object {
        fun factory(userId: String) = viewModelFactory { initializer { HistoryViewModel(userId, FeatureModule.history) } }
    }
}

data class DetailUiState(
    val detail: TransactionDetail? = null,
    val checking: Boolean = false,
    val message: String? = null,
    val notFound: Boolean = false
)

/** FR-24: detail transaksi + status, selalu dibaca dari ledger. */
class TransactionDetailViewModel(
    private val userId: String,
    private val txId: String,
    private val repo: HistoryRepository,
    private val transfer: TransferRepository
) : ViewModel() {
    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            CoreModule.revision.collect {
                val detail = repo.detail(userId, txId)
                _state.update { it.copy(detail = detail, notFound = detail == null) }
            }
        }
    }

    /** UC-01 A5: cek ulang status transaksi yang masih diproses. */
    fun checkStatus() {
        viewModelScope.launch {
            _state.update { it.copy(checking = true, message = null) }
            val r = transfer.checkStatus(userId, txId)
            _state.update {
                it.copy(
                    checking = false,
                    message = when (r) {
                        is AppResult.Success -> "Status terbaru: ${r.data.status.label}"
                        is AppResult.Failure -> r.message
                    }
                )
            }
        }
    }

    companion object {
        fun factory(userId: String, txId: String) = viewModelFactory {
            initializer { TransactionDetailViewModel(userId, txId, FeatureModule.history, FeatureModule.transfer) }
        }
    }
}

data class AnalyticsUiState(
    val months: List<MonthSummary> = emptyList(),
    val selected: Int = 0,
    /** Geser jendela 6 bulan ke belakang. 0 = berakhir di bulan berjalan. */
    val endMonthsBack: Int = 0
)

/** FR-25: grafik pemasukan & pengeluaran per bulan, dapat berganti bulan. */
class AnalyticsViewModel(private val userId: String, private val repo: HistoryRepository) : ViewModel() {
    private val _state = MutableStateFlow(AnalyticsUiState())
    val state: StateFlow<AnalyticsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { CoreModule.revision.collect { load(keepSelection = true) } }
    }

    private suspend fun load(keepSelection: Boolean) {
        val s = _state.value
        val months = repo.monthSeries(userId, WINDOW, s.endMonthsBack)
        _state.update { it.copy(months = months, selected = if (keepSelection && it.months.isNotEmpty()) it.selected else months.lastIndex) }
    }

    fun select(index: Int) = _state.update { it.copy(selected = index) }

    fun previousMonth() {
        val s = _state.value
        if (s.selected > 0) select(s.selected - 1)
        else viewModelScope.launch {
            _state.update { it.copy(endMonthsBack = it.endMonthsBack + 1) }
            load(keepSelection = false)
            _state.update { it.copy(selected = 0) }
        }
    }

    fun nextMonth() {
        val s = _state.value
        if (s.selected < s.months.lastIndex) select(s.selected + 1)
        else if (s.endMonthsBack > 0) viewModelScope.launch {
            _state.update { it.copy(endMonthsBack = it.endMonthsBack - 1) }
            load(keepSelection = false)
        }
    }

    companion object {
        const val WINDOW = 6
        fun factory(userId: String) = viewModelFactory { initializer { AnalyticsViewModel(userId, FeatureModule.history) } }
    }
}
