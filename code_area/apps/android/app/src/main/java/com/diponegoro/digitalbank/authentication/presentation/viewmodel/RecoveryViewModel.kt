package com.diponegoro.digitalbank.authentication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diponegoro.digitalbank.authentication.domain.model.AuthResult
import com.diponegoro.digitalbank.authentication.domain.model.RecoveryType
import com.diponegoro.digitalbank.authentication.domain.usecase.RequestRecoveryUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.ResetPasswordUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.ResetPinUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.SendOtpUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.VerifyOtpUseCase
import com.diponegoro.digitalbank.authentication.presentation.state.RecoveryStep
import com.diponegoro.digitalbank.authentication.presentation.state.RecoveryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecoveryViewModel(
    private val requestRecoveryUseCase: RequestRecoveryUseCase,
    private val sendOtpUseCase: SendOtpUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val resetPinUseCase: ResetPinUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecoveryUiState())
    val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()

    private fun setLoading() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
    }

    private fun fail(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    fun selectType(type: RecoveryType) {
        _uiState.update { it.copy(type = type, errorMessage = null) }
    }

    fun requestRecovery(identifier: String) {
        viewModelScope.launch {
            setLoading()
            when (val result = requestRecoveryUseCase(identifier, _uiState.value.type)) {
                is AuthResult.Failure -> fail(result.message)
                is AuthResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        step = RecoveryStep.VERIFY_OTP,
                        userId = result.data.userId,
                        demoOtpCode = result.data.otpCode,
                        infoMessage = "Kode OTP dikirim (simulasi)"
                    )
                }
            }
        }
    }

    fun resendOtp() {
        val state = _uiState.value
        val userId = state.userId ?: return
        viewModelScope.launch {
            setLoading()
            when (val result = sendOtpUseCase(userId, state.type.otpPurpose)) {
                is AuthResult.Failure -> fail(result.message)
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoading = false, demoOtpCode = result.data, infoMessage = "Kode OTP baru dikirim (simulasi)")
                }
            }
        }
    }

    fun verifyOtp(code: String) {
        val state = _uiState.value
        val userId = state.userId ?: return
        viewModelScope.launch {
            setLoading()
            when (val result = verifyOtpUseCase(userId, state.type.otpPurpose, code)) {
                is AuthResult.Failure -> fail(result.message)
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoading = false, step = RecoveryStep.RESET, demoOtpCode = null)
                }
            }
        }
    }

    fun resetCredential(newValue: String) {
        val state = _uiState.value
        val userId = state.userId ?: return
        viewModelScope.launch {
            setLoading()
            val result = when (state.type) {
                RecoveryType.PASSWORD -> resetPasswordUseCase(userId, newValue)
                RecoveryType.PIN -> resetPinUseCase(userId, newValue)
            }
            when (result) {
                is AuthResult.Failure -> fail(result.message)
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoading = false, step = RecoveryStep.DONE)
                }
            }
        }
    }
}