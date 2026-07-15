package com.expressit.journal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Eucalyptus,
    onPrimary = PaperCard,
    primaryContainer = MistGreen,
    onPrimaryContainer = EucalyptusDeep,
    secondary = InkSoft,
    onSecondary = PaperCard,
    secondaryContainer = PaperRaised,
    onSecondaryContainer = InkGreen,
    background = Paper,
    onBackground = InkGreen,
    surface = PaperCard,
    onSurface = InkGreen,
    surfaceVariant = PaperRaised,
    onSurfaceVariant = InkSoft,
    outline = LineHairline,
    outlineVariant = LineHairline,
    error = ClaySignal,
    onError = PaperCard
)

private val DarkColors = darkColorScheme(
    primary = EucalyptusLight,
    onPrimary = NightMoss,
    primaryContainer = EucalyptusDim,
    onPrimaryContainer = EucalyptusLight,
    secondary = MoonSoft,
    onSecondary = NightMoss,
    secondaryContainer = NightRaised,
    onSecondaryContainer = MoonText,
    background = NightMoss,
    onBackground = MoonText,
    surface = NightCard,
    onSurface = MoonText,
    surfaceVariant = NightRaised,
    onSurfaceVariant = MoonSoft,
    outline = NightLine,
    outlineVariant = NightLine,
    error = ClaySignalLight,
    onError = NightMoss
)

@Composable
fun ExpressItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ExpressItTypography,
        shapes = ExpressItShapes,
        content = content
    )
}
