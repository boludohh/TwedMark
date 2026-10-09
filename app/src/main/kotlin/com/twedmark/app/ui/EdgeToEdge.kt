package com.twedmark.app.ui

import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat

/**
 * Configuración global de Edge-to-Edge para TwedMark.
 *
 * Centraliza en un único archivo toda la configuración necesaria para que la app
 * dibuje su contenido por debajo de las barras de estado y navegación con ambas
 * barras 100% transparentes, cubriendo desde API 27 hasta API 35+.
 *
 * Lo que aplica esta función:
 * 1. `enableEdgeToEdge()` antes de `setContent` (androidx.activity).
 * 2. Eliminación de flags traslúcidos heredados (`FLAG_TRANSLUCENT_NAVIGATION` y
 *    `FLAG_TRANSLUCENT_STATUS`) que agregan degradados en API 27 y 28.
 * 3. Transparencia explícita a nivel de `Window`, para que capas de personalización
 *    OEM (MIUI/HyperOS, One UI, ColorOS) que pisen el tema XML no recuperen colores
 *    opacos en las barras.
 * 4. Desactivación del contraste forzado por el sistema (`isStatusBarContrastEnforced`
 *    y `isNavigationBarContrastEnforced`, API 29+). Este es el mecanismo que AOSP y las
 *    capas OEM utilizan para dibujar un "filtro"/scrim sobre las barras transparentes.
 * 5. Desactivación del "force dark" de plataforma mediante `View.setForceDarkAllowed`
 *    sobre el `decorView` (API 29+). Force dark es una API de `android.view.View`, no
 *    de `Window`; al desactivarlo en la vista raíz queda cubierto todo el árbol de la
 *    ventana, incluido el contenido Compose. Es el mecanismo utilizado por MIUI/HyperOS
 *    y otras capas para alterar forzosamente los colores.
 * 6. Extensión del contenido al área del recorte de pantalla (notch) en API 28+
 *    (`LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`).
 *
 * Esta configuración se complementa a nivel XML en:
 * - `res/values/themes.xml`: colores de barra transparentes para todos los APIs,
 *   y `windowBackground` para el color de splash según el tema.
 *
 * Nota sobre capas de fabricantes: solo se utilizan APIs públicas documentadas.
 * MIUI/HyperOS, One UI y ColorOS respetan los atributos estándar de contraste y
 * force-dark, por lo que desactivarlos cubre los casos reales documentados.
 *
 * Apariencia de las barras del sistema:
 * - La apariencia de los iconos de las barras (oscuros para tema claro, claros
 *   para tema oscuro) se gestiona dinámicamente mediante [configureSystemBarsAppearance],
 *   no en esta función.
 *
 * Nota sobre deprecations: los flags traslúcidos, las propiedades statusBarColor/
 * navigationBarColor y los scrims de contraste están deprecados en Android 15+
 * (API 35) porque edge-to-edge es obligatorio y ya no tienen efecto. Se mantienen
 * aquí para garantizar el comportamiento correcto en API 27-34, donde sí son
 * necesarios. Los `@Suppress("DEPRECATION")` locales silencian las advertencias
 * de forma explícita y justificada.
 */
fun ComponentActivity.configureEdgeToEdge() {
    // 1) Edge to edge base: dibuja bajo las barras con transparencia.
    enableEdgeToEdge()

    // Garantiza que el contenido se extienda detrás de las barras del sistema.
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // 2) Elimina flags traslúcidos heredados que agregan degradados
    //    en API 27 y 28. Deprecados en Android 15+ (API 35).
    @Suppress("DEPRECATION")
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
    @Suppress("DEPRECATION")
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

    // Flag necesario para que apliquen los colores de barra definidos aquí.
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

    // 3) Transparencia explícita a nivel de ventana. Deprecadas en
    //    Android 15+ (API 35), necesarias en API 27-34.
    @Suppress("DEPRECATION")
    window.statusBarColor = Color.TRANSPARENT
    @Suppress("DEPRECATION")
    window.navigationBarColor = Color.TRANSPARENT

    // 4) y 5) Desactiva scrims de contraste y force dark (API 29+).
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        @Suppress("DEPRECATION")
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
        window.decorView.setForceDarkAllowed(false)
    }

    // 6) Extiende el contenido al área del notch (API 28+).
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val params = window.attributes
        params.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        window.attributes = params
    }
}