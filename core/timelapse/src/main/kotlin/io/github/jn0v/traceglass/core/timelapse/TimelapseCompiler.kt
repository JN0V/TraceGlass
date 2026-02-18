package io.github.jn0v.traceglass.core.timelapse

import java.io.File

sealed class CompilationResult {
    data class Success(val outputFile: File) : CompilationResult()
    data class Error(val message: String) : CompilationResult()
}

interface TimelapseCompiler {
    suspend fun compile(
        snapshotFiles: List<File>,
        outputFile: File,
        fps: Int = 10,
        onProgress: (Float) -> Unit = {}
    ): CompilationResult
}
