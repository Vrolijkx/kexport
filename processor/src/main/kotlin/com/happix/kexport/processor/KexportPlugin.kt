package com.happix.kexport.processor

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class KexportPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (!project.plugins.hasPlugin("com.google.devtools.ksp")) {
            project.plugins.apply("com.google.devtools.ksp")
        }

        val extension = project.extensions.create("kexport", KexportExtension::class.java)

        project.dependencies.add("compileOnly", "com.happix.kexport:annotation:1.0.0")
        project.dependencies.add("ksp", "com.happix.kexport:processor:1.0.0")

        project.afterEvaluate {
            if (!extension.packageToScan.isPresent) {
                throw GradleException("Missing required option: kexport.packageToScan")
            }
            val packageToScan = extension.packageToScan.get()
            val outputPackage = extension.outputPackage.orNull ?: "$packageToScan.dsl"

            project.extensions.configure(KspExtension::class.java) { ksp ->
                ksp.arg("kexport.packageToScan", packageToScan)
                ksp.arg("kexport.outputPackage", outputPackage)
            }
        }
    }
}
