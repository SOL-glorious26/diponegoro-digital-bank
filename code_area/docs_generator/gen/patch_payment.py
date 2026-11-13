import sys, subprocess
root = sys.argv[1]; gen = sys.argv[2]
P = lambda p: root + '/' + p
subprocess.check_call([sys.executable, '-I', gen + '/localize.py', P('payment/data/PaymentRepository.kt'), 'PaymentRepository',
                       'personalQr,simulateIncoming,scan,pay,demoQrs'])

lp = P('payment/data/LocalPaymentRepository.kt')
s = open(lp, encoding='utf8').read()
start = s.index('/** Hasil pindai yang sudah divalidasi')
end = s.index('/** Modul Payment (QRIS)')
models = s[start:end]
s = s[:start] + s[end:]
s = s.replace('return AppResult.Success(ScannedQris(payload, target.id, target.ownerUserId))',
              'return AppResult.Success(ScannedQris(payload, target.id, target.ownerUserId, raw))')
s = s.replace('return AppResult.Success(ScannedQris(payload, null, null))', 'return AppResult.Success(ScannedQris(payload, null, null, raw))')
open(lp, 'w', encoding='utf8', newline='\n').write(s)

models = models.replace('''    val internalOwnerId: String?
)''', '''    val internalOwnerId: String?,
    /** Teks QR asli (dikirim ke backend pada varian online). */
    val raw: String = ""
)''')
open(P('payment/data/PaymentRepository.kt'), 'w', encoding='utf8', newline='\n').write('''package com.diponegoro.digitalbank.payment.data

import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.payment.domain.QrisPayload

''' + models + '''/**
 * Modul Payment (QRIS) — FR-15, FR-16, FR-17. Implementasi:
 *  - [LocalPaymentRepository]  : ledger in-memory (varian demo)
 *  - [RemotePaymentRepository] : backend /api/v1/qris (varian online)
 */
interface PaymentRepository {
    suspend fun personalQr(userId: String, amount: Long?): AppResult<String>
    suspend fun simulateIncoming(userId: String, rawAmount: String, payerName: String, idempotencyKey: String): AppResult<LedgerTransaction>
    suspend fun scan(userId: String, raw: String): AppResult<ScannedQris>
    suspend fun pay(userId: String, scanned: ScannedQris, rawAmount: String, pin: String, idempotencyKey: String): AppResult<LedgerTransaction>
    suspend fun demoQrs(): List<DemoQr>
}
''')

open(P('payment/data/RemotePaymentRepository.kt'), 'w', encoding='utf8', newline='\n').write('''package com.diponegoro.digitalbank.payment.data

import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.network.ApiClient
import com.diponegoro.digitalbank.core.network.IncomingRequest
import com.diponegoro.digitalbank.core.network.PayRequest
import com.diponegoro.digitalbank.core.network.ScanRequest
import com.diponegoro.digitalbank.core.network.idemKey
import com.diponegoro.digitalbank.core.network.toLedger
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.core.util.map
import com.diponegoro.digitalbank.core.util.parseRupiah
import com.diponegoro.digitalbank.payment.domain.QrisCodec
import com.diponegoro.digitalbank.payment.domain.QrisPayload

/**
 * QRIS lewat backend. Kode didekode juga secara lokal (QrisCodec identik dengan backend) untuk tampilan,
 * tetapi validasi, batas nominal, saldo, PIN, dan posting ledger dijalankan server.
 */
class RemotePaymentRepository(
    private val client: ApiClient,
    private val onDataChanged: () -> Unit = {}
) : PaymentRepository {

    private fun <T> AppResult<T>.changed(): AppResult<T> = also { if (it is AppResult.Success) onDataChanged() }

    override suspend fun personalQr(userId: String, amount: Long?): AppResult<String> =
        client.call { myQr(amount) }.map { it.payload }

    override suspend fun simulateIncoming(userId: String, rawAmount: String, payerName: String, idempotencyKey: String): AppResult<LedgerTransaction> {
        val amount = parseRupiah(rawAmount) ?: return AppResult.Failure("Nominal wajib diisi")
        return client.call { simulateIncoming(idemKey(idempotencyKey), IncomingRequest(amount, payerName.ifBlank { null })) }
            .map { it.toLedger(userId) }.changed()
    }

    override suspend fun scan(userId: String, raw: String): AppResult<ScannedQris> =
        client.call { scanQris(ScanRequest(raw)) }.map { dto ->
            val payload = QrisCodec.decode(raw).getOrElse {
                QrisPayload(dto.issuer.orEmpty(), "", dto.merchantId, dto.merchantName, dto.city.orEmpty(), dto.mcc.orEmpty(), dto.amount)
            }
            ScannedQris(payload, internalAccountId = if (dto.sameBank) "ddb" else null, internalOwnerId = null, raw = raw)
        }

    override suspend fun pay(userId: String, scanned: ScannedQris, rawAmount: String, pin: String, idempotencyKey: String): AppResult<LedgerTransaction> {
        val amount = scanned.payload.amount ?: parseRupiah(rawAmount) ?: return AppResult.Failure("Nominal wajib diisi")
        val raw = scanned.raw.ifEmpty { QrisCodec.encode(scanned.payload) }
        return client.call { payQris(idemKey(idempotencyKey), PayRequest(raw, amount, pin)) }.map { it.toLedger(userId) }.changed()
    }

    override suspend fun demoQrs(): List<DemoQr> =
        when (val r = client.call { demoQrs() }) {
            is AppResult.Success -> r.data.map { DemoQr(it.label, it.payload) }
            is AppResult.Failure -> emptyList()
        }
}
''')

