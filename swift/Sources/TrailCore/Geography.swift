import Foundation

public struct TrailGeoBounds: Sendable, Equatable {
    public let center: TrailCoordinate
    public let latitudeSpan: Double
    public let longitudeSpan: Double
}

public extension TrailRoute {
    static func direct(id: String, from: TrailCoordinate, to: TrailCoordinate, revision: Int = 0) throws -> Self {
        try Self(id: id, coordinates: [from, to], revision: revision)
    }
    static func arc(id: String, from: TrailCoordinate, to: TrailCoordinate, bend: Double = 0.25, steps: Int = 128, revision: Int = 0) throws -> Self {
        try Self(id: id, coordinates: TrailGeography.arc(from, to, bend: bend, steps: steps), revision: revision)
    }
    static func greatCircle(id: String, from: TrailCoordinate, to: TrailCoordinate, steps: Int = 128, revision: Int = 0) throws -> Self {
        try Self(id: id, coordinates: TrailGeography.greatCircle(from, to, steps: steps), revision: revision)
    }
    static func encodedPolyline(id: String, encoded: String, precision: Int = 5, revision: Int = 0) throws -> Self {
        try Self(id: id, coordinates: TrailGeography.decodePolyline(encoded, precision: precision), revision: revision)
    }
}

enum TrailGeography {
    static let earthRadius = 6_371_008.8
    static let mercatorLimit = 85.0511287798066
    static func radians(_ degrees: Double) -> Double { degrees * .pi / 180 }
    static func degrees(_ radians: Double) -> Double { radians * 180 / .pi }
    static func wrap(_ longitude: Double) -> Double {
        let result = ((longitude+180).truncatingRemainder(dividingBy: 360)+360).truncatingRemainder(dividingBy: 360)-180
        return result == -180 && longitude > 0 ? 180 : result
    }
    static func delta(_ a: Double, _ b: Double) -> Double { wrap(b-a) }
    static func coordinate(_ lat: Double, _ lon: Double) -> TrailCoordinate {
        // Internal generated values are finite and normalized by the algorithms below.
        try! TrailCoordinate(latitude: lat, longitude: lon)
    }
    static func distance(_ a: TrailCoordinate, _ b: TrailCoordinate) -> Double {
        let dLat = radians(b.latitude-a.latitude), dLon = radians(delta(a.longitude,b.longitude))
        let h = pow(sin(dLat/2),2) + cos(radians(a.latitude))*cos(radians(b.latitude))*pow(sin(dLon/2),2)
        return 2 * earthRadius * atan2(sqrt(max(0,min(1,h))),sqrt(max(0,min(1,1-h))))
    }
    static func validateSteps(_ steps: Int) throws {
        guard (2...2048).contains(steps) else { throw TrailError.invalidRoute("Sampling steps must be in 2...2048") }
    }
    static func mercatorY(_ latitude: Double) -> Double { log(tan(.pi/4+radians(max(-mercatorLimit,min(mercatorLimit,latitude)))/2)) }
    static func inverseY(_ y: Double) -> Double { degrees(2*atan(exp(y)) - .pi/2) }
    static func arc(_ from: TrailCoordinate, _ to: TrailCoordinate, bend: Double, steps: Int) throws -> [TrailCoordinate] {
        try validateSteps(steps)
        guard bend.isFinite, (-1...1).contains(bend) else { throw TrailError.invalidRoute("Arc bend must be finite and in [-1, 1]") }
        guard abs(from.latitude) <= mercatorLimit, abs(to.latitude) <= mercatorLimit else { throw TrailError.invalidRoute("Mercator arcs require endpoints within ±85.051129° latitude") }
        let ax = radians(from.longitude), ay = mercatorY(from.latitude)
        let bx = ax+radians(delta(from.longitude,to.longitude)), by = mercatorY(to.latitude)
        let cx = (ax+bx)/2-(by-ay)*bend, cy = (ay+by)/2+(bx-ax)*bend
        return (0...steps).map { i in
            if i == 0 { return from }; if i == steps { return to }
            let t = Double(i)/Double(steps), s = 1-t
            return coordinate(inverseY(s*s*ay+2*s*t*cy+t*t*by),wrap(degrees(s*s*ax+2*s*t*cx+t*t*bx)))
        }
    }
    static func greatCircle(_ from: TrailCoordinate, _ to: TrailCoordinate, steps: Int) throws -> [TrailCoordinate] {
        try validateSteps(steps)
        func vector(_ c: TrailCoordinate) -> [Double] {
            let lat = radians(c.latitude), lon = radians(c.longitude)
            return [cos(lat)*cos(lon),cos(lat)*sin(lon),sin(lat)]
        }
        let a = vector(from), b = vector(to)
        let dot = max(-1,min(1,zip(a,b).reduce(0) { $0+$1.0*$1.1 }))
        let cross = [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
        let magnitude = sqrt(cross.reduce(0) { $0+$1*$1 })
        guard !(magnitude < 1e-12 && dot < 0) else { throw TrailError.invalidRoute("Antipodal endpoints have no unique great circle; provide an intermediate waypoint") }
        let angle = atan2(magnitude,dot)
        return (0...steps).map { i in
            if i == 0 { return from }; if i == steps { return to }
            let t = Double(i)/Double(steps)
            if magnitude < 1e-12 { return coordinate(from.latitude+(to.latitude-from.latitude)*t,wrap(from.longitude+delta(from.longitude,to.longitude)*t)) }
            let p = a.indices.map { cos(angle*t)*a[$0]+sin(angle*t)*(b[$0]-dot*a[$0])/magnitude }
            return coordinate(degrees(atan2(p[2],hypot(p[0],p[1]))),wrap(degrees(atan2(p[1],p[0]))))
        }
    }
    static func bounds(_ points: [TrailCoordinate]) -> TrailGeoBounds? {
        guard !points.isEmpty else { return nil }
        let longitudes = points.map { ($0.longitude+360).truncatingRemainder(dividingBy: 360) }.sorted()
        var largestGap = -1.0, beginning = longitudes[0]
        for i in longitudes.indices {
            let next = i == longitudes.count-1 ? longitudes[0]+360 : longitudes[i+1]
            if next-longitudes[i] > largestGap { largestGap = next-longitudes[i]; beginning = next.truncatingRemainder(dividingBy: 360) }
        }
        let south = points.map(\.latitude).min()!, north = points.map(\.latitude).max()!
        let span = max(0,min(360,360-largestGap))
        return TrailGeoBounds(center: coordinate((south+north)/2,wrap(beginning+span/2)), latitudeSpan: north-south, longitudeSpan: span)
    }
    static func splitAtDateLine(_ points: [TrailCoordinate]) -> [[TrailCoordinate]] {
        guard let first = points.first else { return [] }
        var result: [[TrailCoordinate]] = [], current = [first]
        for (a,b) in zip(points,points.dropFirst()) {
            if abs(b.longitude-a.longitude) > 180 {
                let d = delta(a.longitude,b.longitude)
                if abs(d) < 1e-12 { result.append(current); current = [b]; continue }
                let boundary = d > 0 ? 180.0 : -180.0
                let t = max(0,min(1,(boundary-a.longitude)/d))
                let lat = inverseY(mercatorY(a.latitude)+(mercatorY(b.latitude)-mercatorY(a.latitude))*t)
                current.append(coordinate(lat,boundary)); result.append(current)
                current = [coordinate(lat,-boundary)]
            }
            current.append(b)
        }
        result.append(current); return result
    }
    static func decodePolyline(_ encoded: String, precision: Int) throws -> [TrailCoordinate] {
        guard precision == 5 || precision == 6 else { throw TrailError.invalidRoute("Polyline precision must be 5 or 6") }
        guard encoded.utf8.count <= 2_000_000 else { throw TrailError.invalidRoute("Encoded polyline exceeds the input budget") }
        let bytes = Array(encoded.utf8); var index = 0
        func component() throws -> Int64 {
            var result: Int64 = 0, shift = 0
            while true {
                guard index < bytes.count, shift <= 30 else { throw TrailError.invalidRoute("Truncated or overflowing encoded polyline at character \(index)") }
                let byte = Int(bytes[index])-63; index += 1
                guard (0...63).contains(byte) else { throw TrailError.invalidRoute("Invalid encoded polyline character at \(index-1)") }
                result |= Int64(byte & 31) << shift
                if byte < 32 { break }; shift += 5
            }
            return result & 1 != 0 ? ~(result >> 1) : result >> 1
        }
        let scale = pow(10.0,Double(precision)); var lat: Int64 = 0, lon: Int64 = 0
        var output: [TrailCoordinate] = []
        while index < bytes.count {
            guard output.count < 100_000 else { throw TrailError.invalidRoute("A route supports at most 100,000 coordinates") }
            lat += try component(); lon += try component()
            output.append(try TrailCoordinate(latitude: Double(lat)/scale,longitude: Double(lon)/scale))
        }
        return output
    }
}
