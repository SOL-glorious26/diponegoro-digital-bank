package com.example.qrisapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.qrisapp.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRepository.init(this)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvWelcome.text = "Halo, ${AppRepository.user.name}"

        binding.btnQris.setOnClickListener { startActivity(Intent(this, PaymentActivity::class.java)) }
        binding.btnProfile.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        binding.btnSecurity.setOnClickListener { startActivity(Intent(this, SecurityActivity::class.java)) }
        binding.btnNotification.setOnClickListener { startActivity(Intent(this, NotificationActivity::class.java)) }
        binding.btnLogout.setOnClickListener { finish() }
    }
}
