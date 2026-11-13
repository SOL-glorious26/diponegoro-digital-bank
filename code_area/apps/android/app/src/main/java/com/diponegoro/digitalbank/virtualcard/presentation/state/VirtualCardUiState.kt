package com.diponegoro.digitalbank.virtualcard.presentation.state

import com.diponegoro.digitalbank.virtualcard.domain.model.VirtualCard

/** Dialog yang sedang terbuka, mengikuti cabang pada Activity Diagram Virtual Card. */
sealed interface CardDialog {
    data object CreateCard : CardDialog
    data class EnterPin(val cardId: String) : CardDialog
    data class SetLimit(val cardId: String, val currentLimit: Long) : CardDialog
    data class SimulateTransaction(val cardId: String) : CardDialog
}

data class VirtualCardUiState(
    val isLoading: Boolean = false,
    val cards: List<VirtualCard> = emptyList(),
    val financialEnabled: Boolean = true,
    /** Kartu yang detailnya sedang ditampilkan setelah PIN valid. */
    val revealedCardId: String? = null,
    val dialog: CardDialog? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)
