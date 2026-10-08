package com.twedmark.app.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.twedmark.app.core.common.ApplicationScope
import com.twedmark.app.core.common.AppDispatchers
import com.twedmark.app.core.common.DefaultAppDispatchers
import com.twedmark.app.core.file.AtomicFileWriter
import com.twedmark.app.core.file.MarkdownPath
import com.twedmark.app.core.file.NameSanitizer
import com.twedmark.app.core.file.NoteIO
import com.twedmark.app.core.file.WorkspaceManager
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val coreModule = module {
    single<AppDispatchers> { DefaultAppDispatchers() }
    single { ApplicationScope(get()) }
    single { FileSystem.SYSTEM }
    single {
        val context = androidContext()
        val baseDir = context.filesDir.absolutePath.toPath()
        WorkspaceManager(get(), baseDir)
    }
    single { AtomicFileWriter(get()) }
    single { NoteIO(get(), get()) }
    single { MarkdownPath }
    single { NameSanitizer }
    single<DataStore<Preferences>> { androidContext().dataStore }
}