package com.twedmark.app

import android.app.Application
import com.twedmark.app.app.di.coreModule
import com.twedmark.app.app.di.editorModule
import com.twedmark.app.app.di.explorerModule
import com.twedmark.app.core.common.ApplicationScope
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
        
        // TODO: CrashHandler.install(filesDir) se añadirá en el Paso 0.8
        
        startKoin {
            androidContext(this@TwedMarkApp)
            modules(coreModule, explorerModule, editorModule)
        }
        
        // Ejecutar ensureStructure en el scope de aplicación
        applicationScope.scope.launch {
            workspaceManager.ensureStructure()
        }
    }
}