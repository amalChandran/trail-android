import SwiftUI
import MapKit
import TrailCore
import TrailUI
import TrailMapKit
import TrailEffects

private struct Journey: Decodable, Identifiable {
    let id: String, title: String, origin: String, destination: String, symbol: String
    let description: String, provenance: String, coordinates: [[Double]], defaultGeometry: String
    var color: TrailColor { id == "cab" ? TrailColor(0xFFFFC857) : id == "ferry" ? .mint : TrailColor(0xFF8FB8FF) }
    func route(_ mode: DrawingMode) -> TrailRoute {
        // Bundled, validated fixtures. External input belongs behind do/try/catch.
        let points = coordinates.map { try! TrailCoordinate(latitude: $0[0],longitude: $0[1]) }
        let key = "\(id)/\(mode.rawValue)"
        switch mode {
        case .arc: return try! .arc(id: key,from: points.first!,to: points.last!,bend: 0.28)
        case .direct: return try! .direct(id: key,from: points.first!,to: points.last!)
        case .greatCircle: return try! .greatCircle(id: key,from: points.first!,to: points.last!)
        case .route:
            return id == "flight" ? try! .greatCircle(id: key,from: points.first!,to: points.last!) : try! TrailRoute(id: key,coordinates: points)
        }
    }
}
private enum DrawingMode: String, CaseIterable {
    case route, direct, arc, greatCircle
    var title: String { switch self { case .route: "Full route"; case .direct: "Two points"; case .arc: "Arc"; case .greatCircle: "Great circle" } }
}
private struct JourneyFixtures: Decodable {
    let journeys: [Journey]
    static let shared = try! JSONDecoder().decode(Self.self,from: Data(contentsOf: Bundle.main.url(forResource: "journeys",withExtension: "json")!))
}

@MainActor struct JourneyExamples: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var systemReducedMotion
    @State private var selected = 0
    @State private var mode = DrawingMode.arc
    @State private var style = TrailStylePreset.cased
    @State private var motion = TrailMotionPreset.reveal
    @State private var reduced = false
    @State private var playback = TrailPlayback()
    private var journey: Journey { JourneyFixtures.shared.journeys[selected] }
    private var vehicle: VehicleArtwork { VehicleArtwork.fleet[journey.id]! }
    private var effect: TrailEffect { TrailEffect {
        Layer { Stroke(journey.color.opacity(0.22),width: 6) }
        Layer { Style(style.style(color: journey.color)); Animate(vehicle.animation(motion)) }
    } }
    private var effectKey: String { "\(selected)/\(style.rawValue)/\(motion.rawValue)" }

    var body: some View {
        let route = journey.route(mode)
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading,spacing: 14) {
                    Text("Routes, in the real world.").font(.title2.bold())
                    HStack {
                        ForEach(Array(JourneyFixtures.shared.journeys.enumerated()),id: \.element.id) { index,item in
                            Button("\(item.symbol) \(item.id.capitalized)") { selected=index; mode=DrawingMode(rawValue: item.defaultGeometry)! }
                                .buttonStyle(.bordered).tint(selected == index ? .mint : .gray).accessibilityIdentifier("journey-\(item.id)")
                        }
                    }
                    Text(journey.title).font(.headline).accessibilityIdentifier("journeyTitle")
                    Text(journey.description).font(.caption).foregroundStyle(.secondary)
                    AppleJourneyMap(route: route,origin: journey.origin,destination: journey.destination,vehicle: vehicle,
                                    playback: playback,reduced: reduced || systemReducedMotion)
                        .frame(height: 330).clipShape(RoundedRectangle(cornerRadius: 20))
                    Text("\(mode.title) · \(route.coordinates.count) points · \(route.distanceMeters/1000,specifier: "%.1f") km")
                        .font(.caption).accessibilityIdentifier("journeyGeometry")
                    JourneyPlaybackControls(playback: playback)
                    Picker("Drawing",selection: $mode) { ForEach(DrawingMode.allCases,id: \.self) { Text($0.title).tag($0) } }.accessibilityIdentifier("journeyDrawing")
                    HStack {
                        Picker("Line",selection: $style) { ForEach(TrailStylePreset.allCases,id: \.self) { Text($0.rawValue).tag($0) } }.accessibilityIdentifier("journeyStyle")
                        Picker("Motion",selection: $motion) { ForEach(TrailMotionPreset.allCases,id: \.self) { Text($0.rawValue).tag($0) } }.accessibilityIdentifier("journeyMotion")
                    }
                    Toggle("Reduced motion",isOn: $reduced).accessibilityIdentifier("journeyReduced")
                    Text(mode == .arc ? "A decorative connection from two endpoints." : mode == .direct ? "A direct connection; intermediate waypoints are intentionally omitted." : mode == .greatCircle ? "A sampled shortest path on a spherical Earth." : journey.provenance)
                        .font(.caption).foregroundStyle(.secondary)
                    if journey.id == "cab" { Text("Route data © OpenStreetMap contributors · ODbL").font(.caption2).foregroundStyle(.secondary) }
                }.padding(18)
            }
            .navigationTitle("Map journeys").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() }.accessibilityIdentifier("journeysDone") } }
        }
        .preferredColorScheme(.dark)
        .onAppear { playback.configure(effect,reset: true) }
        .onChange(of: route.key) { _,_ in playback.configure(effect,reset: true) }
        .onChange(of: effectKey) { _,_ in playback.configure(effect) }
    }
}

