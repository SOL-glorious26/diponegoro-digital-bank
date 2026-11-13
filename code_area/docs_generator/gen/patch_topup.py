import sys, subprocess
root = sys.argv[1]; gen = sys.argv[2]
P = lambda p: root + '/' + p
subprocess.check_call([sys.executable, '-I', gen + '/localize.py', P('topup/data/TopUpRepository.kt'), 'TopUpRepository',
                       'orderPulsa,orderData,inquirePln,orderPln,inquireBill,orderBill,orderEwallet,execute'])
lp = P('topup/data/LocalTopUpRepository.kt')
s = open(lp, encoding='utf8').read()
for a, b in [
    ('override suspend fun orderBill(inquiry: BillInquiry): TopUpOrder = TopUpOrder(',
     'override suspend fun orderBill(inquiry: BillInquiry): AppResult<TopUpOrder> = AppResult.Success(TopUpOrder('),
    ('        billKey = billKey(inquiry.biller, inquiry.customerNumber, inquiry.period)\n    )',
     '        billKey = billKey(inquiry.biller, inquiry.customerNumber, inquiry.period)\n    ))'),
]:
    assert a in s, a[:60]; s = s.replace(a, b)
open(lp, 'w', encoding='utf8', newline='\n').write(s)

open(P('topup/data/TopUpRepository.kt'), 'w', encoding='utf8', newline='\n').write('''package com.diponegoro.digitalbank.topup.data

import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.topup.domain.BillInquiry
import com.diponegoro.digitalbank.topup.domain.Biller
import com.diponegoro.digitalbank.topup.domain.EWallet
import com.diponegoro.digitalbank.topup.domain.PlnCustomer
import com.diponegoro.digitalbank.topup.domain.Product
import com.diponegoro.digitalbank.topup.domain.TopUpOrder

/**
 * Modul Top-up (FR-18 – FR-20). Implementasi:
 *  - [LocalTopUpRepository]  : simulator penyedia + ledger in-memory (varian demo)
 *  - [RemoteTopUpRepository] : backend /api/v1/topup (varian online)
 */
interface TopUpRepository {
    suspend fun orderPulsa(phone: String, nominal: Long): AppResult<TopUpOrder>
    suspend fun orderData(phone: String, product: Product): AppResult<TopUpOrder>
    suspend fun inquirePln(rawMeter: String): AppResult<PlnCustomer>
    suspend fun orderPln(customer: PlnCustomer, nominal: Long): AppResult<TopUpOrder>
    suspend fun inquireBill(biller: Biller, rawNumber: String): AppResult<BillInquiry>
    suspend fun orderBill(inquiry: BillInquiry): AppResult<TopUpOrder>
    suspend fun orderEwallet(wallet: EWallet, phone: String, rawAmount: String): AppResult<TopUpOrder>
    suspend fun execute(userId: String, order: TopUpOrder, pin: String, idempotencyKey: String): AppResult<LedgerTransaction>
}
''')

open(P('topup/data/RemoteTopUpRepository.kt'), 'w', encoding='utf8', newline='\n').write('''package com.diponegoro.digitalbank.topup.data

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
''')

vp = P('topup/presentation/TopUpViewModel.kt')
s = open(vp, encoding='utf8').read()
pairs = [
    ('''    fun orderPulsa(phone: String, nominal: Long) = setOrder(repo.orderPulsa(phone, nominal))
    fun orderData(phone: String, product: Product) = setOrder(repo.orderData(phone, product))
    fun orderEwallet(wallet: EWallet, phone: String, amount: String) = setOrder(repo.orderEwallet(wallet, phone, amount))

    fun inquirePln(meter: String) = when (val r = repo.inquirePln(meter)) {
        is AppResult.Failure -> _state.update { it.copy(error = r.message, plnCustomer = null) }
        is AppResult.Success -> _state.update { it.copy(plnCustomer = r.data, error = null) }
    }

    fun orderPln(nominal: Long) {
        val customer = _state.value.plnCustomer ?: return
        setOrder(repo.orderPln(customer, nominal))
    }

    fun inquireBill(biller: Biller, number: String) = when (val r = repo.inquireBill(biller, number)) {
        is AppResult.Failure -> _state.update { it.copy(error = r.message, bill = null) }
        is AppResult.Success -> _state.update { it.copy(bill = r.data, error = null) }
    }

    fun orderBill() {
        val bill = _state.value.bill ?: return
        setOrder(AppResult.Success(repo.orderBill(bill)))
    }''', '''    private fun quote(block: suspend () -> AppResult<TopUpOrder>) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            val r = block()
            _state.update { it.copy(busy = false) }
            setOrder(r)
        }
    }

    fun orderPulsa(phone: String, nominal: Long) = quote { repo.orderPulsa(phone, nominal) }
    fun orderData(phone: String, product: Product) = quote { repo.orderData(phone, product) }
    fun orderEwallet(wallet: EWallet, phone: String, amount: String) = quote { repo.orderEwallet(wallet, phone, amount) }

    fun inquirePln(meter: String) {
        viewModelScope.launch {
            when (val r = repo.inquirePln(meter)) {
                is AppResult.Failure -> _state.update { it.copy(error = r.message, plnCustomer = null) }
                is AppResult.Success -> _state.update { it.copy(plnCustomer = r.data, error = null) }
            }
        }
    }

    fun orderPln(nominal: Long) {
        val customer = _state.value.plnCustomer ?: return
        quote { repo.orderPln(customer, nominal) }
    }

    fun inquireBill(biller: Biller, number: String) {
        viewModelScope.launch {
            when (val r = repo.inquireBill(biller, number)) {
                is AppResult.Failure -> _state.update { it.copy(error = r.message, bill = null) }
                is AppResult.Success -> _state.update { it.copy(bill = r.data, error = null) }
            }
        }
    }

    fun orderBill() {
        val bill = _state.value.bill ?: return
        quote { repo.orderBill(bill) }
    }'''),
]
for a, b in pairs:
    assert a in s, a[:90]
    s = s.replace(a, b)
open(vp, 'w', encoding='utf8', newline='\n').write(s)
print('ok')
