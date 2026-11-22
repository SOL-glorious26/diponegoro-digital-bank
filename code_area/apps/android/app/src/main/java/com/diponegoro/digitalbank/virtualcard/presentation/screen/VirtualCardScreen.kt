package com.diponegoro.digitalbank.virtualcard.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import com.diponegoro.digitalbank.core.ui.DdbOutlinedButton as OutlinedButton
import com.diponegoro.digitalbank.core.ui.ScreenScaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.authentication.presentation.components.AuthTextField
import com.diponegoro.digitalbank.authentication.presentation.components.LoadingButton
import com.diponegoro.digitalbank.authentication.presentation.components.MessageBanner
import com.diponegoro.digitalbank.authentication.presentation.components.PinField
import com.diponegoro.digitalbank.virtualcard.domain.model.CardScope
import com.diponegoro.digitalbank.virtualcard.domain.model.CardStatus
import com.diponegoro.digitalbank.virtualcard.domain.model.VirtualCard
import com.diponegoro.digitalbank.virtualcard.domain.model.VirtualCardLimits
import com.diponegoro.digitalbank.virtualcard.domain.usecase.formatRupiah
import com.diponegoro.digitalbank.virtualcard.presentation.state.CardDialog
import com.diponegoro.digitalbank.virtualcard.presentation.state.VirtualCardUiState

private fun CardScope.label() = when (this) {
    CardScope.DOMESTIC -> "Domestik"
    CardScope.INTERNATIONAL -> "Internasional"
}

private fun CardStatus.label() = when (this) {
    CardStatus.ACTIVE -> "Aktif"
    CardStatus.FROZEN -> "Dibekukan"
}

@Composable
fun VirtualCardScreen(
    state: VirtualCardUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onConfirmCreate: (CardScope) -> Unit,
    onViewDetails: (cardId: String) -> Unit,
    onSubmitPin: (String) -> Unit,
    onHideDetails: () -> Unit,
    onFreeze: (cardId: String) -> Unit,
    onActivate: (cardId: String) -> Unit,
    onOpenLimit: (cardId: String) -> Unit,
    onSaveLimit: (String) -> Unit,
    onOpenSimulate: (cardId: String) -> Unit,
    onSimulate: (amount: String, international: Boolean) -> Unit,
    onDismissDialog: () -> Unit
) {
    ScreenScaffold(title = "Kartu", onBack = onBack) {
        Text(
            "Buat dan kelola kartu virtual untuk transaksi online",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!state.financialEnabled) {
            Text(
                "Transaksi finansial belum aktif. Selesaikan e-KYC untuk membuat atau mengaktifkan kartu.",
                color = MaterialTheme.colorScheme.error
            )
        }

        if (state.dialog == null) MessageBanner(state.errorMessage, state.infoMessage)

        LoadingButton(
            text = "+ Buat kartu virtual",
            isLoading = state.isLoading && state.dialog == null,
            enabled = state.financialEnabled,
            onClick = onCreate
        )

        if (state.cards.isEmpty()) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Belum ada kartu virtual.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        state.cards.forEach { card ->
            VirtualCardItem(
                card = card,
                revealed = card.cardId == state.revealedCardId,
                enabled = !state.isLoading,
                onViewDetails = { onViewDetails(card.cardId) },
                onHideDetails = onHideDetails,
                onFreeze = { onFreeze(card.cardId) },
                onActivate = { onActivate(card.cardId) },
                onOpenLimit = { onOpenLimit(card.cardId) },
                onOpenSimulate = { onOpenSimulate(card.cardId) }
            )
        }
    }

    when (val dialog = state.dialog) {
        null -> Unit
        CardDialog.CreateCard -> CreateCardDialog(state, onConfirmCreate, onDismissDialog)
        is CardDialog.EnterPin -> PinDialog(state, onSubmitPin, onDismissDialog)
        is CardDialog.SetLimit -> LimitDialog(state, dialog.currentLimit, onSaveLimit, onDismissDialog)
        is CardDialog.SimulateTransaction -> SimulateDialog(state, onSimulate, onDismissDialog)
    }
}

