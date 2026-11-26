package com.edgeai.wearable.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ClinicalColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = TextInvert,
    primaryContainer = AccentBlueLight,
    onPrimaryContainer = AccentBlueDark,
    secondary = BgNavy,
    onSecondary = TextInvert,
    secondaryContainer = BgNavyActive,
    onSecondaryContainer = TextInvert,
    background = BgPage,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = NeutralBg,
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
        colorScheme = ClinicalColorScheme,
        typography = Typography,
        content = content,
    )
}
