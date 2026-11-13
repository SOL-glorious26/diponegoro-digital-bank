package com.diponegoro.digitalbank.topup.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.diponegoro.digitalbank.account.data.AccountDatabase
import com.diponegoro.digitalbank.account.data.AccountRepository
import com.diponegoro.digitalbank.authentication.domain.model.AccountStatus
import com.diponegoro.digitalbank.authentication.domain.model.AuthResult
import com.diponegoro.digitalbank.authentication.domain.model.Session
import com.diponegoro.digitalbank.authentication.domain.repository.AuthRepository
import com.diponegoro.digitalbank.authentication.domain.usecase.ValidateTransactionPinUseCase
import com.diponegoro.digitalbank.topup.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

/** Gateway lokal: hasil hanya diproses melalui tindakan simulasi pengguna, tanpa jaringan. */
class TopUpRepository(
    private val helper: AccountDatabase, private val accounts: AccountRepository, private val auth: AuthRepository
) {
    private suspend fun authorized(session: Session, financial: Boolean = false) {
        require(!session.isExpired() && auth.getActiveSessions(session.userId).any { it.sessionId == session.sessionId }) {
            "Sesi sudah berakhir. Silakan login kembali."
        }
        val user = requireNotNull(auth.getUser(session.userId)) { "Pengguna tidak ditemukan." }
        require(!financial || user.accountStatus == AccountStatus.FINANCIAL_ENABLED) { "Top-up memerlukan e-KYC demo yang disetujui." }
    }
    private fun <T> atomic(block: (SQLiteDatabase) -> T): T {
        val db = helper.writableDatabase
        db.beginTransaction()
        try { val result = block(db); db.setTransactionSuccessful(); return result }
        finally { db.endTransaction() }
    }
    private fun changed() = accounts.notifyChanged()

    suspend fun snapshot(session: Session): TopUpSnapshot = withContext(Dispatchers.IO) {
        authorized(session)
        var expired = false
        val result = atomic { db ->
            seed(db, session.userId)
            history(db, session.userId).filter { it.status == TopUpStatus.PENDING && it.expiresAt <= System.currentTimeMillis() }.forEach {
                finish(db, session.userId, it, TopUpStatus.KADALUWARSA, "Batas waktu pembayaran demo telah berakhir.")
                expired = true
            }
            TopUpSnapshot(methods(db, session.userId), history(db, session.userId), notifications(db, session.userId),
                limit(db, session.userId), usedToday(db, session.userId))
        }
        if (expired) changed()
        result
    }
    suspend fun quote(session: Session, draft: TopUpDraft): TopUpQuote = withContext(Dispatchers.IO) {
        authorized(session, financial = true)
        atomic { db -> seed(db, session.userId); validate(db, session.userId, draft) }
    }
    suspend fun begin(session: Session, draft: TopUpDraft, pin: String): TopUpTransaction = withContext(Dispatchers.IO) {
        authorized(session, financial = true)
        when (val result = ValidateTransactionPinUseCase(auth)(session.userId, pin)) {
            is AuthResult.Failure -> error(result.message)
            is AuthResult.Success -> Unit
        }
        authorized(session, financial = true)
        val result = atomic { db ->
            val previous = history(db, session.userId).firstOrNull { it.operationId == draft.operationId }
            if (previous != null) {
                require(previous.accountId == draft.accountId && previous.target == draft.target && previous.amount == draft.amount) {
                    "Referensi operasi telah digunakan dengan data berbeda."
                }
                db.rawQuery("SELECT serviceId, methodId FROM topups WHERE id = ? AND userId = ?", arrayOf(previous.id, session.userId)).use {
                    require(it.moveToFirst() && it.getString(0) == draft.serviceId && it.getString(1) == draft.methodId) { "Referensi operasi telah digunakan dengan data berbeda." }
                }
                return@atomic previous
            }
            seed(db, session.userId)
            val quote = validate(db, session.userId, draft)
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val transaction = TopUpTransaction(id, draft.operationId, draft.accountId, quote.accountNumber,
                "TU-${UUID.randomUUID()}", now, now + 10 * 60 * 1000L, quote.service.name,
                draft.target, quote.methodName, draft.amount, quote.fee, quote.service.direction,
                TopUpStatus.PENDING, "Menunggu hasil pembayaran simulasi. Saldo belum berubah.", null)
            db.insertOrThrow("topups", null, ContentValues().apply {
                put("id", id); put("operationId", draft.operationId); put("userId", session.userId)
                put("accountId", draft.accountId); put("accountNumber", quote.accountNumber); put("reference", transaction.reference)
                put("createdAt", now); put("expiresAt", transaction.expiresAt); put("serviceId", draft.serviceId)
                put("serviceName", quote.service.name); put("target", draft.target); put("methodId", draft.methodId)
                put("methodName", quote.methodName); put("amount", draft.amount); put("fee", quote.fee)
                put("direction", quote.service.direction.name); put("status", TopUpStatus.PENDING.name); put("note", transaction.note)
            })
            val account = account(db, session.userId, draft.accountId)
            db.insertOrThrow("account_transactions", null, ContentValues().apply {
                put("id", id); put("accountId", draft.accountId); put("createdAt", now); put("type", "TOP_UP")
                put("description", "${quote.service.name} • ${draft.target}"); put("amount", draft.amount)
                put("balanceAfter", account.balance); put("reference", transaction.reference); put("status", "PENDING")
                put("direction", quote.service.direction.name); put("operationId", draft.operationId); put("fee", quote.fee)
            })
            transaction
        }
        changed()
        result
    }
    suspend fun settle(session: Session, id: String, outcome: TopUpStatus): TopUpTransaction = withContext(Dispatchers.IO) {
        authorized(session, financial = outcome == TopUpStatus.BERHASIL)
        require(outcome in listOf(TopUpStatus.BERHASIL, TopUpStatus.GAGAL, TopUpStatus.DIBATALKAN)) { "Hasil pembayaran tidak valid." }
        val result = atomic { db ->
            val transaction = history(db, session.userId).firstOrNull { it.id == id }
                ?: error("Transaksi tidak ditemukan atau bukan milik Anda.")
            if (transaction.status != TopUpStatus.PENDING) return@atomic transaction
            if (transaction.expiresAt <= System.currentTimeMillis()) {
                return@atomic finish(db, session.userId, transaction, TopUpStatus.KADALUWARSA, "Batas waktu pembayaran demo telah berakhir.")
            }
            if (outcome != TopUpStatus.BERHASIL) {
                return@atomic finish(db, session.userId, transaction, outcome,
                    if (outcome == TopUpStatus.DIBATALKAN) "Dibatalkan pengguna saat pending; saldo tidak berubah." else "Pembayaran demo gagal; saldo tidak berubah.")
            }
            val account = account(db, session.userId, transaction.accountId)
            if (account.status != "AKTIF") return@atomic finish(db, session.userId, transaction, TopUpStatus.GAGAL, "Rekening tidak aktif; saldo tidak berubah.")
            val delta = if (transaction.direction == MoneyDirection.CREDIT) transaction.amount else -transaction.total
            if (transaction.direction == MoneyDirection.DEBIT && account.available < transaction.total) {
                return@atomic finish(db, session.userId, transaction, TopUpStatus.GAGAL, "Saldo tidak cukup saat penyelesaian; saldo tidak berubah.")
            }
            val newBalance: Long
            val newAvailable: Long
            try {
                newBalance = Math.addExact(account.balance, delta)
                newAvailable = Math.addExact(account.available, delta)
            } catch (_: ArithmeticException) {
                return@atomic finish(db, session.userId, transaction, TopUpStatus.GAGAL, "Saldo melebihi batas penyimpanan; saldo tidak berubah.")
            }
            require(newBalance >= 0 && newAvailable >= 0) { "Saldo tidak boleh negatif." }
            db.execSQL("UPDATE accounts SET balance = ?, availableBalance = ? WHERE id = ? AND userId = ?",
                arrayOf<Any>(newBalance, newAvailable, transaction.accountId, session.userId))
            finish(db, session.userId, transaction, TopUpStatus.BERHASIL,
                "Pembayaran simulasi berhasil. ${if (transaction.direction == MoneyDirection.CREDIT) "Saldo rekening dikreditkan." else "Saldo rekening didebitkan; layanan tujuan bersifat simulasi."}", newBalance)
        }
        changed()
        result
    }

    suspend fun saveMethod(session: Session, type: PaymentMethodType, name: String, identifier: String): Unit = withContext(Dispatchers.IO) {
        authorized(session)
        require(type == PaymentMethodType.VIRTUAL_ACCOUNT || type == PaymentMethodType.E_WALLET) { "Hanya virtual account dan e-wallet demo yang dapat disimpan." }
        require(name.trim().length in 1..40 && name.none { it.isISOControl() }) { "Nama metode harus 1–40 karakter." }
        require(if (type == PaymentMethodType.E_WALLET) Regex("08[0-9]{8,11}").matches(identifier)
            else Regex("[0-9]{6,24}").matches(identifier)) { "Nomor metode demo tidak valid." }
        atomic { db ->
            seed(db, session.userId)
            require(methods(db, session.userId).size < 10) { "Maksimal 10 metode tersimpan." }
            require(methods(db, session.userId).none { it.type == type && it.identifier == identifier }) { "Metode sudah tersimpan." }
            db.insertOrThrow("payment_methods", null, ContentValues().apply {
                put("id", UUID.randomUUID().toString()); put("userId", session.userId); put("type", type.name)
                put("name", name.trim()); put("identifier", identifier); put("status", "AKTIF"); put("isDefault", 0)
            })
        }; changed()
    }
    suspend fun defaultMethod(session: Session, id: String): Unit = withContext(Dispatchers.IO) {
        authorized(session)
        atomic { db ->
            require(methods(db, session.userId).any { it.id == id && it.status == PaymentMethodStatus.AKTIF }) { "Metode tidak aktif atau bukan milik Anda." }
            db.execSQL("UPDATE payment_methods SET isDefault = 0 WHERE userId = ?", arrayOf(session.userId))
            db.execSQL("UPDATE payment_methods SET isDefault = 1 WHERE id = ? AND userId = ?", arrayOf(id, session.userId))
        }; changed()
    }
    suspend fun saveLimit(session: Session, amount: Long): Unit = withContext(Dispatchers.IO) {
        authorized(session)
        require(amount in 10_000L..10_000_000L) { "Limit harian harus Rp10.000–Rp10.000.000." }
        atomic { db ->
            seed(db, session.userId)
            require(amount >= usedToday(db, session.userId)) { "Limit tidak boleh lebih kecil dari total berhasil dan pending hari ini." }
            db.execSQL("UPDATE topup_settings SET dailyLimit = ? WHERE userId = ?", arrayOf<Any>(amount, session.userId))
        }; changed()
    }
    suspend fun receipt(session: Session, id: String): TopUpTransaction = withContext(Dispatchers.IO) {
        authorized(session)
        history(helper.readableDatabase, session.userId).firstOrNull { it.id == id }
            ?.also { require(it.status == TopUpStatus.BERHASIL) { "Bukti pembayaran tersedia untuk transaksi berhasil." } }
            ?: error("Transaksi tidak ditemukan atau bukan milik Anda.")
    }
    private data class AccountBalance(val number: String, val balance: Long, val available: Long, val status: String)
    private fun account(db: SQLiteDatabase, userId: String, id: String): AccountBalance =
        db.rawQuery("SELECT number, balance, availableBalance, status FROM accounts WHERE id = ? AND userId = ?", arrayOf(id, userId)).use {
            require(it.moveToFirst()) { "Rekening tidak ditemukan atau bukan milik Anda." }
            AccountBalance(it.getString(0), it.getLong(1), it.getLong(2), it.getString(3))
        }
    private fun validate(db: SQLiteDatabase, userId: String, draft: TopUpDraft): TopUpQuote {
        require(draft.operationId.isNotBlank()) { "Referensi operasi tidak tersedia." }
        val service = TopUpCatalog.service(draft.serviceId)
        require(service.status == ServiceStatus.AKTIF) { "Layanan tidak aktif atau sedang maintenance." }
        require(draft.amount in service.minimum..service.maximum) { "Nominal di luar batas layanan." }
        TopUpCatalog.validateTarget(service, draft.target)
        val account = account(db, userId, draft.accountId)
        require(account.status == "AKTIF") { "Rekening harus aktif." }
        val method = methods(db, userId).firstOrNull { it.id == draft.methodId && it.status == PaymentMethodStatus.AKTIF }
            ?: error("Metode pembayaran tidak aktif atau bukan milik Anda.")
        val total = Math.addExact(draft.amount, service.fee)
        require(total <= limit(db, userId) - usedToday(db, userId)) { "Limit top-up harian terlampaui (termasuk transaksi pending)." }
        if (service.direction == MoneyDirection.CREDIT) {
            require(draft.target == account.number) { "Nomor tujuan harus sama dengan rekening tujuan." }
            require(method.type in listOf(PaymentMethodType.VIRTUAL_ACCOUNT, PaymentMethodType.E_WALLET)) { "Pilih virtual account atau e-wallet demo untuk pengisian rekening." }
            Math.addExact(account.balance, draft.amount); Math.addExact(account.available, draft.amount)
        } else {
            require(method.type == PaymentMethodType.SALDO_AKUN) { "Top-up layanan memakai saldo rekening sebagai sumber dana." }
            val held = db.rawQuery("SELECT COALESCE(SUM(amount + fee), 0) FROM topups WHERE accountId = ? AND userId = ? AND status = 'PENDING' AND direction = 'DEBIT'", arrayOf(draft.accountId, userId)).use { it.moveToFirst(); it.getLong(0) }
            require(account.available >= held && total <= account.available - held) { "Saldo tidak cukup setelah memperhitungkan top-up pending." }
        }
        return TopUpQuote(draft, service, account.number, method.name, service.fee, total)
    }
    private fun finish(db: SQLiteDatabase, userId: String, transaction: TopUpTransaction, status: TopUpStatus, note: String, balance: Long? = null): TopUpTransaction {
        val now = System.currentTimeMillis()
        val result = transaction.copy(status = status, note = note, balanceAfter = balance, completedAt = now)
        db.update("topups", ContentValues().apply { put("status", status.name); put("note", note); put("completedAt", now)
            if (balance != null) put("balanceAfter", balance) else putNull("balanceAfter") }, "id = ? AND userId = ? AND status = 'PENDING'", arrayOf(transaction.id, userId))
        val ledgerStatus = when (status) {
            TopUpStatus.BERHASIL -> "BERHASIL"
            TopUpStatus.DIBATALKAN -> "DIBATALKAN"
            else -> "GAGAL"
        }
        db.update("account_transactions", ContentValues().apply { put("status", ledgerStatus)
            if (balance != null) { put("balanceAfter", balance); put("createdAt", now) } },
            "id = ? AND accountId = ?", arrayOf(transaction.id, transaction.accountId))
        db.insertOrThrow("bank_notifications", null, ContentValues().apply {
            put("id", UUID.randomUUID().toString()); put("userId", userId); put("transactionId", transaction.id)
            put("eventKey", "${transaction.id}:${status.name}"); put("message", "${transaction.serviceName}: ${status.label}. $note")
            put("createdAt", now)
        })
        return result
    }
    private fun seed(db: SQLiteDatabase, userId: String) {
        db.execSQL("INSERT OR IGNORE INTO topup_settings VALUES (?, 2000000)", arrayOf(userId))
        if (methods(db, userId).isEmpty()) {
            listOf(PaymentMethodType.VIRTUAL_ACCOUNT to "Virtual account demo", PaymentMethodType.SALDO_AKUN to "Saldo rekening").forEachIndexed { index, pair ->
                db.insertOrThrow("payment_methods", null, ContentValues().apply {
                    put("id", "${pair.first.name}:$userId"); put("userId", userId); put("type", pair.first.name)
                    put("name", pair.second); put("identifier", if (index == 0) "DEMO-VA" else "REKENING-DEMO")
                    put("status", "AKTIF"); put("isDefault", if (index == 0) 1 else 0)
                })
            }
        }
    }
    private fun limit(db: SQLiteDatabase, userId: String): Long = db.rawQuery("SELECT dailyLimit FROM topup_settings WHERE userId = ?", arrayOf(userId)).use { require(it.moveToFirst()); it.getLong(0) }
    private fun usedToday(db: SQLiteDatabase, userId: String): Long {
        val start = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        return db.rawQuery("SELECT COALESCE(SUM(amount + fee), 0) FROM topups WHERE userId = ? AND (status = 'PENDING' OR (status = 'BERHASIL' AND completedAt >= ?))", arrayOf(userId, start.toString())).use { it.moveToFirst(); it.getLong(0) }
    }
    private fun methods(db: SQLiteDatabase, userId: String): List<PaymentMethod> =
        db.rawQuery("SELECT * FROM payment_methods WHERE userId = ? ORDER BY isDefault DESC, name", arrayOf(userId)).use { c -> buildList {
            while (c.moveToNext()) add(PaymentMethod(c.s("id"), PaymentMethodType.valueOf(c.s("type")), c.s("name"), c.s("identifier"), PaymentMethodStatus.valueOf(c.s("status")), c.l("isDefault") == 1L))
        } }
    private fun history(db: SQLiteDatabase, userId: String): List<TopUpTransaction> =
        db.rawQuery("SELECT * FROM topups WHERE userId = ? ORDER BY createdAt DESC, id DESC", arrayOf(userId)).use { c -> buildList {
            while (c.moveToNext()) add(TopUpTransaction(c.s("id"), c.s("operationId"), c.s("accountId"), c.s("accountNumber"),
                c.s("reference"), c.l("createdAt"), c.l("expiresAt"), c.s("serviceName"), c.s("target"), c.s("methodName"),
                c.l("amount"), c.l("fee"), MoneyDirection.valueOf(c.s("direction")), TopUpStatus.valueOf(c.s("status")), c.s("note"),
                if (c.isNull(c.getColumnIndexOrThrow("balanceAfter"))) null else c.l("balanceAfter"),
                if (c.isNull(c.getColumnIndexOrThrow("completedAt"))) null else c.l("completedAt")))
        } }
    private fun notifications(db: SQLiteDatabase, userId: String): List<TopUpNotification> =
        db.rawQuery("SELECT * FROM bank_notifications WHERE userId = ? ORDER BY createdAt DESC, id DESC", arrayOf(userId)).use { c -> buildList {
            while (c.moveToNext()) add(TopUpNotification(c.s("id"), c.s("transactionId"), c.s("message"), c.l("createdAt")))
        } }
    private fun Cursor.s(name: String) = getString(getColumnIndexOrThrow(name))
    private fun Cursor.l(name: String) = getLong(getColumnIndexOrThrow(name))
}
