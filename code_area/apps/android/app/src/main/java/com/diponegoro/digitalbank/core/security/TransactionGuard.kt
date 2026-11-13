package com.diponegoro.digitalbank.core.security

import com.diponegoro.digitalbank.authentication.domain.model.AccountStatus
import com.diponegoro.digitalbank.authentication.domain.model.AuthResult
import com.diponegoro.digitalbank.authentication.domain.repository.AuthRepository
import com.diponegoro.digitalbank.core.util.AppResult

/**
 * Pemeriksaan yang wajib dilewati setiap transaksi keuangan:
 * - FR-02: transaksi keuangan hanya aktif setelah e-KYC disetujui.
 * - FR-04 / BR-02: PIN 6 digit, blokir sementara setelah 3 kali salah (logika di AuthRepository).
 */
class TransactionGuard(private val auth: AuthRepository) {

    suspend fun requireFinancial(userId: String): AppResult<Unit> {
        val user = auth.getUser(userId) ?: return AppResult.Failure("Pengguna tidak ditemukan")
        return if (user.accountStatus == AccountStatus.FINANCIAL_ENABLED) AppResult.Success(Unit)
        else AppResult.Failure(KYC_REQUIRED)
    }

    suspend fun isFinancialEnabled(userId: String): Boolean = requireFinancial(userId) is AppResult.Success

    suspend fun verifyPin(userId: String, pin: String): AppResult<Unit> {
        if (pin.length != 6 || !pin.all { it.isDigit() }) return AppResult.Failure("PIN harus 6 digit angka")
        return when (val r = auth.validatePin(userId, pin)) {
            is AuthResult.Success -> AppResult.Success(Unit)
            is AuthResult.Failure -> AppResult.Failure(r.message)
        }
    }

    /** e-KYC lalu PIN, berurutan. */
    suspend fun authorize(userId: String, pin: String): AppResult<Unit> {
        val financial = requireFinancial(userId)
        if (financial is AppResult.Failure) return financial
        return verifyPin(userId, pin)
    }

    companion object {
        const val KYC_REQUIRED = "Transaksi keuangan belum aktif. Selesaikan e-KYC terlebih dahulu."
    }
}