@Composable
private fun VirtualCardItem(
    card: VirtualCard,
    revealed: Boolean,
    enabled: Boolean,
    onViewDetails: () -> Unit,
    onHideDetails: () -> Unit,
    onFreeze: () -> Unit,
    onActivate: () -> Unit,
    onOpenLimit: () -> Unit,
    onOpenSimulate: () -> Unit
) {
    val frozen = card.status == CardStatus.FROZEN
    val colors = if (frozen) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), colors = colors) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Diponegoro Virtual", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${card.usageScope.label()} · ${card.status.label()}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(
                    text = if (revealed) card.formattedNumber else card.maskedNumber,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace
                )
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column {
                        Text("Pemegang", style = MaterialTheme.typography.labelSmall)
                        Text(card.cardholderName, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text("Berlaku", style = MaterialTheme.typography.labelSmall)
                        Text(if (revealed) card.expiry else "••/••", fontFamily = FontFamily.Monospace)
                    }
                    Column {
                        Text("CVV", style = MaterialTheme.typography.labelSmall)
                        Text(if (revealed) card.cvv else "•••", fontFamily = FontFamily.Monospace)
                    }
                }
                Text(
                    "Limit per transaksi: ${formatRupiah(card.transactionLimit)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (revealed) {
                OutlinedButton(onClick = onHideDetails, modifier = Modifier.weight(1f)) { Text("Sembunyikan") }
            } else {
                OutlinedButton(onClick = onViewDetails, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Text("Lihat detail")
                }
            }
            if (frozen) {
                OutlinedButton(onClick = onActivate, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Text("Aktifkan")
                }
            } else {
                OutlinedButton(onClick = onFreeze, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Text("Bekukan")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenLimit, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text("Atur limit")
            }
            OutlinedButton(onClick = onOpenSimulate, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text("Uji transaksi")
            }
        }
    }
}

@Composable
private fun CreateCardDialog(
    state: VirtualCardUiState,
    onConfirm: (CardScope) -> Unit,
    onDismiss: () -> Unit
) {
    var scope by rememberSaveable { mutableStateOf(CardScope.DOMESTIC) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat kartu virtual") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pilih cakupan penggunaan kartu:")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardScope.entries.forEach { option ->
                        FilterChip(
                            selected = scope == option,
                            onClick = { scope = option },
                            label = { Text(option.label()) }
                        )
                    }
                }
                Text(
                    if (scope == CardScope.DOMESTIC) "Hanya untuk merchant dalam negeri."
                    else "Dapat dipakai di merchant dalam dan luar negeri.",
                    style = MaterialTheme.typography.bodySmall
                )
                MessageBanner(state.errorMessage, null)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(scope) }, enabled = !state.isLoading) {
                Text(if (state.isLoading) "Memproses..." else "Buat kartu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun PinDialog(
    state: VirtualCardUiState,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Masukkan PIN Transaksi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Detail kartu hanya ditampilkan setelah PIN valid.")
                PinField(pin, { pin = it })
                MessageBanner(state.errorMessage, null)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(pin); pin = "" },
                enabled = !state.isLoading && pin.length == 6
            ) { Text(if (state.isLoading) "Memverifikasi..." else "Konfirmasi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun LimitDialog(
    state: VirtualCardUiState,
    currentLimit: Long,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var limit by rememberSaveable { mutableStateOf(currentLimit.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur Limit Transaksi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Rentang ${formatRupiah(VirtualCardLimits.MIN_LIMIT)} – " +
                        formatRupiah(VirtualCardLimits.MAX_LIMIT) + " per transaksi.",
                    style = MaterialTheme.typography.bodySmall
                )
                AuthTextField(
                    value = limit,
                    onValueChange = { limit = it.filter(Char::isDigit).take(12) },
                    label = "Limit (Rp)",
                    keyboardType = KeyboardType.Number
                )
                MessageBanner(state.errorMessage, null)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(limit) }, enabled = !state.isLoading) {
                Text(if (state.isLoading) "Menyimpan..." else "Simpan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun SimulateDialog(
    state: VirtualCardUiState,
    onSimulate: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by rememberSaveable { mutableStateOf("") }
    var international by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uji Transaksi (Simulasi)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Mensimulasikan pembayaran merchant untuk menguji status beku, limit, dan cakupan kartu.",
                    style = MaterialTheme.typography.bodySmall
                )
                AuthTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit).take(12) },
                    label = "Nominal (Rp)",
                    keyboardType = KeyboardType.Number
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = international, onCheckedChange = { international = it })
                    Text("Merchant luar negeri")
                }
                MessageBanner(state.errorMessage, null)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSimulate(amount, international) }, enabled = !state.isLoading) {
                Text(if (state.isLoading) "Memproses..." else "Bayar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}
