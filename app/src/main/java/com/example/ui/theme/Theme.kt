package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CraftHubColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = DiamondSecondary,
    onSecondary = ObsidianBackground,
    secondaryContainer = DiamondContainer,
    onSecondaryContainer = DiamondOnContainer,
    tertiary = AmethystTertiary,
    onTertiary = ObsidianBackground,
    tertiaryContainer = AmethystContainer,
    onTertiaryContainer = AmethystOnContainer,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CraftHubColorScheme,
        typography = Typography,
        content = content
    )
}
