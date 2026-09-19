import SwiftUI
import TrailCore
import TrailEffects
import TrailUI
import TrailMapKit
import TrailSamplePlugin

@main struct TrailPlaygroundApp: App {
    var body: some Scene { WindowGroup { StudioView() } }
}

private let ink = Color(red: 0.047, green: 0.098, blue: 0.129)
private let panel = Color(red: 0.075, green: 0.149, blue: 0.192)
private let mint = Color(red: 0.196, green: 0.839, blue: 0.678)
private let muted = Color(red: 0.58, green: 0.66, blue: 0.71)
private let demoPath = TrailPath([
    TrailPoint(0, 85), TrailPoint(30, 85), TrailPoint(44, 66), TrailPoint(68, 66),
    TrailPoint(84, 40), TrailPoint(120, 40), TrailPoint(140, 17), TrailPoint(170, 17),
    TrailPoint(190, 44), TrailPoint(222, 44), TrailPoint(246, 0), TrailPoint(280, 0),
])
private let mapRoute = try! TrailRoute(id: "san-francisco", coordinates: [
    TrailCoordinate(latitude: 37.779, longitude: -122.423), TrailCoordinate(latitude: 37.779, longitude: -122.418),
    TrailCoordinate(latitude: 37.783, longitude: -122.418), TrailCoordinate(latitude: 37.783, longitude: -122.412),
    TrailCoordinate(latitude: 37.787, longitude: -122.412), TrailCoordinate(latitude: 37.790, longitude: -122.405),
])

