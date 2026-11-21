package com.diponegoro.digitalbank.transfer.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import com.diponegoro.digitalbank.core.ui.DdbButton as Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.diponegoro.digitalbank.core.ui.DdbOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.core.ledger.TxStatus
import com.diponegoro.digitalbank.core.ui.EmptyState
import com.diponegoro.digitalbank.core.ui.ErrorText
import com.diponegoro.digitalbank.core.ui.InfoRow
import com.diponegoro.digitalbank.core.ui.InfoText
import com.diponegoro.digitalbank.core.ui.MoneyField
import com.diponegoro.digitalbank.core.ui.PinDialog
import com.diponegoro.digitalbank.core.ui.ReceiptCard
import com.diponegoro.digitalbank.core.ui.ScreenScaffold
import com.diponegoro.digitalbank.core.ui.SectionCard
import com.diponegoro.digitalbank.core.util.formatRupiah
import com.diponegoro.digitalbank.transfer.domain.Bank
import com.diponegoro.digitalbank.transfer.domain.TransferRules

@Composable
fun TransferScreen(state: TransferUiState, vm: TransferViewModel, onClose: () -> Unit, onOpenDetail: (String) -> Unit) {
    BackHandler(enabled = state.step == TransferStep.AMOUNT || state.step == TransferStep.CONFIRM) { vm.back() }
    val title = when (state.step) {
        TransferStep.RECIPIENT -> "Transfer • Penerima"
        TransferStep.AMOUNT -> "Transfer • Nominal"
        TransferStep.CONFIRM -> "Transfer • Konfirmasi"
        TransferStep.RESULT -> "Bukti Transfer"
    }
    ScreenScaffold(title = title, onBack = { if (!vm.back()) onClose() }) {
        when (state.step) {
            TransferStep.RECIPIENT -> RecipientStep(state, vm)
            TransferStep.AMOUNT -> AmountStep(state, vm)
            TransferStep.CONFIRM -> ConfirmStep(state, vm)
            TransferStep.RESULT -> ResultStep(state, vm, onClose, onOpenDetail)
        }
    }
    if (state.showPin) {
        PinDialog(
            subtitle = state.quote?.let { "Transfer ${formatRupiah(it.total)} ke ${it.recipient.name}" },
            errorMessage = state.error,
            isLoading = state.busy,
            onSubmit = vm::submit,
            onDismiss = vm::dismissPin
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipientStep(state: TransferUiState, vm: TransferViewModel) {
    var bankCode by rememberSaveable { mutableStateOf(Bank.INTERNAL_CODE) }
    var number by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val bank = state.banks.firstOrNull { it.code == bankCode }

    SectionCard(title = "Rekening tujuan") {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = bank?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Bank tujuan") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.banks.forEach { b ->
                    DropdownMenuItem(
                        text = { Text(if (b.isInternal) "${b.name} (sesama, gratis)" else b.name) },
                        onClick = { bankCode = b.code; expanded = false; vm.clearMessages() }
                    )
                }
            }
        }
        OutlinedTextField(
            value = number,
            onValueChange = { number = it.filter(Char::isDigit).take(16); vm.clearMessages() },
            label = { Text("Nomor rekening") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        ErrorText(state.error)
        Button(onClick = { vm.inquire(bankCode, number) }, modifier = Modifier.fillMaxWidth()) { Text("Cek rekening") }
        Text(
            if (bankCode == Bank.INTERNAL_CODE) "Transfer sesama Diponegoro gratis dan langsung masuk."
            else "Biaya antarbank ${formatRupiah(TransferRules.INTERBANK_FEE)}. Minimal ${formatRupiah(TransferRules.MIN_AMOUNT)}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    SectionCard(title = "Penerima favorit") {
        if (state.favorites.isEmpty()) EmptyState("Belum ada favorit. Simpan penerima setelah cek rekening.")
        state.favorites.forEach { fav ->
            Row(
                Modifier.fillMaxWidth().clickable { vm.selectRecipient(fav.recipient) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(fav.recipient.name, style = MaterialTheme.typography.bodyLarge)
                    Text("${fav.recipient.bank.name} • ${fav.recipient.accountNumber}", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { vm.removeFavorite(fav.id) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Hapus ${fav.recipient.name} dari favorit")
                }
            }
        }
    }
}

@Composable
private fun AmountStep(state: TransferUiState, vm: TransferViewModel) {
    val recipient = state.recipient ?: return
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    SectionCard(title = "Penerima") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(recipient.name, style = MaterialTheme.typography.titleMedium)
                Text("${recipient.bank.name} • ${recipient.accountNumber}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = vm::toggleFavorite) {
                Icon(
                    if (state.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (state.isFavorite) "Hapus dari favorit" else "Simpan ke favorit"
                )
            }
        }
    }
    InfoText(state.info)
    MoneyField(amount, { amount = it; vm.clearMessages() }, "Nominal transfer",
        supportingText = "Minimal ${formatRupiah(TransferRules.MIN_AMOUNT)}")
    OutlinedTextField(
        value = note,
        onValueChange = { note = it.take(TransferRules.MAX_NOTE) },
        label = { Text("Catatan (opsional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    ErrorText(state.error)
    Button(onClick = { vm.review(amount, note) }, modifier = Modifier.fillMaxWidth()) { Text("Lanjut") }
}

@Composable
private fun ConfirmStep(state: TransferUiState, vm: TransferViewModel) {
    val q = state.quote ?: return
    SectionCard(title = "Periksa kembali") {
        InfoRow("Penerima", q.recipient.name)
        InfoRow("Bank", q.recipient.bank.name)
        InfoRow("Rekening", q.recipient.accountNumber)
        InfoRow("Nominal", formatRupiah(q.amount))
        InfoRow("Biaya", formatRupiah(q.fee))
        InfoRow("Total", formatRupiah(q.total), emphasize = true)
        if (q.note.isNotBlank()) InfoRow("Catatan", q.note)
        InfoRow("Saldo setelah", formatRupiah(q.balance - q.total))
    }
    Button(onClick = vm::requestPin, modifier = Modifier.fillMaxWidth()) { Text("Transfer sekarang") }
    OutlinedButton(onClick = { vm.back() }, modifier = Modifier.fillMaxWidth()) { Text("Ubah nominal") }
}

@Composable
private fun ResultStep(state: TransferUiState, vm: TransferViewModel, onClose: () -> Unit, onOpenDetail: (String) -> Unit) {
    val tx = state.result ?: return
    val headline = when (tx.status) {
        TxStatus.SUCCESS -> "Transfer berhasil"
        TxStatus.PROCESSING -> "Transfer sedang diproses"
        TxStatus.FAILED -> "Transfer gagal — dana dikembalikan"
    }
    ReceiptCard(tx, headline)
    if (tx.status == TxStatus.PROCESSING) {
        Text(
            "Jaringan antarbank belum merespons. Saldo sudah ditahan; periksa ulang status — tidak akan terjadi debit ganda.",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(onClick = vm::checkStatus, modifier = Modifier.fillMaxWidth()) { Text("Periksa ulang status") }
    }
    InfoText(state.info)
    ErrorText(state.error)
    OutlinedButton(onClick = { onOpenDetail(tx.id) }, modifier = Modifier.fillMaxWidth()) { Text("Lihat detail transaksi") }
    OutlinedButton(onClick = vm::restart, modifier = Modifier.fillMaxWidth()) { Text("Transfer lagi") }
    Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Kembali ke beranda") }
}
