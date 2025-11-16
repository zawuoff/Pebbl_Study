package com.fouwaz.studypal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueDark,
    onPrimaryContainer = Color(0xFFBBDEFB),

    secondary = BrandBlueDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E3A5F),
    onSecondaryContainer = Color(0xFFBBDEFB),

    tertiary = BrandBlueLight,
    onTertiary = Color.White,

    background = SurfaceDark,
    onBackground = Color(0xFFE0E0E0),

    surface = SurfaceVariantDark,
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = CardSurfaceDark,
    onSurfaceVariant = Color(0xFFBDBDBD),

    error = ErrorRed,
    onError = Color.White,

    outline = Color(0xFF424242),
    outlineVariant = Color(0xFF616161)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = BrandBlueDark,

    secondary = BrandBlueDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBBDEFB),
    onSecondaryContainer = Color(0xFF0D47A1),

    tertiary = BrandBlueLight,
    onTertiary = Color.White,

    background = SurfaceLight,
    onBackground = TextPrimary,

    surface = CardSurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondary,

    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFC62828),

    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
