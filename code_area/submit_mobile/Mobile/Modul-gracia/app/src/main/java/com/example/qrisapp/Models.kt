package com.example.qrisapp

data class User(
    val userId: String,
    var name: String,
    var email: String,
    var noHp: String
)

data class Profile(
    val profileId: String,
    val userId: String,
    var name: String,
    var foto: String = "",
    var alamat: String = ""
)

data class SecuritySetting(
    val securityId: String,
    val userId: String,
    var password: String,
    var pin: String,
    var biometric: Boolean = false
)

data class Merchant(
    val merchantId: String,
    val name: String,
    val kategori: String
)

data class QRISCode(
    val qrId: String,
    val type: String,
    val kodeQR: String,
    val expiredAt: Long
)

data class QRISPayment(
    val paymentId: String,
    val tanggal: Long,
    val nominal: Double,
    var status: String,
    val qrCode: QRISCode
)

data class Transaction(
    val transactionId: String,
    val amount: Double,
    val date: Long,
    var status: String,
    val merchant: Merchant
)

data class NotificationItem(
    val notifId: String,
    val userId: String,
    val type: String,
    val pesan: String,
    val tanggal: Long,
    var status: String = "UNREAD"
)

data class NotificationSetting(
    val settingId: String,
    val userId: String,
    var jenisNotifikasi: String,
    var channel: String
)

data class OTP(
    val otpId: String,
    val userId: String,
    val code: String,
    val expiredAt: Long
)
