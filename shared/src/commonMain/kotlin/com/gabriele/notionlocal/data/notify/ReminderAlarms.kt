package com.gabriele.notionlocal.data.notify

/**
 * **Rimettere in fila gli avvisi dei promemoria** dopo una scrittura nel
 * database (dal 04/11/2026, nel codice comune).
 *
 * Le repository, che ora stanno nel codice comune, non conoscono le sveglie
 * di Android: chiamano [reschedule], e a fare il lavoro è chi si è registrato.
 * Sul telefono è `NoteAlarms`, che si registra quando parte (`NoteAlarms.init`);
 * prima di allora, come prima, non succede niente. Sul PC, per ora, nessuno:
 * gli avvisi del PC arriveranno più avanti.
 */
object ReminderAlarms {
    @Volatile
    private var onReschedule: () -> Unit = {}

    fun register(reschedule: () -> Unit) {
        onReschedule = reschedule
    }

    fun reschedule() = onReschedule()
}
