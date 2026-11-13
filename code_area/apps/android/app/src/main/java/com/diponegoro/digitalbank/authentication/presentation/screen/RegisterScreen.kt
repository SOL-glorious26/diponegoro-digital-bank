package com.diponegoro.digitalbank.authentication.presentation.screen

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.diponegoro.digitalbank.authentication.presentation.components.AuthScreenContainer
import com.diponegoro.digitalbank.authentication.presentation.components.AuthTextField
import com.diponegoro.digitalbank.authentication.presentation.components.LoadingButton
import com.diponegoro.digitalbank.authentication.presentation.components.MessageBanner
import com.diponegoro.digitalbank.authentication.presentation.components.PasswordField
import com.diponegoro.digitalbank.authentication.presentation.state.AuthUiState

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onRegister: (username: String, email: String, phone: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    AuthScreenContainer(
        title = "Daftar Akun",
        subtitle = "Daftar menggunakan email atau nomor telepon"
    ) {
        AuthTextField(username, { username = it }, "Username")
        AuthTextField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
        AuthTextField(phone, { phone = it }, "Nomor telepon", keyboardType = KeyboardType.Phone)
        PasswordField(password, { password = it })
        MessageBanner(state.errorMessage, state.infoMessage)
        LoadingButton("Daftar", state.isLoading) { onRegister(username, email, phone, password) }
        TextButton(onClick = onNavigateToLogin) { Text("Sudah punya akun? Login") }
    }
}