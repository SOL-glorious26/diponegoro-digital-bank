package com.example.diponegorodigitalbank

import java.math.BigDecimal
import java.time.LocalDateTime

data class User(
    val userId: String,
    val username: String,
    val email: String,
    val phoneNumber: String,
    val pin: String,
    val accounts: MutableList<Account> = mutableListOf()
) {
    fun login(inputUsername: String, inputPin: String): Boolean =
        username == inputUsername && pin == inputPin

    fun verifyPIN(inputPin: String): Boolean = pin == inputPin

    fun viewDashboard() = accounts.firstOrNull()
}

data class Account(
    val accountNumber: String,
    val bankName: String,
    var balance: BigDecimal
) {
    fun checkBalance(): BigDecimal = balance

    fun debit(amount: BigDecimal): Boolean {
        if (amount <= BigDecimal.ZERO || amount > balance) return false
        balance -= amount
        return true
    }

    fun credit(amount: BigDecimal) {
        if (amount > BigDecimal.ZERO) balance += amount
    }
}

data class Recipient(
    val recipientId: String,
    val bankName: String,
    val accountNumber: String,
    val recipientName: String,
    var isFavorite: Boolean = false
) {
    fun validateAccount(): Boolean =
        bankName.isNotBlank() && accountNumber.isNotBlank()

    fun saveFavorite() {
        isFavorite = true
    }
}

data class Transfer(
    val transferId: String,
    val amount: BigDecimal,
    val transferDate: LocalDateTime = LocalDateTime.now(),
    val note: String = "",
    var status: String = "Pending",
    val recipient: Recipient
) {
    fun validateAccount(): Boolean = recipient.validateAccount()
    fun processTransfer(): Boolean {
        status = "Berhasil"
        return true
    }
    fun verifyPIN(inputPin: String, user: User): Boolean = user.verifyPIN(inputPin)
    fun generateReceipt(): TransferReceipt =
        TransferReceipt(
            receiptId = "RC-$transferId",
            transferId = transferId,
            recipientName = recipient.recipientName,
            bankName = recipient.bankName,
            accountNumber = recipient.accountNumber,
            amount = amount,
            status = status,
            note = note
        )
}

data class Transaction(
    val transactionId: String,
    val transactionDate: LocalDateTime,
    val transactionType: String,
    val amount: BigDecimal,
    val fee: BigDecimal,
    val status: String,
    val recipientName: String? = null
) {
    fun getDetail(): String =
        "$transactionType - ${recipientName ?: "Transaksi"} - $status"
}

data class TransferReceipt(
    val receiptId: String,
    val transferId: String,
    val recipientName: String,
    val bankName: String,
    val accountNumber: String,
    val amount: BigDecimal,
    val status: String,
    val note: String
) {
    fun viewDetail(): String =
        "$recipientName / $bankName / $accountNumber / $amount / $status"

    fun exportHistory(): String = viewDetail()
}

class TransactionHistory(
    val transactions: MutableList<Transaction> = mutableListOf()
) {
    fun getHistory(): List<Transaction> = transactions.toList()

    fun filterTransaction(type: String): List<Transaction> =
        transactions.filter { it.transactionType == type }

    fun searchTransaction(keyword: String): List<Transaction> =
        search(keyword)

    fun viewDetail(transactionId: String): Transaction? =
        transactions.find { it.transactionId == transactionId }

    fun exportHistory(): String =
        transactions.joinToString("\n") { it.getDetail() }

    fun search(keyword: String): List<Transaction> {
        if (keyword.isBlank()) return transactions
        return transactions.filter {
            it.transactionId.contains(keyword, true) ||
            it.transactionType.contains(keyword, true) ||
            (it.recipientName?.contains(keyword, true) == true)
        }
    }
}

class Dashboard {
    var selectedMonth: String = "Bulan ini"
    var totalIncome: BigDecimal = BigDecimal.ZERO
    var totalExpense: BigDecimal = BigDecimal.ZERO

    fun displayBalance(account: Account): BigDecimal = account.balance

    fun displayChart(): String = "Grafik pemasukan dan pengeluaran"

    fun calculateIncome(history: TransactionHistory): BigDecimal =
        history.transactions
            .filter { it.transactionType == "Pemasukan" }
            .sumOf { it.amount }

    fun calculateExpense(history: TransactionHistory): BigDecimal =
        history.transactions
            .filter { it.transactionType == "Transfer" }
            .sumOf { it.amount }
}
