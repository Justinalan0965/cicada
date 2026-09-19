package app.cicada.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CicadaLightColorScheme = lightColorScheme(
    primary = CicadaGreen,
    onPrimary = Color.White,

    primaryContainer = CicadaGreenLight,
    onPrimaryContainer = CicadaGreenDark,

    secondary = CicadaGreenDark,
    onSecondary = Color.White,

    background = CicadaBackground,
    onBackground = CicadaText,

    surface = CicadaSurface,
    onSurface = CicadaText,

    surfaceVariant = CicadaGreenLight,
    onSurfaceVariant = CicadaTextSecondary,

    error = CicadaError,
    onError = Color.White
)

private val CicadaDarkColorScheme = darkColorScheme(
    primary = Color(0xFF66BB6A),
    onPrimary = Color(0xFF0D1B0F),

    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFB9F6BC),

    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF102012),

    background = Color(0xFF101410),
    onBackground = Color(0xFFE8F0E8),

    surface = Color(0xFF181D18),
    onSurface = Color(0xFFE8F0E8),

    surfaceVariant = Color(0xFF293029),
    onSurfaceVariant = Color(0xFFC2CCC2),

    error = CicadaError,
    onError = Color.White
)

@Composable
fun CicadaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        CicadaDarkColorScheme
    } else {
        CicadaLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}