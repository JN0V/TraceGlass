package io.github.jn0v.traceglass.feature.timelapse

import io.github.jn0v.traceglass.core.timelapse.CompilationResult
import io.github.jn0v.traceglass.core.timelapse.TimelapseCompiler
import java.io.File

class FakeTimelapseCompiler : TimelapseCompiler {

    var shouldSucceed: Boolean = true
    var errorMessage: String = "Fake compilation error"
    var compileCalls: Int = 0
        private set
    var lastSnapshotCount: Int = 0
        private set
    var lastFps: Int = 0
        private set
    var progressValues: MutableList<Float> = mutableListOf()
        private set

    override suspend fun compile(
        snapshotFiles: List<File>,
        outputFile: File,
        fps: Int,
        onProgress: (Float) -> Unit
    ): CompilationResult {
        compileCalls++
        lastSnapshotCount = snapshotFiles.size
        lastFps = fps

        if (!shouldSucceed) {
            return CompilationResult.Error(errorMessage)
        }

        snapshotFiles.forEachIndexed { i, _ ->
            val progress = (i + 1).toFloat() / snapshotFiles.size
            progressValues.add(progress)
            onProgress(progress)
        }

        outputFile.writeBytes(byteArrayOf(0x00))
        return CompilationResult.Success(outputFile)
    }
}
