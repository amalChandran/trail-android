pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Trail"
include(":trail-core", ":trail-effects", ":trail-android", ":trail-compose", ":trail-google-maps", ":sample-plugin", ":playground")
include(":size-probe")
