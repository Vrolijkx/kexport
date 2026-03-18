package com.happix.kexport.processor

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspSourcesDir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
class ExportProcessorTest {
    typealias GeneratedCode = String

    @Test
    fun `class annotated with Export generates typealias`() {
        val (result, generated) = compile(
            SourceFile.kotlin(
                "User.kt",
                """
                package com.example
                import com.happix.kexport.Export
                @Export
                class User(val name: String)
                """.trimIndent(),
            ),
        )
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        generated shouldContain "typealias User = com.example.User"
    }

    @Test
    fun `class with custom alias uses alias name`() {
        val (result, generated) = compile(
            SourceFile.kotlin(
                "User.kt",
                """
                package com.example
                import com.happix.kexport.Export
                @Export(alias = "UserModel")
                class User(val name: String)
                """.trimIndent(),
            ),
        )
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        generated shouldContain "typealias UserModel = com.example.User"
        generated shouldNotContain "typealias User ="
    }

    @Test
    fun `function annotated with Export generates inline wrapper`() {
        val (result, generated) = compile(
            SourceFile.kotlin(
                "Funcs.kt",
                """
                package com.example
                import com.happix.kexport.Export
                @Export
                fun greet(name: String): String = name
                """.trimIndent(),
            ),
        )
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        generated shouldContain "inline fun greet"
        generated shouldContain "com.example.greet"
    }

    @Test
    fun `function with custom alias uses alias name`() {
        val (result, generated) = compile(
            SourceFile.kotlin(
                "Funcs.kt",
                """
                package com.example
                import com.happix.kexport.Export
                @Export(alias = "farewell")
                fun sayGoodbye(name: String): String = name
                """.trimIndent(),
            ),
        )
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        generated shouldContain "inline fun farewell"
        generated shouldContain "com.example.sayGoodbye"
        generated shouldNotContain "inline fun sayGoodbye"
    }

    @Test
    fun `function with Unit return type omits return type in wrapper`() {
        val (result, generated) = compile(
            SourceFile.kotlin(
                "Funcs.kt",
                """
                package com.example
                import com.happix.kexport.Export
                @Export
                fun log(msg: String) {}
                """.trimIndent(),
            ),
        )
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        generated shouldContain "inline fun log"
        generated shouldNotContain ": kotlin.Unit"
        generated shouldNotContain ": Unit"
    }

    @Test
    fun `classes outside packageToScan are excluded`() {
        val (result, generated) = compile(
            SourceFile.kotlin(
                "Other.kt",
                """
                package com.other
                import com.happix.kexport.Export
                @Export
                class OtherClass
                """.trimIndent(),
            ),
            packageToScan = "com.example",
        )
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        generated shouldBe null
    }

    @Test
    fun `classes in sub-packages are included`() {
        val (result, generated) = compile(
            SourceFile.kotlin(
                "Inner.kt",
                """
                package com.example.models
                import com.happix.kexport.Export
                @Export
                class Inner
                """.trimIndent(),
            ),
            packageToScan = "com.example",
        )
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        generated shouldContain "typealias Inner = com.example.models.Inner"
    }

    @Test
    fun `exports are sorted alphabetically by simple name`() {
        val (result, generated) = compile(
            SourceFile.kotlin(
                "Models.kt",
                """
                package com.example
                import com.happix.kexport.Export
                @Export class Zebra
                @Export class Apple
                @Export class Mango
                """.trimIndent(),
            ),
        )
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        val content = generated!!
        val appleIdx = content.indexOf("typealias Apple")
        val mangoIdx = content.indexOf("typealias Mango")
        val zebraIdx = content.indexOf("typealias Zebra")
        (appleIdx < mangoIdx) shouldBe true
        (mangoIdx < zebraIdx) shouldBe true
    }

    @Test
    fun `no annotated symbols produces no output file`() {
        val (result, generated) = compile(
            SourceFile.kotlin(
                "Plain.kt",
                """
                package com.example
                class Plain
                """.trimIndent(),
            ),
        )
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        generated shouldBe null
    }

