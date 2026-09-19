import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension
import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm") version "2.3.20" apply false
    kotlin("android") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("com.android.library") version "8.13.2" apply false
    id("com.android.application") version "8.13.2" apply false
}
allprojects {
    group = providers.gradleProperty("trailGroup").getOrElse("io.github.amalchandran")
    version = providers.gradleProperty("trailVersion").getOrElse("2.0.0-alpha02")
}

subprojects {
    plugins.withId("maven-publish") {
        apply(plugin = "signing")
        val apiDocs = tasks.register<Jar>("apiDocsJar") {
            archiveClassifier.set("javadoc")
            from(rootProject.file("../docs/API_GUIDE.md"))
            from(rootProject.file("../docs/MAPS.md"))
            from(rootProject.file("../docs/examples/Android.md"))
            from("src/main/kotlin") { include("**/*.kt") }
        }
        extensions.configure<PublishingExtension> {
            repositories { maven { name = "staging"; url = rootProject.layout.buildDirectory.dir("release-repository").get().asFile.toURI() } }
            publications.withType<MavenPublication>().configureEach {
                artifact(apiDocs)
                pom {
                    name.set(project.name); description.set("Trail: small, extensible native route animation for Kotlin")
                    url.set("https://github.com/amalChandran/trail-android")
                    licenses { license { name.set("MIT License"); url.set("https://opensource.org/licenses/MIT") } }
                    developers { developer { id.set("amalChandran"); name.set("Amal Chandran"); url.set("https://github.com/amalChandran") } }
                    scm {
                        url.set("https://github.com/amalChandran/trail-android")
                        connection.set("scm:git:https://github.com/amalChandran/trail-android.git")
                        developerConnection.set("scm:git:ssh://git@github.com/amalChandran/trail-android.git")
                    }
                }
            }
        }
        extensions.configure<SigningExtension> {
            val key = providers.environmentVariable("TRAIL_SIGNING_KEY").orNull
            if (key != null) {
                useInMemoryPgpKeys(key, providers.environmentVariable("TRAIL_SIGNING_PASSWORD").orNull)
                sign(extensions.getByType<PublishingExtension>().publications)
            }
        }
    }
}
