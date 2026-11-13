package com.diponegoro.digitalbank.payment.data

import com.diponegoro.digitalbank.core.ledger.DemoData
import com.diponegoro.digitalbank.core.ledger.ExternalAccounts
import com.diponegoro.digitalbank.core.ledger.Ledger
import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.ledger.PostingRequest
import com.diponegoro.digitalbank.core.ledger.TxCategory
import com.diponegoro.digitalbank.core.security.TransactionGuard
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.core.util.formatRupiah
import com.diponegoro.digitalbank.core.util.parseRupiah
import com.diponegoro.digitalbank.payment.domain.QrisCodec
import com.diponegoro.digitalbank.payment.domain.QrisPayload

/** Modul Payment (QRIS) — FR-15, FR-16, FR-17. */
class LocalPaymentRepository(
    private val ledger: Ledger,
    private val guard: TransactionGuard
) : PaymentRepository {

    // ---------------------------------------------------------------- FR-16 / FR-17

    /** QRIS pribadi; amount != null menghasilkan QRIS dinamis yang nominalnya tidak bisa diubah pembayar. */
    override suspend fun personalQr(userId: String, amount: Long?): AppResult<String> {
        val main = ledger.mainAccount(userId) ?: return AppResult.Failure("Rekening tidak ditemukan")
        if (amount != null && amount !in QrisRules.MIN_AMOUNT..QrisRules.MAX_AMOUNT)
            return AppResult.Failure(
                "Nominal QRIS ${formatRupiah(QrisRules.MIN_AMOUNT)} – ${formatRupiah(QrisRules.MAX_AMOUNT)}"
            )
        return AppResult.Success(
            QrisCodec.encode(
                QrisPayload(
                    issuerGuid = QrisCodec.DDB_GUID,
                    accountId = main.number,
                    merchantId = "DDB${main.number}",
                    merchantName = main.name,
                    city = "SEMARANG",
                    mcc = QrisCodec.PERSONAL_MCC,
                    amount = amount
                )
            )
        )
    }

    /** Simulasi dana masuk dari pengguna bank lain yang memindai QRIS pribadi (FR-16). */
    override suspend fun simulateIncoming(
        userId: String, rawAmount: String, payerName: String, idempotencyKey: String
    ): AppResult<LedgerTransaction> {
        val amount = parseRupiah(rawAmount) ?: return AppResult.Failure("Nominal wajib diisi")
        if (amount !in QrisRules.MIN_AMOUNT..QrisRules.MAX_AMOUNT)
            return AppResult.Failure("Nominal ${formatRupiah(QrisRules.MIN_AMOUNT)} – ${formatRupiah(QrisRules.MAX_AMOUNT)}")
        val main = ledger.mainAccount(userId) ?: return AppResult.Failure("Rekening tidak ditemukan")
        return ledger.post(
            PostingRequest(
                idempotencyKey = idempotencyKey,
                category = TxCategory.QRIS,
                title = "Terima dana QRIS",
                amount = amount,
                lines = listOf(ExternalAccounts.INTERBANK to -amount, main.id to amount),
                debitUserId = null,
                creditUserId = userId,
                fromName = payerName.ifBlank { "PENGGUNA BANK LAIN" }.uppercase(),
                fromDetail = "QRIS antarbank (simulasi)",
                toName = main.name,
                toDetail = main.number
            )
        )
    }

    // ---------------------------------------------------------------- FR-15

    override suspend fun scan(userId: String, raw: String): AppResult<ScannedQris> {
        val payload = QrisCodec.decode(raw).getOrElse { return AppResult.Failure(it.message ?: "QRIS tidak valid") }
        if (payload.issuerGuid == QrisCodec.DDB_GUID) {
            val target = ledger.findByNumber(payload.accountId)
                ?: return AppResult.Failure("Penerima QRIS tidak ditemukan")
            if (target.ownerUserId == userId) return AppResult.Failure("Tidak dapat membayar ke QRIS milik sendiri")
            return AppResult.Success(ScannedQris(payload, target.id, target.ownerUserId, raw))
        }
        return AppResult.Success(ScannedQris(payload, null, null, raw))
    }

    override suspend fun pay(
        userId: String, scanned: ScannedQris, rawAmount: String, pin: String, idempotencyKey: String
    ): AppResult<LedgerTransaction> {
        ledger.findByIdempotencyKey(idempotencyKey)?.let { return AppResult.Success(it) }
        // QRIS dinamis: nominal dari kode, input pembayar diabaikan (FR-17).
        val amount = scanned.payload.amount ?: parseRupiah(rawAmount)
            ?: return AppResult.Failure("Nominal wajib diisi")
        if (amount !in QrisRules.MIN_AMOUNT..QrisRules.MAX_AMOUNT)
            return AppResult.Failure("Nominal ${formatRupiah(QrisRules.MIN_AMOUNT)} – ${formatRupiah(QrisRules.MAX_AMOUNT)}")
        val main = ledger.mainAccount(userId) ?: return AppResult.Failure("Rekening tidak ditemukan")
        if (ledger.balance(main.id) < amount) return AppResult.Failure("Saldo tidak mencukupi")

        val authorized = guard.authorize(userId, pin)
        if (authorized is AppResult.Failure) return authorized

        val creditAccount = scanned.internalAccountId ?: ExternalAccounts.MERCHANT
        return ledger.post(
            PostingRequest(
                idempotencyKey = idempotencyKey,
                category = TxCategory.QRIS,
                title = "Pembayaran QRIS",
                amount = amount,
                lines = listOf(main.id to -amount, creditAccount to amount),
                debitUserId = userId,
                creditUserId = scanned.internalOwnerId,
                fromName = main.name,
                fromDetail = main.number,
                toName = scanned.payload.merchantName,
                toDetail = "${if (scanned.payload.isDynamic) "QRIS dinamis" else "QRIS statis"} • ${scanned.payload.merchantId}",
                extra = mapOf("Kota merchant" to scanned.payload.city, "MCC" to scanned.payload.mcc)
            )
        )
    }

    /** Kode QR contoh untuk pengujian di emulator tanpa kamera. */
    override suspend fun demoQrs(): List<DemoQr> {
        fun merchant(name: String, id: String, mcc: String, amount: Long?) = QrisCodec.encode(
            QrisPayload("ID.CO.QRIS.WWW", "9360000$id", id, name, "SEMARANG", mcc, amount)
        )
        val andi = DemoData.customers.first()
        return listOf(
            DemoQr("Warung Bu Sri (QRIS statis)", merchant("WARUNG BU SRI", "M0001", "5812", null)),
            DemoQr("Kopi Senja Tembalang (dinamis Rp28.000)", merchant("KOPI SENJA TEMBALANG", "M0002", "5814", 28_000)),
            DemoQr("Toko Buku Diponegoro (dinamis Rp150.000)", merchant("TOKO BUKU DIPONEGORO", "M0003", "5942", 150_000)),
            DemoQr(
                "QRIS pribadi ${andi.name} (sesama bank)",
                QrisCodec.encode(
                    QrisPayload(QrisCodec.DDB_GUID, andi.number, "DDB${andi.number}", andi.name, "SEMARANG",
                        QrisCodec.PERSONAL_MCC, null)
                )
            )
        )
    }
}
