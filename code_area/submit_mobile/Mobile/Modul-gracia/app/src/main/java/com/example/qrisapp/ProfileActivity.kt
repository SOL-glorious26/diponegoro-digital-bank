package com.example.qrisapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.qrisapp.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRepository.init(this)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etName.setText(AppRepository.user.name)
        binding.etEmail.setText(AppRepository.user.email)
        binding.etPhone.setText(AppRepository.user.noHp)

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()

            if (name.isBlank() || email.isBlank() || phone.isBlank()) {
                Toast.makeText(this, "Semua data wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sesuai diagram: perubahan profil dapat dilanjutkan ke verifikasi OTP.
            val intent = Intent(this, OtpActivity::class.java)
            intent.putExtra("purpose", "PROFILE")
            intent.putExtra("name", name)
            intent.putExtra("email", email)
            intent.putExtra("phone", phone)
            startActivity(intent)
        }
    }
}
