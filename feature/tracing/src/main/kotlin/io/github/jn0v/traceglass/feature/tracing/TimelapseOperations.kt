package io.github.jn0v.traceglass.feature.tracing

import io.github.jn0v.traceglass.core.timelapse.SnapshotStorage
import io.github.jn0v.traceglass.core.timelapse.TimelapseCompiler
import io.github.jn0v.traceglass.core.timelapse.VideoExporter
import io.github.jn0v.traceglass.core.timelapse.VideoSharer
import java.io.File

/**
 * Groups timelapse-related dependencies to reduce TracingViewModel constructor parameter count.
 */
class TimelapseOperations(
    val snapshotStorage: SnapshotStorage,
    val compiler: TimelapseCompiler,
    val exporter: VideoExporter,
    val sharer: VideoSharer,
    val cacheDir: File
)
