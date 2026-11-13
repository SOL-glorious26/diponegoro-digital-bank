package com.diponegoro.digitalbank.transfer.data

import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.network.ApiClient
import com.diponegoro.digitalbank.core.network.FavoriteDto
import com.diponegoro.digitalbank.core.network.FavoriteRequest
import com.diponegoro.digitalbank.core.network.InquiryRequest
import com.diponegoro.digitalbank.core.network.IsoTime
import com.diponegoro.digitalbank.core.network.QuoteRequest
import com.diponegoro.digitalbank.core.network.RecipientDto
import com.diponegoro.digitalbank.core.network.TransferRequest
import com.diponegoro.digitalbank.core.network.idemKey
import com.diponegoro.digitalbank.core.network.toLedger
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.core.util.map
import com.diponegoro.digitalbank.core.util.parseRupiah
import com.diponegoro.digitalbank.transfer.domain.Bank
import com.diponegoro.digitalbank.transfer.domain.FavoriteRecipient
import com.diponegoro.digitalbank.transfer.domain.Recipient
import com.diponegoro.digitalbank.transfer.domain.TransferQuote

/** Transfer lewat backend: validasi nominal, saldo, biaya, idempotensi, dan antarbank dijalankan server. */
class RemoteTransferRepository(
    private val client: ApiClient,
    private val onDataChanged: () -> Unit = {}
) : TransferRepository {

    @Volatile private var bankCache: List<Bank> = emptyList()
    @Volatile private var favoriteCache: List<FavoriteRecipient> = emptyList()

    private fun RecipientDto.toModel() = Recipient(Bank(bankCode, bankName), accountNumber, name)
    private fun FavoriteDto.toModel() = FavoriteRecipient(id, Recipient(Bank(bankCode, bankName), accountNumber, name), IsoTime.parse(addedAt))

    override suspend fun banks(): List<Bank> {
        if (bankCache.isNotEmpty()) return bankCache
        return when (val r = client.call { banks() }) {
            is AppResult.Success -> r.data.map { Bank(it.code, it.name) }.also { bankCache = it }
            is AppResult.Failure -> emptyList()
        }
    }

    override suspend fun inquire(userId: String, bankCode: String, rawNumber: String): AppResult<Recipient> =
        client.call { inquiry(InquiryRequest(bankCode, rawNumber.filter { it.isDigit() })) }.map { it.toModel() }

    override suspend fun quote(userId: String, recipient: Recipient, rawAmount: String, note: String): AppResult<TransferQuote> {
        val amount = parseRupiah(rawAmount) ?: return AppResult.Failure("Nominal wajib diisi")
        return client.call { quote(QuoteRequest(recipient.bank.code, recipient.accountNumber, amount, note.trim())) }
            .map { TransferQuote(it.recipient.toModel(), it.amount, it.fee, it.note.orEmpty(), it.balance) }
    }

    override suspend fun execute(userId: String, quote: TransferQuote, pin: String, idempotencyKey: String): AppResult<LedgerTransaction> {
        val r = quote.recipient
        return client.call {
            transfer(idemKey(idempotencyKey), TransferRequest(r.bank.code, r.accountNumber, quote.amount, quote.note, pin))
        }.map { it.toLedger(userId) }.also { if (it is AppResult.Success) onDataChanged() }
    }

    override suspend fun checkStatus(userId: String, txId: String): AppResult<LedgerTransaction> =
        client.call { checkStatus(txId) }.map { it.toLedger(userId) }.also { if (it is AppResult.Success) onDataChanged() }

    override suspend fun favorites(userId: String): List<FavoriteRecipient> =
        when (val r = client.call { favorites() }) {
            is AppResult.Success -> r.data.map { it.toModel() }.sortedBy { it.recipient.name }.also { favoriteCache = it }
            is AppResult.Failure -> favoriteCache
        }

    override suspend fun isFavorite(userId: String, recipient: Recipient): Boolean = favorites(userId).any {
        it.recipient.bank.code == recipient.bank.code && it.recipient.accountNumber == recipient.accountNumber
    }

    override suspend fun addFavorite(userId: String, recipient: Recipient): AppResult<FavoriteRecipient> =
        client.call { addFavorite(FavoriteRequest(recipient.bank.code, recipient.accountNumber)) }.map { it.toModel() }

    override suspend fun removeFavorite(userId: String, favoriteId: String) {
        client.call { removeFavorite(favoriteId) }
    }
}
