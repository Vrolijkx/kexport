package com.happix.kexport

/**
 * Marks a class for export. The KSP processor will generate a file containing
 * a typealias for each annotated class, collected into a single `Exports.kt` file.
 *
 * Optionally provide [alias] to override the generated alias name. Defaults to the
 * simple class name.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Export(val alias: String = "")
