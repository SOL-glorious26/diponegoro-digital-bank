package com.diponegoro.digitalbank.authentication.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diponegoro.digitalbank.core.ui.ddbFieldColors
import com.diponegoro.digitalbank.ui.theme.DdbMark
import com.diponegoro.digitalbank.ui.theme.DdbTheme
import com.diponegoro.digitalbank.ui.theme.GoldRule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Kerangka layar autentikasi (registrasi, OTP, e-KYC, pemulihan, buat PIN):
 * logo, garis emas, judul besar, lalu konten. Tanpa kartu dan bayangan.
 */
@Composable
fun AuthScreenContainer(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        DdbMark(height = 44.dp)
        GoldRule(Modifier.padding(top = 14.dp))
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
fun MessageBanner(errorMessage: String?, infoMessage: String?) {
    if (errorMessage != null) {
        Text(text = errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
    if (infoMessage != null) {
        Text(text = infoMessage, color = DdbTheme.colors.link, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Tombol utama setinggi 52 dp; satu per layar. */
@Composable
fun LoadingButton(
    text: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier.fillMaxWidth().height(52.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        colors = ddbFieldColors(),
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String = "Password") {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        colors = ddbFieldColors(),
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) {
                Text(if (visible) "Sembunyikan" else "Tampilkan", color = DdbTheme.colors.link)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun PinField(value: String, onValueChange: (String) -> Unit, label: String = "PIN (6 digit)") {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(6)) },
        label = { Text(label) },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        colors = ddbFieldColors(),
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun DemoOtpCard(code: String?) {
    if (code == null) return
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(DdbTheme.colors.panel)
            .border(1.dp, DdbTheme.colors.line, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Kode OTP (simulasi, karena belum ada SMS/email)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(code, style = MaterialTheme.typography.displaySmall)
        Text("Berlaku 2 menit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- PIN: titik + keypad

/** Enam titik PIN. [shakeKey] berubah → titik bergetar singkat (PIN salah). */
@Composable
fun PinDots(filled: Int, shakeKey: Any?, modifier: Modifier = Modifier) {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey != null) {
            offset.animateTo(0f, keyframes {
                durationMillis = 240
                -10f at 40; 10f at 80; -8f at 120; 8f at 160; -4f at 200
            })
        }
    }
    Row(
        modifier
            .offset { IntOffset(offset.value.dp.roundToPx(), 0) }
            .semantics { contentDescription = "$filled dari 6 digit PIN terisi" },
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        repeat(6) { i ->
            val on = i < filled
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (on) DdbTheme.colors.pinFilled else MaterialTheme.colorScheme.background)
                    .border(1.dp, if (on) DdbTheme.colors.pinFilled else DdbTheme.colors.pinEmpty, CircleShape)
            )
        }
    }
}

/** Keypad angka 3 × 4 tanpa latar: angka tipis, umpan balik riak lembut. */
@Composable
fun PinKeypad(
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf("123", "456", "789")
    Column(modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { d -> PinKey(enabled, onClick = { onDigit(d) }) { DigitText(d) } }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Spacer(Modifier.size(width = 84.dp, height = 64.dp))
            PinKey(enabled, onClick = { onDigit('0') }) { DigitText('0') }
            PinKey(enabled, onClick = onDelete) {
                Icon(
                    Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = "Hapus satu digit",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun DigitText(d: Char) {
    Text(d.toString(), fontSize = 28.sp, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal))
}

@Composable
private fun PinKey(enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(width = 84.dp, height = 64.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

fun formatTime(millis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))

/** Tanggal dan jam, mis. "18 Nov 2026 21:29" — dipakai bila rentang waktu bisa lebih dari satu hari. */
fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID")).format(Date(millis))
