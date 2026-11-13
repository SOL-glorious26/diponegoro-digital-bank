package com.diponegoro.digitalbank.transfer.data

import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.transfer.domain.Bank
import com.diponegoro.digitalbank.transfer.domain.FavoriteRecipient
import com.diponegoro.digitalbank.transfer.domain.Recipient
import com.diponegoro.digitalbank.transfer.domain.TransferQuote

/**
 * Modul Transfer (FR-10 – FR-14, UC-01). Implementasi:
 *  - [LocalTransferRepository]  : ledger in-memory + simulator antarbank (varian demo)
 *  - [RemoteTransferRepository] : backend /api/v1/transfers (varian online)
 */
interface TransferRepository {
    suspend fun banks(): List<Bank>
    suspend fun inquire(userId: String, bankCode: String, rawNumber: String): AppResult<Recipient>
    suspend fun quote(userId: String, recipient: Recipient, rawAmount: String, note: String): AppResult<TransferQuote>
    suspend fun execute(userId: String, quote: TransferQuote, pin: String, idempotencyKey: String): AppResult<LedgerTransaction>
    suspend fun checkStatus(userId: String, txId: String): AppResult<LedgerTransaction>
    suspend fun favorites(userId: String): List<FavoriteRecipient>
    suspend fun isFavorite(userId: String, recipient: Recipient): Boolean
    suspend fun addFavorite(userId: String, recipient: Recipient): AppResult<FavoriteRecipient>
    suspend fun removeFavorite(userId: String, favoriteId: String)
}
