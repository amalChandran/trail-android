import Foundation
import Testing
import TrailCore

private protocol FixtureCase: Decodable, Sendable, CustomStringConvertible { var id: String { get } }
private extension FixtureCase { var description: String { id } }
private struct PathSample: FixtureCase {
    let id: String, points: [[Double]], breaks: [Int], fraction: Double, expected: [Double]?, tangent: Double, length: Double
}
private struct PathSlice: FixtureCase {
    let id: String, points: [[Double]], breaks: [Int], start: Double, end: Double, length: Double
}
private struct RouteCase: FixtureCase {
    let id: String, kind: String, fromPoint: [Double], toPoint: [Double], steps: Int, bend: Double, invalid: Bool
}
private struct BoundsCase: FixtureCase {
    let id: String, coordinates: [[Double]], center: [Double]?, latitudeSpan: Double, longitudeSpan: Double
}
private struct PolylineCase: FixtureCase {
    let id: String, encoded: String, precision: Int, coordinates: [[Double]], invalid: Bool
}
private struct PlaybackCase: FixtureCase {
    struct Operation: Decodable, Sendable { let op: String, value: Double }
    let id: String, duration: Double, repeats: Bool, operations: [Operation], progress: Double, status: String
}
private struct ProjectionCase: FixtureCase {
    let id: String, coordinates: [[Double]], bearing: Double, density: Double, ready: Bool
}
private struct PoseCase: FixtureCase {
    let id: String, points: [[Double]], breaks: [Int], fraction: Double, expected: [Double]?
    let heading: Double, reverse: Bool, contour: Int, window: Double
}
private struct Contracts: Decodable, Sendable {
    let pathSamples: [PathSample], pathSlices: [PathSlice], routes: [RouteCase], bounds: [BoundsCase]
    let polylines: [PolylineCase], playback: [PlaybackCase], projections: [ProjectionCase]
    let poses: [PoseCase]
    static let shared: Contracts = {
        let url = Bundle.module.url(forResource: "contracts", withExtension: "json", subdirectory: "Fixtures")!
        return try! JSONDecoder().decode(Self.self, from: Data(contentsOf: url))
    }()
}
private func coordinate(_ p: [Double]) throws -> TrailCoordinate { try TrailCoordinate(latitude: p[0], longitude: p[1]) }
private func close(_ a: Double, _ b: Double, tolerance: Double = 1e-9) -> Bool { abs(a-b) <= tolerance }
private func path(_ points: [[Double]], _ breaks: [Int]) -> TrailPath {
    TrailPath(points.map { TrailPoint($0[0],$0[1]) }, breakBefore: Set(breaks))
}

@Test(arguments: Contracts.shared.pathSamples)
private func sampledPathMatchesIndependentLinearReference(_ c: PathSample) {
    let path = path(c.points,c.breaks), actual = path.point(at: c.fraction)
    #expect(close(c.length,path.length))
    if let expected = c.expected, let actual {
        #expect(close(expected[0],actual.x)); #expect(close(expected[1],actual.y))
    } else { #expect(c.expected == nil && actual == nil) }
    #expect(close(c.tangent,path.tangent(at: c.fraction)))
}

@Test(arguments: Contracts.shared.pathSlices)
private func clippingPreservesDistanceAndNeverBridgesContours(_ c: PathSlice) {
    let path = path(c.points,c.breaks), sections = path.slices(from: c.start,to: c.end)
    #expect(close(c.length,sections.reduce(0) { $0+TrailPath($1.points).length }))
    #expect(sections.allSatisfy { $0.points.count >= 2 && $0.distanceFromStart >= 0 })
    #expect(zip(sections,sections.dropFirst()).allSatisfy { $0.distanceFromStart <= $1.distanceFromStart })
    let maxEdge = zip(path.points,path.points.dropFirst()).enumerated().map { i,pair in
        c.breaks.contains(i+1) ? 0 : hypot(pair.1.x-pair.0.x,pair.1.y-pair.0.y)
    }.max() ?? 0
    #expect(sections.allSatisfy { section in
        zip(section.points,section.points.dropFirst()).allSatisfy { hypot($1.x-$0.x,$1.y-$0.y) <= maxEdge+1e-9 }
    })
}

