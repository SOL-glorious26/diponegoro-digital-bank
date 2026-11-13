package com.diponegoro.digitalbank.authentication.presentation.state

import com.diponegoro.digitalbank.authentication.domain.model.EkycStatus
import com.diponegoro.digitalbank.authentication.domain.model.Session
import com.diponegoro.digitalbank.authentication.domain.model.User

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    // register / otp / e-kyc
    val pendingUserId: String? = null,
    val demoOtpCode: String? = null,
    val ekycStatus: EkycStatus? = null,
    // login & session
    val biometricAvailable: Boolean = false,
    val currentUser: User? = null,
    val currentSession: Session? = null,
    val activeSessions: List<Session> = emptyList(),
    // PIN
    val pinBlocked: Boolean = false,
    val pinVerified: Boolean = false
)