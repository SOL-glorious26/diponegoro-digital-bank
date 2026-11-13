package com.diponegoro.digitalbank.authentication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diponegoro.digitalbank.authentication.domain.model.AuthResult
import com.diponegoro.digitalbank.authentication.domain.model.AccountStatus
import com.diponegoro.digitalbank.authentication.domain.model.EkycStatus
import com.diponegoro.digitalbank.authentication.domain.model.OtpPurpose
import com.diponegoro.digitalbank.authentication.domain.model.Session
import com.diponegoro.digitalbank.authentication.domain.model.User
import com.diponegoro.digitalbank.authentication.domain.usecase.CheckEkycStatusUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.CreateTransactionPinUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.GetActiveSessionsUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.GetTransactionPinUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.GetUserUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.IsBiometricAvailableUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.LoginUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.LoginWithBiometricUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.LogoutAllDevicesUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.LogoutUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.RegisterUserUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.SendOtpUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.SubmitEkycUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.ValidateTransactionPinUseCase
import com.diponegoro.digitalbank.authentication.domain.usecase.VerifyOtpUseCase
import com.diponegoro.digitalbank.authentication.presentation.navigation.AuthRoutes
import com.diponegoro.digitalbank.authentication.presentation.state.AuthEvent
import com.diponegoro.digitalbank.authentication.presentation.state.AuthUiState
import com.diponegoro.digitalbank.core.di.CoreModule
import com.diponegoro.digitalbank.core.security.InactivityMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val registerUserUseCase: RegisterUserUseCase,
    private val sendOtpUseCase: SendOtpUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val submitEkycUseCase: SubmitEkycUseCase,
    private val checkEkycStatusUseCase: CheckEkycStatusUseCase,
    private val loginUseCase: LoginUseCase,
    private val loginWithBiometricUseCase: LoginWithBiometricUseCase,
    private val isBiometricAvailableUseCase: IsBiometricAvailableUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val getTransactionPinUseCase: GetTransactionPinUseCase,
    private val createTransactionPinUseCase: CreateTransactionPinUseCase,
    private val validateTransactionPinUseCase: ValidateTransactionPinUseCase,
    private val getActiveSessionsUseCase: GetActiveSessionsUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val logoutAllDevicesUseCase: LogoutAllDevicesUseCase
) : ViewModel() {

    private companion object {
        const val DEVICE_ID = "Android Emulator"
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refreshBiometric()
        // Varian online: refresh token ditolak server (logout di perangkat lain / sesi kedaluwarsa).
        viewModelScope.launch {
            CoreModule.sessionExpired.collect {
                if (_uiState.value.currentSession != null) {
                    resetToLoggedOut("Sesi berakhir. Silakan login kembali.")
                }
            }
        }
    }

    // ---------- helpers ----------

    private fun setLoading() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
    }

    private fun fail(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    private fun navigate(route: String, clearAll: Boolean = false) {
        _events.trySend(AuthEvent.Navigate(route, clearAll))
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    fun refreshBiometric() {
        viewModelScope.launch {
            val available = isBiometricAvailableUseCase()
            _uiState.update { it.copy(biometricAvailable = available) }
        }
    }

    // ---------- Register & OTP ----------

    fun register(username: String, email: String, phone: String, password: String) {
        viewModelScope.launch {
            setLoading()
            when (val result = registerUserUseCase(username, email, phone, password)) {
                is AuthResult.Failure -> fail(result.message)
                is AuthResult.Success -> {
                    _uiState.update { it.copy(pendingUserId = result.data.userId) }
                    issueRegistrationOtp(result.data.userId, navigateToOtp = true)
                }
            }
        }
    }

    fun resendOtp() {
        val userId = _uiState.value.pendingUserId ?: return fail("Sesi registrasi tidak ditemukan")
        viewModelScope.launch {
            setLoading()
            issueRegistrationOtp(userId, navigateToOtp = false)
        }
    }

    private suspend fun issueRegistrationOtp(userId: String, navigateToOtp: Boolean) {
        when (val result = sendOtpUseCase(userId, OtpPurpose.REGISTRATION)) {
            is AuthResult.Failure -> fail(result.message)
            is AuthResult.Success -> {
                _uiState.update {
                    it.copy(isLoading = false, demoOtpCode = result.data, infoMessage = "Kode OTP dikirim (simulasi)")
                }
                if (navigateToOtp) navigate(AuthRoutes.OTP)
            }
        }
    }

    fun verifyOtp(code: String) {
        val userId = _uiState.value.pendingUserId ?: return fail("Sesi registrasi tidak ditemukan")
        viewModelScope.launch {
            setLoading()
            when (val result = verifyOtpUseCase(userId, OtpPurpose.REGISTRATION, code)) {
                is AuthResult.Failure -> fail(result.message) // pengguna dapat meminta OTP ulang
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, demoOtpCode = null) }
                    // OTP sudah terpakai: layar OTP dikeluarkan dari back stack agar tombol Kembali
                    // tidak membawa pengguna ke OTP yang sudah selesai (DEF-07).
                    navigate(AuthRoutes.EKYC, clearAll = true)
                }
            }
        }
    }

    // ---------- e-KYC ----------

    fun submitEkyc(nik: String, selfieCaptured: Boolean) {
        val userId = _uiState.value.pendingUserId ?: return fail("Sesi registrasi tidak ditemukan")
        viewModelScope.launch {
            setLoading()
            when (val submit = submitEkycUseCase(userId, nik, selfieCaptured)) {
                is AuthResult.Failure -> fail(submit.message)
                is AuthResult.Success -> {
                    var status = submit.data.status
                    _uiState.update { it.copy(isLoading = false, ekycStatus = status) }
                    while (status == EkycStatus.PROCESSING) {
                        delay(800)
                        when (val check = checkEkycStatusUseCase(userId)) {
                            is AuthResult.Failure -> {
                                _uiState.update { it.copy(ekycStatus = null, errorMessage = check.message) }
                                return@launch
                            }
                            is AuthResult.Success -> status = check.data.status
                        }
                    }
                    _uiState.update { it.copy(ekycStatus = status) }
                }
            }
        }
    }

    fun retryEkyc() {
        _uiState.update { it.copy(ekycStatus = null, errorMessage = null) }
    }

    fun finishEkyc() {
        _uiState.update { it.copy(ekycStatus = null, pendingUserId = null) }
        refreshBiometric()
        navigate(AuthRoutes.LOGIN, clearAll = true)
    }

    // ---------- Login ----------

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            setLoading()
            when (val result = loginUseCase(identifier, password, DEVICE_ID)) {
                is AuthResult.Failure -> fail(result.message)
                is AuthResult.Success -> onLoginSuccess(result.data)
            }
        }
    }

    fun loginWithBiometric() {
        viewModelScope.launch {
            setLoading()
            when (val result = loginWithBiometricUseCase(DEVICE_ID)) {
                is AuthResult.Failure -> fail(result.message)
                is AuthResult.Success -> onLoginSuccess(result.data)
            }
        }
    }

    private suspend fun onLoginSuccess(session: Session) {
        val user = getUserUseCase(session.userId)
        // DEF-08: pengguna yang e-KYC-nya belum disetujui harus menyelesaikan e-KYC dulu (FR-02),
        // bukan langsung diarahkan ke pembuatan PIN.
        if (user?.accountStatus != AccountStatus.FINANCIAL_ENABLED) {
            val ekyc = (checkEkycStatusUseCase(session.userId) as? AuthResult.Success)?.data?.status
            if (ekyc != EkycStatus.APPROVED) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentSession = session,
                        currentUser = user,
                        pendingUserId = session.userId,
                        ekycStatus = null,
                        errorMessage = if (ekyc == EkycStatus.REJECTED) "e-KYC sebelumnya ditolak. Periksa data lalu kirim ulang." else null
                    )
                }
                navigate(AuthRoutes.EKYC, clearAll = true)
                return
            }
        }
        val pin = getTransactionPinUseCase(session.userId)
        _uiState.update {
            it.copy(
                isLoading = false,
                currentSession = session,
                currentUser = user,
                pinBlocked = pin?.isBlocked() == true,
                pinVerified = false
            )
        }
        navigate(if (pin == null) AuthRoutes.CREATE_PIN else AuthRoutes.ENTER_PIN, clearAll = true)
    }

    // ---------- Transaction PIN ----------

    fun createPin(pin: String, confirmPin: String) {
        val userId = _uiState.value.currentUser?.userId ?: return fail("Sesi tidak ditemukan")
        viewModelScope.launch {
            setLoading()
            when (val result = createTransactionPinUseCase(userId, pin, confirmPin)) {
                is AuthResult.Failure -> fail(result.message)
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    navigate(AuthRoutes.ENTER_PIN, clearAll = true)
                }
            }
        }
    }

    fun validatePin(pin: String) {
        val userId = _uiState.value.currentUser?.userId ?: return fail("Sesi tidak ditemukan")
        viewModelScope.launch {
            setLoading()
            when (val result = validateTransactionPinUseCase(userId, pin)) {
                is AuthResult.Failure -> {
                    val blocked = getTransactionPinUseCase(userId)?.isBlocked() == true
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message, pinBlocked = blocked)
                    }
                }
                is AuthResult.Success -> {
                    val sessions = getActiveSessionsUseCase(userId)
                    _uiState.update {
                        it.copy(isLoading = false, pinVerified = true, pinBlocked = false, activeSessions = sessions)
                    }
                    startInactivityWatch()
                    navigate(AuthRoutes.HOME, clearAll = true)
                }
            }
        }
    }

    // ---------- Session ----------

    fun refreshSessions() {
        val userId = _uiState.value.currentUser?.userId ?: return
        viewModelScope.launch {
            val sessions = getActiveSessionsUseCase(userId)
            _uiState.update { it.copy(activeSessions = sessions) }
        }
    }

    fun logout() {
        val sessionId = _uiState.value.currentSession?.sessionId ?: return
        viewModelScope.launch {
            setLoading()
            logoutUseCase(sessionId)
            resetToLoggedOut("Anda telah logout")
        }
    }

    fun logoutAllDevices() {
        val userId = _uiState.value.currentUser?.userId ?: return
        viewModelScope.launch {
            setLoading()
            logoutAllDevicesUseCase(userId)
            resetToLoggedOut("Semua sesi di semua perangkat telah diakhiri")
        }
    }

    /** Dipanggil modul Profile setelah data pengguna berubah (username, kontak, foto). */
    fun updateCurrentUser(user: User) {
        _uiState.update { it.copy(currentUser = user) }
    }

    private var inactivityJob: Job? = null

    /** FR-05: mulai memantau ketidakaktifan setelah PIN terverifikasi. */
    private fun startInactivityWatch() {
        inactivityJob?.cancel()
        InactivityMonitor.touch()
        inactivityJob = viewModelScope.launch {
            while (true) {
                delay(10_000)
                if (InactivityMonitor.isExpired()) {
                    expireSession()
                    break
                }
            }
        }
    }

    /** FR-05: sesi berakhir otomatis saat tidak aktif. */
    fun expireSession() {
        val sessionId = _uiState.value.currentSession?.sessionId ?: return
        viewModelScope.launch {
            logoutUseCase(sessionId)
            resetToLoggedOut("Sesi berakhir otomatis karena tidak ada aktivitas. Silakan login kembali.")
        }
    }

    private suspend fun resetToLoggedOut(info: String) {
        inactivityJob?.cancel()
        inactivityJob = null
        val biometric = isBiometricAvailableUseCase()
        _uiState.value = AuthUiState(biometricAvailable = biometric, infoMessage = info)
        navigate(AuthRoutes.LOGIN, clearAll = true)
    }
}