package com.diponegoro.digitalbank.topup.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diponegoro.digitalbank.core.di.FeatureModule
import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.topup.data.TopUpRepository
import com.diponegoro.digitalbank.topup.domain.BillInquiry
import com.diponegoro.digitalbank.topup.domain.Biller
import com.diponegoro.digitalbank.topup.domain.EWallet
import com.diponegoro.digitalbank.topup.domain.PlnCustomer
import com.diponegoro.digitalbank.topup.domain.Product
import com.diponegoro.digitalbank.topup.domain.TopUpKind
import com.diponegoro.digitalbank.topup.domain.TopUpOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class TopUpUiState(
    val tab: TopUpKind = TopUpKind.PULSA,
    val plnCustomer: PlnCustomer? = null,
    val bill: BillInquiry? = null,
    val order: TopUpOrder? = null,
    val showPin: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val result: LedgerTransaction? = null
)

/** FR-18 – FR-20. */
class TopUpViewModel(private val userId: String, private val repo: TopUpRepository) : ViewModel() {
    private val _state = MutableStateFlow(TopUpUiState())
    val state: StateFlow<TopUpUiState> = _state.asStateFlow()
    private var idempotencyKey = UUID.randomUUID().toString()

    fun selectTab(kind: TopUpKind) = _state.update { TopUpUiState(tab = kind) }
    fun clearError() = _state.update { it.copy(error = null) }

    private fun setOrder(result: AppResult<TopUpOrder>) = when (result) {
        is AppResult.Failure -> _state.update { it.copy(error = result.message) }
        is AppResult.Success -> {
            idempotencyKey = UUID.randomUUID().toString()
            _state.update { it.copy(order = result.data, error = null) }
        }
    }

    private fun quote(block: suspend () -> AppResult<TopUpOrder>) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            val r = block()
            _state.update { it.copy(busy = false) }
            setOrder(r)
        }
    }

    fun orderPulsa(phone: String, nominal: Long) = quote { repo.orderPulsa(phone, nominal) }
    fun orderData(phone: String, product: Product) = quote { repo.orderData(phone, product) }
    fun orderEwallet(wallet: EWallet, phone: String, amount: String) = quote { repo.orderEwallet(wallet, phone, amount) }

    fun inquirePln(meter: String) {
        viewModelScope.launch {
            when (val r = repo.inquirePln(meter)) {
                is AppResult.Failure -> _state.update { it.copy(error = r.message, plnCustomer = null) }
                is AppResult.Success -> _state.update { it.copy(plnCustomer = r.data, error = null) }
            }
        }
    }

    fun orderPln(nominal: Long) {
        val customer = _state.value.plnCustomer ?: return
        quote { repo.orderPln(customer, nominal) }
    }

    fun inquireBill(biller: Biller, number: String) {
        viewModelScope.launch {
            when (val r = repo.inquireBill(biller, number)) {
                is AppResult.Failure -> _state.update { it.copy(error = r.message, bill = null) }
                is AppResult.Success -> _state.update { it.copy(bill = r.data, error = null) }
            }
        }
    }

    fun orderBill() {
        val bill = _state.value.bill ?: return
        quote { repo.orderBill(bill) }
    }

    fun cancelOrder() = _state.update { it.copy(order = null, error = null) }
    fun requestPin() = _state.update { it.copy(showPin = true, error = null) }
    fun dismissPin() = _state.update { it.copy(showPin = false, error = null) }

    fun pay(pin: String) {
        val order = _state.value.order ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            when (val r = repo.execute(userId, order, pin, idempotencyKey)) {
                is AppResult.Failure -> _state.update { it.copy(busy = false, error = r.message) }
                is AppResult.Success -> _state.update { it.copy(busy = false, showPin = false, result = r.data) }
            }
        }
    }

    fun restart() = _state.update { TopUpUiState(tab = it.tab) }

    companion object {
        fun factory(userId: String) = viewModelFactory { initializer { TopUpViewModel(userId, FeatureModule.topUp) } }
    }
}
