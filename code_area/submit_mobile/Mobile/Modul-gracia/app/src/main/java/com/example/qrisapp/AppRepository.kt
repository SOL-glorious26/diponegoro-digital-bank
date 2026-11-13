package com.example.qrisapp

import android.content.Context
import java.util.UUID

object AppRepository {
    lateinit var user: User
    lateinit var security: SecuritySetting
    lateinit var profile: Profile
    private val transactions = mutableListOf<Transaction>()
    private val notifications = mutableListOf<NotificationItem>()

    fun init(context: Context) {
        if (!::user.isInitialized) {
            user = User("U001", "Pengguna QRIS", "user@gmail.com", "08123456789")
            security = SecuritySetting("S001", user.userId, "12345678", "123456", false)
            profile = Profile("P001", user.userId, user.name)
            notifications.add(
                NotificationItem(
                    "N001", user.userId, "TRANSACTION",
                    "Selamat datang di aplikasi QRIS.", System.currentTimeMillis()
                )
            )
        }
    }

    fun validateLogin(email: String, password: String): Boolean =
        email == user.email && password == security.password

    fun verifyPin(pin: String): Boolean = pin == security.pin

    fun changePin(oldPin: String, newPin: String): Boolean {
        if (oldPin != security.pin || newPin.length < 6) return false
        security.pin = newPin
        return true
    }

    fun saveProfile(name: String, email: String, phone: String) {
        user.name = name
        user.email = email
        user.noHp = phone
        profile.name = name
    }

    fun processPayment(amount: Double): Transaction {
        val merchant = Merchant("M001", "Demo Merchant QRIS", "Retail")
        val trx = Transaction(
            UUID.randomUUID().toString(),
            amount,
            System.currentTimeMillis(),
            "SUCCESS",
            merchant
        )
        transactions.add(0, trx)
        notifications.add(
            0, NotificationItem(
                UUID.randomUUID().toString(), user.userId, "TRANSACTION",
                "Pembayaran Rp${"%,.0f".format(amount)} berhasil di ${merchant.name}.",
                System.currentTimeMillis(), "UNREAD"
            )
        )
        return trx
    }

    fun getTransactions(): List<Transaction> = transactions.toList()
    fun getNotifications(): List<NotificationItem> = notifications.toList()
}

class PaymentGateway(private val gatewayId: String = "PG001", private val nama: String = "Demo QRIS Gateway") {
    fun prosesTransaksi(amount: Double): Boolean = amount > 0
    fun cekStatus(): String = "READY"
}
