package com.example.diponegorodigitalbank

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DigitalBankApp()
            }
        }
    }
}

private enum class Screen { LOGIN, DASHBOARD, TRANSFER, HISTORY, RECEIPT }

@Composable
fun DigitalBankApp() {
    val repository = remember { BankRepository() }
    var screen by remember { mutableStateOf(Screen.LOGIN) }
    var lastReceipt by remember { mutableStateOf<TransferReceipt?>(null) }

    when (screen) {
        Screen.LOGIN -> LoginScreen(
            onLogin = { screen = Screen.DASHBOARD },
            repository = repository
        )
        Screen.DASHBOARD -> DashboardScreen(
            repository = repository,
            onTransfer = { screen = Screen.TRANSFER },
            onHistory = { screen = Screen.HISTORY },
            onLogout = { screen = Screen.LOGIN }
        )
        Screen.TRANSFER -> TransferScreen(
            repository = repository,
            onBack = { screen = Screen.DASHBOARD },
            onSuccess = {
                lastReceipt = it
                screen = Screen.RECEIPT
            }
        )
        Screen.HISTORY -> HistoryScreen(
            repository = repository,
            onBack = { screen = Screen.DASHBOARD }
        )
        Screen.RECEIPT -> ReceiptScreen(
            receipt = lastReceipt,
            onDone = { screen = Screen.DASHBOARD }
        )
    }
}

@Composable
private fun LoginScreen(
    onLogin: () -> Unit,
    repository: BankRepository
) {
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Diponegoro Digital Bank", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Login Simulator")
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )

        if (error.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (repository.login(username, pin)) onLogin()
                else error = "Username atau PIN salah. Demo: user / 123456"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
    }
}

@Composable
private fun DashboardScreen(
    repository: BankRepository,
    onTransfer: () -> Unit,
    onHistory: () -> Unit,
    onLogout: () -> Unit
) {
    val account = repository.user.accounts.first()
    val income = repository.history.transactions
        .filter { it.transactionType == "Pemasukan" }
        .sumOf { it.amount }
    val expense = repository.history.transactions
        .filter { it.transactionType == "Transfer" }
        .sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    TextButton(onClick = onLogout) { Text("Logout") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Halo, ${repository.user.username}", style = MaterialTheme.typography.titleLarge)
                Text("Selamat datang di Diponegoro Digital Bank")
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Saldo", style = MaterialTheme.typography.labelLarge)
                        Text(
                            money(account.balance),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(account.bankName)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onTransfer,
                        modifier = Modifier.weight(1f)
                    ) { Text("Transfer") }
                    OutlinedButton(
                        onClick = onHistory,
                        modifier = Modifier.weight(1f)
                    ) { Text("Riwayat") }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Ringkasan Keuangan", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Pemasukan: ${money(income)}")
                        Text("Pengeluaran: ${money(expense)}")
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferScreen(
    repository: BankRepository,
    onBack: () -> Unit,
    onSuccess: (TransferReceipt) -> Unit
) {
    var bank by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var verified by remember { mutableStateOf(false) }

    val amount = amountText.toBigDecimalOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer Antarbank") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Kembali") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("1. Validasi Rekening", style = MaterialTheme.typography.titleMedium) }

            item {
                OutlinedTextField(
                    value = bank,
                    onValueChange = { bank = it; verified = false },
                    label = { Text("Nama Bank") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it; verified = false },
                    label = { Text("Nomor Rekening") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Button(
                    onClick = {
                        val recipient = repository.validateAccount(bank, accountNumber)
                        if (recipient != null) {
                            recipientName = recipient.recipientName
                            verified = true
                            error = ""
                        } else {
                            verified = false
                            error = "Rekening tidak ditemukan."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Validasi Rekening") }
            }

            if (verified) {
                item { Text("Penerima: $recipientName") }
            }

            item { Text("2. Nominal Transfer", style = MaterialTheme.typography.titleMedium) }
            item {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Nominal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan (opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Text("3. Konfirmasi & PIN", style = MaterialTheme.typography.titleMedium) }
            item {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN 6 Digit") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (error.isNotEmpty()) {
                item { Text(error, color = MaterialTheme.colorScheme.error) }
            }

            item {
                Button(
                    onClick = {
                        val result = repository.processTransfer(
                            bank = bank,
                            accountNumber = accountNumber,
                            amount = amount,
                            note = note,
                            pin = pin
                        )
                        result.onSuccess {
                            onSuccess(it)
                        }.onFailure {
                            error = it.message ?: "Transfer gagal."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Konfirmasi Transfer")
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    repository: BankRepository,
    onBack: () -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Semua") }

    val transactions = repository.history.search(keyword).filter {
        filter == "Semua" || it.transactionType == filter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Transaksi") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Kembali") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(20.dp)) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text("Cari transaksi") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Semua", "Transfer", "Pemasukan").forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions) { transaction ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(transaction.transactionType)
                            Text(transaction.recipientName ?: "-")
                            Text(money(transaction.amount))
                            Text(transaction.status)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptScreen(
    receipt: TransferReceipt?,
    onDone: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Bukti Transfer") }) }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Transfer Berhasil", style = MaterialTheme.typography.headlineSmall)
            if (receipt != null) {
                Text("ID Transaksi: ${receipt.transferId}")
                Text("Penerima: ${receipt.recipientName}")
                Text("Bank: ${receipt.bankName}")
                Text("Rekening: ${receipt.accountNumber}")
                Text("Nominal: ${money(receipt.amount)}")
                Text("Status: ${receipt.status}")
                Text("Catatan: ${receipt.note.ifBlank { "-" }}")
            }
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Kembali ke Dashboard")
            }
        }
    }
}

private fun money(value: java.math.BigDecimal): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value)
