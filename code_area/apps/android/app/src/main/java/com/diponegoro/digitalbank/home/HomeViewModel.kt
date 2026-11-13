package com.diponegoro.digitalbank.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diponegoro.digitalbank.account.data.AccountRepository
import com.diponegoro.digitalbank.account.domain.AccountOverview
import com.diponegoro.digitalbank.core.di.CoreModule
import com.diponegoro.digitalbank.core.di.FeatureModule
import com.diponegoro.digitalbank.core.notification.NotificationSource
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.history.data.HistoryItem
import com.diponegoro.digitalbank.history.data.HistoryRepository
import com.diponegoro.digitalbank.history.data.MonthSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val overview: AccountOverview? = null,
    val recent: List<HistoryItem> = emptyList(),
    val month: MonthSummary? = null,
    val unread: Int = 0,
    val error: String? = null
)

class HomeViewModel(
    private val userId: String,
    private val displayName: String,
    private val accounts: AccountRepository,
    private val history: HistoryRepository,
    private val notifications: NotificationSource
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (!CoreModule.online) CoreModule.ensureCustomer(userId, displayName)
            combine(CoreModule.revision, notifications.revision) { _, _ -> }.collect { refresh() }
        }
    }

    private suspend fun refresh() {
        when (val r = accounts.overview(userId)) {
            is AppResult.Failure -> _state.update { it.copy(loading = false, error = r.message) }
            is AppResult.Success -> {
                val recent = history.recent(userId, 5)
                val month = history.monthSummary(userId, 0)
                val unread = notifications.unreadCount(userId)
                _state.update {
                    it.copy(loading = false, overview = r.data, recent = recent, month = month, unread = unread, error = null)
                }
            }
        }
    }

    /** FR-06: sembunyikan/tampilkan saldo, pilihan tersimpan. */
    fun toggleBalance() {
        val hidden = !(_state.value.overview?.balanceHidden ?: false)
        _state.update { s -> s.copy(overview = s.overview?.copy(balanceHidden = hidden)) }
        viewModelScope.launch { accounts.setBalanceHidden(userId, hidden) }
    }

    companion object {
        fun factory(userId: String, name: String) = viewModelFactory {
            initializer {
                HomeViewModel(userId, name, FeatureModule.accounts, FeatureModule.history, CoreModule.notificationSource)
            }
        }
    }
}
