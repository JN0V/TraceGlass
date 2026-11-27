package io.github.jn0v.traceglass.core.cv.di

import io.github.jn0v.traceglass.core.cv.DetectionModeSelector
import io.github.jn0v.traceglass.core.cv.MarkerDetector
import io.github.jn0v.traceglass.core.cv.impl.CustomShapeMarkerDetector
import io.github.jn0v.traceglass.core.cv.impl.ModeSwitchingMarkerDetector
import io.github.jn0v.traceglass.core.cv.impl.OpenCvMarkerDetector
import org.koin.dsl.module

val cvModule = module {
    single { DetectionModeSelector() }
    single<MarkerDetector> {
        ModeSwitchingMarkerDetector(
            selector = get(),
            aruco = OpenCvMarkerDetector(),
            customShapes = CustomShapeMarkerDetector()
        )
    }
}
