package com.twedmark.app.app.di

import com.twedmark.app.feature.settings.crash.CrashLogViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val crashModule = module {
    viewModel { CrashLogViewModel(get()) }
}