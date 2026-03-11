package com.happix.kexport.processor

private const val KEY_PACKAGE_TO_SCAN = "kexport.packageToScan"
private const val KEY_OUTPUT_PACKAGE = "kexport.outputPackage"

private const val DEFAULT_OUTPUT_PACKAGE = "com.happix.kexport.generated"

/**
 * Parsed configuration for the Export KSP processor.
 */
data class ExportConfiguration(
    /** The package (and sub-packages) to scan for @Export-annotated classes, or null to scan all. */
    val packageToScan: String?,
    /** The package for the generated Exports.kt file. */
    val outputPackage: String,
) {
    companion object {
        fun from(options: Map<String, String>): ExportConfiguration {
            val packageToScan = options[KEY_PACKAGE_TO_SCAN]
            return ExportConfiguration(
                packageToScan = packageToScan,
                outputPackage = options[KEY_OUTPUT_PACKAGE] ?: packageToScan ?: DEFAULT_OUTPUT_PACKAGE,
            )
        }
    }
}

