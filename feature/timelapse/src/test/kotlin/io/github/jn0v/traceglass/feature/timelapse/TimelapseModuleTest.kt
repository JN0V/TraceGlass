package io.github.jn0v.traceglass.feature.timelapse

import io.github.jn0v.traceglass.core.timelapse.SnapshotStorage
import io.github.jn0v.traceglass.core.timelapse.TimelapseCompiler
import io.github.jn0v.traceglass.core.timelapse.TimelapseSession
import io.github.jn0v.traceglass.core.timelapse.VideoSharer
import io.github.jn0v.traceglass.feature.timelapse.di.timelapseModule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get
import io.mockk.mockk
import android.content.Context
import org.koin.android.ext.koin.androidContext

class TimelapseModuleTest : KoinTest {

    @AfterEach
    fun tearDown() {
        try { stopKoin() } catch (_: Exception) { }
    }

    @Test
    fun `timelapseModule resolves SnapshotStorage`() {
        val context = mockk<Context>(relaxed = true)
        startKoin {
            androidContext(context)
            modules(timelapseModule)
        }
        val storage: SnapshotStorage = get()
        assertNotNull(storage)
        assertTrue(storage is FileSnapshotStorage)
    }

@Test
    fun `timelapseModule resolves TimelapseCompiler`() {
        val context = mockk<Context>(relaxed = true)
        startKoin {
            androidContext(context)
            modules(timelapseModule)
        }
        val compiler: TimelapseCompiler = get()
        assertNotNull(compiler)
        assertTrue(compiler is MediaCodecCompiler)
    }

    @Test
    fun `timelapseModule resolves VideoSharer`() {
        val context = mockk<Context>(relaxed = true)
        startKoin {
            androidContext(context)
            modules(timelapseModule)
        }
        val sharer: VideoSharer = get()
        assertNotNull(sharer)
    }

    @Test
    fun `FileSnapshotStorage implements SnapshotStorage interface`() {
        val storage: SnapshotStorage = FileSnapshotStorage(java.io.File(System.getProperty("java.io.tmpdir")))
        assertNotNull(storage)
        assertTrue(storage is FileSnapshotStorage)
    }

    @Test
    fun `TimelapseSession factory creates independent instances`() {
        val session1 = TimelapseSession()
        val session2 = TimelapseSession()
        assertTrue(session1 !== session2)
    }
}
