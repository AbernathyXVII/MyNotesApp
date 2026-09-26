package com.gabriele.notionlocal.viewmodel

import com.gabriele.notionlocal.data.repository.PageRepository.PageChange

/**
 * **L'Annulla lasciato da una schermata che se ne va.**
 *
 * "Move to", "Duplicate" e "Move to trash" dai tre puntini di una pagina o
 * di un database lasciano la schermata su cui si fanno: si torna indietro,
 * o si entra nella copia. Il loro Annulla non può restare nella cronologia
 * di quella schermata — non la si rivede — e va alla **prossima pagina che
 * compare**, che lo prende appena si apre
 * (`PageEditorViewModel.adoptPendingPageChange`). È "Annulla l'ultima
 * cosa fatta", come in ogni editor, anche se l'ultima cosa è stata fatta
 * da un'altra parte.
 *
 * Ne tiene uno solo: quello che si è appena fatto. Il successivo prende il
 * suo posto, e la prima pagina che compare lo porta via, che lo usi o no.
 * Vive quanto l'app: è memoria di lavoro, non un dato da salvare.
 */
internal object PendingPageUndo {
    private var pending: PageChange? = null

    fun offer(change: PageChange) {
        pending = change
    }

    fun take(): PageChange? = pending.also { pending = null }
}
