package com.happix.kexport.processor

import org.gradle.api.provider.Property

abstract class KexportExtension {
    /** The package (and sub-packages) to scan for @Export-annotated classes. Required. */
    abstract val packageToScan: Property<String>

    /** The package for the generated Exports.kt file. Defaults to [packageToScan].dsl */
    abstract val outputPackage: Property<String>

    /** The name of the generated file. Defaults to "Dsl.kt" */
    abstract val outputFileName: Property<String>
}
