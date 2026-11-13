package com.diponegoro.digitalbank.account.domain

import java.text.NumberFormat
import java.util.Locale

enum class BankAccountType(val label: String) {
    TABUNGAN("Tabungan"), GIRO("Giro"), DEPOSITO("Deposito"), REKENING_VALAS("Rekening Valas"),
    TABUNGAN_PELAJAR("Tabungan Pelajar"), TABUNGAN_SYARIAH("Tabungan Syariah"), LAINNYA("Lainnya")
}
enum class BankAccountStatus(val label: String) {
    AKTIF("Aktif"), DORMANT("Dormant"), DIBLOKIR("Diblokir"), DALAM_PENUTUPAN("Dalam penutupan"), TERTUTUP("Tertutup")
}
enum class TransactionType { SETORAN, PENARIKAN, TRANSFER_MASUK, TRANSFER_KELUAR, PEMBAYARAN, PEMBELIAN, BUNGA, BIAYA_ADMIN, LAINNYA }
enum class TransactionStatus { BERHASIL, PENDING, GAGAL, DIBATALKAN }
enum class StatementFormat(val mime: String, val extension: String) {
    PDF("application/pdf", "pdf"), CSV("text/csv", "csv")
}
data class BankAccount(
    val id: String, val userId: String, val number: String, val type: BankAccountType,
    val alias: String, val currency: String, val balance: Long, val availableBalance: Long,
    val status: BankAccountStatus, val openedAt: Long, val closedAt: Long?, val isPrimary: Boolean
)
data class AccountTransaction(
    val id: String, val accountId: String, val createdAt: Long, val type: TransactionType,
    val description: String, val amount: Long, val balanceAfter: Long, val reference: String,
    val status: TransactionStatus
)
data class AccountSnapshot(val owner: String, val verified: Boolean, val accounts: List<BankAccount>)
data class StatementDocument(val name: String, val format: StatementFormat, val bytes: ByteArray)

object AccountRules {
    const val MAX_ACCOUNTS = 5
    val supportedTypes = listOf(BankAccountType.TABUNGAN, BankAccountType.GIRO, BankAccountType.TABUNGAN_SYARIAH)
    fun validateOpening(verified: Boolean, count: Int, type: BankAccountType, alias: String) {
        require(verified) { "Pembukaan rekening memerlukan e-KYC demo yang disetujui." }
        require(count < MAX_ACCOUNTS) { "Maksimal $MAX_ACCOUNTS rekening yang belum ditutup." }
        require(type in supportedTypes) { "Jenis rekening ini belum tersedia dalam simulasi IDR." }
        validateAlias(alias)
    }
    fun validateAlias(alias: String) {
        require(alias.trim().length in 1..40 && alias.none { it.isISOControl() }) {
            "Alias harus berisi 1–40 karakter tanpa karakter kontrol."
        }
    }
    fun validateClosing(account: BankAccount, hasPending: Boolean) {
        require(account.status == BankAccountStatus.AKTIF || account.status == BankAccountStatus.DORMANT) {
            "Status rekening tidak mengizinkan penutupan."
        }
        require(account.balance == 0L && account.availableBalance == 0L) { "Saldo harus Rp0 sebelum rekening ditutup." }
        require(!hasPending) { "Masih ada transaksi pending. Penutupan ditolak." }
    }
}

fun formatRupiah(amount: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    .apply { maximumFractionDigits = 0 }.format(amount)
