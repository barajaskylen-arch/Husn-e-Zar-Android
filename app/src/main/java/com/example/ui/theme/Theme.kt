package com.example.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkNavyScheme = darkColorScheme(
    primary = Gold,
    onPrimary = NavyDark,
    primaryContainer = NavyLight,
    onPrimaryContainer = White,
    secondary = GoldLight,
    onSecondary = NavyDark,
    secondaryContainer = NavyMedium,
    onSecondaryContainer = WhiteSoft,
    background = NavyDark,
    onBackground = White,
    surface = NavyMedium,
    onSurface = White,
    surfaceVariant = NavyLight,
    onSurfaceVariant = TextSecondary,
    outline = NavyBorder,
    error = ExpenseRed,
    onError = White,
    errorContainer = AlertRedBg,
    onErrorContainer = ExpenseRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkNavyScheme,
        typography = Typography,
        content = content
    )
}
