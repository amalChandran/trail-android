# Try the example apps

Trail Studio is a local example app. No Play Console or App Store setup is needed.

## Android

Use Java 17 and Android SDK 36. Put a restricted Google Maps SDK key in ignored `android/local.properties` as `MAPS_API_KEY=…`; the local sample package is `dev.trail.playground` and uses the standard local debug signing certificate.

```sh
./scripts/run-android.sh
```

That builds, starts an emulator if needed, installs and opens the example. For a connected emulator of your choice, set `TRAIL_ANDROID_SERIAL`.

For an optimized sample with R8 and resource shrinking:

```sh
./scripts/release/prepare-android.sh
adb -s emulator-5554 install -r artifacts/release/trail-studio-example.apk
adb -s emulator-5554 shell am start -n dev.trail.playground/.MainActivity
```

## iOS

From the independent `trail-ios` directory:

```sh
./scripts/run-ios.sh
```

Or open `Examples/TrailPlayground.xcodeproj` and choose an iPhone simulator. The example consumes the root Swift package; MapKit needs no API key.

## Flows to try

1. Open **Map journeys →**. Switch between Flight, Cab and Ferry.
2. Compare **Full route**, **Two points**, **Arc** and **Great circle**. The cab's full route preserves all 119 street points.
3. Pause and scrub. Pan rapidly in alternating directions, then rotate/zoom the map; the route and vehicle should retain geographic anchors.
4. Tap **Try loading arc → route**. The example waits for bundled directions, morphs into the complete route, then resumes the vehicle.
5. Change line style and motion; replay. Try reduced motion and background/resume.
6. Open **API examples** for working code: a named effect, custom plugin, playback controls, layers, sequences and map bindings.

These are illustrative demo routes, not live navigation or passenger booking. Networking belongs to the consuming app. See [native recordings](media/README.md), [compiled Android examples](examples/Android.md) and [verification](VERIFICATION.md).
