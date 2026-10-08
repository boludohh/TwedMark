package com.twedmark.app.app

import kotlinx.serialization.Serializable

/**
 * Rutas de navegación tipadas para la aplicación.
 * Se usan con Navigation Compose type-safe.
 */
@Serializable
data object Main

@Serializable
data object CrashLog

@Serializable
data object DebugPaths