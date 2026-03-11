package com.happix.kexport.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Holds the resolved export name and qualified class name for a single annotated class.
 */
data class ExportEntry(
    val classDeclaration: KSClassDeclaration,
    val exportName: String,
    val qualifiedName: String,
)


