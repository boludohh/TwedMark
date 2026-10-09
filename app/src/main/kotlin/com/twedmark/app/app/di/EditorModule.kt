package com.twedmark.app.app.di

import com.twedmark.app.feature.editor.domain.DocumentSaver
import org.koin.dsl.module

val editorModule = module {
    // Factory: se crea una nueva instancia por cada EditorViewModel
    factory { DocumentSaver(get(), get()) }
}