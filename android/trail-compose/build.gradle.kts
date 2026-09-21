plugins { id("com.android.library"); kotlin("android"); id("org.jetbrains.kotlin.plugin.compose"); `maven-publish` }
android {
    namespace = "dev.trail.compose"; compileSdk = 36
    defaultConfig { minSdk = 24 }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    publishing { singleVariant("release") { withSourcesJar() } }
}
kotlin { jvmToolchain(17) }
dependencies {
    api(project(":trail-android"))
    api(platform("androidx.compose:compose-bom:2026.03.00"))
    api("androidx.compose.ui:ui"); implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
}
afterEvaluate { publishing { publications { create<MavenPublication>("release") { from(components["release"]) } } } }
