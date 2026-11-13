package com.example.qrisapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.qrisapp.databinding.ActivitySecurityBinding

class SecurityActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySecurityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRepository.init(this)
        binding = ActivitySecurityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnChangePin.setOnClickListener {
            val oldPin = binding.etOldPin.text.toString()
            val newPin = binding.etNewPin.text.toString()
            if (AppRepository.changePin(oldPin, newPin)) {
                Toast.makeText(this, "PIN berhasil diubah", Toast.LENGTH_SHORT).show()
                binding.etOldPin.text?.clear()
                binding.etNewPin.text?.clear()
            } else {
                Toast.makeText(this, "PIN lama salah / PIN baru minimal 6 digit", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnChangePassword.setOnClickListener {
            Toast.makeText(this, "Fitur ubah password dapat dihubungkan ke OTP.", Toast.LENGTH_SHORT).show()
        }

        binding.btnBiometric.setOnClickListener {
            AppRepository.security.biometric = !AppRepository.security.biometric
            Toast.makeText(
                this,
                "Biometrik: ${if (AppRepository.security.biometric) "aktif" else "nonaktif"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
