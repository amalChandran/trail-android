import SwiftUI
import TrailCore

private enum ExampleSelection: String, CaseIterable {
    case basic = "Named preset", runtimeColor = "Runtime color", plugin = "Custom plugin"
    case playback = "Playback controls", sequence = "Sequence", layers = "Layers"
    case mapKit = "MapKit", existingSwiftUIMap = "Existing SwiftUI Map", existingMKMapView = "Existing MKMapView"
    var file: String {
        switch self {
        case .runtimeColor: "runtime-color"
        case .mapKit: "mapkit"
        case .existingSwiftUIMap: "existing-swiftui-map"
        case .existingMKMapView: "existing-mkmapview"
        default: String(describing: self)
        }
    }
}

struct ExamplesBrowser: View {
    @Environment(\.dismiss) private var dismiss
    @State private var selected = ExampleSelection.basic
    @State private var coral = false
    private var code: String {
        guard let url = Bundle.main.url(forResource: "example-\(selected.file)", withExtension: "txt"),
              let source = try? String(contentsOf: url, encoding: .utf8) else { return "Example source missing: run scripts/sync-examples.py." }
        return source
    }
    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 14) {
                Text("Learn by running it.").font(.title2.bold())
                Text("These examples compile with the app.").font(.caption).foregroundStyle(.secondary)
                Picker("Example", selection: $selected) {
                    ForEach(ExampleSelection.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                }.accessibilityIdentifier("examplePicker")
                if selected == .runtimeColor || selected == .plugin {
                    Button("Change brand color") { coral.toggle() }.accessibilityIdentifier("changeBrandColor")
                }
                preview.id(selected).frame(height: 260).background(Color.white.opacity(0.05), in: RoundedRectangle(cornerRadius: 18)).clipShape(RoundedRectangle(cornerRadius: 18))
                ScrollView {
                    Text(code).font(.system(size: 12, design: .monospaced)).frame(maxWidth: .infinity, alignment: .leading).textSelection(.enabled)
                        .accessibilityIdentifier("exampleCode")
                }
            }.padding(20)
                .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() }.accessibilityIdentifier("examplesDone") } }
                .navigationTitle("API examples").navigationBarTitleDisplayMode(.inline)
        }.preferredColorScheme(.dark)
    }
    @ViewBuilder private var preview: some View {
        switch selected {
        case .basic: BasicRouteExample()
        case .runtimeColor: RuntimeColorExample(brandColor: coral ? .coral : .blue)
        case .plugin: PluginExample(brandColor: coral ? .coral : .blue)
        case .playback: PlaybackExample()
        case .sequence: SequenceExample()
        case .layers: LayersExample()
        case .mapKit: MapKitExample()
        case .existingSwiftUIMap: ExistingSwiftUIMapExample()
        case .existingMKMapView: ExistingMKMapViewExample()
        }
    }
}
