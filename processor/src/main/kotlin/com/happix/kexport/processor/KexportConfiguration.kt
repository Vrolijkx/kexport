package com.happix.kexport.processor

private const val KEY_PACKAGE_TO_SCAN = "kexport.packageToScan"
private const val KEY_OUTPUT_PACKAGE = "kexport.outputPackage"
private const val KEY_OUTPUT_FILE_NAME = "kexport.outputFileName"

/**
 * Parsed configuration for the Export KSP processor.
 */
data class KexportConfiguration(
    /** The package (and sub-packages) to scan for @Export-annotated classes, or null to scan all. */
    val packageToScan: String,
    /** The package where the full export file is generated to. */
    val outputPackage: String = "$packageToScan.dsl",
    /** The name of the generated file.  defaults to "Dsl.kt"*/
    val outputFileName: String = "Dsl.kt",
) {

    init {
        require(packageToScan.isNotBlank()) { "kexport.packageToScan must not be blank" }
        require(outputPackage.isNotBlank()) { "kexport.outputPackage must not be blank" }
        require(outputFileName.isNotBlank()) { "kexport.outputFileName must not be blank" }
        require(outputFileName.endsWith(".kt")) { "kexport.outputFileName must end with .kt extension" }
    }

    companion object {
        fun fromKspArguments(options: Map<String, String>): KexportConfiguration {
            val packageToScan = options[KEY_PACKAGE_TO_SCAN] ?: throw IllegalArgumentException("Missing required option: $KEY_PACKAGE_TO_SCAN")
            return KexportConfiguration(
                packageToScan = packageToScan,
                outputPackage = options[KEY_OUTPUT_PACKAGE] ?: "$packageToScan.dsl",
                outputFileName = options[KEY_OUTPUT_FILE_NAME] ?: "Dsl.kt",
            )
        }

        fun fromGradleExtension(extension: KexportExtension): KexportConfiguration {
            val packageToScan = extension.packageToScan.orNull ?: throw IllegalArgumentException("Missing required option: kexport.packageToScan")
            return KexportConfiguration(
                packageToScan = packageToScan,
                outputPackage = extension.outputPackage.orNull ?: "$packageToScan.dsl",
                outputFileName = extension.outputFileName.orNull ?: "Dsl.kt",
            )
        }
    }

    fun toKspArguments() = mapOf(
        KEY_PACKAGE_TO_SCAN to packageToScan,
        KEY_OUTPUT_PACKAGE to outputPackage,
        KEY_OUTPUT_FILE_NAME to outputFileName,
    )
}
