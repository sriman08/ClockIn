package com.inkline.lockscreen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val InkLineDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFAF9F6),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFFFFD426),
    onSecondary = Color(0xFF000000),
    tertiary = Color(0xFF9CA3AF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFAF9F6),
    surface = Color(0xFF111113),
    onSurface = Color(0xFFFAF9F6),
    surfaceVariant = Color(0xFF1C1C20),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF2B2B30),
    error = Color(0xFFFF4F87),
    onError = Color(0xFF1F0A0A)
)

@Composable
fun InkLineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = InkLineDarkColorScheme,
        content = content
    )
}