@MainActor struct StudioView: View {
    @State private var style = TrailStylePreset.cased
    @State private var motion = TrailMotionPreset.reveal
    @State private var duration = 3.0
    @State private var repeats = true
    @State private var reduced = false
    @State private var plugin = false
    @State private var showMap = false
    @State private var showExamples = false
    @State private var playback = TrailPlayback(effect: TrailMotionPreset.reveal.effect(style: TrailStylePreset.cased.style()))
    private var configuration: String { "\(style.rawValue)|\(motion.rawValue)|\(duration)|\(repeats)|\(plugin)" }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 21) {
                HStack(alignment: .firstTextBaseline) {
                    Text("trail.").font(.system(size: 44, weight: .bold, design: .rounded)).tracking(-2)
                    Spacer()
                    Text("STUDIO / 2.0").font(.system(size: 11, design: .monospaced)).foregroundStyle(mint)
                }
                Text("A little motion.\nA clear direction.").font(.system(size: 29, weight: .medium)).lineSpacing(1)
                Text("Native Swift · public plugins · live preview").font(.system(size: 12)).foregroundStyle(muted)
                Button("API examples →") { showExamples = true }.accessibilityIdentifier("openExamples")
                preview
                PlaybackControls(playback: playback)
                HStack {
                    VStack(alignment: .leading, spacing: 8) {
                        eyebrow("LINE STYLE")
                        Picker("Line style", selection: $style) { ForEach(TrailStylePreset.allCases, id: \.self) { Text($0.rawValue).tag($0) } }
                            .labelsHidden().accessibilityIdentifier("stylePicker")
                    }
                    Spacer()
                    VStack(alignment: .leading, spacing: 8) {
                        eyebrow("ANIMATION")
                        Picker("Animation", selection: $motion) { ForEach(TrailMotionPreset.allCases, id: \.self) { Text($0.rawValue).tag($0) } }
                            .labelsHidden().accessibilityIdentifier("motionPicker")
                    }
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text("Duration  \(duration, specifier: "%.1f") s").font(.system(size: 13))
                    Slider(value: $duration, in: 1...8, step: 0.5).accessibilityIdentifier("duration")
                }
                Toggle("Loop animation", isOn: $repeats).accessibilityIdentifier("loop")
                Toggle("Reduced motion", isOn: $reduced).accessibilityIdentifier("reducedMotion")
                Toggle("Use my Metro plugin", isOn: $plugin).accessibilityIdentifier("customPlugin")
                Toggle("MapKit preview", isOn: $showMap).accessibilityIdentifier("mapPreview")
                if motion == .dashFlow || motion == .revealThenFlow {
                    Text("Flow is visible on Dashed, Dotted, and Chevrons.").font(.caption).foregroundStyle(mint)
                }
                VStack(alignment: .leading, spacing: 12) {
                    eyebrow("MAKE IT YOURS").foregroundStyle(mint)
                    Text(effectSource)
                        .font(.system(size: 12, design: .monospaced)).lineSpacing(5).textSelection(.enabled)
                }.frame(maxWidth: .infinity, alignment: .leading).padding(18).background(panel, in: RoundedRectangle(cornerRadius: 16))
                Text("Built for your next route.  /  alpha 01").font(.system(size: 11)).foregroundStyle(muted)
            }.padding(24)
        }
        .background(ink).foregroundStyle(Color(red: 0.89, green: 0.94, blue: 0.95)).tint(mint).preferredColorScheme(.dark)
        .sheet(isPresented: $showExamples) { ExamplesBrowser() }
        .onChange(of: configuration) { _, _ in
            let effect = plugin
                ? TrailEffect { Style(MetroStyle()); Animate(TrailAnimations.custom(QuadraticReveal(), duration: .seconds(duration), repeats: repeats)) }
                : motion.effect(style: style.style(), duration: .seconds(duration), repeats: repeats)
            playback.configure(effect, reset: true)
        }
    }

    private var effectSource: String {
        let stroke = plugin ? "Style(MetroStyle())" : "Style(TrailStylePreset.\(String(describing: style)).style())"
        let animation = plugin ? "Animate(TrailAnimations.custom(QuadraticReveal(), duration: .seconds(\(duration)), repeats: \(repeats)))"
            : "Animate(TrailMotionPreset.\(String(describing: motion)).animation(duration: .seconds(\(duration)), repeats: \(repeats)))"
        let body = !plugin && motion == .spotlight
            ? "    Layer { Stroke(TrailColor(0xFF344D5B), width: 6) }\n    Layer {\n        \(stroke)\n        \(animation)\n    }"
            : "    \(stroke)\n    \(animation)"
        return "let effect = TrailEffect {\n\(body)\n}\nTrailCanvas(path: path, effect: effect)"
    }

    private var preview: some View {
        ZStack(alignment: .topLeading) {
            if showMap {
                TrailMap(route: mapRoute, playback: playback, reducedMotion: reduced)
            } else {
                Canvas { context, size in
                    var grid = Path()
                    for x in stride(from: 0.0, through: size.width, by: 22) { grid.move(to: CGPoint(x: x, y: 0)); grid.addLine(to: CGPoint(x: x, y: size.height)) }
                    for y in stride(from: 0.0, through: size.height, by: 22) { grid.move(to: CGPoint(x: 0, y: y)); grid.addLine(to: CGPoint(x: size.width, y: y)) }
                    context.stroke(grid, with: .color(Color.white.opacity(0.055)), lineWidth: 0.7)
                }
                TrailCanvas(path: demoPath, playback: playback, reducedMotion: reduced)
                VStack(alignment: .leading) {
                    eyebrow(reduced ? "REDUCED MOTION" : "LIVE CANVAS")
                    Spacer()
                    eyebrow(plugin ? "YOUR PLUGIN" : "\(style.rawValue.uppercased()) / \(motion.rawValue.uppercased())").foregroundStyle(mint)
                }.padding(17).allowsHitTesting(false)
            }
        }.frame(height: 230).background(panel).clipShape(RoundedRectangle(cornerRadius: 22)).accessibilityIdentifier("preview")
    }
    private func eyebrow(_ value: String) -> some View { Text(value).font(.system(size: 10, weight: .medium)).tracking(1.5).foregroundStyle(muted) }
}

@MainActor private struct PlaybackControls: View {
    let playback: TrailPlayback
    var body: some View {
        VStack(spacing: 21) {
            HStack(spacing: 12) {
                Button { playback.isPlaying ? playback.pause() : playback.play() } label: {
                    Text(playback.isPlaying ? "Pause" : "Play").frame(maxWidth: .infinity).padding(.vertical, 5)
                }.buttonStyle(.borderedProminent).foregroundStyle(ink).accessibilityIdentifier("playPause")
                Button { playback.replay() } label: { Text("Replay").frame(maxWidth: .infinity).padding(.vertical, 5) }
                    .buttonStyle(.bordered).accessibilityIdentifier("replay")
            }
            VStack(spacing: 4) {
                HStack {
                    Text("PROGRESS").font(.system(size: 10, weight: .medium)).tracking(1.5).foregroundStyle(muted); Spacer()
                    Text("\(Int(playback.progress * 100))%").font(.system(size: 12, design: .monospaced)).accessibilityIdentifier("progressValue")
                }
                Slider(value: Binding(get: { playback.progress }, set: { playback.seek(to: $0) }), in: 0...1).accessibilityIdentifier("progress")
            }
        }
    }
}
