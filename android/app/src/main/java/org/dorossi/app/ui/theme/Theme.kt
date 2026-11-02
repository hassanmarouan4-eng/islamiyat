package org.dorossi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Generated from the seed colours #0E5A43, #9A7420, #2E5E8C.
// Every on-colour has at least 4.5:1 contrast with its background colour.
private val LightColors = lightColorScheme(
    primary = Color(0xFF0F6148),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8F3EB),
    onPrimaryContainer = Color(0xFF0B372A),
    inversePrimary = Color(0xFF98E1CB),
    secondary = Color(0xFF926E1E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2EAD9),
    onSecondaryContainer = Color(0xFF35290E),
    tertiary = Color(0xFF2E5E8C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCE6EF),
    onTertiaryContainer = Color(0xFF122130),
    background = Color(0xFFFAFCFB),
    onBackground = Color(0xFF191F1D),
    surface = Color(0xFFFAFCFB),
    onSurface = Color(0xFF191F1D),
    surfaceVariant = Color(0xFFDEE7E5),
    onSurfaceVariant = Color(0xFF435650),
    surfaceTint = Color(0xFF0F6148),
    inverseSurface = Color(0xFF2D3432),
    inverseOnSurface = Color(0xFFF0F4F3),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF71847E),
    outlineVariant = Color(0xFFC7D1CE),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFAFCFB),
    surfaceContainer = Color(0xFFEFF3F2),
    surfaceContainerHigh = Color(0xFFEAEEED),
    surfaceContainerHighest = Color(0xFFE4EAE8),
    surfaceContainerLow = Color(0xFFF5F8F7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDBE1DF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF98E1CB),
    onPrimary = Color(0xFF114132),
    primaryContainer = Color(0xFF1C6952),
    onPrimaryContainer = Color(0xFFD8F3EB),
    inversePrimary = Color(0xFF0F6148),
    secondary = Color(0xFFDDC99C),
    onSecondary = Color(0xFF3E3113),
    secondaryContainer = Color(0xFF654F20),
    onSecondaryContainer = Color(0xFFF2EAD9),
    tertiary = Color(0xFFA4BDD6),
    onTertiary = Color(0xFF182939),
    tertiaryContainer = Color(0xFF27435D),
    onTertiaryContainer = Color(0xFFDCE6EF),
    background = Color(0xFF101413),
    onBackground = Color(0xFFE2E9E7),
    surface = Color(0xFF101413),
    onSurface = Color(0xFFE2E9E7),
    surfaceVariant = Color(0xFF3B4E49),
    onSurfaceVariant = Color(0xFFC6D2CE),
    surfaceTint = Color(0xFF98E1CB),
    inverseSurface = Color(0xFFE2E9E7),
    inverseOnSurface = Color(0xFF2C3532),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8B9C97),
    outlineVariant = Color(0xFF3E4C48),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF364540),
    surfaceContainer = Color(0xFF1B2220),
    surfaceContainerHigh = Color(0xFF222B28),
    surfaceContainerHighest = Color(0xFF2B3633),
    surfaceContainerLow = Color(0xFF161D1B),
    surfaceContainerLowest = Color(0xFF0A0D0C),
    surfaceDim = Color(0xFF101413),
)

@Composable
fun AppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
