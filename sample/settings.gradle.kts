pluginManagement {
    includeBuild("..")
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

// Composite build substitution: replaces com.happix.kexport:* Maven coordinates
// with local project references when developing within this repo.
includeBuild("..")

rootProject.name = "sample"

