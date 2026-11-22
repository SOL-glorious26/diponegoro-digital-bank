package com.diponegoro.digitalbank.ui.theme

import androidx.compose.ui.graphics.Color

// Diponegoro Digital Bank — quiet institutional.
// Midnight Navy = warna tinta (teks & tombol utama di mode terang, permukaan di mode gelap).
// Muted Gold dipakai sangat terbatas sebagai aksen garis/titik, tidak untuk teks di mode terang (kontras ±2,6:1).
val MidnightNavy = Color(0xFF0B1D33)
val DiponegoroBlue = Color(0xFF0B3D91)
val MutedGold = Color(0xFFB69A62)

// Mode terang
val LightBackground = Color(0xFFF5F5F2)
val LightPanel = Color(0xFFFFFFFF)
val LightInk = MidnightNavy
val LightInkSecondary = Color(0xFF5B6573)
val LightLine = Color(0xFFE4E2DB)
val LightLineStrong = Color(0xFFC9C5BA)

// Mode gelap
val DarkBackground = Color(0xFF070F1B)
val DarkPanel = MidnightNavy
val DarkInk = Color(0xFFF2F1EC)
val DarkInkSecondary = Color(0xFFA9B1BC)
val DarkLine = Color(0xFF1C2B40)
val DarkLineStrong = Color(0xFF2A3B54)
val DarkBlue = Color(0xFF8FB0E8)
val DarkGold = Color(0xFFC9B07A)

// Warna grafik (palet kategorikal tervalidasi CVD, slot 1 & 2), versi terang & gelap.
val ChartBlueLight = Color(0xFF2A78D6)
val ChartBlueDark = Color(0xFF3987E5)
val ChartOrangeLight = Color(0xFFEB6834)
val ChartOrangeDark = Color(0xFFD95926)

// Status (selalu disertai ikon + label, bukan warna saja). Nada diredam agar tidak mencolok.
val StatusSuccess = Color(0xFF2F6B4F)
val StatusWarning = Color(0xFF7A5C16)
val StatusError = Color(0xFF9E2A2B)
val StatusSuccessDark = Color(0xFF7FC4A0)
val StatusWarningDark = Color(0xFFD9B45F)
val StatusErrorDark = Color(0xFFE59A97)
