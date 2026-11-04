package com.gabriele.notionlocal.data.export

import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

actual suspend fun exportToFile(
    context: PlatformContext,
    factory: ViewModelFactory,
    pageId: String,
    options: ExportOptions,
    untitled: String
): ExportedFile? = runCatching {
    PageExporter(context, factory.pageRepository, DatabaseReader(factory, factory.pageRepository)).export(pageId, options, untitled)
}.getOrNull()
