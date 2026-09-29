package com.gabriele.notionlocal.data.entity

/**
 * **Dove sta la colonna Name** nella tabella (dal 29/09/2026: "permettimi di
 * spostare ovunque io voglia anche la colonna Name"). Il posto lo tiene
 * `PageEntity.nameColumnOrder` del database che ha i dati, come ogni colonna
 * tiene il suo `orderIndex`: vale per tutte le sue viste, come l'ordine
 * delle colonne.
 *
 * Name sta subito prima della prima colonna con un numero uguale o più alto
 * del suo; -1, com'è per tutti i database di prima, vuol dire la prima.
 * Così spostando Name le colonne non cambiano numero.
 *
 * È la posizione di Name fra `columns` (in ordine): 0 = prima di tutte,
 * `columns.size` = dopo l'ultima.
 */
fun namePosition(columns: List<DatabaseColumnEntity>, nameOrder: Int): Int {
    if (nameOrder < 0) return 0
    val at = columns.indexOfFirst { it.orderIndex >= nameOrder }
    return if (at < 0) columns.size else at
}

/** Le chiavi della tabella da sinistra a destra: le colonne, con [NAME_COLUMN_KEY] al suo posto. */
fun tableKeys(columns: List<DatabaseColumnEntity>, nameOrder: Int): List<String> =
    columns.map { it.id }.toMutableList().apply { add(namePosition(columns, nameOrder), NAME_COLUMN_KEY) }

/**
 * Il numero da dare a Name perché stia a `place` fra colonne numerate 0, 1,
 * 2…: quello della colonna che le sta subito a destra, o uno più
 * dell'ultima. In testa -1, com'è di suo.
 */
fun nameOrderAt(place: Int): Int = if (place <= 0) -1 else place
