package rs.coffeeconquest.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Espresso browns with a warm crema accent - the app should look like the thing it is about.
private val Espresso = Color(0xFF4E342E)
private val EspressoDark = Color(0xFF2E1B16)
private val Crema = Color(0xFFD7A86E)
private val CremaDark = Color(0xFF8C6239)
private val Mint = Color(0xFF2E7D5B)
private val Cream = Color(0xFFFFFBF7)

private val LightColors = lightColorScheme(
    primary = Espresso,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E3D3),
    onPrimaryContainer = EspressoDark,
    secondary = CremaDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7E9D8),
    onSecondaryContainer = Color(0xFF3E2A16),
    tertiary = Mint,
    background = Cream,
    onBackground = Color(0xFF23180F),
    surface = Color.White,
    onSurface = Color(0xFF23180F),
    surfaceVariant = Color(0xFFF1E7DF),
    onSurfaceVariant = Color(0xFF57453B),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Crema,
    onPrimary = Color(0xFF3A2318),
    primaryContainer = Color(0xFF54372A),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFE2BE96),
    onSecondary = Color(0xFF412D14),
    tertiary = Color(0xFF7FD1A8),
    background = Color(0xFF19110D),
    onBackground = Color(0xFFF1E2D8),
    surface = Color(0xFF231913),
    onSurface = Color(0xFFF1E2D8),
    surfaceVariant = Color(0xFF453229),
    onSurfaceVariant = Color(0xFFD6C2B4),
    error = Color(0xFFF2B8B5),
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun CoffeeConquestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
