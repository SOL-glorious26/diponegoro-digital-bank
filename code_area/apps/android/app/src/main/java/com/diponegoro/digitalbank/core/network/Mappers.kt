package com.diponegoro.digitalbank.core.network

import com.diponegoro.digitalbank.authentication.domain.model.AccountStatus
import com.diponegoro.digitalbank.authentication.domain.model.User
import com.diponegoro.digitalbank.core.ledger.JournalLine
import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.ledger.TxCategory
import com.diponegoro.digitalbank.core.ledger.TxStatus

/**
 * Pemetaan transaksi backend → model ledger aplikasi, agar layar bukti, riwayat, dan detail
 * dipakai bersama oleh varian demo dan online tanpa perubahan.
 *
 * Backend mengirim arah transaksi dari sudut pandang [viewerUserId]; model aplikasi menyimpannya
 * sebagai debitUserId/creditUserId, sehingga directionFor(viewerUserId) menghasilkan arah yang sama.
 */
fun TransactionDto.toLedger(viewerUserId: String): LedgerTransaction = LedgerTransaction(
    id = id,
    reference = reference,
    idempotencyKey = "",
    category = runCatching { TxCategory.valueOf(category) }.getOrDefault(TxCategory.TRANSFER),
    status = runCatching { TxStatus.valueOf(status) }.getOrDefault(TxStatus.PROCESSING),
    createdAt = IsoTime.parse(createdAt),
    updatedAt = IsoTime.parse(updatedAt),
    amount = amount,
    fee = fee,
    title = title,
    debitUserId = if (direction == "OUT" || direction == "INTERNAL") viewerUserId else null,
    creditUserId = if (direction == "IN" || direction == "INTERNAL") viewerUserId else null,
    fromName = fromName,
    fromDetail = fromDetail,
    toName = toName,
    toDetail = toDetail,
    note = note.orEmpty(),
    statusReason = statusReason,
    extra = extra.orEmpty()
)

fun JournalLineDto.toLedger(txId: String) = JournalLine(
    txId = txId, accountId = account, amount = amount, postedAt = IsoTime.parse(postedAt), memo = memo
)

/** Kunci idempotensi backend: maks. 64 karakter [A-Za-z0-9._:-] (common/Idempotency.java). */
fun idemKey(raw: String): String = raw.filter { it.isLetterOrDigit() || it in "._:-" }.take(64).ifEmpty { "k" }

fun UserDto.toUser() = User(
    userId = id, email = email.orEmpty(), phoneNumber = phoneNumber.orEmpty(), username = username,
    accountStatus = runCatching { AccountStatus.valueOf(status) }.getOrDefault(AccountStatus.ACTIVE),
    photoUri = photoUrl
)
