package com.diponegoro.digitalbank.core.ledger

import java.util.concurrent.TimeUnit

/**
 * Data simulasi untuk akun demo agar riwayat, grafik saldo, dan dashboard langsung berisi.
 * Semua pihak & nominal fiktif (SRS 2.5: data bersifat simulasi).
 */
object DemoData {
    const val DEMO_USER_ID = "user-demo"
    const val DEMO_ACCOUNT = "1400123456"

    /** Nasabah lain PT Diponegoro Digital Bank (tujuan transfer sesama bank & QRIS pribadi). */
    data class Customer(val userId: String, val name: String, val number: String)

    val customers = listOf(
        Customer("cust-andi", "ANDI PRATAMA", "1400000002"),
        Customer("cust-budi", "BUDI SANTOSO", "1400000003"),
        Customer("cust-citra", "CITRA LESTARI", "1400000004")
    )

    private fun daysAgo(now: Long, days: Int, hour: Int = 10) =
        now - TimeUnit.DAYS.toMillis(days.toLong()) + TimeUnit.HOURS.toMillis(hour.toLong() - 12)

    fun seed(ledger: Ledger, now: Long = System.currentTimeMillis()) {
        customers.forEach {
            ledger.ensureMainAccountLocked(it.userId, it.name, 2_000_000, daysAgo(now, 200), it.number)
        }
        val main = ledger.ensureMainAccountLocked(
            DEMO_USER_ID, "DEMO", 3_000_000, daysAgo(now, 150), DEMO_ACCOUNT
        )

        var seq = 0
        fun income(day: Int, amount: Long, from: String, title: String) = ledger.postLocked(
            PostingRequest(
                "seed:${seq++}", TxCategory.TRANSFER, title, amount,
                lines = listOf(ExternalAccounts.FUNDING to -amount, main.id to amount),
                debitUserId = null, creditUserId = DEMO_USER_ID,
                fromName = from, fromDetail = "Bank lain", toName = "DEMO", toDetail = DEMO_ACCOUNT,
                at = daysAgo(now, day, 9)
            )
        )

        fun expense(
            day: Int, amount: Long, category: TxCategory, title: String, to: String, detail: String,
            external: String, fee: Long = 0, extra: Map<String, String> = emptyMap()
        ) = ledger.postLocked(
            PostingRequest(
                "seed:${seq++}", category, title, amount, fee,
                lines = buildList {
                    add(main.id to -(amount + fee))
                    add(external to amount)
                    if (fee > 0) add(ExternalAccounts.FEE_INCOME to fee)
                },
                debitUserId = DEMO_USER_ID, creditUserId = null,
                fromName = "DEMO", fromDetail = DEMO_ACCOUNT, toName = to, toDetail = detail,
                extra = extra, at = daysAgo(now, day, 15)
            )
        )

        // Lima bulan terakhir: gaji bulanan + pengeluaran rutin.
        for (month in 4 downTo 0) {
            val base = month * 30 + 25
            income(base, 8_500_000, "PT SINAR TEMBALANG", "Gaji bulanan")
            expense(base - 1, 1_500_000, TxCategory.TRANSFER, "Transfer antarbank", "IBU KOS MAWAR", "BCA • 1234567890",
                ExternalAccounts.INTERBANK, fee = 2_500)
            expense(base - 3, 100_000, TxCategory.TOPUP, "Pulsa Telkomsel 100.000", "Telkomsel", "081234567890",
                ExternalAccounts.BILLER, fee = 1_500)
            expense(base - 5, 200_000, TxCategory.TOPUP, "Token listrik 200.000", "PLN", "12345678901",
                ExternalAccounts.BILLER, fee = 2_500)
            expense(base - 8, 45_000 + month * 7_000L, TxCategory.QRIS, "Pembayaran QRIS", "WARUNG BU SRI", "QRIS statis",
                ExternalAccounts.MERCHANT)
            expense(base - 12, 1_200_000 + month * 150_000L, TxCategory.QRIS, "Pembayaran QRIS",
                "TOKO BUKU DIPONEGORO", "QRIS dinamis", ExternalAccounts.MERCHANT)
            expense(base - 18, 750_000, TxCategory.TRANSFER, "Transfer antarbank", "ORANG TUA", "BRI • 0012345678",
                ExternalAccounts.INTERBANK, fee = 2_500)
        }
        income(3, 350_000, "ANDI PRATAMA", "Transfer masuk (patungan)")
    }
}