    @Test
    fun `output file is written to the configured outputPackage`() {
        val (result, generated) = compile(
            SourceFile.kotlin(
                "User.kt",
                """
                package com.example
                import com.happix.kexport.Export
                @Export class User
                """.trimIndent(),
            ),
            outputPackage = "com.example.exports",
        )
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        generated shouldContain "package com.example.exports"
    }

    @Test
    fun `multiple classes and functions all appear in output`() {
        val (result, generated) = compile(
            SourceFile.kotlin(
                "Mixed.kt",
                """
                package com.example
                import com.happix.kexport.Export
                @Export class Foo
                @Export class Bar
                @Export fun baz() {}
                """.trimIndent(),
            ),
        )
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        generated shouldContain "typealias Bar = com.example.Bar"
        generated shouldContain "typealias Foo = com.example.Foo"
        generated shouldContain "inline fun baz"
    }

    @Test
    fun `delegate method can be called omitting parameters with default arguments`() {
        val (result, _) = compile(
            SourceFile.kotlin(
                "Funcs.kt",
                """
                package com.example
                import com.happix.kexport.Export
                @Export
                fun send(to: String, subject: String = "No subject", body: String = ""): String =
                    "${'$'}to|${'$'}subject|${'$'}body"
                """.trimIndent(),
            ),
        )
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK

        val exportsClass = result.classLoader.loadClass("com.example.dsl.ExportsKt")

        val fullOverload = exportsClass.getMethod("send", String::class.java, String::class.java, String::class.java)
        fullOverload.invoke(null, "alice", "Hello", "World") shouldBe "alice|Hello|World"

        val twoParamOverload = exportsClass.getMethod("send", String::class.java, String::class.java)
        twoParamOverload.invoke(null, "alice", "Hello") shouldBe "alice|Hello|"

        val oneParamOverload = exportsClass.getMethod("send", String::class.java)
        oneParamOverload.invoke(null, "alice") shouldBe "alice|No subject|"
    }

    @Test
    fun `wrapper can be called with named parameters`() {
        val (result, _) = compile(
            SourceFile.kotlin(
                "Funcs.kt",
                """
                package com.example
                import com.happix.kexport.Export
                @Export
                fun send(to: String, subject: String, body: String) {}
                """.trimIndent(),
            ),
            SourceFile.kotlin(
                "Usage.kt",
                """
                package com.example.test
                import com.example.dsl.send
                fun test() {
                    send(to = "alice@example.com", subject = "Hello", body = "World")
                }
                """.trimIndent(),
            ),
        )
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    }

    @Test
    fun `function with vararg and positional parameter generates correct wrapper`() {
        val (result, generated) = compile(
            SourceFile.kotlin(
                "Funcs.kt",
                """
                package com.example
                import com.happix.kexport.Export
                @Export
                fun log(tag: String, vararg msgs: String) {}
                """.trimIndent(),
            ),
        )
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        generated shouldContain "inline fun log(tag: kotlin.String, vararg msgs: kotlin.String)"
        generated shouldContain "com.example.log(tag = tag, *msgs)"
    }

    private fun compile(
        vararg sources: SourceFile,
        packageToScan: String = "com.example",
        outputPackage: String = "com.example.dsl",
    ): Pair<JvmCompilationResult, GeneratedCode?> {
        val compilation = KotlinCompilation().apply {
            this.sources = sources.toList()
            inheritClassPath = true
            configureKsp {
                symbolProcessorProviders += ExportProcessorProvider()
                processorOptions["kexport.packageToScan"] = packageToScan
                processorOptions["kexport.outputPackage"] = outputPackage
            }
        }
        val result = compilation.compile()
        val generated = compilation.kspSourcesDir
            .walkTopDown()
            .firstOrNull { it.name == "Exports.kt" }
            ?.readText()
        return result to generated
    }
}
