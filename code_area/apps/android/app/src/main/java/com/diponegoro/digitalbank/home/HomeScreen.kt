package com.diponegoro.digitalbank.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diponegoro.digitalbank.core.ledger.TxStatus
import com.diponegoro.digitalbank.core.navigation.AppRoutes
import com.diponegoro.digitalbank.core.ui.EmptyState
import com.diponegoro.digitalbank.core.ui.ErrorText
import com.diponegoro.digitalbank.core.ui.SignedAmount
import com.diponegoro.digitalbank.core.ui.StatusChip
import com.diponegoro.digitalbank.core.util.formatAccountNumber
import com.diponegoro.digitalbank.core.util.formatDateTime
import com.diponegoro.digitalbank.core.util.formatRupiah
import com.diponegoro.digitalbank.history.data.HistoryItem
import com.diponegoro.digitalbank.ui.theme.DdbMark
import com.diponegoro.digitalbank.ui.theme.DdbTheme
import com.diponegoro.digitalbank.ui.theme.GoldRule
import java.util.Calendar

private const val HIDDEN = "Rp ••••••"

private data class QuickAction(val label: String, val icon: ImageVector, val route: String)

// Baris pertama: tindakan paling sering. Baris kedua: fitur lain agar tetap terjangkau dari Beranda.
private val quickActions = listOf(
    QuickAction("Transfer", Icons.Outlined.NorthEast, AppRoutes.TRANSFER),
    QuickAction("Top-up", Icons.Outlined.AddCircleOutline, AppRoutes.TOPUP),
    QuickAction("Kantong", Icons.Outlined.Layers, AppRoutes.POCKETS),
    QuickAction("Kartu", Icons.Outlined.CreditCard, "virtualcard/home"),
    QuickAction("Bayar QRIS", Icons.Outlined.QrCodeScanner, AppRoutes.QRIS_PAY),
    QuickAction("Terima QRIS", Icons.Outlined.QrCode, AppRoutes.QRIS_RECEIVE),
    QuickAction("Grafik Saldo", Icons.AutoMirrored.Outlined.ShowChart, AppRoutes.BALANCE_CHART),
    QuickAction("Analitik", Icons.Outlined.BarChart, AppRoutes.ANALYTICS)
)

private fun greeting(hour: Int) = when (hour) {
    in 4..10 -> "Selamat pagi"
    in 11..14 -> "Selamat siang"
    in 15..17 -> "Selamat sore"
    else -> "Selamat malam"
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    username: String,
    onToggleBalance: () -> Unit,
    onNavigate: (String) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    // Android 13+: minta izin notifikasi untuk push transaksi (FR-29).
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val hello = remember { greeting(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    val name = username.replaceFirstChar { it.titlecase() }
    val overview = state.overview
    val hidden = overview?.balanceHidden == true

    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = bottomBar) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // Header minimal: logo + nama, notifikasi, profil.
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                DdbMark(height = 28.dp, contentDescription = null)
                Text(
                    "DIPONEGORO",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(start = 12.dp).weight(1f)
                )
                IconButton(onClick = { onNavigate(AppRoutes.NOTIFICATIONS) }) {
                    Box {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifikasi, ${state.unread} belum dibaca")
                        if (state.unread > 0) {
                            Box(
                                Modifier.align(Alignment.TopEnd).size(7.dp).clip(CircleShape)
                                    .background(DdbTheme.colors.gold)
                            )
                        }
                    }
                }
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, DdbTheme.colors.lineStrong, CircleShape)
                        .clickable(role = Role.Button, onClickLabel = "Profil") { onNavigate(AppRoutes.PROFILE) },
                    contentAlignment = Alignment.Center
                ) { Text(name.take(1), style = MaterialTheme.typography.labelLarge) }
            }

            Text(
                "$hello, $name",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 28.dp)
            )

            ErrorText(state.error)
            if (overview != null && !overview.financialEnabled) {
                Text(
                    "e-KYC belum disetujui. Transfer, QRIS, top-up, dan kantong belum dapat digunakan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), MaterialTheme.shapes.small)
                        .padding(14.dp)
                )
            }

            // Saldo: elemen visual utama.
            Row(Modifier.padding(top = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Saldo rekening utama",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onToggleBalance, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = if (hidden) "Tampilkan saldo" else "Sembunyikan saldo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            val balance = when {
                overview == null -> "…"
                hidden -> HIDDEN
                else -> formatRupiah(overview.balance)
            }
            Text(
                buildAnnotatedString {
                    if (balance.startsWith("Rp")) {
                        withStyle(SpanStyle(fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)) { append("Rp") }
                        append(balance.removePrefix("Rp"))
                    } else append(balance)
                },
                style = MaterialTheme.typography.displayMedium
            )
            if (overview != null) {
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    GoldRule(width = 16.dp)
                    Text(
                        "${overview.ownerName} · ${formatAccountNumber(overview.accountNumber)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                Text(
                    "Di kantong: ${if (hidden) HIDDEN else formatRupiah(overview.pocketsTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 26.dp)
                )
            }

            QuickActionGrid(Modifier.padding(top = 32.dp)) { onNavigate(it) }

            state.month?.let { month ->
                HorizontalDivider(Modifier.padding(top = 28.dp), color = DdbTheme.colors.line)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = "Lihat analitik") { onNavigate(AppRoutes.ANALYTICS) }
                        .padding(vertical = 20.dp)
                ) {
                    MonthFigure("Pemasukan · ${month.label}", if (hidden) HIDDEN else formatRupiah(month.income), Modifier.weight(1f))
                    MonthFigure("Pengeluaran", if (hidden) HIDDEN else formatRupiah(month.expense), Modifier.weight(1f))
                }
            }

            HorizontalDivider(color = DdbTheme.colors.line)
            Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Transaksi terakhir", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "Lihat semua",
                    style = MaterialTheme.typography.labelMedium,
                    color = DdbTheme.colors.link,
                    modifier = Modifier.clickable(role = Role.Button) { onNavigate(AppRoutes.HISTORY) }.padding(8.dp)
                )
            }
            if (state.recent.isEmpty()) EmptyState("Belum ada transaksi")
            state.recent.forEach { item ->
                TransactionRow(item, showDivider = true) { onNavigate(AppRoutes.historyDetail(item.tx.id)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MonthFigure(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun QuickActionGrid(modifier: Modifier = Modifier, onClick: (String) -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        quickActions.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { action ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .clickable(role = Role.Button) { onClick(action.route) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier.size(52.dp).border(1.dp, DdbTheme.colors.lineStrong, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(action.icon, contentDescription = null, modifier = Modifier.size(22.dp))
                        }
                        Text(
                            action.label,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/** Baris transaksi yang dipakai di dashboard dan halaman riwayat: daftar bersih, tanpa kartu. */
@Composable
fun TransactionRow(item: HistoryItem, showDivider: Boolean = false, onClick: () -> Unit) {
    val tx = item.tx
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(tx.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(
                    "${if (item.direction.name == "IN") tx.fromName else tx.toName} · ${formatDateTime(tx.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (tx.status != TxStatus.SUCCESS) StatusChip(tx.status)
            }
            Spacer(Modifier.width(12.dp))
            SignedAmount(
                if (item.direction.name == "IN") tx.amount else tx.total,
                item.direction,
                failed = tx.status == TxStatus.FAILED
            )
        }
        if (showDivider) HorizontalDivider(color = DdbTheme.colors.line)
    }
}
