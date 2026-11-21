package com.diponegoro.digitalbank.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.diponegoro.digitalbank.R

/** Plus Jakarta Sans (SIL OFL 1.1, lihat app/licenses). Hanya tiga bobot: 400, 500, 600. */
val JakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold)
)

/** Angka tabular: digit nominal tidak bergeser saat nilainya berubah. */
private const val TABULAR = "tnum"

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0, tabular: Boolean = false) = TextStyle(
    fontFamily = JakartaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
    fontFeatureSettings = if (tabular) TABULAR else null
)

val Typography = Typography(
    displayLarge = style(48, 56, FontWeight.Medium, -1.0, tabular = true),
    displayMedium = style(40, 48, FontWeight.Medium, -0.8, tabular = true),   // saldo
    displaySmall = style(32, 40, FontWeight.Medium, -0.5, tabular = true),
    headlineLarge = style(30, 36, FontWeight.Medium, -0.3),
    headlineMedium = style(28, 34, FontWeight.Medium, -0.3),                   // judul layar autentikasi
    headlineSmall = style(22, 28, FontWeight.Medium),
    titleLarge = style(20, 26, FontWeight.Medium),                             // app bar
    titleMedium = style(17, 24, FontWeight.SemiBold),                          // judul bagian
    titleSmall = style(15, 20, FontWeight.Medium, tabular = true),             // baris daftar, nominal
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(13, 18, FontWeight.Normal),
    labelLarge = style(15, 20, FontWeight.Medium),                             // tombol
    labelMedium = style(13, 18, FontWeight.Medium),
    labelSmall = style(11, 16, FontWeight.Medium, 0.3)
)
