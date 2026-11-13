package com.diponegoro.digitalbank.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = NavyContainer,
    onPrimaryContainer = Color(0xFF001A43),
    secondary = Gold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7B0),
    onSecondaryContainer = Color(0xFF3B2A00),
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE6E9F2),
    error = StatusError
)

private val DarkColors = darkColorScheme(
    primary = NavyLight,
    onPrimary = Color(0xFF00225C),
    primaryContainer = NavyContainerDark,
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = GoldLight,
    onSecondary = Color(0xFF3B2A00),
    background = Color(0xFF111318),
    surface = Color(0xFF181B22),
    surfaceVariant = Color(0xFF2A2E38)
)

/** Warna grafik yang dipilih per mode (bukan hasil inversi otomatis). */
@Immutable
data class ChartColors(val income: Color, val expense: Color, val balance: Color)

val LocalChartColors = staticCompositionLocalOf {
    ChartColors(ChartBlueLight, ChartOrangeLight, ChartBlueLight)
}

/** Tema brand tetap (tanpa dynamic color) agar tampilan konsisten dengan aplikasi web (NFR-08). */
@Composable
fun DigitalBankTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val chartColors = if (darkTheme) ChartColors(ChartBlueDark, ChartOrangeDark, ChartBlueDark)
    else ChartColors(ChartBlueLight, ChartOrangeLight, ChartBlueLight)
    CompositionLocalProvider(LocalChartColors provides chartColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography,
            content = content
        )
    }
}
