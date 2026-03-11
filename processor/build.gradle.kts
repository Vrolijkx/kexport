plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
}

group = "com.happix.kexport"
version = "1.0.0"

dependencies {
    implementation(project(":annotation"))
    implementation(libs.ksp.api)
    implementation(libs.ksp.gradle.plugin)
}

gradlePlugin {
    plugins {
        create("kexport") {
            id = "com.happix.kexport"
            implementationClass = "com.happix.kexport.processor.KexportPlugin"
        }
    }
}
