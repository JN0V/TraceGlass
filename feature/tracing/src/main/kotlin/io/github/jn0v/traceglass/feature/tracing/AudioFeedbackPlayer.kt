package io.github.jn0v.traceglass.feature.tracing

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.util.Log

class AudioFeedbackPlayer(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null
    @Volatile
    private var isReleased = false
    private var currentRingtone: Ringtone? = null

    fun playBreakReminderTone() {
        if (isReleased) return
        try {
            // Stop any previously playing ringtone to avoid resource leak
            currentRingtone?.stop()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            currentRingtone = ringtone
            ringtone?.play()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play break reminder tone", e)
        }
    }

    fun playTrackingGainedTone() {
        playTone(ToneGenerator.TONE_DTMF_1, 120)
    }

    fun playTrackingLostTone() {
        playTone(ToneGenerator.TONE_DTMF_0, 200)
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        if (isReleased) return
        val gen = toneGenerator ?: try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 80).also { toneGenerator = it }
        } catch (e: RuntimeException) {
            Log.w(TAG, "Failed to create ToneGenerator", e)
            return
        }
        try {
            gen.startTone(toneType, durationMs)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Failed to play tone", e)
        }
    }

    fun release() {
        isReleased = true
        try {
            currentRingtone?.stop()
        } catch (_: RuntimeException) { }
        currentRingtone = null
        try {
            toneGenerator?.release()
        } catch (_: RuntimeException) { }
        toneGenerator = null
    }

    private companion object {
        const val TAG = "AudioFeedbackPlayer"
    }
}
