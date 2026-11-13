package com.diponegoro.digitalbank.account.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Database rekening bersama; tidak menyimpan credential autentikasi. */
class AccountDatabase(context: Context, name: String = "bank_accounts.db") : SQLiteOpenHelper(context.applicationContext, name, null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE accounts (
            id TEXT PRIMARY KEY, userId TEXT NOT NULL, number TEXT NOT NULL UNIQUE,
            type TEXT NOT NULL, alias TEXT NOT NULL, currency TEXT NOT NULL DEFAULT 'IDR',
            balance INTEGER NOT NULL CHECK(balance >= 0), availableBalance INTEGER NOT NULL CHECK(availableBalance >= 0 AND availableBalance <= balance),
            status TEXT NOT NULL, openedAt INTEGER NOT NULL, closedAt INTEGER,
            isPrimary INTEGER NOT NULL DEFAULT 0, operationId TEXT NOT NULL UNIQUE)""")
        db.execSQL("CREATE UNIQUE INDEX primary_account ON accounts(userId) WHERE isPrimary = 1")
        db.execSQL("CREATE INDEX account_owner ON accounts(userId)")
        db.execSQL("""CREATE TABLE account_transactions (
            id TEXT PRIMARY KEY, accountId TEXT NOT NULL REFERENCES accounts(id), createdAt INTEGER NOT NULL,
            type TEXT NOT NULL, description TEXT NOT NULL, amount INTEGER NOT NULL CHECK(amount > 0),
            balanceAfter INTEGER NOT NULL CHECK(balanceAfter >= 0), reference TEXT NOT NULL, status TEXT NOT NULL)""")
        db.execSQL("CREATE INDEX account_history ON account_transactions(accountId, createdAt)")
        db.execSQL("""CREATE TABLE statements (
            id TEXT PRIMARY KEY, accountId TEXT NOT NULL REFERENCES accounts(id), startAt INTEGER NOT NULL,
            endAt INTEGER NOT NULL, format TEXT NOT NULL, generatedAt INTEGER NOT NULL)""")
    }
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Migrasi database $oldVersion ke $newVersion belum tersedia.")
    }
}
