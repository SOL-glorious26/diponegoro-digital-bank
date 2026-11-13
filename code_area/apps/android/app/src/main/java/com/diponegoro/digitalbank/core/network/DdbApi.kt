package com.diponegoro.digitalbank.core.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Kontrak REST backend (base URL berakhir "/", path relatif "api/v1/..."). */
interface DdbApi {
    // ---------------------------------------------------------------- Authentication (FR-01 – FR-05)
    @POST("api/v1/auth/register") suspend fun register(@Body r: RegisterRequest): RegisterResponse
    @POST("api/v1/auth/register/verify-otp") suspend fun verifyRegistration(@Body r: VerifyRegistrationRequest): AuthTokens
    @POST("api/v1/auth/register/resend-otp") suspend fun resendRegistrationOtp(@Body r: ResendOtpRequest): OtpInfo
    @POST("api/v1/auth/login") suspend fun login(@Body r: LoginRequest): AuthTokens
    @POST("api/v1/auth/biometric-login") suspend fun biometricLogin(@Body r: BiometricLoginRequest): AuthTokens
    @POST("api/v1/auth/biometric/enroll") suspend fun enrollBiometric(@Body r: BiometricEnrollRequest): BiometricEnrollment
    @POST("api/v1/auth/biometric/disable") suspend fun disableBiometric(): MessageResponse
    @POST("api/v1/auth/refresh") suspend fun refresh(@Body r: RefreshRequest): AuthTokens
    @POST("api/v1/auth/ekyc") suspend fun submitEkyc(@Body r: EkycRequest): EkycDto
    @GET("api/v1/auth/ekyc") suspend fun ekyc(): EkycDto
    @POST("api/v1/auth/pin") suspend fun createPin(@Body r: PinRequest): MessageResponse
    @POST("api/v1/auth/pin/verify") suspend fun verifyPin(@Body r: PinRequest): MessageResponse
    @GET("api/v1/auth/sessions") suspend fun sessions(): List<SessionDto>
    @POST("api/v1/auth/logout") suspend fun logout(): MessageResponse
    @POST("api/v1/auth/logout-all") suspend fun logoutAll(): MessageResponse
    @POST("api/v1/auth/recovery/request") suspend fun startRecovery(@Body r: RecoveryRequest): RecoveryStarted
    @POST("api/v1/auth/recovery/verify") suspend fun verifyRecovery(@Body r: RecoveryVerifyRequest): RecoveryVerified
    @POST("api/v1/auth/recovery/reset") suspend fun resetRecovery(@Body r: RecoveryResetRequest): MessageResponse

    // ---------------------------------------------------------------- Profile & Notification (FR-27 – FR-29)
    @GET("api/v1/me") suspend fun me(): UserDto
    @POST("api/v1/me/profile-change") suspend fun requestProfileChange(@Body r: ProfileDraftDto): OtpInfo
    @POST("api/v1/me/profile-change/confirm") suspend fun confirmProfileChange(@Body r: OtpCodeRequest): UserDto
    @PUT("api/v1/me/photo") suspend fun updatePhoto(@Body r: PhotoRequest): UserDto
    @POST("api/v1/me/password") suspend fun changePassword(@Body r: PasswordChange): MessageResponse
    @POST("api/v1/me/pin") suspend fun changePin(@Body r: PinChange): MessageResponse
    @GET("api/v1/me/notification-settings") suspend fun notificationSettings(): SettingsDto
    @PUT("api/v1/me/notification-settings") suspend fun updateNotificationSettings(@Body r: SettingsDto): SettingsDto
    @GET("api/v1/me/notifications") suspend fun notifications(@Query("page") page: Int = 0): NotificationPage
    @GET("api/v1/me/notifications/push") suspend fun pendingPush(): List<NotificationDto>
    @POST("api/v1/me/notifications/{id}/read") suspend fun markRead(@Path("id") id: String): MessageResponse
    @POST("api/v1/me/notifications/read-all") suspend fun markAllRead(): MessageResponse
    @GET("api/v1/me/emails") suspend fun emails(): List<EmailDto>

    // ---------------------------------------------------------------- Account (FR-06 – FR-09)
    @GET("api/v1/accounts/me") suspend fun account(): AccountOverviewDto
    @PUT("api/v1/accounts/me/balance-visibility") suspend fun setBalanceVisibility(@Body r: BalanceVisibilityRequest): AccountOverviewDto
    @GET("api/v1/accounts/me/balance-chart") suspend fun balanceChart(@Query("period") period: String): BalanceChartDto
    @GET("api/v1/accounts/pockets") suspend fun pockets(): List<PocketDto>
    @POST("api/v1/accounts/pockets") suspend fun createPocket(@Body r: CreatePocketRequest): PocketDto
    @PUT("api/v1/accounts/pockets/{id}") suspend fun updatePocket(@Path("id") id: String, @Body r: UpdatePocketRequest): PocketDto
    @DELETE("api/v1/accounts/pockets/{id}") suspend fun deletePocket(@Path("id") id: String, @Header("Idempotency-Key") key: String): retrofit2.Response<Unit>
    @POST("api/v1/accounts/pockets/{id}/lock") suspend fun lockPocket(@Path("id") id: String, @Body r: LockPocketRequest): PocketDto
    @POST("api/v1/accounts/pockets/{id}/deposit") suspend fun deposit(@Path("id") id: String, @Header("Idempotency-Key") key: String, @Body r: MoveRequest): PocketDto
    @POST("api/v1/accounts/pockets/{id}/withdraw") suspend fun withdraw(@Path("id") id: String, @Header("Idempotency-Key") key: String, @Body r: WithdrawRequest): PocketDto

