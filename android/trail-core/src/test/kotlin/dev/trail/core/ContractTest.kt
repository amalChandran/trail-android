package dev.trail.core

import com.google.gson.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

data class ContractCase(val family: String, val json: JsonObject) {
    override fun toString(): String = json["id"].asString
}

/** Each fixture is reported separately, with the same ID in the Swift suite. */
@RunWith(Parameterized::class)
class ContractTest(private val case: ContractCase) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases(): Collection<Array<Any>> {
            val stream = checkNotNull(ContractTest::class.java.getResourceAsStream("/contracts.json"))
            val root = stream.bufferedReader().use { JsonParser.parseReader(it).asJsonObject }
            return root.entrySet().filter { it.key != "schema" }.flatMap { (family, values) ->
                values.asJsonArray.map { arrayOf<Any>(ContractCase(family, it.asJsonObject)) }
            }
        }
    }
    private val c get() = case.json
    private fun coordinates(key: String) = c[key].asJsonArray.map { it.asJsonArray.let { p -> TrailCoordinate(p[0].asDouble,p[1].asDouble) } }
    private fun coordinate(key: String) = c[key].asJsonArray.let { TrailCoordinate(it[0].asDouble,it[1].asDouble) }
    private fun path() = TrailPath(c["points"].asJsonArray.map { it.asJsonArray.let { p -> TrailPoint(p[0].asDouble,p[1].asDouble) } }, c["breaks"].asJsonArray.map { it.asInt }.toSet())
    private fun close(expected: Double, actual: Double, tolerance: Double = 1e-9) = assertEquals(expected,actual,tolerance,case.toString())

    @Test fun contract() {
        when (case.family) {
            "poses" -> {
                val path=path(); val fraction=c["fraction"].asDouble
                val direction=if(c["reverse"].asBoolean) TrailDirection.Reverse else TrailDirection.Forward
                val pose=path.poseAt(fraction,c["window"].asDouble,direction)
                if(c["expected"].isJsonNull) assertNull(pose) else {
                    assertNotNull(pose); val expected=c["expected"].asJsonArray
                    close(expected[0].asDouble,pose.point.x); close(expected[1].asDouble,pose.point.y)
                    close(cos(c["heading"].asDouble),cos(pose.headingRadians))
                    close(sin(c["heading"].asDouble),sin(pose.headingRadians))
                    assertEquals(c["contour"].asInt,pose.contourIndex)
                    assertEquals(path.pointAt(fraction),pose.point)
                    path.poseAt(1-fraction) // Scrubbing in another order cannot change a pose.
                    assertEquals(pose,path.poseAt(fraction,c["window"].asDouble,direction))
                }
            }
            "pathSamples" -> {
                val path = path(); val fraction = c["fraction"].asDouble
                close(c["length"].asDouble,path.length)
                val actual = path.pointAt(fraction)
                if (c["expected"].isJsonNull) assertNull(actual) else {
                    assertNotNull(actual); val expected = c["expected"].asJsonArray
                    close(expected[0].asDouble,actual.x); close(expected[1].asDouble,actual.y)
                }
                close(c["tangent"].asDouble,path.tangentAt(fraction))
            }
            "pathSlices" -> {
                val path=path(); val slices=path.slices(c["start"].asDouble,c["end"].asDouble)
                close(c["length"].asDouble,slices.sumOf { TrailPath(it.points).length })
                assertTrue(slices.all { it.points.size >= 2 && it.distanceFromStart >= 0 })
                assertTrue(slices.zipWithNext().all { (a,b) -> a.distanceFromStart <= b.distanceFromStart })
                // Every drawn segment must stay inside one original continuous contour.
                val maxEdge=path.points.zipWithNext().mapIndexed { i,(a,b) -> if (i+1 in path.breakBefore) 0.0 else hypot(b.x-a.x,b.y-a.y) }.maxOrNull() ?: 0.0
                assertTrue(slices.all { section -> section.points.zipWithNext().all { (a,b) -> hypot(b.x-a.x,b.y-a.y) <= maxEdge+1e-9 } })
            }
            "routes" -> {
                val a=coordinate("fromPoint"); val b=coordinate("toPoint")
                fun build() = when (c["kind"].asString) {
                    "arc" -> TrailRoute.arc("fixture",a,b,c["bend"].asDouble,c["steps"].asInt)
                    "greatCircle" -> TrailRoute.greatCircle("fixture",a,b,c["steps"].asInt)
                    else -> TrailRoute.direct("fixture",a,b)
                }
                if (c["invalid"].asBoolean) { assertFailsWith<IllegalArgumentException> { build() }; return }
                val route=build()
                assertEquals(a,route.coordinates.first()); assertEquals(b,route.coordinates.last())
                assertTrue(route.coordinates.all { it.latitude.isFinite() && it.longitude.isFinite() })
                assertTrue(route.segments.all { segment -> segment.zipWithNext().all { (p,q) -> abs(p.longitude-q.longitude) <= 180+1e-9 } })
                val shortest=TrailRoute.direct("direct",a,b).distanceMeters
                assertTrue(route.distanceMeters >= shortest-max(.05,shortest*1e-8))
                if (c["kind"].asString == "greatCircle") close(shortest,route.distanceMeters,max(.05,shortest*1e-8))
                assertNotNull(route.bounds)
                assertEquals(route.coordinates,build().coordinates)
            }
            "bounds" -> {
                val bounds=TrailRoute("fixture",coordinates("coordinates")).bounds
                if (c["center"].isJsonNull) assertNull(bounds) else {
                    assertNotNull(bounds); val expected=coordinate("center")
                    close(expected.latitude,bounds.center.latitude)
                    val longitudeError=abs(expected.longitude-bounds.center.longitude)
                    assertTrue(min(longitudeError,abs(360-longitudeError))<1e-9)
                    close(c["latitudeSpan"].asDouble,bounds.latitudeSpan); close(c["longitudeSpan"].asDouble,bounds.longitudeSpan)
                }
            }
            "polylines" -> {
                fun decode()=TrailRoute.encodedPolyline("fixture",c["encoded"].asString,c["precision"].asInt)
                if (c["invalid"].asBoolean) assertFailsWith<IllegalArgumentException> { decode() }
                else assertEquals(coordinates("coordinates"),decode().coordinates)
            }
            "playback" -> {
                val player=TrailPlayer(trailEffect { reveal(c["duration"].asDouble.seconds,c["repeats"].asBoolean) })
                for (raw in c["operations"].asJsonArray) {
                    val op=raw.asJsonObject; val value=op["value"].asDouble
                    when (op["op"].asString) { "advance" -> player.advance(value); "pause" -> player.pause(); "seek" -> player.seek(value); "replay" -> player.replay() }
                }
                close(c["progress"].asDouble,player.progress)
                assertEquals(c["status"].asString,player.status.name.lowercase())
                assertTrue(player.frame(0).windows.all { it.start <= it.end })
            }
            "projections" -> {
                val route=TrailRoute("fixture",coordinates("coordinates")); val ready=c["ready"].asBoolean
                val density=c["density"].asDouble; val angle=c["bearing"].asDouble*PI/180
                var calls=0
                val projected=route.projectIfReady(TrailProjection { coordinate ->
                    calls++
                    if (!ready && calls == 2) null else {
                        val x=coordinate.longitude; val y=coordinate.latitude
                        // SDK pixels converted exactly once to renderer logical units.
                        TrailPoint((cos(angle)*x-sin(angle)*y)*density/density,(sin(angle)*x+cos(angle)*y)*density/density)
                    }
                })
                if (!ready && route.coordinates.isNotEmpty()) assertNull(projected) else {
                    assertNotNull(projected)
                    val reference=route.project { TrailPoint(it.longitude,it.latitude) }
                    close(reference.length,projected.length,1e-8)
                    assertEquals(reference.breakBefore,projected.breakBefore)
                    assertEquals(reference.points.size,projected.points.size)
                    // Changing the projection does not own or mutate playback.
                    val player=TrailPlayer(trailEffect { reveal() }); player.seek(.37)
                    route.projectIfReady(TrailProjection { TrailPoint(it.longitude*2,it.latitude*2) })
                    close(.37,player.progress); assertEquals(TrailPlaybackStatus.Paused,player.status)
                }
            }
            else -> fail("Unknown contract family ${case.family}")
        }
    }
}
