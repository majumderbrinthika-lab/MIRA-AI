package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MiraDarkColorScheme = darkColorScheme(
    primary = RoseGoldPrimary,
    onPrimary = OnRoseGold,
    primaryContainer = RoseGoldContainer,
    onPrimaryContainer = MoonlightText,
    secondary = ChampagneSecondary,
    onSecondary = MidnightPlumBg,
    secondaryContainer = VelvetCardElevated,
    onSecondaryContainer = ChampagneSecondary,
    tertiary = SoftBlushTertiary,
    background = MidnightPlumBg,
    onBackground = MoonlightText,
    surface = VelvetSurface,
    onSurface = MoonlightText,
    surfaceVariant = VelvetCard,
    onSurfaceVariant = MutedRoseText,
    outline = VelvetOutline,
    error = SoftCoralError
)

private val MiraLightColorScheme = lightColorScheme(
    primary = CrimsonRosePrimary,
    onPrimary = OnCrimsonRose,
    primaryContainer = RoseMistContainer,
    onPrimaryContainer = PlumInkText,
    secondary = WarmAmberSecondary,
    onSecondary = OnCrimsonRose,
    secondaryContainer = IvoryCard,
    onSecondaryContainer = PlumInkText,
    tertiary = CrimsonRosePrimary,
    background = IvoryBg,
    onBackground = PlumInkText,
    surface = IvorySurface,
    onSurface = PlumInkText,
    surfaceVariant = IvoryCard,
    onSurfaceVariant = SoftPlumMuted,
    outline = IvoryOutline,
    error = CrimsonRosePrimary
)

val MiraShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp)
)

@Composable
fun MiraTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MiraDarkColorScheme else MiraLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MiraTypography,
        shapes = MiraShapes,
        content = content
    )
}
