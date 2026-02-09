package io.github.jn0v.traceglass.feature.tracing

import android.content.Context
import android.media.RingtoneManager

class AudioFeedbackPlayer(private val context: Context) {

    fun playBreakReminderTone() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone?.play()
    }
}
