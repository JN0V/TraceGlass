package io.github.jn0v.traceglass.feature.timelapse

import java.io.File

class FileSnapshotStorage(
    private val baseDir: File
) : SnapshotStorage {

    private val timelapseDir: File
        get() = File(baseDir, "timelapse").also { it.mkdirs() }

    override fun saveSnapshot(jpegData: ByteArray, index: Int): File {
        val file = File(timelapseDir, "snapshot_%04d.jpg".format(index))
        file.writeBytes(jpegData)
        return file
    }

    override fun getSnapshotFiles(): List<File> {
        return timelapseDir
            .listFiles { f -> f.extension == "jpg" }
            ?.sorted()
            ?: emptyList()
    }

    override fun getSnapshotCount(): Int = getSnapshotFiles().size

    override fun clear() {
        timelapseDir.listFiles()?.forEach { it.delete() }
    }
}
