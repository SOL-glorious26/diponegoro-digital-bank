package com.diponegoro.digitalbank.account.presentation

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diponegoro.digitalbank.account.data.AccountRepository
import com.diponegoro.digitalbank.account.domain.*
import com.diponegoro.digitalbank.authentication.domain.model.Session
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class AccountUiState(
    val loading: Boolean = true, val busy: Boolean = false, val owner: String = "",
    val verified: Boolean = false, val accounts: List<BankAccount> = emptyList(),
    val transactions: List<AccountTransaction> = emptyList(), val error: String? = null,
    val message: String? = null
)

class AccountViewModel(private val repository: AccountRepository, private val session: Session) : ViewModel() {
    private val mutableState = MutableStateFlow(AccountUiState())
    val state = mutableState.asStateFlow()
    private val exportRequests = Channel<StatementDocument>(Channel.BUFFERED)
    val exports = exportRequests.receiveAsFlow()
    private var pendingExport: StatementDocument? = null
    private var historyQuery: Triple<String, Long, Long>? = null

    init { viewModelScope.launch { repository.changes.collect { refresh() } } }

    private suspend fun refresh() {
        try {
            val snapshot = repository.snapshot(session)
            val history = historyQuery?.let { repository.history(session, it.first, it.second, it.third) }.orEmpty()
            mutableState.update { it.copy(loading = false, owner = snapshot.owner, verified = snapshot.verified,
                accounts = snapshot.accounts, transactions = history) }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { mutableState.update { it.copy(loading = false, accounts = emptyList(),
            transactions = emptyList(), owner = "", verified = false, error = e.message ?: "Gagal memuat rekening.") } }
    }
    private fun perform(message: String?, block: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try { block(); refresh(); mutableState.update { it.copy(message = message) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutableState.update { it.copy(error = e.message ?: "Operasi rekening gagal.") } }
            finally { mutableState.update { it.copy(busy = false) } }
        }
    }
    fun reload() = perform(null) { }
    fun open(type: BankAccountType, alias: String, operation: String) = perform("Rekening demo berhasil dibuka dengan saldo Rp0.") {
        repository.open(session, type, alias, operation)
    }
    fun rename(id: String, alias: String) = perform("Alias rekening diperbarui.") { repository.rename(session, id, alias) }
    fun primary(id: String) = perform("Rekening utama diperbarui.") { repository.setPrimary(session, id) }
    fun close(id: String) = perform("Rekening berhasil ditutup.") { repository.close(session, id) }
    fun history(id: String, start: String, end: String) = perform(null) {
        if (historyQuery?.first != id) {
            historyQuery = null
            mutableState.update { it.copy(transactions = emptyList()) }
        }
        val period = parsePeriod(start, end)
        val rows = repository.history(session, id, period.first, period.second)
        historyQuery = Triple(id, period.first, period.second)
        mutableState.update { it.copy(transactions = rows) }
    }
    fun export(id: String, start: String, end: String, format: StatementFormat) = perform(null) {
        val period = parsePeriod(start, end)
        val document = repository.statement(session, id, period.first, period.second, format)
        pendingExport = document
        exportRequests.send(document)
    }
    fun saveExport(resolver: ContentResolver, uri: Uri?) {
        if (uri == null) { pendingExport = null; return }
        perform("e-Statement berhasil disimpan.") {
            val document = requireNotNull(pendingExport) { "Dokumen tidak tersedia. Silakan ekspor ulang." }
            repository.snapshot(session) // Periksa kembali sesi sebelum menulis dokumen.
            withContext(Dispatchers.IO) {
                requireNotNull(resolver.openOutputStream(uri, "wt")) { "Lokasi berkas tidak dapat dibuka." }
                    .use { it.write(document.bytes) }
            }
            pendingExport = null
        }
    }
    companion object {
        fun parsePeriod(start: String, end: String): Pair<Long, Long> {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("id-ID")).apply { isLenient = false }
            fun parse(value: String): Long {
                require(Regex("\\d{2}/\\d{2}/\\d{4}").matches(value)) { "Gunakan tanggal dd/MM/yyyy." }
                val date = requireNotNull(runCatching { format.parse(value) }.getOrNull()) { "Tanggal tidak valid." }
                return date.time
            }
            val first = parse(start)
            val last = parse(end)
            require(first <= last) { "Tanggal mulai harus sebelum atau sama dengan tanggal selesai." }
            val exclusive = Calendar.getInstance().apply { timeInMillis = last; add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
            return first to exclusive
        }
    }
}
