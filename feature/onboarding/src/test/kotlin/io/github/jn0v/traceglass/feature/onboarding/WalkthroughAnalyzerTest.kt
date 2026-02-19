package io.github.jn0v.traceglass.feature.onboarding

import androidx.camera.core.ImageProxy
import io.github.jn0v.traceglass.core.cv.DetectedMarker
import io.github.jn0v.traceglass.core.cv.MarkerDetector
import io.github.jn0v.traceglass.core.cv.MarkerResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class WalkthroughAnalyzerTest {

    private lateinit var markerDetector: MarkerDetector
    private lateinit var analyzer: WalkthroughAnalyzer

    @BeforeEach
    fun setUp() {
        markerDetector = mockk()
        analyzer = WalkthroughAnalyzer(markerDetector)
    }

    private fun mockImageProxy(
        width: Int = 640,
        height: Int = 480,
        rowStride: Int = 640,
        rotation: Int = 0
    ): ImageProxy {
        val buffer = ByteBuffer.allocateDirect(rowStride * height)
        val plane = mockk<ImageProxy.PlaneProxy> {
            every { this@mockk.buffer } returns buffer
            every { this@mockk.rowStride } returns rowStride
        }
        val imageInfo = mockk<android.media.ImageReader>()
        return mockk<ImageProxy> {
            every { this@mockk.width } returns width
            every { this@mockk.height } returns height
            every { planes } returns arrayOf(plane)
            every { this@mockk.imageInfo } returns mockk {
                every { rotationDegrees } returns rotation
            }
            every { close() } returns Unit
        }
    }

    @Nested
    inner class InitialState {
        @Test
        fun `markersFound is initially false`() {
            assertFalse(analyzer.markersFound.value)
        }
    }

    @Nested
    inner class Analyze {
        @Test
        fun `sets markersFound to true when markers detected`() {
            val marker = DetectedMarker(
                id = 0,
                centerX = 100f,
                centerY = 100f,
                corners = listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f),
                confidence = 0.9f
            )
            every { markerDetector.detect(any(), any(), any(), any(), any()) } returns
                MarkerResult(markers = listOf(marker), detectionTimeMs = 5L)

            val image = mockImageProxy()
            analyzer.analyze(image)

            assertTrue(analyzer.markersFound.value)
            verify { image.close() }
        }

        @Test
        fun `markersFound stays false when no markers detected`() {
            every { markerDetector.detect(any(), any(), any(), any(), any()) } returns
                MarkerResult(markers = emptyList(), detectionTimeMs = 5L)

            val image = mockImageProxy()
            analyzer.analyze(image)

            assertFalse(analyzer.markersFound.value)
            verify { image.close() }
        }

        @Test
        fun `always closes image even on detection failure`() {
            every { markerDetector.detect(any(), any(), any(), any(), any()) } throws
                RuntimeException("Detection failed")

            val image = mockImageProxy()
            analyzer.analyze(image)

            assertFalse(analyzer.markersFound.value)
            verify { image.close() }
        }

        @Test
        fun `passes correct parameters to marker detector`() {
            every { markerDetector.detect(any(), any(), any(), any(), any()) } returns
                MarkerResult(markers = emptyList(), detectionTimeMs = 5L)

            val image = mockImageProxy(width = 800, height = 600, rowStride = 832, rotation = 90)
            analyzer.analyze(image)

            verify {
                markerDetector.detect(
                    frameBuffer = any(),
                    width = 800,
                    height = 600,
                    rowStride = 832,
                    rotation = 90
                )
            }
        }

        @Test
        fun `markersFound stays true after subsequent empty detections`() {
            val marker = DetectedMarker(
                id = 0,
                centerX = 100f,
                centerY = 100f,
                corners = listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f),
                confidence = 0.9f
            )
            every { markerDetector.detect(any(), any(), any(), any(), any()) } returns
                MarkerResult(markers = listOf(marker), detectionTimeMs = 5L)

            analyzer.analyze(mockImageProxy())
            assertTrue(analyzer.markersFound.value)

            // Second analysis with no markers
            every { markerDetector.detect(any(), any(), any(), any(), any()) } returns
                MarkerResult(markers = emptyList(), detectionTimeMs = 5L)

            analyzer.analyze(mockImageProxy())
            assertTrue(analyzer.markersFound.value) // stays true
        }
    }
}
