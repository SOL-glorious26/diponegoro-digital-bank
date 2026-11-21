package com.diponegoro.digitalbank.history.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.diponegoro.digitalbank.core.ui.DdbOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.core.ledger.Direction
import com.diponegoro.digitalbank.core.ledger.TxCategory
import com.diponegoro.digitalbank.core.ledger.TxStatus
import com.diponegoro.digitalbank.core.ui.EmptyState
import com.diponegoro.digitalbank.core.ui.GroupedBarChart
import com.diponegoro.digitalbank.core.ui.InfoRow
import com.diponegoro.digitalbank.core.ui.InfoText
import com.diponegoro.digitalbank.core.ui.ReceiptCard
import com.diponegoro.digitalbank.core.ui.ScreenScaffold
import com.diponegoro.digitalbank.core.ui.SectionCard
import com.diponegoro.digitalbank.core.util.formatRupiah
import com.diponegoro.digitalbank.history.data.DateRange
import com.diponegoro.digitalbank.home.TransactionRow
import com.diponegoro.digitalbank.ui.theme.LocalChartColors

@Composable
private fun <T> ChipRow(label: String, options: List<T?>, selected: T?, name: (T?) -> String, onSelect: (T?) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(name(option)) })
        }
    }
}

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    viewModel: HistoryViewModel,
    onOpen: (String) -> Unit,
    onAnalytics: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val filter = state.filter
    var showFilters by rememberSaveable { mutableStateOf(false) }
    ScreenScaffold(title = "Riwayat Transaksi", onBack = null, bottomBar = bottomBar) {
        OutlinedTextField(
            value = filter.query,
            onValueChange = { q -> viewModel.updateFilter { it.copy(query = q) } },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            label = { Text("Cari ID, nama, atau keterangan") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        ChipRow("Periode", DateRange.entries, filter.range, { it!!.label }) { r ->
            viewModel.updateFilter { it.copy(range = r ?: DateRange.ALL) }
        }
        // Bisa digeser bila layar sempit, agar label tombol tidak terpotong ke baris kedua.
        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { showFilters = !showFilters }) {
                Text(if (showFilters) "Sembunyikan filter lain" else "Filter jenis & status")
            }
            TextButton(onClick = viewModel::resetFilter) { Text("Reset") }
            TextButton(onClick = onAnalytics) { Text("Analitik") }
        }
        if (showFilters) {
            ChipRow("Jenis", listOf(null) + TxCategory.entries, filter.category, { it?.label ?: "Semua" }) { c ->
                viewModel.updateFilter { it.copy(category = c) }
            }
            ChipRow("Arah", listOf(null, Direction.IN, Direction.OUT), filter.direction, { it?.label ?: "Semua" }) { d ->
                viewModel.updateFilter { it.copy(direction = d) }
            }
            ChipRow("Status", listOf(null) + TxStatus.entries, filter.status, { it?.label ?: "Semua" }) { s ->
                viewModel.updateFilter { it.copy(status = s) }
            }
        }

        val loadError = state.page.error
        if (loadError != null) {
            // DEF-11: kegagalan memuat (mis. server tidak terjangkau) tidak boleh tampil sebagai "0 transaksi".
            Text(loadError, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        } else {
            Text(
                "${state.page.totalMatches} transaksi ditemukan",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SectionCard {
            if (state.page.items.isEmpty() && loadError == null) EmptyState("Tidak ada transaksi yang cocok dengan filter")
            state.page.items.forEach { item -> TransactionRow(item) { onOpen(item.tx.id) } }
        }
        if (state.page.hasMore) {
            OutlinedButton(onClick = viewModel::loadMore, modifier = Modifier.fillMaxWidth()) { Text("Muat lebih banyak") }
        }
    }
}

@Composable
fun TransactionDetailScreen(state: DetailUiState, onCheckStatus: () -> Unit, onBack: () -> Unit) {
    ScreenScaffold(title = "Detail Transaksi", onBack = onBack) {
        if (state.notFound) EmptyState("Transaksi tidak ditemukan")
        val detail = state.detail ?: return@ScreenScaffold
        val tx = detail.item.tx
        ReceiptCard(tx, headline = "${tx.title} (${detail.item.direction.label})")
        if (tx.status == TxStatus.PROCESSING) {
            OutlinedButton(onClick = onCheckStatus, enabled = !state.checking, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.checking) "Memeriksa…" else "Periksa ulang status")
            }
        }
        InfoText(state.message)
        SectionCard(title = "Catatan buku besar (double-entry)") {
            Text(
                "Setiap transaksi dicatat sebagai baris debit/kredit yang jumlahnya nol.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            detail.journal.forEach { line ->
                InfoRow(
                    "${line.accountId.take(18)}${if (line.memo == "refund") " (refund)" else ""}",
                    (if (line.amount > 0) "+" else "") + formatRupiah(line.amount)
                )
            }
            InfoRow("Total", formatRupiah(detail.journal.sumOf { it.amount }), emphasize = true)
        }
    }
}

@Composable
fun AnalyticsScreen(state: AnalyticsUiState, viewModel: AnalyticsViewModel, onBack: () -> Unit) {
    val colors = LocalChartColors.current
    var showTable by rememberSaveable { mutableStateOf(false) }
    ScreenScaffold(title = "Pemasukan & Pengeluaran", onBack = onBack) {
        val months = state.months
        if (months.isEmpty()) return@ScreenScaffold
        val selected = months[state.selected.coerceIn(0, months.lastIndex)]

        SectionCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya")
                }
                Text(selected.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = viewModel::nextMonth, enabled = state.selected < months.lastIndex || state.endMonthsBack > 0) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Bulan berikutnya")
                }
            }
            InfoRow("Total pemasukan", formatRupiah(selected.income), emphasize = true)
            InfoRow("Total pengeluaran", formatRupiah(selected.expense), emphasize = true)
            InfoRow("Selisih", formatRupiah(selected.net))
        }

        SectionCard(title = "6 bulan terakhir") {
            GroupedBarChart(
                labels = months.map { it.label.substringBefore(' ') },
                series = listOf("Pemasukan" to months.map { it.income }, "Pengeluaran" to months.map { it.expense }),
                colors = listOf(colors.income, colors.expense),
                selectedIndex = state.selected.coerceIn(0, months.lastIndex),
                onSelect = viewModel::select
            )
            TextButton(onClick = { showTable = !showTable }) { Text(if (showTable) "Sembunyikan tabel" else "Lihat sebagai tabel") }
            if (showTable) months.forEach { m ->
                InfoRow(m.label, "+${formatRupiah(m.income)} / −${formatRupiah(m.expense)}")
            }
        }
        Text(
            "Pengeluaran termasuk biaya transaksi. Perpindahan antar-kantong tidak dihitung.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
