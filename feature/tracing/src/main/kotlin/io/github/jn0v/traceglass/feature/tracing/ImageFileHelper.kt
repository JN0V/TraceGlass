package io.github.jn0v.traceglass.feature.tracing

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File

object ImageFileHelper {
    private const val TAG = "ImageFileHelper"

    fun copyImageToInternal(context: Context, sourceUri: Uri): Uri? {
        return try {
            val input = context.contentResolver.openInputStream(sourceUri) ?: return null
            val mimeType = context.contentResolver.getType(sourceUri)
            val extension = when {
                mimeType?.contains("png") == true -> ".png"
                mimeType?.contains("webp") == true -> ".webp"
                else -> ".jpg"
            }
            // Delete any previous reference images to avoid stale files
            context.filesDir.listFiles { f -> f.name.startsWith("reference_image") }
                ?.forEach { it.delete() }
            val file = File(context.filesDir, "reference_image$extension")
            file.outputStream().use { output -> input.use { it.copyTo(output) } }
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy image to internal storage", e)
            null
        }
    }
}
