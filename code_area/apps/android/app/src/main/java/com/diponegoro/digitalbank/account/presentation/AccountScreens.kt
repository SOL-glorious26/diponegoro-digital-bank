package com.diponegoro.digitalbank.account.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import com.diponegoro.digitalbank.core.ui.DdbButton as Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.diponegoro.digitalbank.core.ui.DdbOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.account.domain.BalancePeriod
import com.diponegoro.digitalbank.account.domain.LockRule
import com.diponegoro.digitalbank.account.domain.Pocket
import com.diponegoro.digitalbank.core.ui.ChartPoint
import com.diponegoro.digitalbank.core.ui.EmptyState
import com.diponegoro.digitalbank.core.ui.ErrorText
import com.diponegoro.digitalbank.core.ui.InfoRow
import com.diponegoro.digitalbank.core.ui.InfoText
import com.diponegoro.digitalbank.core.ui.LineChart
import com.diponegoro.digitalbank.core.ui.MoneyField
import com.diponegoro.digitalbank.core.ui.PinDialog
import com.diponegoro.digitalbank.core.ui.ScreenScaffold
import com.diponegoro.digitalbank.core.ui.SectionCard
import com.diponegoro.digitalbank.core.util.formatDate
import com.diponegoro.digitalbank.core.util.formatRupiah
import com.diponegoro.digitalbank.ui.theme.LocalChartColors
import java.util.Calendar

@Composable
fun BalanceChartScreen(state: BalanceChartUiState, onSelect: (BalancePeriod) -> Unit, onBack: () -> Unit) {
    var showTable by rememberSaveable { mutableStateOf(false) }
    ScreenScaffold(title = "Grafik Saldo", onBack = onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BalancePeriod.entries.forEach { p ->
                FilterChip(selected = state.period == p, onClick = { onSelect(p) }, label = { Text(p.label) })
            }
        }
        SectionCard(title = "Saldo rekening utama") {
            LineChart(
                points = state.points.map { ChartPoint(it.label, it.balance) },
                color = LocalChartColors.current.balance
            )
            Text(
                "Ketuk grafik untuk melihat saldo pada titik tertentu. Data dihitung dari buku besar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { showTable = !showTable }) { Text(if (showTable) "Sembunyikan tabel" else "Lihat sebagai tabel") }
            if (showTable) state.points.forEach { InfoRow(it.label, formatRupiah(it.balance)) }
        }
        if (state.points.size >= 2) {
            val change = state.points.last().balance - state.points.first().balance
            SectionCard {
                InfoRow("Perubahan periode ini", (if (change >= 0) "+" else "") + formatRupiah(change), emphasize = true)
            }
        }
    }
}

@Composable
fun PocketsScreen(
    state: PocketsUiState,
    viewModel: PocketsViewModel,
    onBack: (() -> Unit)?,
    bottomBar: @Composable () -> Unit = {}
) {
    ScreenScaffold(title = "Kantong Simpanan", onBack = onBack, bottomBar = bottomBar) {
        SectionCard {
            InfoRow("Saldo rekening utama", formatRupiah(state.mainBalance), emphasize = true)
            InfoRow("Total di kantong", formatRupiah(state.pockets.sumOf { it.balance }))
        }
        if (!state.financialEnabled) ErrorText("Selesaikan e-KYC untuk membuat dan mengisi kantong.")
        if (state.dialog == null) { ErrorText(state.error); InfoText(state.info) }
        Button(
            onClick = { viewModel.open(PocketDialog.Create) },
            enabled = state.financialEnabled,
            modifier = Modifier.fillMaxWidth()
        ) { Text("+ Buat kantong") }

        if (state.pockets.isEmpty()) EmptyState("Belum ada kantong. Buat kantong untuk menabung dengan tujuan tertentu.")
        state.pockets.forEach { pocket -> PocketCard(pocket, viewModel.lockDescription(pocket), viewModel::open) }
    }
    PocketDialogs(state, viewModel)
}

