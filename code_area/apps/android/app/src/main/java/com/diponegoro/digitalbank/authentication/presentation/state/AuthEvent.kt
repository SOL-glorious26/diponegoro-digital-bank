package com.diponegoro.digitalbank.authentication.presentation.state

sealed interface AuthEvent {
    data class Navigate(val route: String, val clearAll: Boolean = false) : AuthEvent
}