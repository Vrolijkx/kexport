plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "com.happix.kexport"
version = "1.0.0"

dependencies {
    implementation(project(":annotation"))
    implementation(libs.ksp.api)
}