@Composable
private fun PocketCard(pocket: Pocket, lockText: String, open: (PocketDialog) -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pocket.name, style = MaterialTheme.typography.titleMedium)
                if (pocket.purpose.isNotBlank()) Text(pocket.purpose, style = MaterialTheme.typography.bodySmall)
            }
            if (pocket.locked) Icon(Icons.Filled.Lock, contentDescription = "Terkunci")
        }
        Text(formatRupiah(pocket.balance), style = MaterialTheme.typography.headlineSmall)
        pocket.target?.let { target ->
            LinearProgressIndicator(progress = { pocket.progress }, modifier = Modifier.fillMaxWidth())
            Text("${(pocket.progress * 100).toInt()}% dari target ${formatRupiah(target)}", style = MaterialTheme.typography.bodySmall)
        }
        Text(lockText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { open(PocketDialog.Deposit(pocket)) }) { Text("Isi") }
            OutlinedButton(onClick = { open(PocketDialog.Withdraw(pocket)) }, enabled = pocket.balance > 0) {
                Text(if (pocket.locked) "Tarik dini" else "Tarik")
            }
            OutlinedButton(onClick = { open(PocketDialog.Edit(pocket)) }) { Text("Ubah") }
            if (!pocket.locked) OutlinedButton(onClick = { open(PocketDialog.Lock(pocket)) }) { Text("Kunci") }
            if (!pocket.locked) OutlinedButton(onClick = { open(PocketDialog.Delete(pocket)) }) { Text("Hapus") }
        }
    }
}

private val lockMonths = listOf(1, 3, 6, 12)

private fun monthsFromNow(months: Int): Long = Calendar.getInstance().apply {
    add(Calendar.MONTH, months)
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Pilihan kunci: tanpa kunci, N bulan, atau sampai target. Mengembalikan -1 = target, 0 = tanpa kunci. */
@Composable
private fun LockPicker(selected: Int, onSelect: (Int) -> Unit) {
    Text("Kunci kantong", style = MaterialTheme.typography.labelLarge)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = selected == 0, onClick = { onSelect(0) }, label = { Text("Tidak") })
        lockMonths.forEach { m -> FilterChip(selected = selected == m, onClick = { onSelect(m) }, label = { Text("$m bln") }) }
        FilterChip(selected = selected == -1, onClick = { onSelect(-1) }, label = { Text("Sampai target") })
    }
    if (selected > 0) Text("Terkunci sampai ${formatDate(monthsFromNow(selected))}", style = MaterialTheme.typography.bodySmall)
}

private fun lockRule(choice: Int): LockRule? = when {
    choice == -1 -> LockRule.UntilTarget
    choice > 0 -> LockRule.UntilDate(monthsFromNow(choice))
    else -> null
}

