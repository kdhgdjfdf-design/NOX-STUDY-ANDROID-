package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NightStudyAmber,
    onPrimary = Color(0xFF3B1E06),
    primaryContainer = NightStudyAmberContainer,
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = NightStudySage,
    onSecondary = Color(0xFF1B2914),
    secondaryContainer = NightStudySageContainer,
    onSecondaryContainer = Color(0xFFD4E7C8),
    tertiary = NightStudyTerracotta,
    onTertiary = Color(0xFF45170B),
    background = NightStudyBg,
    surface = NightStudySurface,
    surfaceVariant = NightStudyVariant,
    onBackground = NightStudyTextLight,
    onSurface = NightStudyTextLight,
    onSurfaceVariant = NightStudyTextMuted,
    outline = NightStudyBorder
)

private val LightColorScheme = lightColorScheme(
    primary = CozyEspressoPrimary,
    onPrimary = Color.White,
    primaryContainer = CozyEspressoContainer,
    onPrimaryContainer = CozyEspressoOnContainer,
    secondary = CozySageSecondary,
    onSecondary = Color.White,
    secondaryContainer = CozySageContainer,
    onSecondaryContainer = CozySageOnContainer,
    tertiary = CozyTerracotta,
    onTertiary = Color.White,
    background = CozyCreamBg,
    surface = CozyCreamSurface,
    surfaceVariant = CozyLatteVariant,
    onBackground = CozyTextDark,
    onSurface = CozyTextDark,
    onSurfaceVariant = CozyTextMuted,
    outline = CozyBorder
)

@Composable
fun NoxStudyTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
