import Testing
import TrailCore

@Test func distanceHasIndependentKnownSphericalAnchors() throws {
    let origin=try TrailCoordinate(latitude: 0,longitude: 0)
    let anchors: [(Double,Double,Double)] = [
        (0,0,0), (0,1,111195.0802335329), (1,0,111195.0802335329),
        (0,90,10007557.221017962), (90,0,10007557.221017962), (0,180,20015114.442035925),
    ]
    for (lat,lon,meters) in anchors {
        let route=try TrailRoute.direct(id: "anchor",from: origin,to: TrailCoordinate(latitude: lat,longitude: lon))
        #expect(abs(route.distanceMeters-meters)<0.001)
    }
}
@Test func samplingAndBendRejectInvalidExternalValues() throws {
    let origin=try TrailCoordinate(latitude: 0,longitude: 0)
    for steps in [Int.min,-1,0,1,2049,Int.max] {
        #expect(throws: TrailError.self) { try TrailRoute.arc(id: "arc",from: origin,to: origin,steps: steps) }
        #expect(throws: TrailError.self) { try TrailRoute.greatCircle(id: "gc",from: origin,to: origin,steps: steps) }
    }
    for bend in [Double.nan,Double.infinity,-Double.infinity,-1.01,1.01] {
        #expect(throws: TrailError.self) { try TrailRoute.arc(id: "arc",from: origin,to: origin,bend: bend) }
    }
    #expect(try TrailRoute.arc(id: "max",from: origin,to: origin,bend: -1,steps: 2048).coordinates.count==2049)
}
@Test func routeAndEncodedInputBudgetsAreEnforced() throws {
    let origin=try TrailCoordinate(latitude: 0,longitude: 0)
    #expect(try TrailRoute(id: "max",coordinates: Array(repeating: origin,count: 100_000)).coordinates.count==100_000)
    #expect(throws: TrailError.self) { try TrailRoute(id: "large",coordinates: Array(repeating: origin,count: 100_001)) }
    #expect(throws: TrailError.invalidRoute("Encoded polyline exceeds the input budget")) {
        try TrailRoute.encodedPolyline(id: "large",encoded: String(repeating: "?",count: 2_000_001))
    }
    #expect(throws: TrailError.self) { try TrailRoute.encodedPolyline(id: "points",encoded: String(repeating: "??",count: 100_001)) }
}
@Test func routeIdentityAndInputsAreValidatedAndValueCopied() throws {
    let origin=try TrailCoordinate(latitude: 0,longitude: 0)
    for id in [""," ","\n\t"] { #expect(throws: TrailError.self) { try TrailRoute(id: id,coordinates: [origin]) } }
    #expect(throws: TrailError.self) { try TrailRoute(id: "route",coordinates: [origin],revision: -1) }
    var input=[origin,try TrailCoordinate(latitude: 1,longitude: 1)]
    let route=try TrailRoute(id: "immutable",coordinates: input); input.removeAll()
    #expect(route.coordinates.count==2); #expect(route.segments.first?.count==2); #expect(route.distanceMeters>0)
}
