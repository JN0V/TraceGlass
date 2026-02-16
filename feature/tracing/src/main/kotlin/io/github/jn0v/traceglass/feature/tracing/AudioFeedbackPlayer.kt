package io.github.jn0v.traceglass.feature.tracing

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

class AudioFeedbackPlayer(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())

    fun playBreakReminderTone() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone?.play()
    }

    fun playTrackingGainedTone() {
        playTone(ToneGenerator.TONE_DTMF_1, 120)
    }

    fun playTrackingLostTone() {
        playTone(ToneGenerator.TONE_DTMF_0, 200)
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        toneGen.startTone(toneType, durationMs)
        handler.postDelayed({ toneGen.release() }, durationMs + 100L)
    }
}
