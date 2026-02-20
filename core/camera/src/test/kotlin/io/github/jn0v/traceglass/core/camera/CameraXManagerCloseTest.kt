package io.github.jn0v.traceglass.core.camera

import android.content.Context
import io.github.jn0v.traceglass.core.camera.impl.CameraXManager
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutorService

class CameraXManagerCloseTest {

    private val context = mockk<Context>(relaxed = true)

    @Test
    fun `close() shuts down analysis executor`() {
        val manager = CameraXManager(context)

        manager.close()

        val field = CameraXManager::class.java.getDeclaredField("analysisExecutor")
        field.isAccessible = true
        val executor = field.get(manager) as ExecutorService
        assertTrue(executor.isShutdown)
    }

    @Test
    fun `close() transitions isCameraReady to false`() {
        val manager = CameraXManager(context)

        manager.close()

        assertFalse(manager.isCameraReady.value)
    }

    @Test
    fun `unbind() does not shut down executor`() {
        val manager = CameraXManager(context)

        manager.unbind()

        val field = CameraXManager::class.java.getDeclaredField("analysisExecutor")
        field.isAccessible = true
        val executor = field.get(manager) as ExecutorService
        assertFalse(executor.isShutdown)
    }

    @Test
    fun `close() is idempotent`() {
        val manager = CameraXManager(context)

        assertDoesNotThrow {
            manager.close()
            manager.close()
        }
    }
}
