plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.0"
    signing
}

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
    website = "https://github.com/Vrolijkx/kexport"
    vcsUrl = "https://github.com/Vrolijkx/kexport"
    plugins {
        create("kexport") {
            id = "com.happix.kexport"
            implementationClass = "com.happix.kexport.processor.KexportPlugin"
            displayName = "kexport"
            description = "Generates a clean DSL surface for your Kotlin modules using @Export annotations"
            tags = listOf("kotlin", "ksp", "codegen", "dsl")
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("kexport-processor")
            description.set("Generates a clean DSL surface for your Kotlin modules using @Export annotations")
            url.set("https://github.com/Vrolijkx/kexport")
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("Vrolijkx")
                    name.set("Vrolijkx")
                }
            }
            scm {
                url.set("https://github.com/Vrolijkx/kexport")
                connection.set("scm:git:git://github.com/Vrolijkx/kexport.git")
                developerConnection.set("scm:git:ssh://git@github.com/Vrolijkx/kexport.git")
            }
        }
    }
}

signing {
    val signingKey = providers.environmentVariable("GPG_SIGNING_KEY").orNull
    val signingPassword = providers.environmentVariable("GPG_SIGNING_PASSWORD").orNull
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}
