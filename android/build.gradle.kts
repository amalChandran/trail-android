plugins {
    kotlin("jvm") version "2.3.20" apply false
    kotlin("android") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("com.android.library") version "8.13.2" apply false
    id("com.android.application") version "8.13.2" apply false
}
allprojects { group = "dev.trail"; version = "2.0.0-alpha01" }
