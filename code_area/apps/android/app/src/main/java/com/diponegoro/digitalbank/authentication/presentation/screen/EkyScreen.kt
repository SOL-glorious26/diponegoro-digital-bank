package com.diponegoro.digitalbank.authentication.presentation.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.diponegoro.digitalbank.core.ui.DdbOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import com.diponegoro.digitalbank.authentication.domain.model.EkycStatus
import com.diponegoro.digitalbank.authentication.presentation.components.AuthScreenContainer
import com.diponegoro.digitalbank.authentication.presentation.components.AuthTextField
import com.diponegoro.digitalbank.authentication.presentation.components.LoadingButton
import com.diponegoro.digitalbank.authentication.presentation.components.MessageBanner
import com.diponegoro.digitalbank.authentication.presentation.state.AuthUiState

@Composable
fun EkycScreen(
    state: AuthUiState,
    onSubmit: (nik: String, selfieCaptured: Boolean) -> Unit,
    onRetry: () -> Unit,
    onFinish: () -> Unit
) {
    var nik by rememberSaveable { mutableStateOf("") }
    var selfieCaptured by rememberSaveable { mutableStateOf(false) }

    AuthScreenContainer(
        title = "Verifikasi e-KYC",
        subtitle = "Simulasi: NIK berakhiran 0000 akan ditolak, selain itu disetujui"
    ) {
        when (state.ekycStatus) {
            null -> {
                AuthTextField(
                    value = nik,
                    onValueChange = { nik = it.filter(Char::isDigit).take(16) },
                    label = "NIK KTP (16 digit)",
                    keyboardType = KeyboardType.Number
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(checked = selfieCaptured, onCheckedChange = { selfieCaptured = it })
                    Text("Selfie sudah diambil (simulasi)")
                }
                MessageBanner(state.errorMessage, state.infoMessage)
                LoadingButton("Kirim KTP & Selfie", state.isLoading) { onSubmit(nik, selfieCaptured) }
            }

            EkycStatus.PROCESSING -> {
                CircularProgressIndicator()
                Text("Memproses verifikasi e-KYC...")
            }

            EkycStatus.APPROVED -> {
                Text("e-KYC disetujui. Transaksi finansial diaktifkan.", style = MaterialTheme.typography.titleMedium)
                LoadingButton("Lanjut ke Login", false, onClick = onFinish)
            }

            EkycStatus.REJECTED -> {
                Text(
                    "e-KYC ditolak. Transaksi finansial tetap dinonaktifkan.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium
                )
                LoadingButton("Lanjut ke Login", false, onClick = onFinish)
                OutlinedButton(onClick = onRetry) { Text("Coba kirim ulang data") }
            }
        }
    }
}