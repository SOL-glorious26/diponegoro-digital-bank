package com.diponegoro.digitalbank.core.network

import kotlinx.serialization.Serializable

/*
 * DTO JSON backend (kontrak OpenAPI /v3/api-docs, FSD-DDB-001 Tabel 4.2).
 * Nama field sama persis dengan backend. Uang = Long rupiah, waktu = string ISO-8601 UTC
 * (diubah ke epoch millis oleh IsoTime), tanggal = "yyyy-MM-dd".
 */

@Serializable data class ErrorBody(val code: String? = null, val message: String? = null, val fields: Map<String, String>? = null)
@Serializable data class MessageResponse(val message: String = "")

// ------------------------------------------------------------------ Authentication
@Serializable data class UserDto(
    val id: String, val username: String, val email: String? = null, val phoneNumber: String? = null,
    val status: String, val ekycStatus: String? = null, val hasPin: Boolean = false,
    val biometricEnabled: Boolean = false, val photoUrl: String? = null
)
@Serializable data class OtpInfo(val sentTo: String? = null, val expiresAt: String? = null, val demoCode: String? = null)
@Serializable data class RegisterRequest(val username: String, val email: String?, val phoneNumber: String?, val password: String)
@Serializable data class RegisterResponse(val userId: String, val otp: OtpInfo? = null)
@Serializable data class VerifyRegistrationRequest(val userId: String, val code: String, val deviceId: String, val deviceName: String?)
@Serializable data class ResendOtpRequest(val userId: String)
@Serializable data class AuthTokens(
    val accessToken: String, val accessTokenExpiresAt: String? = null, val refreshToken: String,
    val refreshTokenExpiresAt: String? = null, val sessionId: String, val user: UserDto
)
@Serializable data class RefreshRequest(val refreshToken: String)
@Serializable data class LoginRequest(val identifier: String, val password: String, val deviceId: String, val deviceName: String?)
@Serializable data class EkycRequest(val nik: String, val fullName: String?, val selfieReference: String)
@Serializable data class EkycDto(
    val id: String, val status: String, val nikMasked: String? = null, val reason: String? = null,
    val submittedAt: String? = null, val decidedAt: String? = null
)
@Serializable data class PinRequest(val pin: String)
@Serializable data class SessionDto(
    val id: String, val deviceId: String, val deviceName: String? = null, val authMethod: String,
    val createdAt: String, val lastSeenAt: String, val expiresAt: String, val current: Boolean = false
)
@Serializable data class RecoveryRequest(val identifier: String, val type: String)
@Serializable data class RecoveryStarted(val message: String = "", val otp: OtpInfo? = null)
@Serializable data class RecoveryVerifyRequest(val identifier: String, val type: String, val code: String)
@Serializable data class RecoveryVerified(val recoveryToken: String)
@Serializable data class RecoveryResetRequest(val identifier: String, val type: String, val recoveryToken: String, val newSecret: String)
@Serializable data class BiometricEnrollRequest(val deviceId: String)
@Serializable data class BiometricEnrollment(val biometricToken: String)
@Serializable data class BiometricLoginRequest(val biometricToken: String, val deviceId: String, val deviceName: String?)

// ------------------------------------------------------------------ Profile & Notification
@Serializable data class ProfileDraftDto(val username: String, val email: String?, val phoneNumber: String?)
@Serializable data class OtpCodeRequest(val code: String)
@Serializable data class PhotoRequest(val photoUrl: String?)
@Serializable data class PasswordChange(val oldPassword: String, val newPassword: String)
@Serializable data class PinChange(val oldPin: String, val newPin: String)
@Serializable data class SettingsDto(val types: List<String> = emptyList(), val channels: List<String> = emptyList())
@Serializable data class NotificationDto(
    val id: String, val type: String, val title: String, val message: String,
    val transactionId: String? = null, val read: Boolean = false, val createdAt: String
)
@Serializable data class EmailDto(val id: String, val to: String, val subject: String, val body: String, val sentAt: String)
@Serializable data class NotificationPage(val items: List<NotificationDto> = emptyList(), val unread: Long = 0, val page: Int = 0, val hasMore: Boolean = false)

// ------------------------------------------------------------------ Account
@Serializable data class AccountOverviewDto(
    val ownerName: String, val accountNumber: String, val balance: Long, val pocketsTotal: Long,
    val balanceHidden: Boolean, val financialEnabled: Boolean
)
@Serializable data class BalanceVisibilityRequest(val hidden: Boolean)
@Serializable data class BalancePointDto(val label: String, val at: String, val balance: Long)
@Serializable data class BalanceChartDto(val period: String, val points: List<BalancePointDto> = emptyList())
@Serializable data class PocketDto(
    val id: String, val name: String, val purpose: String? = null, val target: Long? = null, val balance: Long,
    val lockType: String = "NONE", val lockUntil: String? = null, val locked: Boolean = false,
    val lockDescription: String? = null, val createdAt: String
)
@Serializable data class CreatePocketRequest(val name: String, val purpose: String?, val target: Long?, val lockType: String, val lockUntil: String?)
@Serializable data class UpdatePocketRequest(val name: String, val purpose: String?, val target: Long?)
@Serializable data class LockPocketRequest(val lockType: String, val lockUntil: String?)
@Serializable data class MoveRequest(val amount: Long)
@Serializable data class WithdrawRequest(val amount: Long, val earlyWithdrawal: Boolean, val pin: String?)

