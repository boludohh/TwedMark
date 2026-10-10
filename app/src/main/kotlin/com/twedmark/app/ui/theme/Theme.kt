package com.twedmark.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    background = LightBackground,
    surface = LightCardLevel1,
    surfaceVariant = LightCardLevel2,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorderCardLevel1,
    outlineVariant = LightBorderCardLevel2,
    primary = LightBorderActive,
    onPrimary = LightBackground,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    scrim = LightScrim
)

private val DarkColorScheme = darkColorScheme(
    background = DarkBackground,
    surface = DarkCardLevel1,
    surfaceVariant = DarkCardLevel2,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderCardLevel1,
    outlineVariant = DarkBorderCardLevel2,
    primary = DarkBorderActive,
    onPrimary = DarkBackground,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    scrim = DarkScrim
)

private val AmoledColorScheme = darkColorScheme(
    background = AmoledBackground,
    surface = AmoledCardLevel1,
    surfaceVariant = AmoledCardLevel2,
    onBackground = AmoledTextPrimary,
    onSurface = AmoledTextPrimary,
    onSurfaceVariant = AmoledTextSecondary,
    outline = AmoledBorderCardLevel1,
    outlineVariant = AmoledBorderCardLevel2,
    primary = AmoledBorderActive,
    onPrimary = AmoledBackground,
    error = AmoledError,
    onError = AmoledOnError,
    errorContainer = AmoledErrorContainer,
    onErrorContainer = AmoledOnErrorContainer,
    scrim = AmoledScrim
)

// Datos extendidos para colores personalizados
data class ExtendedColors(
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color
)

val LightExtendedColors = ExtendedColors(
    warning = LightWarning,
    onWarning = LightOnWarning,
    warningContainer = LightWarningContainer,
    onWarningContainer = LightOnWarningContainer,
    success = LightSuccess,
    onSuccess = LightOnSuccess,
    successContainer = LightSuccessContainer,
    onSuccessContainer = LightOnSuccessContainer
)

val DarkExtendedColors = ExtendedColors(
    warning = DarkWarning,
    onWarning = DarkOnWarning,
    warningContainer = DarkWarningContainer,
    onWarningContainer = DarkOnWarningContainer,
    success = DarkSuccess,
    onSuccess = DarkOnSuccess,
    successContainer = DarkSuccessContainer,
    onSuccessContainer = DarkOnSuccessContainer
)

val AmoledExtendedColors = ExtendedColors(
    warning = AmoledWarning,
    onWarning = AmoledOnWarning,
    warningContainer = AmoledWarningContainer,
    onWarningContainer = AmoledOnWarningContainer,
    success = AmoledSuccess,
    onSuccess = AmoledOnSuccess,
    successContainer = AmoledSuccessContainer,
    onSuccessContainer = AmoledOnSuccessContainer
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/**
 * Tema principal de TwedMark.
 * Por ahora sigue el sistema: claro u oscuro.
 * AMOLED se activará manualmente en fases posteriores con DataStore.
 */
@Composable
fun TwedMarkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}

// Extensión para acceder fácilmente a los colores extendidos
object MaterialThemeExtended {
    val colorScheme: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}