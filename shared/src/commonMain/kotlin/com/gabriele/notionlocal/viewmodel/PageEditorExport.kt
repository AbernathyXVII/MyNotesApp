package com.gabriele.notionlocal.viewmodel

import com.gabriele.notionlocal.data.export.ExportOptions
import com.gabriele.notionlocal.data.export.ExportedFile
import com.gabriele.notionlocal.data.export.exportToFile
import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.ui.i18n.Strings

/**
 * **La pagina in un file** da mandare, salvare o stampare (vedi `data/export`);
 * null se non riesce. Era dentro `PageEditorViewModel` fino al 04/11/2026; il
 * file lo prepara la piattaforma (`exportToFile`).
 */
suspend fun PageEditorViewModel.export(context: PlatformContext, factory: ViewModelFactory, options: ExportOptions): ExportedFile? {
    val pageId = openPageId ?: return null
    return exportToFile(context, factory, pageId, options, Strings.untitled)
}
