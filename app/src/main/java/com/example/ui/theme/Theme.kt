package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = DarkBg,
    primaryContainer = CardBg,
    onPrimaryContainer = TealPrimary,
    secondary = GoldAccent,
    onSecondary = DarkBg,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = CardBg,
    onSurface = TextPrimary,
    surfaceVariant = DarkBgElevated,
    onSurfaceVariant = TextSecondary,
    outline = TealBorder,
    error = RedAccent,
    onError = TextPrimary
)

@Composable
fun CryptoSignalsTheme(
    content: @Composable () -> Unit
) {
    // Force RTL for Arabic layout and Dark Color Scheme per user requirements
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
