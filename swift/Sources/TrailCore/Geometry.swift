import Foundation

public enum TrailError: Error, Equatable { case invalidRoute(String) }

public struct TrailPoint: Sendable, Equatable {
    public let x: Double
    public let y: Double
    public init(_ x: Double, _ y: Double) {
        precondition(x.isFinite && y.isFinite, "Point coordinates must be finite")
        self.x = x; self.y = y
    }
}

public struct TrailCoordinate: Sendable, Equatable {
    public let latitude: Double
    public let longitude: Double
    public init(latitude: Double, longitude: Double) throws {
        guard latitude.isFinite, (-90...90).contains(latitude), longitude.isFinite, (-180...180).contains(longitude) else {
            throw TrailError.invalidRoute("Coordinates must be finite; latitude [-90,90], longitude [-180,180]")
        }
        self.latitude = latitude; self.longitude = longitude
    }
}

public struct TrailRoute: Sendable {
    public let id: String
    public let revision: Int
    public let coordinates: [TrailCoordinate]
    public let segments: [[TrailCoordinate]]
    public let distanceMeters: Double
    public let bounds: TrailGeoBounds?
    public var key: TrailRouteKey { TrailRouteKey(id: id, revision: revision) }
    public init(id: String, coordinates: [TrailCoordinate], revision: Int = 0) throws {
        guard !id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { throw TrailError.invalidRoute("ID must not be blank") }
        guard revision >= 0 else { throw TrailError.invalidRoute("Revision must not be negative") }
        guard coordinates.count <= 100_000 else { throw TrailError.invalidRoute("A route supports at most 100,000 coordinates") }
        self.id = id; self.coordinates = coordinates; self.revision = revision
        self.segments = TrailGeography.splitAtDateLine(coordinates)
        self.distanceMeters = zip(coordinates, coordinates.dropFirst()).reduce(0) { $0 + TrailGeography.distance($1.0, $1.1) }
        self.bounds = TrailGeography.bounds(coordinates)
    }
    public func project(_ projection: (TrailCoordinate) -> TrailPoint) -> TrailPath {
        var points: [TrailPoint] = [], breaks: Set<Int> = []
        for segment in segments {
            if !points.isEmpty { breaks.insert(points.count) }
            points.append(contentsOf: segment.map(projection))
        }
        return TrailPath(points, breakBefore: breaks)
    }
    /// All-or-nothing: missing projection must never bridge unrelated visible coordinates.
    public func projectIfReady(_ projection: TrailProjection) -> TrailPath? {
        var points: [TrailPoint] = [], breaks: Set<Int> = []
        for segment in segments {
            if !points.isEmpty { breaks.insert(points.count) }
            for coordinate in segment {
                guard let point = projection.project(coordinate) else { return nil }
                points.append(point)
            }
        }
        return TrailPath(points, breakBefore: breaks)
    }
}

public struct TrailRouteKey: Sendable, Hashable {
    public let id: String
    public let revision: Int
    public init(id: String, revision: Int) { self.id = id; self.revision = revision }
}
/// SDK boundary: return local logical units, or nil until the map is ready.
public struct TrailProjection {
    private let body: (TrailCoordinate) -> TrailPoint?
    public init(_ body: @escaping (TrailCoordinate) -> TrailPoint?) { self.body = body }
    public func project(_ coordinate: TrailCoordinate) -> TrailPoint? { body(coordinate) }
}
public struct TrailPathSlice: Sendable {
    public let points: [TrailPoint]
    public let distanceFromStart: Double
}

public enum TrailDirection: Sendable, Equatable { case forward, reverse }
/// Radians clockwise from local +x. Up-facing artwork needs a +pi/2 drawing offset.
public struct TrailPose: Sendable, Equatable {
    public let point: TrailPoint
    public let headingRadians: Double
    public let contourIndex: Int
}

