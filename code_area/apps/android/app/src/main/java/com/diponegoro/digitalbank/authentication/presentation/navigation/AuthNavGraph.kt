package com.diponegoro.digitalbank.authentication.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.diponegoro.digitalbank.account.presentation.BalanceChartScreen
import com.diponegoro.digitalbank.account.presentation.BalanceChartViewModel
import com.diponegoro.digitalbank.account.presentation.PocketsScreen
import com.diponegoro.digitalbank.account.presentation.PocketsViewModel
import com.diponegoro.digitalbank.authentication.di.AuthModule
import com.diponegoro.digitalbank.authentication.domain.model.User
import com.diponegoro.digitalbank.authentication.presentation.screen.CreatePinScreen
import com.diponegoro.digitalbank.authentication.presentation.screen.EkycScreen
import com.diponegoro.digitalbank.authentication.presentation.screen.EnterPinScreen
import com.diponegoro.digitalbank.authentication.presentation.screen.HomeScreen as SessionsScreen
import com.diponegoro.digitalbank.authentication.presentation.screen.LoginScreen
import com.diponegoro.digitalbank.authentication.presentation.screen.OtpScreen
import com.diponegoro.digitalbank.authentication.presentation.screen.RecoveryScreen
import com.diponegoro.digitalbank.authentication.presentation.screen.RegisterScreen
import com.diponegoro.digitalbank.authentication.presentation.state.AuthEvent
import com.diponegoro.digitalbank.authentication.presentation.viewmodel.AuthViewModel
import com.diponegoro.digitalbank.authentication.presentation.viewmodel.RecoveryViewModel
import com.diponegoro.digitalbank.core.navigation.AppRoutes
import com.diponegoro.digitalbank.core.navigation.MainBottomBar
import com.diponegoro.digitalbank.history.presentation.AnalyticsScreen
import com.diponegoro.digitalbank.history.presentation.AnalyticsViewModel
import com.diponegoro.digitalbank.history.presentation.HistoryScreen
import com.diponegoro.digitalbank.history.presentation.HistoryViewModel
import com.diponegoro.digitalbank.history.presentation.TransactionDetailScreen
import com.diponegoro.digitalbank.history.presentation.TransactionDetailViewModel
import com.diponegoro.digitalbank.home.HomeScreen
import com.diponegoro.digitalbank.home.HomeViewModel
import com.diponegoro.digitalbank.payment.presentation.QrisPayScreen
import com.diponegoro.digitalbank.payment.presentation.QrisPayViewModel
import com.diponegoro.digitalbank.payment.presentation.QrisReceiveScreen
import com.diponegoro.digitalbank.payment.presentation.QrisReceiveViewModel
import com.diponegoro.digitalbank.profile.presentation.NotificationSettingsScreen
import com.diponegoro.digitalbank.profile.presentation.NotificationSettingsViewModel
import com.diponegoro.digitalbank.profile.presentation.NotificationsScreen
import com.diponegoro.digitalbank.profile.presentation.NotificationsViewModel
import com.diponegoro.digitalbank.profile.presentation.ProfileScreen
import com.diponegoro.digitalbank.profile.presentation.ProfileViewModel
import com.diponegoro.digitalbank.profile.presentation.SecurityScreen
import com.diponegoro.digitalbank.profile.presentation.SecurityViewModel
import com.diponegoro.digitalbank.topup.presentation.TopUpScreen
import com.diponegoro.digitalbank.topup.presentation.TopUpViewModel
import com.diponegoro.digitalbank.transfer.presentation.TransferScreen
import com.diponegoro.digitalbank.transfer.presentation.TransferViewModel
import com.diponegoro.digitalbank.virtualcard.di.VirtualCardModule
import com.diponegoro.digitalbank.virtualcard.presentation.screen.VirtualCardScreen
import com.diponegoro.digitalbank.virtualcard.presentation.viewmodel.VirtualCardViewModel

