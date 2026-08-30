package de.shakie.billcheck.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF0A2B66),
    secondary = Color(0xFF475569),
    tertiary = Color(0xFFC2410C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8D5),
    onTertiaryContainer = Color(0xFF7C2D12),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF0F172A),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DB2FF),
    onPrimary = Color(0xFF002F6C),
    primaryContainer = Color(0xFF164A9C),
    onPrimaryContainer = Color(0xFFDCE7FF),
    secondary = Color(0xFFB8C5D6),
    tertiary = Color(0xFFFFB077),
    onTertiary = Color(0xFF5C2600),
    tertiaryContainer = Color(0xFF7C2D12),
    onTertiaryContainer = Color(0xFFFFDCC2),
    surface = Color(0xFF172033),
    surfaceVariant = Color(0xFF202B3D),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFF8FAFC),
    outline = Color(0xFF475569),
    error = Color(0xFFFFB4AB),
)

val MatchGreen = Color(0xFF059669)
val WarningYellow = Color(0xFFFACC15)
val DifferenceOrange = Color(0xFFEA580C)
val MissingRed = Color(0xFFDC2626)

@Composable
fun BillCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}
