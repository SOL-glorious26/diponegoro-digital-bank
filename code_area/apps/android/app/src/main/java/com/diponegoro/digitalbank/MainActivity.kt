package com.diponegoro.digitalbank

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.diponegoro.digitalbank.authentication.presentation.navigation.AuthNavGraph
import com.diponegoro.digitalbank.core.di.CoreModule
import com.diponegoro.digitalbank.core.security.InactivityMonitor
import com.diponegoro.digitalbank.ui.theme.DigitalBankTheme

class MainActivity : ComponentActivity() {

    private val localNetworkPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* hasil ditangani lewat pesan offline */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocalNetworkIfNeeded()
        setContent {
            DigitalBankTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthNavGraph()
                }
            }
        }
    }

    /**
     * Varian online + Android 17 (API 37): akses ke backend di jaringan lokal (laptop/LAN, emulator 10.0.2.2)
     * memerlukan izin ACCESS_LOCAL_NETWORK. Tanpa izin, koneksi diblokir sistem dan aplikasi menampilkan
     * pesan "Tidak dapat terhubung ke server".
     */
    private fun requestLocalNetworkIfNeeded() {
        if (!CoreModule.online || Build.VERSION.SDK_INT < 37) return
        if (ContextCompat.checkSelfPermission(this, LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED) return
        localNetworkPermission.launch(LOCAL_NETWORK)
    }

    /** Setiap sentuhan/ketikan memperpanjang sesi (FR-05: timeout saat tidak aktif). */
    override fun onUserInteraction() {
        super.onUserInteraction()
        InactivityMonitor.touch()
    }

    private companion object {
        const val LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    }
}
