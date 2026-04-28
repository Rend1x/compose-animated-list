package com.rend1x.composeanimatedlist.sample

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF256B4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F3E3),
    onPrimaryContainer = Color(0xFF082116),
    secondary = Color(0xFF52635A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E8DD),
    onSecondaryContainer = Color(0xFF101F18),
    background = Color(0xFFF8FAF7),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFDFDF9),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDDE5DD),
    onSurfaceVariant = Color(0xFF414941),
    outline = Color(0xFF717971),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8DCC7),
    onPrimary = Color(0xFF123825),
    primaryContainer = Color(0xFF1D5138),
    onPrimaryContainer = Color(0xFFD4F8E1),
    secondary = Color(0xFFBBCDC1),
    onSecondary = Color(0xFF26342D),
    secondaryContainer = Color(0xFF3C4B43),
    onSecondaryContainer = Color(0xFFD7E9DD),
    background = Color(0xFF111411),
    onBackground = Color(0xFFE1E4DF),
    surface = Color(0xFF191C19),
    onSurface = Color(0xFFE1E4DF),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9C0),
    outline = Color(0xFF8B938B),
)

@Composable
fun SampleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
