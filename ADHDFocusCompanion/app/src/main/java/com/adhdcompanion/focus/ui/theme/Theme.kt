package com.adhdcompanion.focus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = FocusTeal,
    onPrimary = BgDark,
    secondary = FocusIndigo,
    tertiary = FocusAmber,
    background = BgDark,
    onBackground = OnDark,
    surface = SurfaceDark,
    onSurface = OnDark,
    surfaceVariant = SurfaceDarkAlt,
    onSurfaceVariant = OnDarkMuted
)

private val LightColors = lightColorScheme(
    primary = FocusTealDark,
    onPrimary = SurfaceLight,
    secondary = FocusIndigo,
    tertiary = FocusAmber,
    background = BgLight,
    onBackground = OnLight,
    surface = SurfaceLight,
    onSurface = OnLight,
    surfaceVariant = Color(0xFFF1F1EF),
    onSurfaceVariant = OnLightMuted
)

@Composable
fun ADHDFocusCompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = FocusTypography,
        content = content
    )
}
