package io.github.jn0v.traceglass.feature.tracing.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExpandableMenuTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createTestItems(
        onPickImage: () -> Unit = {},
        onSettings: () -> Unit = {}
    ) = listOf(
        ExpandableMenuItem(
            icon = Icons.Filled.Add,
            label = "Pick reference image",
            onClick = onPickImage
        ),
        ExpandableMenuItem(
            icon = Icons.Filled.Settings,
            label = "Settings",
            onClick = onSettings
        )
    )

    @Test
    fun menuStartsCollapsed() {
        composeTestRule.setContent {
            ExpandableMenu(items = createTestItems())
        }

        // Menu items should not be visible initially
        composeTestRule.onNodeWithContentDescription("Pick reference image")
            .assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Settings")
            .assertDoesNotExist()
        // FAB should be visible
        composeTestRule.onNodeWithContentDescription("Open menu")
            .assertExists()
    }

    @Test
    fun tapFabExpandsMenu() {
        composeTestRule.setContent {
            ExpandableMenu(items = createTestItems())
        }

        composeTestRule.onNodeWithContentDescription("Open menu").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Pick reference image")
            .assertExists()
        composeTestRule.onNodeWithContentDescription("Settings")
            .assertExists()
        composeTestRule.onNodeWithContentDescription("Close menu")
            .assertExists()
    }

    @Test
    fun tapFabAgainCollapsesMenu() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            ExpandableMenu(items = createTestItems())
        }

        composeTestRule.onNodeWithContentDescription("Open menu").performClick()
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.onNodeWithContentDescription("Close menu").performClick()
        // Advance past animation duration
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithContentDescription("Open menu")
            .assertExists()
    }

    @Test
    fun menuItemTriggersCallbackAndCollapses() {
        var pickImageCount by mutableIntStateOf(0)

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            ExpandableMenu(items = createTestItems(onPickImage = { pickImageCount++ }))
        }

        composeTestRule.onNodeWithContentDescription("Open menu").performClick()
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.onNodeWithContentDescription("Pick reference image").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)

        assertEquals(1, pickImageCount)
        // Menu should have collapsed
        composeTestRule.onNodeWithContentDescription("Open menu")
            .assertExists()
    }

    @Test
    fun menuAutoCollapsesAfter5sInactivity() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            ExpandableMenu(items = createTestItems())
        }

        composeTestRule.onNodeWithContentDescription("Open menu").performClick()
        composeTestRule.mainClock.advanceTimeByFrame()

        // Should still be expanded at 4s
        composeTestRule.mainClock.advanceTimeBy(4000)
        composeTestRule.onNodeWithContentDescription("Pick reference image")
            .assertExists()

        // Should collapse after 5s
        composeTestRule.mainClock.advanceTimeBy(1500)

        composeTestRule.onNodeWithContentDescription("Open menu")
            .assertExists()
    }

    @Test
    fun accessibilityMenuDescription() {
        composeTestRule.setContent {
            ExpandableMenu(items = createTestItems())
        }

        composeTestRule.onNodeWithContentDescription("Menu: 2 options")
            .assertExists()
    }
}
