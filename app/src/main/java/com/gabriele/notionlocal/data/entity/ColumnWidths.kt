package com.gabriele.notionlocal.data.entity

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * **Le larghezze delle colonne della tabella** (dal 29/09/2026, chieste
 * dall'utente "come in Excel"): ogni vista ha le sue, come su Notion.
 * `PageEntity.columnWidths` le tiene in dp per chiave — l'id della colonna,
 * o [NAME_COLUMN_KEY] per Name; una colonna senza la sua ha la larghezza di
 * sempre. `PageEntity.fitColumns` dice quali si allargano da sole quanto il
 * loro contenuto ("Fit to content"): finché è acceso la larghezza salvata
 * resta da parte, e spegnendolo torna quella.
 */
const val NAME_COLUMN_KEY = "__name__"

/** Più stretta di così una colonna non si legge più: resta l'icona o poco più. */
const val MIN_COLUMN_WIDTH_DP = 48

/** Un limite solo di sicurezza: un testo lunghissimo non fa una tabella di chilometri. */
const val MAX_COLUMN_WIDTH_DP = 1200

private val widthsJson = Json { ignoreUnknownKeys = true }

fun parseColumnWidths(raw: String?): Map<String, Int> =
    raw?.let { runCatching { widthsJson.decodeFromString<Map<String, Int>>(it) }.getOrNull() }.orEmpty()

fun encodeColumnWidths(widths: Map<String, Int>): String? =
    widths.takeIf { it.isNotEmpty() }?.let { widthsJson.encodeToString<Map<String, Int>>(it.toSortedMap()) }

fun parseFitColumns(raw: String?): Set<String> =
    raw?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet().orEmpty()

fun encodeFitColumns(keys: Set<String>): String? =
    keys.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(",")

fun clampColumnWidth(widthDp: Int): Int = widthDp.coerceIn(MIN_COLUMN_WIDTH_DP, MAX_COLUMN_WIDTH_DP)

/** La larghezza di sempre: 150, Name 200. */
const val DEFAULT_COLUMN_WIDTH_DP = 150
const val DEFAULT_NAME_COLUMN_WIDTH_DP = 200

/**
 * Trascinando fin quasi alla larghezza di sempre (a 4 dp o meno) ci si
 * ferma lì: col dito non si ritroverebbe mai il punto preciso.
 */
const val SNAP_TO_DEFAULT_DP = 4

/** Una colonna "Image" è un quadrato: larga quanto è alta una riga. */
const val DEFAULT_IMAGE_COLUMN_WIDTH_DP = 44

fun defaultColumnWidth(key: String, type: ColumnType? = null): Int = when {
    key == NAME_COLUMN_KEY -> DEFAULT_NAME_COLUMN_WIDTH_DP
    type == ColumnType.IMAGE -> DEFAULT_IMAGE_COLUMN_WIDTH_DP
    else -> DEFAULT_COLUMN_WIDTH_DP
}

/** Dentro i limiti, e agganciata alla larghezza di sempre se ci è vicina. */
fun snapColumnWidth(key: String, widthDp: Int, default: Int = defaultColumnWidth(key)): Int {
    return if (kotlin.math.abs(widthDp - default) <= SNAP_TO_DEFAULT_DP) default else clampColumnWidth(widthDp)
}
