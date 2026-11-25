package com.eternalvpn.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = GreenPrimary,
    onPrimary = OnGreen,
    primaryContainer = GreenSurfaceVariant,
    onPrimaryContainer = GreenPrimary,
    secondary = GreenPrimary,
    onSecondary = OnGreen,
    background = GreenDark,
    onBackground = TextPrimary,
    surface = GreenSurface,
    onSurface = TextPrimary,
    surfaceVariant = GreenSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White,
)

@Composable
fun EternalVpnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content,
    )
}
