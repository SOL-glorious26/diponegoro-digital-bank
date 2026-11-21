package com.diponegoro.digitalbank.authentication.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import com.diponegoro.digitalbank.core.ui.DdbOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.authentication.domain.model.AccountStatus
import com.diponegoro.digitalbank.authentication.presentation.components.AuthScreenContainer
import com.diponegoro.digitalbank.authentication.presentation.components.LoadingButton
import com.diponegoro.digitalbank.authentication.presentation.components.MessageBanner
import com.diponegoro.digitalbank.authentication.presentation.components.formatDateTime
import com.diponegoro.digitalbank.authentication.presentation.state.AuthUiState

private fun AccountStatus.label(): String = when (this) {
    AccountStatus.PENDING_OTP -> "Menunggu verifikasi OTP"
    AccountStatus.ACTIVE -> "Aktif (e-KYC belum selesai)"
    AccountStatus.FINANCIAL_ENABLED -> "Aktif, transaksi finansial diaktifkan"
    AccountStatus.KYC_REJECTED -> "Aktif, e-KYC ditolak (transaksi finansial nonaktif)"
}

@Composable
fun HomeScreen(
    state: AuthUiState,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onLogoutAllDevices: () -> Unit,
    onRecover: () -> Unit
) {
    LaunchedEffect(Unit) { onRefresh() }

    val user = state.currentUser
    AuthScreenContainer(
        title = "Sesi & Perangkat",
        subtitle = "Kelola sesi terautentikasi untuk ${user?.username ?: "akun ini"}"
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Status akun", style = MaterialTheme.typography.labelMedium)
                Text(user?.accountStatus?.label() ?: "-", style = MaterialTheme.typography.bodyLarge)
                Text("PIN transaksi: terverifikasi", style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (state.activeSessions.isEmpty() && state.currentSession != null) {
            // DEF-11: sesi yang sedang dipakai selalu aktif; daftar kosong berarti gagal dimuat (mis. server tidak terjangkau).
            Text(
                "Daftar sesi tidak dapat dimuat. Periksa koneksi ke server, lalu ketuk Muat ulang sesi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Text("Sesi aktif (${state.activeSessions.size})", style = MaterialTheme.typography.titleMedium)
        }
        state.activeSessions.forEach { session ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val current = if (session.sessionId == state.currentSession?.sessionId) " (sesi ini)" else ""
                    Text("${session.deviceId}$current", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Metode: ${session.authMethod}  |  Mulai ${formatDateTime(session.createdAt)}  |  " +
                                "Berakhir ${formatDateTime(session.expiresAt)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        TextButton(onClick = onRefresh) { Text("Muat ulang sesi") }

        MessageBanner(state.errorMessage, state.infoMessage)
        LoadingButton("Logout", state.isLoading, onClick = onLogout)
        OutlinedButton(
            onClick = onLogoutAllDevices,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Logout dari semua perangkat") }
        TextButton(onClick = onRecover) { Text("Pemulihan password / PIN") }
    }
}