package com.gabriele.notionlocal.data.export

import java.io.File

/** Il file esportato, pronto da mandare, salvare o stampare. */
data class ExportedFile(val file: File, val mime: String) {
    val name: String get() = file.name
}

/** Cosa mettere nell'export, come scelto nella finestra "Export page". */
data class ExportOptions(
    val format: ExportFormat,
    val subpages: Boolean = false,
    /** I disegni a mano (dal 03/11/2026): solo nel PDF, che allora ha le proporzioni del telefono. */
    val drawings: Boolean = false,
    /** Per la stampa: un PDF solo, senza quelli delle pagine delle righe (che farebbero uno .zip). */
    val singleFile: Boolean = false
)
