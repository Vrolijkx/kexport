package com.happix.kexport.processor

import io.kotest.matchers.shouldBe
import io.kotest.property.Exhaustive
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.collection
import kotlinx.coroutines.runBlocking
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class KexportPluginKspCompatibilityTest {

    data class KspVersion(val ksp: String, val kotlin: String)

    @Test
    fun `plugin applies correctly when project uses various KSP versions`() = runBlocking<Unit> {
        Exhaustive.collection(KSP_VERSIONS).checkAll { version ->
            val projectDir = Files.createTempDirectory("kexport-ksp-compat").toFile()
            try {
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

                val result = GradleRunner.create()
                    .withProjectDir(projectDir)
                    .withPluginClasspath()
                    .withArguments("tasks", "--stacktrace")
                    .build()

                result.task(":tasks")?.outcome shouldBe TaskOutcome.SUCCESS
            } finally {
                projectDir.deleteRecursively()
            }
        }
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
                repositories {
                    mavenCentral()
                }
            }
            rootProject.name = "test-project"
            """.trimIndent(),
        )
    }

    companion object {
        private val KSP_VERSIONS = listOf(
            KspVersion(ksp = "2.0.21-1.0.28", kotlin = "2.0.21"),
            KspVersion(ksp = "2.1.10-1.0.30", kotlin = "2.1.10"),
            KspVersion(ksp = "2.3.5", kotlin = "2.3.10"),
        )
    }
}
