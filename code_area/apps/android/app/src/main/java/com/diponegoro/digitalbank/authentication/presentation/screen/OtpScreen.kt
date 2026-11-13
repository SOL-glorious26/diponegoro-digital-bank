package com.diponegoro.digitalbank.authentication.presentation.screen

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.diponegoro.digitalbank.authentication.presentation.components.AuthScreenContainer
import com.diponegoro.digitalbank.authentication.presentation.components.AuthTextField
import com.diponegoro.digitalbank.authentication.presentation.components.DemoOtpCard
import com.diponegoro.digitalbank.authentication.presentation.components.LoadingButton
import com.diponegoro.digitalbank.authentication.presentation.components.MessageBanner
import com.diponegoro.digitalbank.authentication.presentation.state.AuthUiState

@Composable
fun OtpScreen(
    state: AuthUiState,
    onVerify: (String) -> Unit,
    onResend: () -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }

    AuthScreenContainer(
        title = "Verifikasi OTP",
        subtitle = "Masukkan 6 digit kode OTP untuk mengaktifkan akun"
    ) {
        DemoOtpCard(state.demoOtpCode)
        AuthTextField(
            value = code,
            onValueChange = { code = it.filter(Char::isDigit).take(6) },
            label = "Kode OTP",
            keyboardType = KeyboardType.Number
        )
        MessageBanner(state.errorMessage, state.infoMessage)
        LoadingButton("Verifikasi", state.isLoading) { onVerify(code) }
        TextButton(onClick = onResend, enabled = !state.isLoading) { Text("Kirim ulang OTP") }
    }
}