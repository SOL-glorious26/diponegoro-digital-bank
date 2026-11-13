package com.example.qrisapp

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.qrisapp.databinding.ActivityPaymentBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.util.Locale

class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private val gateway = PaymentGateway()

    private val scanner = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val parts = result.contents.split("|")
            binding.tvMerchant.text = "Merchant: ${parts.getOrNull(0) ?: "Demo Merchant"}"
            binding.tvNominal.text = "Nominal: Rp${parts.getOrNull(1) ?: "0"}"
            Toast.makeText(this, "QR berhasil dibaca", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRepository.init(this)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnScan.setOnClickListener {
            val options = ScanOptions().apply {
                setPrompt("Arahkan kamera ke QRIS")
                setBeepEnabled(true)
                setOrientationLocked(false)
            }
            scanner.launch(options)
        }

        binding.btnPay.setOnClickListener {
            val amount = binding.etNominal.text.toString().toDoubleOrNull() ?: 0.0
            val pin = binding.etPin.text.toString()

            if (amount <= 0) {
                Toast.makeText(this, "Masukkan nominal pembayaran", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!AppRepository.verifyPin(pin)) {
                Toast.makeText(this, "PIN salah", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!gateway.prosesTransaksi(amount)) {
                Toast.makeText(this, "Gateway gagal memproses transaksi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val trx = AppRepository.processPayment(amount)
            Toast.makeText(this, "Pembayaran ${trx.status}", Toast.LENGTH_LONG).show()
            binding.etPin.text?.clear()
        }

        binding.btnHistory.setOnClickListener {
            val list = AppRepository.getTransactions()
            val message = if (list.isEmpty()) "Belum ada transaksi."
            else list.joinToString("\n") {
                "${it.merchant.name} - Rp${"%,.0f".format(Locale.US, it.amount)} - ${it.status}"
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }

        binding.btnGenerate.setOnClickListener {
            val data = "USER:${AppRepository.user.userId}|0"
            binding.imgQr.setImageBitmap(generateQr(data))
            binding.imgQr.visibility = android.view.View.VISIBLE
        }
    }

    private fun generateQr(text: String): Bitmap {
        val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 600, 600)
        val bitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.RGB_565)
        for (x in 0 until 600) for (y in 0 until 600) {
            bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
        return bitmap
    }
}
