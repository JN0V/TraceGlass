package io.github.jn0v.traceglass.feature.tracing

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File

object ImageFileHelper {
    private const val TAG = "ImageFileHelper"

    fun copyImageToInternal(context: Context, sourceUri: Uri): Uri? {
        return try {
            val stream = context.contentResolver.openInputStream(sourceUri)
            if (stream == null) {
                Log.w(TAG, "openInputStream returned null for $sourceUri")
                return null
            }
            stream.use { input ->
                val mimeType = context.contentResolver.getType(sourceUri)
                val extension = when {
                    mimeType?.contains("png") == true -> ".png"
                    mimeType?.contains("webp") == true -> ".webp"
                    else -> ".jpg"
                }
                val file = File(context.filesDir, "reference_image$extension")
                val tempFile = File(context.filesDir, "reference_image_tmp$extension")
                tempFile.outputStream().use { output -> input.copyTo(output) }
                // Rename first (atomic on same filesystem), then clean up stale files
                if (!tempFile.renameTo(file)) {
                    tempFile.delete()
                    return@use null
                }
                context.filesDir.listFiles { f ->
                    f.name.startsWith("reference_image") && f != file
                }?.forEach { it.delete() }
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy image to internal storage", e)
            null
        }
    }
}
