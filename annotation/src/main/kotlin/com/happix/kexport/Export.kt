package com.happix.kexport

/**
 * Marks a class or function for export. The KSP processor will generate a file containing
 * a typealias for each annotated class and a delegating wrapper for each annotated function,
 * collected into a single `Exports.kt` file.
 *
 * Optionally provide [alias] to override the generated alias name. Defaults to the
 * simple class/function name.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Export(val alias: String = "")
