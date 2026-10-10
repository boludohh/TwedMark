package com.twedmark.app.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ApplicationScope(dispatchers: AppDispatchers) {
    val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    
    // Canal global para eventos de ciclo de vida del proceso
    private val _processLifecycleEvents = MutableSharedFlow<FlushReason>(extraBufferCapacity = 16)
    val processLifecycleEvents: SharedFlow<FlushReason> = _processLifecycleEvents.asSharedFlow()
    
    fun emitLifecycleEvent(reason: FlushReason) {
        _processLifecycleEvents.tryEmit(reason)
    }
}

// Razones de flush compartidas entre Application y ViewModels
enum class FlushReason { 
    Debounce,           // Autoguardado por debounce de 1s
    Manual,             // Usuario pulsó guardar manualmente
    AppPaused,          // Proceso entra en ON_PAUSE
    ScreenStopped,      // Pantalla específica entra en ON_STOP
    SwitchingFile,      // Usuario abre otro archivo
    Closing             // Usuario cierra el documento
}