package com.diponegoro.digitalbank.payment.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode
import com.diponegoro.digitalbank.core.ui.DdbButton as Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.diponegoro.digitalbank.core.ui.DdbOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.core.ledger.TxStatus
import com.diponegoro.digitalbank.core.ui.ErrorText
import com.diponegoro.digitalbank.core.ui.InfoRow
import com.diponegoro.digitalbank.core.ui.InfoText
import com.diponegoro.digitalbank.core.ui.MoneyField
import com.diponegoro.digitalbank.core.ui.PinDialog
import com.diponegoro.digitalbank.core.ui.ReceiptCard
import com.diponegoro.digitalbank.core.ui.ScreenScaffold
import com.diponegoro.digitalbank.core.ui.SectionCard
import com.diponegoro.digitalbank.core.util.formatRupiah
import com.diponegoro.digitalbank.core.util.parseRupiah
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun QrisPayScreen(
    state: QrisPayUiState,
    vm: QrisPayViewModel,
    onReceive: () -> Unit,
    onBack: (() -> Unit)?,
    onDone: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val context = LocalContext.current
    val camera = rememberLauncherForActivityResult(ScanContract()) { result -> result.contents?.let(vm::onScanned) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.onScanned(QrImages.decode(context, uri))
    }
    var manual by rememberSaveable { mutableStateOf("") }

    ScreenScaffold(title = "Bayar QRIS", onBack = onBack, bottomBar = bottomBar) {
        val result = state.result
        val scanned = state.scanned
        when {
            result != null -> {
                ReceiptCard(result, if (result.status == TxStatus.SUCCESS) "Pembayaran berhasil" else "Pembayaran gagal")
                OutlinedButton(onClick = vm::reset, modifier = Modifier.fillMaxWidth()) { Text("Bayar lagi") }
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Kembali ke beranda") }
            }

            scanned != null -> {
                val p = scanned.payload
                SectionCard(title = "Detail pembayaran") {
                    InfoRow("Merchant", p.merchantName)
                    InfoRow("Kota", p.city)
                    InfoRow("Jenis", if (p.isDynamic) "QRIS dinamis (nominal ditetapkan)" else "QRIS statis")
                    InfoRow("Penerbit", if (scanned.internalAccountId != null) "Diponegoro Digital Bank" else "Bank / PJSP lain")
                    InfoRow("Saldo Anda", formatRupiah(state.balance))
                }
                MoneyField(
                    value = state.amount,
                    onValueChange = vm::setAmount,
                    label = "Nominal",
                    enabled = !p.isDynamic,
                    supportingText = if (p.isDynamic) "Nominal ditetapkan penerima dan tidak dapat diubah" else null
                )
                ErrorText(state.error.takeIf { !state.showPin })
                Button(onClick = vm::requestPin, modifier = Modifier.fillMaxWidth()) { Text("Bayar") }
                OutlinedButton(onClick = vm::reset, modifier = Modifier.fillMaxWidth()) { Text("Batal") }
            }

            else -> {
                SectionCard(title = "Pindai kode QRIS") {
                    Button(
                        onClick = {
                            camera.launch(
                                ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    .setPrompt("Arahkan kamera ke kode QRIS").setBeepEnabled(false).setOrientationLocked(false)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Filled.CameraAlt, null); Text("  Pindai dengan kamera") }
                    OutlinedButton(
                        onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Filled.PhotoLibrary, null); Text("  Pilih gambar QR dari galeri") }
                    ErrorText(state.error)
                }
                SectionCard(title = "QR contoh (untuk uji di emulator)") {
                    state.demoQrs.forEach { qr ->
                        TextButton(onClick = { vm.onScanned(qr.raw) }) { Text(qr.label) }
                    }
                    OutlinedTextField(
                        value = manual, onValueChange = { manual = it.trim() },
                        label = { Text("Atau tempel payload QRIS") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = { vm.onScanned(manual) }, enabled = manual.isNotBlank()) { Text("Proses payload") }
                }
                OutlinedButton(onClick = onReceive, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.QrCode, null); Text("  Terima dana dengan QRIS saya")
                }
            }
        }
    }

    if (state.showPin) {
        PinDialog(
            subtitle = state.scanned?.let {
               "Bayar ${formatRupiah(it.payload.amount ?: parseRupiah(state.amount) ?: 0)} ke ${it.payload.merchantName}"
            },
            errorMessage = state.error,
            isLoading = state.busy,
            onSubmit = vm::pay,
            onDismiss = vm::dismissPin
        )
    }
}

@Composable
private fun QrImage(payload: String, description: String) {
    val image = remember(payload) { QrImages.encode(payload) }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Image(
            bitmap = image,
            contentDescription = description,
            modifier = Modifier.size(240.dp).background(Color.White).padding(8.dp)
        )
    }
}

@Composable
fun QrisReceiveScreen(state: QrisReceiveUiState, vm: QrisReceiveViewModel, onBack: () -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }
    var simAmount by rememberSaveable { mutableStateOf("") }
    var payer by rememberSaveable { mutableStateOf("") }
    ScreenScaffold(title = "Terima dengan QRIS", onBack = onBack) {
        val dynamic = state.dynamicQr
        SectionCard(title = if (dynamic != null) "QRIS dinamis ${formatRupiah(state.dynamicAmount ?: 0)}" else "QRIS pribadi (statis)") {
            val payload = dynamic ?: state.staticQr
            if (payload != null) QrImage(payload, "Kode QRIS untuk menerima dana")
            Text(
                if (dynamic != null) "Pembayar tidak dapat mengubah nominal pada kode ini."
                else "Dapat dipindai pengguna Diponegoro Digital Bank maupun aplikasi bank/e-wallet lain.",
                style = MaterialTheme.typography.bodySmall
            )
            if (dynamic != null) TextButton(onClick = vm::clearDynamic) { Text("Kembali ke QRIS statis") }
        }
        SectionCard(title = "Buat QRIS dengan nominal") {
            MoneyField(amount, { amount = it }, "Nominal yang diminta")
            OutlinedButton(onClick = { vm.generateDynamic(amount) }, modifier = Modifier.fillMaxWidth()) { Text("Buat QRIS dinamis") }
        }
        SectionCard(title = "Simulasi dana masuk") {
            Text(
                "Simulator: pengguna bank lain membayar ke QRIS Anda.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(payer, { payer = it.take(30) }, label = { Text("Nama pembayar") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            MoneyField(simAmount, { simAmount = it }, "Nominal")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.simulateIncoming(state.dynamicAmount?.toString() ?: simAmount, payer) }, enabled = !state.busy) {
                    Text(if (state.dynamicAmount != null) "Bayar QR dinamis" else "Kirim dana")
                }
            }
        }
        ErrorText(state.error)
        InfoText(state.info)
    }
}
