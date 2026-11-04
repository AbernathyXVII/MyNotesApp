package com.gabriele.notionlocal.viewmodel

import android.content.Context
import com.gabriele.notionlocal.data.export.DatabaseReader
import com.gabriele.notionlocal.data.export.ExportOptions
import com.gabriele.notionlocal.data.export.ExportedFile
import com.gabriele.notionlocal.data.export.PageExporter
import com.gabriele.notionlocal.ui.i18n.Strings

/**
 * **La pagina in un file** da mandare, salvare o stampare (vedi `data/export`);
 * null se non riesce. Era dentro `PageEditorViewModel` fino al 04/11/2026: il
 * ViewModel è passato al codice comune, e l'esportazione usa la stampa e i
 * file di Android.
 */
suspend fun PageEditorViewModel.export(context: Context, factory: ViewModelFactory, options: ExportOptions): ExportedFile? {
    val pageId = openPageId ?: return null
    return runCatching {
        PageExporter(context, pages, DatabaseReader(factory, pages)).export(pageId, options, Strings.untitled)
    }.getOrNull()
}
