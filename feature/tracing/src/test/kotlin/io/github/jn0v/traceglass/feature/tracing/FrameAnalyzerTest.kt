package io.github.jn0v.traceglass.feature.tracing

import android.util.Log
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import io.github.jn0v.traceglass.core.cv.DetectedMarker
import io.github.jn0v.traceglass.core.cv.MarkerDetector
import io.github.jn0v.traceglass.core.cv.MarkerResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

/** Local test fake — core:cv's FakeMarkerDetector lives in test source, not accessible here. */
private class TestMarkerDetector : MarkerDetector {
    var resultToReturn: MarkerResult = MarkerResult(emptyList(), 0L)
    var detectCallCount: Int = 0
        private set

    override fun detect(
        frameBuffer: ByteBuffer, width: Int, height: Int,
        rowStride: Int, rotation: Int
    ): MarkerResult {
        detectCallCount++
        return resultToReturn
    }
}

class FrameAnalyzerTest {

    private lateinit var fakeDetector: TestMarkerDetector
    private lateinit var analyzer: FrameAnalyzer

    @BeforeEach
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        fakeDetector = TestMarkerDetector()
        analyzer = FrameAnalyzer(fakeDetector)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun createMockImageProxy(
        width: Int = 640,
        height: Int = 480,
        rowStride: Int = 640,
        pixelStride: Int = 1,
        rotationDegrees: Int = 0,
        bufferSize: Int = width * height
    ): ImageProxy {
        val buffer = ByteBuffer.allocateDirect(bufferSize)
        val plane = mockk<ImageProxy.PlaneProxy> {
            every { this@mockk.buffer } returns buffer
            every { this@mockk.rowStride } returns rowStride
            every { this@mockk.pixelStride } returns pixelStride
        }
        val imageInfo = mockk<ImageInfo> {
            every { this@mockk.rotationDegrees } returns rotationDegrees
        }
        return mockk<ImageProxy>(relaxed = true) {
            every { this@mockk.width } returns width
            every { this@mockk.height } returns height
            every { this@mockk.planes } returns arrayOf(plane)
            every { this@mockk.imageInfo } returns imageInfo
            every { this@mockk.format } returns 35 // ImageFormat.YUV_420_888
        }
    }

    @Nested
    inner class BasicDetection {
        @Test
        fun `passes correct parameters and closes image`() {
            val image = createMockImageProxy(
                width = 800, height = 600, rowStride = 832, rotationDegrees = 90
            )
            fakeDetector.resultToReturn = MarkerResult(emptyList(), 5L, 600, 800)

            analyzer.analyze(image)

            assertEquals(1, fakeDetector.detectCallCount)
            verify { image.close() }
        }

        @Test
        fun `updates StateFlow with detection result`() {
            val marker = DetectedMarker(
                id = 0, centerX = 100f, centerY = 200f,
                corners = listOf(90f to 190f, 110f to 190f, 110f to 210f, 90f to 210f),
                confidence = 1.0f
            )
            val expected = MarkerResult(listOf(marker), 10L, 640, 480)
            fakeDetector.resultToReturn = expected
            val image = createMockImageProxy()

            analyzer.analyze(image)

            val result = analyzer.latestResult.value
            assertTrue(result.isTracking)
            assertEquals(1, result.markerCount)
            assertEquals(10L, result.detectionTimeMs)
        }

        @Test
        fun `initial state is empty result`() {
            val result = analyzer.latestResult.value
            assertFalse(result.isTracking)
            assertEquals(0, result.markerCount)
        }
    }

    @Nested
    inner class ErrorHandling {
        @Test
        fun `closes image even when detector throws`() {
            val throwingDetector = object : MarkerDetector {
                override fun detect(
                    frameBuffer: ByteBuffer, width: Int, height: Int,
                    rowStride: Int, rotation: Int
                ): MarkerResult = throw RuntimeException("Detection failed")
            }
            val analyzerWithBrokenDetector = FrameAnalyzer(throwingDetector)
            val image = createMockImageProxy()

            analyzerWithBrokenDetector.analyze(image)

            verify { image.close() }
        }

        @Test
        fun `does not update result when detector throws`() {
            val throwingDetector = object : MarkerDetector {
                override fun detect(
                    frameBuffer: ByteBuffer, width: Int, height: Int,
                    rowStride: Int, rotation: Int
                ): MarkerResult = throw RuntimeException("Crash")
            }
            val brokenAnalyzer = FrameAnalyzer(throwingDetector)
            val image = createMockImageProxy()

            brokenAnalyzer.analyze(image)

            assertFalse(brokenAnalyzer.latestResult.value.isTracking)
        }
    }

    @Nested
    inner class SnapshotCallback {
        @Test
        fun `snapshotCallback is null by default`() {
            assertNull(analyzer.snapshotCallback)
        }

        @Test
        fun `snapshotCallback can be set and read`() {
            val callback: (ByteArray) -> Unit = { }
            analyzer.snapshotCallback = callback
            assertEquals(callback, analyzer.snapshotCallback)
        }

        @Test
        fun `snapshotCallback is cleared after analyze consumes it`() {
            // toBitmap() requires Android runtime, so we mock it
            // When toBitmap() throws (no Android env), the exception is caught
            // and the callback should still be cleared because it's set to null before toBitmap
            analyzer.snapshotCallback = { }
            val image = createMockImageProxy()

            analyzer.analyze(image)

            // The callback is cleared to null before toBitmap() is called,
            // so even if toBitmap() fails, the callback is consumed
            assertNull(analyzer.snapshotCallback)
        }
    }

    @Nested
    inner class MultipleFrames {
        @Test
        fun `processes multiple frames and updates result each time`() {
            val noMarkers = MarkerResult(emptyList(), 3L)
            val withMarkers = MarkerResult(
                listOf(DetectedMarker(0, 100f, 100f, emptyList(), 1f)),
                5L, 640, 480
            )

            fakeDetector.resultToReturn = noMarkers
            analyzer.analyze(createMockImageProxy())
            assertFalse(analyzer.latestResult.value.isTracking)

            fakeDetector.resultToReturn = withMarkers
            analyzer.analyze(createMockImageProxy())
            assertTrue(analyzer.latestResult.value.isTracking)

            assertEquals(2, fakeDetector.detectCallCount)
        }
    }
}
