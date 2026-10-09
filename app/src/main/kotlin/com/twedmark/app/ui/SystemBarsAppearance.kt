package com.twedmark.app.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Ajusta la apariencia de los iconos de las barras del sistema según el tema.
 *
 * - Tema claro: iconos oscuros sobre barra transparente (visibles sobre fondo blanco).
 * - Tema oscuro/AMOLED: iconos claros sobre barra transparente (visibles sobre fondo negro).
 *
 * La función se debe llamar dentro de un [SideEffect] para reaccionar automáticamente
 * a los cambios de tema del sistema sin recrear la Activity.
 *
 * Usa [WindowCompat.getInsetsController] que es la API recomendada de `androidx.core`
 * y funciona desde API 27 (minSdk del proyecto).
 */
@Composable
fun configureSystemBarsAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
                ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            // isAppearanceLightStatusBars = true → iconos OSCUROS (para tema claro)
            // isAppearanceLightStatusBars = false → iconos CLAROS (para tema oscuro)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}