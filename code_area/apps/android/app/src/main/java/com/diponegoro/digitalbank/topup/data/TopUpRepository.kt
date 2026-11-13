package com.diponegoro.digitalbank.topup.data

import com.diponegoro.digitalbank.core.ledger.LedgerTransaction
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.topup.domain.BillInquiry
import com.diponegoro.digitalbank.topup.domain.Biller
import com.diponegoro.digitalbank.topup.domain.EWallet
import com.diponegoro.digitalbank.topup.domain.PlnCustomer
import com.diponegoro.digitalbank.topup.domain.Product
import com.diponegoro.digitalbank.topup.domain.TopUpOrder

/**
 * Modul Top-up (FR-18 – FR-20). Implementasi:
 *  - [LocalTopUpRepository]  : simulator penyedia + ledger in-memory (varian demo)
 *  - [RemoteTopUpRepository] : backend /api/v1/topup (varian online)
 */
interface TopUpRepository {
    suspend fun orderPulsa(phone: String, nominal: Long): AppResult<TopUpOrder>
    suspend fun orderData(phone: String, product: Product): AppResult<TopUpOrder>
    suspend fun inquirePln(rawMeter: String): AppResult<PlnCustomer>
    suspend fun orderPln(customer: PlnCustomer, nominal: Long): AppResult<TopUpOrder>
    suspend fun inquireBill(biller: Biller, rawNumber: String): AppResult<BillInquiry>
    suspend fun orderBill(inquiry: BillInquiry): AppResult<TopUpOrder>
    suspend fun orderEwallet(wallet: EWallet, phone: String, rawAmount: String): AppResult<TopUpOrder>
    suspend fun execute(userId: String, order: TopUpOrder, pin: String, idempotencyKey: String): AppResult<LedgerTransaction>
}
