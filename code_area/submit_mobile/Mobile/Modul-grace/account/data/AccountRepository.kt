package com.diponegoro.digitalbank.account.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.diponegoro.digitalbank.account.domain.*
import com.diponegoro.digitalbank.authentication.domain.model.AccountStatus
import com.diponegoro.digitalbank.authentication.domain.model.Session
import com.diponegoro.digitalbank.authentication.domain.model.User
import com.diponegoro.digitalbank.authentication.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class AccountRepository(private val helper: AccountDatabase, private val auth: AuthRepository) {
    private val revision = MutableStateFlow(0L)
    val changes = revision.asStateFlow()

    private suspend fun authorized(session: Session): User {
        require(!session.isExpired() && auth.getActiveSessions(session.userId).any { it.sessionId == session.sessionId }) {
            "Sesi sudah berakhir. Silakan login kembali."
        }
        return requireNotNull(auth.getUser(session.userId)) { "Pengguna tidak ditemukan." }
    }
    private fun <T> atomic(block: (SQLiteDatabase) -> T): T {
        val db = helper.writableDatabase
        db.beginTransaction()
        try { val result = block(db); db.setTransactionSuccessful(); return result }
        finally { db.endTransaction() }
    }
    private fun changed() { revision.value += 1 }

    suspend fun snapshot(session: Session): AccountSnapshot = withContext(Dispatchers.IO) {
        val user = authorized(session)
        atomic { db ->
            // Seed hanya sekali. Pengguna baru mendapat saldo nol; seed demo tidak diisi ulang.
            val operation = "initial:${user.userId}"
            db.rawQuery("SELECT id FROM accounts WHERE operationId = ?", arrayOf(operation)).use {
                if (!it.moveToFirst()) insertAccount(db, user.userId, BankAccountType.TABUNGAN,
                    "Tabungan Utama", operation, if (user.userId == "user-demo") 1_000_000L else 0L)
            }
            AccountSnapshot(user.username, user.accountStatus == AccountStatus.FINANCIAL_ENABLED, accounts(db, user.userId))
        }
    }
    suspend fun open(session: Session, type: BankAccountType, alias: String, operationId: String): Unit = withContext(Dispatchers.IO) {
        val user = authorized(session)
        atomic { db ->
            db.rawQuery("SELECT userId FROM accounts WHERE operationId = ?", arrayOf(operationId)).use {
                if (it.moveToFirst()) {
                    require(it.getString(0) == user.userId) { "Operasi tidak valid." }
                    return@atomic
                }
            }
            AccountRules.validateOpening(user.accountStatus == AccountStatus.FINANCIAL_ENABLED,
                accounts(db, user.userId).count { it.status != BankAccountStatus.TERTUTUP }, type, alias)
            insertAccount(db, user.userId, type, alias.trim(), operationId, 0)
        }
        changed()
    }
    private fun insertAccount(db: SQLiteDatabase, userId: String, type: BankAccountType, alias: String, operation: String, balance: Long) {
        val id = UUID.randomUUID().toString()
        val number = "10" + UUID.randomUUID().toString().replace("-", "").take(14).toBigInteger(16).toString().padStart(17, '0')
        val primary = accounts(db, userId).none { it.isPrimary }
        db.insertOrThrow("accounts", null, ContentValues().apply {
            put("id", id); put("userId", userId); put("number", number); put("type", type.name)
            put("alias", alias); put("balance", balance); put("availableBalance", balance)
            put("status", BankAccountStatus.AKTIF.name); put("openedAt", System.currentTimeMillis())
            put("isPrimary", if (primary) 1 else 0); put("operationId", operation)
        })
    }
    suspend fun rename(session: Session, id: String, alias: String): Unit = withContext(Dispatchers.IO) {
        val user = authorized(session)
        AccountRules.validateAlias(alias)
        atomic { db ->
            require(owned(db, user.userId, id).status != BankAccountStatus.TERTUTUP) { "Rekening sudah ditutup." }
            db.execSQL("UPDATE accounts SET alias = ? WHERE id = ? AND userId = ?", arrayOf(alias.trim(), id, user.userId))
        }; changed()
    }
    suspend fun setPrimary(session: Session, id: String): Unit = withContext(Dispatchers.IO) {
        val user = authorized(session)
        atomic { db ->
            require(owned(db, user.userId, id).status == BankAccountStatus.AKTIF) { "Rekening utama harus aktif." }
            db.execSQL("UPDATE accounts SET isPrimary = 0 WHERE userId = ?", arrayOf(user.userId))
            db.execSQL("UPDATE accounts SET isPrimary = 1 WHERE id = ? AND userId = ?", arrayOf(id, user.userId))
        }; changed()
    }
    suspend fun close(session: Session, id: String): Unit = withContext(Dispatchers.IO) {
        val user = authorized(session)
        atomic { db ->
            val account = owned(db, user.userId, id)
            if (account.status == BankAccountStatus.TERTUTUP) return@atomic // Retry tidak mengulang penutupan.
            val pending = db.rawQuery("SELECT id FROM account_transactions WHERE accountId = ? AND status = 'PENDING' LIMIT 1", arrayOf(id)).use { it.moveToFirst() }
            AccountRules.validateClosing(account, pending)
            db.execSQL("UPDATE accounts SET status = 'TERTUTUP', closedAt = ?, isPrimary = 0 WHERE id = ? AND userId = ?",
                arrayOf<Any>(System.currentTimeMillis(), id, user.userId))
            if (account.isPrimary) accounts(db, user.userId).firstOrNull { it.status == BankAccountStatus.AKTIF }?.let {
                db.execSQL("UPDATE accounts SET isPrimary = 1 WHERE id = ?", arrayOf(it.id))
            }
        }; changed()
    }
    suspend fun history(session: Session, id: String, start: Long, endExclusive: Long): List<AccountTransaction> = withContext(Dispatchers.IO) {
        val user = authorized(session)
        require(user.accountStatus == AccountStatus.FINANCIAL_ENABLED) { "Detail rekening memerlukan e-KYC demo yang disetujui." }
        owned(helper.readableDatabase, user.userId, id)
        require(start < endExclusive) { "Periode tanggal tidak valid." }
        transactions(helper.readableDatabase, id, start, endExclusive)
    }
    suspend fun statement(session: Session, id: String, start: Long, endExclusive: Long, format: StatementFormat): StatementDocument = withContext(Dispatchers.IO) {
        val user = authorized(session)
        require(user.accountStatus == AccountStatus.FINANCIAL_ENABLED) { "e-Statement memerlukan e-KYC demo yang disetujui." }
        require(start < endExclusive) { "Periode tanggal tidak valid." }
        atomic { db ->
            val account = owned(db, user.userId, id)
            val rows = transactions(db, id, start, endExclusive)
            val statementId = UUID.randomUUID().toString()
            val document = StatementExporter.generate(account, user.username, start, endExclusive, rows, format, statementId)
            db.insertOrThrow("statements", null, ContentValues().apply {
                put("id", statementId); put("accountId", id); put("startAt", start); put("endAt", endExclusive)
                put("format", format.name); put("generatedAt", System.currentTimeMillis())
            })
            document
        }
    }
    private fun owned(db: SQLiteDatabase, userId: String, id: String): BankAccount =
        accounts(db, userId).firstOrNull { it.id == id } ?: error("Rekening tidak ditemukan atau bukan milik Anda.")

    private fun accounts(db: SQLiteDatabase, userId: String): List<BankAccount> =
        db.rawQuery("SELECT * FROM accounts WHERE userId = ? ORDER BY isPrimary DESC, openedAt DESC", arrayOf(userId)).use { c ->
            buildList { while (c.moveToNext()) add(BankAccount(c.s("id"), c.s("userId"), c.s("number"),
                BankAccountType.valueOf(c.s("type")), c.s("alias"), c.s("currency"), c.l("balance"), c.l("availableBalance"),
                BankAccountStatus.valueOf(c.s("status")), c.l("openedAt"),
                if (c.isNull(c.getColumnIndexOrThrow("closedAt"))) null else c.l("closedAt"), c.l("isPrimary") == 1L)) }
        }
    private fun transactions(db: SQLiteDatabase, id: String, start: Long, end: Long): List<AccountTransaction> =
        db.rawQuery("SELECT * FROM account_transactions WHERE accountId = ? AND createdAt >= ? AND createdAt < ? ORDER BY createdAt DESC, id DESC",
            arrayOf(id, start.toString(), end.toString())).use { c -> buildList {
            while (c.moveToNext()) add(AccountTransaction(c.s("id"), c.s("accountId"), c.l("createdAt"),
                TransactionType.valueOf(c.s("type")), c.s("description"), c.l("amount"), c.l("balanceAfter"),
                c.s("reference"), TransactionStatus.valueOf(c.s("status"))))
        } }
    private fun Cursor.s(name: String) = getString(getColumnIndexOrThrow(name))
    private fun Cursor.l(name: String) = getLong(getColumnIndexOrThrow(name))
}
