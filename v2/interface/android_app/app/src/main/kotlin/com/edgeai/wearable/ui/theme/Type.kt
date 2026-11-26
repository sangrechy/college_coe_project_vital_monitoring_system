package com.edgeai.wearable.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.edgeai.wearable.R

val ManropeFontFamily = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

val IbmPlexMonoFontFamily = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

val Typography = Typography(
    headlineMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.25).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.15).sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        fontFeatureSettings = "tnum",
    ),
    bodySmall = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontFeatureSettings = "tnum",
    ),
    labelMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.8.sp,
    ),
)
