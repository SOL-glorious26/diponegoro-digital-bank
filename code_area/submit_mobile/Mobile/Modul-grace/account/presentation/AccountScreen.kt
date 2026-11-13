package com.diponegoro.digitalbank.account.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.R
import com.diponegoro.digitalbank.account.domain.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun AccountScreen(viewModel: AccountViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val copyLabel = stringResource(R.string.account_copy)
    val clipboard = remember(context) { context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        viewModel.saveExport(context.contentResolver, it)
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) {
        viewModel.saveExport(context.contentResolver, it)
    }
    LaunchedEffect(viewModel) { viewModel.exports.collect {
        if (it.format == StatementFormat.PDF) pdfLauncher.launch(it.name) else csvLauncher.launch(it.name)
    } }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var opening by rememberSaveable { mutableStateOf(false) }
    var confirmOpen by rememberSaveable { mutableStateOf(false) }
    var confirmClose by rememberSaveable { mutableStateOf(false) }
    var newAlias by rememberSaveable { mutableStateOf("") }
    var alias by rememberSaveable { mutableStateOf("") }
    var typeName by rememberSaveable { mutableStateOf(BankAccountType.TABUNGAN.name) }
    var operationId by rememberSaveable { mutableStateOf("") }
    var transactionId by rememberSaveable { mutableStateOf<String?>(null) }
    var copied by rememberSaveable { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("id-ID")) }
    var start by rememberSaveable { mutableStateOf(dateFormat.format(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.time)) }
    var end by rememberSaveable { mutableStateOf(dateFormat.format(Date())) }
    val account = state.accounts.firstOrNull { it.id == selectedId }
    LaunchedEffect(selectedId) {
        copied = false
        if (account != null) viewModel.history(account.id, start, end)
    }
    LaunchedEffect(state.message) {
        if (state.message != null) { opening = false; confirmOpen = false; confirmClose = false }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { if (selectedId != null) selectedId = null else onBack() }) { Text(stringResource(R.string.account_back)) }
        Text(stringResource(if (account == null) R.string.account_title else R.string.account_detail), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.account_demo), style = MaterialTheme.typography.bodySmall)
        if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        TextButton(onClick = viewModel::reload, enabled = !state.busy) { Text(stringResource(R.string.account_refresh)) }
        if (account == null) {
            if (!state.verified) Text(stringResource(R.string.account_kyc_required))
            Button(onClick = {
                newAlias = ""; operationId = UUID.randomUUID().toString(); opening = true
            }, enabled = !state.loading && !state.busy && state.verified) { Text(stringResource(R.string.account_open)) }
            if (!state.loading && state.accounts.isEmpty()) Text(stringResource(R.string.account_empty))
            state.accounts.forEach { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.alias, style = MaterialTheme.typography.titleMedium)
                        Text(item.number)
                        Text("${item.type.label} • ${item.status.label} • ${item.currency}")
                        Text(formatRupiah(item.balance), style = MaterialTheme.typography.titleLarge)
                        if (item.isPrimary) Text(stringResource(R.string.account_primary))
                        Text(stringResource(R.string.account_owner, state.owner))
                        TextButton(onClick = { clipboard.setPrimaryClip(android.content.ClipData.newPlainText(copyLabel, item.number)); copied = true }) { Text(stringResource(R.string.account_copy)) }
                        TextButton(onClick = { alias = item.alias; selectedId = item.id }, enabled = !state.busy && state.verified) { Text(stringResource(R.string.account_detail)) }
                    }
                }
            }
        } else {
            Text(account.alias, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.account_owner, state.owner))
            Text(account.number)
            Text("${account.type.label} • ${account.status.label} • ${account.currency}")
            Text(stringResource(R.string.account_balance, formatRupiah(account.balance)))
            Text(stringResource(R.string.account_available, formatRupiah(account.availableBalance)))
            Text(stringResource(R.string.account_opened, dateFormat.format(Date(account.openedAt))))
            account.closedAt?.let { Text(stringResource(R.string.account_closed, dateFormat.format(Date(it)))) }
            TextButton(onClick = { clipboard.setPrimaryClip(android.content.ClipData.newPlainText(copyLabel, account.number)); copied = true }) { Text(stringResource(R.string.account_copy)) }
            if (account.status != BankAccountStatus.TERTUTUP) {
                OutlinedTextField(value = alias, onValueChange = { alias = it }, label = { Text(stringResource(R.string.account_alias)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.rename(account.id, alias) }, enabled = !state.busy) { Text(stringResource(R.string.account_save_alias)) }
                OutlinedButton(onClick = { viewModel.primary(account.id) }, enabled = !state.busy && !account.isPrimary && account.status == BankAccountStatus.AKTIF) {
                    Text(stringResource(if (account.isPrimary) R.string.account_primary else R.string.account_set_primary))
                }
                OutlinedButton(onClick = { confirmClose = true }, enabled = !state.busy) { Text(stringResource(R.string.account_close)) }
            }
            HorizontalDivider()
            Text(stringResource(R.string.account_history), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(start, { start = it }, label = { Text(stringResource(R.string.account_start)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(end, { end = it }, label = { Text(stringResource(R.string.account_end)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { viewModel.history(account.id, start, end) }, enabled = !state.busy) { Text(stringResource(R.string.account_filter)) }
            if (state.transactions.isEmpty()) Text(stringResource(R.string.account_no_transactions))
            state.transactions.forEach { row ->
                OutlinedButton(onClick = { transactionId = row.id }, modifier = Modifier.fillMaxWidth()) {
                    Column { Text("${dateFormat.format(Date(row.createdAt))} • ${row.type}"); Text("${formatRupiah(row.amount)} • ${row.status}") }
                }
            }
            Text(stringResource(R.string.account_statement), style = MaterialTheme.typography.titleMedium)
            StatementFormat.entries.forEach { format ->
                OutlinedButton(onClick = { viewModel.export(account.id, start, end, format) }, enabled = !state.busy && state.verified) {
                    Text(stringResource(R.string.account_export, format.name))
                }
            }
        }
        if (copied) Text(stringResource(R.string.account_copied))
        Spacer(Modifier.height(20.dp))
    }
    if (opening) AlertDialog(
        onDismissRequest = { if (!state.busy) opening = false },
        title = { Text(stringResource(R.string.account_open)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.account_open_terms))
            OutlinedTextField(newAlias, { newAlias = it }, label = { Text(stringResource(R.string.account_alias)) }, singleLine = true)
            AccountRules.supportedTypes.forEach { type ->
                Row { RadioButton(selected = typeName == type.name, onClick = { typeName = type.name }); TextButton(onClick = { typeName = type.name }) { Text(type.label) } }
            }
        } },
        confirmButton = { TextButton(onClick = { confirmOpen = true }, enabled = !state.busy && newAlias.trim().isNotEmpty()) { Text(stringResource(R.string.account_continue)) } },
        dismissButton = { TextButton(onClick = { opening = false }, enabled = !state.busy) { Text(stringResource(R.string.account_cancel)) } }
    )
    if (confirmOpen) AlertDialog(
        onDismissRequest = { if (!state.busy) confirmOpen = false },
        title = { Text(stringResource(R.string.account_confirm_open)) },
        text = { Column { Text(newAlias); Text(BankAccountType.valueOf(typeName).label); Text(stringResource(R.string.account_open_terms)); state.error?.let { Text(it) } } },
        confirmButton = { TextButton(onClick = { viewModel.open(BankAccountType.valueOf(typeName), newAlias, operationId) }, enabled = !state.busy) { Text(stringResource(R.string.account_confirm)) } },
        dismissButton = { TextButton(onClick = { confirmOpen = false }, enabled = !state.busy) { Text(stringResource(R.string.account_cancel)) } }
    )
    if (confirmClose && account != null) AlertDialog(
        onDismissRequest = { if (!state.busy) confirmClose = false },
        title = { Text(stringResource(R.string.account_close)) },
        text = { Column { Text(account.number); Text(stringResource(R.string.account_close_terms)); state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
        confirmButton = { TextButton(onClick = { viewModel.close(account.id) }, enabled = !state.busy) { Text(stringResource(R.string.account_confirm)) } },
        dismissButton = { TextButton(onClick = { confirmClose = false }, enabled = !state.busy) { Text(stringResource(R.string.account_cancel)) } }
    )
    state.transactions.firstOrNull { it.id == transactionId }?.let { row ->
        AlertDialog(onDismissRequest = { transactionId = null }, title = { Text(stringResource(R.string.account_transaction_detail)) },
            text = { Column { Text(row.description); Text("${row.type} • ${row.status}"); Text(dateFormat.format(Date(row.createdAt)))
                Text(formatRupiah(row.amount)); Text(stringResource(R.string.account_balance_after, formatRupiah(row.balanceAfter))); Text(row.reference) } },
            confirmButton = { TextButton(onClick = { transactionId = null }) { Text(stringResource(R.string.account_done)) } })
    }
}
