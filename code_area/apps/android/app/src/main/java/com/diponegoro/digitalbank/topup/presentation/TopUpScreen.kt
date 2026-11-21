package com.diponegoro.digitalbank.topup.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import com.diponegoro.digitalbank.core.ui.DdbButton as Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import com.diponegoro.digitalbank.core.ui.DdbOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.core.ledger.TxStatus
import com.diponegoro.digitalbank.core.ui.ErrorText
import com.diponegoro.digitalbank.core.ui.InfoRow
import com.diponegoro.digitalbank.core.ui.MoneyField
import com.diponegoro.digitalbank.core.ui.PinDialog
import com.diponegoro.digitalbank.core.ui.ReceiptCard
import com.diponegoro.digitalbank.core.ui.ScreenScaffold
import com.diponegoro.digitalbank.core.ui.SectionCard
import com.diponegoro.digitalbank.core.util.formatRupiah
import com.diponegoro.digitalbank.topup.domain.Biller
import com.diponegoro.digitalbank.topup.domain.EWallet
import com.diponegoro.digitalbank.topup.domain.TopUpCatalog
import com.diponegoro.digitalbank.topup.domain.TopUpKind

@Composable
fun TopUpScreen(state: TopUpUiState, vm: TopUpViewModel, onBack: () -> Unit) {
    ScreenScaffold(title = "Top-up & Tagihan", onBack = onBack) {
        val result = state.result
        val order = state.order
        when {
            result != null -> {
                ReceiptCard(result, if (result.status == TxStatus.SUCCESS) "Transaksi berhasil" else "Transaksi gagal")
                result.extra["Token"]?.let { token ->
                    SectionCard(title = "Token listrik") {
                        Text(token, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)
                        Text("Masukkan token ini ke meteran listrik Anda.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedButton(onClick = vm::restart, modifier = Modifier.fillMaxWidth()) { Text("Transaksi lain") }
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Kembali ke beranda") }
            }

            order != null -> {
                SectionCard(title = "Konfirmasi ${order.kind.label}") {
                    order.details.forEach { (k, v) -> InfoRow(k, v) }
                    InfoRow("Harga", formatRupiah(order.amount))
                    InfoRow("Biaya admin", formatRupiah(order.fee))
                    InfoRow("Total", formatRupiah(order.total), emphasize = true)
                }
                ErrorText(state.error.takeIf { !state.showPin })
                Button(onClick = vm::requestPin, modifier = Modifier.fillMaxWidth()) { Text("Bayar") }
                OutlinedButton(onClick = vm::cancelOrder, modifier = Modifier.fillMaxWidth()) { Text("Ubah") }
            }

            else -> {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TopUpKind.entries.forEach { kind ->
                        FilterChip(selected = state.tab == kind, onClick = { vm.selectTab(kind) }, label = { Text(kind.label) })
                    }
                }
                when (state.tab) {
                    TopUpKind.PULSA -> PulsaForm(state, vm)
                    TopUpKind.DATA -> DataForm(state, vm)
                    TopUpKind.PLN -> PlnForm(state, vm)
                    TopUpKind.BILL -> BillForm(state, vm)
                    TopUpKind.EWALLET -> EwalletForm(state, vm)
                }
            }
        }
    }
    if (state.showPin) {
        PinDialog(
            subtitle = state.order?.let { "${it.title} — total ${formatRupiah(it.total)}" },
            errorMessage = state.error,
            isLoading = state.busy,
            onSubmit = vm::pay,
            onDismiss = vm::dismissPin
        )
    }
}

@Composable
private fun PhoneField(phone: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = phone,
        onValueChange = { onChange(it.filter(Char::isDigit).take(13)) },
        label = { Text("Nomor HP") },
        singleLine = true,
        supportingText = { Text(TopUpCatalog.detectOperator(phone)?.name ?: "Operator terdeteksi otomatis") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PulsaForm(state: TopUpUiState, vm: TopUpViewModel) {
    var phone by rememberSaveable { mutableStateOf("") }
    SectionCard(title = "Beli pulsa") {
        PhoneField(phone) { phone = it; vm.clearError() }
        ErrorText(state.error)
        TopUpCatalog.pulsaNominals.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { nominal ->
                    NominalButton(formatRupiah(nominal).removePrefix("Rp"), Modifier.weight(1f)) { vm.orderPulsa(phone, nominal) }
                }
                repeat(3 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
            }
        }
        Text("Biaya admin ${formatRupiah(TopUpCatalog.PULSA_FEE)}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DataForm(state: TopUpUiState, vm: TopUpViewModel) {
    var phone by rememberSaveable { mutableStateOf("") }
    val operator = TopUpCatalog.detectOperator(phone)
    SectionCard(title = "Beli paket data") {
        PhoneField(phone) { phone = it; vm.clearError() }
        ErrorText(state.error)
        if (operator == null) Text("Masukkan nomor HP untuk melihat paket.", style = MaterialTheme.typography.bodySmall)
        else TopUpCatalog.dataPackages(operator).forEach { product ->
            Row(
                Modifier.fillMaxWidth().clickable { vm.orderData(phone, product) }.padding(vertical = 8.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.bodyLarge)
                    Text(product.description, style = MaterialTheme.typography.bodySmall)
                }
                Text(formatRupiah(product.price), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun PlnForm(state: TopUpUiState, vm: TopUpViewModel) {
    var meter by rememberSaveable { mutableStateOf("") }
    SectionCard(title = "Token listrik PLN") {
        OutlinedTextField(
            value = meter,
            onValueChange = { meter = it.filter(Char::isDigit).take(12); vm.clearError() },
            label = { Text("ID pelanggan / nomor meter") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        ErrorText(state.error)
        Button(onClick = { vm.inquirePln(meter) }, modifier = Modifier.fillMaxWidth()) { Text("Cek ID pelanggan") }
        state.plnCustomer?.let { c ->
            InfoRow("Nama", c.name)
            InfoRow("Tarif/Daya", c.tariff)
            Text("Pilih nominal (admin ${formatRupiah(TopUpCatalog.PLN_ADMIN_FEE)})", style = MaterialTheme.typography.labelLarge)
            TopUpCatalog.plnNominals.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { n ->
                        NominalButton(formatRupiah(n), Modifier.weight(1f)) { vm.orderPln(n) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BillForm(state: TopUpUiState, vm: TopUpViewModel) {
    var biller by rememberSaveable { mutableStateOf(Biller.PDAM) }
    var number by rememberSaveable { mutableStateOf("") }
    SectionCard(title = "Bayar tagihan") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Biller.entries.forEach { b ->
                FilterChip(selected = biller == b, onClick = { biller = b; vm.clearError() }, label = { Text(b.label) })
            }
        }
        OutlinedTextField(
            value = number,
            onValueChange = { number = it.filter(Char::isDigit).take(16); vm.clearError() },
            label = { Text(biller.numberHint) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        ErrorText(state.error)
        Button(onClick = { vm.inquireBill(biller, number) }, modifier = Modifier.fillMaxWidth()) { Text("Cek tagihan") }
        state.bill?.let { b ->
            InfoRow("Nama", b.customerName)
            InfoRow("Periode", b.period)
            InfoRow("Tagihan", formatRupiah(b.amount), emphasize = true)
            Button(onClick = vm::orderBill, modifier = Modifier.fillMaxWidth()) { Text("Lanjut bayar") }
        }
    }
}

@Composable
private fun EwalletForm(state: TopUpUiState, vm: TopUpViewModel) {
    var wallet by rememberSaveable { mutableStateOf(EWallet.GOPAY) }
    var phone by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    SectionCard(title = "Top-up e-wallet") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EWallet.entries.forEach { w ->
                FilterChip(selected = wallet == w, onClick = { wallet = w }, label = { Text(w.label) })
            }
        }
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter(Char::isDigit).take(13); vm.clearError() },
            label = { Text("Nomor HP terdaftar") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        MoneyField(amount, { amount = it; vm.clearError() }, "Nominal",
            supportingText = "${formatRupiah(TopUpCatalog.ewalletRange.first)} – ${formatRupiah(TopUpCatalog.ewalletRange.last)}")
        ErrorText(state.error)
        Button(onClick = { vm.orderEwallet(wallet, phone, amount) }, modifier = Modifier.fillMaxWidth()) { Text("Lanjut") }
    }
}

/**
 * Tombol nominal dalam grid 3 kolom. Padding horizontal bawaan tombol (24dp) terlalu lebar untuk
 * layar sempit sehingga angka terpotong per karakter; di sini padding diperkecil dan teks satu baris.
 */
@Composable
private fun NominalButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Text(label, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
    }
}
