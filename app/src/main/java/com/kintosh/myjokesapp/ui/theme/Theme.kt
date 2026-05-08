package com.kintosh.myjokesapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1B1A55),
    onBackground = Color.White,
    surface = Color(0xFF2D3250),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF424769),
    onSurfaceVariant = Color(0xFFCCC2DC),
    primaryContainer = Color(0xFF424769),
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFF7077A1),
    onSecondaryContainer = Color.White,
    outline = Color(0xFF7077A1),
    outlineVariant = Color(0xFF424769)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF424769),
    onPrimary = Color.White,
    secondary = Color(0xFF7077A1),
    onSecondary = Color.White,
    tertiary = Color(0xFFF6B17A),
    onTertiary = Color(0xFF3E2723),
    background = Color(0xFFF0F2F5),
    onBackground = Color(0xFF2D3250),
    surface = Color.White,
    onSurface = Color(0xFF2D3250),
    surfaceVariant = Color(0xFFE1E2E5),
    onSurfaceVariant = Color(0xFF424769),
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF001453),
    secondaryContainer = Color(0xFFE1E2E5),
    onSecondaryContainer = Color(0xFF1B1B1F),
    outline = Color(0xFF7077A1),
    outlineVariant = Color(0xFFC7C5D0)
)

@Composable
fun MyjokesappTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
