package io.github.jn0v.traceglass.feature.timelapse

import io.github.jn0v.traceglass.core.timelapse.ExportResult
import io.github.jn0v.traceglass.core.timelapse.VideoExporter
import android.net.Uri
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class VideoExporterTest {

    @TempDir
    lateinit var tempDir: File

    @Nested
    inner class FakeExporter {

        @Test
        fun `successful export returns Success with URI`() = runTest {
            val fakeUri = mockk<Uri>()
            val exporter = object : VideoExporter {
                override suspend fun exportToGallery(videoFile: File, displayName: String): ExportResult {
                    return ExportResult.Success(fakeUri)
                }
            }

            val videoFile = File(tempDir, "test.mp4").apply { writeBytes(byteArrayOf(0x00)) }
            val result = exporter.exportToGallery(videoFile, "timelapse_test")

            assertTrue(result is ExportResult.Success)
            assertEquals(fakeUri, (result as ExportResult.Success).uri)
        }

        @Test
        fun `failed export returns Error with message`() = runTest {
            val exporter = object : VideoExporter {
                override suspend fun exportToGallery(videoFile: File, displayName: String): ExportResult {
                    return ExportResult.Error("MediaStore unavailable")
                }
            }

            val videoFile = File(tempDir, "test.mp4").apply { writeBytes(byteArrayOf(0x00)) }
            val result = exporter.exportToGallery(videoFile, "timelapse_test")

            assertTrue(result is ExportResult.Error)
            assertEquals("MediaStore unavailable", (result as ExportResult.Error).message)
        }

        @Test
        fun `export passes correct display name`() = runTest {
            var capturedName = ""
            val exporter = object : VideoExporter {
                override suspend fun exportToGallery(videoFile: File, displayName: String): ExportResult {
                    capturedName = displayName
                    return ExportResult.Success(mockk())
                }
            }

            val videoFile = File(tempDir, "test.mp4").apply { writeBytes(byteArrayOf(0x00)) }
            exporter.exportToGallery(videoFile, "my_timelapse_2026")

            assertEquals("my_timelapse_2026", capturedName)
        }
    }
}
