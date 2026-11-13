package com.diponegoro.digitalbank.virtualcard.domain.repository

import com.diponegoro.digitalbank.virtualcard.domain.model.CardResult
import com.diponegoro.digitalbank.virtualcard.domain.model.CardScope
import com.diponegoro.digitalbank.virtualcard.domain.model.CardStatus
import com.diponegoro.digitalbank.virtualcard.domain.model.VirtualCard

interface VirtualCardRepository {
    suspend fun getCards(userId: String): List<VirtualCard>
    suspend fun getCard(userId: String, cardId: String): VirtualCard?
    suspend fun createCard(userId: String, cardholderName: String, scope: CardScope): CardResult<VirtualCard>
    suspend fun updateStatus(userId: String, cardId: String, status: CardStatus): CardResult<VirtualCard>
    suspend fun updateLimit(userId: String, cardId: String, limit: Long): CardResult<VirtualCard>

    /**
     * Otorisasi transaksi kartu (simulasi jaringan kartu).
     * Ditolak bila kartu beku, nominal melebihi limit, atau transaksi luar negeri
     * dengan kartu berlingkup domestik.
     */
    suspend fun authorizeTransaction(
        userId: String, cardId: String, amount: Long, international: Boolean
    ): CardResult<Unit>

    /**
     * Buka detail kartu dengan PIN di sisi penerbit (varian online: server memverifikasi PIN dan
     * mendekripsi nomor & CVV). null = tidak didukung; use case memakai verifikasi PIN lokal.
     */
    suspend fun revealDetails(userId: String, cardId: String, pin: String): CardResult<VirtualCard>? = null
}

/**
 * Jembatan ke modul Authentication, supaya Virtual Card tidak bergantung langsung
 * pada detail implementasi modul lain.
 */
interface AuthGateway {
    /** e-KYC disetujui → transaksi finansial aktif (FR-02). */
    suspend fun isFinancialEnabled(userId: String): Boolean

    /** Verifikasi PIN transaksi, termasuk aturan blokir 3x salah (FR-04 / BR-02). */
    suspend fun verifyPin(userId: String, pin: String): CardResult<Unit>
}
