package com.diponegoro.digitalbank.authentication.presentation.state

import com.diponegoro.digitalbank.authentication.domain.model.RecoveryType

enum class RecoveryStep { REQUEST, VERIFY_OTP, RESET, DONE }

data class RecoveryUiState(
    val step: RecoveryStep = RecoveryStep.REQUEST,
    val type: RecoveryType = RecoveryType.PASSWORD,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val userId: String? = null,
    val demoOtpCode: String? = null
)