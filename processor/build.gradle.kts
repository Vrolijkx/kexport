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

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("kexport") {
            id = "com.happix.kexport"
            implementationClass = "com.happix.kexport.processor.KexportPlugin"
        }
    }
}
