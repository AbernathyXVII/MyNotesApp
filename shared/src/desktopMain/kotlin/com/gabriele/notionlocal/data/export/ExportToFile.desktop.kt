package com.gabriele.notionlocal.data.export

import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

/** Sul PC l'esportazione non c'è ancora. */
actual suspend fun exportToFile(
    context: PlatformContext,
    factory: ViewModelFactory,
    pageId: String,
    options: ExportOptions,
    untitled: String
): ExportedFile? = null
