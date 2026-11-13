package com.example.diponegorodigitalbank

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class BankRepository {
    val user = User(
        userId = "U001",
        username = "user",
        email = "user@diponegoro.ac.id",
        phoneNumber = "081234567890",
        pin = "123456"
    )

    val history = TransactionHistory()

    private val recipients = listOf(
        Recipient("R001", "BCA", "1234567890", "Andi"),
        Recipient("R002", "BRI", "9876543210", "Budi"),
        Recipient("R003", "BNI", "1122334455", "Citra"),
        Recipient("R004", "Mandiri", "5566778899", "Dimas")
    )

    init {
        user.accounts.add(
            Account(
                accountNumber = "1400012345678",
                bankName = "Diponegoro Digital Bank",
                balance = BigDecimal("10000000")
            )
        )

        history.transactions.add(
            Transaction(
                transactionId = "TRX-001",
                transactionDate = LocalDateTime.now().minusDays(1),
                transactionType = "Pemasukan",
                amount = BigDecimal("2500000"),
                fee = BigDecimal.ZERO,
                status = "Berhasil",
                recipientName = "Gaji"
            )
        )
    }

    fun login(username: String, pin: String): Boolean =
        user.login(username, pin)

    fun validateAccount(bank: String, accountNumber: String): Recipient? =
        recipients.firstOrNull {
            it.bankName.equals(bank.trim(), true) &&
            it.accountNumber == accountNumber.trim()
        }

    fun processTransfer(
        bank: String,
        accountNumber: String,
        amount: BigDecimal?,
        note: String,
        pin: String
    ): Result<TransferReceipt> {
        if (amount == null || amount <= BigDecimal.ZERO) {
            return Result.failure(IllegalArgumentException("Nominal transfer tidak valid."))
        }

        if (pin.length != 6) {
            return Result.failure(IllegalArgumentException("PIN harus 6 digit."))
        }

        if (!user.verifyPIN(pin)) {
            return Result.failure(IllegalArgumentException("PIN salah."))
        }

        val recipient = validateAccount(bank, accountNumber)
            ?: return Result.failure(IllegalArgumentException("Rekening tidak ditemukan."))

        val account = user.accounts.first()

        if (amount > account.balance) {
            return Result.failure(IllegalArgumentException("Saldo tidak mencukupi."))
        }

        if (!account.debit(amount)) {
            return Result.failure(IllegalArgumentException("Transfer gagal diproses."))
        }

        val transferId = "TRX-" + UUID.randomUUID().toString().take(8).uppercase()

        val transfer = Transfer(
            transferId = transferId,
            amount = amount,
            note = note,
            recipient = recipient
        )

        transfer.processTransfer()

        history.transactions.add(
            Transaction(
                transactionId = transferId,
                transactionDate = transfer.transferDate,
                transactionType = "Transfer",
                amount = amount,
                fee = BigDecimal.ZERO,
                status = transfer.status,
                recipientName = recipient.recipientName
            )
        )

        return Result.success(transfer.generateReceipt())
    }
}
