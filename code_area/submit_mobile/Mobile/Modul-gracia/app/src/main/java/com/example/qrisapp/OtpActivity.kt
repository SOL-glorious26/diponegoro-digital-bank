package com.example.qrisapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.qrisapp.databinding.ActivityOtpBinding

class OtpActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOtpBinding
    private val demoOtp = "123456"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRepository.init(this)
        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvOtpInfo.text = "OTP demo: $demoOtp"

        binding.btnVerify.setOnClickListener {
            if (binding.etOtp.text.toString() != demoOtp) {
                Toast.makeText(this, "OTP tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (intent.getStringExtra("purpose") == "PROFILE") {
                AppRepository.saveProfile(
                    intent.getStringExtra("name").orEmpty(),
                    intent.getStringExtra("email").orEmpty(),
                    intent.getStringExtra("phone").orEmpty()
                )
                Toast.makeText(this, "Profil berhasil diubah", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}
