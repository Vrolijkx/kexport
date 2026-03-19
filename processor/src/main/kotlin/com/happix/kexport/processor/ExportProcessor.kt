package com.happix.kexport.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate

private const val ANNOTATION_NAME = "com.happix.kexport.Export"
private const val ANNOTATION_SIMPLE_NAME = "Export"
private const val OUTPUT_FILE = "Exports"

class ExportProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val configuration: ExportConfiguration,
) : SymbolProcessor {

    // Tracks whether we have already written the output file in a previous round.
    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(ANNOTATION_NAME)

        val (valid, deferred) = symbols.partition { it.validate() }

        val exports = resolveExports(valid)

        if (exports.isEmpty()) return deferred

        validateNoDuplicateExportNames(exports)

        if (generated) {
            logger.warn("$OUTPUT_FILE already generated; skipping extra round.")
            return deferred
        }

        writeExportsFile(exports)

        generated = true

        return deferred
    }

    /**
     * Resolves the annotated classes and functions and pairs each with its export name.
     * Returns a list of [ExportEntry]s for all valid annotated declarations.
     */
    private fun resolveExports(
        validSymbols: List<KSAnnotated>,
    ): List<ExportEntry> {
        val allDeclarations = validSymbols.filterIsInstance<KSDeclaration>()

        val packageToScan = configuration.packageToScan
        val declarations = if (packageToScan != null) {
            allDeclarations.filter { decl ->
                val pkg = decl.packageName.asString()
                pkg == packageToScan || pkg.startsWith("$packageToScan.")
            }
        } else {
            allDeclarations
        }

        val exportedQualifiedNames = declarations
            .mapNotNull { it.qualifiedName?.asString() }
            .toSet()

        return declarations
            .sortedBy { it.simpleName.asString() }
            .mapNotNull { decl ->
                val qualifiedName = decl.qualifiedName?.asString()
                if (qualifiedName == null) {
                    logger.error("Cannot resolve qualified name for ${decl.simpleName.asString()}", decl)
                    return@mapNotNull null
                }
                when (decl) {
                    is KSClassDeclaration -> {
                        if (Modifier.SEALED in decl.modifiers) {
                            validateSealedSubclassesAnnotated(decl, exportedQualifiedNames)
                        }
                        ExportEntry.ClassEntry(
                            declaration = decl,
                            exportName = decl.determineExportName(),
                            qualifiedName = qualifiedName,
                        )
                    }
                    is KSFunctionDeclaration -> ExportEntry.FunctionEntry(
                        declaration = decl,
                        exportName = decl.determineExportName(),
                        qualifiedName = qualifiedName,
                    )
                    else -> {
                        logger.warn("@Export is not supported on ${decl.simpleName.asString()}")
                        null
                    }
                }
            }
    }

    private fun validateNoDuplicateExportNames(exports: List<ExportEntry>) {
        exports
            .groupBy { it.exportName }
            .filter { (_, entries) -> entries.size > 1 }
            .forEach { (name, entries) ->
                entries.forEach { entry ->
                    logger.error(
                        "duplicate export name '$name': '${entry.declaration.simpleName.asString()}' " +
                            "and '${entries.first { it !== entry }.declaration.simpleName.asString()}' " +
                            "both export under the same name.",
                        entry.declaration,
                    )
                }
            }
    }

    private fun validateSealedSubclassesAnnotated(
        sealedClass: KSClassDeclaration,
        exportedQualifiedNames: Set<String>,
    ) {
        sealedClass.getSealedSubclasses().forEach { subclass ->
            val subQualifiedName = subclass.qualifiedName?.asString()
            if (subQualifiedName == null || subQualifiedName !in exportedQualifiedNames) {
                logger.error(
                    "@Export on sealed class '${sealedClass.simpleName.asString()}' requires all subclasses " +
                        "to be annotated with @Export, but '${subclass.simpleName.asString()}' is missing it.",
                    subclass,
                )
            }
            if (Modifier.SEALED in subclass.modifiers) {
                validateSealedSubclassesAnnotated(subclass, exportedQualifiedNames)
            }
        }
    }

    /**
     * Writes the generated file containing typealias declarations for classes
     * and delegating wrapper functions for exported functions.
     */
    private fun writeExportsFile(
        exports: List<ExportEntry>,
    ) {
        val outputPackage = configuration.outputPackage

        val sourceFiles = exports.mapNotNull { it.declaration.containingFile }.toTypedArray()

        val outputStream = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true, *sourceFiles),
            packageName = outputPackage,
            fileName = OUTPUT_FILE,
        )

        val classEntries = exports.filterIsInstance<ExportEntry.ClassEntry>()
        val functionEntries = exports.filterIsInstance<ExportEntry.FunctionEntry>()

        outputStream.bufferedWriter().use { writer ->
            writer.appendLine("@file:Suppress(\"unused\")")
            writer.appendLine()
            writer.appendLine("package $outputPackage")
            writer.appendLine()
            writer.appendLine("// Auto-generated by kexport — do not edit manually.")

            if (classEntries.isNotEmpty()) {
                writer.appendLine()
                for (entry in classEntries) {
                    writer.appendLine("typealias ${entry.exportName} = ${entry.qualifiedName}")
                }
            }

            if (functionEntries.isNotEmpty()) {
                writer.appendLine()
                for (entry in functionEntries) {
                    for (overload in generateFunctionWrappers(entry)) {
                        writer.appendLine(overload)
                    }
                }
            }
        }

        logger.info("Generated $outputPackage.$OUTPUT_FILE with ${exports.size} export(s).")
    }

    /**
     * Generates delegating wrapper functions for an exported function.
     * Returns one overload per trailing-optional-param count (from full signature down to required-only).
     */
    private fun generateFunctionWrappers(entry: ExportEntry.FunctionEntry): List<String> {
        val func = entry.declaration
        val returnType = func.returnType?.resolve()
        val returnTypeName = returnType?.declaration?.qualifiedName?.asString() ?: "Unit"
        val isUnit = returnTypeName == "Unit" || returnTypeName == "kotlin.Unit"
        val returnTypeStr = if (isUnit) "" else ": $returnTypeName"

        val params = func.parameters
        val trailingOptionalCount = params.reversed().takeWhile { it.hasDefault && !it.isVararg }.count()

        return (0..trailingOptionalCount).map { omit ->
            val activeParams = params.dropLast(omit)
            val paramDeclarations = activeParams.joinToString(", ") { param ->
                val prefix = if (param.isVararg) "vararg " else ""
                val name = param.name?.asString() ?: "_"
                val typeName = param.type.resolve().declaration.qualifiedName?.asString() ?: "Any"
                "$prefix$name: $typeName"
            }
            val paramPassThrough = activeParams.joinToString(", ") { param ->
                val name = param.name?.asString() ?: "_"
                if (param.isVararg) "*$name" else "$name = $name"
            }
            buildString {
                append("inline fun ${entry.exportName}($paramDeclarations)$returnTypeStr")
                append(" = ${entry.qualifiedName}($paramPassThrough)")
            }
        }
    }

    private fun KSDeclaration.determineExportName(): String = this.annotations
        .firstOrNull { it.shortName.asString() == ANNOTATION_SIMPLE_NAME }
        ?.arguments
        ?.firstOrNull { it.name?.asString() == "alias" }
        ?.value
        ?.toString()
        ?.takeIf { it.isNotBlank() }
        ?: this.simpleName.asString()
}
