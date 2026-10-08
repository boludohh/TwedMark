package com.twedmark.app.app.di

import com.twedmark.app.feature.explorer.data.WorkspaceRepository
import com.twedmark.app.feature.explorer.presentation.ExplorerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val explorerModule = module {
    single { WorkspaceRepository(get(), get(), get()) }
    viewModel { ExplorerViewModel(get()) }
}