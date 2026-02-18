package io.github.jn0v.traceglass.core.cv.di

import io.github.jn0v.traceglass.core.cv.MarkerDetector
import io.github.jn0v.traceglass.core.cv.impl.OpenCvMarkerDetector
import org.koin.dsl.module

val cvModule = module {
    single<MarkerDetector> { OpenCvMarkerDetector() }
}
