package com.twedmark.app.app.di

import com.twedmark.app.feature.settings.debug.DebugPathsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val debugModule = module {
    viewModel { DebugPathsViewModel(androidContext(), get()) }
}