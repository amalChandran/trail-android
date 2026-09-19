plugins { kotlin("jvm"); `maven-publish` }
kotlin { jvmToolchain(17) }
dependencies { api(project(":trail-core")); testImplementation(kotlin("test-junit")) }
publishing { publications { create<MavenPublication>("library") { from(components["java"]) } } }
