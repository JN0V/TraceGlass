package io.github.jn0v.traceglass.feature.tracing.di

import io.github.jn0v.traceglass.core.overlay.OverlayTransformCalculator
import io.github.jn0v.traceglass.feature.tracing.FrameAnalyzer
import io.github.jn0v.traceglass.feature.tracing.TracingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val tracingModule = module {
    single { OverlayTransformCalculator() }
    single { FrameAnalyzer(markerDetector = get()) }
    viewModel { TracingViewModel(flashlightController = get(), transformCalculator = get()) }
}
