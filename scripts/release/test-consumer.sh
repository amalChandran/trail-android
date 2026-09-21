#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/../common.sh"
TRAIL_CONSUMER="$TRAIL_ROOT/artifacts/release/consumer"
export TRAIL_CONSUMER TRAIL_ROOT
python3 - <<'PY'
from pathlib import Path
import os
root=Path(os.environ['TRAIL_ROOT']); target=Path(os.environ['TRAIL_CONSUMER']);target.mkdir(parents=True,exist_ok=True)
repo=(root/'android/build/release-repository').as_posix()
(target/'settings.gradle.kts').write_text('''pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { maven { url=uri("'''+repo+'''") }; google(); mavenCentral() } }
rootProject.name="TrailConsumer"
''')
(target/'build.gradle.kts').write_text('''plugins { id("com.android.application") version "8.13.2"; kotlin("android") version "2.3.20"; id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" }
android { namespace="dev.trail.sizeprobe"; compileSdk=36
 defaultConfig { applicationId="dev.trail.consumer"; minSdk=24; targetSdk=36 }
 buildFeatures { compose=true }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
}
kotlin { jvmToolchain(17) }
dependencies {
 implementation("io.github.amalchandran:trail-google-maps:2.0.0-alpha02")
 implementation("androidx.activity:activity-compose:1.12.4")
}
''')
(target/'gradle.properties').write_text('android.useAndroidX=true\norg.gradle.jvmargs=-Xmx2g\n')
for source,relative in [('android/size-probe/src/main/AndroidManifest.xml','src/main/AndroidManifest.xml'),('android/size-probe/src/mapTrail/kotlin/dev/trail/sizeprobe/MainActivity.kt','src/main/kotlin/dev/trail/sizeprobe/MainActivity.kt')]:
 path=target/relative;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes((root/source).read_bytes())
PY
"$TRAIL_ROOT/android/gradlew" -p "$TRAIL_CONSUMER" assembleDebug --console=plain
