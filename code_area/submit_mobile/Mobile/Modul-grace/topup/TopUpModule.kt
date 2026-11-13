package com.diponegoro.digitalbank.topup

import android.content.Context
import com.diponegoro.digitalbank.account.AccountModule
import com.diponegoro.digitalbank.authentication.di.AuthModule
import com.diponegoro.digitalbank.topup.data.TopUpRepository

object TopUpModule {
    private var instance: TopUpRepository? = null
    @Synchronized fun repository(context: Context): TopUpRepository = instance ?: TopUpRepository(
        AccountModule.database(context), AccountModule.repository(context), AuthModule.repository
    ).also { instance = it }
}
