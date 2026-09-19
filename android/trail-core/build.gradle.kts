plugins { kotlin("jvm"); `maven-publish`; jacoco }
kotlin { jvmToolchain(17) }
dependencies { testImplementation(kotlin("test-junit")); testImplementation("com.google.code.gson:gson:2.11.0") }
sourceSets.test { resources.srcDir(rootProject.file("../spec/fixtures")) }
tasks.test { finalizedBy(tasks.jacocoTestReport) }
tasks.jacocoTestReport { reports { xml.required = true; html.required = true } }
publishing { publications { create<MavenPublication>("library") { from(components["java"]) } } }
