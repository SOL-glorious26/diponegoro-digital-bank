package com.diponegoro.digitalbank.transfer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diponegoro.digitalbank.core.di.FeatureModule
import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.transfer.data.TransferRepository
import com.diponegoro.digitalbank.transfer.domain.Bank
import com.diponegoro.digitalbank.transfer.domain.FavoriteRecipient
import com.diponegoro.digitalbank.transfer.domain.Recipient
import com.diponegoro.digitalbank.transfer.domain.TransferQuote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class TransferStep { RECIPIENT, AMOUNT, CONFIRM, RESULT }

data class TransferUiState(
    val step: TransferStep = TransferStep.RECIPIENT,
    val banks: List<Bank> = emptyList(),
    val favorites: List<FavoriteRecipient> = emptyList(),
    val recipient: Recipient? = null,
    val isFavorite: Boolean = false,
    val quote: TransferQuote? = null,
    val showPin: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val result: LedgerTransaction? = null
)

/** UC-01 Transfer Dana. */
class TransferViewModel(private val userId: String, private val repo: TransferRepository) : ViewModel() {
    private val _state = MutableStateFlow(TransferUiState())
    val state: StateFlow<TransferUiState> = _state.asStateFlow()

    /** Kunci idempotensi dibuat saat layar konfirmasi tampil; retry memakai kunci yang sama (BR-04). */
    private var idempotencyKey = UUID.randomUUID().toString()

    init {
        loadLists()
    }

    private fun loadLists() = viewModelScope.launch {
        val banks = repo.banks()
        val favorites = repo.favorites(userId)
        _state.update { it.copy(banks = banks, favorites = favorites) }
    }

    fun clearMessages() = _state.update { it.copy(error = null, info = null) }

    // Langkah 1–2: bank + rekening → validasi nama penerima.
    fun inquire(bankCode: String, number: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            when (val r = repo.inquire(userId, bankCode, number)) {
                is AppResult.Failure -> _state.update { it.copy(busy = false, error = r.message) }
                is AppResult.Success -> { _state.update { it.copy(busy = false) }; showRecipient(r.data) }
            }
        }
    }

    fun selectRecipient(recipient: Recipient) {
        viewModelScope.launch { showRecipient(recipient) }
    }

    private suspend fun showRecipient(recipient: Recipient) {
        val favorite = repo.isFavorite(userId, recipient)
        _state.update {
            it.copy(recipient = recipient, isFavorite = favorite, step = TransferStep.AMOUNT, error = null, info = null)
        }
    }

    fun toggleFavorite() {
        val recipient = _state.value.recipient ?: return
        viewModelScope.launch {
            val existing = repo.favorites(userId).firstOrNull {
                it.recipient.bank.code == recipient.bank.code && it.recipient.accountNumber == recipient.accountNumber
            }
            if (existing != null) {
                repo.removeFavorite(userId, existing.id)
                _state.update { it.copy(isFavorite = false, favorites = repo.favorites(userId), info = "Dihapus dari favorit") }
            } else when (val r = repo.addFavorite(userId, recipient)) {
                is AppResult.Failure -> _state.update { it.copy(error = r.message) }
                is AppResult.Success -> _state.update { it.copy(isFavorite = true, favorites = repo.favorites(userId), info = "Disimpan ke favorit") }
            }
        }
    }

    fun removeFavorite(id: String) {
        viewModelScope.launch {
            repo.removeFavorite(userId, id)
            _state.update { it.copy(favorites = repo.favorites(userId)) }
        }
    }

    // Langkah 3–4: nominal → konfirmasi.
    fun review(rawAmount: String, note: String) {
        val recipient = _state.value.recipient ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            when (val r = repo.quote(userId, recipient, rawAmount, note)) {
                is AppResult.Failure -> _state.update { it.copy(busy = false, error = r.message) }
                is AppResult.Success -> {
                    idempotencyKey = UUID.randomUUID().toString()
                    _state.update { it.copy(busy = false, quote = r.data, step = TransferStep.CONFIRM, error = null) }
                }
            }
        }
    }

    fun requestPin() = _state.update { it.copy(showPin = true, error = null) }
    fun dismissPin() = _state.update { it.copy(showPin = false, error = null) }

    // Langkah 5–7: PIN → proses → bukti.
    fun submit(pin: String) {
        val quote = _state.value.quote ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            when (val r = repo.execute(userId, quote, pin, idempotencyKey)) {
                is AppResult.Failure -> _state.update { it.copy(busy = false, error = r.message) }
                is AppResult.Success -> _state.update {
                    it.copy(busy = false, showPin = false, result = r.data, step = TransferStep.RESULT)
                }
            }
        }
    }

    fun checkStatus() {
        val tx = _state.value.result ?: return
        viewModelScope.launch {
            when (val r = repo.checkStatus(userId, tx.id)) {
                is AppResult.Failure -> _state.update { it.copy(error = r.message) }
                is AppResult.Success -> _state.update { it.copy(result = r.data, info = "Status diperbarui: ${r.data.status.label}") }
            }
        }
    }

    fun back(): Boolean {
        val s = _state.value
        val previous = when (s.step) {
            TransferStep.RECIPIENT, TransferStep.RESULT -> return false
            TransferStep.AMOUNT -> TransferStep.RECIPIENT
            TransferStep.CONFIRM -> TransferStep.AMOUNT
        }
        _state.update { it.copy(step = previous, error = null, info = null) }
        return true
    }

    fun restart() {
        _state.value = TransferUiState(banks = _state.value.banks)
        loadLists()
    }

    companion object {
        fun factory(userId: String) = viewModelFactory { initializer { TransferViewModel(userId, FeatureModule.transfer) } }
    }
}
