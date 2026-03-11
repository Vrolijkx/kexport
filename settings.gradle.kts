pluginManagement {
    includeBuild(".")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "kexport"

include(":annotation", ":processor")

includeBuild("sample")
