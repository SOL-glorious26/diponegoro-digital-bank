package com.diponegoro.digitalbank.payment.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diponegoro.digitalbank.account.data.AccountRepository
import com.diponegoro.digitalbank.core.di.CoreModule
import com.diponegoro.digitalbank.core.di.FeatureModule
import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.core.util.formatRupiah
import com.diponegoro.digitalbank.core.util.parseRupiah
import com.diponegoro.digitalbank.payment.data.DemoQr
import com.diponegoro.digitalbank.payment.data.PaymentRepository
import com.diponegoro.digitalbank.payment.data.QrisRules
import com.diponegoro.digitalbank.payment.data.ScannedQris
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class QrisPayUiState(
    val demoQrs: List<DemoQr> = emptyList(),
    val scanned: ScannedQris? = null,
    val amount: String = "",
    val balance: Long = 0,
    val showPin: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val result: LedgerTransaction? = null
)

/** FR-15: pindai QRIS → tampil merchant & nominal → PIN → berhasil/gagal. */
class QrisPayViewModel(
    private val userId: String,
    private val repo: PaymentRepository,
    private val accounts: AccountRepository = FeatureModule.accounts
) : ViewModel() {
    private val _state = MutableStateFlow(QrisPayUiState())
    val state: StateFlow<QrisPayUiState> = _state.asStateFlow()
    private var idempotencyKey = UUID.randomUUID().toString()

    init {
        viewModelScope.launch {
            val demo = repo.demoQrs()
            _state.update { it.copy(demoQrs = demo) }
        }
        viewModelScope.launch {
            CoreModule.revision.collect {
                val balance = (accounts.overview(userId) as? AppResult.Success)?.data?.balance ?: 0
                _state.update { s -> s.copy(balance = balance) }
            }
        }
    }

    fun onScanned(raw: String?) {
        if (raw.isNullOrBlank()) return _state.update { it.copy(error = "Kode QR tidak terbaca") }
        viewModelScope.launch {
            when (val r = repo.scan(userId, raw)) {
                is AppResult.Failure -> _state.update { it.copy(error = r.message) }
                is AppResult.Success -> {
                    idempotencyKey = UUID.randomUUID().toString()
                    _state.update {
                        it.copy(scanned = r.data, amount = r.data.payload.amount?.toString() ?: "", error = null, result = null)
                    }
                }
            }
        }
    }

    fun setAmount(value: String) {
        if (_state.value.scanned?.payload?.isDynamic == true) return // nominal QRIS dinamis terkunci
        _state.update { it.copy(amount = value, error = null) }
    }

    fun requestPin() {
        val s = _state.value
        val amount = s.scanned?.payload?.amount ?: parseRupiah(s.amount)
        if (amount == null || amount <= 0) return _state.update { it.copy(error = "Masukkan nominal pembayaran") }
        // Validasi sebelum meminta PIN, supaya PIN tidak diminta untuk transaksi yang pasti ditolak.
        if (amount !in QrisRules.MIN_AMOUNT..QrisRules.MAX_AMOUNT) return _state.update {
            it.copy(error = "Nominal QRIS ${formatRupiah(QrisRules.MIN_AMOUNT)} – ${formatRupiah(QrisRules.MAX_AMOUNT)}")
        }
        if (amount > s.balance) return _state.update { it.copy(error = "Saldo tidak mencukupi") }
        _state.update { it.copy(showPin = true, error = null) }
    }

    fun dismissPin() = _state.update { it.copy(showPin = false, error = null) }

    fun pay(pin: String) {
        val s = _state.value
        val scanned = s.scanned ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            when (val r = repo.pay(userId, scanned, s.amount, pin, idempotencyKey)) {
                is AppResult.Failure -> _state.update { it.copy(busy = false, error = r.message) }
                is AppResult.Success -> _state.update { it.copy(busy = false, showPin = false, result = r.data) }
            }
        }
    }

    fun reset() = _state.update { QrisPayUiState(demoQrs = it.demoQrs, balance = it.balance) }

    companion object {
        fun factory(userId: String) = viewModelFactory { initializer { QrisPayViewModel(userId, FeatureModule.payment) } }
    }
}

data class QrisReceiveUiState(
    val staticQr: String? = null,
    val dynamicQr: String? = null,
    val dynamicAmount: Long? = null,
    val error: String? = null,
    val info: String? = null,
    val busy: Boolean = false
)

/** FR-16 & FR-17: QRIS pribadi untuk menerima dana (statis atau dinamis). */
class QrisReceiveViewModel(private val userId: String, private val repo: PaymentRepository) : ViewModel() {
    private val _state = MutableStateFlow(QrisReceiveUiState())
    val state: StateFlow<QrisReceiveUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val r = repo.personalQr(userId, null)) {
                is AppResult.Success -> _state.update { it.copy(staticQr = r.data) }
                is AppResult.Failure -> _state.update { it.copy(error = r.message) }
            }
        }
    }

    fun generateDynamic(rawAmount: String) {
        val amount = parseRupiah(rawAmount) ?: return _state.update { it.copy(error = "Nominal wajib diisi") }
        viewModelScope.launch {
            when (val r = repo.personalQr(userId, amount)) {
                is AppResult.Failure -> _state.update { it.copy(error = r.message) }
                is AppResult.Success -> _state.update { it.copy(dynamicQr = r.data, dynamicAmount = amount, error = null) }
            }
        }
    }

    fun clearDynamic() = _state.update { it.copy(dynamicQr = null, dynamicAmount = null) }

    /** Simulasi pengguna bank lain memindai QRIS pribadi lalu membayar. */
    fun simulateIncoming(rawAmount: String, payer: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, info = null) }
            when (val r = repo.simulateIncoming(userId, rawAmount, payer, UUID.randomUUID().toString())) {
                is AppResult.Failure -> _state.update { it.copy(busy = false, error = r.message) }
                is AppResult.Success -> _state.update {
                    it.copy(busy = false, info = "Dana masuk ${com.diponegoro.digitalbank.core.util.formatRupiah(r.data.amount)} dari ${r.data.fromName} tercatat di saldo & riwayat.")
                }
            }
        }
    }

    companion object {
        fun factory(userId: String) = viewModelFactory { initializer { QrisReceiveViewModel(userId, FeatureModule.payment) } }
    }
}
