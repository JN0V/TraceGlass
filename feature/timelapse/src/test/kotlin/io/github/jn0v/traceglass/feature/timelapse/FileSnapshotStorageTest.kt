package io.github.jn0v.traceglass.feature.timelapse

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FileSnapshotStorageTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var storage: FileSnapshotStorage

    @BeforeEach
    fun setup() {
        storage = FileSnapshotStorage(tempDir)
    }

    @AfterEach
    fun cleanup() {
        storage.clear()
    }

    @Nested
    inner class SaveSnapshot {
        @Test
        fun `saves JPEG data to numbered file`() {
            val data = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01, 0x02)
            val file = storage.saveSnapshot(data, 0)
            assertTrue(file.exists())
            assertEquals("snapshot_0000.jpg", file.name)
            assertEquals(data.toList(), file.readBytes().toList())
        }

        @Test
        fun `creates timelapse subdirectory`() {
            storage.saveSnapshot(byteArrayOf(1), 0)
            assertTrue(File(tempDir, "timelapse").isDirectory)
        }

        @Test
        fun `saves multiple snapshots with sequential names`() {
            storage.saveSnapshot(byteArrayOf(1), 0)
            storage.saveSnapshot(byteArrayOf(2), 1)
            storage.saveSnapshot(byteArrayOf(3), 2)
            val files = storage.getSnapshotFiles()
            assertEquals(3, files.size)
            assertEquals("snapshot_0000.jpg", files[0].name)
            assertEquals("snapshot_0001.jpg", files[1].name)
            assertEquals("snapshot_0002.jpg", files[2].name)
        }
    }

    @Nested
    inner class GetSnapshots {
        @Test
        fun `returns empty list when no snapshots`() {
            assertEquals(emptyList<File>(), storage.getSnapshotFiles())
        }

        @Test
        fun `returns files sorted by name`() {
            storage.saveSnapshot(byteArrayOf(3), 2)
            storage.saveSnapshot(byteArrayOf(1), 0)
            storage.saveSnapshot(byteArrayOf(2), 1)
            val names = storage.getSnapshotFiles().map { it.name }
            assertEquals(
                listOf("snapshot_0000.jpg", "snapshot_0001.jpg", "snapshot_0002.jpg"),
                names
            )
        }

        @Test
        fun `count matches saved snapshots`() {
            assertEquals(0, storage.getSnapshotCount())
            storage.saveSnapshot(byteArrayOf(1), 0)
            storage.saveSnapshot(byteArrayOf(2), 1)
            assertEquals(2, storage.getSnapshotCount())
        }
    }

    @Nested
    inner class Clear {
        @Test
        fun `removes all snapshots`() {
            storage.saveSnapshot(byteArrayOf(1), 0)
            storage.saveSnapshot(byteArrayOf(2), 1)
            assertEquals(2, storage.getSnapshotCount())
            storage.clear()
            assertEquals(0, storage.getSnapshotCount())
        }
    }
}
