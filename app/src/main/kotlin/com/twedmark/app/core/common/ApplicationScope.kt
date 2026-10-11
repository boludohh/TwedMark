package com.twedmark.app.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ApplicationScope(dispatchers: AppDispatchers) {
    val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    
    private val _processLifecycleEvents = MutableSharedFlow<FlushReason>(extraBufferCapacity = 16)
    val processLifecycleEvents: SharedFlow<FlushReason> = _processLifecycleEvents.asSharedFlow()
    
    fun emitLifecycleEvent(reason: FlushReason) {
        _processLifecycleEvents.tryEmit(reason)
    }
}

enum class FlushReason { 
    Debounce,
    Manual,
    AppPaused,
    AppStopped,      // NUEVO: Proceso entra en ON_STOP
    ScreenStopped,
    SwitchingFile,
    Closing
}