package com.diponegoro.digitalbank.virtualcard.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.diponegoro.digitalbank.authentication.di.AuthModule
import com.diponegoro.digitalbank.authentication.domain.model.AccountStatus
import com.diponegoro.digitalbank.authentication.domain.model.AuthResult
import com.diponegoro.digitalbank.authentication.domain.usecase.GetUserUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.ValidateTransactionPinUseCase
import com.diponegoro.digitalbank.core.di.CoreModule
import com.diponegoro.digitalbank.virtualcard.data.repository.MockVirtualCardRepository
import com.diponegoro.digitalbank.virtualcard.data.repository.RemoteVirtualCardRepository
import com.diponegoro.digitalbank.virtualcard.domain.model.CardResult
import com.diponegoro.digitalbank.virtualcard.domain.repository.AuthGateway
import com.diponegoro.digitalbank.virtualcard.domain.repository.VirtualCardRepository
import com.diponegoro.digitalbank.virtualcard.domain.usecase.*
import com.diponegoro.digitalbank.virtualcard.presentation.viewmodel.VirtualCardViewModel

/** Manual DI, mengikuti pola AuthModule. */
object VirtualCardModule {

    private val repository: VirtualCardRepository by lazy {
        if (CoreModule.online) RemoteVirtualCardRepository(CoreModule.api, CoreModule::dataChanged) else MockVirtualCardRepository()
    }

    /** Menghubungkan Virtual Card ke modul Authentication (status e-KYC & PIN transaksi). */
    private val authGateway: AuthGateway by lazy {
        val getUser = GetUserUseCase(AuthModule.repository)
        val validatePin = ValidateTransactionPinUseCase(AuthModule.repository)
        object : AuthGateway {
            override suspend fun isFinancialEnabled(userId: String): Boolean =
                getUser(userId)?.accountStatus == AccountStatus.FINANCIAL_ENABLED

            override suspend fun verifyPin(userId: String, pin: String): CardResult<Unit> =
                when (val r = validatePin(userId, pin)) {
                    is AuthResult.Success -> CardResult.Success(Unit)
                    is AuthResult.Failure -> CardResult.Failure(r.message)
                }
        }
    }

    fun viewModelFactory(userId: String, cardholderName: String): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(VirtualCardViewModel::class.java)) {
                    "Unknown ViewModel: ${modelClass.name}"
                }
                return VirtualCardViewModel(
                    userId = userId,
                    cardholderName = cardholderName,
                    isFinancialEnabled = authGateway::isFinancialEnabled,
                    getVirtualCards = GetVirtualCardsUseCase(repository),
                    createVirtualCard = CreateVirtualCardUseCase(repository, authGateway),
                    viewCardDetails = ViewCardDetailsUseCase(repository, authGateway),
                    freezeCard = FreezeCardUseCase(repository),
                    activateCard = ActivateCardUseCase(repository, authGateway),
                    setTransactionLimit = SetTransactionLimitUseCase(repository),
                    simulateTransaction = SimulateCardTransactionUseCase(repository)
                ) as T
            }
        }
}
