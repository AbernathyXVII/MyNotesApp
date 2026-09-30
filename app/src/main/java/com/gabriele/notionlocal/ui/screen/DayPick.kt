package com.gabriele.notionlocal.ui.screen

import java.time.LocalDate

/**
 * **Il tocco su un giorno nel calendario della proprietà Date**: dove
 * vanno inizio e fine. `withEnd` è l'interruttore "End date".
 *
 * **Ritoccando un giorno col pallino arancione, il pallino se ne va** (dal
 * 30/09/2026, chiesto dall'utente): la fine si toglie; l'inizio si toglie,
 * e se c'era una fine è lei a restare, da sola. Senza più niente scelto,
 * "Done" svuota la cella, come prima.
 *
 * Gli altri tocchi come sempre: senza "End date" il giorno toccato è la
 * data; con "End date" il primo tocco è l'inizio, il secondo (più avanti)
 * la fine, e un tocco dopo un periodo completo, o prima dell'inizio,
 * ricomincia.
 */
internal fun pickCalendarDay(
    day: LocalDate,
    start: LocalDate?,
    end: LocalDate?,
    withEnd: Boolean
): Pair<LocalDate?, LocalDate?> {
    val shownEnd = end.takeIf { withEnd }
    return when {
        day == shownEnd -> start to null
        day == start -> shownEnd to null
        !withEnd -> day to null
        start == null || shownEnd != null || day < start -> day to null
        else -> start to day
    }
}
