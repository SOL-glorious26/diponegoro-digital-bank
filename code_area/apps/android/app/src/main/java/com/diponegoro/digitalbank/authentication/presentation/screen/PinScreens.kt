package com.diponegoro.digitalbank.authentication.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.authentication.presentation.components.AuthScreenContainer
import com.diponegoro.digitalbank.authentication.presentation.components.LoadingButton
import com.diponegoro.digitalbank.authentication.presentation.components.MessageBanner
import com.diponegoro.digitalbank.authentication.presentation.components.PinDots
import com.diponegoro.digitalbank.authentication.presentation.components.PinField
import com.diponegoro.digitalbank.authentication.presentation.components.PinKeypad
import com.diponegoro.digitalbank.authentication.presentation.state.AuthUiState
import com.diponegoro.digitalbank.ui.theme.DdbMark
import com.diponegoro.digitalbank.ui.theme.DdbTheme

@Composable
fun CreatePinScreen(
    state: AuthUiState,
    onCreatePin: (pin: String, confirmPin: String) -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }

    AuthScreenContainer(
        title = "Buat PIN Transaksi",
        subtitle = "PIN 6 digit digunakan untuk mengotorisasi transaksi"
    ) {
        PinField(pin, { pin = it }, "PIN baru (6 digit)")
        PinField(confirm, { confirm = it }, "Konfirmasi PIN")
        MessageBanner(state.errorMessage, state.infoMessage)
        LoadingButton("Simpan PIN", state.isLoading) { onCreatePin(pin, confirm) }
    }
}

/**
 * Layar PIN setelah login: enam titik + keypad angka sendiri. PIN dikirim otomatis saat digit
 * keenam diketuk; ViewModel dan aturan blokir (BR-02) tidak berubah.
 */
@Composable
fun EnterPinScreen(
    state: AuthUiState,
    onValidate: (String) -> Unit,
    onRecoverPin: () -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }

    // Kosongkan titik setelah server menjawab (salah/diblokir); sukses langsung berpindah layar.
    LaunchedEffect(state.isLoading) { if (!state.isLoading && pin.length == 6) pin = "" }
    LaunchedEffect(state.errorMessage, state.pinBlocked) { if (pin.length == 6 && !state.isLoading) pin = "" }

    val enabled = !state.isLoading && !state.pinBlocked

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        DdbMark(height = 32.dp)
        Text(
            "Masukkan PIN",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 48.dp)
        )
        Text(
            "PIN transaksi 6 digit",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        PinDots(filled = pin.length, shakeKey = state.errorMessage, modifier = Modifier.padding(top = 40.dp))

        Box(Modifier.fillMaxWidth().padding(top = 20.dp).height(44.dp), contentAlignment = Alignment.TopCenter) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    Modifier.size(20.dp), strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.onBackground
                )
                state.pinBlocked -> Text(
                    "PIN sedang diblokir sementara. Gunakan pemulihan PIN atau coba lagi nanti.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                state.errorMessage != null -> Text(
                    state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                state.infoMessage != null -> Text(
                    state.infoMessage,
                    color = DdbTheme.colors.link,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.weight(1f))
        PinKeypad(
            enabled = enabled,
            onDigit = { d ->
                if (pin.length < 6) {
                    pin += d
                    if (pin.length == 6) onValidate(pin)
                }
            },
            onDelete = { pin = pin.dropLast(1) }
        )
        TextButton(onClick = onRecoverPin, modifier = Modifier.padding(top = 12.dp)) {
            Text("Lupa PIN?", color = DdbTheme.colors.link, style = MaterialTheme.typography.labelMedium)
        }
    }
}
