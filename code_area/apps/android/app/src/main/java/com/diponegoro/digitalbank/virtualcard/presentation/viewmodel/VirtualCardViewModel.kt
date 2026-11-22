package com.diponegoro.digitalbank.virtualcard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diponegoro.digitalbank.virtualcard.domain.model.CardResult
import com.diponegoro.digitalbank.virtualcard.domain.model.CardScope
import com.diponegoro.digitalbank.virtualcard.domain.model.VirtualCard
import com.diponegoro.digitalbank.virtualcard.domain.usecase.ActivateCardUseCase
import com.diponegoro.digitalbank.virtualcard.domain.usecase.CreateVirtualCardUseCase
import com.diponegoro.digitalbank.virtualcard.domain.usecase.FreezeCardUseCase
import com.diponegoro.digitalbank.virtualcard.domain.usecase.GetVirtualCardsUseCase
import com.diponegoro.digitalbank.virtualcard.domain.usecase.SetTransactionLimitUseCase
import com.diponegoro.digitalbank.virtualcard.domain.usecase.SimulateCardTransactionUseCase
import com.diponegoro.digitalbank.virtualcard.domain.usecase.ViewCardDetailsUseCase
import com.diponegoro.digitalbank.virtualcard.domain.usecase.formatRupiah
import com.diponegoro.digitalbank.virtualcard.presentation.state.CardDialog
import com.diponegoro.digitalbank.virtualcard.presentation.state.VirtualCardUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VirtualCardViewModel(
    private val userId: String,
    private val cardholderName: String,
    private val isFinancialEnabled: suspend (String) -> Boolean,
    private val getVirtualCards: GetVirtualCardsUseCase,
    private val createVirtualCard: CreateVirtualCardUseCase,
    private val viewCardDetails: ViewCardDetailsUseCase,
    private val freezeCard: FreezeCardUseCase,
    private val activateCard: ActivateCardUseCase,
    private val setTransactionLimit: SetTransactionLimitUseCase,
    private val simulateTransaction: SimulateCardTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VirtualCardUiState())
    val uiState: StateFlow<VirtualCardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    // ---------- helpers ----------

    private fun setLoading() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
    }

    private fun fail(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    private fun replaceCard(card: VirtualCard) {
        _uiState.update { s -> s.copy(cards = s.cards.map { if (it.cardId == card.cardId) card else it }) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialog = null, errorMessage = null) }
    }

    // ---------- Display Virtual Card options ----------

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val enabled = isFinancialEnabled(userId)
            val cards = getVirtualCards(userId)
            _uiState.update { it.copy(isLoading = false, cards = cards, financialEnabled = enabled) }
        }
    }

    // ---------- [create] ----------

    fun openCreate() {
        _uiState.update { it.copy(dialog = CardDialog.CreateCard, errorMessage = null, infoMessage = null) }
    }

    fun createCard(scope: CardScope) {
        viewModelScope.launch {
            setLoading()
            when (val r = createVirtualCard(userId, cardholderName, scope)) {
                is CardResult.Failure -> fail(r.message)
                is CardResult.Success -> _uiState.update { s ->
                    s.copy(
                        isLoading = false,
                        dialog = null,
                        cards = listOf(r.data) + s.cards,
                        infoMessage = "Kartu virtual ${r.data.maskedNumber} berhasil dibuat (status: Aktif)"
                    )
                }
            }
        }
    }

    // ---------- [view] ----------

    fun requestDetails(cardId: String) {
        _uiState.update { it.copy(dialog = CardDialog.EnterPin(cardId), errorMessage = null, infoMessage = null) }
    }

    fun submitPin(pin: String) {
        val dialog = _uiState.value.dialog as? CardDialog.EnterPin ?: return
        viewModelScope.launch {
            setLoading()
            when (val r = viewCardDetails(userId, dialog.cardId, pin)) {
                is CardResult.Failure -> fail(r.message) // Reject access to card details
                is CardResult.Success -> {
                    // Daftar kartu dari server hanya memuat nomor tersamar; pakai nomor dan CVV hasil verifikasi PIN.
                    replaceCard(r.data)
                    _uiState.update {
                        it.copy(isLoading = false, dialog = null, revealedCardId = r.data.cardId)
                    }
                }
            }
        }
    }

    fun hideDetails() {
        _uiState.update { it.copy(revealedCardId = null) }
    }

    // ---------- [freeze] / [activate] ----------

    fun freeze(cardId: String) {
        viewModelScope.launch {
            setLoading()
            when (val r = freezeCard(userId, cardId)) {
                is CardResult.Failure -> fail(r.message)
                is CardResult.Success -> {
                    replaceCard(r.data)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            revealedCardId = null,
                            infoMessage = "Kartu ${r.data.maskedNumber} dibekukan. Semua transaksi akan ditolak."
                        )
                    }
                }
            }
        }
    }

    fun activate(cardId: String) {
        viewModelScope.launch {
            setLoading()
            when (val r = activateCard(userId, cardId)) {
                is CardResult.Failure -> fail(r.message)
                is CardResult.Success -> {
                    replaceCard(r.data)
                    _uiState.update {
                        it.copy(isLoading = false, infoMessage = "Kartu ${r.data.maskedNumber} aktif kembali.")
                    }
                }
            }
        }
    }

    // ---------- [limit] ----------

    fun openLimit(cardId: String) {
        val card = _uiState.value.cards.firstOrNull { it.cardId == cardId } ?: return
        _uiState.update {
            it.copy(dialog = CardDialog.SetLimit(cardId, card.transactionLimit), errorMessage = null, infoMessage = null)
        }
    }

    fun saveLimit(rawLimit: String) {
        val dialog = _uiState.value.dialog as? CardDialog.SetLimit ?: return
        viewModelScope.launch {
            setLoading()
            when (val r = setTransactionLimit(userId, dialog.cardId, rawLimit)) {
                is CardResult.Failure -> fail(r.message) // Reject value, display error → input ulang
                is CardResult.Success -> {
                    replaceCard(r.data)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            dialog = null,
                            infoMessage = "Limit diperbarui menjadi ${formatRupiah(r.data.transactionLimit)} " +
                                "dan berlaku untuk transaksi berikutnya."
                        )
                    }
                }
            }
        }
    }

    // ---------- Simulasi transaksi (pengujian FR-22) ----------

    fun openSimulate(cardId: String) {
        _uiState.update {
            it.copy(dialog = CardDialog.SimulateTransaction(cardId), errorMessage = null, infoMessage = null)
        }
    }

    fun simulate(rawAmount: String, international: Boolean) {
        val dialog = _uiState.value.dialog as? CardDialog.SimulateTransaction ?: return
        viewModelScope.launch {
            setLoading()
            when (val r = simulateTransaction(userId, dialog.cardId, rawAmount, international)) {
                is CardResult.Failure -> fail(r.message)
                is CardResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        dialog = null,
                        infoMessage = "Transaksi ${formatRupiah(r.data)} disetujui (simulasi)."
                    )
                }
            }
        }
    }
}
