package com.neurasamu.build.samu_chat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6B35),
    onPrimary = Color.White,
    background = Color(0xFF0E0E10),
    surface = Color(0xFF17171A),
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED),
    secondary = Color(0xFF22C55E)
)

@Composable
fun SamuChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
