package com.diponegoro.digitalbank.topup.presentation

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diponegoro.digitalbank.account.data.AccountRepository
import com.diponegoro.digitalbank.account.domain.BankAccount
import com.diponegoro.digitalbank.authentication.domain.model.Session
import com.diponegoro.digitalbank.topup.data.TopUpReceiptExporter
import com.diponegoro.digitalbank.topup.data.TopUpRepository
import com.diponegoro.digitalbank.topup.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class TopUpUiState(
    val loading: Boolean = true, val busy: Boolean = false, val verified: Boolean = false,
    val accounts: List<BankAccount> = emptyList(), val methods: List<PaymentMethod> = emptyList(),
    val history: List<TopUpTransaction> = emptyList(), val notifications: List<TopUpNotification> = emptyList(),
    val dailyLimit: Long = 2_000_000, val usedToday: Long = 0, val quote: TopUpQuote? = null,
    val selectedId: String? = null, val error: String? = null, val message: String? = null
)

class TopUpViewModel(
    private val repository: TopUpRepository, private val accounts: AccountRepository, private val session: Session
) : ViewModel() {
    private val mutableState = MutableStateFlow(TopUpUiState())
    val state = mutableState.asStateFlow()
    private val exportChannel = Channel<String>(Channel.BUFFERED)
    val exports = exportChannel.receiveAsFlow()
    private var exportId: String? = null
    private val refreshMutex = Mutex()
    init {
        viewModelScope.launch { accounts.changes.collect { if (!state.value.busy) refresh() } }
        viewModelScope.launch { while (true) { delay(30_000); if (!state.value.busy) refresh() } }
    }
    private suspend fun refresh() = refreshMutex.withLock {
        try {
            val bank = accounts.snapshot(session)
            val topUp = repository.snapshot(session)
            mutableState.update { it.copy(loading = false, verified = bank.verified, accounts = bank.accounts,
                methods = topUp.methods, history = topUp.history, notifications = topUp.notifications,
                dailyLimit = topUp.dailyLimit, usedToday = topUp.usedToday) }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            mutableState.update { TopUpUiState(loading = false, busy = it.busy, error = e.message ?: "Gagal memuat top-up.") }
        }
    }
    private fun perform(message: String? = null, block: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try { block(); refresh(); mutableState.update { it.copy(message = message) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutableState.update { it.copy(error = e.message ?: "Operasi top-up gagal.") } }
            finally { mutableState.update { it.copy(busy = false) } }
        }
    }
    fun reload() = perform { }
    fun preview(draft: TopUpDraft) = perform {
        val quote = repository.quote(session, draft)
        mutableState.update { it.copy(quote = quote, selectedId = null) }
    }
    fun dismissQuote() { if (!state.value.busy) mutableState.update { it.copy(quote = null, error = null) } }
    fun confirm(pin: String) {
        val quote = state.value.quote ?: return
        perform {
            val transaction = repository.begin(session, quote.draft, pin)
            mutableState.update { it.copy(quote = null, selectedId = transaction.id) }
        }
    }
    fun select(id: String?) { if (!state.value.busy) mutableState.update { it.copy(selectedId = id, error = null, message = null) } }
    fun settle(id: String, status: TopUpStatus) = perform {
        val transaction = repository.settle(session, id, status)
        mutableState.update { it.copy(selectedId = transaction.id) }
    }
    fun saveMethod(type: PaymentMethodType, name: String, identifier: String) = perform("Metode pembayaran demo disimpan.") {
        repository.saveMethod(session, type, name, identifier)
    }
    fun defaultMethod(id: String) = perform("Metode default diperbarui.") { repository.defaultMethod(session, id) }
    fun saveLimit(raw: String) = perform("Limit top-up harian diperbarui.") {
        require(Regex("[0-9]+").matches(raw)) { "Limit harus berupa angka rupiah penuh." }
        val amount = requireNotNull(raw.toLongOrNull()) { "Limit terlalu besar." }
        repository.saveLimit(session, amount)
    }
    fun export(id: String) = perform {
        val transaction = repository.receipt(session, id)
        exportId = id
        exportChannel.send("bukti-${transaction.reference}.pdf")
    }
    fun saveReceipt(resolver: ContentResolver, uri: Uri?) {
        if (uri == null) { exportId = null; return }
        viewModelScope.launch {
            state.first { !it.busy }
            perform("Bukti top-up PDF berhasil disimpan.") {
                val id = requireNotNull(exportId) { "Silakan pilih unduh bukti kembali." }
                val transaction = repository.receipt(session, id)
                withContext(Dispatchers.IO) {
                    val bytes = TopUpReceiptExporter.generate(transaction)
                    requireNotNull(resolver.openOutputStream(uri, "wt")) { "Lokasi berkas tidak dapat dibuka." }
                        .use { it.write(bytes) }
                }
                exportId = null
            }
        }
    }
}
