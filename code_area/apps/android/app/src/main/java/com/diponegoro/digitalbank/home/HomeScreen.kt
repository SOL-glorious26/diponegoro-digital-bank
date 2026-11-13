package com.diponegoro.digitalbank.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.core.ledger.TxStatus
import com.diponegoro.digitalbank.core.navigation.AppRoutes
import com.diponegoro.digitalbank.core.ui.EmptyState
import com.diponegoro.digitalbank.core.ui.ErrorText
import com.diponegoro.digitalbank.core.ui.HighlightCard
import com.diponegoro.digitalbank.core.ui.InfoRow
import com.diponegoro.digitalbank.core.ui.ScreenScaffold
import com.diponegoro.digitalbank.core.ui.SectionCard
import com.diponegoro.digitalbank.core.ui.SignedAmount
import com.diponegoro.digitalbank.core.ui.StatusChip
import com.diponegoro.digitalbank.core.util.formatAccountNumber
import com.diponegoro.digitalbank.core.util.formatDateTime
import com.diponegoro.digitalbank.core.util.formatRupiah
import com.diponegoro.digitalbank.history.data.HistoryItem

private const val HIDDEN = "Rp ••••••"

private data class QuickAction(val label: String, val icon: ImageVector, val route: String)

private val quickActions = listOf(
    QuickAction("Transfer", Icons.AutoMirrored.Filled.Send, AppRoutes.TRANSFER),
    QuickAction("Bayar QRIS", Icons.Filled.QrCodeScanner, AppRoutes.QRIS_PAY),
    QuickAction("Terima QRIS", Icons.Filled.QrCode, AppRoutes.QRIS_RECEIVE),
    QuickAction("Top-up", Icons.Filled.PhoneAndroid, AppRoutes.TOPUP),
    QuickAction("Kantong", Icons.Filled.Savings, AppRoutes.POCKETS),
    QuickAction("Virtual Card", Icons.Filled.CreditCard, "virtualcard/home"),
    QuickAction("Grafik Saldo", Icons.AutoMirrored.Filled.ShowChart, AppRoutes.BALANCE_CHART),
    QuickAction("Analitik", Icons.Filled.BarChart, AppRoutes.ANALYTICS)
)

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

    ScreenScaffold(
        title = "Halo, $username",
        onBack = null,
        bottomBar = bottomBar,
        actions = {
            IconButton(onClick = { onNavigate(AppRoutes.NOTIFICATIONS) }) {
                BadgedBox(badge = { if (state.unread > 0) Badge { Text(state.unread.coerceAtMost(99).toString()) } }) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifikasi, ${state.unread} belum dibaca")
                }
            }
        }
    ) {
        ErrorText(state.error)
        val overview = state.overview

        if (overview != null && !overview.financialEnabled) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("e-KYC belum disetujui. Transfer, QRIS, top-up, dan kantong belum dapat digunakan.")
                }
            }
        }

        HighlightCard {
            Text("Saldo rekening utama", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        overview == null -> "…"
                        overview.balanceHidden -> HIDDEN
                        else -> formatRupiah(overview.balance)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleBalance) {
                    Icon(
                        if (overview?.balanceHidden == true) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (overview?.balanceHidden == true) "Tampilkan saldo" else "Sembunyikan saldo",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            if (overview != null) {
                Text("${overview.ownerName} • ${formatAccountNumber(overview.accountNumber)}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Di kantong: ${if (overview.balanceHidden) HIDDEN else formatRupiah(overview.pocketsTotal)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        QuickActionGrid { onNavigate(it) }

        state.month?.let { month ->
            SectionCard(title = "Bulan ini (${month.label})") {
                InfoRow("Pemasukan", if (overview?.balanceHidden == true) HIDDEN else formatRupiah(month.income))
                InfoRow("Pengeluaran", if (overview?.balanceHidden == true) HIDDEN else formatRupiah(month.expense))
                TextButton(onClick = { onNavigate(AppRoutes.ANALYTICS) }) { Text("Lihat grafik pemasukan & pengeluaran") }
            }
        }

        SectionCard(title = "Transaksi terakhir") {
            if (state.recent.isEmpty()) EmptyState("Belum ada transaksi")
            state.recent.forEach { item ->
                TransactionRow(item) { onNavigate(AppRoutes.historyDetail(item.tx.id)) }
            }
            TextButton(onClick = { onNavigate(AppRoutes.HISTORY) }) { Text("Lihat semua riwayat") }
        }
    }
}

@Composable
private fun QuickActionGrid(onClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        quickActions.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { action ->
                    Card(
                        modifier = Modifier.weight(1f).clickable { onClick(action.route) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(action.icon, contentDescription = null, modifier = Modifier.size(26.dp))
                            Text(
                                action.label, style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center, maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Baris transaksi yang dipakai di dashboard dan halaman riwayat. */
@Composable
fun TransactionRow(item: HistoryItem, onClick: () -> Unit) {
    val tx = item.tx
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(tx.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(
                "${if (item.direction.name == "IN") tx.fromName else tx.toName} • ${formatDateTime(tx.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            if (tx.status != TxStatus.SUCCESS) StatusChip(tx.status)
        }
        SignedAmount(
            if (item.direction.name == "IN") tx.amount else tx.total,
            item.direction,
            failed = tx.status == TxStatus.FAILED
        )
    }
}