@Test(arguments: Contracts.shared.routes)
private func geographicConnectionsKeepEndpointsAndShortestPathContract(_ c: RouteCase) throws {
    let a = try coordinate(c.fromPoint), b = try coordinate(c.toPoint)
    func build() throws -> TrailRoute {
        switch c.kind {
        case "arc": try TrailRoute.arc(id: "fixture",from: a,to: b,bend: c.bend,steps: c.steps)
        case "greatCircle": try TrailRoute.greatCircle(id: "fixture",from: a,to: b,steps: c.steps)
        default: try TrailRoute.direct(id: "fixture",from: a,to: b)
        }
    }
    if c.invalid { #expect(throws: TrailError.self) { try build() }; return }
    let route = try build()
    #expect(route.coordinates.first == a); #expect(route.coordinates.last == b)
    #expect(route.coordinates.allSatisfy { $0.latitude.isFinite && $0.longitude.isFinite })
    #expect(route.segments.allSatisfy { zip($0,$0.dropFirst()).allSatisfy { abs($1.longitude-$0.longitude) <= 180+1e-9 } })
    let shortest = try TrailRoute.direct(id: "direct",from: a,to: b).distanceMeters
    #expect(route.distanceMeters >= shortest-max(0.05,shortest*1e-8))
    if c.kind == "greatCircle" { #expect(close(shortest,route.distanceMeters,tolerance: max(0.05,shortest*1e-8))) }
    #expect(route.bounds != nil)
    #expect(route.coordinates == (try build()).coordinates)
}

@Test(arguments: Contracts.shared.bounds)
private func boundsChooseTheSmallestLongitudeInterval(_ c: BoundsCase) throws {
    let bounds = try TrailRoute(id: "fixture",coordinates: c.coordinates.map(coordinate)).bounds
    if let center = c.center, let bounds {
        #expect(close(center[0],bounds.center.latitude))
        let longitudeError = abs(center[1]-bounds.center.longitude)
        #expect(min(longitudeError,abs(360-longitudeError)) < 1e-9)
        #expect(close(c.latitudeSpan,bounds.latitudeSpan)); #expect(close(c.longitudeSpan,bounds.longitudeSpan))
    } else { #expect(c.center == nil && bounds == nil) }
}

@Test(arguments: Contracts.shared.polylines)
private func polylineInputPreservesCoordinatesOrRejectsMalformedData(_ c: PolylineCase) throws {
    func decode() throws -> TrailRoute { try TrailRoute.encodedPolyline(id: "fixture",encoded: c.encoded,precision: c.precision) }
    if c.invalid { #expect(throws: TrailError.self) { try decode() } }
    else { #expect(try decode().coordinates == c.coordinates.map(coordinate)) }
}

@Test(arguments: Contracts.shared.playback)
private func playbackMatchesTheSharedCommandContract(_ c: PlaybackCase) {
    var player = TrailPlayer(effect: TrailEffect { Reveal(duration: .seconds(c.duration),repeats: c.repeats) })
    for op in c.operations {
        switch op.op {
        case "advance": player.advance(by: op.value)
        case "pause": player.pause()
        case "seek": player.seek(to: op.value)
        case "replay": player.replay()
        default: Issue.record("Unknown fixture command \(op.op)")
        }
    }
    #expect(close(c.progress,player.progress))
    #expect(String(describing: player.status) == c.status)
    #expect(player.frame(layer: 0).windows.allSatisfy { $0.start <= $0.end })
}

@Test(arguments: Contracts.shared.projections)
private func providersOnlyProjectAndNeverOwnPlayback(_ c: ProjectionCase) throws {
    let route = try TrailRoute(id: "fixture",coordinates: c.coordinates.map(coordinate))
    let angle = c.bearing * .pi/180; var calls = 0
    let projected = route.projectIfReady(TrailProjection { coordinate in
        calls += 1
        if !c.ready && calls == 2 { return nil }
        let x = coordinate.longitude, y = coordinate.latitude
        return TrailPoint((cos(angle)*x-sin(angle)*y)*c.density/c.density,
                          (sin(angle)*x+cos(angle)*y)*c.density/c.density)
    })
    if !c.ready && !route.coordinates.isEmpty { #expect(projected == nil) }
    else {
        let projected = try #require(projected)
        let reference = route.project { TrailPoint($0.longitude,$0.latitude) }
        #expect(close(reference.length,projected.length,tolerance: 1e-8))
        #expect(reference.breakBefore == projected.breakBefore); #expect(reference.points.count == projected.points.count)
        var player = TrailPlayer(effect: TrailEffect { Reveal() }); player.seek(to: 0.37)
        _ = route.projectIfReady(TrailProjection { TrailPoint($0.longitude*2,$0.latitude*2) })
        #expect(close(0.37,player.progress)); #expect(player.status == .paused)
    }
}

@Test(arguments: Contracts.shared.poses)
private func vehiclePosesMatchAnalyticHeadingsAndNeverCutCorners(_ c: PoseCase) throws {
    let path=path(c.points,c.breaks), direction: TrailDirection = c.reverse ? .reverse : .forward
    let pose=path.pose(at: c.fraction,headingWindow: c.window,direction: direction)
    if let expected=c.expected {
        let pose=try #require(pose)
        #expect(close(expected[0],pose.point.x)); #expect(close(expected[1],pose.point.y))
        #expect(close(cos(c.heading),cos(pose.headingRadians))); #expect(close(sin(c.heading),sin(pose.headingRadians)))
        #expect(pose.contourIndex == c.contour); #expect(pose.point == path.point(at: c.fraction))
        _ = path.pose(at: 1-c.fraction)
        #expect(pose == path.pose(at: c.fraction,headingWindow: c.window,direction: direction))
    } else { #expect(pose == nil) }
}
