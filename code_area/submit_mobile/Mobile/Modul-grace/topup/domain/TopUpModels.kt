package com.diponegoro.digitalbank.topup.domain

enum class TopUpStatus(val label: String) {
    PENDING("Pending"), BERHASIL("Berhasil"), GAGAL("Gagal"), DIBATALKAN("Dibatalkan"), KADALUWARSA("Kedaluwarsa")
}
enum class PaymentMethodType(val label: String) {
    VIRTUAL_ACCOUNT("Virtual account demo"), E_WALLET("E-wallet demo"), KARTU_DEBIT("Kartu debit"),
    KARTU_KREDIT("Kartu kredit"), SALDO_AKUN("Saldo rekening")
}
enum class PaymentMethodStatus { AKTIF, TIDAK_AKTIF, KADALUWARSA, DIBLOKIR }
enum class ServiceCategory { E_WALLET, PULSA, PAKET_DATA, PLN, AIR_PDAM, E_MONEY, LAINNYA }
enum class ServiceStatus { AKTIF, TIDAK_AKTIF, MAINTENANCE }
enum class MoneyDirection { CREDIT, DEBIT }

data class TopUpService(
    val id: String, val name: String, val category: ServiceCategory, val provider: String,
    val minimum: Long, val maximum: Long, val fee: Long = 0,
    val status: ServiceStatus = ServiceStatus.AKTIF, val direction: MoneyDirection = MoneyDirection.DEBIT
)
data class PaymentMethod(val id: String, val type: PaymentMethodType, val name: String,
                         val identifier: String, val status: PaymentMethodStatus, val isDefault: Boolean)
data class TopUpDraft(val accountId: String, val serviceId: String, val target: String,
                      val amount: Long, val methodId: String, val operationId: String)
data class TopUpQuote(val draft: TopUpDraft, val service: TopUpService, val accountNumber: String,
                      val methodName: String, val fee: Long, val total: Long)
data class TopUpTransaction(
    val id: String, val operationId: String, val accountId: String, val accountNumber: String,
    val reference: String, val createdAt: Long, val expiresAt: Long, val serviceName: String,
    val target: String, val methodName: String, val amount: Long, val fee: Long,
    val direction: MoneyDirection, val status: TopUpStatus, val note: String, val balanceAfter: Long?,
    val completedAt: Long? = null
) { val total: Long get() = Math.addExact(amount, fee) }
data class TopUpNotification(val id: String, val transactionId: String, val message: String, val createdAt: Long)
data class TopUpSnapshot(val methods: List<PaymentMethod>, val history: List<TopUpTransaction>,
                         val notifications: List<TopUpNotification>, val dailyLimit: Long, val usedToday: Long)

object TopUpCatalog {
    val services = listOf(
        TopUpService("bank", "Isi saldo rekening demo", ServiceCategory.LAINNYA, "Digital Bank Demo", 10_000, 2_000_000, direction = MoneyDirection.CREDIT),
        TopUpService("gopay", "GoPay demo", ServiceCategory.E_WALLET, "Provider demo", 10_000, 1_000_000),
        TopUpService("ovo", "OVO demo", ServiceCategory.E_WALLET, "Provider demo", 10_000, 1_000_000),
        TopUpService("pulsa", "Pulsa demo", ServiceCategory.PULSA, "Operator demo", 10_000, 500_000),
        TopUpService("data", "Paket data demo", ServiceCategory.PAKET_DATA, "Operator demo", 10_000, 500_000),
        TopUpService("pln", "Token PLN demo", ServiceCategory.PLN, "PLN demo", 20_000, 1_000_000),
        TopUpService("pdam", "Air PDAM demo", ServiceCategory.AIR_PDAM, "PDAM demo", 10_000, 1_000_000),
        TopUpService("emoney", "E-money demo", ServiceCategory.E_MONEY, "Provider demo", 10_000, 1_000_000)
    )
    fun service(id: String) = services.firstOrNull { it.id == id } ?: error("Layanan tidak tersedia.")
    fun validateTarget(service: TopUpService, target: String) {
        val pattern = when (service.category) {
            ServiceCategory.E_WALLET, ServiceCategory.PULSA, ServiceCategory.PAKET_DATA -> Regex("08[0-9]{8,11}")
            ServiceCategory.PLN -> Regex("[0-9]{11,12}")
            ServiceCategory.E_MONEY -> Regex("[0-9]{16}")
            else -> Regex("[0-9]{6,24}")
        }
        require(pattern.matches(target)) { "Nomor tujuan tidak sesuai format layanan (ponsel 08…, PLN 11–12 digit, e-money 16 digit, lainnya 6–24 digit)." }
    }
}
