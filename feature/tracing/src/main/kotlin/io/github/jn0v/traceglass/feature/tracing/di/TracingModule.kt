package io.github.jn0v.traceglass.feature.tracing.di

import io.github.jn0v.traceglass.feature.tracing.TracingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val tracingModule = module {
    viewModel { TracingViewModel() }
}
