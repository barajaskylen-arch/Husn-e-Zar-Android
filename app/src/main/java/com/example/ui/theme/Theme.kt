package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val LightColorScheme = lightColorScheme(
    primary = DarkBlue,
    onPrimary = Color.White,
    primaryContainer = Yellow,
    onPrimaryContainer = DarkBlue,
    secondary = Yellow,
    onSecondary = DarkBlue,
    secondaryContainer = YellowDeep,
    onSecondaryContainer = DarkBlue,
    background = Color.White,
    onBackground = DarkBlue,
    surface = Color.White,
    onSurface = DarkBlue,
    surfaceVariant = Color(0xFFF4F6F9),
    onSurfaceVariant = Color(0xFF5A6E85),
    outline = DarkBlue,
    error = AlertRed,
    onError = Color.White,
    errorContainer = AlertRedBg,
    onErrorContainer = AlertRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
