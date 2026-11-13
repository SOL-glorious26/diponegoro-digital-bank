package com.example.qrisapp

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.qrisapp.databinding.ActivityNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRepository.init(this)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val data = AppRepository.getNotifications().map {
            "${formatter.format(Date(it.tanggal))}\n${it.pesan}"
        }

        binding.listNotification.adapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, data)
    }
}
