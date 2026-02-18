package io.github.jn0v.traceglass.feature.tracing

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

class ImageFileHelperTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private val sourceUri: Uri = mockk(name = "sourceUri")

    @BeforeEach
    fun setUp() {
        contentResolver = mockk()
        context = mockk {
            every { this@mockk.contentResolver } returns this@ImageFileHelperTest.contentResolver
            every { filesDir } returns tempDir
        }
        every { contentResolver.getType(any()) } returns "image/jpeg"
        mockkStatic(Uri::class)
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Uri::class)
        unmockkStatic(Log::class)
    }

    @Test
    fun `copies image bytes to internal storage file`() {
        val imageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) // PNG magic bytes
        every { contentResolver.openInputStream(sourceUri) } returns ByteArrayInputStream(imageBytes)
        val expectedUri = mockk<Uri>(name = "fileUri")
        every { Uri.fromFile(any()) } returns expectedUri

        val result = ImageFileHelper.copyImageToInternal(context, sourceUri)

        assertEquals(expectedUri, result)
        val outputFile = File(tempDir, "reference_image.jpg")
        assertTrue(outputFile.exists())
        assertEquals(imageBytes.toList(), outputFile.readBytes().toList())
    }

    @Test
    fun `returns null when openInputStream returns null`() {
        every { contentResolver.openInputStream(sourceUri) } returns null

        val result = ImageFileHelper.copyImageToInternal(context, sourceUri)

        assertNull(result)
    }

    @Test
    fun `returns null when openInputStream throws IOException`() {
        every { contentResolver.openInputStream(sourceUri) } throws IOException("permission denied")

        val result = ImageFileHelper.copyImageToInternal(context, sourceUri)

        assertNull(result)
    }

    @Test
    fun `returns null when openInputStream throws SecurityException`() {
        every { contentResolver.openInputStream(sourceUri) } throws SecurityException("no access")

        val result = ImageFileHelper.copyImageToInternal(context, sourceUri)

        assertNull(result)
    }

    @Test
    fun `overwrites existing reference image file`() {
        // Pre-create a file with old content
        val oldFile = File(tempDir, "reference_image.jpg")
        oldFile.writeBytes(byteArrayOf(0x01, 0x02))

        val newBytes = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte())
        every { contentResolver.openInputStream(sourceUri) } returns ByteArrayInputStream(newBytes)
        val expectedUri = mockk<Uri>(name = "fileUri")
        every { Uri.fromFile(any()) } returns expectedUri

        val result = ImageFileHelper.copyImageToInternal(context, sourceUri)

        assertEquals(expectedUri, result)
        assertEquals(newBytes.toList(), oldFile.readBytes().toList())
    }

    @Test
    fun `uses png extension for png mime type`() {
        every { contentResolver.getType(sourceUri) } returns "image/png"
        val imageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        every { contentResolver.openInputStream(sourceUri) } returns ByteArrayInputStream(imageBytes)
        val expectedUri = mockk<Uri>(name = "fileUri")
        every { Uri.fromFile(any()) } returns expectedUri

        ImageFileHelper.copyImageToInternal(context, sourceUri)

        val outputFile = File(tempDir, "reference_image.png")
        assertTrue(outputFile.exists())
    }
}
