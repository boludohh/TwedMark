package com.twedmark.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.twedmark.app.app.di.coreModule
import com.twedmark.app.app.di.crashModule
import com.twedmark.app.app.di.debugModule
import com.twedmark.app.app.di.editorModule
import com.twedmark.app.app.di.explorerModule
import com.twedmark.app.core.common.ApplicationScope
import com.twedmark.app.core.common.FlushReason
import com.twedmark.app.core.file.WorkspaceManager
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TwedMarkApp : Application() {
    
    private val workspaceManager: WorkspaceManager by inject()
    private val applicationScope: ApplicationScope by inject()
    
    override fun onCreate() {
        super.onCreate()
        
        CrashHandler.install(filesDir)
        
        startKoin {
            androidContext(this@TwedMarkApp)
            modules(
                coreModule,
                explorerModule,
                editorModule,
                crashModule,
                debugModule
            )
        }
        
        applicationScope.scope.launch {
            workspaceManager.ensureStructure()
        }
        
        ProcessLifecycleOwner.get().lifecycle.addObserver(ProcessLifecycleObserver())
    }
    
    private inner class ProcessLifecycleObserver : DefaultLifecycleObserver {
        override fun onPause(owner: LifecycleOwner) {
            applicationScope.emitLifecycleEvent(FlushReason.AppPaused)
        }
        
        override fun onStop(owner: LifecycleOwner) {
            // Emitir AppStopped para garantizar flush antes de que la app se destruya
            applicationScope.emitLifecycleEvent(FlushReason.AppStopped)
        }
    }
}