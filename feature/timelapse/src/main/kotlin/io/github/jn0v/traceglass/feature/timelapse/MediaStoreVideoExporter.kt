package io.github.jn0v.traceglass.feature.timelapse

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreVideoExporter(
    private val context: Context
) : VideoExporter {

    override suspend fun exportToGallery(
        videoFile: File,
        displayName: String
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/TraceGlass")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext ExportResult.Error("Failed to create MediaStore entry")

            resolver.openOutputStream(uri)?.use { outputStream ->
                videoFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext ExportResult.Error("Failed to open output stream")

            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            ExportResult.Success(uri)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Unknown export error")
        }
    }
}
