package com.happix.kexport.processor

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files

class KexportPluginTest : FunSpec({

    lateinit var projectDir: File
    lateinit var buildFile: File
    lateinit var settingsFile: File

    beforeEach {
        projectDir = Files.createTempDirectory("kexport-test").toFile()
        buildFile = projectDir.resolve("build.gradle.kts")
        settingsFile = projectDir.resolve("settings.gradle.kts")
        settingsFile.writeText("""
            rootProject.name = "test-project"
        """.trimIndent())
    }

    afterEach {
        projectDir.deleteRecursively()
    }

    fun runBuild(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*args, "--stacktrace")
        .build()

    fun runBuildAndFail(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*args, "--stacktrace")
        .buildAndFail()

    test("plugin applies without errors") {
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "2.3.10"
                id("com.happix.kexport")
            }

            kexport {
                packageToScan = "com.example"
            }
        """.trimIndent())

        val result = runBuild("tasks")
        result.task(":tasks")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    test("plugin configures default outputPackage") {
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "2.3.10"
                id("com.happix.kexport")
            }

            kexport {
                packageToScan = "com.example"
            }

            tasks.register("printKspArgs") {
                doLast {
                    val ksp = extensions.findByType(com.google.devtools.ksp.gradle.KspExtension::class.java)
                    println("KSP_ARGS: " + ksp?.arguments)
                }
            }
        """.trimIndent())

        val result = runBuild("printKspArgs")
        result.output shouldContain "kexport.outputPackage=com.example.dsl"
    }

    test("plugin passes custom outputPackage") {
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "2.3.10"
                id("com.happix.kexport")
            }

            kexport {
                packageToScan = "com.example"
                outputPackage = "com.example.custom"
            }

            tasks.register("printKspArgs") {
                doLast {
                    val ksp = extensions.findByType(com.google.devtools.ksp.gradle.KspExtension::class.java)
                    println("KSP_ARGS: " + ksp?.arguments)
                }
            }
        """.trimIndent())

        val result = runBuild("printKspArgs")
        result.output shouldContain "kexport.outputPackage=com.example.custom"
    }

    test("missing packageToScan fails build") {
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "2.3.10"
                id("com.happix.kexport")
            }

            // Intentionally NOT setting kexport { packageToScan = ... }
        """.trimIndent())

        val result = runBuildAndFail("tasks")
        // The build should fail because packageToScan is a required property
        result.output.shouldNotBeNull()
    }
})
