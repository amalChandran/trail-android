plugins { id("com.android.library"); kotlin("android"); id("org.jetbrains.kotlin.plugin.compose"); `maven-publish` }
android {
    namespace = "dev.trail.googlemaps"; compileSdk = 36
    defaultConfig { minSdk = 24 }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    publishing { singleVariant("release") { withSourcesJar() } }
}
kotlin { jvmToolchain(17) }
dependencies { api(project(":trail-compose")); api("com.google.maps.android:maps-compose:6.12.0") }
afterEvaluate { publishing { publications { create<MavenPublication>("release") { from(components["release"]) } } } }