vp = P('payment/presentation/PaymentViewModels.kt')
s = open(vp, encoding='utf8').read()
pairs = [
    ('class QrisPayViewModel(private val userId: String, private val repo: PaymentRepository) : ViewModel() {\n    private val _state = MutableStateFlow(QrisPayUiState(demoQrs = repo.demoQrs()))',
     'class QrisPayViewModel(\n    private val userId: String,\n    private val repo: PaymentRepository,\n    private val accounts: AccountRepository = FeatureModule.accounts\n) : ViewModel() {\n    private val _state = MutableStateFlow(QrisPayUiState())'),
    ('''        viewModelScope.launch {
            CoreModule.ledger.revision.collect {
                val main = CoreModule.ledger.mainAccount(userId)
                _state.update { s -> s.copy(balance = main?.let { CoreModule.ledger.balance(it.id) } ?: 0) }
            }
        }''', '''        viewModelScope.launch {
            val demo = repo.demoQrs()
            _state.update { it.copy(demoQrs = demo) }
        }
        viewModelScope.launch {
            CoreModule.revision.collect {
                val balance = (accounts.overview(userId) as? AppResult.Success)?.data?.balance ?: 0
                _state.update { s -> s.copy(balance = balance) }
            }
        }'''),
    ('''        if (raw.isNullOrBlank()) return _state.update { it.copy(error = "Kode QR tidak terbaca") }
        when (val r = repo.scan(userId, raw)) {
            is AppResult.Failure -> _state.update { it.copy(error = r.message) }
            is AppResult.Success -> {
                idempotencyKey = UUID.randomUUID().toString()
                _state.update {
                    it.copy(scanned = r.data, amount = r.data.payload.amount?.toString() ?: "", error = null, result = null)
                }
            }
        }''', '''        if (raw.isNullOrBlank()) return _state.update { it.copy(error = "Kode QR tidak terbaca") }
        viewModelScope.launch {
            when (val r = repo.scan(userId, raw)) {
                is AppResult.Failure -> _state.update { it.copy(error = r.message) }
                is AppResult.Success -> {
                    idempotencyKey = UUID.randomUUID().toString()
                    _state.update {
                        it.copy(scanned = r.data, amount = r.data.payload.amount?.toString() ?: "", error = null, result = null)
                    }
                }
            }
        }'''),
    ('''    init {
        when (val r = repo.personalQr(userId, null)) {
            is AppResult.Success -> _state.update { it.copy(staticQr = r.data) }
            is AppResult.Failure -> _state.update { it.copy(error = r.message) }
        }
    }''', '''    init {
        viewModelScope.launch {
            when (val r = repo.personalQr(userId, null)) {
                is AppResult.Success -> _state.update { it.copy(staticQr = r.data) }
                is AppResult.Failure -> _state.update { it.copy(error = r.message) }
            }
        }
    }'''),
    ('''        when (val r = repo.personalQr(userId, amount)) {
            is AppResult.Failure -> _state.update { it.copy(error = r.message) }
            is AppResult.Success -> _state.update { it.copy(dynamicQr = r.data, dynamicAmount = amount, error = null) }
        }''', '''        viewModelScope.launch {
            when (val r = repo.personalQr(userId, amount)) {
                is AppResult.Failure -> _state.update { it.copy(error = r.message) }
                is AppResult.Success -> _state.update { it.copy(dynamicQr = r.data, dynamicAmount = amount, error = null) }
            }
        }'''),
]
for a, b in pairs:
    assert a in s, a[:90]
    s = s.replace(a, b)
s = s.replace('import com.diponegoro.digitalbank.core.di.CoreModule', 'import com.diponegoro.digitalbank.account.data.AccountRepository\nimport com.diponegoro.digitalbank.core.di.CoreModule', 1)
open(vp, 'w', encoding='utf8', newline='\n').write(s)
print('ok')
