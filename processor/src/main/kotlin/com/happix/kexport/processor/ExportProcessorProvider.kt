package com.happix.kexport.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class ExportProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        ExportProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            configuration = ExportConfiguration.from(environment.options),
        )
}
