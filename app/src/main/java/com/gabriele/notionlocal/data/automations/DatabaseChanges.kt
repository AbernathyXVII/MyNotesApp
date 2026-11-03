package com.gabriele.notionlocal.data.automations

import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * **Cosa cambia nei database**, per le automazioni (dal 03/11/2026): una riga
 * nuova, una cella scritta. Lo dice `DatabaseRepository`, chiunque abbia
 * fatto la modifica (la tabella, la pagina di una riga, un bottone); ascolta
 * `Automations`, quando l'app l'ha messo in ascolto.
 */
interface DatabaseChangeListener {
    fun rowAdded(databaseId: String, rowId: String)
    fun cellChanged(rowId: String, columnId: String, before: String, after: String)
}

object DatabaseChanges {
    @Volatile
    var listener: DatabaseChangeListener? = null

    /**
     * Le modifiche fatte da un'automazione (dentro [AutomationRun]) non ne
     * fanno partire altre, come su Notion: un'automazione che cambia la
     * proprietà che la fa partire girerebbe per sempre.
     */
    suspend fun rowAdded(databaseId: String, rowId: String) {
        val listener = listener ?: return
        if (currentCoroutineContext()[AutomationRun] != null) return
        listener.rowAdded(databaseId, rowId)
    }

    suspend fun cellChanged(rowId: String, columnId: String, before: String?, after: String) {
        val listener = listener ?: return
        if (currentCoroutineContext()[AutomationRun] != null) return
        if (before.orEmpty() == after) return
        listener.cellChanged(rowId, columnId, before.orEmpty(), after)
    }
}

/** Il segno delle scritture fatte da un'automazione: vedi [DatabaseChanges]. */
class AutomationRun : AbstractCoroutineContextElement(AutomationRun) {
    companion object Key : CoroutineContext.Key<AutomationRun>
}
