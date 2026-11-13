package com.diponegoro.digitalbank.authentication.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.diponegoro.digitalbank.authentication.data.repository.MockAuthRepository
import com.diponegoro.digitalbank.authentication.data.repository.RemoteAuthRepository
import com.diponegoro.digitalbank.authentication.domain.repository.AuthRepository
import com.diponegoro.digitalbank.core.di.CoreModule
import com.diponegoro.digitalbank.authentication.domain.usecase.*
import com.diponegoro.digitalbank.authentication.presentation.viewmodel.AuthViewModel
import com.diponegoro.digitalbank.authentication.presentation.viewmodel.RecoveryViewModel

/** Manual DI sederhana (tanpa Hilt) supaya cepat untuk prototype. */
object AuthModule {

    /** Dipakai bersama oleh modul lain (mis. Virtual Card) untuk validasi PIN & status akun. */
    internal val repository: AuthRepository by lazy {
        if (CoreModule.online) RemoteAuthRepository(CoreModule.api, CoreModule.tokens) else MockAuthRepository()
    }

    val viewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(
                registerUserUseCase = RegisterUserUseCase(repository),
                sendOtpUseCase = SendOtpUseCase(repository),
                verifyOtpUseCase = VerifyOtpUseCase(repository),
                submitEkycUseCase = SubmitEkycUseCase(repository),
                checkEkycStatusUseCase = CheckEkycStatusUseCase(repository),
                loginUseCase = LoginUseCase(repository),
                loginWithBiometricUseCase = LoginWithBiometricUseCase(repository),
                isBiometricAvailableUseCase = IsBiometricAvailableUseCase(repository),
                getUserUseCase = GetUserUseCase(repository),
                getTransactionPinUseCase = GetTransactionPinUseCase(repository),
                createTransactionPinUseCase = CreateTransactionPinUseCase(repository),
                validateTransactionPinUseCase = ValidateTransactionPinUseCase(repository),
                getActiveSessionsUseCase = GetActiveSessionsUseCase(repository),
                logoutUseCase = LogoutUseCase(repository),
                logoutAllDevicesUseCase = LogoutAllDevicesUseCase(repository)
            ) as T

            modelClass.isAssignableFrom(RecoveryViewModel::class.java) -> RecoveryViewModel(
                requestRecoveryUseCase = RequestRecoveryUseCase(repository),
                sendOtpUseCase = SendOtpUseCase(repository),
                verifyOtpUseCase = VerifyOtpUseCase(repository),
                resetPasswordUseCase = ResetPasswordUseCase(repository),
                resetPinUseCase = ResetPinUseCase(repository)
            ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}