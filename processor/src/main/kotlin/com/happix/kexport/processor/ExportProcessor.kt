package com.happix.kexport.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStream

private const val ANNOTATION_NAME = "com.happix.kexport.Export"
private const val ANNOTATION_SIMPLE_NAME = "Export"

class ExportProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val configuration: KexportConfiguration,
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
            logger.warn("${configuration.outputFileName} already generated; skipping extra round.")
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
        val declarationsPartOfScanPackage = allDeclarations.filter { decl ->
            val pkg = decl.packageName.asString()
            pkg == packageToScan || pkg.startsWith("$packageToScan.")
        }

        return declarationsPartOfScanPackage
            .sortedBy { it.simpleName.asString() }
            .mapNotNull { decl ->
                return@mapNotNull exportEntryFor(decl)
            }
    }

    private fun exportEntryFor(
        decl: KSDeclaration,
    ): ExportEntry? {
        val qualifiedName = decl.qualifiedName?.asString()
        if (qualifiedName == null) {
            logger.error("Cannot resolve qualified name for ${decl.simpleName.asString()}", decl)
            return null
        }
        return when (decl) {
            is KSClassDeclaration -> {
                if (Modifier.SEALED in decl.modifiers) {
                    validateSealedSubclassesAnnotated(decl)
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
    ) {
        sealedClass.getSealedSubclasses().forEach { subclass ->
            if (!subclass.hasExportAnnotation()) {
                logger.error(
                    "@Export on sealed class '${sealedClass.simpleName.asString()}' requires all subclasses " +
                        "to be annotated with @Export, but '${subclass.simpleName.asString()}' is missing it.",
                    subclass,
                )
            }
            if (Modifier.SEALED in subclass.modifiers) {
                validateSealedSubclassesAnnotated(subclass)
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
        val sourceFiles = exports.mapNotNull { it.declaration.containingFile }.toTypedArray()

        val newCodeFileStream = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true, *sourceFiles),
            packageName = configuration.outputPackage,
            fileName = configuration.outputFileName.removeSuffix(".kt"),
        )

        newCodeFileStream.writeExportsFile(exports)
        logger.info("Generated ${configuration.outputPackage}.${configuration.outputFileName} with ${exports.size} export(s).")
    }

    private fun OutputStream.writeExportsFile(
        exports: List<ExportEntry>,
    ) {
        val classEntries = exports.filterIsInstance<ExportEntry.ClassEntry>()
        val functionEntries = exports.filterIsInstance<ExportEntry.FunctionEntry>()

        this.bufferedWriter().use { writer ->
            writer.appendLine("@file:Suppress(\"unused\", \"NOTHING_TO_INLINE\")")
            writer.appendLine()
            writer.appendLine("package ${configuration.outputPackage}")
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

    private fun KSDeclaration.hasExportAnnotation(): Boolean = this.annotations.any {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == ANNOTATION_NAME
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