@MainActor private struct AppleJourneyMap: View {
    let route: TrailRoute
    let origin: String, destination: String
    let vehicle: VehicleArtwork
    let playback: TrailPlayback
    let reduced: Bool
    @State private var position = MapCameraPosition.automatic
    @State private var cameraRevision = 0
    @State private var projected: TrailPath?
    var body: some View {
        MapReader { proxy in
            Map(position: $position) {
                if let first=route.coordinates.first,let last=route.coordinates.last {
                    Marker(origin,coordinate: CLLocationCoordinate2D(latitude: first.latitude,longitude: first.longitude)).tint(.mint)
                    Marker(destination,coordinate: CLLocationCoordinate2D(latitude: last.latitude,longitude: last.longitude)).tint(.orange)
                }
            }
            .mapStyle(.standard(elevation: .flat))
            .onMapCameraChange(frequency: .continuous) { _ in cameraRevision &+= 1 }
            .overlay {
                TrailMapOverlay(route: route,map: proxy,cameraRevision: cameraRevision,playback: playback,reducedMotion: reduced,onProjected: { if projected != $0 { projected=$0 } })
            }
            .overlay { if let projected { JourneyVehicle(path: projected,artwork: vehicle,playback: playback,reduced: reduced) } }
            .overlay(alignment: .topLeading) {
                Text(projected == nil ? "Projecting route…" : "Apple Maps · ready").font(.system(size: 10)).padding(5)
                    .background(.black.opacity(0.75),in: RoundedRectangle(cornerRadius: 5)).padding(10).accessibilityIdentifier("journeyMapStatus")
            }
        }
        .onChange(of: route.key,initial: true) { _,_ in
            projected=nil
            guard let bounds=route.bounds else { return }
            position = .region(MKCoordinateRegion(center: CLLocationCoordinate2D(latitude: bounds.center.latitude,longitude: bounds.center.longitude),
                span: MKCoordinateSpan(latitudeDelta: max(0.005,min(170,bounds.latitudeSpan*1.5)),longitudeDelta: max(0.005,min(359,bounds.longitudeSpan*1.5)))))
        }
    }
}

@MainActor private struct JourneyPlaybackControls: View {
    let playback: TrailPlayback
    var body: some View {
        HStack {
            Button(playback.isPlaying ? "Pause" : "Play") { playback.isPlaying ? playback.pause() : playback.play() }
                .buttonStyle(.borderedProminent).accessibilityIdentifier("journeyPlayPause")
            Button("Replay") { playback.replay() }.buttonStyle(.bordered).accessibilityIdentifier("journeyReplay")
        }
        Slider(value: Binding(get: { playback.progress },set: { playback.seek(to: $0) }),in: 0...1).accessibilityIdentifier("journeyProgress")
    }
}
