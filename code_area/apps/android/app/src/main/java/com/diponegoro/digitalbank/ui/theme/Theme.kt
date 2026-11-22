package com.diponegoro.digitalbank.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = LightInk,
    onPrimary = LightBackground,
    primaryContainer = Color(0xFFE7EAEF),
    onPrimaryContainer = LightInk,
    secondary = DiponegoroBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EAF6),
    onSecondaryContainer = LightInk,
    tertiary = MutedGold,
    onTertiary = LightInk,
    background = LightBackground,
    onBackground = LightInk,
    surface = LightBackground,
    onSurface = LightInk,
    surfaceVariant = Color(0xFFECEAE4),
    onSurfaceVariant = LightInkSecondary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = LightPanel,
    surfaceContainerLow = Color(0xFFFAFAF8),
    surfaceContainer = Color(0xFFF0EFEA),
    surfaceContainerHigh = LightPanel,          // dialog
    surfaceContainerHighest = Color(0xFFECEAE4),
    outline = LightLineStrong,
    outlineVariant = LightLine,
    error = StatusError,
    onError = Color.White,
    errorContainer = Color(0xFFF6E4E3),
    onErrorContainer = Color(0xFF5C1414)
)

private val DarkColors = darkColorScheme(
    primary = DarkInk,
    onPrimary = MidnightNavy,
    primaryContainer = Color(0xFF16273F),
    onPrimaryContainer = DarkInk,
    secondary = DarkBlue,
    onSecondary = Color(0xFF07152E),
    secondaryContainer = Color(0xFF15284A),
    onSecondaryContainer = DarkInk,
    tertiary = DarkGold,
    onTertiary = MidnightNavy,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkBackground,
    onSurface = DarkInk,
    surfaceVariant = Color(0xFF132236),
    onSurfaceVariant = DarkInkSecondary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color(0xFF050B14),
    surfaceContainerLow = Color(0xFF0A1524),
    surfaceContainer = DarkPanel,
    surfaceContainerHigh = Color(0xFF0F2138),   // dialog
    surfaceContainerHighest = Color(0xFF13263F),
    outline = DarkLineStrong,
    outlineVariant = DarkLine,
    error = StatusErrorDark,
    onError = Color(0xFF3B0A0A),
    errorContainer = Color(0xFF3A1618),
    onErrorContainer = Color(0xFFF6D6D5)
)

/** Warna merek di luar skema Material: aksen emas, dana masuk, panel, garis, logo, status. */
@Immutable
data class DdbColors(
    val gold: Color,
    val incoming: Color,
    val link: Color,
    val panel: Color,
    val line: Color,
    val lineStrong: Color,
    val mark: Color,
    val pinFilled: Color,
    val pinEmpty: Color,
    val success: Color,
    val warning: Color
)

private val LightDdb = DdbColors(
    gold = MutedGold, incoming = DiponegoroBlue, link = DiponegoroBlue, panel = LightPanel,
    line = LightLine, lineStrong = LightLineStrong, mark = MidnightNavy,
    pinFilled = MidnightNavy, pinEmpty = Color(0xFF8A929C),
    success = StatusSuccess, warning = StatusWarning
)
private val DarkDdb = DdbColors(
    gold = DarkGold, incoming = DarkBlue, link = DarkBlue, panel = DarkPanel,
    line = DarkLine, lineStrong = DarkLineStrong, mark = Color(0xFFD6BE8C),
    pinFilled = DarkGold, pinEmpty = Color(0xFF4A5B73),
    success = StatusSuccessDark, warning = StatusWarningDark
)

val LocalDdbColors = staticCompositionLocalOf { LightDdb }

object DdbTheme {
    val colors: DdbColors
        @Composable get() = LocalDdbColors.current
}

private val DdbShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),     // tombol, input
    medium = RoundedCornerShape(14.dp),    // panel
    large = RoundedCornerShape(20.dp),     // bottom sheet
    extraLarge = RoundedCornerShape(28.dp) // dialog
)

/** Warna grafik yang dipilih per mode (bukan hasil inversi otomatis). */
@Immutable
data class ChartColors(val income: Color, val expense: Color, val balance: Color)

val LocalChartColors = staticCompositionLocalOf {
    ChartColors(ChartBlueLight, ChartOrangeLight, ChartBlueLight)
}

/** Tema merek tetap (tanpa dynamic color). Mengikuti mode terang/gelap sistem. */
@Composable
fun DigitalBankTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val chartColors = if (darkTheme) ChartColors(ChartBlueDark, ChartOrangeDark, ChartBlueDark)
    else ChartColors(ChartBlueLight, ChartOrangeLight, ChartBlueLight)
    CompositionLocalProvider(
        LocalChartColors provides chartColors,
        LocalDdbColors provides if (darkTheme) DarkDdb else LightDdb
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography,
            shapes = DdbShapes,
            content = content
        )
    }
}