/// Immutable, measured local geometry. Logical units are points on Apple platforms.
public struct TrailPath: Sendable, Equatable {
    public let identity = UUID()
    public let points: [TrailPoint]
    public let breakBefore: Set<Int>
    private let distances: [Double]
    private let contours: [ClosedRange<Int>]
    public let length: Double
    public static func == (lhs: Self, rhs: Self) -> Bool { lhs.identity == rhs.identity || (lhs.points == rhs.points && lhs.breakBefore == rhs.breakBefore) }
    public init(_ points: [TrailPoint], breakBefore: Set<Int> = []) {
        precondition(breakBefore.allSatisfy { $0 > 0 && $0 < points.count }, "Break indices must refer to a point after the first")
        self.points = points; self.breakBefore = breakBefore
        var distances = [Double](repeating: 0, count: points.count)
        if points.count > 1 {
            for i in 1..<points.count {
                distances[i] = distances[i - 1] + (breakBefore.contains(i) ? 0 : hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y))
                precondition(distances[i].isFinite, "Path length overflow")
            }
        }
        self.distances = distances; self.length = distances.last ?? 0
        let starts = [0] + breakBefore.sorted()
        self.contours = points.isEmpty ? [] : starts.enumerated().map { i, start in start...(i + 1 < starts.count ? starts[i+1]-1 : points.count-1) }
    }
    public func point(at fraction: Double) -> TrailPoint? {
        fractionCheck(fraction)
        guard let first = points.first else { return nil }
        if length == 0 || fraction == 0 { return first }
        if fraction == 1 { return points.last }
        let distance = length * fraction
        let end = max(1, min(points.count - 1, upperBound(distance)))
        let start = end - 1
        let span = distances[end] - distances[start]
        let t = span == 0 ? 0 : (distance - distances[start]) / span
        return TrailPoint(points[start].x + (points[end].x - points[start].x) * t,
                          points[start].y + (points[end].y - points[start].y) * t)
    }
    public func tangent(at fraction: Double) -> Double {
        fractionCheck(fraction)
        if length == 0 { return 0 }
        var end = max(1, min(points.count - 1, upperBound(length * fraction)))
        while end > 1 && distances[end] == distances[end-1] { end -= 1 }
        let start = end - 1
        return atan2(points[end].y - points[start].y, points[end].x - points[start].x)
    }
    /// Exact route position and a distance-smoothed heading, independent of clocks/sampling order.
    /// The logical-unit heading window is clipped to the current contour. Zero uses its tangent.
    /// Position never cuts corners or bridges gaps, including after seeking or camera changes.
    public func pose(at fraction: Double, headingWindow: Double = 16,
                     direction: TrailDirection = .forward) -> TrailPose? {
        precondition(headingWindow.isFinite && headingWindow >= 0, "Heading window must be finite and nonnegative")
        guard let point = point(at: fraction) else { return nil }
        let distance = length * fraction
        let anchor = length == 0 || fraction == 0 ? 0 : fraction == 1 ? points.count-1 : max(0, upperBound(distance)-1)
        var low = 0, high = contours.count
        while low < high { let mid = (low+high)/2; if contours[mid].lowerBound <= anchor { low = mid+1 } else { high = mid } }
        let contour = max(0,low-1), range = contours[contour]
        var heading = 0.0
        if distances[range.upperBound] > distances[range.lowerBound] {
            let before = pointInContour(max(distances[range.lowerBound],distance-headingWindow/2),range)
            let after = pointInContour(min(distances[range.upperBound],distance+headingWindow/2),range)
            var dx = after.x-before.x, dy = after.y-before.y
            if hypot(dx,dy) < 1e-9 {
                var end = max(range.lowerBound+1,min(range.upperBound,upperBound(distance)))
                while end > range.lowerBound+1 && distances[end] == distances[end-1] { end -= 1 }
                dx = points[end].x-points[end-1].x; dy = points[end].y-points[end-1].y
            }
            heading = atan2(dy,dx)
        }
        if direction == .reverse { heading += .pi }
        return TrailPose(point: point,headingRadians: atan2(sin(heading),cos(heading)),contourIndex: contour)
    }
    private func pointInContour(_ distance: Double, _ range: ClosedRange<Int>) -> TrailPoint {
        if distance <= distances[range.lowerBound] { return points[range.lowerBound] }
        if distance >= distances[range.upperBound] { return points[range.upperBound] }
        let end = max(range.lowerBound+1,min(range.upperBound,upperBound(distance)))
        let t = (distance-distances[end-1])/(distances[end]-distances[end-1])
        return TrailPoint(points[end-1].x+(points[end].x-points[end-1].x)*t,
                          points[end-1].y+(points[end].y-points[end-1].y)*t)
    }
    public func slice(from start: Double, to end: Double) -> [TrailPoint] {
        precondition(breakBefore.isEmpty, "Use slices() for a path with disconnected contours")
        return slices(from: start, to: end).flatMap(\.points)
    }
    public func slices(from start: Double, to end: Double) -> [TrailPathSlice] {
        fractionCheck(start); fractionCheck(end); precondition(start <= end)
        guard length > 0, start < end else { return [] }
        var result: [TrailPathSlice] = []
        for range in contours {
            let a = max(start * length, distances[range.lowerBound]), b = min(end * length, distances[range.upperBound])
            guard a < b else { continue }
            func point(_ distance: Double) -> TrailPoint {
                if distance <= distances[range.lowerBound] { return points[range.lowerBound] }
                if distance >= distances[range.upperBound] { return points[range.upperBound] }
                let right = max(range.lowerBound+1, min(range.upperBound, upperBound(distance)))
                let t = max(0, min(1, (distance-distances[right-1])/(distances[right]-distances[right-1])))
                return TrailPoint(points[right-1].x+(points[right].x-points[right-1].x)*t,
                                  points[right-1].y+(points[right].y-points[right-1].y)*t)
            }
            var section = [point(a)], i = upperBound(a)
            while i <= range.upperBound && distances[i] < b { section.append(points[i]); i += 1 }
            section.append(point(b)); result.append(TrailPathSlice(points: section, distanceFromStart: a))
        }
        return result
    }
    public func fitted(width: Double, height: Double, padding: Double = 16) -> TrailPath {
        precondition(width.isFinite && height.isFinite && width >= 0 && height >= 0 && padding.isFinite && padding >= 0)
        guard !points.isEmpty else { return self }
        let minX = points.map(\.x).min()!, maxX = points.map(\.x).max()!
        let minY = points.map(\.y).min()!, maxY = points.map(\.y).max()!
        let w = max(0, width - padding * 2), h = max(0, height - padding * 2)
        let rawScale = min(maxX == minX ? .infinity : w / (maxX - minX), maxY == minY ? .infinity : h / (maxY - minY))
        let scale = rawScale.isFinite ? rawScale : 1
        return TrailPath(points.map { TrailPoint(($0.x - (minX + maxX) / 2) * scale + width / 2, ($0.y - (minY + maxY) / 2) * scale + height / 2) }, breakBefore: breakBefore)
    }
    private func upperBound(_ value: Double) -> Int {
        var low = 0, high = distances.count
        while low < high { let mid = (low + high) / 2; if distances[mid] <= value { low = mid + 1 } else { high = mid } }
        return low
    }
}

func fractionCheck(_ value: Double) { precondition(value.isFinite && (0...1).contains(value), "Fraction must be finite and in [0,1]") }
