package dev.trail.playground

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class PlaygroundTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun playbackPresetAndPluginFlow() {
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("preview").assertIsDisplayed()
        compose.onNodeWithTag("playPause").performClick()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("Play", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("progress").performTouchInput { click(center) }
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag("replay").performClick()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("Pause", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("LINE STYLE").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Glow").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("ANIMATION").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Comet").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("playPause").performClick()
        compose.mainClock.advanceTimeBy(100)
        // Scroll actions animate. Let their clock advance after route playback has paused.
        compose.mainClock.autoAdvance = true
        compose.onNodeWithTag("Reduced motion").performScrollTo().performClick()
        compose.onNodeWithTag("Reduced motion").assertIsOn()
        compose.onNodeWithTag("Use my Metro plugin").performScrollTo().performClick()
        compose.onNodeWithTag("Use my Metro plugin").assertIsOn()
    }
}
