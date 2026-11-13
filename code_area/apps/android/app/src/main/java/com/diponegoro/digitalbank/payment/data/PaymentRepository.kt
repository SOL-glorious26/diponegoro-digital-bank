package com.diponegoro.digitalbank.payment.data

import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.payment.domain.QrisPayload

/** Hasil pindai yang sudah divalidasi, siap ditampilkan sebelum PIN (FR-15). */
data class ScannedQris(
    val payload: QrisPayload,
    /** Rekening Diponegoro tujuan bila QR pribadi nasabah bank yang sama. */
    val internalAccountId: String?,
    val internalOwnerId: String?,
    /** Teks QR asli (dikirim ke backend pada varian online). */
    val raw: String = ""
)

data class DemoQr(val label: String, val raw: String)

object QrisRules {
    const val MIN_AMOUNT = 1_000L
    /** Batas nominal per transaksi QRIS (ketentuan umum QRIS). */
    const val MAX_AMOUNT = 10_000_000L
}

/**
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
