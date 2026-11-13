package com.diponegoro.digitalbank.topup.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.R
import com.diponegoro.digitalbank.account.domain.BankAccountStatus
import com.diponegoro.digitalbank.account.domain.formatRupiah
import com.diponegoro.digitalbank.topup.domain.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun TopUpScreen(viewModel: TopUpViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) {
        viewModel.saveReceipt(context.contentResolver, it)
    }
    LaunchedEffect(viewModel) { viewModel.exports.collect { launcher.launch(it) } }
    var tab by rememberSaveable { mutableStateOf("form") }
    var serviceId by rememberSaveable { mutableStateOf("bank") }
    var accountId by rememberSaveable { mutableStateOf("") }
    var methodId by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("ALL") }
    var methodName by rememberSaveable { mutableStateOf("") }
    var methodIdentifier by rememberSaveable { mutableStateOf("") }
    var methodType by rememberSaveable { mutableStateOf(PaymentMethodType.VIRTUAL_ACCOUNT.name) }
    var limitInput by rememberSaveable { mutableStateOf("") }
    var cancelId by rememberSaveable { mutableStateOf<String?>(null) }
    val service = TopUpCatalog.service(serviceId)
    val activeAccounts = state.accounts.filter { it.status == BankAccountStatus.AKTIF }
    val account = activeAccounts.firstOrNull { it.id == accountId } ?: activeAccounts.firstOrNull()
    val eligibleMethods = state.methods.filter {
        it.status == PaymentMethodStatus.AKTIF && if (service.direction == MoneyDirection.CREDIT)
            it.type in listOf(PaymentMethodType.VIRTUAL_ACCOUNT, PaymentMethodType.E_WALLET)
        else it.type == PaymentMethodType.SALDO_AKUN
    }
    val method = eligibleMethods.firstOrNull { it.id == methodId } ?: eligibleMethods.firstOrNull()
    val selected = state.history.firstOrNull { it.id == state.selectedId }
    val enabled = !state.busy && !state.loading

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { if (state.selectedId != null) viewModel.select(null) else onBack() }, enabled = !state.busy) {
            Text(stringResource(R.string.account_back))
        }
        Text(stringResource(R.string.topup_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.topup_demo), style = MaterialTheme.typography.bodySmall)
        if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        TextButton(onClick = viewModel::reload, enabled = enabled) { Text(stringResource(R.string.account_refresh)) }
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            listOf("form" to R.string.topup_new, "history" to R.string.topup_history,
                "settings" to R.string.topup_settings, "notifications" to R.string.topup_notifications).forEach { (key, label) ->
                TextButton(onClick = { tab = key; viewModel.select(null) }, enabled = enabled) { Text(stringResource(label)) }
            }
        }
        if (selected != null) {
            TopUpDetails(selected)
            if (selected.status == TopUpStatus.PENDING) {
                Text(stringResource(R.string.topup_pending_help))
                Button(onClick = { viewModel.settle(selected.id, TopUpStatus.BERHASIL) }, enabled = enabled && state.verified) { Text(stringResource(R.string.topup_simulate_success)) }
                OutlinedButton(onClick = { viewModel.settle(selected.id, TopUpStatus.GAGAL) }, enabled = enabled) { Text(stringResource(R.string.topup_simulate_failure)) }
                OutlinedButton(onClick = { cancelId = selected.id }, enabled = enabled) { Text(stringResource(R.string.topup_cancel_pending)) }
            }
            if (selected.status == TopUpStatus.BERHASIL) {
                Button(onClick = { viewModel.export(selected.id) }, enabled = enabled) { Text(stringResource(R.string.topup_receipt)) }
            }
            TextButton(onClick = { viewModel.select(null); tab = "form"; amount = "" }, enabled = enabled) { Text(stringResource(R.string.topup_new)) }
        } else when (tab) {
            "form" -> {
                if (!state.verified) Text(stringResource(R.string.topup_kyc))
                TopUpChoice(stringResource(R.string.topup_service), serviceId, TopUpCatalog.services.map { it.id to it.name }, enabled) {
                    serviceId = it; methodId = ""; target = ""
                }
                Text(stringResource(if (service.direction == MoneyDirection.CREDIT) R.string.topup_credit_help else R.string.topup_debit_help))
                TopUpChoice(stringResource(if (service.direction == MoneyDirection.CREDIT) R.string.topup_destination_account else R.string.topup_source_account),
                    account?.id.orEmpty(), activeAccounts.map { it.id to "${it.alias} • ${it.number}" }, enabled) { accountId = it }
                if (!state.loading && activeAccounts.isEmpty()) Text(stringResource(R.string.topup_no_accounts))
                account?.let { Text(stringResource(R.string.account_available, formatRupiah(it.availableBalance))) }
                TopUpChoice(stringResource(R.string.topup_method), method?.id.orEmpty(), eligibleMethods.map { it.id to it.name }, enabled) { methodId = it }
                TopUpInput(if (service.direction == MoneyDirection.CREDIT) account?.number.orEmpty() else target,
                    { target = it }, R.string.topup_target, enabled && service.direction == MoneyDirection.DEBIT)
                TopUpInput(amount, { amount = it }, R.string.topup_amount, enabled)
                Text(stringResource(R.string.topup_range, formatRupiah(service.minimum), formatRupiah(service.maximum)))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("50000", "100000", "200000").forEach { value ->
                        TextButton(onClick = { amount = value }, enabled = enabled) { Text(formatRupiah(value.toLong())) }
                    }
                }
                Text(stringResource(R.string.topup_fee_value, formatRupiah(service.fee)))
                Text(stringResource(R.string.topup_limit_summary, formatRupiah(state.dailyLimit), formatRupiah(state.usedToday)))
                Button(onClick = {
                    viewModel.preview(TopUpDraft(account?.id.orEmpty(), serviceId,
                        if (service.direction == MoneyDirection.CREDIT) account?.number.orEmpty() else target.trim(),
                        if (Regex("[0-9]+").matches(amount)) amount.toLongOrNull() ?: 0 else 0,
                        method?.id.orEmpty(), UUID.randomUUID().toString()))
                }, enabled = enabled && state.verified && account != null && method != null) { Text(stringResource(R.string.account_continue)) }
            }
            "history" -> {
                TopUpChoice(stringResource(R.string.topup_status_filter), filter,
                    listOf("ALL" to stringResource(R.string.topup_all)) + TopUpStatus.entries.map { it.name to it.label }, enabled) { filter = it }
                val rows = state.history.filter { filter == "ALL" || it.status.name == filter }
                if (rows.isEmpty()) Text(stringResource(R.string.topup_empty_history))
                rows.forEach { transaction ->
                    OutlinedButton(onClick = { viewModel.select(transaction.id) }, modifier = Modifier.fillMaxWidth(), enabled = enabled) {
                        Column { Text(transaction.serviceName); Text("${formatRupiah(transaction.amount)} • ${transaction.status.label}"); Text(topUpTime(transaction.createdAt)) }
                    }
                }
            }
            "settings" -> {
                Text(stringResource(R.string.topup_daily_limit), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.topup_limit_summary, formatRupiah(state.dailyLimit), formatRupiah(state.usedToday)))
                TopUpInput(limitInput, { limitInput = it }, R.string.topup_limit_input, enabled)
                Button(onClick = { viewModel.saveLimit(limitInput) }, enabled = enabled) { Text(stringResource(R.string.topup_save_limit)) }
                HorizontalDivider()
                Text(stringResource(R.string.topup_saved_methods), style = MaterialTheme.typography.titleMedium)
                state.methods.forEach { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.name); Text(item.type.label)
                            Text(stringResource(R.string.topup_masked_method, item.identifier.takeLast(4)))
                            if (item.isDefault) Text(stringResource(R.string.topup_default))
                            else TextButton(onClick = { viewModel.defaultMethod(item.id) }, enabled = enabled && item.status == PaymentMethodStatus.AKTIF) { Text(stringResource(R.string.topup_set_default)) }
                        }
                    }
                }
                Text(stringResource(R.string.topup_methods_help))
                TopUpChoice(stringResource(R.string.topup_method_type), methodType,
                    listOf(PaymentMethodType.VIRTUAL_ACCOUNT, PaymentMethodType.E_WALLET).map { it.name to it.label }, enabled) { methodType = it }
                OutlinedTextField(methodName, { methodName = it }, label = { Text(stringResource(R.string.topup_method_name)) }, singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth())
                TopUpInput(methodIdentifier, { methodIdentifier = it }, R.string.topup_method_number, enabled)
                Button(onClick = { viewModel.saveMethod(PaymentMethodType.valueOf(methodType), methodName, methodIdentifier.trim()) }, enabled = enabled) { Text(stringResource(R.string.topup_save_method)) }
            }
            "notifications" -> {
                if (state.notifications.isEmpty()) Text(stringResource(R.string.topup_empty_notifications))
                state.notifications.forEach { notification ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(notification.message); Text(topUpTime(notification.createdAt))
                            TextButton(onClick = { viewModel.select(notification.transactionId) }, enabled = enabled) { Text(stringResource(R.string.topup_detail)) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
    state.quote?.let { quote ->
        // PIN hanya berada dalam memori Composable; tidak memakai rememberSaveable.
        var pin by remember(quote.draft.operationId) { mutableStateOf("") }
        AlertDialog(onDismissRequest = viewModel::dismissQuote,
            title = { Text(stringResource(R.string.topup_confirm)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(quote.service.name); Text(quote.service.provider); Text(quote.accountNumber); Text(quote.draft.target)
                Text(quote.methodName); Text(stringResource(R.string.topup_amount_value, formatRupiah(quote.draft.amount)))
                Text(stringResource(R.string.topup_fee_value, formatRupiah(quote.fee)))
                Text(stringResource(R.string.topup_total_value, formatRupiah(quote.total)))
                Text(stringResource(if (quote.service.direction == MoneyDirection.CREDIT) R.string.topup_credit_help else R.string.topup_debit_help))
                OutlinedTextField(pin, { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                    label = { Text(stringResource(R.string.topup_pin)) }, singleLine = true, enabled = !state.busy,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { TextButton(onClick = { val value = pin; pin = ""; viewModel.confirm(value) }, enabled = !state.busy && pin.length == 6) { Text(stringResource(R.string.account_confirm)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissQuote, enabled = !state.busy) { Text(stringResource(R.string.account_cancel)) } })
    }
    cancelId?.let { id ->
        AlertDialog(onDismissRequest = { if (!state.busy) cancelId = null },
            title = { Text(stringResource(R.string.topup_cancel_pending)) }, text = { Text(stringResource(R.string.topup_cancel_terms)) },
            confirmButton = { TextButton(onClick = { cancelId = null; viewModel.settle(id, TopUpStatus.DIBATALKAN) }, enabled = !state.busy) { Text(stringResource(R.string.account_confirm)) } },
            dismissButton = { TextButton(onClick = { cancelId = null }, enabled = !state.busy) { Text(stringResource(R.string.account_cancel)) } })
    }
}

@Composable
private fun TopUpChoice(label: String, selected: String, items: List<Pair<String, String>>, enabled: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled && items.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(items.firstOrNull { it.first == selected }?.second ?: stringResource(R.string.topup_choose))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { expanded = false; onSelect(id) }) }
            }
        }
    }
}

@Composable
private fun TopUpInput(value: String, onChange: (String) -> Unit, label: Int, enabled: Boolean) {
    OutlinedTextField(value, onChange, label = { Text(stringResource(label)) }, singleLine = true,
        enabled = enabled, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun TopUpDetails(transaction: TopUpTransaction) {
    Text(stringResource(R.string.topup_detail), style = MaterialTheme.typography.titleLarge)
    Text(transaction.serviceName); Text(transaction.status.label, style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.topup_reference_value, transaction.reference)); Text(topUpTime(transaction.createdAt))
    transaction.completedAt?.let { Text(stringResource(R.string.topup_completed_value, topUpTime(it))) }
    Text(stringResource(R.string.topup_account_value, transaction.accountNumber)); Text(stringResource(R.string.topup_target_value, transaction.target))
    Text(transaction.methodName); Text(stringResource(R.string.topup_amount_value, formatRupiah(transaction.amount)))
    Text(stringResource(R.string.topup_fee_value, formatRupiah(transaction.fee))); Text(stringResource(R.string.topup_total_value, formatRupiah(transaction.total)))
    Text(stringResource(if (transaction.direction == MoneyDirection.CREDIT) R.string.topup_credit_help else R.string.topup_debit_help))
    transaction.balanceAfter?.let { Text(stringResource(R.string.account_balance_after, formatRupiah(it))) }
    if (transaction.status == TopUpStatus.PENDING) Text(stringResource(R.string.topup_expires_value, topUpTime(transaction.expiresAt)))
    Text(transaction.note)
}

private fun topUpTime(epoch: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("id-ID")).format(Date(epoch))
