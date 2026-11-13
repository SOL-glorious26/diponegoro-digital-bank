package com.diponegoro.digitalbank.transfer.data

import com.diponegoro.digitalbank.transfer.domain.Bank
import com.diponegoro.digitalbank.transfer.domain.NetworkBehavior

/**
 * Simulator jaringan antarbank (SRS 2.5: layanan eksternal diganti simulator lewat adapter).
 * Direktori rekening diambil dari data uji modul Transfer versi awal, ditambah akun untuk skenario gagal.
 */
object InterbankSimulator {

    val banks = listOf(
        Bank(Bank.INTERNAL_CODE, "Diponegoro Digital Bank"),
        Bank("BCA", "BCA"),
        Bank("BRI", "BRI"),
        Bank("BNI", "BNI"),
        Bank("MANDIRI", "Bank Mandiri"),
        Bank("BSI", "Bank Syariah Indonesia"),
        Bank("CIMB", "CIMB Niaga"),
        Bank("PERMATA", "Bank Permata"),
        Bank("BTN", "BTN"),
        Bank("DANAMON", "Bank Danamon")
    )

    private data class Entry(val bankCode: String, val number: String, val name: String, val behavior: NetworkBehavior)

    private val directory = listOf(
        Entry("BCA", "1234567890", "ANDI WIJAYA", NetworkBehavior.ACCEPT),
        Entry("BCA", "1234567891", "IBU KOS MAWAR", NetworkBehavior.ACCEPT),
        Entry("BRI", "9876543210", "BUDI HARTONO", NetworkBehavior.ACCEPT),
        Entry("BRI", "0012345678", "ORANG TUA", NetworkBehavior.ACCEPT),
        Entry("BNI", "1122334455", "CITRA AYU", NetworkBehavior.ACCEPT),
        Entry("MANDIRI", "5566778899", "DIMAS ANGGARA", NetworkBehavior.ACCEPT),
        Entry("BSI", "7001234567", "SITI AMINAH", NetworkBehavior.ACCEPT),
        Entry("CIMB", "8001234567", "KEVIN SANJAYA", NetworkBehavior.ACCEPT),
        // Skenario uji A6: transfer ditolak jaringan → dana dikembalikan otomatis.
        Entry("BRI", "9999999999", "RINA (SIMULASI DITOLAK)", NetworkBehavior.REJECT),
        // Skenario uji A5: waktu habis → status diproses, cek ulang tanpa debit ganda.
        Entry("MANDIRI", "8888888888", "TONI (SIMULASI TIMEOUT)", NetworkBehavior.TIMEOUT)
    )

    fun bank(code: String): Bank? = banks.firstOrNull { it.code == code }

    fun lookup(bankCode: String, accountNumber: String): Pair<String, NetworkBehavior>? =
        directory.firstOrNull { it.bankCode == bankCode && it.number == accountNumber }
            ?.let { it.name to it.behavior }
}
