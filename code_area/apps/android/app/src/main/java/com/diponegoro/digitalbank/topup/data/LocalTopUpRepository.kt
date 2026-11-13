package com.diponegoro.digitalbank.topup.data

import com.diponegoro.digitalbank.core.ledger.ExternalAccounts
import com.diponegoro.digitalbank.core.ledger.Ledger
import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.ledger.PostingRequest
import com.diponegoro.digitalbank.core.ledger.TxCategory
import com.diponegoro.digitalbank.core.security.TransactionGuard
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.core.util.ID_LOCALE
import com.diponegoro.digitalbank.core.util.formatRupiah
import com.diponegoro.digitalbank.core.util.parseRupiah
import com.diponegoro.digitalbank.topup.domain.BillInquiry
import com.diponegoro.digitalbank.topup.domain.Biller
import com.diponegoro.digitalbank.topup.domain.EWallet
import com.diponegoro.digitalbank.topup.domain.PlnCustomer
import com.diponegoro.digitalbank.topup.domain.Product
import com.diponegoro.digitalbank.topup.domain.TopUpCatalog
import com.diponegoro.digitalbank.topup.domain.TopUpKind
import com.diponegoro.digitalbank.topup.domain.TopUpOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Modul Top-up (FR-18 – FR-20). Penyedia pulsa, PLN, dan biller diganti simulator
 * (SRS 2.5). Hasil inquiry dibuat deterministik dari nomor pelanggan agar mudah diuji.
 */
