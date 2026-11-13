package com.diponegoro.digitalbank

import android.app.Application
import com.diponegoro.digitalbank.core.di.CoreModule

class DigitalBankApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CoreModule.init(this)
    }
}
