package com.gabriele.notionlocal.data.share

import androidx.compose.runtime.mutableStateOf

/**
 * **La nota al volo** (dal 02/11/2026, chiesto dall'utente: "un widget sulla
 * schermata Home, o un tasto nelle impostazioni rapide, per scrivere una nota
 * in due secondi"). Il tasto delle impostazioni rapide (`QuickNoteTile`), il
 * widget (`QuickNoteWidget`) e la scorciatoia dell'icona aprono `MainActivity`
 * con [ACTION]; la navigazione (`AppNavHost`) crea la pagina e ci entra col
 * cursore sulla prima riga.
 *
 * Nel codice comune dal 04/11/2026; leggere la richiesta dall'`Intent` di
 * Android (`takeFrom`) sta nell'app, in `Incoming.kt`.
 */
object QuickNotes {
    const val ACTION = "com.gabriele.notionlocal.action.QUICK_NOTE"

    /**
     * Chiesta una nota al volo, per chi non ha una richiesta sua: i test.
     * `MainActivity` ne ha una per finestra (vedi `takeFrom`): con due finestre
     * di notE aperte una sull'altra, una richiesta di tutte e due se la
     * prendeva quella sotto, e la nota nasceva senza che ci si entrasse.
     */
    val requested = mutableStateOf(false)

    /** Le note al volo appena nate: uscendo, vuote se ne vanno (vedi `PageRepository.finishQuickNote`). */
    private val fresh = mutableSetOf<String>()

    /** La riga in cui mettere il cursore aprendo la nota, consumata dall'editor. */
    private var focus: Pair<String, String>? = null


    @Synchronized
    fun created(pageId: String, lineId: String) {
        fresh += pageId
        focus = pageId to lineId
    }

    /** La riga della nota [pageId] in cui mettere il cursore, se l'editor la mostra già ([present]); una volta sola. */
    @Synchronized
    fun takeFocus(pageId: String, present: (String) -> Boolean): String? =
        focus?.takeIf { it.first == pageId && present(it.second) }?.second?.also { focus = null }

    /** Le note al volo già aperte almeno una volta: solo queste, sparite dalla pila, sono "lasciate". */
    private val seen = mutableSetOf<String>()

    /**
     * Le note al volo **lasciate**: aperte, e ora non più fra le pagine
     * aperte una sull'altra ([open], gli id della pila di navigazione). Una
     * volta sola per nota: chi le riceve le chiude (`PageRepository.finishQuickNote`).
     * Entrando in una sottopagina la nota resta sotto, e non è lasciata.
     */
    @Synchronized
    fun leftBehind(open: Set<String>): List<String> {
        seen += fresh.filter { it in open }
        val left = seen.filter { it !in open }
        fresh -= left.toSet()
        seen -= left.toSet()
        return left
    }
}
