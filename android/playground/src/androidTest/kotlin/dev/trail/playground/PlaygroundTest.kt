package dev.trail.playground

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import dev.trail.android.TrailView
import dev.trail.core.*
import kotlin.time.Duration.Companion.seconds
import org.junit.Rule
import org.junit.Test

class PlaygroundTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun nativeViewConfigurationPreservesPositionAndPauseIntent() {
        compose.activityRule.scenario.onActivity { activity ->
            val view = TrailView(activity)
            view.path = TrailPath(listOf(TrailPoint(0.0, 0.0), TrailPoint(100.0, 100.0)))
            view.effect = trailEffect { reveal(2.seconds, repeat = true) }
            view.seek(0.4)
            view.effect = trailEffect { stroke(TrailColor.Coral); reveal(4.seconds) }
            assertEquals(0.4, view.progress, 1e-12)
            assertFalse(view.isPlaying)
            view.replay()
            assertEquals(0.0, view.progress, 1e-12)
            assertTrue(view.isPlaying)
        }
    }
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

    @Test fun compiledExamplesSupportRuntimeColorsPluginsAndPlayback() {
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("openExamples").performClick()
        compose.mainClock.advanceTimeBy(100)
        // Examples are finite; the test clock can run them to completion.
        compose.mainClock.autoAdvance = true
        compose.onNodeWithTag("examplePreview").assertIsDisplayed()

        fun choose(title: String) {
            compose.onNodeWithTag("examplePicker").performClick()
            compose.onNodeWithText(title).performClick()
            compose.onNodeWithTag("examplePreview").assertIsDisplayed()
        }
        fun containsColor(expected: Color): Boolean {
            val pixels = compose.onNodeWithTag("examplePreview").captureToImage().toPixelMap()
            return (0 until pixels.width).any { x ->
                (0 until pixels.height).any { y -> pixels[x, y] == expected }
            }
        }

        choose("Runtime color")
        assertTrue("The initial preset renders blue", containsColor(Color(0xFF2563EB)))
        compose.onNodeWithTag("changeBrandColor").performClick()
        assertTrue("A runtime color change reaches the renderer", containsColor(Color(0xFFFF785A)))
        choose("Custom plugin")
        assertTrue("The external style uses the current brand color", containsColor(Color(0xFFFF785A)))
        compose.onNodeWithTag("changeBrandColor").performClick()
        assertTrue("External styles can be reconfigured", containsColor(Color(0xFF2563EB)))
        choose("Playback controls")
        compose.onNodeWithTag("exampleProgress").performTouchInput { click(center) }
        compose.onNodeWithText("Play", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("exampleReplay").performClick()
        choose("Sequence")
        choose("Layers")
        choose("Android View")
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("examplesBack").performClick()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag("preview").assertIsDisplayed()
    }
}
