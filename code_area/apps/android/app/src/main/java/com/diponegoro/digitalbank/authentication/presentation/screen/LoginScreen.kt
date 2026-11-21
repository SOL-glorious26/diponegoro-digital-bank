package com.diponegoro.digitalbank.authentication.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.diponegoro.digitalbank.authentication.presentation.components.AuthTextField
import com.diponegoro.digitalbank.authentication.presentation.components.LoadingButton
import com.diponegoro.digitalbank.authentication.presentation.components.MessageBanner
import com.diponegoro.digitalbank.authentication.presentation.components.PasswordField
import com.diponegoro.digitalbank.authentication.presentation.state.AuthUiState
import com.diponegoro.digitalbank.core.di.CoreModule
import com.diponegoro.digitalbank.ui.theme.DdbMark
import com.diponegoro.digitalbank.ui.theme.DdbTheme
import com.diponegoro.digitalbank.ui.theme.GoldRule

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        DdbMark(height = 52.dp)
        GoldRule(Modifier.padding(top = 28.dp))
        Text(
            "Selamat datang kembali",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            "Masuk ke rekening Diponegoro Anda.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Column(Modifier.padding(top = 36.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AuthTextField(identifier, { identifier = it }, "Username, email, atau nomor telepon")
            PasswordField(password, { password = it })
        }
        TextButton(onClick = onNavigateToRecovery, modifier = Modifier.align(Alignment.End)) {
            Text("Lupa password atau PIN?", color = DdbTheme.colors.link, style = MaterialTheme.typography.labelMedium)
        }
        MessageBanner(state.errorMessage, state.infoMessage)
        Spacer(Modifier.height(12.dp))
        LoadingButton("Masuk", state.isLoading) { onLogin(identifier, password) }

        if (state.biometricAvailable) {
            OutlinedButton(
                onClick = { showBiometricDialog = true },
                enabled = !state.isLoading,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, DdbTheme.colors.lineStrong),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(52.dp)
            ) {
                Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Masuk dengan biometrik", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(40.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Belum punya rekening?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onNavigateToRegister) {
                Text("Buka rekening", color = DdbTheme.colors.link, style = MaterialTheme.typography.labelMedium)
            }
        }
        // Varian demo (tanpa server): akun contoh tetap ditampilkan agar mudah dicoba saat presentasi.
        if (!CoreModule.online) {
            Text(
                "Akun demo: demo / Demo@1234 · PIN 123456",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
    }

    if (showBiometricDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricDialog = false },
            title = { Text("Autentikasi biometrik (simulasi)") },
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
