plugins { id("com.android.application"); kotlin("android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
    namespace = "dev.trail.sizeprobe"; compileSdk = 36
    defaultConfig { applicationId = "dev.trail.sizeprobe"; minSdk = 24; targetSdk = 36; versionCode = 1; versionName = "1" }
    buildFeatures { compose = true }
    flavorDimensions += "host"
    productFlavors { listOf("viewBase","viewTrail","mapBase","mapTrail").forEach { create(it) { dimension = "host" } } }
    buildTypes { release { isMinifyEnabled = true; isShrinkResources = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt")) } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { jvmToolchain(17) }
dependencies {
    // Compiler-only: the Compose compiler is applied to this multi-flavor probe. These
    // never enter either View APK; inspect the release runtime dependency reports.
    for (flavor in listOf("viewBase","viewTrail")) {
        "${flavor}CompileOnly"(platform("androidx.compose:compose-bom:2026.03.00"))
        "${flavor}CompileOnly"("androidx.compose.runtime:runtime")
    }
    "viewTrailImplementation"(project(":trail-android"))
    for (flavor in listOf("mapBase","mapTrail")) {
        "${flavor}Implementation"(platform("androidx.compose:compose-bom:2026.03.00"))
        "${flavor}Implementation"("androidx.activity:activity-compose:1.12.4")
        "${flavor}Implementation"("com.google.maps.android:maps-compose:6.12.0")
    }
    "mapTrailImplementation"(project(":trail-google-maps"))
}
