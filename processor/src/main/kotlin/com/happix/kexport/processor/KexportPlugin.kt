package com.happix.kexport.processor

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class KexportPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (!project.plugins.hasPlugin("com.google.devtools.ksp")) {
            project.plugins.apply("com.google.devtools.ksp")
        }

        val extension = project.extensions.create("kexport", KexportExtension::class.java)

        // Given the @Export annotation is not published to maven central(yet), we need to resolve it from the gradle plugins repository.
        project.repositories.maven { repo ->
            repo.url = project.uri("https://plugins.gradle.org/m2/")
        }

        project.dependencies.add("compileOnly", "com.happix.kexport:annotation:1.0.0")
        project.dependencies.add("ksp", "com.happix.kexport:processor:1.0.0")

        project.tasks.register("kexport") { task ->
            task.group = "kexport"
            task.description = "Run kexport code generation. Just an alias for invoking kspKotlin."
            task.dependsOn("kspKotlin")
        }

        project.afterEvaluate {
            val exportConfig = KexportConfiguration.fromGradleExtension(extension)

            project.extensions.configure(KspExtension::class.java) { ksp ->
                // For ksp, we need to pass the configuration as string value arguments, we can't pass the full configuration object.
                ksp.args(exportConfig.toKspArguments())
            }
        }
    }

    private fun KspExtension.args(args: Map<String, String>) {
        args.map { (key, value) ->
            this.arg(key, value)
        }
    }
}
