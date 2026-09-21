package dev.trail.plugin

import dev.trail.core.*
import kotlin.test.*

class ExternalPluginTest {
    @Test fun pluginWorksThroughPublicApiWithoutFriendAccess() {
        val effect = trailEffect { metro() }
        assertEquals(3, effect.layers.single().commands.size)
        assertEquals(listOf(0.0, 0.0625, 0.25, 0.5625, 1.0), listOf(0.0, 0.25, 0.5, 0.75, 1.0).map {
            QuadraticReveal().sample(TrailTime(it)).head
        })
    }
}
