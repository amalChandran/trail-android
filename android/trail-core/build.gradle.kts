plugins { kotlin("jvm"); `maven-publish`; jacoco }
kotlin { jvmToolchain(17) }
dependencies { testImplementation(kotlin("test-junit")) }
tasks.test { finalizedBy(tasks.jacocoTestReport) }
tasks.jacocoTestReport { reports { xml.required = true; html.required = true } }
publishing { publications { create<MavenPublication>("library") { from(components["java"]) } } }
