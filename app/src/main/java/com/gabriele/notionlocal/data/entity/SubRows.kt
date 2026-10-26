package com.gabriele.notionlocal.data.entity

/**
 * **Le sotto-righe** (dal 29/09/2026, "Create sub row" tenendo premuto il
 * nome di una riga): una riga può averne altre sotto di sé — "Marmellata"
 * con "Albicocca", "Pesca", "Fragola" —, che sono righe del database come
 * le altre, con la loro pagina e le loro proprietà. `parentRowId` dice da
 * quale sono nate; nella tabella stanno sotto di lei, rientrate, e la
 * freccia sul suo nome le apre e le chiude (`subRowsCollapsed`).
 */

/** Una riga al suo posto nell'albero: quanto è rientrata, e se sotto ne ha altre da mostrare. */
data class TreeRow(val row: DatabaseRowEntity, val depth: Int, val hasChildren: Boolean)

/**
 * Le righe in albero, nell'ordine in cui arrivano (filtro, ricerca e
 * ordinamento restano quelli della vista): ogni riga seguita dalle sue
 * sotto-righe, se sono aperte. Una sotto-riga la cui riga di partenza non
 * si vede (filtrata, o in un altro gruppo) sta in cima, come le altre:
 * nasconderla perché manca lei farebbe sparire righe che il filtro fa
 * vedere.
 */
fun rowTree(rows: List<DatabaseRowEntity>): List<TreeRow> {
    // Quasi sempre nessuna riga ne ha sotto: l'albero è l'elenco così com'è.
    // Con un dizionario di diecimila parole, e a ogni lettera scritta, conta.
    if (rows.none { it.parentRowId != null }) return rows.map { TreeRow(it, 0, false) }
    val ids = rows.mapTo(HashSet()) { it.id }
    fun parentOf(row: DatabaseRowEntity) = row.parentRowId?.takeIf { it in ids && it != row.id }
    val children = rows.groupBy { parentOf(it) }

    // Chi si raggiunge dalle righe in cima, aperte o chiuse che siano.
    val reachable = HashSet<String>()
    fun reach(row: DatabaseRowEntity) {
        if (!reachable.add(row.id)) return
        children[row.id].orEmpty().forEach { reach(it) }
    }
    val roots = children[null].orEmpty()
    roots.forEach { reach(it) }
    // Righe che si fanno da sotto-riga a vicenda: non dovrebbe succedere
    // mai, ma se succede non devono sparire. Stanno in cima.
    val loose = rows.filter { it.id !in reachable }

    val out = ArrayList<TreeRow>(rows.size)
    val seen = HashSet<String>()
    fun visit(row: DatabaseRowEntity, depth: Int) {
        if (!seen.add(row.id)) return
        val kids = children[row.id].orEmpty().filter { it.id !in seen }
        out += TreeRow(row, depth, kids.isNotEmpty())
        if (!row.subRowsCollapsed) kids.forEach { visit(it, depth + 1) }
    }
    (roots + loose).forEach { visit(it, 0) }
    return out
}

/**
 * **Le sotto-righe nell'ordinamento** (dal 26/10/2026, chiesto dall'utente):
 * una riga con le sue sotto-righe si ordina come un blocco, al posto del
 * primo dei suoi elementi (lei o una sotto-riga, a qualunque livello); dentro
 * il blocco le sotto-righe seguono le stesse regole. In "Food", con le non
 * spuntate in cima, una "Rosticceria" spuntata che ha dentro cose ancora da
 * prendere sta in alto con le non spuntate, invece di portarsele giù.
 * `sorted` sono le righe già messe in ordine dalle regole.
 */
fun sortedAsBlocks(sorted: List<DatabaseRowEntity>): List<DatabaseRowEntity> {
    if (sorted.none { it.parentRowId != null }) return sorted
    val ids = sorted.mapTo(HashSet()) { it.id }
    fun parentOf(row: DatabaseRowEntity) = row.parentRowId?.takeIf { it in ids && it != row.id }
    val children = sorted.groupBy { parentOf(it) }
    val position = HashMap<String, Int>(sorted.size).also { map -> sorted.forEachIndexed { i, row -> map[row.id] = i } }

    // Il posto di un blocco: il primo fra la riga e tutte le sue sotto-righe.
    val rank = HashMap<String, Int>()
    val visiting = HashSet<String>()
    fun rankOf(row: DatabaseRowEntity): Int {
        rank[row.id]?.let { return it }
        val own = position.getValue(row.id)
        // Righe che si fanno da sotto-riga a vicenda: ognuna al suo posto.
        if (!visiting.add(row.id)) return own
        val best = children[row.id].orEmpty().minOfOrNull { rankOf(it) }?.coerceAtMost(own) ?: own
        visiting.remove(row.id)
        rank[row.id] = best
        return best
    }

    val out = ArrayList<DatabaseRowEntity>(sorted.size)
    val seen = HashSet<String>()
    fun place(row: DatabaseRowEntity) {
        if (!seen.add(row.id)) return
        out += row
        children[row.id].orEmpty().sortedBy { rankOf(it) }.forEach { place(it) }
    }
    children[null].orEmpty().sortedBy { rankOf(it) }.forEach { place(it) }
    // Quelle in un giro di sotto-righe (non dovrebbe succedere) non spariscono.
    sorted.filter { it.id !in seen }.forEach { out += it }
    return out
}

/** Se fra queste righe qualcuna ha sotto di sé un'altra di loro: allora nella tabella c'è l'albero. */
fun hasSubRows(rows: List<DatabaseRowEntity>): Boolean {
    if (rows.none { it.parentRowId != null }) return false
    val ids = rows.mapTo(HashSet()) { it.id }
    return rows.any { it.parentRowId != null && it.parentRowId != it.id && it.parentRowId in ids }
}

/**
 * Le righe che un calcolo conta: di suo **solo quelle principali**, come
 * ha chiesto l'utente ("di default disattivata"); con "Include sub rows"
 * anche le sotto-righe.
 */
fun rowsForCalculation(rows: List<DatabaseRowEntity>, includeSubRows: Boolean): List<DatabaseRowEntity> =
    if (includeSubRows) rows else rows.filter { it.parentRowId == null }
