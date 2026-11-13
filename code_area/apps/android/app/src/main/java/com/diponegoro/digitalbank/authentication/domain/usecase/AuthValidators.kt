package com.diponegoro.digitalbank.authentication.domain.usecase

internal object AuthValidators {
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val PHONE = Regex("^\\+?[0-9]{9,15}$")

    fun isValidEmail(value: String) = EMAIL.matches(value)
    fun isValidPhone(value: String) = PHONE.matches(value)
    fun isStrongPassword(value: String) =
        value.length >= 8 && value.any { it.isLetter() } && value.any { it.isDigit() }
    fun isValidPin(value: String) = value.length == 6 && value.all { it.isDigit() }
    fun isValidOtp(value: String) = value.length == 6 && value.all { it.isDigit() }
}