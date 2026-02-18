package io.github.jn0v.traceglass.core.timelapse

import android.net.Uri

interface VideoSharer {
    fun shareVideo(videoUri: Uri)
}
