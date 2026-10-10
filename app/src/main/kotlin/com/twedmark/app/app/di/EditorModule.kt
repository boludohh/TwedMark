package com.twedmark.app.app.di

import com.twedmark.app.feature.editor.data.EditorPrefs
import com.twedmark.app.feature.editor.domain.DocumentSaver
import com.twedmark.app.feature.editor.presentation.EditorViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val editorModule = module {
    factory { DocumentSaver(get(), get()) }
    single { EditorPrefs(get()) }
    viewModel { EditorViewModel(get(), get(), get(), get(), get(), get()) }
}