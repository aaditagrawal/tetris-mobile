package com.maverick.tetris_mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.maverick.tetris_mobile.R

val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic)
)

val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = InstrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1).sp,
        color = TextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = InstrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = InstrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = InstrumentSerif,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        color = TextSecondary
    ),
    titleLarge = TextStyle(
        fontFamily = GeistMono,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = GeistMono,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
        color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = GeistMono,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = GeistMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.3.sp,
        color = TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = GeistMono,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp,
        color = TextSecondary
    ),
    labelMedium = TextStyle(
        fontFamily = GeistMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.5.sp,
        color = TextTertiary
    )
)
