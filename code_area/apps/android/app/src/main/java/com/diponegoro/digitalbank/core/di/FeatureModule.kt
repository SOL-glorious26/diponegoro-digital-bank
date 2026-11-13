package com.diponegoro.digitalbank.core.di

import com.diponegoro.digitalbank.account.data.AccountRepository
import com.diponegoro.digitalbank.account.data.LocalAccountRepository
import com.diponegoro.digitalbank.account.data.RemoteAccountRepository
import com.diponegoro.digitalbank.authentication.di.AuthModule
import com.diponegoro.digitalbank.history.data.HistoryRepository
import com.diponegoro.digitalbank.history.data.LocalHistoryRepository
import com.diponegoro.digitalbank.history.data.RemoteHistoryRepository
import com.diponegoro.digitalbank.payment.data.LocalPaymentRepository
import com.diponegoro.digitalbank.payment.data.PaymentRepository
import com.diponegoro.digitalbank.payment.data.RemotePaymentRepository
import com.diponegoro.digitalbank.profile.data.LocalProfileRepository
import com.diponegoro.digitalbank.profile.data.ProfileRepository
import com.diponegoro.digitalbank.profile.data.RemoteProfileRepository
import com.diponegoro.digitalbank.topup.data.LocalTopUpRepository
import com.diponegoro.digitalbank.topup.data.RemoteTopUpRepository
import com.diponegoro.digitalbank.topup.data.TopUpRepository
import com.diponegoro.digitalbank.transfer.data.LocalTransferRepository
import com.diponegoro.digitalbank.transfer.data.RemoteTransferRepository
import com.diponegoro.digitalbank.transfer.data.TransferRepository

/**
 * Repository setiap business module. Varian demo: ledger & guard in-memory bersama.
 * Varian online (CoreModule.online): implementasi Remote* ke backend bersama (Fase 5).
 */
object FeatureModule {
    val accounts: AccountRepository by lazy {
        if (CoreModule.online) RemoteAccountRepository(CoreModule.api, CoreModule::dataChanged)
        else LocalAccountRepository(CoreModule.ledger, CoreModule.guard, CoreModule.preferences)
    }
    val transfer: TransferRepository by lazy {
        if (CoreModule.online) RemoteTransferRepository(CoreModule.api, CoreModule::dataChanged)
        else LocalTransferRepository(CoreModule.ledger, CoreModule.guard)
    }
    val payment: PaymentRepository by lazy {
        if (CoreModule.online) RemotePaymentRepository(CoreModule.api, CoreModule::dataChanged)
        else LocalPaymentRepository(CoreModule.ledger, CoreModule.guard)
    }
    val topUp: TopUpRepository by lazy {
        if (CoreModule.online) RemoteTopUpRepository(CoreModule.api, CoreModule::dataChanged)
        else LocalTopUpRepository(CoreModule.ledger, CoreModule.guard)
    }
    val history: HistoryRepository by lazy {
        if (CoreModule.online) RemoteHistoryRepository(CoreModule.api) else LocalHistoryRepository(CoreModule.ledger)
    }
    val profile: ProfileRepository by lazy {
        if (CoreModule.online) RemoteProfileRepository(CoreModule.api, AuthModule.repository, CoreModule.notificationSource)
        else LocalProfileRepository(AuthModule.repository, CoreModule.notifications)
    }
}
