package dev.trail.playground

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
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
    @Test fun curatedRouteOptionsApplyCompatibleStylesAndReturnToCustom() {
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(300)
        fun openOptions() {
            compose.onNodeWithTag("ROUTE ANIMATION").performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.mainClock.advanceTimeBy(300)
        }
        for (preset in dev.trail.effects.TrailRoutePreset.entries) {
            openOptions()
            compose.onAllNodesWithText(preset.label).onLast().performClick()
            compose.mainClock.advanceTimeBy(100)
            compose.onNodeWithText("TrailRoutePreset.${preset.name}", substring = true).assertExists()
            compose.onNodeWithTag("LINE STYLE").assertDoesNotExist()
        }
        openOptions(); compose.onNodeWithText("Custom").performClick(); compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag("LINE STYLE").assertExists()
        compose.onNodeWithTag("ANIMATION").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithText("Dash flow").performClick(); compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag("LINE STYLE").assertTextContains("Dashed")
    }

    @Test fun loadingArcSettlesIntoTheExactRouteAndReducedMotionSkipsTheMorph() {
        compose.mainClock.autoAdvance=false
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithTag("openJourneys").performClick(); compose.mainClock.advanceTimeBy(300)
        fun click(tag: String) { compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick) { it() } }
        click("journey-cab"); compose.mainClock.advanceTimeBy(100)
        click("journeyLoadRoute"); compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag("journeyRoutePhase").assertTextEquals("Finding a route…")
        compose.mainClock.advanceTimeBy(2400)
        compose.onNodeWithTag("journeyRoutePhase").assertTextContains("settling onto the route",substring=true)
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithTag("journeyRoutePhase").assertTextEquals("Route ready")
        compose.onNodeWithTag("journeyGeometry").assertTextContains("119 points",substring=true)
        click("journeyReduced"); compose.mainClock.advanceTimeBy(100)
        click("journeyLoadRoute"); compose.mainClock.advanceTimeBy(2500)
        compose.onNodeWithTag("journeyRoutePhase").assertTextEquals("Route ready")
    }
    @Test fun journeysUseRealCoordinatesAndSwitchBetweenRouteDirectArcAndGreatCircle() {
        compose.mainClock.autoAdvance=false
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithTag("openJourneys").performClick()
        compose.mainClock.advanceTimeBy(300)
        // Freeze the route clock while allowing scrolling/menu animations to settle.
        compose.onNodeWithTag("journeyReduced").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.mainClock.advanceTimeBy(100)
        compose.mainClock.autoAdvance=true
        fun tap(tag: String) {
            compose.onNodeWithTag(tag).performScrollTo().performClick()
            compose.mainClock.advanceTimeBy(300)
        }
        fun choose(mode: String, points: Int) {
            tap("journeyDrawing")
            compose.onAllNodesWithText(mode).onLast().performClick()
            compose.mainClock.advanceTimeBy(300)
            compose.onNodeWithTag("journeyGeometry").assertTextContains("$mode · $points points",substring=true)
        }
        compose.onNodeWithTag("journeyGeometry").assertTextContains("129 points",substring=true)
        tap("journey-cab")
        compose.onNodeWithTag("journeyGeometry").assertTextContains("119 points",substring=true)
        choose("Two points",2); choose("Arc",129); choose("Great circle",129); choose("Full route",119)
        tap("journey-ferry")
        compose.onNodeWithTag("journeyGeometry").assertTextContains("13 points",substring=true)
        choose("Two points",2); choose("Arc",129)
        tap("journeyPlayPause")
        compose.onNodeWithTag("journeyProgress").performScrollTo().performTouchInput { click(center) }
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag("journeyPlayPause").assertTextContains("Play")
        tap("journeyReplay")
        compose.onNodeWithTag("journeyPlayPause").assertTextContains("Pause")
        tap("journeyPlayPause")
        tap("journeyStyle"); compose.onNodeWithText("Dashed").performClick(); compose.mainClock.advanceTimeBy(300)
        tap("journeyMotion"); compose.onNodeWithText("Comet").performClick(); compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithTag("journeyReduced").assertIsOn()
        compose.onNodeWithTag("journeysBack").performScrollTo()
        compose.mainClock.autoAdvance=false
        compose.onNodeWithTag("journeysBack").performClick(); compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag("preview").assertExists()
    }
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
        // ScrollTo waits for an animated scroll. A frozen clock can deadlock on
        // smaller CI displays; an advancing clock cannot idle a repeating effect.
        // Finish the playback assertions first, then make subsequent effects finite.
        compose.onNodeWithTag("Loop animation").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.mainClock.advanceTimeBy(100)
        compose.mainClock.autoAdvance = true
        compose.onNodeWithTag("Loop animation").assertIsOff()
        compose.onNodeWithTag("LINE STYLE").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Glow").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("ANIMATION").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Comet").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Play", useUnmergedTree = true).assertExists()
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
