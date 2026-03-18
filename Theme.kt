package com.studypulse.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// StudyPulse Dark Navy & Purple Theme
val Navy = Color(0xFF080D1A)
val Navy2 = Color(0xFF0E1628)
val Navy3 = Color(0xFF141F35)
val Navy4 = Color(0xFF1C2B47)
val Purple = Color(0xFF7C3AED)
val Purple2 = Color(0xFF9D5CF6)
val Purple3 = Color(0xFFC084FC)
val Accent = Color(0xFFA78BFA)
val TextPrimary = Color(0xFFE8EAF6)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)
val Green = Color(0xFF10B981)
val Orange = Color(0xFFF59E0B)
val Red = Color(0xFFEF4444)

private val DarkColorScheme = darkColorScheme(
    primary = Purple,
    secondary = Purple2,
    tertiary = Purple3,
    background = Navy,
    surface = Navy2,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = Navy3,
    outline = Color(0xFF2D3F60)
)

@Composable
fun StudyPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
