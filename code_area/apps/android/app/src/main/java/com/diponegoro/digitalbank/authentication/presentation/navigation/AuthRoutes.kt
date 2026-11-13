package com.diponegoro.digitalbank.authentication.presentation.navigation

object AuthRoutes {
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val OTP = "auth/otp"
    const val EKYC = "auth/ekyc"
    const val CREATE_PIN = "auth/create_pin"
    const val ENTER_PIN = "auth/enter_pin"
    const val HOME = "home" // = AppRoutes.HOME (dashboard)
    const val RECOVERY = "auth/recovery"

    // Modul lain yang diakses setelah login
    const val VIRTUAL_CARD = "virtualcard/home"
}