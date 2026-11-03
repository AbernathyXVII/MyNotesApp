package com.gabriele.notionlocal.data.buttons

/** Perché un'azione non si è potuta fare: la schermata lo dice con due parole. */
enum class ButtonProblem {
    /** Il database scelto non c'è più (o è nel cestino). */
    NO_DATABASE,
    /** Il database è protetto da password e chiuso. */
    LOCKED_DATABASE,
    /** "This page" ma il bottone non sta in una riga né nella pagina di una riga. */
    NO_THIS_PAGE,
    /** La pagina da aprire, o in cui mettere i blocchi, non c'è più. */
    NO_PAGE,
    /** La pagina è bloccata ("Lock page"): non ci si scrive. */
    LOCKED_PAGE,
    /** Una proprietà scelta non c'è più, o non è più di un tipo che si può cambiare. */
    NO_PROPERTY,
    /** Un valore che non va bene per la proprietà (un numero che non è un numero, un tag cancellato). */
    BAD_VALUE,
    /** Il filtro di "Edit pages in" guarda una proprietà che non c'è più: per sicurezza non si cambia niente. */
    NO_FILTER,
    /** Le notifiche dell'app sono spente nel telefono. */
    NOTIFICATIONS_OFF
}
