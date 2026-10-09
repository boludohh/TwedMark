package com.twedmark.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.FontFamily
import androidx.compose.ui.text.FontWeight
import androidx.compose.ui.text.font.Font

// Definición de la familia tipográfica IBM Plex Sans
private val IbmPlexSans = FontFamily(
    Font(resId = com.twedmark.app.R.font.ibmplexsans_text, weight = FontWeight.Light),
    Font(resId = com.twedmark.app.R.font.ibmplexsans_text_italic, weight = FontWeight.Light, style = androidx.compose.ui.text.font.FontStyle.Italic),
    Font(resId = com.twedmark.app.R.font.ibmplexsans_regular, weight = FontWeight.Normal),
    Font(resId = com.twedmark.app.R.font.ibmplexsans_regular_italic, weight = FontWeight.Normal, style = androidx.compose.ui.text.font.FontStyle.Italic),
    Font(resId = com.twedmark.app.R.font.ibmplexsans_medium, weight = FontWeight.Medium),
    Font(resId = com.twedmark.app.R.font.ibmplexsans_medium_italic, weight = FontWeight.Medium, style = androidx.compose.ui.text.font.FontStyle.Italic),
    Font(resId = com.twedmark.app.R.font.ibmplexsans_semibold, weight = FontWeight.SemiBold),
    Font(resId = com.twedmark.app.R.font.ibmplexsans_semibold_italic, weight = FontWeight.SemiBold, style = androidx.compose.ui.text.font.FontStyle.Italic),
    Font(resId = com.twedmark.app.R.font.ibmplexsans_bold, weight = FontWeight.Bold),
    Font(resId = com.twedmark.app.R.font.ibmplexsans_bold_italic, weight = FontWeight.Bold, style = androidx.compose.ui.text.font.FontStyle.Italic)
)

// Tipografía personalizada usando IBM Plex Sans
val AppTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Light,
        fontSize = androidx.compose.ui.unit.sp(57.sp),
        lineHeight = androidx.compose.ui.unit.sp(64.sp),
        letterSpacing = androidx.compose.ui.unit.sp((-0.25).sp)
    ),
    displayMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Light,
        fontSize = androidx.compose.ui.unit.sp(45.sp),
        lineHeight = androidx.compose.ui.unit.sp(52.sp),
        letterSpacing = androidx.compose.ui.unit.sp(0.sp)
    ),
    displaySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = androidx.compose.ui.unit.sp(36.sp),
        lineHeight = androidx.compose.ui.unit.sp(44.sp),
        letterSpacing = androidx.compose.ui.unit.sp(0.sp)
    ),
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = androidx.compose.ui.unit.sp(32.sp),
        lineHeight = androidx.compose.ui.unit.sp(40.sp),
        letterSpacing = androidx.compose.ui.unit.sp(0.sp)
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = androidx.compose.ui.unit.sp(28.sp),
        lineHeight = androidx.compose.ui.unit.sp(36.sp),
        letterSpacing = androidx.compose.ui.unit.sp(0.sp)
    ),
    headlineSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = androidx.compose.ui.unit.sp(24.sp),
        lineHeight = androidx.compose.ui.unit.sp(32.sp),
        letterSpacing = androidx.compose.ui.unit.sp(0.sp)
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = androidx.compose.ui.unit.sp(22.sp),
        lineHeight = androidx.compose.ui.unit.sp(28.sp),
        letterSpacing = androidx.compose.ui.unit.sp(0.sp)
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = androidx.compose.ui.unit.sp(16.sp),
        lineHeight = androidx.compose.ui.unit.sp(24.sp),
        letterSpacing = androidx.compose.ui.unit.sp(0.15.sp)
    ),
    titleSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Medium,
        fontSize = androidx.compose.ui.unit.sp(14.sp),
        lineHeight = androidx.compose.ui.unit.sp(20.sp),
        letterSpacing = androidx.compose.ui.unit.sp(0.1.sp)
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = androidx.compose.ui.unit.sp(16.sp),
        lineHeight = androidx.compose.ui.unit.sp(24.sp),
        letterSpacing = androidx.compose.ui.unit.sp(0.5.sp)
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = androidx.compose.ui.unit.sp(14.sp),
        lineHeight = androidx.compose.ui.unit.sp(20.sp),
        letterSpacing = androidx.compose.ui.unit.sp(0.25.sp)
    ),
    bodySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = androidx.compose.ui.unit.sp(12.sp),
        lineHeight = androidx.compose.ui.unit.sp(16.sp),
        letterSpacing = androidx.compose.ui.unit.sp(0.4.sp)
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Medium,
        fontSize = androidx.compose.ui.unit.sp(14.sp),
        lineHeight = androidx.compose.ui.unit.sp(20.sp),
        letterSpacing = androidx.compose.ui.unit.sp(0.1.sp)
    ),
    labelMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Medium,
        fontSize = androidx.compose.ui.unit.sp(12.sp),
        lineHeight = androidx.compose.ui.unit.sp(16.sp),
        letterSpacing = androidx.compose.ui.unit.sp(0.5.sp)
    ),
    labelSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Medium,
        fontSize = androidx.compose.ui.unit.sp(11.sp),
        lineHeight = androidx.compose.ui.unit.sp(16.sp),
        letterSpacing = androidx.compose.ui.unit.sp(0.5.sp)
    )
)

// Extensión para usar sp directamente
private val Int.sp get() = androidx.compose.ui.unit.sp