/** Graph navigasi aplikasi: alur Authentication lalu 8 business module setelah login. */
@Composable
fun AuthNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val viewModel: AuthViewModel = viewModel(factory = AuthModule.viewModelFactory)
    val state by viewModel.uiState.collectAsState()

    // Navigasi yang diputuskan ViewModel (mengikuti alur Activity Diagram)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.Navigate -> navController.navigate(event.route) {
                    if (event.clearAll) {
                        popUpTo(navController.graph.id) { inclusive = true } // kosongkan seluruh back stack
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    fun go(route: String) {
        viewModel.clearMessages()
        navController.navigate(route)
    }

    NavHost(navController = navController, startDestination = AuthRoutes.LOGIN, modifier = modifier) {
        authGraph(navController, viewModel, ::go)
        appGraph(navController, viewModel)
    }
}

private fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    viewModel: AuthViewModel,
    go: (String) -> Unit
) {
    composable(AuthRoutes.LOGIN) {
        val state by viewModel.uiState.collectAsState()
        LoginScreen(
            state = state,
            onLogin = viewModel::login,
            onBiometricLogin = viewModel::loginWithBiometric,
            onRefreshBiometric = viewModel::refreshBiometric,
            onNavigateToRegister = { go(AuthRoutes.REGISTER) },
            onNavigateToRecovery = { go(AuthRoutes.RECOVERY) }
        )
    }
    composable(AuthRoutes.REGISTER) {
        val state by viewModel.uiState.collectAsState()
        RegisterScreen(state = state, onRegister = viewModel::register, onNavigateToLogin = { navController.popBackStack() })
    }
    composable(AuthRoutes.OTP) {
        val state by viewModel.uiState.collectAsState()
        OtpScreen(state = state, onVerify = viewModel::verifyOtp, onResend = viewModel::resendOtp)
    }
    composable(AuthRoutes.EKYC) {
        val state by viewModel.uiState.collectAsState()
        EkycScreen(state = state, onSubmit = viewModel::submitEkyc, onRetry = viewModel::retryEkyc, onFinish = viewModel::finishEkyc)
    }
    composable(AuthRoutes.CREATE_PIN) {
        val state by viewModel.uiState.collectAsState()
        CreatePinScreen(state = state, onCreatePin = viewModel::createPin)
    }
    composable(AuthRoutes.ENTER_PIN) {
        val state by viewModel.uiState.collectAsState()
        EnterPinScreen(state = state, onValidate = viewModel::validatePin, onRecoverPin = { go(AuthRoutes.RECOVERY) })
    }
    composable(AuthRoutes.RECOVERY) {
        val recoveryViewModel: RecoveryViewModel = viewModel(factory = AuthModule.viewModelFactory)
        val recoveryState by recoveryViewModel.uiState.collectAsState()
        RecoveryScreen(
            state = recoveryState,
            onSelectType = recoveryViewModel::selectType,
            onRequest = recoveryViewModel::requestRecovery,
            onVerifyOtp = recoveryViewModel::verifyOtp,
            onResendOtp = recoveryViewModel::resendOtp,
            onReset = recoveryViewModel::resetCredential,
            onBack = { navController.popBackStack() }
        )
    }
    composable(AppRoutes.SESSIONS) {
        val state by viewModel.uiState.collectAsState()
        SessionsScreen(
            state = state,
            onRefresh = viewModel::refreshSessions,
            onLogout = viewModel::logout,
            onLogoutAllDevices = viewModel::logoutAllDevices,
            onRecover = { go(AuthRoutes.RECOVERY) }
        )
    }
}

