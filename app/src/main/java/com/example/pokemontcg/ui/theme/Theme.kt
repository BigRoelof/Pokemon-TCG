package com.example.pokemontcg.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Brand colors are fixed: no dynamic (wallpaper) color, so the red header stays red.
private val LightColorScheme = lightColorScheme(
    primary = PokeRed,
    onPrimary = BallWhite,
    primaryContainer = PokeRed,
    onPrimaryContainer = BallWhite,
    secondary = Ink,
    onSecondary = BallWhite,
    secondaryContainer = MistGrey,
    onSecondaryContainer = Ink,
    tertiary = CaughtYellow,
    onTertiary = Ink,
    background = CloudWhite,
    onBackground = Ink,
    surface = CloudWhite,
    onSurface = Ink,
    surfaceVariant = MistGrey,
    onSurfaceVariant = SlateText,
    surfaceContainer = BallWhite,
    surfaceContainerHigh = BallWhite,
    outline = SlateText,
    outlineVariant = MistGrey
)

private val DarkColorScheme = darkColorScheme(
    primary = PokeRed,
    onPrimary = BallWhite,
    primaryContainer = PokeRedDark,
    onPrimaryContainer = BallWhite,
    secondary = NightText,
    onSecondary = Ink,
    secondaryContainer = NightRaised,
    onSecondaryContainer = NightText,
    tertiary = CaughtYellow,
    onTertiary = Ink,
    background = NightNavy,
    onBackground = NightText,
    surface = NightNavy,
    onSurface = NightText,
    surfaceVariant = NightRaised,
    onSurfaceVariant = NightMuted,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightRaised,
    outline = NightMuted,
    outlineVariant = NightRaised
)

@Composable
fun PokemonTCGChaseListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
