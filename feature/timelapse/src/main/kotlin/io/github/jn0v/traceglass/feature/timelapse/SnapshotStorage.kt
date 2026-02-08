package io.github.jn0v.traceglass.feature.timelapse

import java.io.File

interface SnapshotStorage {
    fun saveSnapshot(jpegData: ByteArray, index: Int): File
    fun getSnapshotFiles(): List<File>
    fun getSnapshotCount(): Int
    fun clear()
}
