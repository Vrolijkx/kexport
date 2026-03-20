package com.happix.kexport.processor

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class KexportConfigurationTest {

    @Test
    fun `fromKspArguments should parse if all arguments are present`() {
        val config = KexportConfiguration.fromKspArguments(
            mapOf(
                "kexport.packageToScan" to "com.example",
                "kexport.outputPackage" to "com.example.exports",
                "kexport.outputFileName" to "SomeFile.kt",
            ),
        )
        config.packageToScan shouldBe "com.example"
        config.outputPackage shouldBe "com.example.exports"
        config.outputFileName shouldBe "SomeFile.kt"
    }

    @Test
    fun `fromKspArguments should use default is only packageToScan is present`() {
        val config = KexportConfiguration.fromKspArguments(
            mapOf("kexport.packageToScan" to "com.example"),
        )
        config.packageToScan shouldBe "com.example"
        config.outputPackage shouldBe "com.example.dsl"
        config.outputFileName shouldBe "Dsl.kt"
    }

    @Test
    fun `fromKspArguments should throw an IllegalArgumentException packageToScan is not present`() {
        shouldThrow<IllegalArgumentException> {
            KexportConfiguration.fromKspArguments(
                mapOf("kexport.outputPackage" to "com.example.exports"),
            )
        }.message shouldBe "Missing required option: kexport.packageToScan"
    }

    @Test
    fun `fromKspArguments should ignore unrelated keys`() {
        val config = KexportConfiguration.fromKspArguments(
            mapOf(
                "kexport.packageToScan" to "com.example",
                "kexport.outputPackage" to "com.example.exports",
                "some.other.key" to "value",
            ),
        )

        config shouldBeEqual KexportConfiguration("com.example", "com.example.exports")
    }

    @Test
    fun `fromGradleExtension should parse if all properties are set`() {
        val extension = createExtension()
        extension.packageToScan.set("com.example")
        extension.outputPackage.set("com.example.exports")
        extension.outputFileName.set("MyExports.kt")

        val config = KexportConfiguration.fromGradleExtension(extension)

        config.packageToScan shouldBe "com.example"
        config.outputPackage shouldBe "com.example.exports"
        config.outputFileName shouldBe "MyExports.kt"
    }

    @Test
    fun `fromGradleExtension should use defaults when only packageToScan is set`() {
        val extension = createExtension()
        extension.packageToScan.set("com.example")

        val config = KexportConfiguration.fromGradleExtension(extension)

        config.packageToScan shouldBe "com.example"
        config.outputPackage shouldBe "com.example.dsl"
        config.outputFileName shouldBe "Dsl.kt"
    }

    @Test
    fun `fromGradleExtension should throw IllegalArgumentException when packageToScan is not set`() {
        val extension = createExtension()

        shouldThrow<IllegalArgumentException> {
            KexportConfiguration.fromGradleExtension(extension)
        }.message shouldBe "Missing required option: kexport.packageToScan"
    }

    @Test
    fun `fromGradleExtension should use default outputFileName when only packageToScan and outputPackage are set`() {
        val extension = createExtension()
        extension.packageToScan.set("com.example")
        extension.outputPackage.set("com.example.api")

        val config = KexportConfiguration.fromGradleExtension(extension)

        config.packageToScan shouldBe "com.example"
        config.outputPackage shouldBe "com.example.api"
        config.outputFileName shouldBe "Dsl.kt"
    }

    @Test
    fun `toKspArguments should produce a map that round-trips through fromKspArguments`() {
        val original = KexportConfiguration(
            packageToScan = "com.example",
            outputPackage = "com.example.exports",
            outputFileName = "MyExports.kt",
        )

        val restored = KexportConfiguration.fromKspArguments(original.toKspArguments())

        restored shouldBeEqual original
    }

    private fun createExtension(): KexportExtension {
        val project = ProjectBuilder.builder().build()
        return project.extensions.create("kexport", KexportExtension::class.java)
    }
}
