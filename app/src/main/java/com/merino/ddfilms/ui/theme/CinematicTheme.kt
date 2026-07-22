package com.merino.ddfilms.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CinematicDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE50914),       // Crimson Noir Accent
    secondary = Color(0xFFFF6575),     // Glowing Coral Rose
    background = Color(0xFF0F0E13),    // Obsidian Deep Dark Base
    surface = Color(0xFF191822),       // Glass Charcoal Surface
    surfaceVariant = Color(0xFF242230),// Elevated Surface
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFE6E1E5),  // Light text
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFC4C2CF)
)

val CinematicLightColorScheme = lightColorScheme(
    primary = Color(0xFFD32F2F),       // Deep Velvet Red
    secondary = Color(0xFF99000D),     // Dark Crimson Accent
    background = Color(0xFFF5F6FA),    // Premium off-white
    surface = Color(0xFFFFFFFF),       // Pure white cards
    surfaceVariant = Color(0xFFECEEF4),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111215),  // Dark text
    onSurface = Color(0xFF111215),
    onSurfaceVariant = Color(0xFF5A5D66)
)

@Composable
fun CinematicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        CinematicDarkColorScheme
    } else {
        CinematicLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
