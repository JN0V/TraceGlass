package io.github.jn0v.traceglass.feature.tracing

import android.content.Context
import io.mockk.mockk
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AudioFeedbackPlayer].
 *
 * On JVM (no Android runtime), ToneGenerator/RingtoneManager constructors throw
 * RuntimeException("Stub!"). These tests verify that the player's defensive
 * try-catch logic handles this gracefully without crashing.
 */
class AudioFeedbackPlayerTest {

    private val context: Context = mockk(relaxed = true)

    @Test
    fun `playTrackingGainedTone does not crash when ToneGenerator is unavailable`() {
        val player = AudioFeedbackPlayer(context)
        player.playTrackingGainedTone()
    }

    @Test
    fun `playTrackingLostTone does not crash when ToneGenerator is unavailable`() {
        val player = AudioFeedbackPlayer(context)
        player.playTrackingLostTone()
    }

    @Test
    fun `playBreakReminderTone does not crash when RingtoneManager is unavailable`() {
        val player = AudioFeedbackPlayer(context)
        player.playBreakReminderTone()
    }

    @Test
    fun `release is idempotent`() {
        val player = AudioFeedbackPlayer(context)
        player.release()
        player.release()
    }

    @Test
    fun `play after release does not crash`() {
        val player = AudioFeedbackPlayer(context)
        player.release()
        player.playTrackingGainedTone()
        player.playTrackingLostTone()
        player.playBreakReminderTone()
    }
}
