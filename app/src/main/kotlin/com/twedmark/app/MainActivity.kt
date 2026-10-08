package com.twedmark.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.twedmark.app.app.AppNavHost
import com.twedmark.app.ui.theme.TwedMarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TwedMarkTheme {
                Surface(modifier = Modifier) {
                    AppNavHost()
                }
            }
        }
    }
}