    // ---------------------------------------------------------------- Transfer (FR-10 – FR-14)
    @GET("api/v1/transfers/banks") suspend fun banks(): List<BankDto>
    @POST("api/v1/transfers/inquiry") suspend fun inquiry(@Body r: InquiryRequest): RecipientDto
    @POST("api/v1/transfers/quote") suspend fun quote(@Body r: QuoteRequest): QuoteDto
    @POST("api/v1/transfers") suspend fun transfer(@Header("Idempotency-Key") key: String, @Body r: TransferRequest): TransactionDto
    @POST("api/v1/transfers/{txId}/check-status") suspend fun checkStatus(@Path("txId") txId: String): TransactionDto
    @GET("api/v1/transfers/favorites") suspend fun favorites(): List<FavoriteDto>
    @POST("api/v1/transfers/favorites") suspend fun addFavorite(@Body r: FavoriteRequest): FavoriteDto
    @DELETE("api/v1/transfers/favorites/{id}") suspend fun removeFavorite(@Path("id") id: String): retrofit2.Response<Unit>

    // ---------------------------------------------------------------- Payment QRIS (FR-15 – FR-17)
    @POST("api/v1/qris/scan") suspend fun scanQris(@Body r: ScanRequest): ScanResultDto
    @POST("api/v1/qris/pay") suspend fun payQris(@Header("Idempotency-Key") key: String, @Body r: PayRequest): TransactionDto
    @GET("api/v1/qris/my-qr") suspend fun myQr(@Query("amount") amount: Long? = null): MyQrDto
    @GET("api/v1/qris/demo") suspend fun demoQrs(): List<DemoQrDto>
    @POST("api/v1/qris/simulate-incoming") suspend fun simulateIncoming(@Header("Idempotency-Key") key: String, @Body r: IncomingRequest): TransactionDto

    // ---------------------------------------------------------------- Top-up (FR-18 – FR-20)
    @GET("api/v1/topup/data-packages") suspend fun dataPackages(@Query("phone") phone: String): DataPackagesDto
    @GET("api/v1/topup/pln/inquiry") suspend fun plnInquiry(@Query("meterNumber") meter: String): PlnCustomerDto
    @GET("api/v1/topup/bills/inquiry") suspend fun billInquiry(@Query("biller") biller: String, @Query("customerNumber") number: String): BillInquiryDto
    @POST("api/v1/topup/quote") suspend fun topUpQuote(@Body r: OrderRequest): OrderDto
    @POST("api/v1/topup/orders") suspend fun topUpOrder(@Header("Idempotency-Key") key: String, @Body r: ExecuteRequest): TransactionDto

    // ---------------------------------------------------------------- Virtual Card (FR-21 – FR-22)
    @GET("api/v1/cards") suspend fun cards(): List<CardDto>
    @POST("api/v1/cards") suspend fun createCard(@Body r: CreateCardRequest): CardDto
    @POST("api/v1/cards/{id}/details") suspend fun cardDetails(@Path("id") id: String, @Body r: PinRequest): CardDetailsDto
    @POST("api/v1/cards/{id}/freeze") suspend fun freezeCard(@Path("id") id: String): CardDto
    @POST("api/v1/cards/{id}/activate") suspend fun activateCard(@Path("id") id: String): CardDto
    @PUT("api/v1/cards/{id}/limit") suspend fun cardLimit(@Path("id") id: String, @Body r: LimitRequest): CardDto
    @POST("api/v1/cards/{id}/simulate-purchase") suspend fun simulatePurchase(@Path("id") id: String, @Header("Idempotency-Key") key: String, @Body r: PurchaseRequest): TransactionDto

    // ---------------------------------------------------------------- History & Dashboard (FR-23 – FR-25)
    @GET("api/v1/history") suspend fun history(
        @Query("range") range: String? = null, @Query("category") category: String? = null,
        @Query("direction") direction: String? = null, @Query("status") status: String? = null,
        @Query("q") q: String? = null, @Query("page") page: Int = 0, @Query("size") size: Int = 20
    ): HistoryPageDto
    @GET("api/v1/history/{txId}") suspend fun transactionDetail(@Path("txId") txId: String): TransactionDetailDto
    @GET("api/v1/history/monthly-summary") suspend fun monthlySummary(@Query("months") months: Int, @Query("endMonthsBack") endMonthsBack: Int = 0): List<MonthSummaryDto>
}

