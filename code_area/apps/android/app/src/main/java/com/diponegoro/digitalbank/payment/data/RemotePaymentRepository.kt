package com.diponegoro.digitalbank.payment.data

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
