package com.diponegoro.digitalbank.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.ui.theme.DdbTheme

object AppRoutes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val HISTORY_DETAIL = "history/detail/{txId}"
    fun historyDetail(txId: String) = "history/detail/$txId"
    const val ANALYTICS = "history/analytics"
    const val BALANCE_CHART = "account/chart"
    const val POCKETS = "account/pockets"
    const val TRANSFER = "transfer"
    const val QRIS_PAY = "qris/pay"
    const val QRIS_RECEIVE = "qris/receive"
    const val TOPUP = "topup"
    const val PROFILE = "profile"
    const val SECURITY = "profile/security"
    const val NOTIFICATION_SETTINGS = "profile/notification-settings"
    const val NOTIFICATIONS = "notifications"
    const val SESSIONS = "profile/sessions"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(AppRoutes.HOME, "Beranda", Icons.Outlined.Home),
    Tab(AppRoutes.HISTORY, "Riwayat", Icons.Outlined.History),
    Tab(AppRoutes.QRIS_PAY, "QRIS", Icons.Outlined.QrCodeScanner),
    Tab(AppRoutes.POCKETS, "Kantong", Icons.Outlined.Layers),
    Tab(AppRoutes.PROFILE, "Profil", Icons.Outlined.Person)
)

/**
 * Navigasi utama minimal: garis tipis di atas, ikon monokrom, QRIS sebagai aksi utama di tengah
 * (bayar QRIS maksimal beberapa ketukan dari dashboard, NFR-08).
 */
@Composable
fun MainBottomBar(currentRoute: String, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        HorizontalDivider(color = DdbTheme.colors.line)
        Row(
            Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                val center = tab.route == AppRoutes.QRIS_PAY
                Column(
                    Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .semantics { this.selected = selected }
                        .clickable(role = Role.Tab) { if (!selected) onNavigate(tab.route) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (center) {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(tab.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                        }
                    } else {
                        Icon(
                            tab.icon,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected || center) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
