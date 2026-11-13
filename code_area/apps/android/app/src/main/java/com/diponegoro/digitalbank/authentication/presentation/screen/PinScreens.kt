package com.diponegoro.digitalbank.authentication.presentation.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.diponegoro.digitalbank.authentication.presentation.components.AuthScreenContainer
import com.diponegoro.digitalbank.authentication.presentation.components.LoadingButton
import com.diponegoro.digitalbank.authentication.presentation.components.MessageBanner
import com.diponegoro.digitalbank.authentication.presentation.components.PinField
import com.diponegoro.digitalbank.authentication.presentation.state.AuthUiState

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

@Composable
fun EnterPinScreen(
    state: AuthUiState,
    onValidate: (String) -> Unit,
    onRecoverPin: () -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }

    AuthScreenContainer(
        title = "Masukkan PIN Transaksi",
        subtitle = "Salah 3 kali akan memblokir PIN sementara"
    ) {
        PinField(pin, { pin = it })
        if (state.pinBlocked) {
            Text(
                "PIN sedang diblokir sementara. Gunakan pemulihan PIN atau coba lagi nanti.",
                color = MaterialTheme.colorScheme.error
            )
        }
        MessageBanner(state.errorMessage, state.infoMessage)
        LoadingButton("Konfirmasi PIN", state.isLoading) {
            onValidate(pin)
            pin = ""
        }
        TextButton(onClick = onRecoverPin) { Text("Lupa PIN?") }
    }
}