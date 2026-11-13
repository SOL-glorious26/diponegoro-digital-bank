package com.diponegoro.digitalbank.authentication.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.authentication.domain.model.RecoveryType
import com.diponegoro.digitalbank.authentication.presentation.components.AuthScreenContainer
import com.diponegoro.digitalbank.authentication.presentation.components.AuthTextField
import com.diponegoro.digitalbank.authentication.presentation.components.DemoOtpCard
import com.diponegoro.digitalbank.authentication.presentation.components.LoadingButton
import com.diponegoro.digitalbank.authentication.presentation.components.MessageBanner
import com.diponegoro.digitalbank.authentication.presentation.components.PasswordField
import com.diponegoro.digitalbank.authentication.presentation.components.PinField
import com.diponegoro.digitalbank.authentication.presentation.state.RecoveryStep
import com.diponegoro.digitalbank.authentication.presentation.state.RecoveryUiState

@Composable
fun RecoveryScreen(
    state: RecoveryUiState,
    onSelectType: (RecoveryType) -> Unit,
    onRequest: (identifier: String) -> Unit,
    onVerifyOtp: (String) -> Unit,
    onResendOtp: () -> Unit,
    onReset: (String) -> Unit,
    onBack: () -> Unit
) {
    var identifier by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var newValue by rememberSaveable { mutableStateOf("") }
    val typeLabel = if (state.type == RecoveryType.PASSWORD) "password" else "PIN"

    AuthScreenContainer(
        title = "Pemulihan Password / PIN",
        subtitle = "Verifikasi OTP untuk mengatur ulang kredensial"
    ) {
        when (state.step) {
            RecoveryStep.REQUEST -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.type == RecoveryType.PASSWORD,
                        onClick = { onSelectType(RecoveryType.PASSWORD) },
                        label = { Text("Password") }
                    )
                    FilterChip(
                        selected = state.type == RecoveryType.PIN,
                        onClick = { onSelectType(RecoveryType.PIN) },
                        label = { Text("PIN transaksi") }
                    )
                }
                AuthTextField(identifier, { identifier = it }, "Username / email / nomor telepon")
                MessageBanner(state.errorMessage, state.infoMessage)
                LoadingButton("Kirim OTP", state.isLoading) { onRequest(identifier) }
            }

            RecoveryStep.VERIFY_OTP -> {
                DemoOtpCard(state.demoOtpCode)
                AuthTextField(
                    value = otp,
                    onValueChange = { otp = it.filter(Char::isDigit).take(6) },
                    label = "Kode OTP",
                    keyboardType = KeyboardType.Number
                )
                MessageBanner(state.errorMessage, state.infoMessage)
                LoadingButton("Verifikasi OTP", state.isLoading) { onVerifyOtp(otp) }
                TextButton(onClick = onResendOtp, enabled = !state.isLoading) { Text("Kirim ulang OTP") }
            }

            RecoveryStep.RESET -> {
                if (state.type == RecoveryType.PASSWORD) {
                    PasswordField(newValue, { newValue = it }, "Password baru")
                } else {
                    PinField(newValue, { newValue = it }, "PIN baru (6 digit)")
                }
                MessageBanner(state.errorMessage, state.infoMessage)
                LoadingButton("Simpan $typeLabel baru", state.isLoading) { onReset(newValue) }
            }

            RecoveryStep.DONE -> {
                Text(
                    "Berhasil! $typeLabel kamu telah diatur ulang.",
                    style = MaterialTheme.typography.titleMedium
                )
                LoadingButton("Kembali", false, onClick = onBack)
            }
        }
        if (state.step != RecoveryStep.DONE) {
            TextButton(onClick = onBack) { Text("Batal") }
        }
    }
}