package com.gabriele.notionlocal.data.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * **Una regola di raggruppamento**: la proprietà, se i suoi gruppi senza
 * nemmeno una pagina si nascondono e, per una data, come si raggruppa
 * (giorni, settimane, mesi o anni).
 *
 * **Le regole sono quante se ne vogliono** (dal 28/10/2026, chiesto
 * dall'utente: "la stessa cosa [dei Sort] con il Group"), tutte in un
 * elenco solo, nell'ordine in cui contano: la prima fa i gruppi, la seconda
 * divide ogni gruppo, la terza ogni sottogruppo, e così via. Nella bacheca
 * la prima fa le colonne e le altre le fasce, una dentro l'altra.
 *
 * Prima c'erano il gruppo e il sottogruppo, in campi loro
 * (`tableGroupColumnId`, `tableSubGroupColumnId`, i loro "Hide empty" e
 * `dateGroupingJson`): la migrazione 65→66 li ha spostati in
 * `tableGroupRulesJson` e `boardGroupRulesJson` e quei campi sono rimasti vuoti.
 */
@Serializable
data class GroupRuleSpec(
    val column: String,
    val hideEmpty: Boolean = true,
    val dateBy: DateGroupBy = DateGroupBy()
)

/**
 * Un livello di raggruppamento pronto da usare: la regola con la sua
 * proprietà, che c'è ancora e si può raggruppare.
 */
data class GroupLevel(
    val column: DatabaseColumnEntity,
    val hideEmpty: Boolean = true,
    val dateBy: DateGroupBy = DateGroupBy()
)

/** "Hide empty groups" delle colonne della bacheca: di suo spento, la bacheca le mostra tutte. */
const val BOARD_COLUMNS_HIDE_EMPTY = false

private val groupJson = Json { ignoreUnknownKeys = true; encodeDefaults = false }

/**
 * Le regole salvate, nell'ordine in cui contano. Una proprietà conta una
 * volta sola (la prima); un testo che non si legge vale "nessuna".
 */
fun parseGroupRules(json: String?): List<GroupRuleSpec> =
    json?.let { runCatching { groupJson.decodeFromString<List<GroupRuleSpec>>(it) }.getOrNull() }
        .orEmpty()
        .distinctBy { it.column }

/** Le regole da salvare; nessuna = null. */
fun encodeGroupRules(rules: List<GroupRuleSpec>): String? =
    rules.distinctBy { it.column }.takeIf { it.isNotEmpty() }?.let { groupJson.encodeToString(it) }

/** I raggruppamenti della tabella di questa pagina (o vista), dal primo. */
fun PageEntity.tableGroupRules(): List<GroupRuleSpec> = parseGroupRules(tableGroupRulesJson)

/** Quelli della bacheca: la prima regola fa le colonne, le altre le fasce. */
fun PageEntity.boardGroupRules(): List<GroupRuleSpec> = parseGroupRules(boardGroupRulesJson)

fun PageEntity.groupRules(board: Boolean): List<GroupRuleSpec> = if (board) boardGroupRules() else tableGroupRules()

fun PageEntity.withGroupRules(board: Boolean, rules: List<GroupRuleSpec>): PageEntity =
    if (board) copy(boardGroupRulesJson = encodeGroupRules(rules)) else copy(tableGroupRulesJson = encodeGroupRules(rules))

/**
 * I livelli che valgono davvero: le regole su proprietà che ci sono ancora e
 * si possono raggruppare (`canGroup`), nell'ordine. Una regola rimasta senza
 * la sua proprietà (cancellata) si salta, e quelle dopo salgono di un posto.
 */
fun List<GroupRuleSpec>.levelsIn(columns: List<DatabaseColumnEntity>): List<GroupLevel> =
    mapNotNull { rule ->
        columns.find { it.id == rule.column }?.takeIf { it.type.canGroup }?.let { GroupLevel(it, rule.hideEmpty, rule.dateBy) }
    }

/** Le regole con gli id delle proprietà tradotti: per la copia di un database. */
fun encodeGroupRulesMapped(json: String?, column: (String) -> String): String? =
    json?.let { encodeGroupRules(parseGroupRules(it).map { rule -> rule.copy(column = column(rule.column)) }) }

/**
 * **Dal gruppo e dal sottogruppo di prima** (migrazione 65→66): le regole
 * che facevano. Il sottogruppo valeva solo con un gruppo e su un'altra proprietà.
 */
fun legacyGroupRules(
    group: String?,
    sub: String?,
    hideEmptyGroups: Boolean,
    hideEmptySubGroups: Boolean,
    groupDateBy: DateGroupBy,
    subDateBy: DateGroupBy
): List<GroupRuleSpec> {
    if (group == null) return emptyList()
    val first = GroupRuleSpec(group, hideEmptyGroups, groupDateBy)
    val second = sub?.takeIf { it != group }?.let { GroupRuleSpec(it, hideEmptySubGroups, subDateBy) }
    return listOfNotNull(first, second)
}
