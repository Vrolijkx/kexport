plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
}

group = "com.happix.kexport"
version = "1.0.0"

// Separate configuration for extra jars that must be on the TestKit plugin classpath.
// Using testRuntimeClasspath directly would create a circular dependency, because
// the java-gradle-plugin adds pluginUnderTestMetadata output to testRuntimeClasspath.
val pluginTestClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    implementation(project(":annotation"))
    implementation(libs.ksp.api)
    implementation(libs.ksp.gradle.plugin)

    // kotlin-gradle-plugin is a compileOnly dep of ksp-gradle-plugin, so it is not
    // transitively included in the plugin classpath. Add it explicitly so that TestKit
    // builds can apply kotlin("jvm") and KSP without a NoClassDefFoundError.
    pluginTestClasspath(libs.kotlin.gradle.plugin)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kctfork.core)
    testImplementation(libs.kctfork.ksp)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    val annotationJarTask = project(":annotation").tasks.named<Jar>("jar")
    val processorJarTask = tasks.named<Jar>("jar")
    dependsOn(annotationJarTask, processorJarTask)
    doFirst {
        systemProperty("kexport.annotationJar", annotationJarTask.get().archiveFile.get().asFile.absolutePath)
        systemProperty("kexport.processorJar", processorJarTask.get().archiveFile.get().asFile.absolutePath)
    }
}

tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    pluginClasspath.from(pluginTestClasspath)
}

gradlePlugin {
    plugins {
        create("kexport") {
            id = "com.happix.kexport"
            implementationClass = "com.happix.kexport.processor.KexportPlugin"
        }
    }
}
