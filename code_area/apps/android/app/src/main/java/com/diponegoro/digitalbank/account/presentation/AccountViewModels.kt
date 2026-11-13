package com.diponegoro.digitalbank.account.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diponegoro.digitalbank.account.data.AccountRepository
import com.diponegoro.digitalbank.account.domain.BalancePeriod
import com.diponegoro.digitalbank.account.domain.BalancePoint
import com.diponegoro.digitalbank.account.domain.LockRule
import com.diponegoro.digitalbank.account.domain.Pocket
import com.diponegoro.digitalbank.core.di.CoreModule
import com.diponegoro.digitalbank.core.di.FeatureModule
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.core.util.formatRupiah
import com.diponegoro.digitalbank.core.util.parseRupiah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class BalanceChartUiState(
    val period: BalancePeriod = BalancePeriod.DAILY,
    val points: List<BalancePoint> = emptyList()
)

/** FR-07: grafik perubahan saldo harian / mingguan / bulanan, dihitung dari ledger. */
class BalanceChartViewModel(private val userId: String, private val repo: AccountRepository) : ViewModel() {
    private val _state = MutableStateFlow(BalanceChartUiState())
    val state: StateFlow<BalanceChartUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { CoreModule.revision.collect { load() } }
    }

    private suspend fun load() {
        val period = _state.value.period
        val points = repo.balanceSeries(userId, period)
        _state.update { if (it.period == period) it.copy(points = points) else it }
    }

    fun selectPeriod(period: BalancePeriod) {
        _state.update { it.copy(period = period) }
        viewModelScope.launch { load() }
    }

    companion object {
        fun factory(userId: String) = viewModelFactory { initializer { BalanceChartViewModel(userId, FeatureModule.accounts) } }
    }
}

/** Dialog yang sedang terbuka di layar kantong. */
sealed interface PocketDialog {
    data object Create : PocketDialog
    data class Edit(val pocket: Pocket) : PocketDialog
    data class Deposit(val pocket: Pocket) : PocketDialog
    data class Withdraw(val pocket: Pocket) : PocketDialog
    /** Penarikan dini kantong terkunci: konfirmasi tambahan (FR-09). */
    data class EarlyWithdrawConfirm(val pocket: Pocket, val amount: Long) : PocketDialog
    data class EarlyWithdrawPin(val pocket: Pocket, val amount: Long) : PocketDialog
    data class Lock(val pocket: Pocket) : PocketDialog
    data class Delete(val pocket: Pocket) : PocketDialog
}

data class PocketsUiState(
    val pockets: List<Pocket> = emptyList(),
    val mainBalance: Long = 0,
    val financialEnabled: Boolean = true,
    val dialog: PocketDialog? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

/** FR-08 & FR-09: kantong simpanan + locking. */
class PocketsViewModel(private val userId: String, private val repo: AccountRepository) : ViewModel() {
    private val _state = MutableStateFlow(PocketsUiState())
    val state: StateFlow<PocketsUiState> = _state.asStateFlow()

    /** Kunci idempotensi per dialog: dibuat saat dialog dibuka, dipakai ulang bila tombol ditekan lagi. */
    private var operationKey = UUID.randomUUID().toString()

    init {
        viewModelScope.launch { CoreModule.revision.collect { refresh() } }
    }

    private suspend fun refresh() {
        val overview = repo.overview(userId)
        val pockets = repo.pockets(userId)
        _state.update {
            it.copy(
                pockets = pockets,
                mainBalance = (overview as? AppResult.Success)?.data?.balance ?: 0,
                financialEnabled = (overview as? AppResult.Success)?.data?.financialEnabled ?: false
            )
        }
    }

    fun open(dialog: PocketDialog) {
        operationKey = UUID.randomUUID().toString()
        _state.update { it.copy(dialog = dialog, error = null, info = null) }
    }

    fun dismiss() = _state.update { it.copy(dialog = null, error = null) }

    private fun run(success: String, block: suspend () -> AppResult<*>) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, info = null) }
            when (val r = block()) {
                is AppResult.Failure -> _state.update { it.copy(busy = false, error = r.message) }
                is AppResult.Success -> {
                    refresh()
                    _state.update { it.copy(busy = false, dialog = null, info = success) }
                }
            }
        }
    }

    fun create(name: String, purpose: String, rawTarget: String, lock: LockRule?) {
        val target = rawTarget.takeIf { it.isNotBlank() }?.let { parseRupiah(it) }
        run("Kantong \"${name.trim()}\" dibuat") { repo.createPocket(userId, name, purpose, target, lock) }
    }

    fun update(pocket: Pocket, name: String, purpose: String, rawTarget: String) {
        val target = rawTarget.takeIf { it.isNotBlank() }?.let { parseRupiah(it) }
        run("Kantong diperbarui") { repo.updatePocket(userId, pocket.id, name, purpose, target) }
    }

    fun deposit(pocket: Pocket, rawAmount: String) {
        val amount = parseRupiah(rawAmount) ?: return _state.update { it.copy(error = "Nominal wajib diisi") }
        run("${formatRupiah(amount)} dipindahkan ke ${pocket.name}") { repo.deposit(userId, pocket.id, amount, operationKey) }
    }

    fun withdraw(pocket: Pocket, rawAmount: String) {
        val amount = parseRupiah(rawAmount) ?: return _state.update { it.copy(error = "Nominal wajib diisi") }
        if (amount > pocket.balance) return _state.update { it.copy(error = "Saldo kantong tidak mencukupi") }
        if (pocket.locked) {
            // Kantong terkunci → konfirmasi tambahan dulu, baru PIN.
            _state.update { it.copy(dialog = PocketDialog.EarlyWithdrawConfirm(pocket, amount), error = null) }
            return
        }
        run("${formatRupiah(amount)} dipindahkan ke rekening utama") { repo.withdraw(userId, pocket.id, amount, operationKey) }
    }

    fun confirmEarlyWithdraw(pocket: Pocket, amount: Long) =
        _state.update { it.copy(dialog = PocketDialog.EarlyWithdrawPin(pocket, amount), error = null) }

    fun earlyWithdraw(pocket: Pocket, amount: Long, pin: String) =
        run("Penarikan dini ${formatRupiah(amount)} berhasil") {
            repo.withdraw(userId, pocket.id, amount, operationKey, earlyWithdrawalConfirmed = true, pin = pin)
        }

    fun lock(pocket: Pocket, rule: LockRule) = run("Kantong ${pocket.name} dikunci") { repo.setLock(userId, pocket.id, rule) }

    fun delete(pocket: Pocket) = run("Kantong ${pocket.name} dihapus") { repo.deletePocket(userId, pocket.id, operationKey) }

    fun lockDescription(pocket: Pocket) = repo.lockDescription(pocket)

    companion object {
        fun factory(userId: String) = viewModelFactory { initializer { PocketsViewModel(userId, FeatureModule.accounts) } }
    }
}
