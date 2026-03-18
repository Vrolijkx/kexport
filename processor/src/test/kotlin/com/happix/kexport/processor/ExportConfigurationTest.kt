package com.happix.kexport.processor

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ExportConfigurationTest {

    @Test
    fun `both keys present`() {
        val config = ExportConfiguration.from(
            mapOf(
                "kexport.packageToScan" to "com.example",
                "kexport.outputPackage" to "com.example.exports",
            ),
        )
        config.packageToScan shouldBe "com.example"
        config.outputPackage shouldBe "com.example.exports"
    }

    @Test
    fun `only packageToScan - outputPackage defaults to packageToScan`() {
        val config = ExportConfiguration.from(
            mapOf("kexport.packageToScan" to "com.example"),
        )
        config.packageToScan shouldBe "com.example"
        config.outputPackage shouldBe "com.example.dsl"
    }

    @Test
    fun `throw an IllegalArgumentException packageToScan is null`() {
        shouldThrow<IllegalArgumentException> {
            ExportConfiguration.from(
                mapOf("kexport.outputPackage" to "com.example.exports"),
            )
        }.message shouldBe "Missing required option: kexport.packageToScan"
    }

    @Test
    fun `unrelated keys are ignored`() {
        val config = ExportConfiguration.from(
            mapOf(
                "kexport.packageToScan" to "com.example",
                "kexport.outputPackage" to "com.example.exports",
                "some.other.key" to "value",
            ),
        )

        config shouldBeEqual ExportConfiguration("com.example", "com.example.exports")
    }
}
