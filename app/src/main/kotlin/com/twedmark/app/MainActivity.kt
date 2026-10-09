package com.twedmark.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.twedmark.app.app.AppNavHost
import com.twedmark.app.ui.configureEdgeToEdge
import com.twedmark.app.ui.configureSystemBarsAppearance
import com.twedmark.app.ui.theme.TwedMarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-Edge antes de setContent para que la primera composición
        // ya tenga las barras transparentes.
        configureEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TwedMarkTheme {
                // Reacciona al cambio de tema del sistema (claro ↔ oscuro)
                // y actualiza el color de los iconos de las barras.
                configureSystemBarsAppearance(darkTheme = isSystemInDarkTheme())
                Surface(modifier = Modifier) {
                    AppNavHost()
                }
            }
        }
    }
}