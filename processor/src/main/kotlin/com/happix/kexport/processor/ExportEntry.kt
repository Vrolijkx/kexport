package com.happix.kexport.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/**
 * Holds the resolved export name and qualified name for a single annotated declaration
 * (either a class or a function).
 */
sealed class ExportEntry {
    abstract val declaration: KSDeclaration
    abstract val exportName: String
    abstract val qualifiedName: String

    data class ClassEntry(
        override val declaration: KSClassDeclaration,
        override val exportName: String,
        override val qualifiedName: String,
    ) : ExportEntry()

    data class FunctionEntry(
        override val declaration: KSFunctionDeclaration,
        override val exportName: String,
        override val qualifiedName: String,
    ) : ExportEntry()
}
