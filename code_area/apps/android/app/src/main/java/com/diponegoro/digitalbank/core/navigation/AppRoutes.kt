package com.diponegoro.digitalbank.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

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
    Tab(AppRoutes.HOME, "Beranda", Icons.Filled.Home),
    Tab(AppRoutes.HISTORY, "Riwayat", Icons.Filled.History),
    Tab(AppRoutes.QRIS_PAY, "QRIS", Icons.Filled.QrCodeScanner),
    Tab(AppRoutes.PROFILE, "Profil", Icons.Filled.Person)
)

/** Navigasi utama: tindakan inti (bayar QRIS) maksimal beberapa ketukan dari dashboard (NFR-08). */
@Composable
fun MainBottomBar(currentRoute: String, onNavigate: (String) -> Unit) {
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { if (currentRoute != tab.route) onNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) }
            )
        }
    }
}
