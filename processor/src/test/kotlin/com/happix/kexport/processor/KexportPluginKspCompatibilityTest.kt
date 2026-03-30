package com.happix.kexport.processor

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class KexportPluginKspCompatibilityTest {
    private val supportedKspVersions = listOf(
        KspVersion(ksp = "2.0.21-1.0.28", kotlin = "2.0.21"),
        KspVersion(ksp = "2.1.10-1.0.30", kotlin = "2.1.10"),
        KspVersion(ksp = "2.3.5", kotlin = "2.3.10"),
    )

    data class KspVersion(val ksp: String, val kotlin: String)

    @Test
    fun `plugin applies correctly when project uses various KSP versions`() = runBlocking<Unit> {
        val failures = supportedKspVersions.map { version ->
            async(Dispatchers.IO) {
                val projectDir = Files.createTempDirectory("kexport-ksp-compat").toFile()
                try {
                    testKspVersionInTempDir(projectDir, version)
                    null
                } catch (e: Throwable) {
                    "KSP ${version.ksp} / Kotlin ${version.kotlin}: ${e.message}"
                } finally {
                    projectDir.deleteRecursively()
                }
            }
        }.awaitAll().filterNotNull()

        if (failures.isNotEmpty()) {
            throw AssertionError("Compatibility failures:\n${failures.joinToString("\n")}")
        }
    }

    private fun testKspVersionInTempDir(
        projectDir: File,
        version: KspVersion,
    ) {
        setupLocalRepo(projectDir)
        writeSettingsFile(projectDir)
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "${version.kotlin}"
                id("com.google.devtools.ksp") version "${version.ksp}"
                id("com.happix.kexport")
            }
            kexport {
                packageToScan = "com.example"
            }
            """.trimIndent(),
        )

        projectDir.resolve("src/main/kotlin/com/example").mkdirs()
        projectDir.resolve("src/main/kotlin/com/example/User.kt").writeText(
            """
            package com.example
            import com.happix.kexport.Export

            @Export
            class User(val name: String)
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("build", "--stacktrace")
            .build()

        result.task(":build")?.outcome shouldBe TaskOutcome.SUCCESS

        val generated = projectDir
            .walkTopDown()
            .firstOrNull { it.name == "Dsl.kt" }
        generated?.readText() shouldContain "typealias User = com.example.User"
    }

    // Copies the locally-built annotation and processor jars (passed via system properties
    // by the Gradle test task) into a libs/ flat-dir repository inside the temp project.
    // This lets Gradle resolve com.happix.kexport:annotation:1.0.0 and
    // com.happix.kexport:processor:1.0.0 without requiring a published Maven artifact.
    private fun setupLocalRepo(projectDir: File) {
        val libsDir = projectDir.resolve("libs").also { it.mkdirs() }
        File(System.getProperty("kexport.annotationJar")).copyTo(libsDir.resolve("annotation-1.0.0.jar"))
        File(System.getProperty("kexport.processorJar")).copyTo(libsDir.resolve("processor-1.0.0.jar"))
    }

    private fun writeSettingsFile(projectDir: File) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
                repositories {
                    flatDir { dirs("libs") }
                    mavenCentral()
                }
            }
            rootProject.name = "test-project"
            """.trimIndent(),
        )
    }
}
