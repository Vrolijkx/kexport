package com.happix.kexport.processor

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class KexportPluginTest {

    private lateinit var projectDir: File
    private lateinit var buildFile: File
    private lateinit var settingsFile: File

    @BeforeEach
    fun setUp() {
        projectDir = Files.createTempDirectory("kexport-test").toFile()
        buildFile = projectDir.resolve("build.gradle.kts")
        settingsFile = projectDir.resolve("settings.gradle.kts")
        settingsFile.writeText(
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

    @AfterEach
    fun tearDown() {
        projectDir.deleteRecursively()
    }

    private fun runBuild(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*args, "--stacktrace")
        .build()

    private fun runBuildAndFail(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*args, "--stacktrace")
        .buildAndFail()

    @Test
    fun `plugin applies without errors`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm")
                id("com.happix.kexport")
            }

            kexport {
                packageToScan = "com.example"
            }
            """.trimIndent(),
        )

        val result = runBuild("tasks")
        result.task(":tasks")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `plugin configures default outputPackage`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm")
                id("com.happix.kexport")
            }

            kexport {
                packageToScan = "com.example"
            }

            tasks.register("printKspArgs") {
                doLast {
                    val ksp = project.extensions.findByType(com.google.devtools.ksp.gradle.KspExtension::class.java)
                    println("KSP_ARGS: " + ksp?.arguments)
                }
            }
            """.trimIndent(),
        )

        val result = runBuild("printKspArgs")
        result.output shouldContain "kexport.outputPackage=com.example.dsl"
    }

    @Test
    fun `plugin passes custom outputPackage`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm")
                id("com.happix.kexport")
            }

            kexport {
                packageToScan = "com.example"
                outputPackage = "com.example.custom"
            }

            tasks.register("printKspArgs") {
                doLast {
                    val ksp = project.extensions.findByType(com.google.devtools.ksp.gradle.KspExtension::class.java)
                    println("KSP_ARGS: " + ksp?.arguments)
                }
            }
            """.trimIndent(),
        )

        val result = runBuild("printKspArgs")
        result.output shouldContain "kexport.outputPackage=com.example.custom"
    }

    @Test
    fun `plugin works when KSP is already applied by the project`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm")
                id("com.google.devtools.ksp")
                id("com.happix.kexport")
            }

            kexport {
                packageToScan = "com.example"
            }
            """.trimIndent(),
        )

        val result = runBuild("tasks")
        result.task(":tasks")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `missing packageToScan fails build`() {
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm")
                id("com.happix.kexport")
            }

            // Intentionally NOT setting kexport { packageToScan = ... }
            """.trimIndent(),
        )

        val result = runBuildAndFail("tasks")
        result.output shouldContain "Missing required option: kexport.packageToScan"
    }
}