/** Halaman setelah login. Tanpa pengguna aktif (mis. setelah logout) kembali ke login. */
private fun NavGraphBuilder.secured(
    route: String,
    navController: NavHostController,
    auth: AuthViewModel,
    content: @Composable (User, NavBackStackEntry) -> Unit
) {
    composable(route) { entry ->
        val user = auth.uiState.collectAsState().value.currentUser
        if (user == null) {
            LaunchedEffect(Unit) {
                navController.navigate(AuthRoutes.LOGIN) {
                    popUpTo(navController.graph.id) { inclusive = true } // kosongkan seluruh back stack
                    launchSingleTop = true
                }
            }
        } else {
            content(user, entry)
        }
    }
}

private fun NavGraphBuilder.appGraph(navController: NavHostController, auth: AuthViewModel) {
    val back: () -> Unit = { navController.popBackStack() }
    val navigate: (String) -> Unit = { route -> navController.navigate(route) { launchSingleTop = true } }
    val switchTab: (String) -> Unit = { route ->
        if (route == AppRoutes.HOME) {
            // Beranda = akar tab. Jangan navigate + restoreState ke HOME: state yang disimpan
            // dengan popUpTo(HOME) { saveState } ikut tersimpan atas nama HOME, sehingga tab
            // Beranda malah memulihkan tab terakhir (mis. QRIS). Cukup kembali ke HOME.
            navController.popBackStack(AppRoutes.HOME, inclusive = false)
        } else {
            navController.navigate(route) {
                popUpTo(AppRoutes.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    val home: () -> Unit = { navController.popBackStack(AppRoutes.HOME, inclusive = false) }

    @Composable
    fun bottomBar() {
        val current = navController.currentBackStackEntryAsState().value?.destination?.route ?: AppRoutes.HOME
        MainBottomBar(current, switchTab)
    }

    // ---------------- Beranda (FR-06, ringkasan FR-25)
    secured(AppRoutes.HOME, navController, auth) { u, _ ->
        val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(u.userId, u.username))
        val s by vm.state.collectAsState()
        HomeScreen(s, u.username, vm::toggleBalance, navigate) { bottomBar() }
    }

    // ---------------- Account (FR-07 – FR-09)
    secured(AppRoutes.BALANCE_CHART, navController, auth) { u, _ ->
        val vm: BalanceChartViewModel = viewModel(factory = BalanceChartViewModel.factory(u.userId))
        val s by vm.state.collectAsState()
        BalanceChartScreen(s, vm::selectPeriod, back)
    }
    secured(AppRoutes.POCKETS, navController, auth) { u, _ ->
        val vm: PocketsViewModel = viewModel(factory = PocketsViewModel.factory(u.userId))
        val s by vm.state.collectAsState()
        // Kantong juga tab di bottom bar: tampilkan bar, tombol kembali tidak perlu.
        PocketsScreen(s, vm, onBack = null) { bottomBar() }
    }

    // ---------------- Transfer (FR-10 – FR-14)
    secured(AppRoutes.TRANSFER, navController, auth) { u, _ ->
        val vm: TransferViewModel = viewModel(factory = TransferViewModel.factory(u.userId))
        val s by vm.state.collectAsState()
        TransferScreen(s, vm, onClose = home, onOpenDetail = { navigate(AppRoutes.historyDetail(it)) })
    }

    // ---------------- Payment QRIS (FR-15 – FR-17)
    secured(AppRoutes.QRIS_PAY, navController, auth) { u, _ ->
        val vm: QrisPayViewModel = viewModel(factory = QrisPayViewModel.factory(u.userId))
        val s by vm.state.collectAsState()
        QrisPayScreen(s, vm, onReceive = { navigate(AppRoutes.QRIS_RECEIVE) }, onBack = null, onDone = { switchTab(AppRoutes.HOME) }) { bottomBar() }
    }
    secured(AppRoutes.QRIS_RECEIVE, navController, auth) { u, _ ->
        val vm: QrisReceiveViewModel = viewModel(factory = QrisReceiveViewModel.factory(u.userId))
        val s by vm.state.collectAsState()
        QrisReceiveScreen(s, vm, back)
    }

    // ---------------- Top-up (FR-18 – FR-20)
    secured(AppRoutes.TOPUP, navController, auth) { u, _ ->
        val vm: TopUpViewModel = viewModel(factory = TopUpViewModel.factory(u.userId))
        val s by vm.state.collectAsState()
        TopUpScreen(s, vm, back)
    }

    // ---------------- Virtual Card (FR-21 – FR-22)
    secured(AuthRoutes.VIRTUAL_CARD, navController, auth) { u, _ ->
        val cardViewModel: VirtualCardViewModel = viewModel(factory = VirtualCardModule.viewModelFactory(u.userId, u.username))
        val cardState by cardViewModel.uiState.collectAsState()
        VirtualCardScreen(
            state = cardState,
            onBack = back,
            onCreate = cardViewModel::openCreate,
            onConfirmCreate = cardViewModel::createCard,
            onViewDetails = cardViewModel::requestDetails,
            onSubmitPin = cardViewModel::submitPin,
            onHideDetails = cardViewModel::hideDetails,
            onFreeze = cardViewModel::freeze,
            onActivate = cardViewModel::activate,
            onOpenLimit = cardViewModel::openLimit,
            onSaveLimit = cardViewModel::saveLimit,
            onOpenSimulate = cardViewModel::openSimulate,
            onSimulate = cardViewModel::simulate,
            onDismissDialog = cardViewModel::dismissDialog
        )
    }

    // ---------------- Transaction History & Dashboard (FR-23 – FR-25)
    secured(AppRoutes.HISTORY, navController, auth) { u, _ ->
        val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(u.userId))
        val s by vm.state.collectAsState()
        HistoryScreen(s, vm, onOpen = { navigate(AppRoutes.historyDetail(it)) }, onAnalytics = { navigate(AppRoutes.ANALYTICS) }) { bottomBar() }
    }
    secured(AppRoutes.HISTORY_DETAIL, navController, auth) { u, entry ->
        val txId = entry.arguments?.getString("txId").orEmpty()
        val vm: TransactionDetailViewModel = viewModel(key = txId, factory = TransactionDetailViewModel.factory(u.userId, txId))
        val s by vm.state.collectAsState()
        TransactionDetailScreen(s, vm::checkStatus, back)
    }
    secured(AppRoutes.ANALYTICS, navController, auth) { u, _ ->
        val vm: AnalyticsViewModel = viewModel(factory = AnalyticsViewModel.factory(u.userId))
        val s by vm.state.collectAsState()
        AnalyticsScreen(s, vm, back)
    }

    // ---------------- Profile & Notification (FR-27 – FR-29)
    secured(AppRoutes.PROFILE, navController, auth) { u, _ ->
        val vm: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(u.userId, auth::updateCurrentUser))
        val s by vm.state.collectAsState()
        ProfileScreen(s, vm, navigate, onLogout = auth::logout) { bottomBar() }
    }
    secured(AppRoutes.SECURITY, navController, auth) { u, _ ->
        val vm: SecurityViewModel = viewModel(factory = SecurityViewModel.factory(u.userId))
        val s by vm.state.collectAsState()
        SecurityScreen(s, vm, onSessions = { navigate(AppRoutes.SESSIONS) }, onBack = back)
    }
    secured(AppRoutes.NOTIFICATION_SETTINGS, navController, auth) { u, _ ->
        val vm: NotificationSettingsViewModel = viewModel(factory = NotificationSettingsViewModel.factory(u.userId))
        val s by vm.state.collectAsState()
        NotificationSettingsScreen(s, vm, back)
    }
    secured(AppRoutes.NOTIFICATIONS, navController, auth) { u, _ ->
        val vm: NotificationsViewModel = viewModel(factory = NotificationsViewModel.factory(u.userId))
        val s by vm.state.collectAsState()
        NotificationsScreen(s, vm, onOpenTx = { navigate(AppRoutes.historyDetail(it)) }, onBack = back)
    }
}
