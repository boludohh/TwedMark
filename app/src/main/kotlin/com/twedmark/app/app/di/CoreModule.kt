package com.twedmark.app.app.di

import com.twedmark.app.core.common.ApplicationScope
import com.twedmark.app.core.common.AppDispatchers
import com.twedmark.app.core.common.DefaultAppDispatchers
import com.twedmark.app.core.file.WorkspaceManager
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single<AppDispatchers> { DefaultAppDispatchers() }
    single { ApplicationScope(get()) }
    single { FileSystem.SYSTEM }
    single {
        val context = androidContext()
        val baseDir = context.filesDir.absolutePath.toPath()
        WorkspaceManager(get(), baseDir)
    }
}