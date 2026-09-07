package com.jinman.assistant.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Paper = Color(0xFFF3F1EC)
val Surface = Color(0xFFFFFCF7)
val Surface2 = Color(0xFFECE8E1)
val Ink = Color(0xFF1C1B19)
val Muted = Color(0xFF5E5C57)
val Subtle = Color(0xFF8A877F)
val Accent = Color(0xFF2C5F56)
val AccentFg = Color(0xFFF3FAF7)
val Danger = Color(0xFFA33B32)

private val colors = lightColorScheme(
    primary = Accent,
    onPrimary = AccentFg,
    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Surface2,
    onSurfaceVariant = Muted,
    outline = Color(0xFFD8D2C8),
    error = Danger,
)

@Composable
fun JinmanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
