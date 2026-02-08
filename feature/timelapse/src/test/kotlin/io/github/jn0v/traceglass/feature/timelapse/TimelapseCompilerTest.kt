package io.github.jn0v.traceglass.feature.timelapse

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TimelapseCompilerTest {

    @TempDir
    lateinit var tempDir: File

    @Nested
    inner class SuccessfulCompilation {
        @Test
        fun `returns Success with output file`() = runTest {
            val compiler = FakeTimelapseCompiler()
            val snapshots = createFakeSnapshots(3)
            val output = File(tempDir, "output.mp4")

            val result = compiler.compile(snapshots, output)

            assertTrue(result is CompilationResult.Success)
            assertEquals(output, (result as CompilationResult.Success).outputFile)
        }

        @Test
        fun `tracks snapshot count`() = runTest {
            val compiler = FakeTimelapseCompiler()
            val snapshots = createFakeSnapshots(5)

            compiler.compile(snapshots, File(tempDir, "output.mp4"))

            assertEquals(5, compiler.lastSnapshotCount)
        }

        @Test
        fun `reports progress incrementally`() = runTest {
            val compiler = FakeTimelapseCompiler()
            val snapshots = createFakeSnapshots(4)
            val progressUpdates = mutableListOf<Float>()

            compiler.compile(snapshots, File(tempDir, "output.mp4")) { progress ->
                progressUpdates.add(progress)
            }

            assertEquals(4, progressUpdates.size)
            assertEquals(0.25f, progressUpdates[0], 0.01f)
            assertEquals(0.5f, progressUpdates[1], 0.01f)
            assertEquals(0.75f, progressUpdates[2], 0.01f)
            assertEquals(1.0f, progressUpdates[3], 0.01f)
        }

        @Test
        fun `uses provided fps`() = runTest {
            val compiler = FakeTimelapseCompiler()
            val snapshots = createFakeSnapshots(1)

            compiler.compile(snapshots, File(tempDir, "output.mp4"), fps = 15)

            assertEquals(15, compiler.lastFps)
        }
    }

    @Nested
    inner class FailedCompilation {
        @Test
        fun `returns Error with message`() = runTest {
            val compiler = FakeTimelapseCompiler().apply {
                shouldSucceed = false
                errorMessage = "Codec not available"
            }
            val snapshots = createFakeSnapshots(1)

            val result = compiler.compile(snapshots, File(tempDir, "output.mp4"))

            assertTrue(result is CompilationResult.Error)
            assertEquals("Codec not available", (result as CompilationResult.Error).message)
        }
    }

    @Nested
    inner class EmptyInput {
        @Test
        fun `compile with empty list returns Success with empty output`() = runTest {
            val compiler = FakeTimelapseCompiler()
            val output = File(tempDir, "output.mp4")

            val result = compiler.compile(emptyList(), output)

            assertTrue(result is CompilationResult.Success)
            assertEquals(0, compiler.lastSnapshotCount)
        }
    }

    private fun createFakeSnapshots(count: Int): List<File> {
        return (0 until count).map { i ->
            File(tempDir, "snapshot_%04d.jpg".format(i)).apply {
                writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))
            }
        }
    }
}
