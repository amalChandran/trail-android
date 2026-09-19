import Testing
import UIKit
import MapKit
import TrailCore
import TrailUI
import TrailMapKit

private struct CameraCase: Sendable, CustomStringConvertible {
    let latitude: Double, longitude: Double, heading: Double, span: Double
    var description: String { "lat=\(latitude)/lon=\(longitude)/heading=\(heading)/span=\(span)" }
    static let all = [(40.7527,-73.9772),(-33.8597,151.2105),(51.4706,-0.4619),(0.0,179.8),(70.0,20.0)].flatMap { location in
        [0.0,45.0,180.0].flatMap { heading in [0.02,2.0].map { CameraCase(latitude: location.0,longitude: location.1,heading: heading,span: $0) } }
    }
}

@MainActor private final class MapFixture {
    let window: UIWindow
    let parent=UIViewController()
    let map=MKMapView(frame: CGRect(x: 24,y: 40,width: 320,height: 400))
    let overlay=UIView(frame: CGRect(x: 0,y: 0,width: 320,height: 400))
    init() {
        window=UIWindow(frame: CGRect(x: 0,y: 0,width: 390,height: 700))
        window.rootViewController=parent
        parent.view.addSubview(map); map.addSubview(overlay)
        window.isHidden=false; parent.view.layoutIfNeeded(); map.layoutIfNeeded()
    }
    func close() { window.isHidden=true; window.rootViewController=nil }
}
@MainActor private final class ExistingMapDelegate: NSObject, MKMapViewDelegate {
    var callbacks=0
    func mapViewDidChangeVisibleRegion(_ mapView: MKMapView) { callbacks += 1 }
}

@Suite(.serialized)
@MainActor struct MapKitContractTests {
    @Test(arguments: CameraCase.all)
    fileprivate func realMapProjectionRoundTripsAtDifferentCameras(_ c: CameraCase) async throws {
        let fixture=MapFixture(); defer { fixture.close() }
        let center=CLLocationCoordinate2D(latitude: c.latitude,longitude: c.longitude)
        fixture.map.setRegion(MKCoordinateRegion(center: center,span: MKCoordinateSpan(latitudeDelta: c.span,longitudeDelta: c.span)),animated: false)
        let camera=fixture.map.camera.copy() as! MKMapCamera; camera.heading=c.heading
        fixture.map.setCamera(camera,animated: false); fixture.map.layoutIfNeeded()
        try await Task.sleep(for: .milliseconds(60))
        let coordinate=try TrailCoordinate(latitude: c.latitude+c.span*0.05,longitude: c.longitude+c.span*0.05)
        let point=try #require(TrailProjection.appleMaps(fixture.map,in: fixture.overlay).project(coordinate))
        let native=fixture.map.convert(CLLocationCoordinate2D(latitude: coordinate.latitude,longitude: coordinate.longitude),toPointTo: fixture.overlay)
        // UIKit already returns points. Multiplying by a Retina scale here would fail.
        #expect(abs(point.x-native.x)<0.01); #expect(abs(point.y-native.y)<0.01)
        let restored=fixture.map.convert(CGPoint(x: point.x,y: point.y),toCoordinateFrom: fixture.overlay)
        #expect(abs(restored.latitude-coordinate.latitude)<0.00001)
        #expect(abs(restored.longitude-coordinate.longitude)<0.00001)
        #expect(point.x.isFinite && point.y.isFinite)
    }

    @Test func detachedZeroSizeAndReleasedMapsAreNotReady() throws {
        let coordinate=try TrailCoordinate(latitude: 0,longitude: 0)
        let fixture=MapFixture(); defer { fixture.close() }
        let projection=TrailProjection.appleMaps(fixture.map,in: fixture.overlay)
        #expect(projection.project(coordinate) != nil)
        fixture.map.frame = .zero
        #expect(projection.project(coordinate) == nil)
        fixture.map.frame=CGRect(x: 0,y: 0,width: 320,height: 400)
        fixture.map.removeFromSuperview()
        #expect(projection.project(coordinate) == nil)
        weak var releasedMap: MKMapView?
        var escapedProjection: TrailProjection?
        autoreleasepool {
            let map=MKMapView(frame: .zero), overlay=UIView()
            releasedMap=map; escapedProjection = .appleMaps(map,in: overlay)
        }
        #expect(releasedMap == nil)
        #expect(escapedProjection?.project(coordinate) == nil)
    }

    @Test func attachmentPreservesDelegateUpdatesRouteAndDetachesIdempotently() async throws {
        let fixture=MapFixture(); defer { fixture.close() }
        let delegate=ExistingMapDelegate(); fixture.map.delegate=delegate
        let a=try TrailCoordinate(latitude: 40.758,longitude: -73.9855), b=try TrailCoordinate(latitude: 40.7527,longitude: -73.9772)
        let route=try TrailRoute.direct(id: "cab",from: a,to: b)
        let playback=TrailPlayback(effect: TrailEffect { Reveal(duration: .seconds(8)) },autoPlay: false)
        let originalChildren=fixture.parent.children.count
        let attachment=TrailMapAttachment(mapView: fixture.map,parent: fixture.parent,route: route,playback: playback)
        #expect(attachment.isAttached); #expect(fixture.map.delegate === delegate)
        #expect(fixture.parent.children.count == originalChildren+1)
        playback.seek(to: 0.4)
        fixture.map.setCenter(CLLocationCoordinate2D(latitude: a.latitude,longitude: a.longitude),animated: false)
        try await Task.sleep(for: .milliseconds(60)); attachment.updateProjection()
        #expect(delegate.callbacks>0); #expect(abs(playback.progress-0.4)<1e-9); #expect(!playback.isPlaying)
        attachment.updateRoute(route)
        #expect(abs(playback.progress-0.4)<1e-9)
        attachment.updateRoute(try TrailRoute.direct(id: "cab",from: b,to: a,revision: 1),replay: false)
        #expect(abs(playback.progress-0.4)<1e-9); #expect(!playback.isPlaying)
        attachment.updateRoute(try TrailRoute.direct(id: "cab",from: a,to: b,revision: 2),replay: true)
        #expect(playback.progress==0); #expect(playback.isPlaying)
        playback.pause(); attachment.detach(); attachment.detach()
        #expect(!attachment.isAttached); #expect(fixture.map.delegate === delegate)
        #expect(fixture.parent.children.count==originalChildren)
        playback.seek(to: 0.7); attachment.updateRoute(route); attachment.updateProjection()
        #expect(abs(playback.progress-0.7)<1e-9)
    }

    @Test func dateLineProducesTwoShortNativeContours() async throws {
        let fixture=MapFixture(); defer { fixture.close() }
        fixture.map.setRegion(MKCoordinateRegion(center: CLLocationCoordinate2D(latitude: 0,longitude: 0),span: MKCoordinateSpan(latitudeDelta: 100,longitudeDelta: 359)),animated: false)
        try await Task.sleep(for: .milliseconds(60))
        let route=try TrailRoute.direct(id: "seam",from: TrailCoordinate(latitude: 0,longitude: 170),to: TrailCoordinate(latitude: 0,longitude: -170))
        let path=try #require(route.projectIfReady(.appleMaps(fixture.map,in: fixture.overlay)))
        #expect(path.breakBefore.count==1)
        let contours=path.slices(from: 0,to: 1)
        #expect(contours.count==2)
        #expect(contours.allSatisfy { TrailPath($0.points).length<fixture.map.bounds.width/3 })
        #expect(path.length<fixture.map.bounds.width/2)
    }
}
