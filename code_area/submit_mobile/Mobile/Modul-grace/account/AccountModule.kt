package com.diponegoro.digitalbank.account

import android.content.Context
import com.diponegoro.digitalbank.account.data.AccountDatabase
import com.diponegoro.digitalbank.account.data.AccountRepository
import com.diponegoro.digitalbank.authentication.di.AuthModule

object AccountModule {
    private var instance: AccountRepository? = null
    @Synchronized fun repository(context: Context): AccountRepository = instance ?: AccountRepository(
        AccountDatabase(context.applicationContext), AuthModule.repository
    ).also { instance = it }
}
