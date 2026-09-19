# Trail 2 relaunch: implementation and release plan

Updated 19 September 2026 after approval of **typed DSL / Swift result builder with named presets**. This repository is a runnable native alpha. The [original detailed proposal](archive/DESIGN.md) remains historical context; [API_GUIDE.md](API_GUIDE.md) and compiled examples define what exists today.

## Architecture implemented

Two native engines share concepts and behavior: Kotlin/JVM on Android and Swift on iOS. Swift does not embed a Kotlin runtime. Geometry, pure animation samplers, effect construction, and deterministic playback live in framework-independent cores. Android Canvas and Core Graphics execute prepared drawing commands. Compose and SwiftUI supply view ownership, frame scheduling, controls, and accessibility integration.

Use Compose for new Android screens and the gallery. Use `TrailView` for View-based consumers that do not need Compose. These are hosts for the same Canvas renderer. On iOS use `TrailCanvas` in SwiftUI; the MapKit attachment also supports an existing UIKit map. Maps and the expressive effects catalog are optional dependencies.

The public extension contracts are `TrailLineStyle` and `TrailAnimation`. Styles prepare bounded strokes/chevrons once per effect. Samplers receive normalized time and return bounded visual state. First-party presets and the separate Metro/Quadratic plugin use these contracts. Applications pass values directly; no registry or runtime discovery is required.

Eight styles and twelve motion presets are implemented. All 96 combinations are tested for deterministic bounded output; that does not imply all combinations communicate equally useful motion. Ordered finite sequences and independently timed layers are explicit. Default rendering is static, and infinite repetition must be requested.

Google Maps Android and Apple MapKit are the first map targets. `TrailProjection` isolates SDK coordinate conversion; `TrailProjectedOverlay` shares readiness, invalidation and rendering behavior. Routes support complete waypoints, direct connections, decorative arcs, spherical great circles and encoded polylines. Date-line splitting and discontinuous contours preserve geometry without drawing a world-spanning gap. The [map contract](MAPS.md) defines what a future adapter must implement and test.

## Delivery gates

| Stage | Work | Exit evidence |
| --- | --- | --- |
| Native alpha — implemented | Selected API, Canvas/View/Compose and SwiftUI hosts, Maps adapters, catalog, external plugin, playback controls, executable integration examples | Native builds and current test results in [VERIFICATION.md](VERIFICATION.md) |
| Consumer beta | Integrate into three independent apps; stage artifacts for external-only plugin tests; finish migration from Java; review diagnostics and naming | First route within ten minutes and custom two-stroke style within thirty minutes, measured with developers who did not design Trail |
| Map hardening — in progress | Shared geography/seam fixtures, live Google camera/route-fit checks, Apple camera/attachment contracts and Android binding lifecycle checks pass; expand the camera/layout/device matrix, globe/world copies, repeated teardown stress and route simplification | Provider-specific native tests and documented accuracy limits; local Google and Apple evidence recorded in [VERIFICATION.md](VERIFICATION.md) |
| Rendering and performance | Golden images for endpoints, alpha, gradient, joins and dashes; measure 100/1,000/10,000 vertices with 1/10/50 routes; profile simple and expensive effect combinations | Repeatable frame-time, allocation, memory, power and idle-clock reports on representative 60/120 Hz devices |
| Footprint | Shrunk release host baseline; add Canvas core, then Compose, effects and maps separately; equivalent iOS product comparisons | Compressed download and installed-size deltas, dependency graph and build times; no debug-playground size used as library overhead |
| Compatibility and release | Public API dumps, Kotlin plugin compatibility, older Swift consumer rebuilds, deployment-target matrix, provenance review, signed packages, changelog and migration guide | Versioned alpha/beta artifacts accepted by external consumers before stable publishing |

The historical 90–130 engineering-day estimate covered a coordinated production release, not this alpha task. Re-estimate remaining work after consumer feedback and device measurements. API generation and additional map providers are separate optional scope.

## Test strategy

Core tests cover distance-based geometry, invalid/degenerate input, immutable data, pause/seek/replay/completion, exact loop boundaries, sequence transitions, layer order, runtime replacement, independent players, reduced motion, plugin output validation, and deterministic sampling. Both implementations now consume 544 machine-readable shared cases. Independent linear geometry references check the binary-search implementation. Native Android Canvas and Core Graphics each run a 144-case pixel matrix for clipping, width, color and alpha, plus disconnected-contour regressions.

Native builds compile the integration examples with real SDKs. Synchronization checks guard example documentation and shared fixtures. UI flows cover controls, runtime configuration, plugin adoption and geographic journeys. MapKit tests exercise real conversion across cameras and attachment lifetime; Android tests exercise the shared binding's lifecycle and a key-gated Google SDK flow. Broader visual goldens, teardown stress, oldest OS versions and real-device performance remain additional gates. Core coverage percentages alone are not a release criterion.

Each fix should add a focused regression test where useful. Release CI should build pinned tools plus the oldest supported platforms, exercise actual external consumers, and store comparable measurements. The workflow now includes native emulator/simulator tests and report artifacts as well as pure tests/lint; it has not been run remotely. The live Google test skips without credentials. A larger supported-OS/device matrix is still release work.

## Performance decisions and open work

Geometry measurement is cached by path/size identity. Styles record commands at construction, not per frame. Frame drivers stop for inactive, paused, completed, reduced-motion, empty or non-visible bindings where the host can detect visibility. Keep one controller per visible binding; a shared controller with multiple display drivers would advance twice.

Current expensive paths include Swift partial-path extraction per stroke, Android dash-path-effect changes, and the stroke count of gradient/comet combinations. Profile those first before introducing GPU resource sessions or a new backend. Add cancellation and resource-release contracts only when a concrete prepared resource needs them.

No smallest-size, zero-allocation, or frame-rate promise is made yet. Prioritize measured dependency and renderer costs over adding another API syntax.
