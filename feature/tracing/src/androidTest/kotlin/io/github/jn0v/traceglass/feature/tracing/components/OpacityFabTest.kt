package io.github.jn0v.traceglass.feature.tracing.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OpacityFabTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sliderOpensOnFabTap() {
        var toggleCount by mutableIntStateOf(0)

        composeTestRule.setContent {
            OpacityFab(
                opacity = 0.5f,
                isSliderVisible = false,
                onToggleSlider = { toggleCount++ },
                onOpacityChanged = {}
            )
        }

        composeTestRule.onNodeWithText("50%").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, toggleCount)
    }

    @Test
    fun sliderAutoCollapsesAfter3sIdle() {
        var collapseCount by mutableIntStateOf(0)

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            OpacityFab(
                opacity = 0.5f,
                isSliderVisible = true,
                onToggleSlider = { collapseCount++ },
                onOpacityChanged = {}
            )
        }

        // Process initial composition
        composeTestRule.mainClock.advanceTimeByFrame()
        assertEquals(0, collapseCount)

        // Advance past 3s delay
        composeTestRule.mainClock.advanceTimeBy(3100)

        assertEquals(1, collapseCount)
    }

    @Test
    fun sliderStaysOpenDuringContinuousInteraction() {
        var opacity by mutableFloatStateOf(0.5f)
        var collapseCount by mutableIntStateOf(0)

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            OpacityFab(
                opacity = opacity,
                isSliderVisible = true,
                onToggleSlider = { collapseCount++ },
                onOpacityChanged = { opacity = it }
            )
        }

        composeTestRule.mainClock.advanceTimeByFrame()

        // Simulate continuous interaction: change opacity every 2s for 8s total
        repeat(4) {
            composeTestRule.mainClock.advanceTimeBy(2000)
            opacity += 0.05f
            // Process recomposition so LaunchedEffect restarts
            composeTestRule.mainClock.advanceTimeByFrame()
        }

        // Should not have collapsed (each change resets the 3s timer)
        assertEquals(0, collapseCount)
    }

    @Test
    fun sliderCollapses3sAfterLastInteraction() {
        var opacity by mutableFloatStateOf(0.5f)
        var collapseCount by mutableIntStateOf(0)

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            OpacityFab(
                opacity = opacity,
                isSliderVisible = true,
                onToggleSlider = { collapseCount++ },
                onOpacityChanged = { opacity = it }
            )
        }

        composeTestRule.mainClock.advanceTimeByFrame()

        // Change opacity at t=0 (restarts timer)
        opacity = 0.7f
        composeTestRule.mainClock.advanceTimeByFrame()

        // Advance 2s — timer still running (2s of 3s elapsed)
        composeTestRule.mainClock.advanceTimeBy(2000)
        assertEquals(0, collapseCount)

        // Advance 1.5s more — past 3s since last interaction
        composeTestRule.mainClock.advanceTimeBy(1500)

        assertEquals(1, collapseCount)
    }
}
