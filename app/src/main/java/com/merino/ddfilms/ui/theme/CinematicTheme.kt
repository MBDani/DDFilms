package com.merino.ddfilms.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CinematicDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4AB),       // Soft cinematic red (Primary accent)
    secondary = Color(0xFFE2C4C0),     // Subtle bronze/champagne
    background = Color(0xFF090A0F),    // Ultra-deep cinematic black
    surface = Color(0xFF12131C),       // Dark charcoal for cards/panels
    onPrimary = Color(0xFF690005),
    onBackground = Color(0xFFE6E1E5),  // Light text
    onSurface = Color(0xFFE6E1E5)
)

val CinematicLightColorScheme = lightColorScheme(
    primary = Color(0xFFBA1A1A),       // Vibrant cinematic red
    secondary = Color(0xFF4A4D55),     // Dark charcoal
    background = Color(0xFFF5F6FA),    // Premium light background (off-white)
    surface = Color(0xFFFFFFFF),       // Pure white for cards
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111215),  // Readable dark text
    onSurface = Color(0xFF111215)
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
