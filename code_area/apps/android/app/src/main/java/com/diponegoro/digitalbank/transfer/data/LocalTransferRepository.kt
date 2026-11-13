package com.diponegoro.digitalbank.transfer.data

import com.diponegoro.digitalbank.core.ledger.ExternalAccounts
import com.diponegoro.digitalbank.core.ledger.Ledger
import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.ledger.PostingRequest
import com.diponegoro.digitalbank.core.ledger.TxCategory
import com.diponegoro.digitalbank.core.ledger.TxStatus
import com.diponegoro.digitalbank.core.security.TransactionGuard
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.core.util.formatRupiah
import com.diponegoro.digitalbank.core.util.parseRupiah
import com.diponegoro.digitalbank.transfer.domain.Bank
import com.diponegoro.digitalbank.transfer.domain.FavoriteRecipient
import com.diponegoro.digitalbank.transfer.domain.NetworkBehavior
import com.diponegoro.digitalbank.transfer.domain.Recipient
import com.diponegoro.digitalbank.transfer.domain.TransferQuote
import com.diponegoro.digitalbank.transfer.domain.TransferRules
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/** Modul Transfer (FR-10 – FR-14, UC-01). */
class LocalTransferRepository(
    private val ledger: Ledger,
    private val guard: TransactionGuard,
    private val clock: () -> Long = System::currentTimeMillis
) : TransferRepository {
    private val favorites = ConcurrentHashMap<String, CopyOnWriteArrayList<FavoriteRecipient>>()

    override suspend fun banks(): List<Bank> = InterbankSimulator.banks

    // ---------------------------------------------------------------- FR-12 / A3

    override suspend fun inquire(userId: String, bankCode: String, rawNumber: String): AppResult<Recipient> {
        val bank = InterbankSimulator.bank(bankCode) ?: return AppResult.Failure("Pilih bank tujuan")
        val number = rawNumber.filter { it.isDigit() }
        if (number.length !in 10..16) return AppResult.Failure("Nomor rekening harus 10–16 digit")

        if (bank.isInternal) {
            val target = ledger.findByNumber(number)
                ?: return AppResult.Failure("Rekening tujuan tidak valid. Periksa kembali data penerima.")
            if (target.ownerUserId == userId) return AppResult.Failure("Tidak dapat transfer ke rekening sendiri")
            return AppResult.Success(Recipient(bank, number, target.name))
        }
        val (name, _) = InterbankSimulator.lookup(bank.code, number)
            ?: return AppResult.Failure("Rekening tujuan tidak valid. Periksa kembali data penerima.")
        return AppResult.Success(Recipient(bank, number, name))
    }

    // ---------------------------------------------------------------- UC-01 langkah 3–4 / A1, A2

    override suspend fun quote(userId: String, recipient: Recipient, rawAmount: String, note: String): AppResult<TransferQuote> {
        val amount = parseRupiah(rawAmount) ?: return AppResult.Failure("Nominal wajib diisi")
        if (amount < TransferRules.MIN_AMOUNT)
            return AppResult.Failure("Nominal minimal ${formatRupiah(TransferRules.MIN_AMOUNT)}")
        if (amount > TransferRules.MAX_AMOUNT_PER_TRANSACTION)
            return AppResult.Failure("Nominal maksimal ${formatRupiah(TransferRules.MAX_AMOUNT_PER_TRANSACTION)} per transaksi")
        if (note.length > TransferRules.MAX_NOTE)
            return AppResult.Failure("Catatan maksimal ${TransferRules.MAX_NOTE} karakter")
        val main = ledger.mainAccount(userId) ?: return AppResult.Failure("Rekening tidak ditemukan")
        val fee = if (recipient.bank.isInternal) 0 else TransferRules.INTERBANK_FEE
        val balance = ledger.balance(main.id)
        if (amount + fee > balance)
            return AppResult.Failure(
                "Saldo tidak mencukupi. Saldo Anda ${formatRupiah(balance)}, dibutuhkan ${formatRupiah(amount + fee)} " +
                    "(termasuk biaya). Kurangi nominal atau tarik dana dari kantong."
            )
        return AppResult.Success(TransferQuote(recipient, amount, fee, note.trim(), balance))
    }

    // ---------------------------------------------------------------- UC-01 langkah 5–7

    /**
     * Eksekusi transfer. idempotencyKey dibuat sekali per layar konfirmasi, sehingga
     * tombol ditekan dua kali / retry setelah timeout tidak menghasilkan debit ganda (BR-04, A5).
     */
    override suspend fun execute(
        userId: String, quote: TransferQuote, pin: String, idempotencyKey: String
    ): AppResult<LedgerTransaction> {
        ledger.findByIdempotencyKey(idempotencyKey)?.let { return AppResult.Success(it) }

        val authorized = guard.authorize(userId, pin)
        if (authorized is AppResult.Failure) return authorized

        val main = ledger.mainAccount(userId) ?: return AppResult.Failure("Rekening tidak ditemukan")
        val recipient = quote.recipient

        if (recipient.bank.isInternal) {
            // FR-10: debit & kredit atomik dalam satu posting.
            val target = ledger.findByNumber(recipient.accountNumber)
                ?: return AppResult.Failure("Rekening tujuan tidak valid")
            return ledger.post(
                PostingRequest(
                    idempotencyKey = idempotencyKey,
                    category = TxCategory.TRANSFER,
                    title = "Transfer sesama Diponegoro",
                    amount = quote.amount,
                    lines = listOf(main.id to -quote.amount, target.id to quote.amount),
                    debitUserId = userId,
                    creditUserId = target.ownerUserId,
                    fromName = main.name, fromDetail = "${Bank.INTERNAL_CODE} • ${main.number}",
                    toName = recipient.name, toDetail = "${recipient.bank.name} • ${recipient.accountNumber}",
                    note = quote.note
                )
            )
        }

        // FR-11: antarbank lewat simulator jaringan.
        val behavior = InterbankSimulator.lookup(recipient.bank.code, recipient.accountNumber)?.second
            ?: return AppResult.Failure("Rekening tujuan tidak valid")
        val posted = ledger.post(
            PostingRequest(
                idempotencyKey = idempotencyKey,
                category = TxCategory.TRANSFER,
                title = "Transfer antarbank",
                amount = quote.amount,
                fee = quote.fee,
                lines = listOf(
                    main.id to -quote.total,
                    ExternalAccounts.INTERBANK to quote.amount,
                    ExternalAccounts.FEE_INCOME to quote.fee
                ),
                debitUserId = userId,
                creditUserId = null,
                fromName = main.name, fromDetail = "${Bank.INTERNAL_CODE} • ${main.number}",
                toName = recipient.name, toDetail = "${recipient.bank.name} • ${recipient.accountNumber}",
                note = quote.note,
                status = if (behavior == NetworkBehavior.ACCEPT) TxStatus.SUCCESS else TxStatus.PROCESSING
            )
        )
        if (posted !is AppResult.Success) return posted
        return when (behavior) {
            NetworkBehavior.ACCEPT, NetworkBehavior.TIMEOUT -> posted
            // A6: ditolak jaringan → dana dikembalikan otomatis (BR-05).
            NetworkBehavior.REJECT -> ledger.failWithRefund(posted.data.id, "Ditolak oleh jaringan antarbank")
        }
    }

    /** A5: periksa ulang status transaksi yang masih diproses. */
    override suspend fun checkStatus(userId: String, txId: String): AppResult<LedgerTransaction> {
        val tx = ledger.transaction(txId)?.takeIf { it.debitUserId == userId }
            ?: return AppResult.Failure("Transaksi tidak ditemukan")
        if (tx.status != TxStatus.PROCESSING) return AppResult.Success(tx)
        return ledger.complete(txId)
    }

    // ---------------------------------------------------------------- FR-13

    override suspend fun favorites(userId: String): List<FavoriteRecipient> =
        favorites[userId].orEmpty().sortedBy { it.recipient.name }

    override suspend fun isFavorite(userId: String, recipient: Recipient): Boolean = favorites[userId].orEmpty().any {
        it.recipient.bank.code == recipient.bank.code && it.recipient.accountNumber == recipient.accountNumber
    }

    override suspend fun addFavorite(userId: String, recipient: Recipient): AppResult<FavoriteRecipient> {
        val list = favorites.getOrPut(userId) { CopyOnWriteArrayList() }
        if (isFavorite(userId, recipient)) return AppResult.Failure("Penerima sudah ada di favorit")
        if (list.size >= TransferRules.MAX_FAVORITES)
            return AppResult.Failure("Maksimal ${TransferRules.MAX_FAVORITES} penerima favorit")
        val favorite = FavoriteRecipient("fav-${UUID.randomUUID()}", recipient, clock())
        list.add(favorite)
        return AppResult.Success(favorite)
    }

    override suspend fun removeFavorite(userId: String, favoriteId: String) {
        favorites[userId]?.removeAll { it.id == favoriteId }
    }
}
