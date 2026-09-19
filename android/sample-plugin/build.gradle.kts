plugins { kotlin("jvm") }
kotlin { jvmToolchain(17) }
dependencies { implementation(project(":trail-core")); testImplementation(kotlin("test-junit")) }
