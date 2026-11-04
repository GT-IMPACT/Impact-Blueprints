pluginManagement {
    includeBuild("remote-build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://nexus.gtnewhorizons.com/repository/public/") {
            content {
                includeGroupByRegex("com\\.gtnewhorizons(\\..*)?")
            }
        }
    }
}

plugins {
    id("settings-setup")
}

rootProject.name = "Gui3dEngine"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("remote-build-logic/libs.versions.toml"))
            version("forgelin", "2.1.3")
        }
    }
}
