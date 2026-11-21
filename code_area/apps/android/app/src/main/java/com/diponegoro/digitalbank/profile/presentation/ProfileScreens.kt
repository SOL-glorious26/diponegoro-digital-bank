package com.diponegoro.digitalbank.profile.presentation

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import com.diponegoro.digitalbank.core.ui.DdbButton as Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.diponegoro.digitalbank.core.ui.DdbOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.authentication.presentation.components.PasswordField
import com.diponegoro.digitalbank.authentication.presentation.components.PinField
import com.diponegoro.digitalbank.core.navigation.AppRoutes
import com.diponegoro.digitalbank.core.notification.NotificationChannel
import com.diponegoro.digitalbank.core.notification.NotificationType
import com.diponegoro.digitalbank.core.ui.EmptyState
import com.diponegoro.digitalbank.core.ui.ErrorText
import com.diponegoro.digitalbank.core.ui.InfoRow
import com.diponegoro.digitalbank.core.ui.InfoText
import com.diponegoro.digitalbank.core.ui.ScreenScaffold
import com.diponegoro.digitalbank.core.ui.SectionCard
import com.diponegoro.digitalbank.core.util.formatDateTime
import com.diponegoro.digitalbank.profile.data.ProfileDraft

@Composable
private fun Avatar(uri: String?, size: Int = 88) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        uri?.let { runCatching { context.contentResolver.openInputStream(Uri.parse(it))?.use(BitmapFactory::decodeStream) }.getOrNull() }
    }
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) Image(bitmap.asImageBitmap(), "Foto profil", contentScale = ContentScale.Crop, modifier = Modifier.size(size.dp))
        else Icon(Icons.Filled.Person, contentDescription = "Belum ada foto", modifier = Modifier.size((size / 2).dp))
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null)
        Text(label, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    vm: ProfileViewModel,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setPhoto(uri.toString())
    }
    ScreenScaffold(title = "Profil", onBack = null, bottomBar = bottomBar) {
        val user = state.user ?: return@ScreenScaffold
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Avatar(user.photoUri)
                Column {
                    Text(user.username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text("Ganti foto")
                    }
                    if (user.photoUri != null) TextButton(onClick = { vm.setPhoto(null) }) { Text("Hapus foto") }
                }
            }
        }
        InfoText(state.info)
        if (!state.editing) {
            SectionCard(title = "Data pribadi") {
                InfoRow("Username", user.username)
                InfoRow("Email", user.email.ifBlank { "-" })
                InfoRow("Nomor telepon", user.phoneNumber.ifBlank { "-" })
                OutlinedButton(onClick = vm::startEdit, modifier = Modifier.fillMaxWidth()) { Text("Ubah data") }
            }
        } else {
            EditProfileCard(state, vm)
        }
        SectionCard {
            MenuRow(Icons.Filled.Security, "Keamanan (password, PIN, biometrik)") { onNavigate(AppRoutes.SECURITY) }
            MenuRow(Icons.Filled.Notifications, "Pengaturan notifikasi") { onNavigate(AppRoutes.NOTIFICATION_SETTINGS) }
            MenuRow(Icons.Filled.Devices, "Sesi & perangkat aktif") { onNavigate(AppRoutes.SESSIONS) }
            MenuRow(Icons.AutoMirrored.Filled.Logout, "Keluar", onLogout)
        }
    }

    state.pending?.let { pending ->
        var code by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!state.busy) vm.cancelEdit() },
            title = { Text("Verifikasi OTP") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kode OTP dikirim ke ${pending.sentTo}. Perubahan aktif setelah OTP valid.")
                    Text("OTP simulasi: ${pending.demoOtp}", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = code, onValueChange = { code = it.filter(Char::isDigit).take(6) },
                        label = { Text("Kode OTP") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    ErrorText(state.error)
                    TextButton(onClick = vm::resendOtp) { Text("Kirim ulang OTP") }
                }
            },
            confirmButton = { TextButton(onClick = { vm.confirm(code) }, enabled = code.length == 6 && !state.busy) { Text("Verifikasi") } },
            dismissButton = { TextButton(onClick = vm::cancelEdit) { Text("Batal") } }
        )
    }
}

@Composable
private fun EditProfileCard(state: ProfileUiState, vm: ProfileViewModel) {
    val user = state.user ?: return
    var username by rememberSaveable { mutableStateOf(user.username) }
    var email by rememberSaveable { mutableStateOf(user.email) }
    var phone by rememberSaveable { mutableStateOf(user.phoneNumber) }
    SectionCard(title = "Ubah data pribadi") {
        OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(phone, { phone = it.filter { c -> c.isDigit() || c == '+' } }, label = { Text("Nomor telepon") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone))
        Text("Perubahan memerlukan verifikasi OTP.", style = MaterialTheme.typography.bodySmall)
        if (state.pending == null) ErrorText(state.error)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.submit(ProfileDraft(username, email, phone)) }, enabled = !state.busy) { Text("Simpan") }
            OutlinedButton(onClick = vm::cancelEdit) { Text("Batal") }
        }
    }
}

