plugins { id("com.android.library"); kotlin("android"); `maven-publish` }
android {
    namespace = "dev.trail.android"; compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    publishing { singleVariant("release") { withSourcesJar() } }
}
kotlin { jvmToolchain(17) }
dependencies { api(project(":trail-core")) }
afterEvaluate { publishing { publications { create<MavenPublication>("release") { from(components["release"]) } } } }
