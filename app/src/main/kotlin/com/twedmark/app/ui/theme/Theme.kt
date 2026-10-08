package com.twedmark.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}