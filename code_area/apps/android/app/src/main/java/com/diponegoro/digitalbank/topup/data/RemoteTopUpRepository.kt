package com.diponegoro.digitalbank.topup.data

import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.network.ApiClient
import com.diponegoro.digitalbank.core.network.ExecuteRequest
import com.diponegoro.digitalbank.core.network.OrderDto
import com.diponegoro.digitalbank.core.network.OrderRequest
import com.diponegoro.digitalbank.core.network.idemKey
import com.diponegoro.digitalbank.core.network.toLedger
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.core.util.map
import com.diponegoro.digitalbank.core.util.parseRupiah
import com.diponegoro.digitalbank.topup.domain.BillInquiry
import com.diponegoro.digitalbank.topup.domain.Biller
import com.diponegoro.digitalbank.topup.domain.EWallet
import com.diponegoro.digitalbank.topup.domain.PlnCustomer
import com.diponegoro.digitalbank.topup.domain.Product
import com.diponegoro.digitalbank.topup.domain.TopUpKind
import com.diponegoro.digitalbank.topup.domain.TopUpOrder
import java.util.concurrent.ConcurrentHashMap

/**
 * Top-up lewat backend. Server selalu menyusun ulang pesanan dari parameter (bukan dari nominal
 * kiriman klien), jadi parameter quote disimpan per pesanan dan dikirim ulang saat bayar.
 */
class RemoteTopUpRepository(
    private val client: ApiClient,
    private val onDataChanged: () -> Unit = {}
) : TopUpRepository {

    private val requests = ConcurrentHashMap<TopUpOrder, OrderRequest>()

    private fun OrderDto.toModel() = TopUpOrder(
        kind = runCatching { TopUpKind.valueOf(kind) }.getOrDefault(TopUpKind.PULSA),
        title = title, target = target, targetName = targetName.orEmpty(), amount = amount, fee = fee,
        details = details.orEmpty(), billKey = billKey
    )

    private suspend fun quote(req: OrderRequest): AppResult<TopUpOrder> =
        client.call { topUpQuote(req) }.map { it.toModel().also { order -> requests[order] = req } }

    override suspend fun orderPulsa(phone: String, nominal: Long) = quote(OrderRequest(kind = "PULSA", phone = phone, nominal = nominal))

    override suspend fun orderData(phone: String, product: Product) = quote(OrderRequest(kind = "DATA", phone = phone, productId = product.id))

    override suspend fun inquirePln(rawMeter: String): AppResult<PlnCustomer> =
        client.call { plnInquiry(rawMeter.filter { it.isDigit() }) }.map { PlnCustomer(it.meterNumber, it.name, it.tariff.orEmpty()) }

    override suspend fun orderPln(customer: PlnCustomer, nominal: Long) =
        quote(OrderRequest(kind = "PLN", meterNumber = customer.meterNumber, nominal = nominal))

    override suspend fun inquireBill(biller: Biller, rawNumber: String): AppResult<BillInquiry> =
        client.call { billInquiry(biller.name, rawNumber.filter { it.isDigit() }) }.map {
            BillInquiry(biller, it.customerNumber, it.customerName, it.period.orEmpty(), it.amount, it.fee)
        }

    override suspend fun orderBill(inquiry: BillInquiry) =
        quote(OrderRequest(kind = "BILL", biller = inquiry.biller.name, customerNumber = inquiry.customerNumber))

    override suspend fun orderEwallet(wallet: EWallet, phone: String, rawAmount: String): AppResult<TopUpOrder> {
        val amount = parseRupiah(rawAmount) ?: return AppResult.Failure("Nominal wajib diisi")
        return quote(OrderRequest(kind = "EWALLET", phone = phone, wallet = wallet.name, amount = amount))
    }

    override suspend fun execute(userId: String, order: TopUpOrder, pin: String, idempotencyKey: String): AppResult<LedgerTransaction> {
        val q = requests[order] ?: return AppResult.Failure("Pesanan kedaluwarsa. Silakan ulangi.")
        val req = ExecuteRequest(q.kind, q.phone, q.nominal, q.productId, q.meterNumber, q.biller, q.customerNumber, q.wallet, q.amount, pin)
        return client.call { topUpOrder(idemKey(idempotencyKey), req) }.map { it.toLedger(userId) }
            .also { if (it is AppResult.Success) onDataChanged() }
    }
}
