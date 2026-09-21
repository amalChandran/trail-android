# Install and distribute Trail

Status: `2.0.0-alpha02` is a **local release candidate**, not yet on Maven Central or Swift Package Index. Installation commands below marked “after publication” become available only after the release gates pass.

## Android: Maven Central

Publish five small artifacts under the proposed namespace `io.github.amalchandran`. Verify that namespace in Sonatype's Central Portal before publication; a GitHub login can provision a matching namespace. [Namespace registration](https://central.sonatype.org/register/namespace/).

After publication, most Google Maps users need one dependency:

```kotlin
repositories { google(); mavenCentral() }
dependencies {
    implementation("io.github.amalchandran:trail-google-maps:2.0.0-alpha02")
}
```

| Add only what you use | Artifact |
| --- | --- |
| Geometry, effect DSL, deterministic animation, plugins, loading transition | `trail-core` |
| Android View / Canvas without Compose or maps | `trail-android` |
| Compose Canvas | `trail-compose` |
| Google Maps native content | `trail-google-maps` |
| Eight styles and twelve named motion presets | `trail-effects` (optional) |

Kotlin packages remain `dev.trail.*`; Maven group IDs are distribution addresses, not import names. Material, vehicle artwork, sample data and the gallery are app-only. Core has no Android, map, reflection, annotation-processor or networking dependency. R8 removes unused APIs. Android minimum API 24; compile/target 36 for the demo.

**Use the candidate now:** run `scripts/release/prepare-android.sh`, then add the absolute `android/build/release-repository` directory as a Maven repository in your consumer's settings. Keep `google()` and `mavenCentral()` for platform dependencies. `scripts/release/test-consumer.sh` builds a separate app from those published coordinates, without project dependencies.

```kotlin
// settings.gradle.kts — LOCAL CANDIDATE ONLY
maven { url = uri("/absolute/path/to/trail/android/build/release-repository") }
```

Use Central as the public default: no JitPack build of every commit, custom repository or GitHub package login for consumers. The legacy Java/JitPack API remains under `legacy/`; version 2 is a documented migration, not a binary-compatible replacement.

## iOS: Swift Package Manager

The independent `trail-ios` repository puts `Package.swift` at its root, with no third-party package dependencies. Xcode: **File → Add Package Dependencies**, paste `https://github.com/amalChandran/trail-ios`, choose the **main** branch for the current untagged candidate, then select **TrailMapKit** for Apple Maps or **TrailUI** for Canvas. **TrailEffects** is optional; plugin authors need **TrailCore** only.

The [public source repository](https://github.com/amalChandran/trail-ios) is available. A sibling `trail-ios` checkout can also be added as a **local package**. iOS 17+, Swift tools 6.0; the package also compiles on macOS 14+.

To try current source before the first versioned tag:

```swift
.package(url: "https://github.com/amalChandran/trail-ios", branch: "main")
```

SPM builds source for the caller's architecture; it does not bundle Android code, vehicle artwork or the sample application. Preserve dead stripping in release builds. Swift Package Index discoverability can be added after the public repository and first tag; CocoaPods/Carthage and binary XCFramework distribution add maintenance without improving this first release's install flow.

## Release gates

1. Run local unit, raster, live map and UI suites, then build the independent package consumers.
2. Stage Maven artifacts, sources, API documentation, POMs and checksums. `central-bundle.py --unsigned-preview` creates a clearly labelled review ZIP. Set `TRAIL_SIGNING_KEY` and `TRAIL_SIGNING_PASSWORD` locally, restage, then run without the preview flag for a signed bundle. [Central requirements](https://central.sonatype.org/publish/requirements/), [Portal upload](https://central.sonatype.org/publish/publish-portal-upload/).
3. Review the published `trail-ios` main branch and Android `master` branch before tagging. Tag both platforms only after final consumer verification. Keep the shared fixture schema/version identical across repositories.
4. Validate the signed Central bundle in the Portal before publishing. Add the Swift tag and verify clean-clone SPM resolution.
5. Keep Trail Studio as an example app. `prepare-android.sh` produces an optimized APK signed with the standard local debug certificate for direct installation, plus the Maven staging directory. The iOS example runs from Xcode or `scripts/run-ios.sh` in `trail-ios`. No store listing, upload key or store version-code confirmation is needed.

No registry credentials, private signing keys, Maps keys or developer-account material belongs in Git. The scripts stage SDK artifacts locally and never publish them.