@Composable
private fun PocketDialogs(state: PocketsUiState, vm: PocketsViewModel) {
    when (val dialog = state.dialog) {
        null -> Unit
        PocketDialog.Create -> {
            var name by rememberSaveable { mutableStateOf("") }
            var purpose by rememberSaveable { mutableStateOf("") }
            var target by rememberSaveable { mutableStateOf("") }
            var lock by rememberSaveable { mutableIntStateOf(0) }
            FormDialog("Buat Kantong", state, "Buat", onConfirm = { vm.create(name, purpose, target, lockRule(lock)) }, onDismiss = vm::dismiss) {
                OutlinedTextField(name, { name = it.take(30) }, label = { Text("Nama kantong") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(purpose, { purpose = it.take(60) }, label = { Text("Fungsi / tujuan (opsional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                MoneyField(target, { target = it }, "Target (opsional)")
                LockPicker(lock) { lock = it }
            }
        }
        is PocketDialog.Edit -> {
            val p = dialog.pocket
            var name by rememberSaveable { mutableStateOf(p.name) }
            var purpose by rememberSaveable { mutableStateOf(p.purpose) }
            var target by rememberSaveable { mutableStateOf(p.target?.toString() ?: "") }
            FormDialog("Ubah Kantong", state, "Simpan", onConfirm = { vm.update(p, name, purpose, target) }, onDismiss = vm::dismiss) {
                OutlinedTextField(name, { name = it.take(30) }, label = { Text("Nama kantong") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(purpose, { purpose = it.take(60) }, label = { Text("Fungsi / tujuan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                MoneyField(target, { target = it }, "Target (opsional)")
            }
        }
        is PocketDialog.Deposit -> {
            var amount by rememberSaveable { mutableStateOf("") }
            FormDialog("Isi ${dialog.pocket.name}", state, "Pindahkan", onConfirm = { vm.deposit(dialog.pocket, amount) }, onDismiss = vm::dismiss) {
                Text("Saldo rekening utama: ${formatRupiah(state.mainBalance)}")
                MoneyField(amount, { amount = it }, "Nominal")
            }
        }
        is PocketDialog.Withdraw -> {
            var amount by rememberSaveable { mutableStateOf("") }
            FormDialog("Tarik dari ${dialog.pocket.name}", state, "Lanjut", onConfirm = { vm.withdraw(dialog.pocket, amount) }, onDismiss = vm::dismiss) {
                Text("Saldo kantong: ${formatRupiah(dialog.pocket.balance)}")
                if (dialog.pocket.locked) Text(
                    "Kantong ini terkunci. Penarikan dini memerlukan konfirmasi tambahan dan PIN.",
                    color = MaterialTheme.colorScheme.error
                )
                MoneyField(amount, { amount = it }, "Nominal")
            }
        }
        is PocketDialog.EarlyWithdrawConfirm -> AlertDialog(
            onDismissRequest = vm::dismiss,
            icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            title = { Text("Konfirmasi penarikan dini") },
            text = {
                Text(
                    "Anda akan menarik ${formatRupiah(dialog.amount)} dari kantong \"${dialog.pocket.name}\" " +
                        "yang masih terkunci (${vm.lockDescription(dialog.pocket)}). Lanjutkan?"
                )
            },
            confirmButton = { TextButton(onClick = { vm.confirmEarlyWithdraw(dialog.pocket, dialog.amount) }) { Text("Ya, tarik dini") } },
            dismissButton = { TextButton(onClick = vm::dismiss) { Text("Batal") } }
        )
        is PocketDialog.EarlyWithdrawPin -> PinDialog(
            subtitle = "Penarikan dini ${formatRupiah(dialog.amount)} dari ${dialog.pocket.name}",
            errorMessage = state.error,
            isLoading = state.busy,
            onSubmit = { vm.earlyWithdraw(dialog.pocket, dialog.amount, it) },
            onDismiss = vm::dismiss
        )
        is PocketDialog.Lock -> {
            var lock by rememberSaveable { mutableIntStateOf(1) }
            FormDialog("Kunci ${dialog.pocket.name}", state, "Kunci", onConfirm = { lockRule(lock)?.let { vm.lock(dialog.pocket, it) } }, onDismiss = vm::dismiss) {
                Text("Dana terkunci tidak dapat ditarik sebelum tanggal/target tercapai, kecuali penarikan dini dengan PIN.")
                LockPicker(lock) { if (it != 0) lock = it }
            }
        }
        is PocketDialog.Delete -> FormDialog(
            "Hapus ${dialog.pocket.name}?", state, "Hapus",
            onConfirm = { vm.delete(dialog.pocket) }, onDismiss = vm::dismiss
        ) {
            Text("Sisa saldo ${formatRupiah(dialog.pocket.balance)} akan dipindahkan ke rekening utama.")
        }
    }
}

@Composable
private fun FormDialog(
    title: String,
    state: PocketsUiState,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!state.busy) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
                ErrorText(state.error)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = !state.busy) { Text(if (state.busy) "Memproses…" else confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.busy) { Text("Batal") } }
    )
}
