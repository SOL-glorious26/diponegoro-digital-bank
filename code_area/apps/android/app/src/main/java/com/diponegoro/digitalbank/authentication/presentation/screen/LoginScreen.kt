package com.diponegoro.digitalbank.authentication.presentation.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import com.diponegoro.digitalbank.authentication.presentation.components.AuthScreenContainer
import com.diponegoro.digitalbank.authentication.presentation.components.AuthTextField
import com.diponegoro.digitalbank.authentication.presentation.components.LoadingButton
import com.diponegoro.digitalbank.authentication.presentation.components.MessageBanner
import com.diponegoro.digitalbank.authentication.presentation.components.PasswordField
import com.diponegoro.digitalbank.authentication.presentation.state.AuthUiState

@Composable
fun LoginScreen(
    state: AuthUiState,
    onLogin: (identifier: String, password: String) -> Unit,
    onBiometricLogin: () -> Unit,
    onRefreshBiometric: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToRecovery: () -> Unit
) {
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showBiometricDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { onRefreshBiometric() }

    AuthScreenContainer(
        title = "Diponegoro Digital Bank",
        subtitle = "Login untuk melanjutkan. Akun demo: demo / Demo@1234 (PIN 123456)"
    ) {
        if (state.biometricAvailable) {
            OutlinedButton(
                onClick = { showBiometricDialog = true },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Login dengan Biometrik") }
            Text("atau gunakan username/email dan password")
        }
        AuthTextField(identifier, { identifier = it }, "Username / email / nomor telepon")
        PasswordField(password, { password = it })
        MessageBanner(state.errorMessage, state.infoMessage)
        LoadingButton("Login", state.isLoading) { onLogin(identifier, password) }
        TextButton(onClick = onNavigateToRecovery) { Text("Lupa password / PIN?") }
        TextButton(onClick = onNavigateToRegister) { Text("Belum punya akun? Daftar") }
    }

    if (showBiometricDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricDialog = false },
            title = { Text("Autentikasi Biometrik (Simulasi)") },
            text = { Text("Pada prototype ini autentikasi biometrik dianggap berhasil saat kamu menekan tombol di bawah.") },
            confirmButton = {
                TextButton(onClick = {
                    showBiometricDialog = false
                    onBiometricLogin()
                }) { Text("Autentikasi") }
            },
            dismissButton = {
                TextButton(onClick = { showBiometricDialog = false }) { Text("Batal") }
            }
        )
    }
}