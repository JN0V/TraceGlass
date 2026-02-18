package io.github.jn0v.traceglass.core.timelapse

import android.net.Uri
import java.io.File

sealed class ExportResult {
    data class Success(val uri: Uri) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

interface VideoExporter {
    suspend fun exportToGallery(videoFile: File, displayName: String): ExportResult
}
