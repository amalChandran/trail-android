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
    public init(id: String, coordinates: [TrailCoordinate], revision: Int = 0) throws {
        guard !id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { throw TrailError.invalidRoute("ID must not be blank") }
        self.id = id; self.coordinates = coordinates; self.revision = revision
    }
    public func project(_ projection: (TrailCoordinate) -> TrailPoint) -> TrailPath { TrailPath(coordinates.map(projection)) }
}

/// Immutable, measured local geometry. Logical units are points on Apple platforms.
public struct TrailPath: Sendable, Equatable {
    public let identity = UUID()
    public let points: [TrailPoint]
    private let distances: [Double]
    public let length: Double
    public static func == (lhs: Self, rhs: Self) -> Bool { lhs.identity == rhs.identity || lhs.points == rhs.points }
    public init(_ points: [TrailPoint]) {
        self.points = points
        var distances = [Double](repeating: 0, count: points.count)
        if points.count > 1 {
            for i in 1..<points.count {
                distances[i] = distances[i - 1] + hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
                precondition(distances[i].isFinite, "Path length overflow")
            }
        }
        self.distances = distances; self.length = distances.last ?? 0
    }
    public func point(at fraction: Double) -> TrailPoint? {
        fractionCheck(fraction)
        guard let first = points.first else { return nil }
        if length == 0 { return first }
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
        let end = max(1, min(points.count - 1, upperBound(length * fraction)))
        var start = end - 1
        while start > 0 && points[start] == points[end] { start -= 1 }
        return atan2(points[end].y - points[start].y, points[end].x - points[start].x)
    }
    public func slice(from start: Double, to end: Double) -> [TrailPoint] {
        fractionCheck(start); fractionCheck(end); precondition(start <= end)
        guard length > 0, start < end, let first = point(at: start), let last = point(at: end) else { return [] }
        var result = [first]; var i = upperBound(start * length)
        while i < points.count && distances[i] < end * length { result.append(points[i]); i += 1 }
        result.append(last); return result
    }
    public func fitted(width: Double, height: Double, padding: Double = 16) -> TrailPath {
        precondition(width.isFinite && height.isFinite && width >= 0 && height >= 0 && padding.isFinite && padding >= 0)
        guard !points.isEmpty else { return self }
        let minX = points.map(\.x).min()!, maxX = points.map(\.x).max()!
        let minY = points.map(\.y).min()!, maxY = points.map(\.y).max()!
        let w = max(0, width - padding * 2), h = max(0, height - padding * 2)
        let rawScale = min(maxX == minX ? .infinity : w / (maxX - minX), maxY == minY ? .infinity : h / (maxY - minY))
        let scale = rawScale.isFinite ? rawScale : 1
        return TrailPath(points.map { TrailPoint(($0.x - (minX + maxX) / 2) * scale + width / 2, ($0.y - (minY + maxY) / 2) * scale + height / 2) })
    }
    private func upperBound(_ value: Double) -> Int {
        var low = 0, high = distances.count
        while low < high { let mid = (low + high) / 2; if distances[mid] <= value { low = mid + 1 } else { high = mid } }
        return low
    }
}

func fractionCheck(_ value: Double) { precondition(value.isFinite && (0...1).contains(value), "Fraction must be finite and in [0,1]") }
