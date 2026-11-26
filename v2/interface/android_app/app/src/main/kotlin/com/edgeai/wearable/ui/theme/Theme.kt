package com.edgeai.wearable.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CalmClinicalColorScheme = lightColorScheme(
    primary = AccentTeal,
    onPrimary = TextInvert,
    primaryContainer = AccentTealLight,
    onPrimaryContainer = AccentTealDark,
    secondary = TextPrimary,
    onSecondary = TextInvert,
    secondaryContainer = BgNavActive,
    onSecondaryContainer = TextNavActive,
    background = BgPage,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgSoft,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    outlineVariant = BorderStrong,
    error = DangerColor,
    onError = TextInvert,
    errorContainer = DangerBg,
    onErrorContainer = DangerColor,
)

@Composable
fun EdgeAIWearableTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CalmClinicalColorScheme,
        typography = Typography,
        content = content,
    )
}
