package com.gabriele.notionlocal.data.export

import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

/**
 * **Una pagina o un database in un file** (Markdown o PDF, vedi [ExportOptions]):
 * sul telefono lo prepara `PageExporter`, come prima; sul PC l'esportazione
 * arriverà più avanti, e intanto non c'è (null). Null anche se non riesce.
 */
expect suspend fun exportToFile(
    context: PlatformContext,
    factory: ViewModelFactory,
    pageId: String,
    options: ExportOptions,
    untitled: String
): ExportedFile?
