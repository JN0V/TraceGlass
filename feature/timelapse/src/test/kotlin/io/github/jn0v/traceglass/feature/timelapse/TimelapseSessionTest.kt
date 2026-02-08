package io.github.jn0v.traceglass.feature.timelapse

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimelapseSessionTest {

    @Nested
    inner class InitialState {
        @Test
        fun `starts in IDLE state`() {
            val session = TimelapseSession()
            assertEquals(TimelapseState.IDLE, session.state.value)
        }

        @Test
        fun `initial snapshot count is zero`() {
            val session = TimelapseSession()
            assertEquals(0, session.snapshotCount.value)
        }
    }

    @Nested
    inner class Recording {
        @Test
        fun `start transitions to RECORDING`() = runTest {
            val session = TimelapseSession()
            session.start(this)
            assertEquals(TimelapseState.RECORDING, session.state.value)
            session.stop()
        }

        @Test
        fun `captures first snapshot immediately on start`() = runTest {
            val captured = mutableListOf<Int>()
            val session = TimelapseSession(
                intervalMs = 5000L,
                onCapture = { index -> captured.add(index) }
            )
            session.start(this)
            advanceTimeBy(100)
            assertEquals(listOf(0), captured)
            session.stop()
        }

        @Test
        fun `captures periodically at interval`() = runTest {
            val captured = mutableListOf<Int>()
            val session = TimelapseSession(
                intervalMs = 5000L,
                onCapture = { index -> captured.add(index) }
            )
            session.start(this)
            advanceTimeBy(10100)
            assertEquals(listOf(0, 1, 2), captured)
            session.stop()
        }

        @Test
        fun `snapshot count increments with captures`() = runTest {
            val session = TimelapseSession(intervalMs = 1000L)
            session.start(this)
            advanceTimeBy(3100)
            assertEquals(4, session.snapshotCount.value)
            session.stop()
        }

        @Test
        fun `start when already recording is no-op`() = runTest {
            val session = TimelapseSession()
            session.start(this)
            session.start(this)
            assertEquals(TimelapseState.RECORDING, session.state.value)
            session.stop()
        }
    }

    @Nested
    inner class PauseResume {
        @Test
        fun `pause transitions to PAUSED`() = runTest {
            val session = TimelapseSession()
            session.start(this)
            session.pause()
            assertEquals(TimelapseState.PAUSED, session.state.value)
        }

        @Test
        fun `pause stops capturing`() = runTest {
            val captured = mutableListOf<Int>()
            val session = TimelapseSession(
                intervalMs = 1000L,
                onCapture = { index -> captured.add(index) }
            )
            session.start(this)
            advanceTimeBy(2100)
            val countAtPause = captured.size
            session.pause()
            advanceTimeBy(5000)
            assertEquals(countAtPause, captured.size)
        }

        @Test
        fun `resume transitions back to RECORDING`() = runTest {
            val session = TimelapseSession()
            session.start(this)
            session.pause()
            session.resume(this)
            assertEquals(TimelapseState.RECORDING, session.state.value)
            session.stop()
        }

        @Test
        fun `resume continues counting from pause point`() = runTest {
            val captured = mutableListOf<Int>()
            val session = TimelapseSession(
                intervalMs = 1000L,
                onCapture = { index -> captured.add(index) }
            )
            session.start(this)
            advanceTimeBy(2100)
            session.pause()
            val countAtPause = captured.size
            session.resume(this)
            advanceTimeBy(1100)
            assertEquals(countAtPause + 2, captured.size)
            session.stop()
        }

        @Test
        fun `pause when not recording is no-op`() {
            val session = TimelapseSession()
            session.pause()
            assertEquals(TimelapseState.IDLE, session.state.value)
        }

        @Test
        fun `resume when not paused is no-op`() = runTest {
            val session = TimelapseSession()
            session.resume(this)
            assertEquals(TimelapseState.IDLE, session.state.value)
        }
    }

    @Nested
    inner class Stop {
        @Test
        fun `stop transitions to IDLE`() = runTest {
            val session = TimelapseSession()
            session.start(this)
            session.stop()
            assertEquals(TimelapseState.IDLE, session.state.value)
        }

        @Test
        fun `stop resets snapshot count`() = runTest {
            val session = TimelapseSession(intervalMs = 1000L)
            session.start(this)
            advanceTimeBy(3100)
            session.stop()
            assertEquals(0, session.snapshotCount.value)
        }

        @Test
        fun `stop from paused transitions to IDLE`() = runTest {
            val session = TimelapseSession()
            session.start(this)
            session.pause()
            session.stop()
            assertEquals(TimelapseState.IDLE, session.state.value)
        }
    }
}
