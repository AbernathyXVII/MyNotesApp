package com.gabriele.notionlocal.data.entity

/**
 * **Il testo che non ci sta in una cella** (dal 30/09/2026, "Long text"
 * nelle impostazioni di ogni tabella), nell'ordine in cui l'ha chiesto
 * l'utente:
 * - [TRUNCATE]: finisce coi tre puntini, e toccando la cella compare intero
 *   (di suo);
 * - [WIDEN]: la colonna si allarga quanto basta perché si legga tutto;
 * - [WRAP]: va a capo dentro la cella, e tutta la riga si allunga.
 *
 * Sta in `PageEntity.cellOverflow` come numero, nella colonna del database
 * `wrapCells`: nata come sì/no, il suo 0 era "no" e 1 "sì" — gli stessi
 * numeri di TRUNCATE e WRAP, così niente da migrare.
 */
enum class CellOverflow(val code: Int) {
    TRUNCATE(0),
    WIDEN(2),
    WRAP(1);

    companion object {
        /** Un numero sconosciuto: i tre puntini, come di suo. */
        fun of(code: Int): CellOverflow = entries.find { it.code == code } ?: TRUNCATE
    }
}
