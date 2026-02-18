package io.github.jn0v.traceglass.feature.timelapse.di

import io.github.jn0v.traceglass.core.timelapse.SnapshotStorage
import io.github.jn0v.traceglass.core.timelapse.TimelapseCompiler
import io.github.jn0v.traceglass.core.timelapse.VideoExporter
import io.github.jn0v.traceglass.core.timelapse.VideoSharer
import io.github.jn0v.traceglass.feature.timelapse.FileSnapshotStorage
import io.github.jn0v.traceglass.feature.timelapse.IntentVideoSharer
import io.github.jn0v.traceglass.feature.timelapse.MediaCodecCompiler
import io.github.jn0v.traceglass.feature.timelapse.MediaStoreVideoExporter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val timelapseModule = module {
    single<SnapshotStorage> { FileSnapshotStorage(androidContext().filesDir) }
    single<TimelapseCompiler> { MediaCodecCompiler() }
    single<VideoExporter> { MediaStoreVideoExporter(androidContext()) }
    single<VideoSharer> { IntentVideoSharer(androidContext()) }
}