class LocalTopUpRepository(
    private val ledger: Ledger,
    private val guard: TransactionGuard,
    private val clock: () -> Long = System::currentTimeMillis
) : TopUpRepository {
    private val paidBills = ConcurrentHashMap.newKeySet<String>()

    private val names = listOf("SRI WAHYUNI", "AGUS SETIAWAN", "DEWI KARTIKA", "HENDRA GUNAWAN", "LINA MARLINA", "RUDI HARTONO")

    private fun seed(number: String) = number.hashCode().mod(1_000_003)

    // ---------------------------------------------------------------- FR-18

    override suspend fun orderPulsa(phone: String, nominal: Long): AppResult<TopUpOrder> {
        val operator = validatePhone(phone) ?: return AppResult.Failure(phoneError(phone))
        if (nominal !in TopUpCatalog.pulsaNominals) return AppResult.Failure("Pilih nominal pulsa")
        return AppResult.Success(
            TopUpOrder(
                TopUpKind.PULSA, "Pulsa ${operator.name} ${formatRupiah(nominal)}", phone, operator.name,
                nominal, TopUpCatalog.PULSA_FEE,
                mapOf("Operator" to operator.name, "Nomor" to phone, "Nominal pulsa" to formatRupiah(nominal))
            )
        )
    }

    override suspend fun orderData(phone: String, product: Product): AppResult<TopUpOrder> {
        val operator = validatePhone(phone) ?: return AppResult.Failure(phoneError(phone))
        if (TopUpCatalog.dataPackages(operator).none { it.id == product.id })
            return AppResult.Failure("Paket tidak tersedia untuk operator ${operator.name}")
        return AppResult.Success(
            TopUpOrder(
                TopUpKind.DATA, "Paket data ${operator.name} ${product.name}", phone, operator.name,
                product.price, TopUpCatalog.PULSA_FEE,
                mapOf("Operator" to operator.name, "Nomor" to phone, "Paket" to "${product.name} (${product.description})")
            )
        )
    }

    // ---------------------------------------------------------------- FR-19

    override suspend fun inquirePln(rawMeter: String): AppResult<PlnCustomer> {
        val meter = rawMeter.filter { it.isDigit() }
        if (meter.length !in 11..12) return AppResult.Failure("ID pelanggan / nomor meter harus 11–12 digit")
        if (meter.endsWith("0000")) return AppResult.Failure("ID pelanggan tidak ditemukan")
        val tariffs = listOf("R1/900VA", "R1/1300VA", "R1/2200VA", "R2/3500VA")
        return AppResult.Success(
            PlnCustomer(meter, names[seed(meter) % names.size], tariffs[seed(meter) % tariffs.size])
        )
    }

    override suspend fun orderPln(customer: PlnCustomer, nominal: Long): AppResult<TopUpOrder> {
        if (nominal !in TopUpCatalog.plnNominals) return AppResult.Failure("Pilih nominal token")
        return AppResult.Success(
            TopUpOrder(
                TopUpKind.PLN, "Token listrik ${formatRupiah(nominal)}", customer.meterNumber, customer.name,
                nominal, TopUpCatalog.PLN_ADMIN_FEE,
                mapOf("ID pelanggan" to customer.meterNumber, "Nama" to customer.name, "Tarif/Daya" to customer.tariff)
            )
        )
    }

    // ---------------------------------------------------------------- FR-20

    override suspend fun inquireBill(biller: Biller, rawNumber: String): AppResult<BillInquiry> {
        val number = rawNumber.filter { it.isDigit() }
        if (number.length !in biller.numberLength) return AppResult.Failure(biller.numberHint)
        if (number.endsWith("0000")) return AppResult.Failure("Nomor pelanggan tidak ditemukan")
        val period = SimpleDateFormat("MMMM yyyy", ID_LOCALE).format(Date(clock()))
        if (billKey(biller, number, period) in paidBills)
            return AppResult.Failure("Tagihan ${biller.label} periode $period sudah lunas")
        val amount = when (biller) {
            Biller.PDAM -> 45_000L + (seed(number) % 20) * 7_500
            Biller.BPJS -> listOf(42_000L, 100_000L, 150_000L)[seed(number) % 3]
            Biller.INTERNET -> listOf(250_000L, 330_000L, 385_000L, 450_000L)[seed(number) % 4]
        }
        return AppResult.Success(
            BillInquiry(biller, number, names[seed(number) % names.size], period, amount, TopUpCatalog.BILL_ADMIN_FEE)
        )
    }

    override suspend fun orderBill(inquiry: BillInquiry): AppResult<TopUpOrder> = AppResult.Success(TopUpOrder(
        TopUpKind.BILL, "Tagihan ${inquiry.biller.label}", inquiry.customerNumber, inquiry.customerName,
        inquiry.amount, inquiry.fee,
        mapOf(
            "Layanan" to inquiry.biller.label,
            "Nomor pelanggan" to inquiry.customerNumber,
            "Nama" to inquiry.customerName,
            "Periode" to inquiry.period
        ),
        billKey = billKey(inquiry.biller, inquiry.customerNumber, inquiry.period)
    ))

    override suspend fun orderEwallet(wallet: EWallet, phone: String, rawAmount: String): AppResult<TopUpOrder> {
        if (!TopUpCatalog.isValidPhone(phone)) return AppResult.Failure("Nomor HP harus diawali 08 dan 10–13 digit")
        val amount = parseRupiah(rawAmount) ?: return AppResult.Failure("Nominal wajib diisi")
        val range = TopUpCatalog.ewalletRange
        if (amount !in range)
            return AppResult.Failure("Nominal ${formatRupiah(range.first)} – ${formatRupiah(range.last)}")
        return AppResult.Success(
            TopUpOrder(
                TopUpKind.EWALLET, "Top-up ${wallet.label} ${formatRupiah(amount)}", phone,
                "${wallet.label} ${names[seed(phone) % names.size]}", amount, TopUpCatalog.EWALLET_FEE,
                mapOf("E-wallet" to wallet.label, "Nomor" to phone)
            )
        )
    }

    // ---------------------------------------------------------------- eksekusi

    /** Saldo terdebet hanya setelah PIN valid (FR-18); bukti tersimpan di riwayat (FR-20). */
    override suspend fun execute(
        userId: String, order: TopUpOrder, pin: String, idempotencyKey: String
    ): AppResult<LedgerTransaction> {
        ledger.findByIdempotencyKey(idempotencyKey)?.let { return AppResult.Success(it) }
        if (order.billKey != null && order.billKey in paidBills)
            return AppResult.Failure("Tagihan periode ini sudah dibayar")
        val main = ledger.mainAccount(userId) ?: return AppResult.Failure("Rekening tidak ditemukan")
        if (ledger.balance(main.id) < order.total) return AppResult.Failure("Saldo tidak mencukupi")

        val authorized = guard.authorize(userId, pin)
        if (authorized is AppResult.Failure) return authorized

        val extra = order.details.toMutableMap()
        if (order.kind == TopUpKind.PLN) {
            extra["Token"] = generateToken(idempotencyKey)
            extra["Jumlah kWh"] = String.format(ID_LOCALE, "%.1f kWh", order.amount / TopUpCatalog.PLN_RATE_PER_KWH)
        }
        if (order.kind == TopUpKind.PULSA || order.kind == TopUpKind.DATA) {
            extra["Serial number"] = "SN${abs(idempotencyKey.hashCode()).toString().padStart(10, '0')}"
        }

        val result = ledger.post(
            PostingRequest(
                idempotencyKey = idempotencyKey,
                category = TxCategory.TOPUP,
                title = order.title,
                amount = order.amount,
                fee = order.fee,
                lines = listOf(
                    main.id to -order.total,
                    ExternalAccounts.BILLER to order.amount,
                    ExternalAccounts.FEE_INCOME to order.fee
                ),
                debitUserId = userId,
                creditUserId = null,
                fromName = main.name,
                fromDetail = main.number,
                toName = order.targetName,
                toDetail = "${order.kind.label} • ${order.target}",
                extra = extra
            )
        )
        if (result is AppResult.Success && order.billKey != null) paidBills.add(order.billKey)
        return result
    }

    private fun validatePhone(phone: String) =
        if (TopUpCatalog.isValidPhone(phone)) TopUpCatalog.detectOperator(phone) else null

    private fun phoneError(phone: String) =
        if (!TopUpCatalog.isValidPhone(phone)) "Nomor HP harus diawali 08 dan 10–13 digit"
        else "Operator untuk nomor ini tidak dikenali"

    private fun billKey(biller: Biller, number: String, period: String) = "${biller.name}:$number:$period"

    /** Token 20 digit, deterministik per transaksi agar retry menampilkan token yang sama. */
    private fun generateToken(key: String): String {
        val random = java.util.Random(key.hashCode().toLong())
        return (1..20).joinToString("") { random.nextInt(10).toString() }.chunked(4).joinToString("-")
    }
}
