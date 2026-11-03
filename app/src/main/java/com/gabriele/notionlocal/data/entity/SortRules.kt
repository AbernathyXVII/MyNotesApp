package com.gabriele.notionlocal.data.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * **Una regola di ordinamento**: la proprietà (`SORT_BY_NAME` per il nome
 * della riga) e il verso.
 *
 * **Le regole sono quante se ne vogliono** (dal 17/10/2026, chiesto
 * dall'utente), tutte in un elenco solo, nell'ordine in cui contano:
 * `PageEntity.sortRulesJson`. Prima c'erano solo la prima regola e la
 * seconda, in campi loro (`sortColumnId`, `thenSortColumnId`): la migrazione
 * 56→57 le ha spostate nell'elenco e quei campi sono rimasti vuoti.
 */
@Serializable
data class SortRuleSpec(val column: String, val descending: Boolean = false)

private val sortJson = Json { ignoreUnknownKeys = true }

/**
 * Le regole salvate, nell'ordine in cui contano. Una proprietà conta una
 * volta sola (la prima volta); un testo che non si legge vale "nessuna".
 */
fun parseSortRules(json: String?): List<SortRuleSpec> =
    json?.let { runCatching { sortJson.decodeFromString<List<SortRuleSpec>>(it) }.getOrNull() }
        .orEmpty()
        .distinctBy { it.column }

/** Le regole da salvare; nessuna = null. */
fun encodeSortRules(rules: List<SortRuleSpec>): String? =
    rules.distinctBy { it.column }.takeIf { it.isNotEmpty() }?.let { sortJson.encodeToString(it) }

/** Tutte le regole di questa pagina (o vista), nell'ordine in cui contano. */
fun PageEntity.sortRules(): List<SortRuleSpec> = parseSortRules(sortRulesJson)

/** La pagina con queste regole al posto delle sue. */
fun PageEntity.withSortRules(rules: List<SortRuleSpec>): PageEntity = copy(sortRulesJson = encodeSortRules(rules))

/** Solo le regole su proprietà che ci sono ancora (o sul nome della riga). */
fun List<SortRuleSpec>.existingIn(columns: List<DatabaseColumnEntity>): List<SortRuleSpec> =
    filter { rule -> rule.column == SORT_BY_NAME || columns.any { it.id == rule.column } }

/**
 * **Come si ordinano i valori di una proprietà** (dal 02/11/2026): dà le
 * scritte delle due scelte di "Sort" nel menu della proprietà. Le parole
 * dalla A alla Z o al contrario, i numeri dal più grande al più piccolo o al
 * contrario, le date dalle più recenti, le caselle prima le spuntate.
 */
enum class SortKind { TEXT, NUMBER, DATE, CHECKBOX }

/**
 * Per una proprietà che di suo è un testo (il nome, un tag, un link…): un
 * numero se ogni valore scritto comincia con una cifra ("12", "3 km",
 * "-4"), altrimenti un testo, come chiesto dall'utente: "che inizi con una
 * lettera, lo ordinerà in ordine alfabetico, altrimenti se è un numero lo
 * ordinerà dal più grande al più piccolo". Senza valori, un testo.
 */
fun sortKindOfTexts(texts: List<String>): SortKind {
    val written = texts.map { it.trim() }.filter { it.isNotEmpty() }
    return if (written.isNotEmpty() && written.all { NUMBER_START.containsMatchIn(it) }) SortKind.NUMBER else SortKind.TEXT
}

private val NUMBER_START = Regex("""^[-+]?[.,]?\d""")