@Composable
fun SecurityScreen(state: SecurityUiState, vm: SecurityViewModel, onSessions: () -> Unit, onBack: () -> Unit) {
    var oldPass by rememberSaveable { mutableStateOf("") }
    var newPass by rememberSaveable { mutableStateOf("") }
    var confirmPass by rememberSaveable { mutableStateOf("") }
    var oldPin by rememberSaveable { mutableStateOf("") }
    var newPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    ScreenScaffold(title = "Keamanan", onBack = onBack) {
        ErrorText(state.error)
        InfoText(state.info)
        SectionCard(title = "Login biometrik") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Izinkan login dengan biometrik di perangkat ini", modifier = Modifier.weight(1f))
                Switch(checked = state.biometric, onCheckedChange = vm::setBiometric, enabled = !state.busy)
            }
        }
        SectionCard(title = "Ubah password") {
            PasswordField(oldPass, { oldPass = it }, "Password lama")
            PasswordField(newPass, { newPass = it }, "Password baru")
            PasswordField(confirmPass, { confirmPass = it }, "Konfirmasi password baru")
            Button(onClick = { vm.changePassword(oldPass, newPass, confirmPass); oldPass = ""; newPass = ""; confirmPass = "" },
                enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Ubah password") }
        }
        SectionCard(title = "Ubah PIN transaksi") {
            PinField(oldPin, { oldPin = it }, "PIN lama")
            PinField(newPin, { newPin = it }, "PIN baru")
            PinField(confirmPin, { confirmPin = it }, "Konfirmasi PIN baru")
            Button(onClick = { vm.changePin(oldPin, newPin, confirmPin); oldPin = ""; newPin = ""; confirmPin = "" },
                enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Ubah PIN") }
            Text("PIN lama salah 3 kali akan memblokir PIN sementara.", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = onSessions, modifier = Modifier.fillMaxWidth()) { Text("Kelola sesi & logout semua perangkat") }
    }
}

@Composable
fun NotificationSettingsScreen(state: NotificationSettingsUiState, vm: NotificationSettingsViewModel, onBack: () -> Unit) {
    ScreenScaffold(title = "Pengaturan Notifikasi", onBack = onBack) {
        SectionCard(title = "Jenis notifikasi") {
            NotificationType.entries.forEach { type ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = type in state.settings.types, onCheckedChange = { vm.toggleType(type) })
                    Text(type.label)
                }
            }
        }
        SectionCard(title = "Kanal pengiriman") {
            NotificationChannel.entries.forEach { channel ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = channel in state.settings.channels, onCheckedChange = { vm.toggleChannel(channel) })
                    Text(channel.label + if (channel == NotificationChannel.EMAIL) " (simulasi)" else "")
                }
            }
        }
        ErrorText(state.error)
        InfoText(state.info)
    }
}

@Composable
fun NotificationsScreen(state: NotificationsUiState, vm: NotificationsViewModel, onOpenTx: (String) -> Unit, onBack: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }
    ScreenScaffold(title = "Notifikasi", onBack = onBack, actions = { TextButton(onClick = vm::markAllRead) { Text("Tandai dibaca") } }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Dalam aplikasi (${state.inbox.count { !it.read }})") })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Email (${state.emails.size})") })
        }
        if (tab == 0) {
            if (state.inbox.isEmpty()) EmptyState("Belum ada notifikasi")
            state.inbox.forEach { n ->
                SectionCard(modifier = Modifier.clickable {
                    vm.markRead(n.id)
                    n.transactionId?.let(onOpenTx)
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(n.title, style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (!n.read) Text("Baru", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(n.message, style = MaterialTheme.typography.bodyMedium)
                    Text("${n.type.label} • ${formatDateTime(n.createdAt)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Text("Email tidak benar-benar dikirim; ini outbox simulasi.", style = MaterialTheme.typography.bodySmall)
            if (state.emails.isEmpty()) EmptyState("Aktifkan kanal Email di pengaturan notifikasi")
            state.emails.forEach { e ->
                SectionCard {
                    Text("Kepada: ${e.to}", style = MaterialTheme.typography.labelMedium)
                    Text(e.subject, style = MaterialTheme.typography.titleSmall)
                    Text(e.body, style = MaterialTheme.typography.bodyMedium)
                    Text(formatDateTime(e.sentAt), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
