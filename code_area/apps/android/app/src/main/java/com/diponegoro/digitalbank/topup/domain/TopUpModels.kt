package com.diponegoro.digitalbank.topup.domain

enum class TopUpKind(val label: String) {
    PULSA("Pulsa"),
    DATA("Paket Data"),
    PLN("Token Listrik"),
    BILL("Tagihan"),
    EWALLET("Top-up E-Wallet")
}

data class Operator(val code: String, val name: String, val prefixes: List<String>)

data class Product(val id: String, val name: String, val description: String, val price: Long)

data class PlnCustomer(val meterNumber: String, val name: String, val tariff: String)

enum class Biller(val label: String, val numberHint: String, val numberLength: IntRange) {
    PDAM("Air PDAM", "Nomor pelanggan PDAM (8–12 digit)", 8..12),
    BPJS("BPJS Kesehatan", "Nomor VA BPJS (13–16 digit)", 13..16),
    INTERNET("Internet Rumah", "Nomor pelanggan (10–12 digit)", 10..12)
}

data class BillInquiry(
    val biller: Biller,
    val customerNumber: String,
    val customerName: String,
    val period: String,
    val amount: Long,
    val fee: Long
)

enum class EWallet(val label: String) { GOPAY("GoPay"), OVO("OVO"), DANA("DANA"), SHOPEEPAY("ShopeePay") }

/** Ringkasan pesanan yang ditampilkan di layar konfirmasi sebelum PIN. */
data class TopUpOrder(
    val kind: TopUpKind,
    val title: String,
    val target: String,
    val targetName: String,
    val amount: Long,
    val fee: Long,
    val details: Map<String, String>,
    /** Kunci untuk mencegah tagihan periode yang sama dibayar dua kali. */
    val billKey: String? = null
) {
    val total: Long get() = amount + fee
}

object TopUpCatalog {
    val operators = listOf(
        Operator("TSEL", "Telkomsel", listOf("0811", "0812", "0813", "0821", "0822", "0823", "0851", "0852", "0853")),
        Operator("ISAT", "Indosat Ooredoo", listOf("0814", "0815", "0816", "0855", "0856", "0857", "0858")),
        Operator("XL", "XL", listOf("0817", "0818", "0819", "0859", "0877", "0878")),
        Operator("AXIS", "AXIS", listOf("0831", "0832", "0833", "0838")),
        Operator("TRI", "Tri", listOf("0895", "0896", "0897", "0898", "0899")),
        Operator("SF", "Smartfren", listOf("0881", "0882", "0883", "0884", "0885", "0886", "0887", "0888", "0889"))
    )

    const val PULSA_FEE = 1_500L
    const val PLN_ADMIN_FEE = 2_500L
    const val BILL_ADMIN_FEE = 2_500L
    const val EWALLET_FEE = 1_000L
    /** Tarif simulasi R1/1300VA per kWh. */
    const val PLN_RATE_PER_KWH = 1_444.70

    val pulsaNominals = listOf(5_000L, 10_000L, 20_000L, 25_000L, 50_000L, 100_000L, 200_000L)
    val plnNominals = listOf(20_000L, 50_000L, 100_000L, 200_000L, 500_000L, 1_000_000L)
    val ewalletRange = 10_000L..2_000_000L

    fun dataPackages(operator: Operator): List<Product> = listOf(
        Product("${operator.code}-D1", "Harian 1 GB", "Berlaku 1 hari", 5_000),
        Product("${operator.code}-W5", "Mingguan 5 GB", "Berlaku 7 hari", 25_000),
        Product("${operator.code}-M15", "Bulanan 15 GB", "Berlaku 30 hari", 75_000),
        Product("${operator.code}-M30", "Bulanan 30 GB", "Berlaku 30 hari", 110_000)
    )

    private val phonePattern = Regex("^08[0-9]{8,11}$")

    fun isValidPhone(phone: String) = phonePattern.matches(phone)

    fun detectOperator(phone: String): Operator? =
        if (phone.length < 4) null else operators.firstOrNull { op -> op.prefixes.any { phone.startsWith(it) } }
}
