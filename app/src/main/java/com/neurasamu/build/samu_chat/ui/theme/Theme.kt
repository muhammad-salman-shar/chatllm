package com.neurasamu.build.samu_chat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.neurasamu.build.samu_chat.data.AppSettings

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6B35),
    onPrimary = Color.White,
    background = Color(0xFF0E0E10),
    surface = Color(0xFF17171A),
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED),
    secondary = Color(0xFF22C55E)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF6B35),
    onPrimary = Color.White,
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    secondary = Color(0xFF22C55E)
)

@Composable
fun SamuChatTheme(
    settings: AppSettings,
    content: @Composable () -> Unit
) {
    val darkTheme = when (settings.theme.lowercase()) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val colors = if (darkTheme) DarkColors else LightColors

    val fontScale = settings.fontSizeMultiplier.coerceIn(0.7f, 1.5f)
    val baseDensity = LocalDensity.current

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale * fontScale
        )
    ) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