// ------------------------------------------------------------------ Ledger / History
@Serializable data class TransactionDto(
    val id: String, val reference: String, val category: String, val categoryLabel: String? = null,
    val status: String, val statusLabel: String? = null, val direction: String, val amount: Long, val fee: Long = 0,
    val total: Long = 0, val title: String, val fromName: String, val fromDetail: String, val toName: String,
    val toDetail: String, val note: String? = null, val statusReason: String? = null,
    val extra: Map<String, String>? = null, val createdAt: String, val updatedAt: String
)
@Serializable data class HistoryPageDto(val items: List<TransactionDto> = emptyList(), val page: Int = 0, val size: Int = 0, val totalItems: Long = 0, val hasMore: Boolean = false)
@Serializable data class JournalLineDto(val account: String, val amount: Long, val memo: String, val postedAt: String)
@Serializable data class TransactionDetailDto(val transaction: TransactionDto, val journal: List<JournalLineDto> = emptyList())
@Serializable data class MonthSummaryDto(val label: String, val month: String, val income: Long, val expense: Long, val net: Long = 0)

// ------------------------------------------------------------------ Transfer
@Serializable data class BankDto(val code: String, val name: String, val internal: Boolean, val fee: Long)
@Serializable data class InquiryRequest(val bankCode: String, val accountNumber: String)
@Serializable data class RecipientDto(val bankCode: String, val bankName: String, val accountNumber: String, val name: String, val favorite: Boolean = false)
@Serializable data class QuoteRequest(val bankCode: String, val accountNumber: String, val amount: Long, val note: String?)
@Serializable data class QuoteDto(val recipient: RecipientDto, val amount: Long, val fee: Long, val total: Long, val note: String? = null, val balance: Long, val balanceAfter: Long)
@Serializable data class TransferRequest(val bankCode: String, val accountNumber: String, val amount: Long, val note: String?, val pin: String)
@Serializable data class FavoriteRequest(val bankCode: String, val accountNumber: String)
@Serializable data class FavoriteDto(val id: String, val bankCode: String, val bankName: String, val accountNumber: String, val name: String, val addedAt: String)

// ------------------------------------------------------------------ Payment (QRIS)
@Serializable data class ScanRequest(val payload: String)
@Serializable data class ScanResultDto(
    val merchantName: String, val merchantId: String, val city: String? = null, val mcc: String? = null,
    val dynamic: Boolean = false, val amount: Long? = null, val sameBank: Boolean = false, val issuer: String? = null,
    val payload: String
)
@Serializable data class PayRequest(val payload: String, val amount: Long?, val pin: String)
@Serializable data class MyQrDto(val payload: String, val dynamic: Boolean = false, val amount: Long? = null, val accountName: String? = null, val accountNumber: String? = null)
@Serializable data class DemoQrDto(val label: String, val payload: String)
@Serializable data class IncomingRequest(val amount: Long, val payerName: String?)

// ------------------------------------------------------------------ Top-up
@Serializable data class OperatorDto(val code: String, val name: String, val prefixes: List<String> = emptyList())
@Serializable data class ProductDto(val id: String, val name: String, val description: String? = null, val price: Long)
@Serializable data class DataPackagesDto(val operator: OperatorDto, val packages: List<ProductDto> = emptyList())
@Serializable data class PlnCustomerDto(val meterNumber: String, val name: String, val tariff: String? = null)
@Serializable data class BillInquiryDto(
    val biller: String, val billerLabel: String? = null, val customerNumber: String, val customerName: String,
    val period: String? = null, val amount: Long, val fee: Long, val total: Long
)
@Serializable data class OrderRequest(
    val kind: String, val phone: String? = null, val nominal: Long? = null, val productId: String? = null,
    val meterNumber: String? = null, val biller: String? = null, val customerNumber: String? = null,
    val wallet: String? = null, val amount: Long? = null
)
@Serializable data class ExecuteRequest(
    val kind: String, val phone: String? = null, val nominal: Long? = null, val productId: String? = null,
    val meterNumber: String? = null, val biller: String? = null, val customerNumber: String? = null,
    val wallet: String? = null, val amount: Long? = null, val pin: String
)
@Serializable data class OrderDto(
    val kind: String, val title: String, val target: String, val targetName: String? = null, val amount: Long,
    val fee: Long, val total: Long, val details: Map<String, String>? = null, val billKey: String? = null
)

// ------------------------------------------------------------------ Virtual Card
@Serializable data class CardDto(
    val id: String, val cardholderName: String, val maskedNumber: String, val last4: String, val expiry: String,
    val status: String, val usageScope: String, val transactionLimit: Long, val createdAt: String
)
@Serializable data class CardDetailsDto(
    val id: String, val cardholderName: String, val cardNumber: String, val expiry: String, val cvv: String,
    val status: String, val usageScope: String, val transactionLimit: Long
)
@Serializable data class CreateCardRequest(val usageScope: String)
@Serializable data class LimitRequest(val limit: Long)
@Serializable data class PurchaseRequest(val amount: Long, val international: Boolean, val merchant: String?)
